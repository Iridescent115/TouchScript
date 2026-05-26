package com.lulucloud.touchscript.core.runtime

import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.TextRecognition
import com.lulucloud.touchscript.core.automation.AccessibilityServiceRegistry
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

data class TextRecognitionResult(
    val found: Boolean,
    val text: String,
    val lineCount: Int
)

class TextRecognizerEngine {
    private val recognizer by lazy {
        TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
    }

    suspend fun recognizeScreenText(): TextRecognitionResult {
        val service = AccessibilityServiceRegistry.service.value
            ?: throw IllegalStateException("无障碍服务未连接，无法截图识文字")
        val screenBitmap = service.takeScreenshotBitmap()
            ?: throw IllegalStateException("无法获取屏幕截图，请确认无障碍服务已允许截图")

        return try {
            val image = InputImage.fromBitmap(screenBitmap, 0)
            val visionText = recognizer.process(image).await()
            val lines = visionText.textBlocks.flatMap { block -> block.lines }
            val text = visionText.text.trim()
            TextRecognitionResult(
                found = text.isNotBlank(),
                text = text,
                lineCount = lines.size
            )
        } finally {
            screenBitmap.recycle()
        }
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
