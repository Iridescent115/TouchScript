package com.lulucloud.touchscript.core.script

private data class Token(
    val type: TokenType,
    val text: String,
    val column: Int
)

private enum class TokenType {
    NUMBER,
    STRING,
    IDENTIFIER,
    TRUE,
    FALSE,
    PLUS,
    MINUS,
    STAR,
    SLASH,
    GT,
    GTE,
    LT,
    LTE,
    EQ,
    NEQ,
    AND,
    OR,
    NOT,
    DOT,
    LEFT_PAREN,
    RIGHT_PAREN,
    EOF
}

internal class ExpressionParser(
    private val lineNumber: Int
) {
    private var tokens: List<Token> = emptyList()
    private var currentIndex: Int = 0

    fun parse(expression: String): ExpressionNode {
        val trimmed = expression.trim()
        if (trimmed.isBlank()) {
            throw ScriptParseException("表达式不能为空", lineNumber)
        }
        tokens = tokenize(trimmed)
        currentIndex = 0
        val parsed = parseOr()
        if (!isAtEnd()) {
            val token = peek()
            throw ScriptParseException("无法解析的表达式片段：${token.text}", lineNumber, token.column)
        }
        return parsed
    }

    private fun tokenize(expression: String): List<Token> {
        val result = mutableListOf<Token>()
        var index = 0

        while (index < expression.length) {
            val char = expression[index]
            when {
                char.isWhitespace() -> index += 1
                char.isDigit() -> {
                    val start = index
                    while (index < expression.length && expression[index].isDigit()) {
                        index += 1
                    }
                    if (index < expression.length && expression[index] == '.') {
                        index += 1
                        if (index >= expression.length || !expression[index].isDigit()) {
                            throw ScriptParseException("小数点后必须跟数字", lineNumber, index)
                        }
                        while (index < expression.length && expression[index].isDigit()) {
                            index += 1
                        }
                    }
                    result += Token(
                        type = TokenType.NUMBER,
                        text = expression.substring(start, index),
                        column = start + 1
                    )
                }

                char == '"' -> {
                    val start = index
                    index += 1
                    val builder = StringBuilder()
                    while (index < expression.length && expression[index] != '"') {
                        builder.append(expression[index])
                        index += 1
                    }
                    if (index >= expression.length) {
                        throw ScriptParseException("字符串引号没有闭合", lineNumber, start + 1)
                    }
                    index += 1
                    result += Token(
                        type = TokenType.STRING,
                        text = builder.toString(),
                        column = start + 1
                    )
                }

                isIdentifierStart(char) -> {
                    val start = index
                    while (index < expression.length && isIdentifierPart(expression[index])) {
                        index += 1
                    }
                    val text = expression.substring(start, index)
                    result += Token(
                        type = when (text) {
                            "真" -> TokenType.TRUE
                            "假" -> TokenType.FALSE
                            else -> TokenType.IDENTIFIER
                        },
                        text = text,
                        column = start + 1
                    )
                }

                else -> {
                    val token = when {
                        expression.startsWith(">=", index) -> Token(TokenType.GTE, ">=", index + 1)
                        expression.startsWith("<=", index) -> Token(TokenType.LTE, "<=", index + 1)
                        expression.startsWith("==", index) -> Token(TokenType.EQ, "==", index + 1)
                        expression.startsWith("!=", index) -> Token(TokenType.NEQ, "!=", index + 1)
                        expression.startsWith("&&", index) -> Token(TokenType.AND, "&&", index + 1)
                        expression.startsWith("||", index) -> Token(TokenType.OR, "||", index + 1)
                        else -> when (char) {
                            '+' -> Token(TokenType.PLUS, "+", index + 1)
                            '-' -> Token(TokenType.MINUS, "-", index + 1)
                            '*' -> Token(TokenType.STAR, "*", index + 1)
                            '/' -> Token(TokenType.SLASH, "/", index + 1)
                            '>' -> Token(TokenType.GT, ">", index + 1)
                            '<' -> Token(TokenType.LT, "<", index + 1)
                            '!' -> Token(TokenType.NOT, "!", index + 1)
                            '.' -> Token(TokenType.DOT, ".", index + 1)
                            '(' -> Token(TokenType.LEFT_PAREN, "(", index + 1)
                            ')' -> Token(TokenType.RIGHT_PAREN, ")", index + 1)
                            else -> throw ScriptParseException(
                                "无法识别的表达式字符：$char",
                                lineNumber,
                                index + 1
                            )
                        }
                    }

                    result += token
                    index += token.text.length
                }
            }
        }

        result += Token(TokenType.EOF, "", expression.length + 1)
        return result
    }

    private fun parseOr(): ExpressionNode {
        var expression = parseAnd()
        while (match(TokenType.OR)) {
            expression = BinaryExpressionNode(expression, BinaryOperator.OR, parseAnd())
        }
        return expression
    }

    private fun parseAnd(): ExpressionNode {
        var expression = parseEquality()
        while (match(TokenType.AND)) {
            expression = BinaryExpressionNode(expression, BinaryOperator.AND, parseEquality())
        }
        return expression
    }

    private fun parseEquality(): ExpressionNode {
        var expression = parseComparison()
        while (true) {
            expression = when {
                match(TokenType.EQ) -> BinaryExpressionNode(expression, BinaryOperator.EQUALS, parseComparison())
                match(TokenType.NEQ) -> BinaryExpressionNode(expression, BinaryOperator.NOT_EQUALS, parseComparison())
                else -> return expression
            }
        }
    }

    private fun parseComparison(): ExpressionNode {
        var expression = parseTerm()
        while (true) {
            expression = when {
                match(TokenType.GT) -> BinaryExpressionNode(expression, BinaryOperator.GREATER_THAN, parseTerm())
                match(TokenType.GTE) -> BinaryExpressionNode(expression, BinaryOperator.GREATER_OR_EQUAL, parseTerm())
                match(TokenType.LT) -> BinaryExpressionNode(expression, BinaryOperator.LESS_THAN, parseTerm())
                match(TokenType.LTE) -> BinaryExpressionNode(expression, BinaryOperator.LESS_OR_EQUAL, parseTerm())
                else -> return expression
            }
        }
    }

    private fun parseTerm(): ExpressionNode {
        var expression = parseFactor()
        while (true) {
            expression = when {
                match(TokenType.PLUS) -> BinaryExpressionNode(expression, BinaryOperator.ADD, parseFactor())
                match(TokenType.MINUS) -> BinaryExpressionNode(expression, BinaryOperator.SUBTRACT, parseFactor())
                else -> return expression
            }
        }
    }

    private fun parseFactor(): ExpressionNode {
        var expression = parseUnary()
        while (true) {
            expression = when {
                match(TokenType.STAR) -> BinaryExpressionNode(expression, BinaryOperator.MULTIPLY, parseUnary())
                match(TokenType.SLASH) -> BinaryExpressionNode(expression, BinaryOperator.DIVIDE, parseUnary())
                else -> return expression
            }
        }
    }

    private fun parseUnary(): ExpressionNode {
        return when {
            match(TokenType.NOT) -> UnaryExpressionNode(UnaryOperator.NOT, parseUnary())
            match(TokenType.MINUS) -> UnaryExpressionNode(UnaryOperator.NEGATE, parseUnary())
            else -> parsePostfix()
        }
    }

    private fun parsePostfix(): ExpressionNode {
        var expression = parsePrimary()
        while (match(TokenType.DOT)) {
            val property = consumeIdentifier("点号后必须跟属性名")
            expression = MemberAccessExpressionNode(expression, property.text)
        }
        return expression
    }

    private fun parsePrimary(): ExpressionNode {
        return when {
            match(TokenType.NUMBER) -> NumberLiteralNode(previous().text.toDouble())
            match(TokenType.STRING) -> StringLiteralNode(previous().text)
            match(TokenType.TRUE) -> BooleanLiteralNode(true)
            match(TokenType.FALSE) -> BooleanLiteralNode(false)
            match(TokenType.IDENTIFIER) -> {
                val identifier = previous()
                if (identifier.text == KEYWORD_IMAGE_FIND) {
                    ImageFindExpressionNode(
                        imageName = parseUnary(),
                        confidence = parseOr()
                    )
                } else if (identifier.text == KEYWORD_TEXT_RECOGNITION) {
                    TextRecognitionExpressionNode
                } else {
                    VariableReferenceNode(identifier.text)
                }
            }
            match(TokenType.LEFT_PAREN) -> {
                val expression = parseOr()
                consume(TokenType.RIGHT_PAREN, "缺少右括号")
                expression
            }

            else -> {
                val token = peek()
                throw ScriptParseException("无法解析的表达式片段：${token.text}", lineNumber, token.column)
            }
        }
    }

    private fun consumeIdentifier(message: String): Token {
        if (check(TokenType.IDENTIFIER)) {
            return advance()
        }
        val token = peek()
        throw ScriptParseException(message, lineNumber, token.column)
    }

    private fun consume(type: TokenType, message: String) {
        if (check(type)) {
            advance()
            return
        }
        val token = peek()
        throw ScriptParseException(message, lineNumber, token.column)
    }

    private fun match(type: TokenType): Boolean {
        if (!check(type)) {
            return false
        }
        advance()
        return true
    }

    private fun check(type: TokenType): Boolean = !isAtEnd() && peek().type == type

    private fun advance(): Token {
        if (!isAtEnd()) {
            currentIndex += 1
        }
        return previous()
    }

    private fun isAtEnd(): Boolean = peek().type == TokenType.EOF

    private fun peek(): Token = tokens[currentIndex]

    private fun previous(): Token = tokens[currentIndex - 1]

    private fun isIdentifierStart(char: Char): Boolean = char == '_' || char.isLetter() || char.code > 127

    private fun isIdentifierPart(char: Char): Boolean = isIdentifierStart(char) || char.isDigit()

    private companion object {
        const val KEYWORD_IMAGE_FIND = "识图"
        const val KEYWORD_TEXT_RECOGNITION = "识文字"
    }
}
