package com.lulucloud.touchscript.data.repository

import android.content.Context
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class LocalScriptFile(
    val name: String,
    val fileName: String,
    val absolutePath: String,
    val content: String,
    val isTemplate: Boolean,
    val updatedAt: Long
)

class FileScriptRepository(
    private val context: Context
) {
    private val scriptsDir = File(context.filesDir, "scripts")
    private val templatesDir = File(context.filesDir, "templates")

    suspend fun ensureSeedFiles() = withContext(Dispatchers.IO) {
        scriptsDir.mkdirs()
        templatesDir.mkdirs()

        ensureFile(
            File(scriptsDir, "欢迎脚本.tscript"),
            """
                记录 "开始执行欢迎脚本"
                设 次数 = 3
                循环 次数 次
                点击 540 1600
                等待 300
                结束循环
                记录 "执行完成"
            """.trimIndent()
        )

        ensureFile(
            File(templatesDir, "基础连点模板.tscript"),
            """
                设 次数 = 10
                循环 次数 次
                点击 540 1600
                等待 80
                结束循环
            """.trimIndent()
        )

        ensureFile(
            File(templatesDir, "滑动模板.tscript"),
            """
                记录 "开始滑动"
                滑动 540 1500 540 500 260
                等待 1200
                滑动 540 500 540 1500 260
            """.trimIndent()
        )

        ensureFile(
            File(templatesDir, "启动应用模板.tscript"),
            """
                启动应用 "com.android.settings"
                等待 1200
                返回
            """.trimIndent()
        )
    }

    suspend fun listUserScripts(): List<LocalScriptFile> = listFilesFrom(scriptsDir, isTemplate = false)

    suspend fun listTemplates(): List<LocalScriptFile> = listFilesFrom(templatesDir, isTemplate = true)

    suspend fun listAllScripts(): List<LocalScriptFile> = withContext(Dispatchers.IO) {
        (listUserScripts() + listTemplates()).sortedBy { it.name.lowercase() }
    }

    suspend fun readFile(path: String): LocalScriptFile = withContext(Dispatchers.IO) {
        val file = File(path)
        require(file.exists()) { "文件不存在：$path" }
        LocalScriptFile(
            name = file.nameWithoutExtension,
            fileName = file.name,
            absolutePath = file.absolutePath,
            content = file.readText(Charsets.UTF_8),
            isTemplate = file.parentFile?.absolutePath == templatesDir.absolutePath,
            updatedAt = file.lastModified()
        )
    }

    suspend fun saveScript(
        fileNameWithoutExtension: String,
        content: String,
        path: String? = null,
        isTemplate: Boolean = false
    ): LocalScriptFile = withContext(Dispatchers.IO) {
        val normalizedFileName = normalizeFileName(fileNameWithoutExtension)
        val targetFile = if (path.isNullOrBlank()) {
            File(if (isTemplate) templatesDir else scriptsDir, "$normalizedFileName.tscript")
        } else {
            File(path)
        }
        targetFile.parentFile?.mkdirs()
        targetFile.writeText(content, Charsets.UTF_8)
        readFile(targetFile.absolutePath)
    }

    suspend fun duplicateScript(sourcePath: String, newName: String, isTemplate: Boolean = false): LocalScriptFile {
        val source = readFile(sourcePath)
        return saveScript(newName, source.content, path = null, isTemplate = isTemplate)
    }

    private suspend fun listFilesFrom(directory: File, isTemplate: Boolean): List<LocalScriptFile> =
        withContext(Dispatchers.IO) {
            directory.mkdirs()
            directory.listFiles()
                .orEmpty()
                .filter { it.isFile }
                .sortedByDescending { it.lastModified() }
                .map { file ->
                    LocalScriptFile(
                        name = file.nameWithoutExtension,
                        fileName = file.name,
                        absolutePath = file.absolutePath,
                        content = file.readText(Charsets.UTF_8),
                        isTemplate = isTemplate,
                        updatedAt = file.lastModified()
                    )
                }
        }

    private fun ensureFile(file: File, content: String) {
        if (!file.exists()) {
            file.parentFile?.mkdirs()
            file.writeText(content, Charsets.UTF_8)
        }
    }

    private fun normalizeFileName(name: String): String {
        return name.trim()
            .ifBlank { "未命名脚本" }
            .replace(Regex("[\\\\/:*?\"<>|]"), "_")
    }
}
