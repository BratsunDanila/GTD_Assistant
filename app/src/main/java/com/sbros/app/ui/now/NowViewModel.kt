package com.sbros.app.ui.now

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sbros.app.data.local.PreferencesManager
import com.sbros.app.domain.model.EditableParsedItem
import com.sbros.app.domain.model.EnergyLevel
import com.sbros.app.domain.model.ShelfType
import com.sbros.app.domain.model.TaskItem
import com.sbros.app.domain.model.ThemePalette
import com.sbros.app.domain.usecase.CompleteTaskUseCase
import com.sbros.app.domain.usecase.GetFocusTaskUseCase
import com.sbros.app.domain.usecase.ProcessMindDumpUseCase
import com.sbros.app.domain.usecase.SaveParsedTasksUseCase
import com.sbros.app.domain.usecase.SimplifyTaskUseCase
import com.sbros.app.domain.usecase.SkipTaskUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class NowUiState(
    val selectedEnergy: EnergyLevel = EnergyLevel.MECHANICAL,
    val focusTask: TaskItem? = null,
    val showCompletionFeedback: Boolean = false,
    val feedbackTitle: String = "Отлично, минус одна мысль!",
    val feedbackSubtitle: String = "Можешь отдохнуть или взять следующее дело, если есть настроение."
)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class NowViewModel @Inject constructor(
    private val getFocusTaskUseCase: GetFocusTaskUseCase,
    private val simplifyTaskUseCase: SimplifyTaskUseCase,
    private val skipTaskUseCase: SkipTaskUseCase,
    private val completeTaskUseCase: CompleteTaskUseCase,
    private val processMindDumpUseCase: ProcessMindDumpUseCase,
    private val saveParsedTasksUseCase: SaveParsedTasksUseCase,
    private val aiParserApi: com.sbros.app.data.remote.AiParserApi,
    private val repository: com.sbros.app.domain.repository.SbrosRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    fun processMindDump(rawText: String) {
        viewModelScope.launch {
            processMindDumpUseCase(rawText)
        }
    }

    suspend fun parseTranscript(rawText: String): List<EditableParsedItem> {
        val trimmed = rawText.trim()
        if (trimmed.isNotBlank()) {
            val existingProjects = repository.getAllActiveProjects()
            val response = aiParserApi.parseTranscript(trimmed, existingProjects)
            if (response.parsedItems.isNotEmpty()) {
                return response.parsedItems.map { dto ->
                    EditableParsedItem(
                        id = UUID.randomUUID().toString(),
                        title = dto.title,
                        shelf = try { ShelfType.valueOf(dto.shelf) } catch (_: Exception) { ShelfType.SIMPLE_STEPS },
                        energyLevel = try { EnergyLevel.valueOf(dto.energyLevel) } catch (_: Exception) { EnergyLevel.MECHANICAL },
                        aiHint = dto.aiHint,
                        categoryBadge = dto.categoryBadge,
                        estimatedMinutes = dto.estimatedMinutes,
                        simplifiedSteps = dto.simplifiedSteps,
                        matchedProjectId = dto.matchedProjectId,
                        newProjectTitle = dto.newProjectTitle,
                        aiDraftType = dto.aiDraftType,
                        aiDraftContent = dto.aiDraftContent,
                        scheduledDateMillis = dto.scheduledDateMillis,
                        scheduledTimeText = dto.scheduledTimeText,
                        contextTag = dto.contextTag
                    )
                }
            }
        }
        // Эталонный фолбек без эмодзи по sbros_production_m3.html
        return listOf(
            EditableParsedItem(
                title = "Заказать аккумуляторы АА на маркетплейсе",
                shelf = ShelfType.SIMPLE_STEPS,
                energyLevel = EnergyLevel.MECHANICAL,
                categoryBadge = "Бытовое • Покупки",
                estimatedMinutes = 3,
                aiHint = "ИИ отделил это от мыслей про камеру в простую покупку.",
                aiDraftType = "Рекомендация ИИ",
                aiDraftContent = "Поиск: Ni-MH AA 2100-2500 mAh (низкий саморазряд LSD, держат привод объектива без просадки напряжения).",
                contextTag = "out"
            ),
            EditableParsedItem(
                title = "Написать 3 абзаца черновика введения",
                shelf = ShelfType.BIG_THINGS,
                energyLevel = EnergyLevel.FLOW,
                categoryBadge = "Большое дело: Курсовая",
                estimatedMinutes = 20,
                aiHint = "ИИ разбил проект «Курсовая» и выделил легкий 1-й шаг",
                aiDraftType = "План введения",
                aiDraftContent = "1. Проблема когнитивного перегруза.\n2. Снижение барьера входа.\n3. Декомпозиция с помощью ИИ.",
                contextTag = "pc"
            ),
            EditableParsedItem(
                title = "Правки по первой главе от научрука",
                shelf = ShelfType.WAITING,
                energyLevel = EnergyLevel.MECHANICAL,
                categoryBadge = "Ожидание • Контроль",
                estimatedMinutes = 0,
                aiHint = "Спрятано из экрана «Сейчас», чтобы не мешать",
                aiDraftType = "Черновик напоминания",
                aiDraftContent = "«Здравствуйте! Удалось ли посмотреть черновик первой главы?»",
                contextTag = "phone"
            )
        )
    }

    fun saveParsedTasks(items: List<EditableParsedItem>) {
        viewModelScope.launch {
            saveParsedTasksUseCase(items)
        }
    }

    private val _selectedEnergy = MutableStateFlow(EnergyLevel.MECHANICAL)
    val selectedEnergy: StateFlow<EnergyLevel> = _selectedEnergy.asStateFlow()

    private val _showCompletionFeedback = MutableStateFlow(false)
    val showCompletionFeedback: StateFlow<Boolean> = _showCompletionFeedback.asStateFlow()

    val currentTasksList: StateFlow<List<TaskItem>> = _selectedEnergy
        .flatMapLatest { energy -> getFocusTaskUseCase(energy) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val currentFocusTask: StateFlow<TaskItem?> = currentTasksList
        .map { it.firstOrNull() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val quickWinTask: StateFlow<TaskItem?> = repository.getQuickWinTask()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val userName: StateFlow<String> = preferencesManager.userNameFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "Данила"
        )

    val themePalette: StateFlow<ThemePalette> = preferencesManager.themePaletteFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ThemePalette.LAVENDER
        )

    val isDarkMode: StateFlow<Boolean?> = preferencesManager.isDarkModeFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val hasCompletedOnboarding: StateFlow<Boolean> = preferencesManager.hasCompletedOnboardingFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = false
        )

    fun completeOnboarding() {
        viewModelScope.launch {
            preferencesManager.setOnboardingCompleted(true)
        }
    }

    fun resetOnboarding() {
        viewModelScope.launch {
            preferencesManager.setOnboardingCompleted(false)
        }
    }

    fun setUserName(name: String) {
        viewModelScope.launch {
            preferencesManager.setUserName(name)
        }
    }

    fun selectEnergyLevel(level: EnergyLevel) {
        if (_selectedEnergy.value != level) {
            _selectedEnergy.value = level
        }
    }

    fun onSimplifyClicked() {
        val task = currentFocusTask.value ?: return
        viewModelScope.launch {
            simplifyTaskUseCase(task)
        }
    }

    fun onSkipClicked() {
        val task = currentFocusTask.value ?: return
        viewModelScope.launch {
            skipTaskUseCase(task.id)
        }
    }

    fun onCompleteClicked() {
        val task = currentFocusTask.value ?: return
        viewModelScope.launch {
            _showCompletionFeedback.value = true
            completeTaskUseCase(task.id)
            delay(1500)
            _showCompletionFeedback.value = false
        }
    }

    fun completeQuickWinTask(taskId: String) {
        viewModelScope.launch {
            completeTaskUseCase(taskId)
        }
    }

    fun completeQuickWinTask() {
        val task = quickWinTask.value ?: return
        completeQuickWinTask(task.id)
    }

    fun setThemePalette(palette: ThemePalette) {
        viewModelScope.launch {
            preferencesManager.setThemePalette(palette)
        }
    }

    fun setDarkMode(isDark: Boolean?) {
        viewModelScope.launch {
            preferencesManager.setDarkMode(isDark)
        }
    }
}
