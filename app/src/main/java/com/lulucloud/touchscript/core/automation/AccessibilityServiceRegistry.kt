package com.lulucloud.touchscript.core.automation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AccessibilityServiceRegistry {
    private val _service = MutableStateFlow<TouchWorkshopAccessibilityService?>(null)

    val service: StateFlow<TouchWorkshopAccessibilityService?> = _service.asStateFlow()

    fun attach(service: TouchWorkshopAccessibilityService) {
        _service.value = service
    }

    fun detach(service: TouchWorkshopAccessibilityService) {
        if (_service.value == service) {
            _service.value = null
        }
    }
}
