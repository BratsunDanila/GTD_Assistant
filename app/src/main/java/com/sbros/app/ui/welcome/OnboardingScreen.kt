package com.sbros.app.ui.welcome

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.East
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Swipe
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.West
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sbros.app.domain.model.ThemePalette
import com.sbros.app.domain.repository.AuthResult

/**
 * Полноэкранный 4-шаговый экран онбординга:
 * - Слайд 1 (Шаг 0): «Выговори кашу из головы»
 * - Слайд 2 (Шаг 1): «По одной задаче за раз»
 * - Слайд 3 (Шаг 2): «Вход и Регистрация» (с опцией «Продолжить без аккаунта»)
 * - Слайд 4 (Шаг 3):
 *     - Если гость: «Как к тебе обращаться на главном экране» + палитра
 *     - Если вошел/зарегистрировался: экран выбора цветовой палитры (без слова Material 3)
 */
@Composable
fun OnboardingScreen(
    currentName: String,
    currentPalette: ThemePalette,
    onNameChange: (String) -> Unit,
    onPaletteSelect: (ThemePalette) -> Unit,
    onSignIn: (email: String, pass: String, (AuthResult) -> Unit) -> Unit,
    onSignUp: (email: String, pass: String, name: String, (AuthResult) -> Unit) -> Unit,
    onContinueAsGuest: () -> Unit,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var step by remember { mutableIntStateOf(0) }
    var isGuest by remember { mutableStateOf(true) }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Верхняя строка: Индикатор 4 шагов + кнопка «Пропустить»
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(4) { index ->
                        val width by animateDpAsState(
                            targetValue = if (index == step) 26.dp else 8.dp,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            ),
                            label = "dotWidth"
                        )
                        Box(
                            modifier = Modifier
                                .height(8.dp)
                                .width(width)
                                .clip(CircleShape)
                                .background(
                                    if (index == step) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                                )
                        )
                    }
                }

                Surface(
                    onClick = onComplete,
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Text(
                        text = "Пропустить",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            // Центральная контентная зона со слайдами: СТРОГО ПО ЦЕНТРУ и строго горизонтальная анимация
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = step,
                    transitionSpec = {
                        if (targetState > initialState) {
                            (slideInHorizontally(animationSpec = tween(320)) { width -> width } + fadeIn(animationSpec = tween(320))) togetherWith
                                    (slideOutHorizontally(animationSpec = tween(320)) { width -> -width } + fadeOut(animationSpec = tween(320)))
                        } else {
                            (slideInHorizontally(animationSpec = tween(320)) { width -> -width } + fadeIn(animationSpec = tween(320))) togetherWith
                                    (slideOutHorizontally(animationSpec = tween(320)) { width -> width } + fadeOut(animationSpec = tween(320)))
                        }.using(
                            SizeTransform(clip = false) { _, _ -> tween(0) }
                        )
                    },
                    label = "onboardSlide",
                    modifier = Modifier.fillMaxSize()
                ) { currentStep ->
                    when (currentStep) {
                        0 -> OnboardingSlide1()
                        1 -> OnboardingSlide2()
                        2 -> OnboardingSlideAuth(
                            initialName = currentName,
                            onSignIn = onSignIn,
                            onSignUp = onSignUp,
                            onContinueAsGuest = {
                                onContinueAsGuest()
                                isGuest = true
                                step = 3
                            },
                            onAuthSuccess = { enteredName ->
                                if (enteredName.isNotBlank()) {
                                    onNameChange(enteredName)
                                }
                                isGuest = false
                                step = 3
                            }
                        )
                        3 -> OnboardingSlidePersonalization(
                            isGuest = isGuest,
                            name = currentName,
                            palette = currentPalette,
                            onNameChange = onNameChange,
                            onPaletteSelect = onPaletteSelect
                        )
                    }
                }
            }

            // Нижний ряд кнопок со строго фиксированной высотой 52.dp
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (step > 0) {
                    Surface(
                        onClick = { step-- },
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLowest,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.height(52.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 20.dp)
                        ) {
                            Text(
                                text = "Назад",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.5.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                if (step < 2) {
                    Button(
                        onClick = { step++ },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                    ) {
                        Text(
                            text = "Далее",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else if (step == 3) {
                    Button(
                        onClick = onComplete,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                    ) {
                        Text(
                            text = "Начать работу",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Слайд 1: «Выговори кашу из головы» (выровнен по центру)
 */
@Composable
private fun OnboardingSlide1() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                modifier = Modifier.size(56.dp),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.GraphicEq,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }

            Text(
                text = "Выговори кашу из головы",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 26.sp,
                    letterSpacing = (-0.02).sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "Не нужно придумывать папки и формулировки. Говори всё подряд — ИИ сам отсеет усталость и выделит конкретные шаги.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 13.5.sp,
                    lineHeight = 19.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Демо-карточка (Что ты говоришь -> Что создает ИИ)
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "ЧТО ТЫ ГОВОРИШЬ:",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.5.sp,
                            letterSpacing = 0.4.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = buildAnnotatedString {
                            withStyle(
                                SpanStyle(
                                    textDecoration = TextDecoration.LineThrough,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                                )
                            ) {
                                append("«Блин, ничего не успеваю...»")
                            }
                            append(" Надо ")
                            withStyle(
                                SpanStyle(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                append("скинуть тему старосте")
                            }
                            append(" и в пятницу в 14:00 ")
                            withStyle(
                                SpanStyle(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                append("созвон с научруком")
                            }
                        },
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.5.sp,
                            lineHeight = 18.sp
                        )
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerLow)

                    Text(
                        text = "ЧТО СОЗДАЕТ ИИ:",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.5.sp,
                            letterSpacing = 0.4.sp
                        ),
                        color = Color(0xFF1E6B43)
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "1. Отправить тему старосте",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp
                                )
                            )
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceContainerHigh
                            ) {
                                Text(
                                    text = "Механика",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "2. Созвон с научруком",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp
                                )
                            )
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "Пт, 14:00",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.5.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Слайд 2: «По одной задаче за раз» (выровнен по центру)
 */
@Composable
private fun OnboardingSlide2() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                modifier = Modifier.size(56.dp),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Swipe,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }

            Text(
                text = "По одной задаче за раз",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 26.sp,
                    letterSpacing = (-0.02).sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "Никаких пугающих списков из 50 дел. Выбирай свое состояние, а приложение подаст ровно одну подходящую карточку.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 13.5.sp,
                    lineHeight = 19.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Памятка по жестам
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.East,
                            contentDescription = null,
                            tint = Color(0xFF1E6B43),
                            modifier = Modifier.size(22.dp)
                        )
                        Column {
                            Text(
                                text = "Свайп вправо — Готово",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            )
                            Text(
                                text = "Задача мгновенно закрывается",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.West,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                        Column {
                            Text(
                                text = "Свайп влево — Потом",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            )
                            Text(
                                text = "Отложить без чувства вины и красных дат",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoFixHigh,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Column {
                            Text(
                                text = "Кнопка 1/3 — Сделать проще",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            )
                            Text(
                                text = "ИИ дробит задачу до микро-шага на 1 минуту",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Слайд 3: Экран входа и регистрации (выровнен по центру)
 */
@Composable
private fun OnboardingSlideAuth(
    initialName: String,
    onSignIn: (email: String, pass: String, (AuthResult) -> Unit) -> Unit,
    onSignUp: (email: String, pass: String, name: String, (AuthResult) -> Unit) -> Unit,
    onContinueAsGuest: () -> Unit,
    onAuthSuccess: (enteredName: String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(1) } // По умолчанию "Регистрация"
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf(initialName) }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                modifier = Modifier.size(52.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Cloud,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Text(
                text = "Синхронизация и аккаунт",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    letterSpacing = (-0.02).sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "Приложение полностью работает без интернета. Создайте аккаунт для облачной базы данных Supabase или продолжайте как гость.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Табы: Регистрация / Вход
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.clip(RoundedCornerShape(12.dp))
            ) {
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1; errorMessage = null },
                    text = { Text("Регистрация", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium) }
                )
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0; errorMessage = null },
                    text = { Text("Вход", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium) }
                )
            }

            if (selectedTab == 1) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Ваше имя") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Rounded.Person, contentDescription = null)
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

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

            Spacer(modifier = Modifier.height(2.dp))

            // Кнопка Вход / Регистрация
            Button(
                onClick = {
                    isLoading = true
                    errorMessage = null
                    if (selectedTab == 1) {
                        onSignUp(email, password, name) { result ->
                            isLoading = false
                            when (result) {
                                is AuthResult.Success -> onAuthSuccess(name.trim())
                                is AuthResult.Error -> errorMessage = result.message
                            }
                        }
                    } else {
                        onSignIn(email, password) { result ->
                            isLoading = false
                            when (result) {
                                is AuthResult.Success -> onAuthSuccess("")
                                is AuthResult.Error -> errorMessage = result.message
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
                        text = if (selectedTab == 1) "Создать аккаунт и войти" else "Войти",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.5.sp
                    )
                }
            }

            // Кнопка Продолжить без аккаунта
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

            Text(
                text = "Все дела останутся в телефоне и автоматически загрузятся в облако, если решите зарегистрироваться позже.",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 14.sp),
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            )
        }
    }
}

/**
 * Слайд 4: Настройка имени (если гость) и палитры (выровнен по центру)
 */
@Composable
private fun OnboardingSlidePersonalization(
    isGuest: Boolean,
    name: String,
    palette: ThemePalette,
    onNameChange: (String) -> Unit,
    onPaletteSelect: (ThemePalette) -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                modifier = Modifier.size(56.dp),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isGuest) Icons.Rounded.Tune else Icons.Rounded.Palette,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }

            Text(
                text = if (isGuest) "Как к тебе обращаться?" else "Выбери оформление",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 26.sp,
                    letterSpacing = (-0.02).sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = if (isGuest) {
                    "Как к тебе обращаться на главном экране?"
                } else {
                    "Рады знакомству, ${name.ifBlank { "пользователь" }}! Настрой цветовую гамму под себя."
                },
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Если гость — показываем поле ввода имени
                    if (isGuest) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainer)
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                        ) {
                            BasicTextField(
                                value = name,
                                onValueChange = onNameChange,
                                singleLine = true,
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // Заголовок без слова "Material 3"
                    Text(
                        text = "ЦВЕТОВАЯ ПАЛИТРА:",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.5.sp,
                            letterSpacing = 0.4.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Сетка 2x2 палитр
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            PaletteButton(
                                title = "Лаванда",
                                color = Color(0xFF554F87),
                                isSelected = palette == ThemePalette.LAVENDER,
                                onClick = { onPaletteSelect(ThemePalette.LAVENDER) },
                                modifier = Modifier.weight(1f)
                            )
                            PaletteButton(
                                title = "Шалфей",
                                color = Color(0xFF336746),
                                isSelected = palette == ThemePalette.SAGE,
                                onClick = { onPaletteSelect(ThemePalette.SAGE) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            PaletteButton(
                                title = "Терракота",
                                color = Color(0xFF8B4A27),
                                isSelected = palette == ThemePalette.CLAY,
                                onClick = { onPaletteSelect(ThemePalette.CLAY) },
                                modifier = Modifier.weight(1f)
                            )
                            PaletteButton(
                                title = "Океан",
                                color = Color(0xFF176678),
                                isSelected = palette == ThemePalette.OCEAN,
                                onClick = { onPaletteSelect(ThemePalette.OCEAN) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PaletteButton(
    title: String,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(
            1.dp,
            if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
        ),
        modifier = modifier.height(40.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(7.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    fontSize = 12.sp
                ),
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
