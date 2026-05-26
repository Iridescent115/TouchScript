package com.lulucloud.touchscript.core.script

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScriptParserTest {

    @Test
    fun `parseDsl should build repeat and if nodes`() {
        val parser = ScriptParser()

        val result = parser.parseDsl(
            """
                设 次数 = 3
                循环 次数 次
                点击 540 1600
                键盘输入 "你好，触灵"
                等待 80
                结束循环
                如果 次数 > 0
                记录 "done"
                识图 "登录按钮.png" 0.85
                停止运行
                否则
                返回
                结束如果
            """.trimIndent()
        )

        assertEquals(3, result.statements.size)
        assertTrue(result.statements[0] is AssignActionNode)
        assertTrue(result.statements[1] is RepeatControlNode)
        assertTrue(result.statements[2] is IfControlNode)

        val repeatNode = result.statements[1] as RepeatControlNode
        assertEquals(3, repeatNode.body.size)
        assertTrue(repeatNode.body[1] is KeyboardInputActionNode)

        val ifNode = result.statements[2] as IfControlNode
        assertEquals(3, ifNode.thenBranch.size)
        assertEquals(1, ifNode.elseBranch.size)
        assertTrue(ifNode.thenBranch[1] is ImageFindActionNode)
        assertTrue(ifNode.thenBranch[2] is StopRunningActionNode)
    }

    @Test
    fun `parseDsl should support image find expression result`() {
        val parser = ScriptParser()

        val result = parser.parseDsl(
            """
                设 结果 = 识图 "登录按钮.png" 0.85
                如果 结果.找到
                点击 结果.x 结果.y
                结束如果
            """.trimIndent()
        )

        val assignNode = result.statements[0] as AssignActionNode
        assertTrue(assignNode.expression is ImageFindExpressionNode)

        val ifNode = result.statements[1] as IfControlNode
        assertTrue(ifNode.condition is MemberAccessExpressionNode)
        val clickNode = ifNode.thenBranch[0] as ClickActionNode
        assertTrue(clickNode.x is MemberAccessExpressionNode)
        assertTrue(clickNode.y is MemberAccessExpressionNode)
    }

    @Test
    fun `parseDsl should support text recognition expression result`() {
        val parser = ScriptParser()

        val result = parser.parseDsl(
            """
                设 文字1 = 识文字
                如果 文字1.找到
                记录 文字1.文本
                结束如果
            """.trimIndent()
        )

        val assignNode = result.statements[0] as AssignActionNode
        assertTrue(assignNode.expression is TextRecognitionExpressionNode)

        val ifNode = result.statements[1] as IfControlNode
        assertTrue(ifNode.condition is MemberAccessExpressionNode)
        assertTrue((ifNode.thenBranch[0] as LogActionNode).message is MemberAccessExpressionNode)
    }

    @Test
    fun `parseDsl should support forever loop`() {
        val parser = ScriptParser()

        val result = parser.parseDsl(
            """
                无限循环
                等待 1000
                结束循环
            """.trimIndent()
        )

        val foreverNode = result.statements[0] as ForeverControlNode
        assertEquals(1, foreverNode.body.size)
        assertTrue(foreverNode.body[0] is SleepActionNode)
    }
}
