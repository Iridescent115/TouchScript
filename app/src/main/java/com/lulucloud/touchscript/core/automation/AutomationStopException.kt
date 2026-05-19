package com.lulucloud.touchscript.core.automation

class AutomationStopException(
    message: String = "用户停止了当前脚本"
) : RuntimeException(message)
