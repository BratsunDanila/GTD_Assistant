package com.sbros.app

import com.sbros.app.data.local.SeedData
import com.sbros.app.domain.model.EnergyLevel
import com.sbros.app.domain.model.ProjectWithFirstStep
import com.sbros.app.domain.model.ShelfType
import com.sbros.app.domain.model.TaskItem
import com.sbros.app.domain.model.toDomain
import com.sbros.app.domain.repository.SbrosRepository
import com.sbros.app.domain.usecase.CompleteTaskUseCase
import com.sbros.app.domain.usecase.GetFocusTaskUseCase
import com.sbros.app.domain.usecase.SimplifyTaskUseCase
import com.sbros.app.domain.usecase.SkipTaskUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeSbrosRepository : SbrosRepository {
    private val tasks = MutableStateFlow(SeedData.initialTasks.map { it.toDomain() })

    override fun getFocusTasksByEnergy(energyLevel: EnergyLevel): Flow<List<TaskItem>> {
        return tasks.map { list ->
            list.filter { it.energyLevel == energyLevel && !it.isCompleted && (it.shelf == ShelfType.SIMPLE_STEPS || it.shelf == ShelfType.BIG_THINGS) }
                .sortedBy { it.lastShownAt }
        }
    }

    override fun getTasksByShelf(shelf: ShelfType): Flow<List<TaskItem>> {
        return tasks.map { list ->
            list.filter { it.shelf == shelf && !it.isCompleted }
        }
    }

    override fun getProjectsWithFirstStep(): Flow<List<ProjectWithFirstStep>> {
        return MutableStateFlow(emptyList())
    }

    override suspend fun getTaskById(taskId: String): TaskItem? {
        return tasks.value.firstOrNull { it.id == taskId }
    }

    override suspend fun simplifyTask(taskId: String, nextIndex: Int) {
        tasks.value = tasks.value.map {
            if (it.id == taskId) it.copy(currentSimplifyIndex = nextIndex) else it
        }
    }

    override suspend fun skipTask(taskId: String) {
        tasks.value = tasks.value.map {
            if (it.id == taskId) it.copy(
                skippedCount = it.skippedCount + 1,
                lastShownAt = System.currentTimeMillis()
            ) else it
        }
    }

    override suspend fun getAllActiveProjects(): List<com.sbros.app.data.local.entity.ProjectEntity> = SeedData.initialProjects
    override suspend fun findProjectByTitle(title: String): com.sbros.app.data.local.entity.ProjectEntity? = SeedData.initialProjects.firstOrNull { it.title.equals(title, ignoreCase = true) }
    override suspend fun insertProject(project: com.sbros.app.data.local.entity.ProjectEntity) {}
    override suspend fun insertTasks(tasks: List<com.sbros.app.data.local.entity.TaskEntity>) {}
    override suspend fun getTasksForProject(projectId: String): List<TaskItem> = emptyList()

    override suspend fun completeTask(taskId: String) {
        tasks.value = tasks.value.map {
            if (it.id == taskId) it.copy(isCompleted = true) else it
        }
    }

    override fun getScheduledTasks(): Flow<List<TaskItem>> {
        return tasks.map { list ->
            list.filter { it.scheduledDateMillis != null && !it.isCompleted }
                .sortedBy { it.scheduledDateMillis }
        }
    }

    override fun getTasksForDate(startOfDay: Long, endOfDay: Long): Flow<List<TaskItem>> {
        return tasks.map { list ->
            list.filter { it.scheduledDateMillis != null && it.scheduledDateMillis in startOfDay..endOfDay && !it.isCompleted }
        }
    }

    override suspend fun updateTaskSchedule(taskId: String, scheduledDateMillis: Long?, scheduledTimeText: String?) {
        tasks.value = tasks.value.map {
            if (it.id == taskId) it.copy(scheduledDateMillis = scheduledDateMillis, scheduledTimeText = scheduledTimeText) else it
        }
    }

    override fun getQuickWinTask(): Flow<TaskItem?> {
        return tasks.map { list ->
            list.firstOrNull { it.estimatedMinutes <= 2 && !it.isCompleted && (it.shelf == ShelfType.SIMPLE_STEPS || it.shelf == ShelfType.BIG_THINGS) }
        }
    }

    override suspend fun applySoftAmnesty(): Int = 0
}

