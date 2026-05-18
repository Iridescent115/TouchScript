package com.lulucloud.touchscript.data.repository

import com.lulucloud.touchscript.data.local.RunRecordDao
import com.lulucloud.touchscript.data.local.RunRecordEntity
import com.lulucloud.touchscript.data.local.ScriptDao
import com.lulucloud.touchscript.data.local.ScriptEntity
import com.lulucloud.touchscript.data.local.ScriptTemplateDao
import com.lulucloud.touchscript.data.local.ScriptTemplateEntity
import kotlinx.coroutines.flow.Flow

class ScriptRepository(
    private val scriptDao: ScriptDao,
    private val templateDao: ScriptTemplateDao,
    private val runRecordDao: RunRecordDao
) {

    fun observeScripts(): Flow<List<ScriptEntity>> = scriptDao.observeAll()

    fun observeTemplates(): Flow<List<ScriptTemplateEntity>> = templateDao.observeAll()

    fun observeRecentRuns(limit: Int = 20): Flow<List<RunRecordEntity>> = runRecordDao.observeRecent(limit)

    suspend fun getScript(scriptId: Long): ScriptEntity? = scriptDao.getById(scriptId)

    suspend fun saveScript(id: Long?, name: String, source: String): Long {
        val now = System.currentTimeMillis()
        return if (id == null || id == 0L) {
            scriptDao.insert(
                ScriptEntity(
                    name = name,
                    source = source,
                    updatedAt = now
                )
            )
        } else {
            scriptDao.update(
                ScriptEntity(
                    id = id,
                    name = name,
                    source = source,
                    updatedAt = now
                )
            )
            id
        }
    }

    suspend fun addRunRecord(
        scriptName: String,
        status: String,
        summary: String,
        startedAt: Long,
        endedAt: Long
    ) {
        runRecordDao.insert(
            RunRecordEntity(
                scriptName = scriptName,
                status = status,
                summary = summary,
                startedAt = startedAt,
                endedAt = endedAt
            )
        )
    }

    suspend fun ensureSeedData() {
        if (templateDao.count() == 0) {
            templateDao.insertAll(defaultTemplates)
        }

        if (scriptDao.count() == 0) {
            saveScript(
                id = null,
                name = "欢迎脚本",
                source = """
                    记录 "开始执行欢迎脚本"
                    设 次数 = 3
                    循环 次数 次
                    记录 "执行一次点击"
                    点击 540 1600
                    等待 300
                    结束循环
                    如果 次数 > 0
                    记录 "脚本执行完成"
                    结束如果
                """.trimIndent()
            )
        }
    }

    private val defaultTemplates = listOf(
        ScriptTemplateEntity(
            name = "基础连点",
            description = "最简单的固定坐标点击循环。",
            source = """
                设 次数 = 10
                循环 次数 次
                点击 540 1600
                等待 80
                结束循环
            """.trimIndent()
        ),
        ScriptTemplateEntity(
            name = "滑动示例",
            description = "上下滑动页面的基础模板。",
            source = """
                记录 "开始滑动"
                滑动 540 1500 540 500 260
                等待 1200
                滑动 540 500 540 1500 260
            """.trimIndent()
        ),
        ScriptTemplateEntity(
            name = "启动并返回",
            description = "演示应用启动与系统返回动作。",
            source = """
                启动应用 "com.android.settings"
                等待 1200
                返回
            """.trimIndent()
        )
    )
}
