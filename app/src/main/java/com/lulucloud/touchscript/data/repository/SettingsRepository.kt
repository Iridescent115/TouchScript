package com.lulucloud.touchscript.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "touch_workshop_settings")

data class AppSettings(
    val showFloatingPanel: Boolean = true,
    val editorFontScale: Float = 1f,
    val defaultDelayMs: Int = 120
)

class SettingsRepository(
    private val context: Context
) {
    val settings: Flow<AppSettings> = context.settingsDataStore.data.map { preferences ->
        AppSettings(
            showFloatingPanel = preferences[SHOW_FLOATING_PANEL] ?: true,
            editorFontScale = preferences[EDITOR_FONT_SCALE] ?: 1f,
            defaultDelayMs = preferences[DEFAULT_DELAY_MS] ?: 120
        )
    }

    suspend fun setShowFloatingPanel(enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[SHOW_FLOATING_PANEL] = enabled
        }
    }

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

    private companion object {
        val SHOW_FLOATING_PANEL = booleanPreferencesKey("show_floating_panel")
        val EDITOR_FONT_SCALE = floatPreferencesKey("editor_font_scale")
        val DEFAULT_DELAY_MS = intPreferencesKey("default_delay_ms")
    }
}
