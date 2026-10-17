package com.sbros.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sbros.app.domain.model.ThemePalette
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "sbros_settings")

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object PreferencesKeys {
        val THEME_PALETTE = stringPreferencesKey("theme_palette")
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val HAS_COMPLETED_ONBOARDING = booleanPreferencesKey("has_completed_onboarding")
        val USER_NAME = stringPreferencesKey("user_name")
    }

    val hasCompletedOnboardingFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.HAS_COMPLETED_ONBOARDING] ?: false
    }

    val userNameFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.USER_NAME]?.takeIf { it.isNotBlank() } ?: "Данила"
    }

    val themePaletteFlow: Flow<ThemePalette> = context.dataStore.data.map { preferences ->
        val paletteName = preferences[PreferencesKeys.THEME_PALETTE] ?: ThemePalette.LAVENDER.name
        try {
            ThemePalette.valueOf(paletteName)
        } catch (_: IllegalArgumentException) {
            ThemePalette.LAVENDER
        }
    }

    val isDarkModeFlow: Flow<Boolean?> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.DARK_MODE]
    }

    suspend fun setUserName(name: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.USER_NAME] = name.trim()
        }
    }

    suspend fun setThemePalette(palette: ThemePalette) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_PALETTE] = palette.name
        }
    }

    suspend fun setDarkMode(isDark: Boolean?) {
        context.dataStore.edit { preferences ->
            if (isDark == null) {
                preferences.remove(PreferencesKeys.DARK_MODE)
            } else {
                preferences[PreferencesKeys.DARK_MODE] = isDark
            }
        }
    }

    suspend fun setOnboardingCompleted(completed: Boolean = true) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.HAS_COMPLETED_ONBOARDING] = completed
        }
    }
}
