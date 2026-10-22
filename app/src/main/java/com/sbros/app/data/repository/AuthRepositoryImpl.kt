package com.sbros.app.data.repository

import android.util.Log
import com.sbros.app.data.local.PreferencesManager
import com.sbros.app.data.local.ProjectDao
import com.sbros.app.data.local.TaskDao
import com.sbros.app.domain.repository.AuthRepository
import com.sbros.app.domain.repository.AuthResult
import com.sbros.app.sync.SyncManager
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val supabase: SupabaseClient,
    private val taskDao: TaskDao,
    private val projectDao: ProjectDao,
    private val preferencesManager: PreferencesManager,
    private val syncManager: SyncManager
) : AuthRepository {

    private val TAG = "AuthRepository"

    override val currentUserId: Flow<String?> = preferencesManager.userIdFlow
    override val currentUserEmail: Flow<String?> = preferencesManager.userEmailFlow
    override val isGuestMode: Flow<Boolean> = preferencesManager.isGuestModeFlow
    override val isAuthenticated: Flow<Boolean> = preferencesManager.userIdFlow.map { !it.isNullOrBlank() }

    override suspend fun signUp(email: String, password: String, displayName: String?): AuthResult {
        return try {
            val trimmedEmail = email.trim()
            val trimmedPassword = password.trim()

            if (trimmedEmail.isEmpty() || !trimmedEmail.contains("@")) {
                return AuthResult.Error("Введите корректный адрес электронной почты")
            }
            if (trimmedPassword.length < 6) {
                return AuthResult.Error("Пароль должен содержать минимум 6 символов")
            }

            supabase.auth.signUpWith(Email) {
                this.email = trimmedEmail
                this.password = trimmedPassword
            }

            val user = supabase.auth.currentUserOrNull()
            val userId = user?.id ?: java.util.UUID.randomUUID().toString()

            preferencesManager.saveAuthSession(userId, trimmedEmail)
            if (!displayName.isNullOrBlank()) {
                preferencesManager.setUserName(displayName.trim())
            }

            // Миграция локальных гостевых задач в новый аккаунт пользователя
            taskDao.assignUserToGuestTasks(userId)
            projectDao.assignUserToGuestProjects(userId)

            syncManager.triggerSync()
            AuthResult.Success(userId, trimmedEmail)
        } catch (e: Exception) {
            Log.e(TAG, "Sign up error: ${e.message}", e)
            AuthResult.Error(formatErrorMessage(e))
        }
    }

    override suspend fun signIn(email: String, password: String): AuthResult {
        return try {
            val trimmedEmail = email.trim()
            val trimmedPassword = password.trim()

            if (trimmedEmail.isEmpty()) {
                return AuthResult.Error("Введите email")
            }
            if (trimmedPassword.isEmpty()) {
                return AuthResult.Error("Введите пароль")
            }

            supabase.auth.signInWith(Email) {
                this.email = trimmedEmail
                this.password = trimmedPassword
            }

            val user = supabase.auth.currentUserOrNull()
            val userId = user?.id ?: java.util.UUID.randomUUID().toString()

            preferencesManager.saveAuthSession(userId, trimmedEmail)

            // Миграция локальных гостевых задач, если они были созданы до логина
            taskDao.assignUserToGuestTasks(userId)
            projectDao.assignUserToGuestProjects(userId)

            syncManager.triggerSync()
            AuthResult.Success(userId, trimmedEmail)
        } catch (e: Exception) {
            Log.e(TAG, "Sign in error: ${e.message}", e)
            AuthResult.Error(formatErrorMessage(e))
        }
    }

    override suspend fun signOut() {
        try {
            supabase.auth.signOut()
        } catch (e: Exception) {
            Log.w(TAG, "Supabase sign out warning: ${e.message}")
        } finally {
            preferencesManager.clearAuthSession()
        }
    }

    override suspend fun continueAsGuest() {
        preferencesManager.setGuestMode(true)
    }

    private fun formatErrorMessage(e: Exception): String {
        val msg = e.message ?: ""
        return when {
            msg.contains("Invalid login credentials", ignoreCase = true) ->
                "Неверный email или пароль"
            msg.contains("User already registered", ignoreCase = true) ||
            msg.contains("already exists", ignoreCase = true) ->
                "Пользователь с таким email уже зарегистрирован. Попробуйте войти."
            msg.contains("Password should be at least 6 characters", ignoreCase = true) ->
                "Пароль должен содержать не менее 6 символов"
            msg.contains("Unable to resolve host", ignoreCase = true) ||
            msg.contains("Failed to connect", ignoreCase = true) ||
            msg.contains("timeout", ignoreCase = true) ->
                "Нет связи с сервером. Проверьте интернет или продолжайте в гостевом режиме (все данные сохраняются локально)."
            else -> "Ошибка авторизации: ${e.localizedMessage ?: "проверьте введенные данные"}"
        }
    }
}
