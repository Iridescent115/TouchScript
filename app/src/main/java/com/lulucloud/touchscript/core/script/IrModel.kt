package com.lulucloud.touchscript.core.script

data class IrProgram(
    val instructions: List<IrInstruction>
)

sealed interface IrInstruction

sealed interface IrAction : IrInstruction

sealed interface IrControlFlow : IrInstruction

sealed interface IrExpression

data class IrClickAction(
    val x: IrExpression,
    val y: IrExpression
) : IrAction

data class IrLongPressAction(
    val x: IrExpression,
    val y: IrExpression,
    val durationMs: IrExpression
) : IrAction

data class IrSwipeAction(
    val startX: IrExpression,
    val startY: IrExpression,
    val endX: IrExpression,
    val endY: IrExpression,
    val durationMs: IrExpression
) : IrAction

data class IrKeyboardInputAction(
    val text: IrExpression
) : IrAction

data class IrSleepAction(
    val durationMs: IrExpression
) : IrAction

data class IrLaunchAppAction(
    val packageName: IrExpression
) : IrAction

data class IrLogAction(
    val message: IrExpression
) : IrAction

data class IrImageFindAction(
    val imageUri: IrExpression,
    val confidence: IrExpression
) : IrAction

data class IrAssignAction(
    val variableName: String,
    val expression: IrExpression
) : IrAction

data object IrBackAction : IrAction

data object IrHomeAction : IrAction

data object IrStopRunningAction : IrAction

data class IrRepeatInstruction(
    val count: IrExpression,
    val body: List<IrInstruction>
) : IrControlFlow

data class IrForeverInstruction(
    val body: List<IrInstruction>
) : IrControlFlow

data class IrIfInstruction(
    val condition: IrExpression,
    val thenBranch: List<IrInstruction>,
    val elseBranch: List<IrInstruction>
) : IrControlFlow

data class IrNumberLiteral(
    val value: Double
) : IrExpression

data class IrBooleanLiteral(
    val value: Boolean
) : IrExpression

data class IrStringLiteral(
    val value: String
) : IrExpression

data class IrVariableReference(
    val name: String
) : IrExpression

data class IrUnaryExpression(
    val operator: UnaryOperator,
    val operand: IrExpression
) : IrExpression

data class IrBinaryExpression(
    val left: IrExpression,
    val operator: BinaryOperator,
    val right: IrExpression
) : IrExpression

data class IrMemberAccessExpression(
    val target: IrExpression,
    val propertyName: String
) : IrExpression

data class IrConversionExpression(
    val type: ConversionType,
    val value: IrExpression
) : IrExpression

data class IrImageFindExpression(
    val imageName: IrExpression,
    val confidence: IrExpression
) : IrExpression

data class IrTextRegion(
    val left: IrExpression,
    val top: IrExpression,
    val right: IrExpression,
    val bottom: IrExpression
)

data class IrTextFindExpression(
    val targetText: IrExpression,
    val region: IrTextRegion?
) : IrExpression

data class IrRegionTextRecognitionExpression(
    val region: IrTextRegion
) : IrExpression
