package com.lulucloud.touchscript.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "touch_workshop_settings")

data class AppSettings(
    val editorFontScale: Float = 1f,
    val defaultDelayMs: Int = 120,
    val selectedScriptPath: String? = null,
    val selectedScriptName: String? = null
)

class SettingsRepository(
    private val context: Context
) {
    val settings: Flow<AppSettings> = context.settingsDataStore.data.map { preferences ->
        AppSettings(
            editorFontScale = preferences[EDITOR_FONT_SCALE] ?: 1f,
            defaultDelayMs = preferences[DEFAULT_DELAY_MS] ?: 120,
            selectedScriptPath = preferences[SELECTED_SCRIPT_PATH],
            selectedScriptName = preferences[SELECTED_SCRIPT_NAME]
        )
    }

    suspend fun getSettings(): AppSettings = settings.first()

    suspend fun setEditorFontScale(scale: Float) {
        context.settingsDataStore.edit { preferences ->
            preferences[EDITOR_FONT_SCALE] = scale
        }
    }

    suspend fun setDefaultDelayMs(delayMs: Int) {
        context.settingsDataStore.edit { preferences ->
            preferences[DEFAULT_DELAY_MS] = delayMs
        }
    }

    suspend fun setSelectedScript(path: String?, name: String?) {
        context.settingsDataStore.edit { preferences ->
            if (path.isNullOrBlank()) {
                preferences.remove(SELECTED_SCRIPT_PATH)
            } else {
                preferences[SELECTED_SCRIPT_PATH] = path
            }
            if (name.isNullOrBlank()) {
                preferences.remove(SELECTED_SCRIPT_NAME)
            } else {
                preferences[SELECTED_SCRIPT_NAME] = name
            }
        }
    }

    private companion object {
        val EDITOR_FONT_SCALE = floatPreferencesKey("editor_font_scale")
        val DEFAULT_DELAY_MS = intPreferencesKey("default_delay_ms")
        val SELECTED_SCRIPT_PATH = stringPreferencesKey("selected_script_path")
        val SELECTED_SCRIPT_NAME = stringPreferencesKey("selected_script_name")
    }
}
