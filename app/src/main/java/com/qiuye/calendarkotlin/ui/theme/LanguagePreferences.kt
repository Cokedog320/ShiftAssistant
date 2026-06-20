package com.qiuye.calendarkotlin.ui.theme

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.languageDataStore by preferencesDataStore(name = "language_preferences")

class LanguagePreferences(private val context: Context) {
    private val key = stringPreferencesKey("language_mode")

    val languageMode: Flow<LanguageMode> = context.languageDataStore.data
        .map { preferences ->
            preferences[key]
                ?.let(LanguageMode::valueOf)
                ?: LanguageMode.SYSTEM
        }

    suspend fun setLanguageMode(mode: LanguageMode) {
        context.languageDataStore.edit { preferences ->
            preferences[key] = mode.name
        }
    }
}
