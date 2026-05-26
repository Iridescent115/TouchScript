package com.lulucloud.touchscript.core.automation

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicLong

data class ScreenPoint(
    val x: Int,
    val y: Int
)

data class ScreenRect(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
)

enum class CoordinateCaptureMode {
    POINTS,
    RECTANGLE
}

data class CoordinateCaptureRequest(
    val requestId: Long,
    val pointCount: Int,
    val stepLabels: List<String>,
    val mode: CoordinateCaptureMode = CoordinateCaptureMode.POINTS
)

data class CoordinateCaptureResult(
    val requestId: Long,
    val points: List<ScreenPoint> = emptyList(),
    val rect: ScreenRect? = null
)

object CoordinateCaptureManager {
    private val nextRequestId = AtomicLong(1L)

    private val _activeRequest = MutableStateFlow<CoordinateCaptureRequest?>(null)
    val activeRequest: StateFlow<CoordinateCaptureRequest?> = _activeRequest.asStateFlow()

    private val _results = MutableSharedFlow<CoordinateCaptureResult>(extraBufferCapacity = 1)
    val results: SharedFlow<CoordinateCaptureResult> = _results.asSharedFlow()

    fun createRequest(pointCount: Int, stepLabels: List<String>): CoordinateCaptureRequest {
        return CoordinateCaptureRequest(
            requestId = nextRequestId.getAndIncrement(),
            pointCount = pointCount.coerceAtLeast(1),
            stepLabels = stepLabels.take(pointCount.coerceAtLeast(1))
        )
    }

    fun createRectangleRequest(stepLabel: String): CoordinateCaptureRequest {
        return CoordinateCaptureRequest(
            requestId = nextRequestId.getAndIncrement(),
            pointCount = 0,
            stepLabels = listOf(stepLabel),
            mode = CoordinateCaptureMode.RECTANGLE
        )
    }

    fun activate(request: CoordinateCaptureRequest) {
        _activeRequest.value = request
    }

    fun complete(points: List<ScreenPoint>) {
        val request = _activeRequest.value ?: return
        _results.tryEmit(
            CoordinateCaptureResult(
                requestId = request.requestId,
                points = points
            )
        )
        _activeRequest.value = null
    }

    fun complete(rect: ScreenRect) {
        val request = _activeRequest.value ?: return
        _results.tryEmit(
            CoordinateCaptureResult(
                requestId = request.requestId,
                rect = rect
            )
        )
        _activeRequest.value = null
    }

    fun cancel() {
        _activeRequest.value = null
    }
}
