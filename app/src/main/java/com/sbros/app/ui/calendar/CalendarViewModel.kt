package com.sbros.app.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sbros.app.domain.model.TaskItem
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
import javax.inject.Inject

data class CalendarDayItem(
    val dateMillis: Long,
    val dayOfMonth: Int,
    val dayOfWeekShort: String,
    val hasTasks: Boolean,
    val isSelected: Boolean
)

data class CalendarUiState(
    val monthTitle: String = "Октябрь",
    val days: List<CalendarDayItem> = emptyList(),
    val selectedDateMillis: Long = 0L,
    val tasksForDay: List<TaskItem> = emptyList()
)

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val repository: SbrosRepository,
    private val completeTaskUseCase: CompleteTaskUseCase
) : ViewModel() {

    private val todayStartMillis = getStartOfDay(System.currentTimeMillis())
    private val _selectedDateMillis = MutableStateFlow(todayStartMillis)
    val selectedDateMillis: StateFlow<Long> = _selectedDateMillis.asStateFlow()

    private val weekDaysList = generateCurrentWeekDays()

    private val scheduledTasks: StateFlow<List<TaskItem>> = repository.getScheduledTasks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val uiState: StateFlow<CalendarUiState> = combine(
        _selectedDateMillis,
        scheduledTasks
    ) { selectedDate, tasks ->
        val cal = Calendar.getInstance().apply { timeInMillis = selectedDate }
        val rawMonth = cal.getDisplayName(Calendar.MONTH, Calendar.LONG_STANDALONE, Locale("ru")) ?: "Октябрь"
        val monthTitle = rawMonth.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("ru")) else it.toString() }

        val dayItems = weekDaysList.map { dayMillis ->
            val dayStart = getStartOfDay(dayMillis)
            val dayEnd = getEndOfDay(dayMillis)
            val dayCal = Calendar.getInstance().apply { timeInMillis = dayMillis }
            val dayNum = dayCal.get(Calendar.DAY_OF_MONTH)
            val dayOfWeek = formatDayOfWeek(dayCal.get(Calendar.DAY_OF_WEEK))

            val hasTasks = tasks.any { task ->
                val taskDate = task.scheduledDateMillis
                taskDate != null && taskDate in dayStart..dayEnd && !task.isCompleted
            }

            CalendarDayItem(
                dateMillis = dayMillis,
                dayOfMonth = dayNum,
                dayOfWeekShort = dayOfWeek,
                hasTasks = hasTasks,
                isSelected = isSameDay(selectedDate, dayMillis)
            )
        }

        val selectedStart = getStartOfDay(selectedDate)
        val selectedEnd = getEndOfDay(selectedDate)
        val tasksForSelectedDay = tasks.filter { task ->
            val taskDate = task.scheduledDateMillis
            taskDate != null && taskDate in selectedStart..selectedEnd
        }

        CalendarUiState(
            monthTitle = monthTitle,
            days = dayItems,
            selectedDateMillis = selectedDate,
            tasksForDay = tasksForSelectedDay
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CalendarUiState()
    )

    fun selectDay(dateMillis: Long) {
        _selectedDateMillis.value = getStartOfDay(dateMillis)
    }

    fun completeTask(taskId: String) {
        viewModelScope.launch {
            completeTaskUseCase(taskId)
        }
    }

    private fun generateCurrentWeekDays(): List<Long> {
        val cal = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            // Показываем вчера, сегодня и следующие 5 дней (всего 7 дней)
            add(Calendar.DAY_OF_YEAR, -1)
        }
        val list = mutableListOf<Long>()
        for (i in 0 until 7) {
            list.add(cal.timeInMillis)
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return list
    }

    private fun formatDayOfWeek(dayOfWeek: Int): String {
        return when (dayOfWeek) {
            Calendar.MONDAY -> "ПН"
            Calendar.TUESDAY -> "ВТ"
            Calendar.WEDNESDAY -> "СР"
            Calendar.THURSDAY -> "ЧТ"
            Calendar.FRIDAY -> "ПТ"
            Calendar.SATURDAY -> "СБ"
            Calendar.SUNDAY -> "ВС"
            else -> ""
        }
    }

    private fun isSameDay(time1: Long, time2: Long): Boolean {
        val c1 = Calendar.getInstance().apply { timeInMillis = time1 }
        val c2 = Calendar.getInstance().apply { timeInMillis = time2 }
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
                c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
    }

    private fun getStartOfDay(timeMillis: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = timeMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    private fun getEndOfDay(timeMillis: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = timeMillis
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return cal.timeInMillis
    }
}
