package com.lulucloud.touchscript.core.runtime

import com.lulucloud.touchscript.core.automation.AutomationSessionManager
import com.lulucloud.touchscript.core.script.ScriptCompiler
import com.lulucloud.touchscript.data.repository.ScriptRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.luaj.vm2.lib.jse.JsePlatform

class ScriptRuntime(
    private val scriptRepository: ScriptRepository,
    private val scriptCompiler: ScriptCompiler,
    private val luaHostBridge: LuaHostBridge,
    private val sessionManager: AutomationSessionManager
) {

    suspend fun execute(scriptId: Long) {
        val script = scriptRepository.getScript(scriptId)
            ?: throw IllegalArgumentException("找不到 id=$scriptId 的脚本")
        val compilation = scriptCompiler.compile(script.source)
        execute(compilation.luaSource, script.name)
    }

    suspend fun execute(luaSource: String, scriptName: String = "临时脚本") {
        withContext(Dispatchers.Default) {
            sessionManager.appendLog("INFO", "Lua 运行时已加载：$scriptName")
            val globals = JsePlatform.standardGlobals()
            luaHostBridge.install(globals)
            globals.load(luaSource, scriptName).call()
        }
    }
}
