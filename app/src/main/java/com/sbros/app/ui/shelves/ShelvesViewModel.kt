package com.sbros.app.ui.shelves

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sbros.app.data.local.PreferencesManager
import com.sbros.app.domain.model.ProjectWithFirstStep
import com.sbros.app.domain.model.ShelfType
import com.sbros.app.domain.model.TaskItem
import com.sbros.app.domain.model.ThemePalette
import com.sbros.app.domain.repository.SbrosRepository
import com.sbros.app.domain.usecase.CompleteTaskUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class ShelvesViewModel @Inject constructor(
    private val repository: SbrosRepository,
    private val completeTaskUseCase: CompleteTaskUseCase,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _selectedContext = MutableStateFlow("all")
    val selectedContext: StateFlow<String> = _selectedContext.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isSearchActive = MutableStateFlow(false)
    val isSearchActive: StateFlow<Boolean> = _isSearchActive.asStateFlow()

    val simpleSteps: StateFlow<List<TaskItem>> = repository.getTasksByShelf(ShelfType.SIMPLE_STEPS)
        .combine(_selectedContext) { tasks, ctx -> filterByContext(tasks, ctx) }
        .combine(_searchQuery) { tasks, query -> filterTasks(tasks, query) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bigThings: StateFlow<List<ProjectWithFirstStep>> = repository.getProjectsWithFirstStep()
        .combine(_selectedContext) { projects, ctx ->
            if (ctx == "all") projects
            else projects.filter { it.firstUncompletedStep?.contextTag == ctx }
        }
        .combine(_searchQuery) { projects, query ->
            if (query.isBlank()) {
                projects
            } else {
                val q = query.lowercase(Locale.ROOT)
                projects.filter {
                    it.projectTitle.lowercase(Locale.ROOT).contains(q) ||
                            (it.firstUncompletedStep?.title?.lowercase(Locale.ROOT)?.contains(q) == true)
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val waiting: StateFlow<List<TaskItem>> = repository.getTasksByShelf(ShelfType.WAITING)
        .combine(_selectedContext) { tasks, ctx -> filterByContext(tasks, ctx) }
        .combine(_searchQuery) { tasks, query -> filterTasks(tasks, query) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val someday: StateFlow<List<TaskItem>> = repository.getTasksByShelf(ShelfType.SOMEDAY)
        .combine(_selectedContext) { tasks, ctx -> filterByContext(tasks, ctx) }
        .combine(_searchQuery) { tasks, query -> filterTasks(tasks, query) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val reference: StateFlow<List<TaskItem>> = repository.getTasksByShelf(ShelfType.REFERENCE)
        .combine(_selectedContext) { tasks, ctx -> filterByContext(tasks, ctx) }
        .combine(_searchQuery) { tasks, query -> filterTasks(tasks, query) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val themePalette: StateFlow<ThemePalette> = preferencesManager.themePaletteFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemePalette.LAVENDER)

    val isDarkMode: StateFlow<Boolean?> = preferencesManager.isDarkModeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun setContext(ctx: String) {
        _selectedContext.value = ctx
    }

    fun onTaskComplete(taskId: String) {
        viewModelScope.launch {
            completeTaskUseCase(taskId)
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleSearch(active: Boolean) {
        _isSearchActive.value = active
        if (!active) {
            _searchQuery.value = ""
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

    private fun filterByContext(tasks: List<TaskItem>, ctx: String): List<TaskItem> {
        if (ctx == "all") return tasks
        return tasks.filter { it.contextTag == ctx }
    }

    private fun filterTasks(tasks: List<TaskItem>, query: String): List<TaskItem> {
        if (query.isBlank()) return tasks
        val q = query.lowercase(Locale.ROOT)
        return tasks.filter {
            it.title.lowercase(Locale.ROOT).contains(q) ||
                    it.categoryBadge.lowercase(Locale.ROOT).contains(q)
        }
    }
}
