package com.sbros.app

import com.sbros.app.data.local.SeedData
import com.sbros.app.data.local.entity.ProjectEntity
import com.sbros.app.data.local.entity.TaskEntity
import com.sbros.app.data.remote.AiParserApiImpl
import com.sbros.app.domain.model.EnergyLevel
import com.sbros.app.domain.model.ProjectWithFirstStep
import com.sbros.app.domain.model.ShelfType
import com.sbros.app.domain.model.TaskItem
import com.sbros.app.domain.model.toDomain
import com.sbros.app.domain.repository.SbrosRepository
import com.sbros.app.domain.usecase.ProcessMindDumpUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeFullSbrosRepository : SbrosRepository {
    val projects = MutableStateFlow(SeedData.initialProjects)
    val tasks = MutableStateFlow(SeedData.initialTasks.map { it.toDomain() })

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
        return projects.map { projectList ->
            projectList.filter { !it.isArchived }.map { project ->
                val projectTasks = tasks.value.filter { it.projectId == project.id }
                val firstStep = projectTasks
                    .filter { !it.isCompleted }
                    .minByOrNull { it.orderInProject }

                ProjectWithFirstStep(
                    projectId = project.id,
                    projectTitle = project.title,
                    firstUncompletedStep = firstStep,
                    allSteps = projectTasks,
                    isArchived = project.isArchived
                )
            }
        }
    }

    override suspend fun getTaskById(taskId: String): TaskItem? {
        return tasks.value.firstOrNull { it.id == taskId }
    }

    override suspend fun getAllActiveProjects(): List<ProjectEntity> {
        return projects.value.filter { !it.isArchived }
    }

    override suspend fun findProjectByTitle(title: String): ProjectEntity? {
        return projects.value.firstOrNull { it.title.equals(title, ignoreCase = true) }
    }

    override suspend fun insertProject(project: ProjectEntity) {
        projects.value = projects.value + project
    }

    override suspend fun insertTasks(newTasks: List<TaskEntity>) {
        tasks.value = tasks.value + newTasks.map { it.toDomain() }
    }

    override suspend fun getTasksForProject(projectId: String): List<TaskItem> {
        return tasks.value.filter { it.projectId == projectId }
    }

    override suspend fun simplifyTask(taskId: String, nextIndex: Int) {
        tasks.value = tasks.value.map {
            if (it.id == taskId) it.copy(currentSimplifyIndex = nextIndex) else it
        }
    }

    override suspend fun skipTask(taskId: String) {
        tasks.value = tasks.value.map {
            if (it.id == taskId) it.copy(skippedCount = it.skippedCount + 1, lastShownAt = System.currentTimeMillis()) else it
        }
    }

    override suspend fun completeTask(taskId: String) {
        tasks.value = tasks.value.map {
            if (it.id == taskId) it.copy(isCompleted = true) else it
        }
    }

    override suspend fun applySoftAmnesty(): Int = 0
}

class Stage5UnitTest {

    private lateinit var aiParserApi: AiParserApiImpl
    private lateinit var fakeRepository: FakeFullSbrosRepository
    private lateinit var processMindDumpUseCase: ProcessMindDumpUseCase

    @Before
    fun setUp() {
        aiParserApi = AiParserApiImpl()
        fakeRepository = FakeFullSbrosRepository()
        processMindDumpUseCase = ProcessMindDumpUseCase(fakeRepository, aiParserApi)
    }

    @Test
    fun testVentingEmotionalFilter() = runBlocking {
        // Жалобы на усталость и эмоциональный шум должны отсеиваться
        val result = aiParserApi.parseTranscript(
            "блин как же я устал ничего не успеваю капец",
            emptyList()
        )
        assertTrue(result.parsedItems.isEmpty())
    }

    @Test
    fun testParsingMultipleCategorizedThoughts() = runBlocking {
        val transcript = "надо написать старосте в Telegram, купить батарейки АА, в пятницу глянуть кино"
        val result = aiParserApi.parseTranscript(transcript, emptyList())

        assertEquals(3, result.parsedItems.size)

        // 1. Староста в Telegram -> 5 min
        val quick = result.parsedItems.firstOrNull { it.title.contains("старосте", ignoreCase = true) }
        assertNotNull(quick)
        assertEquals("FIVE_MIN", quick?.energyLevel)
        assertEquals("SIMPLE_STEPS", quick?.shelf)
        assertEquals(2, quick?.simplifiedSteps?.size)

        // 2. Купить батарейки -> Easy
        val easy = result.parsedItems.firstOrNull { it.title.contains("батарейки", ignoreCase = true) }
        assertNotNull(easy)
        assertEquals("EASY", easy?.energyLevel)
        assertEquals("SIMPLE_STEPS", easy?.shelf)

        // 3. Кино -> Someday
        val someday = result.parsedItems.firstOrNull { it.title.contains("кино", ignoreCase = true) }
        assertNotNull(someday)
        assertEquals("SOMEDAY", someday?.shelf)
    }

    @Test
    fun testBigThingsProjectDeconstruction() = runBlocking {
        val transcript = "надо сесть за курсач наконец"
        val existingProjects = fakeRepository.getAllActiveProjects()
        val result = aiParserApi.parseTranscript(transcript, existingProjects)

        assertEquals(1, result.parsedItems.size)
        val bigThing = result.parsedItems.first()

        assertEquals("BIG_THINGS", bigThing.shelf)
        assertEquals("FOCUS", bigThing.energyLevel)
        assertEquals(20, bigThing.estimatedMinutes)
        assertEquals(2, bigThing.simplifiedSteps.size)
        // Должен привязаться к существующему проекту "Курсовая работа"
        assertEquals("project-coursework", bigThing.matchedProjectId)
    }

    @Test
    fun testProcessMindDumpUseCaseCreatesTasksAndProjects() = runBlocking {
        val initialTasksCount = fakeRepository.tasks.value.size
        val transcript = "надо позвонить в деканат и заказать доставку еды"

        val createdCount = processMindDumpUseCase(transcript)
        assertTrue(createdCount >= 2)
        assertEquals(initialTasksCount + createdCount, fakeRepository.tasks.value.size)
    }

    @Test
    fun testBigThingsOnlyExposesFirstStep() = runBlocking {
        val projectsWithStep = fakeRepository.getProjectsWithFirstStep()
        val coursework = fakeRepository.getAllActiveProjects().first { it.id == "project-coursework" }

        val allCourseworkTasks = fakeRepository.getTasksForProject(coursework.id)
        assertEquals(2, allCourseworkTasks.size)

        // Проверяем, что первый невыполненный шаг — это именно введение с orderInProject = 1
        val firstStep = allCourseworkTasks.filter { !it.isCompleted }.minByOrNull { it.orderInProject }
        assertNotNull(firstStep)
        assertEquals(1, firstStep?.orderInProject)
        assertEquals("Написать черновик введения (3 абзаца)", firstStep?.title)

        // Завершаем первый шаг
        fakeRepository.completeTask(firstStep!!.id)

        // Теперь первым шагом должен стать второй шаг
        val updatedTasks = fakeRepository.getTasksForProject(coursework.id)
        val newFirstStep = updatedTasks.filter { !it.isCompleted }.minByOrNull { it.orderInProject }
        assertNotNull(newFirstStep)
        assertEquals(2, newFirstStep?.orderInProject)
        assertEquals("Составить содержание и список глав", newFirstStep?.title)
    }
}
