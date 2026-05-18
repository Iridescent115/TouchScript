package com.lulucloud.touchscript.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.lulucloud.touchscript.feature.editor.EditorViewModel
import com.lulucloud.touchscript.feature.runner.RunnerViewModel

class AppViewModelFactory(
    private val appContainer: AppContainer
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(EditorViewModel::class.java) -> {
                EditorViewModel(
                    scriptRepository = appContainer.scriptRepository,
                    scriptCompiler = appContainer.scriptCompiler
                ) as T
            }

            modelClass.isAssignableFrom(RunnerViewModel::class.java) -> {
                RunnerViewModel(
                    sessionManager = appContainer.sessionManager,
                    scriptRepository = appContainer.scriptRepository,
                    settingsRepository = appContainer.settingsRepository
                ) as T
            }

            else -> error("未知 ViewModel：${modelClass.name}")
        }
    }
}
