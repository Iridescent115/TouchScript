package com.lulucloud.touchscript.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.lulucloud.touchscript.feature.editor.EditorViewModel
import com.lulucloud.touchscript.feature.home.HomeViewModel

class AppViewModelFactory(
    private val appContainer: AppContainer
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(HomeViewModel::class.java) -> {
                HomeViewModel(
                    fileScriptRepository = appContainer.fileScriptRepository,
                    settingsRepository = appContainer.settingsRepository,
                    scriptCompiler = appContainer.scriptCompiler
                ) as T
            }

            modelClass.isAssignableFrom(EditorViewModel::class.java) -> {
                EditorViewModel(
                    fileScriptRepository = appContainer.fileScriptRepository,
                    settingsRepository = appContainer.settingsRepository,
                    scriptCompiler = appContainer.scriptCompiler
                ) as T
            }

            else -> error("未知 ViewModel：${modelClass.name}")
        }
    }
}