class Stage3UnitTest {

    private lateinit var fakeRepository: FakeSbrosRepository
    private lateinit var getFocusTaskUseCase: GetFocusTaskUseCase
    private lateinit var simplifyTaskUseCase: SimplifyTaskUseCase
    private lateinit var skipTaskUseCase: SkipTaskUseCase
    private lateinit var completeTaskUseCase: CompleteTaskUseCase

    @Before
    fun setUp() {
        fakeRepository = FakeSbrosRepository()
        getFocusTaskUseCase = GetFocusTaskUseCase(fakeRepository)
        simplifyTaskUseCase = SimplifyTaskUseCase(fakeRepository)
        skipTaskUseCase = SkipTaskUseCase(fakeRepository)
        completeTaskUseCase = CompleteTaskUseCase(fakeRepository)
    }

    @Test
    fun testGetFocusTasksByEnergy() = runBlocking {
        val mechTasks = getFocusTaskUseCase(EnergyLevel.MECHANICAL).first()
        assertTrue(mechTasks.isNotEmpty())
        assertEquals(EnergyLevel.MECHANICAL, mechTasks.first().energyLevel)

        val stepTasks = getFocusTaskUseCase(EnergyLevel.SINGLE_STEP).first()
        assertTrue(stepTasks.isNotEmpty())
        assertEquals(EnergyLevel.SINGLE_STEP, stepTasks.first().energyLevel)

        val flowTasks = getFocusTaskUseCase(EnergyLevel.FLOW).first()
        assertTrue(flowTasks.isNotEmpty())
        assertEquals(EnergyLevel.FLOW, flowTasks.first().energyLevel)
    }

    @Test
    fun testSimplifyTaskCycling() = runBlocking {
        val task = fakeRepository.getTaskById("task-mech-batteries-buy")
        assertNotNull(task)
        assertEquals(0, task!!.currentSimplifyIndex)
        assertEquals(2, task.simplifiedSteps.size)

        // 0 -> 1
        var nextIdx = simplifyTaskUseCase(task)
        assertEquals(1, nextIdx)
        var updated = fakeRepository.getTaskById("task-mech-batteries-buy")!!
        assertEquals(1, updated.currentSimplifyIndex)
        assertEquals("Открыть маркетплейс и вбить «Аккумуляторы АА Ni-MH 4 шт»", updated.currentStepText)

        // 1 -> 2
        nextIdx = simplifyTaskUseCase(updated)
        assertEquals(2, nextIdx)
        updated = fakeRepository.getTaskById("task-mech-batteries-buy")!!
        assertEquals(2, updated.currentSimplifyIndex)
        assertEquals("Положить первую подходящую упаковку в корзину", updated.currentStepText)

        // 2 -> 0 (цикл возврата)
        nextIdx = simplifyTaskUseCase(updated)
        assertEquals(0, nextIdx)
        updated = fakeRepository.getTaskById("task-mech-batteries-buy")!!
        assertEquals(0, updated.currentSimplifyIndex)
        assertEquals("Заказать аккумуляторы АА на маркетплейсе", updated.currentStepText)
    }

    @Test
    fun testSkipTaskRotatesCard() = runBlocking {
        val mechTasksBefore = getFocusTaskUseCase(EnergyLevel.MECHANICAL).first()
        val firstTask = mechTasksBefore.first()

        skipTaskUseCase(firstTask.id)

        val mechTasksAfter = getFocusTaskUseCase(EnergyLevel.MECHANICAL).first()
        val updatedFirstTask = fakeRepository.getTaskById(firstTask.id)!!
        assertEquals(1, updatedFirstTask.skippedCount)
        assertTrue(updatedFirstTask.lastShownAt > 0)

        // Задача переместилась в конец списка того же уровня энергии
        assertEquals(firstTask.id, mechTasksAfter.last().id)
    }

    @Test
    fun testCompleteTaskRemovesFromFocus() = runBlocking {
        val mechTasksBefore = getFocusTaskUseCase(EnergyLevel.MECHANICAL).first()
        val firstTaskId = mechTasksBefore.first().id

        completeTaskUseCase(firstTaskId)

        val mechTasksAfter = getFocusTaskUseCase(EnergyLevel.MECHANICAL).first()
        assertTrue(mechTasksAfter.none { it.id == firstTaskId })
    }
}
