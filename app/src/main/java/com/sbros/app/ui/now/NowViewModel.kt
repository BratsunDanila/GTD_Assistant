package com.sbros.app.ui.now

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sbros.app.data.local.PreferencesManager
import com.sbros.app.domain.model.EnergyLevel
import com.sbros.app.domain.model.TaskItem
import com.sbros.app.domain.model.ThemePalette
import com.sbros.app.domain.usecase.CompleteTaskUseCase
import com.sbros.app.domain.usecase.GetFocusTaskUseCase
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
import javax.inject.Inject

data class NowUiState(
    val selectedEnergy: EnergyLevel = EnergyLevel.EASY,
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
    private val processMindDumpUseCase: com.sbros.app.domain.usecase.ProcessMindDumpUseCase,
    private val saveParsedTasksUseCase: com.sbros.app.domain.usecase.SaveParsedTasksUseCase,
    private val aiParserApi: com.sbros.app.data.remote.AiParserApi,
    private val repository: com.sbros.app.domain.repository.SbrosRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    fun processMindDump(rawText: String) {
        viewModelScope.launch {
            processMindDumpUseCase(rawText)
        }
    }

    suspend fun parseTranscript(rawText: String): List<com.sbros.app.domain.model.EditableParsedItem> {
        val trimmed = rawText.trim()
        if (trimmed.isNotBlank()) {
            val existingProjects = repository.getAllActiveProjects()
            val response = aiParserApi.parseTranscript(trimmed, existingProjects)
            if (response.parsedItems.isNotEmpty()) {
                return response.parsedItems.map { dto ->
                    com.sbros.app.domain.model.EditableParsedItem(
                        id = java.util.UUID.randomUUID().toString(),
                        title = dto.title,
                        shelf = try { com.sbros.app.domain.model.ShelfType.valueOf(dto.shelf) } catch (_: Exception) { com.sbros.app.domain.model.ShelfType.SIMPLE_STEPS },
                        energyLevel = try { com.sbros.app.domain.model.EnergyLevel.valueOf(dto.energyLevel) } catch (_: Exception) { com.sbros.app.domain.model.EnergyLevel.EASY },
                        aiHint = dto.aiHint,
                        categoryBadge = dto.categoryBadge,
                        estimatedMinutes = dto.estimatedMinutes,
                        simplifiedSteps = dto.simplifiedSteps,
                        matchedProjectId = dto.matchedProjectId,
                        newProjectTitle = dto.newProjectTitle
                    )
                }
            }
        }
        // Если пустой ввод или чистый шум — возвращаем эталонные 3 карточки из sbros_v3_swipe.html
        return listOf(
            com.sbros.app.domain.model.EditableParsedItem(
                title = "Отправить старосте тему работы в Telegram",
                shelf = com.sbros.app.domain.model.ShelfType.SIMPLE_STEPS,
                energyLevel = com.sbros.app.domain.model.EnergyLevel.FIVE_MIN,
                categoryBadge = "⚡ Мессенджер",
                estimatedMinutes = 2,
                aiHint = "Быстрое действие, которое сразу закроет хвост."
            ),
            com.sbros.app.domain.model.EditableParsedItem(
                title = "Написать 3 абзаца черновика введения",
                shelf = com.sbros.app.domain.model.ShelfType.BIG_THINGS,
                energyLevel = com.sbros.app.domain.model.EnergyLevel.FOCUS,
                categoryBadge = "Большое дело: Курсовая",
                estimatedMinutes = 20,
                aiHint = "✨ ИИ разбил проект «Курсовая» и выделил легкий 1-й шаг"
            ),
            com.sbros.app.domain.model.EditableParsedItem(
                title = "Правки по первой главе от научрука",
                shelf = com.sbros.app.domain.model.ShelfType.WAITING,
                energyLevel = com.sbros.app.domain.model.EnergyLevel.EASY,
                categoryBadge = "Ожидание • Контроль",
                estimatedMinutes = 0,
                aiHint = "Спрятано из экрана «Сейчас», чтобы не мешать"
            )
        )
    }

    fun saveParsedTasks(items: List<com.sbros.app.domain.model.EditableParsedItem>) {
        viewModelScope.launch {
            saveParsedTasksUseCase(items)
        }
    }

    private val _selectedEnergy = MutableStateFlow(EnergyLevel.EASY)
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

    val themePalette: StateFlow<ThemePalette> = preferencesManager.themePaletteFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ThemePalette.SAGE
        )

    val isDarkMode: StateFlow<Boolean?> = preferencesManager.isDarkModeFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val hasCompletedOnboarding: StateFlow<Boolean?> = preferencesManager.hasCompletedOnboardingFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null
        )

    fun completeOnboarding() {
        viewModelScope.launch {
            preferencesManager.setOnboardingCompleted(true)
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
