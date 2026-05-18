package com.lulucloud.touchscript.core.runtime

import com.lulucloud.touchscript.core.automation.AutomationAction
import com.lulucloud.touchscript.core.automation.AutomationExecutor
import com.lulucloud.touchscript.core.automation.AutomationResult
import com.lulucloud.touchscript.core.automation.AutomationSessionManager
import com.lulucloud.touchscript.core.script.ScriptCompiler
import com.lulucloud.touchscript.data.local.RunRecordDao
import com.lulucloud.touchscript.data.local.RunRecordEntity
import com.lulucloud.touchscript.data.local.ScriptDao
import com.lulucloud.touchscript.data.local.ScriptEntity
import com.lulucloud.touchscript.data.local.ScriptTemplateDao
import com.lulucloud.touchscript.data.local.ScriptTemplateEntity
import com.lulucloud.touchscript.data.repository.ScriptRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class ScriptRuntimeTest {

    @Test
    fun `execute should route lua host calls to automation executor`() = runBlocking {
        val executor = RecordingAutomationExecutor()
        val repository = ScriptRepository(
            scriptDao = FakeScriptDao(),
            templateDao = FakeTemplateDao(),
            runRecordDao = FakeRunRecordDao()
        )
        val sessionManager = AutomationSessionManager(repository)
        val runtime = ScriptRuntime(
            scriptRepository = repository,
            scriptCompiler = ScriptCompiler(),
            luaHostBridge = LuaHostBridge(executor, sessionManager),
            sessionManager = sessionManager
        )

        sessionManager.start("运行时测试")
        runtime.execute(
            """
                touch.click(10, 20)
                device.sleep(30)
                app.launch("com.demo.app")
                device.back()
            """.trimIndent(),
            scriptName = "运行时测试"
        )

        assertEquals(4, executor.actions.size)
        assertEquals("ClickAction", executor.actions[0]::class.simpleName)
        assertEquals("SleepAction", executor.actions[1]::class.simpleName)
        assertEquals("LaunchAppAction", executor.actions[2]::class.simpleName)
        assertEquals("BackAction", executor.actions[3]::class.simpleName)
    }
}

private class RecordingAutomationExecutor : AutomationExecutor {
    val actions = mutableListOf<AutomationAction>()

    override suspend fun perform(action: AutomationAction): AutomationResult {
        actions += action
        return AutomationResult(success = true)
    }
}

private class FakeScriptDao : ScriptDao {
    private val scripts = MutableStateFlow<List<ScriptEntity>>(emptyList())

    override fun observeAll(): Flow<List<ScriptEntity>> = scripts

    override suspend fun count(): Int = scripts.value.size

    override suspend fun getById(id: Long): ScriptEntity? = scripts.value.firstOrNull { it.id == id }

    override suspend fun insert(entity: ScriptEntity): Long {
        val nextId = (scripts.value.maxOfOrNull { it.id } ?: 0L) + 1
        scripts.value = scripts.value + entity.copy(id = nextId)
        return nextId
    }

    override suspend fun update(entity: ScriptEntity) {
        scripts.value = scripts.value.map { if (it.id == entity.id) entity else it }
    }
}

private class FakeTemplateDao : ScriptTemplateDao {
    private val templates = MutableStateFlow<List<ScriptTemplateEntity>>(emptyList())

    override fun observeAll(): Flow<List<ScriptTemplateEntity>> = templates

    override suspend fun count(): Int = templates.value.size

    override suspend fun insertAll(entities: List<ScriptTemplateEntity>) {
        templates.value = entities
    }
}

private class FakeRunRecordDao : RunRecordDao {
    private val runs = MutableStateFlow<List<RunRecordEntity>>(emptyList())

    override fun observeRecent(limit: Int): Flow<List<RunRecordEntity>> = runs

    override suspend fun insert(entity: RunRecordEntity): Long {
        val nextId = (runs.value.maxOfOrNull { it.id } ?: 0L) + 1
        runs.value = runs.value + entity.copy(id = nextId)
        return nextId
    }
}
