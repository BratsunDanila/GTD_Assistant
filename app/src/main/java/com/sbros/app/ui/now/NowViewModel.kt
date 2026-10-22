package com.sbros.app.ui.now

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sbros.app.data.local.PreferencesManager
import com.sbros.app.domain.model.EditableParsedItem
import com.sbros.app.domain.model.EnergyLevel
import com.sbros.app.domain.model.ShelfType
import com.sbros.app.domain.model.SubstepItem
import com.sbros.app.domain.model.TaskDifficulty
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
import kotlinx.coroutines.flow.first
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
    private val openRouterClient: com.sbros.app.data.remote.OpenRouterClient,
    private val repository: com.sbros.app.domain.repository.SbrosRepository,
    private val preferencesManager: PreferencesManager,
    private val authRepository: com.sbros.app.domain.repository.AuthRepository,
    private val syncManager: com.sbros.app.sync.SyncManager
) : ViewModel() {

    init {
        viewModelScope.launch {
            syncManager.triggerSync()
        }
    }

    fun processMindDump(rawText: String) {
        viewModelScope.launch {
            processMindDumpUseCase(rawText)
        }
    }

    suspend fun parseTranscript(rawText: String): List<EditableParsedItem> {
        val trimmed = rawText.trim()
        if (trimmed.isBlank()) {
            return emptyList()
        }

        val existingProjects = repository.getAllActiveProjects()
        val response = aiParserApi.parseTranscript(trimmed, existingProjects)
        if (response.parsedItems.isNotEmpty()) {
            return response.parsedItems.map { dto ->
                val parsedDiff = when (dto.difficulty.uppercase()) {
                    "HARD" -> TaskDifficulty.HARD
                    "MEDIUM" -> TaskDifficulty.MEDIUM
                    "EASY" -> TaskDifficulty.EASY
                    else -> when (dto.energyLevel.uppercase()) {
                        "FLOW", "HARD" -> TaskDifficulty.HARD
                        "SINGLE_STEP", "MEDIUM" -> TaskDifficulty.MEDIUM
                        else -> TaskDifficulty.EASY
                    }
                }
                val parsedShelf = try {
                    ShelfType.valueOf(dto.shelf)
                } catch (_: Exception) {
                    if (parsedDiff == TaskDifficulty.HARD) ShelfType.BIG_THINGS else ShelfType.SIMPLE_STEPS
                }

                EditableParsedItem(
                    id = UUID.randomUUID().toString(),
                    title = dto.title.ifBlank { trimmed.take(80) },
                    shelf = parsedShelf,
                    difficulty = parsedDiff,
                    energyLevel = parsedDiff,
                    aiHint = dto.aiHint,
                    categoryBadge = dto.categoryBadge.ifBlank { "Бытовое" },
                    estimatedMinutes = if (dto.estimatedMinutes > 0) dto.estimatedMinutes else 5,
                    simplifiedSteps = dto.simplifiedSteps,
                    matchedProjectId = dto.matchedProjectId,
                    newProjectTitle = dto.newProjectTitle,
                    aiDraftType = dto.aiDraftType,
                    aiDraftContent = dto.aiDraftContent,
                    scheduledDateMillis = dto.scheduledDateMillis,
                    scheduledTimeText = dto.scheduledTimeText,
                    contextTag = dto.contextTag,
                    contextCategory = dto.contextCategory,
                    substeps = dto.aiSuggestedSteps.map { SubstepItem(text = it, isDone = false) },
                    aiSuggestedSteps = dto.aiSuggestedSteps,
                    aiAdvice = dto.aiAdvice
                )
            }
        }

        return emptyList()
    }

    fun saveParsedTasks(items: List<EditableParsedItem>) {
        viewModelScope.launch {
            saveParsedTasksUseCase(items)
        }
    }

    private val _selectedDifficulty = MutableStateFlow(TaskDifficulty.EASY)
    val selectedDifficulty: StateFlow<TaskDifficulty> = _selectedDifficulty.asStateFlow()
    val selectedEnergy: StateFlow<TaskDifficulty> = _selectedDifficulty.asStateFlow()

    private val _showCompletionFeedback = MutableStateFlow(false)
    val showCompletionFeedback: StateFlow<Boolean> = _showCompletionFeedback.asStateFlow()

    val currentTasksList: StateFlow<List<TaskItem>> = _selectedDifficulty
        .flatMapLatest { diff -> getFocusTaskUseCase(diff) }
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

    val hasAnyActiveTasks: StateFlow<Boolean> = repository.getAllTasks()
        .map { list -> list.any { !it.isCompleted && (it.shelf == ShelfType.SIMPLE_STEPS || it.shelf == ShelfType.BIG_THINGS) } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
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
            initialValue = false
        )

    val hasCompletedOnboarding: StateFlow<Boolean> = preferencesManager.hasCompletedOnboardingFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = false
        )

    val isAuthenticated: StateFlow<Boolean> = authRepository.isAuthenticated
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    val currentUserEmail: StateFlow<String?> = authRepository.currentUserEmail
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val currentUserId: StateFlow<String?> = authRepository.currentUserId
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val isGuestMode: StateFlow<Boolean> = authRepository.isGuestMode
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    val syncState: StateFlow<com.sbros.app.domain.model.AppSyncState> = syncManager.syncState

    val lastSyncedTime: StateFlow<Long> = preferencesManager.lastSyncedTimeFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0L
        )

    fun signIn(email: String, pass: String, onResult: (com.sbros.app.domain.repository.AuthResult) -> Unit) {
        viewModelScope.launch {
            val res = authRepository.signIn(email, pass)
            onResult(res)
        }
    }

    fun signUp(email: String, pass: String, name: String, onResult: (com.sbros.app.domain.repository.AuthResult) -> Unit) {
        viewModelScope.launch {
            val res = authRepository.signUp(email, pass, name)
            onResult(res)
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
        }
    }

    fun continueAsGuest() {
        viewModelScope.launch {
            authRepository.continueAsGuest()
        }
    }

    fun triggerSync() {
        syncManager.triggerSync()
    }

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

    fun selectDifficulty(diff: TaskDifficulty) {
        if (_selectedDifficulty.value != diff) {
            _selectedDifficulty.value = diff
        }
    }

    fun selectEnergyLevel(level: TaskDifficulty) {
        selectDifficulty(level)
    }

    val openRouterApiKey: StateFlow<String?> = preferencesManager.openRouterApiKeyFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val openRouterModel: StateFlow<String> = preferencesManager.openRouterModelFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = PreferencesManager.DEFAULT_OPENROUTER_MODEL
        )

    fun setOpenRouterApiKey(apiKey: String?) {
        viewModelScope.launch {
            preferencesManager.setOpenRouterApiKey(apiKey)
        }
    }

    fun setOpenRouterModel(model: String) {
        viewModelScope.launch {
            preferencesManager.setOpenRouterModel(model)
        }
    }

    suspend fun testOpenRouterConnection(apiKey: String, model: String): Result<Boolean> {
        return openRouterClient.testApiKey(apiKey, model)
    }

    fun generateAiSubsteps(taskId: String) {
        viewModelScope.launch {
            val task = repository.getTaskById(taskId) ?: return@launch
            val stepsToUse = aiParserApi.decomposeTask(task.title, task.contextCategory)
            val newSubsteps = stepsToUse.map { SubstepItem(text = it, isDone = false) }
            val updatedTask = task.copy(substeps = newSubsteps)
            repository.updateTask(updatedTask)
        }
    }

    private val _isAnalyzingAi = MutableStateFlow(false)
    val isAnalyzingAi: StateFlow<Boolean> = _isAnalyzingAi.asStateFlow()

    fun generateAiDraft(taskId: String) {
        viewModelScope.launch {
            _isAnalyzingAi.value = true
            try {
                val task = repository.getTaskById(taskId) ?: return@launch
                val draft = aiParserApi.generateDraft(task.title, task.contextCategory) ?: return@launch
                val updatedTask = task.copy(
                    aiDraftType = draft.draftType,
                    aiDraftContent = draft.draftContent,
                    aiAdvice = draft.aiAdvice
                )
                repository.updateTask(updatedTask)
            } finally {
                _isAnalyzingAi.value = false
            }
        }
    }

    fun switchToNextAvailableDifficulty(onAllEmpty: () -> Unit = {}) {
        viewModelScope.launch {
            val allTasks = repository.getAllTasks().first()
            val order = listOf(TaskDifficulty.EASY, TaskDifficulty.MEDIUM, TaskDifficulty.HARD)
            val current = _selectedDifficulty.value
            val next = order.firstOrNull { diff ->
                diff != current && allTasks.any { !it.isCompleted && it.difficulty == diff && (it.shelf == ShelfType.SIMPLE_STEPS || it.shelf == ShelfType.BIG_THINGS) }
            } ?: order.firstOrNull { diff ->
                allTasks.any { !it.isCompleted && it.difficulty == diff && (it.shelf == ShelfType.SIMPLE_STEPS || it.shelf == ShelfType.BIG_THINGS) }
            }
            if (next != null) {
                selectDifficulty(next)
            } else {
                onAllEmpty()
            }
        }
    }

    fun addManualSubstep(taskId: String, text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            val task = repository.getTaskById(taskId) ?: return@launch
            val newSubsteps = task.substeps + SubstepItem(text = text.trim(), isDone = false)
            repository.updateTask(task.copy(substeps = newSubsteps))
        }
    }

    fun removeSubstep(taskId: String, index: Int) {
        viewModelScope.launch {
            val task = repository.getTaskById(taskId) ?: return@launch
            if (index in task.substeps.indices) {
                val newSubsteps = task.substeps.toMutableList().apply { removeAt(index) }
                repository.updateTask(task.copy(substeps = newSubsteps))
            }
        }
    }

    fun toggleSubstep(taskId: String, index: Int) {
        viewModelScope.launch {
            val task = repository.getTaskById(taskId) ?: return@launch
            if (index in task.substeps.indices) {
                val current = task.substeps[index]
                val newSubsteps = task.substeps.toMutableList().apply {
                    set(index, current.copy(isDone = !current.isDone))
                }
                repository.updateTask(task.copy(substeps = newSubsteps))
            }
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
