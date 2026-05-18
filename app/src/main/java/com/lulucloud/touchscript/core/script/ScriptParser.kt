package com.lulucloud.touchscript.core.script

class ScriptParser {

    fun parseDsl(source: String): ScriptNode {
        val lines = source.lineSequence()
            .mapIndexed { index, line ->
                SourceLine(
                    lineNumber = index + 1,
                    content = line.substringBefore("#").trim()
                )
            }
            .filter { it.content.isNotBlank() }
            .toList()

        val (statements, nextIndex) = parseBlock(lines, 0, emptySet())
        if (nextIndex != lines.size) {
            val line = lines[nextIndex]
            throw ScriptParseException("出现了未预期的结束语句：${line.content}", line.lineNumber)
        }
        return ScriptNode(statements)
    }

    private fun parseBlock(
        lines: List<SourceLine>,
        startIndex: Int,
        stopWords: Set<String>
    ): Pair<List<StatementNode>, Int> {
        val statements = mutableListOf<StatementNode>()
        var index = startIndex

        while (index < lines.size) {
            val line = lines[index]
            if (line.content in stopWords) {
                break
            }

            when {
                line.content.startsWith(KEYWORD_REPEAT_PREFIX) -> {
                    val repeatHead = line.content.removePrefix(KEYWORD_REPEAT_PREFIX)
                    if (!repeatHead.endsWith(KEYWORD_REPEAT_SUFFIX)) {
                        throw ScriptParseException("循环语句必须以“次”结尾", line.lineNumber)
                    }
                    val expressionText = repeatHead.removeSuffix(KEYWORD_REPEAT_SUFFIX).trim()
                    val count = ExpressionParser(line.lineNumber).parse(expressionText)
                    val (body, nextIndex) = parseBlock(lines, index + 1, setOf(KEYWORD_REPEAT_END))
                    if (nextIndex >= lines.size || lines[nextIndex].content != KEYWORD_REPEAT_END) {
                        throw ScriptParseException("循环语句缺少结束循环", line.lineNumber)
                    }
                    statements += RepeatControlNode(count = count, body = body)
                    index = nextIndex + 1
                }

                line.content.startsWith(KEYWORD_IF_PREFIX) -> {
                    val conditionText = line.content.removePrefix(KEYWORD_IF_PREFIX).trim()
                    val condition = ExpressionParser(line.lineNumber).parse(conditionText)
                    val (thenBranch, branchEndIndex) =
                        parseBlock(lines, index + 1, setOf(KEYWORD_ELSE, KEYWORD_IF_END))

                    val elseBranch = mutableListOf<StatementNode>()
                    var finalIndex = branchEndIndex

                    if (finalIndex < lines.size && lines[finalIndex].content == KEYWORD_ELSE) {
                        val (parsedElseBranch, elseEndIndex) =
                            parseBlock(lines, finalIndex + 1, setOf(KEYWORD_IF_END))
                        elseBranch += parsedElseBranch
                        finalIndex = elseEndIndex
                    }

                    if (finalIndex >= lines.size || lines[finalIndex].content != KEYWORD_IF_END) {
                        throw ScriptParseException("如果语句缺少结束如果", line.lineNumber)
                    }

                    statements += IfControlNode(
                        condition = condition,
                        thenBranch = thenBranch,
                        elseBranch = elseBranch
                    )
                    index = finalIndex + 1
                }

                else -> {
                    statements += parseAction(line)
                    index += 1
                }
            }
        }

        return statements to index
    }

