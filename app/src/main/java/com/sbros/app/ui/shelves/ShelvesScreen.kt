package com.sbros.app.ui.shelves

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Label
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sbros.app.domain.model.TaskItem

/**
 * Экран «Полки» v8.0 (по спецификации sbros_v8.html):
 * - Верхняя панель фильтрации по сложности: Все, Просто, Средне, Сложно
 * - Смысловые аккордеоны полок по контекстам ИИ (Учеба и вуз, Покупки и заказы, Разработка и код, Дом и быт)
 * - Счетчик задач в бейдже
 * - Чекбоксы выполнения прямо в списке с зачеркиванием текста
 * - Бейдж уровня сложности задачи (Просто, Средне, Сложно)
 * - Строго НОЛЬ ЭМОДЗИ!
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShelvesScreen(
    modifier: Modifier = Modifier,
    viewModel: ShelvesViewModel = hiltViewModel()
) {
    val selectedDifficulty by viewModel.selectedDifficulty.collectAsState()
    val contextShelves by viewModel.contextShelves.collectAsState()

    // Состояние развернутых аккордеонов (по умолчанию открыты, кроме "Дом и быт")
    val openState = remember {
        mutableStateMapOf<String, Boolean>().apply {
            put("Учеба и вуз", true)
            put("Покупки и заказы", true)
            put("Разработка и код", true)
            put("Дом и быт", false)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 18.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(14.dp))

        // Заголовок экрана
        Column {
            Text(
                text = "Полки по контекстам",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    letterSpacing = (-0.02).sp
                ),
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "ИИ сгруппировал задачи по сферам жизни",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 1. Верхний ряд фильтрации по сложности: Все / Просто / Средне / Сложно
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            DifficultyChip(
                label = "Все",
                selected = selectedDifficulty == "all",
                onClick = { viewModel.setDifficultyFilter("all") },
                modifier = Modifier.weight(1f)
            )
            DifficultyChip(
                label = "Просто",
                selected = selectedDifficulty == "EASY",
                onClick = { viewModel.setDifficultyFilter("EASY") },
                modifier = Modifier.weight(1f)
            )
            DifficultyChip(
                label = "Средне",
                selected = selectedDifficulty == "MEDIUM",
                onClick = { viewModel.setDifficultyFilter("MEDIUM") },
                modifier = Modifier.weight(1f)
            )
            DifficultyChip(
                label = "Сложно",
                selected = selectedDifficulty == "HARD",
                onClick = { viewModel.setDifficultyFilter("HARD") },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 2. Список контекстных полок (аккордеонов)
        if (contextShelves.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
            ) {
                Box(modifier = Modifier.padding(20.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Все активные задачи в этом фильтре выполнены!",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            contextShelves.forEach { (category, tasks) ->
                val isOpen = openState[category] ?: true
                val icon = getContextIcon(category)

                ContextShelfAccordion(
                    title = category,
                    icon = icon,
                    count = tasks.size,
                    isOpen = isOpen,
                    onToggle = { openState[category] = !isOpen },
                    modifier = Modifier.padding(bottom = 9.dp)
                ) {
                    if (tasks.isEmpty()) {
                        EmptyShelfNotice()
                    } else {
                        tasks.forEach { task ->
                            ContextTaskRow(
                                task = task,
                                onTaskClick = { viewModel.toggleTaskCompletion(task) },
                                onToggleSubstep = { taskId, stepIdx -> viewModel.toggleSubstep(taskId, stepIdx) }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DifficultyChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }

    FilterChip(
        selected = selected,
        onClick = onClick,
        interactionSource = interactionSource,
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 11.5.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold
                ),
                maxLines = 1,
                softWrap = false
            )
        },
        shape = RoundedCornerShape(12.dp),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surface,
            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
        ),
        border = BorderStroke(
            1.dp,
            if (selected) Color.Transparent else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = modifier.height(36.dp)
    )
}

@Composable
private fun ContextShelfAccordion(
    title: String,
    icon: ImageVector,
    count: Int,
    isOpen: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() }
                    .padding(horizontal = 14.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(99.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Text(
                            text = "$count",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.5.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }

                    Icon(
                        imageVector = if (isOpen) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            AnimatedVisibility(
                visible = isOpen,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                        .padding(bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    content()
                }
            }
        }
    }
}

@Composable
private fun ContextTaskRow(
    task: TaskItem,
    onTaskClick: () -> Unit,
    onToggleSubstep: (taskId: String, stepIndex: Int) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onTaskClick() },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    // Круглый чекбокс выполнения
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(if (task.isCompleted) Color(0xFF1E6B43) else Color.Transparent)
                            .then(
                                if (!task.isCompleted) {
                                    Modifier.background(
                                        Color.Transparent,
                                        CircleShape
                                    )
                                } else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (task.isCompleted) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(11.dp)
                            )
                        } else {
                            Surface(
                                modifier = Modifier.fillMaxSize(),
                                shape = CircleShape,
                                color = Color.Transparent,
                                border = BorderStroke(1.8.dp, MaterialTheme.colorScheme.onSurfaceVariant)
                            ) {}
                        }
                    }

                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                        color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        else MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Бейдж уровня сложности
                Surface(
                    shape = RoundedCornerShape(99.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Text(
                        text = task.difficulty.label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }

            // Вложенные подпункты (substeps)
            if (task.substeps.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    task.substeps.forEachIndexed { sIdx, st ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { onToggleSubstep(task.id, sIdx) }
                                .padding(vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (st.isDone) Color(0xFF1E6B43) else Color.Transparent,
                                border = BorderStroke(
                                    1.5.dp,
                                    if (st.isDone) Color(0xFF1E6B43) else MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier.size(13.dp)
                            ) {
                                if (st.isDone) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(9.dp)
                                    )
                                }
                            }

                            Text(
                                text = st.text,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Normal,
                                    textDecoration = if (st.isDone) TextDecoration.LineThrough else null
                                ),
                                color = if (st.isDone) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyShelfNotice() {
    Text(
        text = "Нет задач в этом контексте",
        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

private fun getContextIcon(category: String): ImageVector {
    return when {
        category.contains("учеб", ignoreCase = true) || category.contains("вуз", ignoreCase = true) -> Icons.Rounded.School
        category.contains("покуп", ignoreCase = true) || category.contains("заказ", ignoreCase = true) -> Icons.Rounded.ShoppingBag
        category.contains("разработ", ignoreCase = true) || category.contains("код", ignoreCase = true) -> Icons.Rounded.Code
        category.contains("дом", ignoreCase = true) || category.contains("быт", ignoreCase = true) -> Icons.Rounded.Home
        else -> Icons.AutoMirrored.Rounded.Label
    }
}
