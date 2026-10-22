package com.sbros.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sbros.app.domain.model.ThemePalette
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
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
        val USER_ID = stringPreferencesKey("user_id")
        val USER_EMAIL = stringPreferencesKey("user_email")
        val IS_GUEST_MODE = booleanPreferencesKey("is_guest_mode")
        val LAST_SYNCED_TIME = longPreferencesKey("last_synced_time")
        val SUPABASE_URL = stringPreferencesKey("supabase_url")
        val SUPABASE_ANON_KEY = stringPreferencesKey("supabase_anon_key")
    }

    val hasCompletedOnboardingFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.HAS_COMPLETED_ONBOARDING] ?: false
    }

    val userNameFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.USER_NAME]?.takeIf { it.isNotBlank() } ?: "Данила"
    }

    val userIdFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.USER_ID]
    }

    val userEmailFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.USER_EMAIL]
    }

    val isGuestModeFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.IS_GUEST_MODE] ?: true
    }

    val lastSyncedTimeFlow: Flow<Long> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.LAST_SYNCED_TIME] ?: 0L
    }

    val supabaseUrlFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.SUPABASE_URL]
    }

    val supabaseAnonKeyFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.SUPABASE_ANON_KEY]
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
        preferences[PreferencesKeys.DARK_MODE] ?: false
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

    suspend fun saveAuthSession(userId: String, email: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.USER_ID] = userId
            preferences[PreferencesKeys.USER_EMAIL] = email
            preferences[PreferencesKeys.IS_GUEST_MODE] = false
        }
    }

    suspend fun clearAuthSession() {
        context.dataStore.edit { preferences ->
            preferences.remove(PreferencesKeys.USER_ID)
            preferences.remove(PreferencesKeys.USER_EMAIL)
            preferences[PreferencesKeys.IS_GUEST_MODE] = true
            preferences.remove(PreferencesKeys.LAST_SYNCED_TIME)
        }
    }

    suspend fun setGuestMode(isGuest: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_GUEST_MODE] = isGuest
        }
    }

    suspend fun updateLastSyncedTime(time: Long = System.currentTimeMillis()) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_SYNCED_TIME] = time
        }
    }

    suspend fun setCustomSupabaseCredentials(url: String?, key: String?) {
        context.dataStore.edit { preferences ->
            if (url.isNullOrBlank()) {
                preferences.remove(PreferencesKeys.SUPABASE_URL)
            } else {
                preferences[PreferencesKeys.SUPABASE_URL] = url.trim()
            }
            if (key.isNullOrBlank()) {
                preferences.remove(PreferencesKeys.SUPABASE_ANON_KEY)
            } else {
                preferences[PreferencesKeys.SUPABASE_ANON_KEY] = key.trim()
            }
        }
    }

    suspend fun getUserId(): String? = userIdFlow.first()
    suspend fun isGuest(): Boolean = isGuestModeFlow.first()
}
