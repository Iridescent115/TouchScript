package com.lulucloud.touchscript.app

import android.app.Application
import androidx.room.Room
import com.lulucloud.touchscript.core.automation.AccessibilityAutomationExecutor
import com.lulucloud.touchscript.core.automation.AutomationSessionManager
import com.lulucloud.touchscript.core.runtime.LuaHostBridge
import com.lulucloud.touchscript.core.runtime.ScriptRuntime
import com.lulucloud.touchscript.core.script.ScriptCompiler
import com.lulucloud.touchscript.data.local.TouchWorkshopDatabase
import com.lulucloud.touchscript.data.repository.FileScriptRepository
import com.lulucloud.touchscript.data.repository.ScriptRepository
import com.lulucloud.touchscript.data.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AppContainer(
    application: Application
) {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database: TouchWorkshopDatabase = Room.databaseBuilder(
        application,
        TouchWorkshopDatabase::class.java,
        "touch_workshop.db"
    ).build()

    val settingsRepository = SettingsRepository(application)
    val fileScriptRepository = FileScriptRepository(application)
    val scriptRepository = ScriptRepository(
        scriptDao = database.scriptDao(),
        templateDao = database.scriptTemplateDao(),
        runRecordDao = database.runRecordDao()
    )
    val sessionManager = AutomationSessionManager(scriptRepository)
    val scriptCompiler = ScriptCompiler()
    val automationExecutor = AccessibilityAutomationExecutor(application)
    val scriptRuntime = ScriptRuntime(
        scriptRepository = scriptRepository,
        scriptCompiler = scriptCompiler,
        luaHostBridge = LuaHostBridge(automationExecutor, sessionManager),
        sessionManager = sessionManager
    )

    init {
        applicationScope.launch {
            scriptRepository.ensureSeedData()
            fileScriptRepository.ensureSeedFiles()
        }
    }
}
