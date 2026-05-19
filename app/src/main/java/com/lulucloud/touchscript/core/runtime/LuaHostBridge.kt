package com.lulucloud.touchscript.core.runtime

import com.lulucloud.touchscript.core.automation.AutomationAction
import com.lulucloud.touchscript.core.automation.AutomationExecutor
import com.lulucloud.touchscript.core.automation.AutomationSessionManager
import com.lulucloud.touchscript.core.automation.BackAction
import com.lulucloud.touchscript.core.automation.ClickAction
import com.lulucloud.touchscript.core.automation.HomeAction
import com.lulucloud.touchscript.core.automation.LaunchAppAction
import com.lulucloud.touchscript.core.automation.LongPressAction
import com.lulucloud.touchscript.core.automation.SleepAction
import com.lulucloud.touchscript.core.automation.SwipeAction
import kotlin.math.roundToInt
import kotlinx.coroutines.runBlocking
import org.luaj.vm2.Globals
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.Varargs
import org.luaj.vm2.lib.VarArgFunction

class LuaHostBridge(
    private val automationExecutor: AutomationExecutor,
    private val sessionManager: AutomationSessionManager
) {

    fun install(globals: Globals) {
        globals.set("touch", touchTable())
        globals.set("device", deviceTable())
        globals.set("app", appTable())
        globals.set("log", logTable())
    }

    private fun touchTable(): LuaTable = LuaTable().apply {
        set("click", function { args ->
            runAction(
                ClickAction(
                    x = args.checkdouble(1).roundToInt(),
                    y = args.checkdouble(2).roundToInt()
                )
            )
        })
        set("longPress", function { args ->
            runAction(
                LongPressAction(
                    x = args.checkdouble(1).roundToInt(),
                    y = args.checkdouble(2).roundToInt(),
                    durationMs = args.checklong(3)
                )
            )
        })
        set("swipe", function { args ->
            runAction(
                SwipeAction(
                    startX = args.checkdouble(1).roundToInt(),
                    startY = args.checkdouble(2).roundToInt(),
                    endX = args.checkdouble(3).roundToInt(),
                    endY = args.checkdouble(4).roundToInt(),
                    durationMs = args.checklong(5)
                )
            )
        })
    }

    private fun deviceTable(): LuaTable = LuaTable().apply {
        set("sleep", function { args ->
            runAction(SleepAction(args.checklong(1)))
        })
        set("back", zeroArgFunction { runAction(BackAction) })
        set("home", zeroArgFunction { runAction(HomeAction) })
    }

    private fun appTable(): LuaTable = LuaTable().apply {
        set("launch", function { args ->
            runAction(LaunchAppAction(args.checkjstring(1)))
        })
    }

    private fun logTable(): LuaTable = LuaTable().apply {
        set("info", function { args ->
            runBlocking {
                sessionManager.ensureNotStopped()
                sessionManager.appendLog("INFO", args.checkjstring(1))
            }
            LuaValue.NIL
        })
    }

    private fun function(block: (Varargs) -> LuaValue): VarArgFunction = object : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs = block(args)
    }

    private fun zeroArgFunction(block: () -> LuaValue): VarArgFunction = object : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs = block()
    }

    private fun runAction(action: AutomationAction): LuaValue {
        val result = runBlocking {
            sessionManager.ensureNotStopped()
            sessionManager.awaitIfPaused()
            sessionManager.ensureNotStopped()
            val actionName = action::class.simpleName ?: action.javaClass.simpleName
            sessionManager.appendLog("INFO", "执行动作：$actionName")
            automationExecutor.perform(action)
        }
        if (!result.success) {
            throw IllegalStateException(result.message ?: "动作执行失败")
        }
        return LuaValue.NIL
    }
}
