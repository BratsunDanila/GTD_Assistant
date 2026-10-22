package com.sbros.app.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sbros.app.domain.model.AppSyncState
import com.sbros.app.domain.repository.AuthResult
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthBottomSheet(
    isAuthenticated: Boolean,
    userEmail: String?,
    userId: String?,
    syncState: AppSyncState,
    lastSyncedTime: Long,
    onSignIn: (email: String, pass: String, (AuthResult) -> Unit) -> Unit,
    onSignUp: (email: String, pass: String, name: String, (AuthResult) -> Unit) -> Unit,
    onSignOut: () -> Unit,
    onTriggerSync: () -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isAuthenticated) {
                // --- ЭКРАН ПРОФИЛЯ АВТОРИЗОВАННОГО ПОЛЬЗОВАТЕЛЯ ---
                AuthenticatedProfileContent(
                    email = userEmail ?: "Пользователь",
                    userId = userId ?: "",
                    syncState = syncState,
                    lastSyncedTime = lastSyncedTime,
                    onTriggerSync = onTriggerSync,
                    onSignOut = {
                        scope.launch {
                            onSignOut()
                            sheetState.hide()
                            onDismissRequest()
                        }
                    }
                )
            } else {
                // --- ЭКРАН ВХОДА / РЕГИСТРАЦИИ (ГОСТЕВОЙ РЕЖИМ) ---
                GuestAuthContent(
                    onSignIn = onSignIn,
                    onSignUp = onSignUp,
                    onContinueAsGuest = {
                        scope.launch {
                            sheetState.hide()
                            onDismissRequest()
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun AuthenticatedProfileContent(
    email: String,
    userId: String,
    syncState: AppSyncState,
    lastSyncedTime: Long,
    onTriggerSync: () -> Unit,
    onSignOut: () -> Unit
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.size(72.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Rounded.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(40.dp)
            )
        }
    }

    Spacer(modifier = Modifier.height(14.dp))

    Text(
        text = email,
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center
    )

    if (userId.isNotBlank()) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "ID: ${userId.take(8)}...${userId.takeLast(6)}",
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    Spacer(modifier = Modifier.height(20.dp))

    // Карточка статуса синхронизации
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    when (syncState) {
                        AppSyncState.SYNCING -> {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        AppSyncState.OFFLINE -> {
                            Icon(
                                imageVector = Icons.Rounded.CloudOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        AppSyncState.ERROR -> {
                            Icon(
                                imageVector = Icons.Rounded.CloudOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        else -> {
                            Icon(
                                imageVector = Icons.Rounded.CloudDone,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Text(
                        text = when (syncState) {
                            AppSyncState.SYNCING -> "Синхронизация с Supabase..."
                            AppSyncState.OFFLINE -> "Офлайн (локальный режим Room)"
                            AppSyncState.ERROR -> "Ошибка связи с облаком"
                            else -> "Облачная база синхронизирована"
                        },
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            if (lastSyncedTime > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                val timeStr = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(lastSyncedTime))
                Text(
                    text = "Посл. обновление: $timeStr",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onTriggerSync,
                enabled = syncState != AppSyncState.SYNCING,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (syncState == AppSyncState.SYNCING) "Синхронизация..." else "Синхронизировать сейчас",
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(24.dp))

    OutlinedButton(
        onClick = onSignOut,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.error
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.Logout,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = "Выйти из аккаунта", fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun GuestAuthContent(
    onSignIn: (email: String, pass: String, (AuthResult) -> Unit) -> Unit,
    onSignUp: (email: String, pass: String, name: String, (AuthResult) -> Unit) -> Unit,
    onContinueAsGuest: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Вход, 1 = Регистрация
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.size(60.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Rounded.Cloud,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(32.dp)
            )
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    Text(
        text = "Синхронизация и аккаунт",
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface
    )

    Spacer(modifier = Modifier.height(6.dp))

    Text(
        text = "Приложение полностью работает офлайн. Авторизуйтесь, чтобы подключить облачную базу данных Supabase.",
        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 16.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(18.dp))

    // Вкладки: Вход / Регистрация
    TabRow(
        selectedTabIndex = selectedTab,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.clip(RoundedCornerShape(12.dp))
    ) {
        Tab(
            selected = selectedTab == 0,
            onClick = {
                selectedTab = 0
                errorMessage = null
            },
            text = { Text("Вход", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium) }
        )
        Tab(
            selected = selectedTab == 1,
            onClick = {
                selectedTab = 1
                errorMessage = null
            },
            text = { Text("Регистрация", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium) }
        )
    }

    Spacer(modifier = Modifier.height(18.dp))

    // Поле имени (только при регистрации)
    if (selectedTab == 1) {
        OutlinedTextField(
            value = displayName,
            onValueChange = { displayName = it },
            label = { Text("Ваше имя") },
            leadingIcon = {
                Icon(imageVector = Icons.Rounded.Person, contentDescription = null)
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))
    }

    // Поле Email
    OutlinedTextField(
        value = email,
        onValueChange = {
            email = it
            errorMessage = null
        },
        label = { Text("Электронная почта") },
        leadingIcon = {
            Icon(imageVector = Icons.Rounded.Email, contentDescription = null)
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(12.dp))

    // Поле Пароль
    OutlinedTextField(
        value = password,
        onValueChange = {
            password = it
            errorMessage = null
        },
        label = { Text("Пароль (от 6 символов)") },
        leadingIcon = {
            Icon(imageVector = Icons.Rounded.Lock, contentDescription = null)
        },
        trailingIcon = {
            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                Icon(
                    imageVector = if (passwordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                    contentDescription = null
                )
            }
        },
        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    )

    AnimatedVisibility(visible = errorMessage != null) {
        Column {
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = errorMessage ?: "",
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }

    AnimatedVisibility(visible = successMessage != null) {
        Column {
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = successMessage ?: "",
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp)
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(20.dp))

    // Кнопка действия (Войти или Зарегистрироваться)
    Button(
        onClick = {
            isLoading = true
            errorMessage = null
            if (selectedTab == 0) {
                onSignIn(email, password) { result ->
                    isLoading = false
                    when (result) {
                        is AuthResult.Success -> {
                            successMessage = "Успешный вход! Задачи перенесены."
                        }
                        is AuthResult.Error -> {
                            errorMessage = result.message
                        }
                    }
                }
            } else {
                onSignUp(email, password, displayName) { result ->
                    isLoading = false
                    when (result) {
                        is AuthResult.Success -> {
                            successMessage = "Аккаунт создан! Задачи привязаны к облаку."
                        }
                        is AuthResult.Error -> {
                            errorMessage = result.message
                        }
                    }
                }
            }
        },
        enabled = !isLoading && email.isNotBlank() && password.length >= 6,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp,
                modifier = Modifier.size(22.dp)
            )
        } else {
            Text(
                text = if (selectedTab == 0) "Войти" else "Зарегистрироваться",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // Кнопка продолжить как гость
    OutlinedButton(
        onClick = onContinueAsGuest,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
    ) {
        Text(
            text = "Продолжить без аккаунта (Гость)",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    Spacer(modifier = Modifier.height(12.dp))

    Text(
        text = "Все дела и проекты, созданные офлайн, сохранятся в телефоне и автоматически загрузятся в облако при входе.",
        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 14.sp),
        color = MaterialTheme.colorScheme.outline,
        textAlign = TextAlign.Center
    )
}
