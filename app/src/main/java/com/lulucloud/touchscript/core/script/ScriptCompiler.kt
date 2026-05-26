package com.lulucloud.touchscript.core.script

data class CompilationResult(
    val ast: ScriptNode,
    val ir: IrProgram,
    val luaSource: String
)

class ScriptLowerer {

    fun lowerToIr(ast: ScriptNode): IrProgram = IrProgram(ast.statements.map(::lowerInstruction))

    private fun lowerInstruction(statement: StatementNode): IrInstruction {
        return when (statement) {
            is ClickActionNode -> IrClickAction(lowerExpression(statement.x), lowerExpression(statement.y))
            is LongPressActionNode -> IrLongPressAction(
                x = lowerExpression(statement.x),
                y = lowerExpression(statement.y),
                durationMs = lowerExpression(statement.durationMs)
            )

            is SwipeActionNode -> IrSwipeAction(
                startX = lowerExpression(statement.startX),
                startY = lowerExpression(statement.startY),
                endX = lowerExpression(statement.endX),
                endY = lowerExpression(statement.endY),
                durationMs = lowerExpression(statement.durationMs)
            )

            is KeyboardInputActionNode -> IrKeyboardInputAction(lowerExpression(statement.text))
            is SleepActionNode -> IrSleepAction(lowerExpression(statement.durationMs))
            is LaunchAppActionNode -> IrLaunchAppAction(lowerExpression(statement.packageName))
            is LogActionNode -> IrLogAction(lowerExpression(statement.message))
            is ImageFindActionNode -> IrImageFindAction(
                imageUri = lowerExpression(statement.imageUri),
                confidence = lowerExpression(statement.confidence)
            )
            is AssignActionNode -> IrAssignAction(
                variableName = statement.variableName,
                expression = lowerExpression(statement.expression)
            )

            BackActionNode -> IrBackAction
            HomeActionNode -> IrHomeAction
            StopRunningActionNode -> IrStopRunningAction
            is RepeatControlNode -> IrRepeatInstruction(
                count = lowerExpression(statement.count),
                body = statement.body.map(::lowerInstruction)
            )

            is ForeverControlNode -> IrForeverInstruction(
                body = statement.body.map(::lowerInstruction)
            )

            is IfControlNode -> IrIfInstruction(
                condition = lowerExpression(statement.condition),
                thenBranch = statement.thenBranch.map(::lowerInstruction),
                elseBranch = statement.elseBranch.map(::lowerInstruction)
            )
        }
    }

    private fun lowerExpression(expressionNode: ExpressionNode): IrExpression {
        return when (expressionNode) {
            is NumberLiteralNode -> IrNumberLiteral(expressionNode.value)
            is BooleanLiteralNode -> IrBooleanLiteral(expressionNode.value)
            is StringLiteralNode -> IrStringLiteral(expressionNode.value)
            is VariableReferenceNode -> IrVariableReference(expressionNode.name)
            is UnaryExpressionNode -> IrUnaryExpression(
                operator = expressionNode.operator,
                operand = lowerExpression(expressionNode.operand)
            )

            is BinaryExpressionNode -> IrBinaryExpression(
                left = lowerExpression(expressionNode.left),
                operator = expressionNode.operator,
                right = lowerExpression(expressionNode.right)
            )

            is MemberAccessExpressionNode -> IrMemberAccessExpression(
                target = lowerExpression(expressionNode.target),
                propertyName = expressionNode.propertyName
            )

            is ImageFindExpressionNode -> IrImageFindExpression(
                imageName = lowerExpression(expressionNode.imageName),
                confidence = lowerExpression(expressionNode.confidence)
            )
        }
    }
}

class LuaGenerator {

    fun generateLua(ir: IrProgram): String {
        val builder = StringBuilder()
        builder.appendLine("local vars = {}")
        builder.appendLine("local function __member(value, key)")
        builder.appendLine("    if value == nil then return nil end")
        builder.appendLine("    return value[key]")
        builder.appendLine("end")
        ir.instructions.forEach { appendInstruction(builder, it, 0) }
        return builder.toString().trim()
    }

