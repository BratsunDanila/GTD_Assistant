package com.sbros.app.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.WbTwilight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sbros.app.ui.capture.CaptureBottomSheet
import com.sbros.app.ui.capture.CaptureMode
import com.sbros.app.ui.now.NowScreen
import com.sbros.app.ui.now.NowViewModel
import com.sbros.app.ui.shelves.ShelvesScreen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SbrosAppShell(
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showCaptureSheet by remember { mutableStateOf(false) }
    var captureMode by remember { mutableStateOf(CaptureMode.VOICE) }
    var captureStep by remember { mutableStateOf(com.sbros.app.ui.capture.CaptureStep.RECORDING) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val nowViewModel: NowViewModel = hiltViewModel()

    val hasCompletedOnboarding by nowViewModel.hasCompletedOnboarding.collectAsState()

    if (hasCompletedOnboarding == false) {
        com.sbros.app.ui.welcome.WelcomeScreen(
            onStartClick = { nowViewModel.completeOnboarding() }
        )
        return
    }

    if (showCaptureSheet) {
        CaptureBottomSheet(
            initialMode = captureMode,
            initialStep = captureStep,
            onDismissRequest = { showCaptureSheet = false },
            onParseTranscript = { rawText ->
                nowViewModel.parseTranscript(rawText)
            },
            onSaveParsedTasks = { items ->
                showCaptureSheet = false
                nowViewModel.saveParsedTasks(items)
                scope.launch {
                    val result = snackbarHostState.showSnackbar(
                        message = "Мысли тихо разложены по полкам",
                        actionLabel = "Полки",
                        duration = SnackbarDuration.Short
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        selectedTab = 1
                    }
                }
            }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                tonalElevation = 0.dp
            ) {
                // Вкладка 1: Сейчас
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == 0) Icons.Filled.WbTwilight else Icons.Outlined.WbTwilight,
                            contentDescription = "Сейчас"
                        )
                    },
                    label = {
                        Text(
                            text = "Сейчас",
                            fontWeight = if (selectedTab == 0) FontWeight.SemiBold else FontWeight.Normal
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                        selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.onSurface,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                // Вкладка 2: Полки
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == 1) Icons.Filled.Inventory2 else Icons.Outlined.Inventory2,
                            contentDescription = "Полки"
                        )
                    },
                    label = {
                        Text(
                            text = "Полки",
                            fontWeight = if (selectedTab == 1) FontWeight.SemiBold else FontWeight.Normal
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                        selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.onSurface,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> {
                    NowScreen(
                        viewModel = nowViewModel,
                        onVoiceCaptureClick = {
                            captureMode = CaptureMode.VOICE
                            showCaptureSheet = true
                        },
                        onTextCaptureClick = {
                            captureMode = CaptureMode.TEXT
                            showCaptureSheet = true
                        }
                    )
                }
                1 -> {
                    ShelvesScreen()
                }
            }
        }
    }
}