    private fun parseAction(line: SourceLine): ActionNode {
        val content = line.content

        return when {
            content.startsWith(KEYWORD_CLICK) -> {
                val args = tokenizeArguments(content.removePrefix(KEYWORD_CLICK).trim(), line.lineNumber)
                requireArgumentCount(args, 2, line)
                ClickActionNode(
                    x = ExpressionParser(line.lineNumber).parse(args[0]),
                    y = ExpressionParser(line.lineNumber).parse(args[1])
                )
            }

            content.startsWith(KEYWORD_LONG_PRESS) -> {
                val args =
                    tokenizeArguments(content.removePrefix(KEYWORD_LONG_PRESS).trim(), line.lineNumber)
                requireArgumentCount(args, 3, line)
                LongPressActionNode(
                    x = ExpressionParser(line.lineNumber).parse(args[0]),
                    y = ExpressionParser(line.lineNumber).parse(args[1]),
                    durationMs = ExpressionParser(line.lineNumber).parse(args[2])
                )
            }

            content.startsWith(KEYWORD_SWIPE) -> {
                val args = tokenizeArguments(content.removePrefix(KEYWORD_SWIPE).trim(), line.lineNumber)
                requireArgumentCount(args, 5, line)
                SwipeActionNode(
                    startX = ExpressionParser(line.lineNumber).parse(args[0]),
                    startY = ExpressionParser(line.lineNumber).parse(args[1]),
                    endX = ExpressionParser(line.lineNumber).parse(args[2]),
                    endY = ExpressionParser(line.lineNumber).parse(args[3]),
                    durationMs = ExpressionParser(line.lineNumber).parse(args[4])
                )
            }

            content.startsWith(KEYWORD_SLEEP) -> {
                val expression = content.removePrefix(KEYWORD_SLEEP).trim()
                SleepActionNode(ExpressionParser(line.lineNumber).parse(expression))
            }

            content.startsWith(KEYWORD_LAUNCH_APP) -> {
                val expression = content.removePrefix(KEYWORD_LAUNCH_APP).trim()
                LaunchAppActionNode(ExpressionParser(line.lineNumber).parse(expression))
            }

            content.startsWith(KEYWORD_LOG) -> {
                val expression = content.removePrefix(KEYWORD_LOG).trim()
                LogActionNode(ExpressionParser(line.lineNumber).parse(expression))
            }

            content.startsWith(KEYWORD_ASSIGN) -> {
                val statement = content.removePrefix(KEYWORD_ASSIGN).trim()
                val equalsIndex = statement.indexOf('=')
                if (equalsIndex <= 0) {
                    throw ScriptParseException("赋值语句格式应为：设 变量 = 表达式", line.lineNumber)
                }
                val variableName = statement.substring(0, equalsIndex).trim()
                val expression = statement.substring(equalsIndex + 1).trim()
                if (variableName.isBlank()) {
                    throw ScriptParseException("变量名不能为空", line.lineNumber)
                }
                AssignActionNode(
                    variableName = variableName,
                    expression = ExpressionParser(line.lineNumber).parse(expression)
                )
            }

            content == KEYWORD_BACK -> BackActionNode
            content == KEYWORD_HOME -> HomeActionNode
            else -> throw ScriptParseException("无法识别的语句：$content", line.lineNumber)
        }
    }

    private fun tokenizeArguments(source: String, lineNumber: Int): List<String> {
        if (source.isBlank()) {
            return emptyList()
        }

        val args = mutableListOf<String>()
        val builder = StringBuilder()
        var inString = false

        source.forEachIndexed { index, char ->
            when {
                char == '"' -> {
                    builder.append(char)
                    inString = !inString
                }

                char.isWhitespace() && !inString -> {
                    if (builder.isNotBlank()) {
                        args += builder.toString()
                        builder.clear()
                    }
                }

                else -> builder.append(char)
            }

            if (index == source.lastIndex && builder.isNotBlank()) {
                args += builder.toString()
            }
        }

        if (inString) {
            throw ScriptParseException("字符串引号没有闭合", lineNumber)
        }

        return args
    }

    private fun requireArgumentCount(arguments: List<String>, expected: Int, line: SourceLine) {
        if (arguments.size != expected) {
            throw ScriptParseException(
                "参数数量不正确，期望 $expected 个，实际 ${arguments.size} 个",
                line.lineNumber
            )
        }
    }

    private data class SourceLine(
        val lineNumber: Int,
        val content: String
    )

    private companion object {
        const val KEYWORD_CLICK = "点击 "
        const val KEYWORD_LONG_PRESS = "长按 "
        const val KEYWORD_SWIPE = "滑动 "
        const val KEYWORD_SLEEP = "等待 "
        const val KEYWORD_LAUNCH_APP = "启动应用 "
        const val KEYWORD_LOG = "记录 "
        const val KEYWORD_ASSIGN = "设 "
        const val KEYWORD_BACK = "返回"
        const val KEYWORD_HOME = "主页"
        const val KEYWORD_REPEAT_PREFIX = "循环 "
        const val KEYWORD_REPEAT_SUFFIX = " 次"
        const val KEYWORD_REPEAT_END = "结束循环"
        const val KEYWORD_IF_PREFIX = "如果 "
        const val KEYWORD_ELSE = "否则"
        const val KEYWORD_IF_END = "结束如果"
    }
}

private fun StringBuilder.isNotBlank(): Boolean = this.isNotEmpty() && this.any { !it.isWhitespace() }
