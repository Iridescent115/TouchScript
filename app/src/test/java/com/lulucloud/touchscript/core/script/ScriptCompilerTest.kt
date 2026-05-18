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
                结束如果
            """.trimIndent()
        )

        assertTrue(result.luaSource.contains("""vars["次数"] = 2"""))
        assertTrue(result.luaSource.contains("touch.click(100, 200)"))
        assertTrue(result.luaSource.contains("device.sleep(50)"))
        assertTrue(result.luaSource.contains("""app.launch("com.android.settings")"""))
        assertTrue(result.luaSource.contains("for __index = 1, vars[\"次数\"] do"))
    }
}
