package com.sbros.app.ui.capture.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

/**
 * Блок живой транскрибации (live-transcript-box) по эталону sbros_v3_swipe.html:
 * - Распознанные задачи подсвечиваются фоном primaryContainer
 * - Эмоциональный шум отображается бледным зачеркнутым текстом
 */
@Composable
fun LiveTranscriptBox(
    transcriptText: String,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val onPrimaryContainer = MaterialTheme.colorScheme.onPrimaryContainer
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    val (annotatedText, recognizedCount) = remember(transcriptText, primaryContainer, onPrimaryContainer, onSurfaceVariant, onSurface) {
        processTranscript(
            raw = transcriptText,
            primaryContainer = primaryContainer,
            onPrimaryContainer = onPrimaryContainer,
            onSurfaceVariant = onSurfaceVariant,
            onSurface = onSurface
        )
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            // Верхняя строка: количество распознанных мыслей + статус
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "РАСПОЗНАНО МЫСЛЕЙ: $recognizedCount",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    ),
                    color = primaryColor
                )

                Text(
                    text = if (transcriptText.isNotBlank()) "В эфире" else "Слушаю...",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (transcriptText.isBlank()) {
                Text(
                    text = "Говори всё подряд: дела, идеи, покупки, переживания... ИИ сразу выделит суть и отсеет шум.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 13.5.sp,
                        lineHeight = 20.sp
                    ),
                    color = onSurfaceVariant.copy(alpha = 0.65f)
                )
            } else {
                Text(
                    text = annotatedText,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp,
                        lineHeight = 22.sp
                    )
                )
            }
        }
    }
}

private fun processTranscript(
    raw: String,
    primaryContainer: Color,
    onPrimaryContainer: Color,
    onSurfaceVariant: Color,
    onSurface: Color
): Pair<androidx.compose.ui.text.AnnotatedString, Int> {
    if (raw.isBlank()) {
        return androidx.compose.ui.text.AnnotatedString("") to 0
    }

    // Регулярные списки ключевых слов для выявления шума и действий
    val noisePhrases = listOf(
        "блин", "как же я устал", "я устал", "устал", "тяжелый день", "тяжёлый день",
        "ничего не успеваю", "капец", "жесть", "сил нет", "не могу больше",
        "кошмар", "ужас", "бесит", "не знаю что делать"
    )

    val actionTriggers = listOf(
        "скинуть", "написать", "сесть за", "купить", "заказать", "отправить",
        "позвонить", "жду", "сделать", "проверить", "напомнить", "договориться",
        "оплатить", "записаться", "посмотреть", "ответить", "забрать"
    )

    var count = 0
    val annotated = buildAnnotatedString {
        // Разбиваем текст на сегменты по запятым, союзам или точкам
        val parts = raw.split(Regex("(?<=[,.!?])\\s+|(?<=\\s)(?=и\\s|потом\\s|а\\s+еще\\s|а\\s+ещё\\s|завтра\\s|также\\s)"))

        for ((index, segment) in parts.withIndex()) {
            val lower = segment.lowercase(Locale.ROOT).trim()

            // 1. Проверяем, является ли сегмент эмоциональным шумом
            val isNoise = noisePhrases.any { lower.contains(it) }

            if (isNoise) {
                withStyle(
                    SpanStyle(
                        color = onSurfaceVariant.copy(alpha = 0.45f),
                        textDecoration = TextDecoration.LineThrough
                    )
                ) {
                    append(segment)
                }
            } else {
                // 2. Проверяем, содержит ли сегмент действие/задачу
                val hasAction = actionTriggers.any { lower.contains(it) } ||
                        lower.contains("старост") || lower.contains("курсач") ||
                        lower.contains("научрук") || lower.contains("аккумулятор")

                if (hasAction) {
                    count++
                    // Выделяем значимую часть задачи фоном primaryContainer
                    withStyle(
                        SpanStyle(
                            background = primaryContainer,
                            color = onPrimaryContainer,
                            fontWeight = FontWeight.SemiBold
                        )
                    ) {
                        append(" $segment ")
                    }
                } else {
                    withStyle(SpanStyle(color = onSurface)) {
                        append(segment)
                    }
                }
            }

            if (index < parts.size - 1 && !segment.endsWith(" ")) {
                append(" ")
            }
        }
    }

    return annotated to maxOf(count, if (raw.length > 10) 1 else 0)
}
