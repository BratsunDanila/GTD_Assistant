package com.sbros.app.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sbros.app.domain.model.TaskItem
import com.sbros.app.domain.model.toEntity
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
import java.util.Calendar
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

data class CalendarGridCell(
    val dayNumber: Int?, // null for empty offset cells
    val dateMillis: Long?,
    val hasTasks: Boolean = false,
    val tasksCount: Int = 0,
    val isSelected: Boolean = false
)

data class CalendarMonthInfo(
    val year: Int,
    val monthIndex: Int, // 0-based
    val monthTitle: String,
    val leadingOffset: Int,
    val totalDays: Int
)

data class CalendarUiState(
    val monthTitle: String = "Октябрь 2026",
    val selectedDay: Int = 17,
    val gridCells: List<CalendarGridCell> = emptyList(),
    val tasksForDay: List<TaskItem> = emptyList()
)

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val repository: SbrosRepository,
    private val completeTaskUseCase: CompleteTaskUseCase
) : ViewModel() {

    // Начальный месяц — Октябрь 2026 (согласно эталону sbros_production_m3.html)
    private val _calendarState = MutableStateFlow(
        Calendar.getInstance().apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.OCTOBER)
            set(Calendar.DAY_OF_MONTH, 17)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    )

    private val _selectedDay = MutableStateFlow(17)
    val selectedDay: StateFlow<Int> = _selectedDay.asStateFlow()

    private val scheduledTasks: StateFlow<List<TaskItem>> = repository.getScheduledTasks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val uiState: StateFlow<CalendarUiState> = combine(
        _calendarState,
        _selectedDay,
        scheduledTasks
    ) { calState, selectedDay, tasks ->
        val year = calState.get(Calendar.YEAR)
        val month = calState.get(Calendar.MONTH)

        // Название месяца и год
        val rawMonth = calState.getDisplayName(Calendar.MONTH, Calendar.LONG_STANDALONE, Locale("ru")) ?: "Октябрь"
        val capitalizedMonth = rawMonth.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("ru")) else it.toString() }
        val monthTitle = "$capitalizedMonth $year"

        // Дней в месяце
        val calClone = calState.clone() as Calendar
        calClone.set(Calendar.DAY_OF_MONTH, 1)
        val maxDays = calClone.getActualMaximum(Calendar.DAY_OF_MONTH)

        // Смещение первого дня (Пн = 0, Вт = 1, ... Вс = 6)
        val firstDayOfWeek = calClone.get(Calendar.DAY_OF_WEEK)
        val leadingOffset = when (firstDayOfWeek) {
            Calendar.MONDAY -> 0
            Calendar.TUESDAY -> 1
            Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY -> 3
            Calendar.FRIDAY -> 4
            Calendar.SATURDAY -> 5
            Calendar.SUNDAY -> 6
            else -> 0
        }

        // Формирование ячеек сетки
        val cells = mutableListOf<CalendarGridCell>()
        for (i in 0 until leadingOffset) {
            cells.add(CalendarGridCell(dayNumber = null, dateMillis = null))
        }

        for (d in 1..maxDays) {
            calClone.set(Calendar.DAY_OF_MONTH, d)
            val dayStart = calClone.timeInMillis
            calClone.set(Calendar.HOUR_OF_DAY, 23)
            calClone.set(Calendar.MINUTE, 59)
            calClone.set(Calendar.SECOND, 59)
            val dayEnd = calClone.timeInMillis
            calClone.set(Calendar.HOUR_OF_DAY, 0)
            calClone.set(Calendar.MINUTE, 0)
            calClone.set(Calendar.SECOND, 0)

            val dayTasks = tasks.filter { task ->
                val taskDate = task.scheduledDateMillis
                taskDate != null && taskDate in dayStart..dayEnd && !task.isCompleted
            }
            val count = dayTasks.size

            cells.add(
                CalendarGridCell(
                    dayNumber = d,
                    dateMillis = dayStart,
                    hasTasks = count > 0,
                    tasksCount = count,
                    isSelected = (d == selectedDay)
                )
            )
        }

        // Задачи на выбранный день
        calClone.set(Calendar.DAY_OF_MONTH, selectedDay)
        val selDayStart = calClone.timeInMillis
        calClone.set(Calendar.HOUR_OF_DAY, 23)
        calClone.set(Calendar.MINUTE, 59)
        calClone.set(Calendar.SECOND, 59)
        val selDayEnd = calClone.timeInMillis

        val tasksForSelectedDay = tasks.filter { task ->
            val taskDate = task.scheduledDateMillis
            taskDate != null && taskDate in selDayStart..selDayEnd
        }

        CalendarUiState(
            monthTitle = monthTitle,
            selectedDay = selectedDay,
            gridCells = cells,
            tasksForDay = tasksForSelectedDay
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CalendarUiState()
    )

    fun changeMonth(delta: Int) {
        val current = _calendarState.value.clone() as Calendar
        current.add(Calendar.MONTH, delta)
        _calendarState.value = current
        _selectedDay.value = 1
    }

    fun selectDay(dayNumber: Int) {
        _selectedDay.value = dayNumber
    }

    fun completeTask(taskId: String) {
        viewModelScope.launch {
            completeTaskUseCase(taskId)
        }
    }

    fun addCalendarEventForSelectedDay() {
        viewModelScope.launch {
            val cal = _calendarState.value.clone() as Calendar
            cal.set(Calendar.DAY_OF_MONTH, _selectedDay.value)
            cal.set(Calendar.HOUR_OF_DAY, 16)
            cal.set(Calendar.MINUTE, 0)

            val newTask = TaskItem(
                id = UUID.randomUUID().toString(),
                title = "Новая встреча в календаре",
                originalQuote = "Добавлено вручную на ${_selectedDay.value} число",
                categoryBadge = "Личное",
                estimatedMinutes = 15,
                scheduledDateMillis = cal.timeInMillis,
                scheduledTimeText = "16:00",
                contextTag = "home"
            )
            repository.insertTasks(listOf(newTask.toEntity()))
        }
    }
}
