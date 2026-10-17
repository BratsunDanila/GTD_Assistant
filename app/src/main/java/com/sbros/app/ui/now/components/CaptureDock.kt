package com.sbros.app.ui.now.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FactCheck
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CaptureDock(
    onVoiceCaptureClick: () -> Unit,
    onReviewClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Главная кнопка голосового сброса
        Surface(
            modifier = Modifier
                .weight(1f)
                .height(54.dp)
                .clip(RoundedCornerShape(18.dp))
                .clickable { onVoiceCaptureClick() },
            color = MaterialTheme.colorScheme.primary,
            shape = RoundedCornerShape(18.dp),
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Mic,
                    contentDescription = "Голосовой сброс",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Сбросить мысли",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp
                        ),
                        color = MaterialTheme.colorScheme.onPrimary,
                        maxLines = 1
                    )
                    Text(
                        text = "Скажи всё подряд — ИИ разложит",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp),
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }

        // Вторичная кнопка предпросмотра/разбора
        Surface(
            modifier = Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(18.dp))
                .clickable { onReviewClick() },
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.FactCheck,
                    contentDescription = "Проверить разбор ИИ",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
