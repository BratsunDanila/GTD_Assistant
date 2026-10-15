package com.sbros.app.ui.shelves

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Weekend
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sbros.app.domain.model.ProjectWithFirstStep
import com.sbros.app.ui.shelves.components.ProjectDetailsBottomSheet
import com.sbros.app.ui.shelves.components.ShelfCard
import com.sbros.app.ui.theme.ThemeSelectionDialog

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

    val searchQuery by viewModel.searchQuery.collectAsState()
    val isSearchActive by viewModel.isSearchActive.collectAsState()

    val palette by viewModel.themePalette.collectAsState()
    val isDarkPref by viewModel.isDarkMode.collectAsState()
    val isDark = isDarkPref ?: isSystemInDarkTheme()

    var showThemeDialog by remember { mutableStateOf(false) }
    var selectedProjectForDetails by remember { mutableStateOf<ProjectWithFirstStep?>(null) }

    if (showThemeDialog) {
        ThemeSelectionDialog(
            currentPalette = palette,
            isDarkMode = isDark,
            onPaletteSelect = { viewModel.setThemePalette(it) },
            onDarkModeToggle = { viewModel.setDarkMode(it) },
            onDismissRequest = { showThemeDialog = false }
        )
    }

    if (selectedProjectForDetails != null) {
        ProjectDetailsBottomSheet(
            project = selectedProjectForDetails!!,
            onTaskComplete = { taskId ->
                viewModel.onTaskComplete(taskId)
                selectedProjectForDetails = null
            },
            onDismissRequest = { selectedProjectForDetails = null }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Top App Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Полки",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.02).sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Здесь всё лежит в порядке, без дедлайнов",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer
                ) {
                    IconButton(
                        onClick = { viewModel.toggleSearch(!isSearchActive) },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = if (isSearchActive) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = "Поиск",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer
                ) {
                    IconButton(
                        onClick = { showThemeDialog = true },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = "Тема",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

        // Поле поиска (раскрывается по клику)
        AnimatedVisibility(
            visible = isSearchActive,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Column {
                Spacer(modifier = Modifier.height(14.dp))
                TextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = {
                        Text(
                            text = "Поиск по мыслям и делам...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp)),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 4 тональных контейнера полок GTD (человеческий язык)
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Простые шаги
            ShelfCard(
                title = "Простые шаги",
                subtitle = "Одиночные дела без спешки",
                icon = Icons.AutoMirrored.Filled.DirectionsWalk,
                iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
                iconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                tasks = simpleSteps,
                onTaskComplete = { viewModel.onTaskComplete(it) }
            )

            // 2. Большие дела (ТОЛЬКО первый незавершенный шаг)
            ShelfCard(
                title = "Большие дела",
                subtitle = "Спрятано лишнее, оставлен 1-й шаг",
                icon = Icons.Default.Layers,
                iconContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                iconContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                projects = bigThings,
                onTaskComplete = { viewModel.onTaskComplete(it) },
                onProjectClick = { project -> selectedProjectForDetails = project }
            )

            // 3. Жду от других
            ShelfCard(
                title = "Жду от других",
                subtitle = "Дела на паузе, ожидающие других",
                icon = Icons.Default.HourglassTop,
                iconContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                iconContentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                tasks = waiting,
                onTaskComplete = { viewModel.onTaskComplete(it) }
            )

            // 4. На потом и отдых
            ShelfCard(
                title = "На потом и отдых",
                subtitle = "Планы на отдых, идеи и хотелки",
                icon = Icons.Default.Weekend,
                iconContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                iconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                tasks = someday,
                onTaskComplete = { viewModel.onTaskComplete(it) }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
