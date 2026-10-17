package com.sbros.app.ui.shelves

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.rounded.AccountTree
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.LaptopMac
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sbros.app.domain.model.TaskItem

/**
 * Экран «Полки GTD» v5.0 (по спецификации sbros_production_m3.html):
 * - Фильтр контекстов GTD: Все, Ноутбук, Телефон, Дом, Покупки
 * - 5 раскрывающихся аккордеонов полок:
 *   1. Простые шаги (Checklist)
 *   2. Большие дела (AccountTree)
 *   3. Жду ответа (HourglassTop)
 *   4. На потом и идеи (Bookmark)
 *   5. Справочник и заметки (Description)
 * - Круглый чекбокс выполнения задач прямо на полках
 * - Строго НОЛЬ ЭМОДЗИ!
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShelvesScreen(
    modifier: Modifier = Modifier,
    viewModel: ShelvesViewModel = hiltViewModel()
) {
    val simpleSteps by viewModel.simpleSteps.collectAsState()
    val bigThings by viewModel.bigThings.collectAsState()
    val waiting by viewModel.waiting.collectAsState()
    val someday by viewModel.someday.collectAsState()
    val reference by viewModel.reference.collectAsState()
    val selectedContext by viewModel.selectedContext.collectAsState()

    var openSimple by remember { mutableStateOf(true) }
    var openProjects by remember { mutableStateOf(true) }
    var openWaiting by remember { mutableStateOf(false) }
    var openSomeday by remember { mutableStateOf(false) }
    var openReference by remember { mutableStateOf(false) }

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
                text = "Полки GTD",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    letterSpacing = (-0.02).sp
                ),
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Вся система в порядке и без давления",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Горизонтальный фильтр контекстов GTD
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            ContextPill(
                label = "Все",
                icon = null,
                isSelected = selectedContext == "all",
                onClick = { viewModel.setContext("all") }
            )
            ContextPill(
                label = "Ноутбук",
                icon = Icons.Rounded.LaptopMac,
                isSelected = selectedContext == "pc",
                onClick = { viewModel.setContext("pc") }
            )
            ContextPill(
                label = "Телефон",
                icon = Icons.Rounded.Smartphone,
                isSelected = selectedContext == "phone",
                onClick = { viewModel.setContext("phone") }
            )
            ContextPill(
                label = "Дом",
                icon = Icons.Rounded.Home,
                isSelected = selectedContext == "home",
                onClick = { viewModel.setContext("home") }
            )
            ContextPill(
                label = "Покупки",
                icon = Icons.Rounded.ShoppingBag,
                isSelected = selectedContext == "out",
                onClick = { viewModel.setContext("out") }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Полка 1: Простые шаги
        ShelfAccordion(
            title = "Простые шаги",
            icon = Icons.Rounded.Checklist,
            count = simpleSteps.size,
            isOpen = openSimple,
            onToggle = { openSimple = !openSimple }
        ) {
            if (simpleSteps.isEmpty()) {
                EmptyShelfNotice()
            } else {
                simpleSteps.forEach { task ->
                    ShelfTaskRow(
                        title = task.title,
                        isDone = task.isCompleted,
                        onClick = { viewModel.onTaskComplete(task.id) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Полка 2: Большие дела (Проекты)
        val projectTasks = bigThings.mapNotNull { it.firstUncompletedStep }
        ShelfAccordion(
            title = "Большие дела (Проекты)",
            icon = Icons.Rounded.AccountTree,
            count = bigThings.size,
            isOpen = openProjects,
            onToggle = { openProjects = !openProjects }
        ) {
            if (bigThings.isEmpty()) {
                EmptyShelfNotice()
            } else {
                bigThings.forEach { proj ->
                    val stepTitle = proj.firstUncompletedStep?.title ?: proj.projectTitle
                    val display = "${proj.projectTitle}: $stepTitle"
                    ShelfTaskRow(
                        title = display,
                        isDone = proj.firstUncompletedStep == null || proj.firstUncompletedStep.isCompleted,
                        onClick = {
                            proj.firstUncompletedStep?.let { viewModel.onTaskComplete(it.id) }
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Полка 3: Жду ответа
        ShelfAccordion(
            title = "Жду ответа",
            icon = Icons.Rounded.HourglassTop,
            count = waiting.size,
            isOpen = openWaiting,
            onToggle = { openWaiting = !openWaiting }
        ) {
            if (waiting.isEmpty()) {
                EmptyShelfNotice()
            } else {
                waiting.forEach { task ->
                    ShelfTaskRow(
                        title = task.title,
                        isDone = task.isCompleted,
                        onClick = { viewModel.onTaskComplete(task.id) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Полка 4: На потом и идеи
        ShelfAccordion(
            title = "На потом и идеи",
            icon = Icons.Rounded.Bookmark,
            count = someday.size,
            isOpen = openSomeday,
            onToggle = { openSomeday = !openSomeday }
        ) {
            if (someday.isEmpty()) {
                EmptyShelfNotice()
            } else {
                someday.forEach { task ->
                    ShelfTaskRow(
                        title = task.title,
                        isDone = task.isCompleted,
                        onClick = { viewModel.onTaskComplete(task.id) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Полка 5: Справочник и заметки
        ShelfAccordion(
            title = "Справочник и заметки",
            icon = Icons.Rounded.Description,
            count = reference.size,
            isOpen = openReference,
            onToggle = { openReference = !openReference }
        ) {
            if (reference.isEmpty()) {
                EmptyShelfNotice()
            } else {
                reference.forEach { task ->
                    ShelfTaskRow(
                        title = task.title,
                        isDone = task.isCompleted,
                        onClick = { viewModel.onTaskComplete(task.id) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun ContextPill(
    label: String,
    icon: ImageVector?,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(99.dp),
        color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (isSelected) Color.Transparent else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier.height(30.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(13.dp)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    fontSize = 11.sp
                ),
                color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ShelfAccordion(
    title: String,
    icon: ImageVector,
    count: Int,
    isOpen: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                        color = MaterialTheme.colorScheme.onSurface
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
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    content()
                }
            }
        }
    }
}

@Composable
private fun ShelfTaskRow(
    title: String,
    isDone: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(if (isDone) Color(0xFF1E6B43) else Color.Transparent)
                    .then(
                        if (!isDone) Modifier.clip(CircleShape) else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isDone) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(11.dp)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(Color.Transparent)
                            .then(
                                Modifier.clip(CircleShape)
                            )
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            shape = CircleShape,
                            color = Color.Transparent,
                            border = BorderStroke(1.8.dp, MaterialTheme.colorScheme.onSurfaceVariant)
                        ) {}
                    }
                }
            }

            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium
                ),
                textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None,
                color = if (isDone) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun EmptyShelfNotice() {
    Text(
        text = "Нет задач для выбранного контекста",
        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}
