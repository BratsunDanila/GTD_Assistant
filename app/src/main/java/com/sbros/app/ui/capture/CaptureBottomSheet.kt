package com.sbros.app.ui.capture

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountTree
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FactCheck
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.sbros.app.domain.model.EditableParsedItem
import com.sbros.app.domain.model.EnergyLevel
import com.sbros.app.domain.model.ShelfType
import com.sbros.app.ui.capture.components.GentlePromptCard
import com.sbros.app.ui.capture.components.LiveTranscriptBox
import com.sbros.app.ui.capture.components.VoiceVisualizer
import kotlinx.coroutines.launch

enum class CaptureMode {
    VOICE,
    TEXT
}

enum class CaptureStep {
    RECORDING,
    REVIEW
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptureBottomSheet(
    initialMode: CaptureMode,
    initialStep: CaptureStep = CaptureStep.RECORDING,
    onDismissRequest: () -> Unit,
    onParseTranscript: suspend (String) -> List<EditableParsedItem>,
    onSaveParsedTasks: (List<EditableParsedItem>) -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var mode by remember { mutableStateOf(initialMode) }
    var currentStep by remember { mutableStateOf(initialStep) }
    var textInput by remember { mutableStateOf("") }
    var parsedItems by remember { mutableStateOf<List<EditableParsedItem>>(emptyList()) }
    var isLoadingReview by remember { mutableStateOf(false) }

    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val speechHelper = remember { SpeechRecognizerHelper(context, scope) }
    val amplitude by speechHelper.amplitude.collectAsState()
    val transcriptText by speechHelper.transcriptText.collectAsState()
    val serviceUnavailable by speechHelper.serviceUnavailable.collectAsState()
    val permissionDenied by speechHelper.permissionDenied.collectAsState()
    val errorMessage by speechHelper.errorMessage.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasAudioPermission = isGranted
        if (isGranted && mode == CaptureMode.VOICE && currentStep == CaptureStep.RECORDING) {
            speechHelper.startListening()
        }
    }

    // Если открыли сразу экран REVIEW (например, по тапу на клавиатуру)
    LaunchedEffect(initialStep) {
        if (initialStep == CaptureStep.REVIEW) {
            val items = onParseTranscript("")
            parsedItems = items
        }
    }

