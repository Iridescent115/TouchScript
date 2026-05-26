package com.lulucloud.touchscript.core.script

data class ScriptNode(
    val statements: List<StatementNode>
)

sealed interface StatementNode

sealed interface ActionNode : StatementNode

sealed interface ControlFlowNode : StatementNode

sealed interface ExpressionNode

data class ClickActionNode(
    val x: ExpressionNode,
    val y: ExpressionNode
) : ActionNode

data class LongPressActionNode(
    val x: ExpressionNode,
    val y: ExpressionNode,
    val durationMs: ExpressionNode
) : ActionNode

data class SwipeActionNode(
    val startX: ExpressionNode,
    val startY: ExpressionNode,
    val endX: ExpressionNode,
    val endY: ExpressionNode,
    val durationMs: ExpressionNode
) : ActionNode

data class KeyboardInputActionNode(
    val text: ExpressionNode
) : ActionNode

data class SleepActionNode(
    val durationMs: ExpressionNode
) : ActionNode

data class LaunchAppActionNode(
    val packageName: ExpressionNode
) : ActionNode

data class LogActionNode(
    val message: ExpressionNode
) : ActionNode

data class ImageFindActionNode(
    val imageUri: ExpressionNode,
    val confidence: ExpressionNode
) : ActionNode

data class AssignActionNode(
    val variableName: String,
    val expression: ExpressionNode
) : ActionNode

data object BackActionNode : ActionNode

data object HomeActionNode : ActionNode

data class RepeatControlNode(
    val count: ExpressionNode,
    val body: List<StatementNode>
) : ControlFlowNode

data class ForeverControlNode(
    val body: List<StatementNode>
) : ControlFlowNode

data class IfControlNode(
    val condition: ExpressionNode,
    val thenBranch: List<StatementNode>,
    val elseBranch: List<StatementNode>
) : ControlFlowNode

data class NumberLiteralNode(
    val value: Double
) : ExpressionNode

data class BooleanLiteralNode(
    val value: Boolean
) : ExpressionNode

data class StringLiteralNode(
    val value: String
) : ExpressionNode

data class VariableReferenceNode(
    val name: String
) : ExpressionNode

data class UnaryExpressionNode(
    val operator: UnaryOperator,
    val operand: ExpressionNode
) : ExpressionNode

data class BinaryExpressionNode(
    val left: ExpressionNode,
    val operator: BinaryOperator,
    val right: ExpressionNode
) : ExpressionNode

data class MemberAccessExpressionNode(
    val target: ExpressionNode,
    val propertyName: String
) : ExpressionNode

data class ImageFindExpressionNode(
    val imageName: ExpressionNode,
    val confidence: ExpressionNode
) : ExpressionNode

enum class UnaryOperator {
    NOT,
    NEGATE
}

enum class BinaryOperator {
    ADD,
    SUBTRACT,
    MULTIPLY,
    DIVIDE,
    GREATER_THAN,
    GREATER_OR_EQUAL,
    LESS_THAN,
    LESS_OR_EQUAL,
    EQUALS,
    NOT_EQUALS,
    AND,
    OR
}

class ScriptParseException(
    message: String,
    val line: Int,
    val column: Int = 1
) : IllegalArgumentException("$message（第 $line 行，第 $column 列）")
