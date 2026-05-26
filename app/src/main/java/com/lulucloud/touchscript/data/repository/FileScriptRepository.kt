package com.lulucloud.touchscript.data.repository

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
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

    suspend fun ensureSeedFiles() = withContext(Dispatchers.IO) {
        scriptsDir.mkdirs()

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
    }

    suspend fun listUserScripts(): List<LocalScriptFile> = listFilesFrom(scriptsDir, isTemplate = false)

    suspend fun readFile(path: String): LocalScriptFile = withContext(Dispatchers.IO) {
        if (isContentUri(path)) {
            readUri(Uri.parse(path))
        } else {
            readLocalFile(path)
        }
    }

    suspend fun saveScript(
        fileNameWithoutExtension: String,
        content: String,
        path: String? = null,
        isTemplate: Boolean = false
    ): LocalScriptFile = withContext(Dispatchers.IO) {
        when {
            path.isNullOrBlank() -> {
                val normalizedFileName = normalizeFileName(fileNameWithoutExtension)
                val targetFile = File(scriptsDir, "$normalizedFileName.tscript")
                targetFile.parentFile?.mkdirs()
                targetFile.writeText(content, Charsets.UTF_8)
                readFile(targetFile.absolutePath)
            }

            isContentUri(path) -> {
                val uri = Uri.parse(path)
                context.contentResolver.openOutputStream(uri, "wt")?.use { output ->
                    output.write(content.toByteArray(Charsets.UTF_8))
                } ?: error("无法写入脚本文件")
                readFile(path)
            }

            else -> {
                val targetFile = File(path)
                targetFile.parentFile?.mkdirs()
                targetFile.writeText(content, Charsets.UTF_8)
                readFile(targetFile.absolutePath)
            }
        }
    }

    suspend fun saveScriptToWorkspace(
        fileNameWithoutExtension: String,
        content: String,
        workspaceUri: String
    ): LocalScriptFile = withContext(Dispatchers.IO) {
        val normalizedFileName = normalizeFileName(fileNameWithoutExtension)
        val fileName = "$normalizedFileName.tscript"
        val workspaceDocumentUri = Uri.parse(workspaceUri)
        val targetUri = findChildByName(
            parentDirectoryUri = workspaceDocumentUri,
            targetName = fileName,
            targetMimeType = "text/plain"
        ) ?: DocumentsContract.createDocument(
            context.contentResolver,
            workspaceDocumentUri,
            "text/plain",
            fileName
        ) ?: error("无法创建脚本文件：$fileName")

        context.contentResolver.openOutputStream(targetUri, "wt")?.use { output ->
            output.write(content.toByteArray(Charsets.UTF_8))
        } ?: error("无法写入脚本文件")

        readFile(targetUri.toString())
    }

    suspend fun ensureScriptWorkspace(parentTreeUri: String): String = withContext(Dispatchers.IO) {
        val treeUri = Uri.parse(parentTreeUri)
        val rootDocumentId = DocumentsContract.getTreeDocumentId(treeUri)
        val rootDocumentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, rootDocumentId)
        val rootName = queryDisplayName(rootDocumentUri)

        val scriptWorkspaceUri = if (rootName == SCRIPT_WORKSPACE_DIR_NAME) {
            rootDocumentUri
        } else {
            ensureDirectory(rootDocumentUri, SCRIPT_WORKSPACE_DIR_NAME)
        }

        scriptWorkspaceUri.toString()
    }

    suspend fun importRecognitionImage(
        sourceLocation: String,
        workspaceUri: String,
        preferredFileName: String? = null
    ): String = withContext(Dispatchers.IO) {
        val sourceUri = Uri.parse(sourceLocation)
        val sourceName = queryDisplayName(sourceUri)
        val mimeType = context.contentResolver.getType(sourceUri) ?: "image/*"
        val fileName = normalizeImageFileName(
            preferredFileName ?: sourceName ?: "识图_${System.currentTimeMillis()}.${extensionFromMimeType(mimeType)}"
        )
        val imagesDirectoryUri = ensureRecognitionImagesDirectoryUri(workspaceUri)
        val targetUri = findChildByName(
            parentDirectoryUri = imagesDirectoryUri,
            targetName = fileName,
            targetMimeType = null
        ) ?: DocumentsContract.createDocument(
            context.contentResolver,
            imagesDirectoryUri,
            mimeType,
            fileName
        ) ?: error("无法创建识图图片：$fileName")

        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            context.contentResolver.openOutputStream(targetUri, "wt")?.use { output ->
                input.copyTo(output)
            } ?: error("无法写入识图图片：$fileName")
        } ?: error("无法读取选择的图片")

        fileName
    }

    suspend fun ensureRecognitionImagesDirectory(workspaceUri: String): String = withContext(Dispatchers.IO) {
        ensureRecognitionImagesDirectoryUri(workspaceUri).toString()
    }

    suspend fun resolveRecognitionImageUri(imageName: String, workspaceUri: String): Uri? = withContext(Dispatchers.IO) {
        val normalizedName = normalizeImageFileName(imageName)
        val workspaceDocumentUri = Uri.parse(workspaceUri)
        val imagesDirectoryUri = findChildByName(
            parentDirectoryUri = workspaceDocumentUri,
            targetName = RECOGNITION_IMAGES_DIR_NAME,
            targetMimeType = DocumentsContract.Document.MIME_TYPE_DIR
        ) ?: return@withContext null

        findChildByName(
            parentDirectoryUri = imagesDirectoryUri,
            targetName = normalizedName,
            targetMimeType = null
        )
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

    private fun readLocalFile(path: String): LocalScriptFile {
        val file = File(path)
        require(file.exists()) { "文件不存在：$path" }
        return LocalScriptFile(
            name = file.nameWithoutExtension,
            fileName = file.name,
            absolutePath = file.absolutePath,
            content = file.readText(Charsets.UTF_8),
            isTemplate = false,
            updatedAt = file.lastModified()
        )
    }

    private fun readUri(uri: Uri): LocalScriptFile {
        val fileName = queryDisplayName(uri) ?: "未命名脚本.tscript"
        val content = context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use {
            it.readText()
        } ?: error("无法读取脚本文件")
        return LocalScriptFile(
            name = fileName.substringBeforeLast('.'),
            fileName = fileName,
            absolutePath = uri.toString(),
            content = content,
            isTemplate = false,
            updatedAt = queryLastModified(uri) ?: System.currentTimeMillis()
        )
    }

    private fun queryDisplayName(uri: Uri): String? {
        return context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
        }
    }

    private fun queryLastModified(uri: Uri): Long? {
        return runCatching {
            context.contentResolver.query(
                uri,
                arrayOf(DocumentsContract.Document.COLUMN_LAST_MODIFIED),
                null,
                null,
                null
            )?.use { cursor ->
                val index = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
                if (index >= 0 && cursor.moveToFirst()) cursor.getLong(index) else null
            }
        }.getOrNull()
    }

    private fun ensureDirectory(parentDirectoryUri: Uri, targetName: String): Uri {
        findChildByName(
            parentDirectoryUri = parentDirectoryUri,
            targetName = targetName,
            targetMimeType = DocumentsContract.Document.MIME_TYPE_DIR
        )?.let { return it }

        return DocumentsContract.createDocument(
            context.contentResolver,
            parentDirectoryUri,
            DocumentsContract.Document.MIME_TYPE_DIR,
            targetName
        ) ?: error("无法创建目录：$targetName")
    }

    private fun ensureRecognitionImagesDirectoryUri(workspaceUri: String): Uri {
        return ensureDirectory(Uri.parse(workspaceUri), RECOGNITION_IMAGES_DIR_NAME)
    }

    private fun findChildByName(
        parentDirectoryUri: Uri,
        targetName: String,
        targetMimeType: String?
    ): Uri? {
        val parentDocumentId = DocumentsContract.getDocumentId(parentDirectoryUri)
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(parentDirectoryUri, parentDocumentId)
        return context.contentResolver.query(
            childrenUri,
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE
            ),
            null,
            null,
            null
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val mimeIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
            while (cursor.moveToNext()) {
                val displayName = if (nameIndex >= 0) cursor.getString(nameIndex) else null
                val mimeType = if (mimeIndex >= 0) cursor.getString(mimeIndex) else null
                if (displayName == targetName && (targetMimeType == null || mimeType == targetMimeType)) {
                    val documentId = cursor.getString(idIndex)
                    return DocumentsContract.buildDocumentUriUsingTree(parentDirectoryUri, documentId)
                }
            }
            null
        }
    }

    private fun ensureFile(file: File, content: String) {
        if (!file.exists()) {
            file.parentFile?.mkdirs()
            file.writeText(content, Charsets.UTF_8)
        }
    }

    private fun normalizeFileName(name: String): String {
        return stripScriptExtensionSuffixes(name.trim())
            .ifBlank { "未命名脚本" }
            .replace(Regex("[\\\\/:*?\"<>|]"), "_")
    }

    private fun normalizeImageFileName(name: String): String {
        return name.trim()
            .substringAfterLast('/')
            .substringAfterLast('\\')
            .ifBlank { "未命名图片.png" }
            .replace(Regex("[\\\\/:*?\"<>|]"), "_")
    }

    private fun extensionFromMimeType(mimeType: String): String {
        return MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)?.takeIf { it.isNotBlank() } ?: "png"
    }

    private fun isContentUri(path: String): Boolean = path.startsWith("content://")

    private companion object {
        const val SCRIPT_WORKSPACE_DIR_NAME = "TouchScript"
        const val RECOGNITION_IMAGES_DIR_NAME = "Images"
    }
}

private fun stripScriptExtensionSuffixes(name: String): String {
    var result = name
    while (result.endsWith(".tscript", ignoreCase = true)) {
        result = result.dropLast(".tscript".length)
    }
    return result
}
