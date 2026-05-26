package com.lulucloud.touchscript.core.runtime

import com.lulucloud.touchscript.core.automation.AutomationAction
import com.lulucloud.touchscript.core.automation.AutomationExecutor
import com.lulucloud.touchscript.core.automation.AutomationSessionManager
import com.lulucloud.touchscript.core.automation.BackAction
import com.lulucloud.touchscript.core.automation.ClickAction
import com.lulucloud.touchscript.core.automation.HomeAction
import com.lulucloud.touchscript.core.automation.KeyboardInputAction
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
    private val sessionManager: AutomationSessionManager,
    private val imageMatcher: ImageMatcher? = null,
    private val textRecognizerEngine: TextRecognizerEngine? = null
) {

    fun install(globals: Globals) {
        globals.set("touch", touchTable())
        globals.set("keyboard", keyboardTable())
        globals.set("device", deviceTable())
        globals.set("app", appTable())
        globals.set("log", logTable())
        globals.set("image", imageTable())
        globals.set("ocr", ocrTable())
        globals.set("runtime", runtimeTable())
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

    private fun keyboardTable(): LuaTable = LuaTable().apply {
        set("input", function { args ->
            runAction(KeyboardInputAction(args.arg(1).tojstring()))
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
                sessionManager.appendLog("INFO", args.arg(1).tojstring())
            }
            LuaValue.NIL
        })
    }

    private fun runtimeTable(): LuaTable = LuaTable().apply {
        set("stop", zeroArgFunction {
            runBlocking {
                sessionManager.appendLog("INFO", "脚本请求停止运行")
                sessionManager.requestStop("脚本主动停止运行")
                sessionManager.ensureNotStopped()
            }
            LuaValue.NIL
        })
    }

    private fun imageTable(): LuaTable = LuaTable().apply {
        set("find", function { args ->
            runImageFind(args, failWhenMissing = false)
        })
        set("requireFind", function { args ->
            runImageFind(args, failWhenMissing = true)
        })
    }

    private fun ocrTable(): LuaTable = LuaTable().apply {
        set("recognize", zeroArgFunction {
            val recognizer = textRecognizerEngine ?: throw IllegalStateException("识文字服务未初始化")
            val result = runBlocking {
                sessionManager.ensureNotStopped()
                sessionManager.awaitIfPaused()
                sessionManager.ensureNotStopped()
                sessionManager.appendLog("INFO", "开始识文字")
                recognizer.recognizeScreenText()
            }
            runBlocking {
                sessionManager.appendLog(
                    if (result.found) "INFO" else "ERROR",
                    "识文字${if (result.found) "成功" else "未发现文字"}：${result.lineCount} 行"
                )
            }
            LuaTable().apply {
                set("found", LuaValue.valueOf(result.found))
                set("text", LuaValue.valueOf(result.text))
                set("lineCount", LuaValue.valueOf(result.lineCount))
            }
        })
    }

    private fun runImageFind(args: Varargs, failWhenMissing: Boolean): LuaValue {
        val matcher = imageMatcher ?: throw IllegalStateException("识图服务未初始化")
        val imageUri = args.checkjstring(1)
        val confidence = args.checkdouble(2)
        val result = runBlocking {
            sessionManager.ensureNotStopped()
            sessionManager.awaitIfPaused()
            sessionManager.ensureNotStopped()
            sessionManager.appendLog("INFO", "开始识图：置信度 >= $confidence")
            matcher.findOnScreen(imageUri, confidence)
        }
        runBlocking {
            sessionManager.appendLog(
                if (result.found) "INFO" else "ERROR",
                "识图${if (result.found) "成功" else "失败"}：score=${"%.4f".format(result.score)}，位置=(${result.x}, ${result.y})"
            )
        }
        if (!result.found) {
            if (failWhenMissing) {
                throw IllegalStateException("识图未达到置信度：score=${"%.4f".format(result.score)}，threshold=$confidence")
            }
        }
        return LuaTable().apply {
            set("found", LuaValue.valueOf(result.found))
            set("x", LuaValue.valueOf(result.x))
            set("y", LuaValue.valueOf(result.y))
            set("score", LuaValue.valueOf(result.score))
        }
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
