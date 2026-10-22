package com.sbros.app.domain.repository

import kotlinx.coroutines.flow.Flow

sealed interface AuthResult {
    data class Success(val userId: String, val email: String) : AuthResult
    data class Error(val message: String) : AuthResult
}

interface AuthRepository {
    val currentUserId: Flow<String?>
    val currentUserEmail: Flow<String?>
    val isGuestMode: Flow<Boolean>
    val isAuthenticated: Flow<Boolean>

    suspend fun signUp(email: String, password: String, displayName: String? = null): AuthResult
    suspend fun signIn(email: String, password: String): AuthResult
    suspend fun signOut()
    suspend fun continueAsGuest()
}
