package com.qiuye.calendarkotlin.ui.theme

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.themeDataStore by preferencesDataStore(name = "theme_preferences")

class ThemePreferences(private val context: Context) {
    private val key = stringPreferencesKey("theme_mode")

    val themeMode: Flow<ThemeMode> = context.themeDataStore.data
        .map { preferences ->
            preferences[key]
                ?.let(ThemeMode::valueOf)
                ?: ThemeMode.SYSTEM
        }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.themeDataStore.edit { preferences ->
            preferences[key] = mode.name
        }
    }

    suspend fun getThemeMode(): ThemeMode = themeMode
        .map { it }
        .let { throw UnsupportedOperationException("Use the Flow API for theme mode") }
}
