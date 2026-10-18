package com.sbros.app.ui.now

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Help
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sbros.app.domain.model.TaskDifficulty
import com.sbros.app.ui.now.components.CaptureDock
import com.sbros.app.ui.now.components.DecomposeBottomSheet
import com.sbros.app.ui.now.components.FocusTaskCard
import com.sbros.app.ui.now.components.OnDemandAiPanel
import com.sbros.app.ui.theme.ThemeSelectionDialog
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowScreen(
    viewModel: NowViewModel,
    onVoiceCaptureClick: () -> Unit,
    onReviewCaptureClick: () -> Unit,
    onHelpClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedDifficulty by viewModel.selectedDifficulty.collectAsState()
    val focusTask by viewModel.currentFocusTask.collectAsState()
    val currentTasksList by viewModel.currentTasksList.collectAsState()
    val showFeedback by viewModel.showCompletionFeedback.collectAsState()
    val userName by viewModel.userName.collectAsState()
    val palette by viewModel.themePalette.collectAsState()
    val isDarkPref by viewModel.isDarkMode.collectAsState()
    val isDark = isDarkPref ?: androidx.compose.foundation.isSystemInDarkTheme()

    val remainingInStack = currentTasksList.size
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var showThemeDialog by remember { mutableStateOf(false) }
    var showDecomposeSheet by remember { mutableStateOf(false) }

    if (showThemeDialog) {
        ThemeSelectionDialog(
            currentPalette = palette,
            isDarkMode = isDark,
            onPaletteSelect = { viewModel.setThemePalette(it) },
            onDarkModeToggle = { viewModel.setDarkMode(it) },
            onDismissRequest = { showThemeDialog = false }
        )
    }

    if (showDecomposeSheet && focusTask != null) {
        DecomposeBottomSheet(
            task = focusTask!!,
            onDismissRequest = { showDecomposeSheet = false },
            onAiDecompose = { viewModel.generateAiSubsteps(focusTask!!.id) },
            onAddManualStep = { viewModel.addManualSubstep(focusTask!!.id, it) },
            onRemoveStep = { viewModel.removeSubstep(focusTask!!.id, it) },
            onToggleStep = { viewModel.toggleSubstep(focusTask!!.id, it) }
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(14.dp))

                // 1. Top Header: Приветствие + иконки помощи и темы
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Привет, $userName",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp,
                                letterSpacing = (-0.02).sp
                            ),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Вправо — сделано, влево — потом",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            IconButton(
                                onClick = onHelpClick,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Help,
                                    contentDescription = "Как это работает",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            IconButton(
                                onClick = { showThemeDialog = true },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Palette,
                                    contentDescription = "Палитра оформления",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 2. Градация сложности (3 чипа)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CognitiveLoadChip(
                        selected = selectedDifficulty == TaskDifficulty.EASY,
                        onClick = { viewModel.selectDifficulty(TaskDifficulty.EASY) },
                        label = "Просто",
                        icon = Icons.Rounded.Bolt,
                        modifier = Modifier.weight(1f)
                    )

                    CognitiveLoadChip(
                        selected = selectedDifficulty == TaskDifficulty.MEDIUM,
                        onClick = { viewModel.selectDifficulty(TaskDifficulty.MEDIUM) },
                        label = "Средне",
                        icon = Icons.Rounded.Speed,
                        modifier = Modifier.weight(1f)
                    )

                    CognitiveLoadChip(
                        selected = selectedDifficulty == TaskDifficulty.HARD,
                        onClick = { viewModel.selectDifficulty(TaskDifficulty.HARD) },
                        label = "Сложно",
                        icon = Icons.Rounded.Psychology,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 3. Мета-строка стопки: контекст + счетчик
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Контекст: Все дела",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val countScale = remember { Animatable(1f) }
                    LaunchedEffect(remainingInStack) {
                        countScale.animateTo(
                            targetValue = 1.15f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            )
                        )
                        countScale.animateTo(
                            targetValue = 1f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            )
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(99.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.scale(countScale.value)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Style,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "В стопке: $remainingInStack",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.5.sp
                                ),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                // 4. Центральная 3D колода карточек
                FocusTaskCard(
                    task = focusTask,
                    showCompletionFeedback = showFeedback,
                    onSkip = { viewModel.onSkipClicked() },
                    onComplete = { viewModel.onCompleteClicked() },
                    onDecomposeClick = { showDecomposeSheet = true },
                    onToggleSubstep = { index ->
                        focusTask?.let { viewModel.toggleSubstep(it.id, index) }
                    },
                    onSimplify = { viewModel.onSimplifyClicked() },
                    remainingInStack = remainingInStack
                )

                // 5. Ленивый анализ ИИ по требованию (по клику)
                if (focusTask != null && !showFeedback && focusTask?.aiDraftContent?.isNotBlank() == true) {
                    OnDemandAiPanel(
                        aiDraftContent = focusTask?.aiDraftContent.orEmpty(),
                        taskId = focusTask?.id,
                        aiDraftType = focusTask?.aiDraftType.orEmpty()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Spacer(modifier = Modifier.height(10.dp))
            }

            // 7. Док сброса мыслей над нижней навигацией
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                CaptureDock(
                    onVoiceCaptureClick = onVoiceCaptureClick,
                    onReviewClick = onReviewCaptureClick
                )
                Spacer(modifier = Modifier.height(14.dp))
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 76.dp, start = 16.dp, end = 16.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CognitiveLoadChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "chipScale"
    )

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
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(15.dp)
            )
        },
        shape = RoundedCornerShape(12.dp),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surface,
            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
            selectedLeadingIconColor = MaterialTheme.colorScheme.onSecondaryContainer
        ),
        border = BorderStroke(
            1.dp,
            if (selected) Color.Transparent else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = modifier
            .height(36.dp)
            .scale(scale)
    )
}
