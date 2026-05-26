package com.lulucloud.touchscript.core.runtime

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.lulucloud.touchscript.core.automation.AccessibilityServiceRegistry
import com.lulucloud.touchscript.data.repository.FileScriptRepository
import com.lulucloud.touchscript.data.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc

data class ImageMatchResult(
    val found: Boolean,
    val x: Int,
    val y: Int,
    val score: Double
)

class ImageMatcher(
    private val context: Context,
    private val fileScriptRepository: FileScriptRepository,
    private val settingsRepository: SettingsRepository
) {
    suspend fun findOnScreen(imageReference: String, confidence: Double): ImageMatchResult = withContext(Dispatchers.Default) {
        require(confidence in 0.0..1.0) { "识图置信度必须在 0 到 1 之间" }

        OpenCvRuntime.ensureLoaded()

        val service = AccessibilityServiceRegistry.service.value
            ?: throw IllegalStateException("无障碍服务未连接，无法截图识图")
        val screenBitmap = service.takeScreenshotBitmap()
            ?: throw IllegalStateException("无法获取屏幕截图，请确认无障碍服务已允许截图")
        val templateBitmap = loadBitmap(imageReference)
            ?: throw IllegalArgumentException("无法读取识图图片：$imageReference")

        matchTemplate(screenBitmap, templateBitmap, confidence).also {
            screenBitmap.recycle()
            templateBitmap.recycle()
        }
    }

    private suspend fun loadBitmap(imageReference: String): Bitmap? {
        val imageUri = resolveImageUri(imageReference) ?: return null
        return context.contentResolver.openInputStream(imageUri).use { input ->
            input?.let(BitmapFactory::decodeStream)
        }
    }

    private suspend fun resolveImageUri(imageReference: String): Uri? {
        if (imageReference.startsWith("content://")) {
            return Uri.parse(imageReference)
        }

        val workspaceUri = settingsRepository.getSettings().scriptWorkspaceUri
            ?: throw IllegalStateException("尚未设置脚本目录，无法按文件名读取识图图片")
        return fileScriptRepository.resolveRecognitionImageUri(imageReference, workspaceUri)
    }

    private fun matchTemplate(
        screenBitmap: Bitmap,
        templateBitmap: Bitmap,
        confidence: Double
    ): ImageMatchResult {
        require(screenBitmap.width >= templateBitmap.width && screenBitmap.height >= templateBitmap.height) {
            "识图图片不能大于当前屏幕截图"
        }

        val screen = Mat()
        val template = Mat()
        val screenGray = Mat()
        val templateGray = Mat()
        val result = Mat()

        return try {
            Utils.bitmapToMat(screenBitmap, screen)
            Utils.bitmapToMat(templateBitmap, template)
            Imgproc.cvtColor(screen, screenGray, Imgproc.COLOR_RGBA2GRAY)
            Imgproc.cvtColor(template, templateGray, Imgproc.COLOR_RGBA2GRAY)

            val resultCols = screenGray.cols() - templateGray.cols() + 1
            val resultRows = screenGray.rows() - templateGray.rows() + 1
            result.create(resultRows, resultCols, CvType.CV_32FC1)
            Imgproc.matchTemplate(screenGray, templateGray, result, Imgproc.TM_CCOEFF_NORMED)

            val match = Core.minMaxLoc(result)
            val centerX = match.maxLoc.x + templateGray.cols() / 2.0
            val centerY = match.maxLoc.y + templateGray.rows() / 2.0
            ImageMatchResult(
                found = match.maxVal >= confidence,
                x = centerX.toInt(),
                y = centerY.toInt(),
                score = match.maxVal
            )
        } finally {
            screen.release()
            template.release()
            screenGray.release()
            templateGray.release()
            result.release()
        }
    }
}

private object OpenCvRuntime {
    @Volatile
    private var loaded = false

    fun ensureLoaded() {
        if (loaded) return
        synchronized(this) {
            if (!loaded) {
                System.loadLibrary("opencv_java4")
                loaded = true
            }
        }
    }
}
