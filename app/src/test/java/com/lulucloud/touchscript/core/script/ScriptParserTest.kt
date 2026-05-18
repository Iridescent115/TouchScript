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
                等待 80
                结束循环
                如果 次数 > 0
                记录 "done"
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
        assertEquals(2, repeatNode.body.size)

        val ifNode = result.statements[2] as IfControlNode
        assertEquals(1, ifNode.thenBranch.size)
        assertEquals(1, ifNode.elseBranch.size)
    }
}
