package com.lulucloud.touchscript.core.runtime

import android.graphics.Bitmap
import android.graphics.Rect
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.lulucloud.touchscript.core.automation.AccessibilityServiceRegistry
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

data class TextRecognitionResult(
    val found: Boolean,
    val text: String,
    val lineCount: Int
)

data class TextFindResult(
    val found: Boolean,
    val text: String,
    val x: Int,
    val y: Int,
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
)

data class TextRecognitionRegion(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
)

class TextRecognizerEngine {
    private val recognizer by lazy {
        TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
    }

    suspend fun findTextOnScreen(
        targetText: String,
        region: TextRecognitionRegion?
    ): TextFindResult {
        require(targetText.isNotBlank()) { "查找文字不能为空" }

        val screenBitmap = captureScreenBitmap()
        return try {
            val (bitmap, offsetX, offsetY) = cropIfNeeded(screenBitmap, region)
            try {
                val visionText = recognize(bitmap)
                val matchedLine = visionText.textBlocks
                    .flatMap { block -> block.lines }
                    .firstOrNull { line -> line.text.contains(targetText) }
                matchedLine?.toFindResult(offsetX, offsetY) ?: TextFindResult(
                    found = false,
                    text = "",
                    x = -1,
                    y = -1,
                    left = -1,
                    top = -1,
                    right = -1,
                    bottom = -1
                )
            } finally {
                if (bitmap !== screenBitmap) {
                    bitmap.recycle()
                }
            }
        } finally {
            screenBitmap.recycle()
        }
    }

    suspend fun recognizeRegion(region: TextRecognitionRegion): TextRecognitionResult {
        val screenBitmap = captureScreenBitmap()
        return try {
            val (bitmap, _, _) = cropIfNeeded(screenBitmap, region)
            try {
                val visionText = recognize(bitmap)
                val lines = visionText.textBlocks.flatMap { block -> block.lines }
                val text = visionText.text.trim()
                TextRecognitionResult(
                    found = text.isNotBlank(),
                    text = text,
                    lineCount = lines.size
                )
            } finally {
                if (bitmap !== screenBitmap) {
                    bitmap.recycle()
                }
            }
        } finally {
            screenBitmap.recycle()
        }
    }

    private suspend fun captureScreenBitmap(): Bitmap {
        val service = AccessibilityServiceRegistry.service.value
            ?: throw IllegalStateException("无障碍服务未连接，无法截图识别文字")
        return service.takeScreenshotBitmap()
            ?: throw IllegalStateException("无法获取屏幕截图，请确认无障碍服务已允许截图")
    }

    private suspend fun recognize(bitmap: Bitmap): Text {
        return recognizer.process(InputImage.fromBitmap(bitmap, 0)).await()
    }

    private fun cropIfNeeded(
        source: Bitmap,
        region: TextRecognitionRegion?
    ): Triple<Bitmap, Int, Int> {
        if (region == null) {
            return Triple(source, 0, 0)
        }

        val safeLeft = region.left.coerceIn(0, source.width - 1)
        val safeTop = region.top.coerceIn(0, source.height - 1)
        val safeRight = region.right.coerceIn(safeLeft + 1, source.width)
        val safeBottom = region.bottom.coerceIn(safeTop + 1, source.height)
        val bitmap = Bitmap.createBitmap(
            source,
            safeLeft,
            safeTop,
            safeRight - safeLeft,
            safeBottom - safeTop
        )
        return Triple(bitmap, safeLeft, safeTop)
    }

    private fun Text.Line.toFindResult(offsetX: Int, offsetY: Int): TextFindResult {
        val box = boundingBox ?: Rect(0, 0, 0, 0)
        val left = box.left + offsetX
        val top = box.top + offsetY
        val right = box.right + offsetX
        val bottom = box.bottom + offsetY
        return TextFindResult(
            found = true,
            text = text,
            x = (left + right) / 2,
            y = (top + bottom) / 2,
            left = left,
            top = top,
            right = right,
            bottom = bottom
        )
    }
}

private suspend fun <T> com.google.android.gms.tasks.Task<T>.await(): T {
    return suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { result ->
            if (continuation.isActive) {
                continuation.resume(result)
            }
        }
        addOnFailureListener { throwable ->
            if (continuation.isActive) {
                continuation.resumeWithException(throwable)
            }
        }
        addOnCanceledListener {
            if (continuation.isActive) {
                continuation.cancel()
            }
        }
    }
}
