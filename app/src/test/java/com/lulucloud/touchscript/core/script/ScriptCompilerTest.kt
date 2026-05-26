package com.lulucloud.touchscript.core.script

import org.junit.Assert.assertTrue
import org.junit.Test

class ScriptCompilerTest {

    @Test
    fun `compile should generate expected lua host calls`() {
        val compiler = ScriptCompiler()

        val result = compiler.compile(
            """
                设 次数 = 2
                循环 次数 次
                点击 100 200
                等待 50
                结束循环
                如果 次数 == 2
                启动应用 "com.android.settings"
                识图 "登录按钮.png" 0.9
                结束如果
            """.trimIndent()
        )

        assertTrue(result.luaSource.contains("""vars["次数"] = 2"""))
        assertTrue(result.luaSource.contains("touch.click(100, 200)"))
        assertTrue(result.luaSource.contains("device.sleep(50)"))
        assertTrue(result.luaSource.contains("""app.launch("com.android.settings")"""))
        assertTrue(result.luaSource.contains("""image.requireFind("登录按钮.png", 0.9)"""))
        assertTrue(result.luaSource.contains("for __index = 1, vars[\"次数\"] do"))
    }

    @Test
    fun `compile should support image find result assignment and member access`() {
        val compiler = ScriptCompiler()

        val result = compiler.compile(
            """
                设 结果 = 识图 "登录按钮.png" 0.85
                如果 结果.找到
                点击 结果.x 结果.y
                记录 结果.置信度
                结束如果
            """.trimIndent()
        )

        assertTrue(result.luaSource.contains("""vars["结果"] = image.find("登录按钮.png", 0.85)"""))
        assertTrue(result.luaSource.contains("""if __member(vars["结果"], "found") then"""))
        assertTrue(result.luaSource.contains("""touch.click(__member(vars["结果"], "x"), __member(vars["结果"], "y"))"""))
        assertTrue(result.luaSource.contains("""log.info(__member(vars["结果"], "score"))"""))
    }

    @Test
    fun `compile should generate while true for forever loop`() {
        val compiler = ScriptCompiler()

        val result = compiler.compile(
            """
                无限循环
                等待 1000
                结束循环
            """.trimIndent()
        )

        assertTrue(result.luaSource.contains("while true do"))
        assertTrue(result.luaSource.contains("device.sleep(1000)"))
    }
}
