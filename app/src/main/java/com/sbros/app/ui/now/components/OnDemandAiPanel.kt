package com.sbros.app.ui.now.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Ленивый анализ ИИ по требованию (On-Demand AI Panel) по спецификации sbros_production_m3.html.
 * - По умолчанию панель скрыта и не нагружает экран.
 * - При клике на пунктирную кнопку: отображается состояние "ИИ анализирует задачу..." (380 мс),
 *   после чего плавно раскрывается блок с заготовкой и рекомендацией ИИ.
 * - Кнопка копирования меняется на "Скопировано" на 1.5 секунды.
 * - При смене задачи (новом taskId) панель автоматически сворачивается.
 * - Строго НОЛЬ ЭМОДЗИ.
 */
@Composable
fun OnDemandAiPanel(
    aiDraftContent: String,
    taskId: String?,
    modifier: Modifier = Modifier,
    aiDraftType: String = ""
) {
    if (aiDraftContent.isBlank()) return

    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    var isExpanded by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var isCopied by remember { mutableStateOf(false) }

    // Автоматическое сворачивание при смене карточки
    LaunchedEffect(taskId) {
        isExpanded = false
        isLoading = false
        isCopied = false
    }

    LaunchedEffect(isCopied) {
        if (isCopied) {
            delay(1500)
            isCopied = false
        }
    }

    val triggerInteractionSource = remember { MutableInteractionSource() }
    val isPressed by triggerInteractionSource.collectIsPressedAsState()
    val triggerScale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "aiTriggerScale"
    )

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        // Кнопка-триггер с пунктирной обводкой
        Surface(
            onClick = {
                if (isLoading) return@Surface
                if (isExpanded) {
                    isExpanded = false
                } else {
                    isLoading = true
                    coroutineScope.launch {
                        delay(380)
                        isLoading = false
                        isExpanded = true
                    }
                }
            },
            interactionSource = triggerInteractionSource,
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(
                width = 1.dp,
                color = if (isExpanded) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outlineVariant
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .scale(triggerScale)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ИИ анализирует задачу...",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isExpanded) "Скрыть анализ ИИ" else "Попросить анализ у ИИ",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Раскрывающаяся панель рекомендаций
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            ) + fadeIn(),
            exit = shrinkVertically(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ) + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLowest,
                    border = BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ),
                    tonalElevation = 1.dp,
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "РЕКОМЕНДАЦИЯ И ЗАГОТОВКА ИИ",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.5.sp,
                                    letterSpacing = 0.5.sp
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )

                            // Компактная кнопка Скопировать
                            Button(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(aiDraftContent))
                                    isCopied = true
                                },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 9.dp, vertical = 3.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isCopied) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceContainerHigh,
                                    contentColor = if (isCopied) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurface
                                ),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Icon(
                                    imageVector = if (isCopied) Icons.Rounded.Check else Icons.Rounded.ContentCopy,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isCopied) "Скопировано" else "Скопировать",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 10.5.sp
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Моноширинный блок заготовки от ИИ
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainer)
                                .padding(horizontal = 9.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = aiDraftContent,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.5.sp,
                                    lineHeight = 16.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}