    LaunchedEffect(mode, currentStep, hasAudioPermission) {
        if (currentStep == CaptureStep.RECORDING && mode == CaptureMode.VOICE) {
            if (hasAudioPermission) {
                speechHelper.startListening()
            }
        } else {
            speechHelper.stopListening()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            speechHelper.stopListening()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier
    ) {
        AnimatedContent(
            targetState = currentStep,
            transitionSpec = {
                fadeIn() togetherWith fadeOut()
            },
            label = "captureStepTransition"
        ) { step ->
            when (step) {
                // =============================================================
                // ФАЗА 1: ЗАПИСЬ РЕЧИ (Голос / Текст + Live Transcript + Visualizer)
                // =============================================================
                CaptureStep.RECORDING -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 22.dp, vertical = 8.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Шапка шторки: заголовок + переключатель режима
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp)
                            ) {
                                Text(
                                    text = if (mode == CaptureMode.VOICE) "Говори свободно" else "Сброс текстом",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )
                                Text(
                                    text = if (mode == CaptureMode.VOICE) "ИИ слушает и сразу выделяет дела" else "Выгрузи мысли потоком текста",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            FilledTonalButton(
                                onClick = {
                                    mode = if (mode == CaptureMode.VOICE) CaptureMode.TEXT else CaptureMode.VOICE
                                },
                                shape = CircleShape,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .height(34.dp)
                                    .wrapContentWidth(unbounded = false)
                            ) {
                                Icon(
                                    imageVector = if (mode == CaptureMode.VOICE) Icons.Rounded.Keyboard else Icons.Rounded.Mic,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (mode == CaptureMode.VOICE) "Текстом" else "Голосом",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        when (mode) {
                            CaptureMode.VOICE -> {
                                when {
                                    // 1. Нет разрешения на микрофон
                                    !hasAudioPermission || permissionDenied -> {
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 16.dp),
                                            shape = RoundedCornerShape(24.dp),
                                            color = MaterialTheme.colorScheme.surfaceContainerLow
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(20.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Surface(
                                                    modifier = Modifier.size(52.dp),
                                                    shape = CircleShape,
                                                    color = MaterialTheme.colorScheme.primaryContainer
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(
                                                            imageVector = Icons.Rounded.MicOff,
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                            modifier = Modifier.size(26.dp)
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(14.dp))
                                                Text(
                                                    text = "Нужен доступ к микрофону",
                                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text(
                                                    text = "Чтобы надиктовывать мысли вслух, приложению требуется разрешение на запись аудио.",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    textAlign = TextAlign.Center
                                                )
                                                Spacer(modifier = Modifier.height(18.dp))
                                                Button(
                                                    onClick = {
                                                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                                    },
                                                    shape = RoundedCornerShape(16.dp),
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = MaterialTheme.colorScheme.primary,
                                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                                    )
                                                ) {
                                                    Text("Разрешить микрофон")
                                                }
                                            }
                                        }
                                    }

                                    // 2. Сервис распознавания недоступен
                                    serviceUnavailable -> {
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 12.dp),
                                            shape = RoundedCornerShape(24.dp),
                                            color = MaterialTheme.colorScheme.surfaceContainerLow
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(18.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Text(
                                                    text = "Распознавание речи не настроено",
                                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text(
                                                    text = "На китайских прошивках (Xiaomi / HyperOS, Vivo, Realme, Honor) нужно выбрать Google в системных сервисах голосового ввода.",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    textAlign = TextAlign.Center
                                                )
                                                Spacer(modifier = Modifier.height(14.dp))
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Button(
                                                        onClick = {
                                                            try {
                                                                val intent = Intent(Settings.ACTION_VOICE_INPUT_SETTINGS).apply {
                                                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                                }
                                                                context.startActivity(intent)
                                                            } catch (_: Exception) {}
                                                        },
                                                        modifier = Modifier.weight(1f),
                                                        shape = RoundedCornerShape(16.dp)
                                                    ) {
                                                        Text("Настройки", fontSize = 13.sp)
                                                    }

                                                    FilledTonalButton(
                                                        onClick = { mode = CaptureMode.TEXT },
                                                        modifier = Modifier.weight(1f),
                                                        shape = RoundedCornerShape(16.dp)
                                                    ) {
                                                        Text("Ввести текстом", fontSize = 13.sp)
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // 3. Штатный режим: Звуковой визуализатор + Живой поток
                                    else -> {
                                        // Новый гармонический визуализатор v3.0
                                        VoiceVisualizer(
                                            amplitude = amplitude,
                                            modifier = Modifier.padding(vertical = 6.dp)
                                        )

                                        Spacer(modifier = Modifier.height(10.dp))

                                        // Блок живой транскрибации с выделением задач и зачеркиванием шума
                                        LiveTranscriptBox(
                                            transcriptText = transcriptText,
                                            modifier = Modifier.padding(bottom = 10.dp)
                                        )

                                        // Предупреждение об ошибке
                                        if (errorMessage != null) {
                                            Surface(
                                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                                                shape = RoundedCornerShape(16.dp),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(bottom = 10.dp)
                                            ) {
                                                Text(
                                                    text = errorMessage ?: "",
                                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                                    textAlign = TextAlign.Center,
                                                    modifier = Modifier.padding(10.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            CaptureMode.TEXT -> {
                                TextField(
                                    value = textInput,
                                    onValueChange = { textInput = it },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 160.dp)
                                        .clip(RoundedCornerShape(20.dp)),
                                    placeholder = {
                                        Text(
                                            text = "Пиши сплошным потоком: надо купить батарейки сделать курсач в пятницу кино скинуть тему старосте...",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        )
                                    },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                                        focusedBorderColor = Color.Transparent,
                                        unfocusedBorderColor = Color.Transparent
                                    ),
                                    shape = RoundedCornerShape(20.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }

                        // Мягкий GTD-вопрос-подсказка
                        GentlePromptCard(
                            modifier = Modifier.padding(bottom = 14.dp)
                        )

                        // Кнопки перехода к ИИ-разбору (Фаза 2)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilledTonalButton(
                                onClick = onDismissRequest,
                                shape = CircleShape,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Text("Отмена", style = MaterialTheme.typography.labelLarge)
                            }

                            Button(
                                onClick = {
                                    scope.launch {
                                        isLoadingReview = true
                                        val raw = if (mode == CaptureMode.VOICE) transcriptText else textInput
                                        speechHelper.stopListening()
                                        val items = onParseTranscript(raw)
                                        parsedItems = items
                                        isLoadingReview = false
                                        currentStep = CaptureStep.REVIEW
                                    }
                                },
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                enabled = !isLoadingReview,
                                modifier = Modifier
                                    .weight(2f)
                                    .height(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isLoadingReview) "Разбираю..." else "Посмотреть разбор",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                // =============================================================
                // ФАЗА 2: ЭКРАН ПРЕДПРОСМОТРА И РЕДАКТИРОВАНИЯ ИИ-РАЗБОРА
                // =============================================================
                CaptureStep.REVIEW -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Верхняя строка фазы 2
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "ИИ разложил на ${parsedItems.size} полки",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Нажми на текст или полку, если хочешь подправить",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "Готово",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.5.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Список карточек разбора
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            parsedItems.forEachIndexed { index, item ->
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // Верхний ряд карточки: переключатель полки + переключатель энергии + кнопка удаления
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val shelfIcon: androidx.compose.ui.graphics.vector.ImageVector
                                            val shelfLabel: String
                                            when (item.shelf) {
                                                ShelfType.SIMPLE_STEPS -> {
                                                    shelfIcon = Icons.Rounded.Checklist
                                                    shelfLabel = "Простые шаги"
                                                }
                                                ShelfType.BIG_THINGS -> {
                                                    shelfIcon = Icons.Rounded.AccountTree
                                                    shelfLabel = "Большие дела"
                                                }
                                                ShelfType.WAITING -> {
                                                    shelfIcon = Icons.Rounded.HourglassTop
                                                    shelfLabel = "Жду ответа"
                                                }
                                                ShelfType.SOMEDAY -> {
                                                    shelfIcon = Icons.Rounded.Bookmark
                                                    shelfLabel = "На потом"
                                                }
                                                ShelfType.REFERENCE -> {
                                                    shelfIcon = Icons.Rounded.Description
                                                    shelfLabel = "Справочник"
                                                }
                                            }

                                            Surface(
                                                shape = CircleShape,
                                                color = MaterialTheme.colorScheme.primaryContainer,
                                                modifier = Modifier
                                                    .clip(CircleShape)
                                                    .clickable {
                                                        val shelves = listOf(
                                                            ShelfType.SIMPLE_STEPS,
                                                            ShelfType.BIG_THINGS,
                                                            ShelfType.WAITING,
                                                            ShelfType.SOMEDAY,
                                                            ShelfType.REFERENCE
                                                        )
                                                        val nextShelf = shelves[(shelves.indexOf(item.shelf) + 1) % shelves.size]
                                                        parsedItems = parsedItems.toMutableList().also {
                                                            it[index] = item.copy(shelf = nextShelf)
                                                        }
                                                    }
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.Folder,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                        modifier = Modifier.size(13.dp)
                                                    )
                                                    Text(
                                                        text = shelfLabel,
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 11.5.sp
                                                        ),
                                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                                    )
                                                }
                                            }

                                            // Правый блок: энергия + удаление
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                val energyLabel = when (item.energyLevel) {
                                                    EnergyLevel.MECHANICAL -> "Механика"
                                                    EnergyLevel.SINGLE_STEP -> "Один шаг"
                                                    EnergyLevel.FLOW -> "Поток"
                                                }

                                                Surface(
                                                    shape = CircleShape,
                                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                                    modifier = Modifier
                                                        .clip(CircleShape)
                                                        .clickable {
                                                            val energies = listOf(
                                                                EnergyLevel.MECHANICAL,
                                                                EnergyLevel.SINGLE_STEP,
                                                                EnergyLevel.FLOW
                                                            )
                                                            val nextEnergy = energies[(energies.indexOf(item.energyLevel) + 1) % energies.size]
                                                            parsedItems = parsedItems.toMutableList().also {
                                                                it[index] = item.copy(energyLevel = nextEnergy)
                                                            }
                                                        }
                                                ) {
                                                    Text(
                                                        text = energyLabel,
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            fontWeight = FontWeight.SemiBold,
                                                            fontSize = 11.sp
                                                        ),
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                                    )
                                                }

                                                IconButton(
                                                    onClick = {
                                                        parsedItems = parsedItems.toMutableList().also { it.removeAt(index) }
                                                    },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.Close,
                                                        contentDescription = "Удалить",
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }

                                        // Редактируемый текст названия задачи в один тап
                                        BasicTextField(
                                            value = item.title,
                                            onValueChange = { newTitle ->
                                                parsedItems = parsedItems.toMutableList().also {
                                                    it[index] = item.copy(title = newTitle)
                                                }
                                            },
                                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                                fontSize = 14.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            ),
                                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(
                                                    MaterialTheme.colorScheme.surfaceContainer,
                                                    RoundedCornerShape(8.dp)
                                                )
                                                .padding(horizontal = 8.dp, vertical = 6.dp),
                                            decorationBox = { innerTextField ->
                                                if (item.title.isEmpty()) {
                                                    Text(
                                                        text = "Название задачи...",
                                                        style = MaterialTheme.typography.bodyLarge.copy(
                                                            fontSize = 14.5.sp,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                                        )
                                                    )
                                                }
                                                innerTextField()
                                            }
                                        )

                                        // Подсказка под полем ввода
                                        if (item.aiHint.isNotBlank()) {
                                            Text(
                                                text = item.aiHint,
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontSize = 11.sp,
                                                    fontWeight = if (item.shelf == ShelfType.BIG_THINGS) FontWeight.SemiBold else FontWeight.Normal
                                                ),
                                                color = if (item.shelf == ShelfType.BIG_THINGS) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(start = 2.dp)
                                            )
                                        } else {
                                            Text(
                                                text = "Нажми на текст, чтобы изменить формулировку",
                                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                modifier = Modifier.padding(start = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Нижний ряд: «+ Шаг» и «Подтвердить и сохранить в систему»
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilledTonalButton(
                                onClick = {
                                    parsedItems = parsedItems + EditableParsedItem(
                                        title = "",
                                        shelf = ShelfType.SIMPLE_STEPS,
                                        energyLevel = EnergyLevel.MECHANICAL,
                                        aiHint = "Добавлено вручную"
                                    )
                                },
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                            ) {
                                Text(
                                    text = "+ Шаг",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.5.sp
                                    )
                                )
                            }

                            Button(
                                onClick = {
                                    onSaveParsedTasks(parsedItems)
                                    onDismissRequest()
                                },
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier
                                    .weight(2.2f)
                                    .height(52.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Подтвердить и сохранить",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp
                                    ),
                                    maxLines = 1
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}
