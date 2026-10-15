package com.sbros.app.ui.capture.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val GtdMemoryTriggers = listOf(
    "«Есть ли что-то по учебе или работе, к чему страшно приступить?»",
    "«Кому ты обещал написать или скинуть файл на этой неделе?»",
    "«Что нужно купить из бытовых мелочей в ближайшие дни?»",
    "«От кого ты сейчас ждешь ответа или проверки?»",
    "«Какой фильм, книгу или идею хотелось сохранить на потом?»"
)

/**
 * Спокойная карточка-триггер памяти GTD, появляющаяся при паузе в речи свыше 3 секунд.
 */
@Composable
fun GentlePromptCard(
    modifier: Modifier = Modifier,
    prompts: List<String> = GtdMemoryTriggers,
    onPromptClick: ((String) -> Unit)? = null
) {
    var currentIndex by remember { mutableIntStateOf(0) }
    val currentPrompt = prompts[currentIndex % prompts.size]

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable {
                currentIndex = (currentIndex + 1) % prompts.size
                onPromptClick?.invoke(prompts[currentIndex % prompts.size])
            },
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Пауза в речи • Подсказка (нажми для смены):",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            AnimatedContent(
                targetState = currentPrompt,
                transitionSpec = {
                    (slideInVertically { height -> height / 2 } + fadeIn()) togetherWith
                            (slideOutVertically { height -> -height / 2 } + fadeOut())
                },
                label = "promptTextAnimation"
            ) { promptText ->
                Text(
                    text = promptText,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