    private fun appendInstruction(builder: StringBuilder, instruction: IrInstruction, indentLevel: Int) {
        val indent = "    ".repeat(indentLevel)
        when (instruction) {
            is IrClickAction -> builder.appendLine(
                "${indent}touch.click(${renderExpression(instruction.x)}, ${renderExpression(instruction.y)})"
            )

            is IrLongPressAction -> builder.appendLine(
                "${indent}touch.longPress(${renderExpression(instruction.x)}, ${renderExpression(instruction.y)}, ${renderExpression(instruction.durationMs)})"
            )

            is IrSwipeAction -> builder.appendLine(
                "${indent}touch.swipe(${renderExpression(instruction.startX)}, ${renderExpression(instruction.startY)}, ${renderExpression(instruction.endX)}, ${renderExpression(instruction.endY)}, ${renderExpression(instruction.durationMs)})"
            )

            is IrKeyboardInputAction -> builder.appendLine(
                "${indent}keyboard.input(${renderExpression(instruction.text)})"
            )

            is IrSleepAction -> builder.appendLine(
                "${indent}device.sleep(${renderExpression(instruction.durationMs)})"
            )

            is IrLaunchAppAction -> builder.appendLine(
                "${indent}app.launch(${renderExpression(instruction.packageName)})"
            )

            is IrLogAction -> builder.appendLine(
                "${indent}log.info(${renderExpression(instruction.message)})"
            )

            is IrImageFindAction -> builder.appendLine(
                "${indent}image.requireFind(${renderExpression(instruction.imageUri)}, ${renderExpression(instruction.confidence)})"
            )

            is IrAssignAction -> builder.appendLine(
                "${indent}vars[${renderLuaString(instruction.variableName)}] = ${renderExpression(instruction.expression)}"
            )

            IrBackAction -> builder.appendLine("${indent}device.back()")
            IrHomeAction -> builder.appendLine("${indent}device.home()")
            IrStopRunningAction -> builder.appendLine("${indent}runtime.stop()")
            is IrRepeatInstruction -> {
                builder.appendLine("${indent}for __index = 1, ${renderExpression(instruction.count)} do")
                instruction.body.forEach { appendInstruction(builder, it, indentLevel + 1) }
                builder.appendLine("${indent}end")
            }

            is IrForeverInstruction -> {
                builder.appendLine("${indent}while true do")
                instruction.body.forEach { appendInstruction(builder, it, indentLevel + 1) }
                builder.appendLine("${indent}end")
            }

            is IrIfInstruction -> {
                builder.appendLine("${indent}if ${renderExpression(instruction.condition)} then")
                instruction.thenBranch.forEach { appendInstruction(builder, it, indentLevel + 1) }
                if (instruction.elseBranch.isNotEmpty()) {
                    builder.appendLine("${indent}else")
                    instruction.elseBranch.forEach { appendInstruction(builder, it, indentLevel + 1) }
                }
                builder.appendLine("${indent}end")
            }
        }
    }

    private fun renderExpression(expression: IrExpression): String {
        return when (expression) {
            is IrNumberLiteral -> renderNumber(expression.value)
            is IrBooleanLiteral -> if (expression.value) "true" else "false"
            is IrStringLiteral -> renderLuaString(expression.value)
            is IrVariableReference -> "vars[${renderLuaString(expression.name)}]"
            is IrUnaryExpression -> when (expression.operator) {
                UnaryOperator.NOT -> "(not ${renderExpression(expression.operand)})"
                UnaryOperator.NEGATE -> "(-${renderExpression(expression.operand)})"
            }

            is IrBinaryExpression -> {
                val left = renderExpression(expression.left)
                val right = renderExpression(expression.right)
                val operator = when (expression.operator) {
                    BinaryOperator.ADD -> "+"
                    BinaryOperator.SUBTRACT -> "-"
                    BinaryOperator.MULTIPLY -> "*"
                    BinaryOperator.DIVIDE -> "/"
                    BinaryOperator.GREATER_THAN -> ">"
                    BinaryOperator.GREATER_OR_EQUAL -> ">="
                    BinaryOperator.LESS_THAN -> "<"
                    BinaryOperator.LESS_OR_EQUAL -> "<="
                    BinaryOperator.EQUALS -> "=="
                    BinaryOperator.NOT_EQUALS -> "~="
                    BinaryOperator.AND -> "and"
                    BinaryOperator.OR -> "or"
                }
                "($left $operator $right)"
            }

            is IrMemberAccessExpression -> {
                "__member(${renderExpression(expression.target)}, ${renderLuaString(mapMemberName(expression.propertyName))})"
            }

            is IrImageFindExpression -> {
                "image.find(${renderExpression(expression.imageName)}, ${renderExpression(expression.confidence)})"
            }
        }
    }

    private fun mapMemberName(propertyName: String): String {
        return when (propertyName) {
            "找到", "found" -> "found"
            "置信度", "分数", "score" -> "score"
            "x", "X" -> "x"
            "y", "Y" -> "y"
            else -> propertyName
        }
    }

    private fun renderNumber(value: Double): String {
        val longValue = value.toLong()
        return if (value == longValue.toDouble()) longValue.toString() else value.toString()
    }

    private fun renderLuaString(value: String): String = "\"${value.replace("\"", "\\\"")}\""
}

class ScriptCompiler(
    private val parser: ScriptParser = ScriptParser(),
    private val lowerer: ScriptLowerer = ScriptLowerer(),
    private val luaGenerator: LuaGenerator = LuaGenerator()
) {

    fun compile(source: String): CompilationResult {
        val ast = parser.parseDsl(source)
        val ir = lowerer.lowerToIr(ast)
        val luaSource = luaGenerator.generateLua(ir)
        return CompilationResult(ast = ast, ir = ir, luaSource = luaSource)
    }
}
