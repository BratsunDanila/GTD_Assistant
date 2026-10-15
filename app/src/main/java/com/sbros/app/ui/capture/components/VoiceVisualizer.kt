package com.sbros.app.ui.capture.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp

/**
 * Гармонический звуковой визуализатор v3.0 (взамен угловатого MorphingVoiceOrb):
 * - Два мягких концентрических круга, плавно пульсирующих (масштаб от 1f до 1.28f)
 * - Скругленный квадрат (82.dp, RoundedCornerShape(28.dp), цвет primary)
 * - 5 вертикальных полосок эквалайзера (eq-pill), плавно меняющих высоту от голоса
 */
@Composable
fun VoiceVisualizer(
    amplitude: Float,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "voiceVisualizerTransition")

    // Внешнее кольцо: масштаб 1.0f -> 1.28f, плавное затухание прозрачности
    val outerScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "outerScale"
    )
    val outerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.22f,
        targetValue = 0.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "outerAlpha"
    )

    // Среднее кольцо: масштаб 1.0f -> 1.15f
    val midScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "midScale"
    )
    val midAlpha by infiniteTransition.animateFloat(
        initialValue = 0.40f,
        targetValue = 0.16f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "midAlpha"
    )

    // Плавная амплитуда микрофона
    val smoothAmplitude by animateFloatAsState(
        targetValue = amplitude.coerceIn(0f, 1f),
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 380f),
        label = "smoothAmplitude"
    )

    // Анимации 5 полосок эквалайзера со сдвигом фаз
    val eq1 by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 0.95f,
        animationSpec = infiniteRepeatable(tween(750, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "eq1"
    )
    val eq2 by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(880, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "eq2"
    )
    val eq3 by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(670, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "eq3"
    )
    val eq4 by infiniteTransition.animateFloat(
        initialValue = 0.25f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(920, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "eq4"
    )
    val eq5 by infiniteTransition.animateFloat(
        initialValue = 0.35f, targetValue = 0.85f,
        animationSpec = infiniteRepeatable(tween(790, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "eq5"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val onPrimaryColor = MaterialTheme.colorScheme.onPrimary

    Box(
        modifier = modifier
            .size(150.dp),
        contentAlignment = Alignment.Center
    ) {
        // Внешнее мягкое кольцо (136.dp)
        Box(
            modifier = Modifier
                .size(136.dp)
                .scale(outerScale + smoothAmplitude * 0.15f)
                .clip(CircleShape)
                .background(primaryColor.copy(alpha = outerAlpha))
        )

        // Среднее мягкое кольцо (108.dp)
        Box(
            modifier = Modifier
                .size(108.dp)
                .scale(midScale + smoothAmplitude * 0.10f)
                .clip(CircleShape)
                .background(primaryContainer.copy(alpha = midAlpha))
        )

        // Центральный скругленный квадрат (82.dp, RoundedCornerShape(28.dp), цвет primary)
        Box(
            modifier = Modifier
                .size(82.dp)
                .shadow(elevation = 8.dp, shape = RoundedCornerShape(28.dp), spotColor = primaryColor.copy(alpha = 0.4f))
                .clip(RoundedCornerShape(28.dp))
                .background(primaryColor),
            contentAlignment = Alignment.Center
        ) {
            // 5 вертикальных полосок эквалайзера (eq-pill)
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val heights = listOf(eq1, eq2, eq3, eq4, eq5).map { baseBounce ->
                    // Базовая высота 8.dp .. 28.dp, реагирующая на реальный голос
                    val dynamicFactor = 0.35f + 0.65f * smoothAmplitude
                    val h = (8f + 20f * baseBounce * dynamicFactor).coerceIn(6f, 32f)
                    h.dp
                }

                heights.forEach { barHeight ->
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(barHeight)
                            .clip(RoundedCornerShape(99.dp))
                            .background(onPrimaryColor)
                    )
                }
            }
        }
    }
}
