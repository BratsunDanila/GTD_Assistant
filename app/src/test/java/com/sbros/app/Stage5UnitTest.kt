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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeFullSbrosRepository : SbrosRepository {
    val projects = MutableStateFlow(SeedData.initialProjects)
    val tasks = MutableStateFlow(SeedData.initialTasks.map { it.toDomain() })

    override fun getAllTasks(): Flow<List<TaskItem>> = tasks

    override fun getFocusTasksByDifficulty(difficulty: com.sbros.app.domain.model.TaskDifficulty): Flow<List<TaskItem>> {
        return tasks.map { list ->
            list.filter { it.difficulty == difficulty && !it.isCompleted && (it.shelf == ShelfType.SIMPLE_STEPS || it.shelf == ShelfType.BIG_THINGS) }
                .sortedBy { it.lastShownAt }
        }
    }

    override suspend fun updateTask(task: TaskItem) {
        tasks.value = tasks.value.map { if (it.id == task.id) task else it }
    }

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

    override suspend fun deleteTask(taskId: String) {
        tasks.value = tasks.value.filter { it.id != taskId }
    }

    override suspend fun deleteProject(projectId: String) {
        projects.value = projects.value.filter { it.id != projectId }
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

        // 1. Староста в Telegram -> SINGLE_STEP
        val quick = result.parsedItems.firstOrNull { it.title.contains("старосте", ignoreCase = true) }
        assertNotNull(quick)
        assertEquals("SINGLE_STEP", quick?.energyLevel)
        assertEquals("SIMPLE_STEPS", quick?.shelf)
        assertEquals(2, quick?.simplifiedSteps?.size)

        // 2. Купить батарейки -> MECHANICAL
        val easy = result.parsedItems.firstOrNull { it.title.contains("батарейки", ignoreCase = true) }
        assertNotNull(easy)
        assertEquals("MECHANICAL", easy?.energyLevel)
        assertEquals("SIMPLE_STEPS", easy?.shelf)

        // 3. Кино -> Someday
        val someday = result.parsedItems.firstOrNull { it.title.contains("кино", ignoreCase = true) }
        assertNotNull(someday)
        assertEquals("SOMEDAY", someday?.shelf)
    }

    @Test
    fun testUserReportedPhrasesParsing() = runBlocking {
        val transcript = "сегодня мне нужно сходить в магазин купить чебуреки, завтра в 9:45 сдать курсовую работу"
        val existingProjects = fakeRepository.getAllActiveProjects()
        val result = aiParserApi.parseTranscript(transcript, existingProjects)

        assertTrue(result.parsedItems.isNotEmpty())
        val chebureki = result.parsedItems.firstOrNull { it.title.contains("чебурек", ignoreCase = true) }
        assertNotNull(chebureki)
        assertEquals("Купить чебуреки", chebureki?.title)
        assertEquals("Покупки и заказы", chebureki?.contextCategory)
        assertEquals("SIMPLE_STEPS", chebureki?.shelf)
        assertNotNull(chebureki?.scheduledDateMillis)
        org.junit.Assert.assertNull("Время не должно выдумываться если не названо", chebureki?.scheduledTimeText)
        assertTrue(chebureki?.aiSuggestedSteps.isNullOrEmpty())

        val coursework = result.parsedItems.firstOrNull { it.title.contains("курсов", ignoreCase = true) }
        assertNotNull(coursework)
        assertEquals("Сдать курсовую работу", coursework?.title)
        assertEquals("Учеба и вуз", coursework?.contextCategory)
        assertEquals("09:45", coursework?.scheduledTimeText)
        assertEquals("BIG_THINGS", coursework?.shelf)
        assertNotNull(coursework?.scheduledDateMillis)
        assertTrue(coursework?.aiSuggestedSteps.isNullOrEmpty())
    }

    @Test
    fun testDayWithoutTimeHasDateButNullTime() = runBlocking {
        val transcript = "в пятницу сдать отчет"
        val result = aiParserApi.parseTranscript(transcript, emptyList())

        assertEquals(1, result.parsedItems.size)
        val item = result.parsedItems.first()
        assertEquals("Сдать отчет", item.title)
        assertNotNull("День должен быть зафиксирован в календаре", item.scheduledDateMillis)
        org.junit.Assert.assertNull("Время не должно придумываться", item.scheduledTimeText)
    }

    @Test
    fun testDecompositionMaxSteps() = runBlocking {
        val steps = aiParserApi.decomposeTask("Написать дипломную работу", "Учеба и вуз")
        assertTrue("Шагов должно быть от 2 до 4", steps.size in 2..4)
    }

    @Test
    fun testAiDraftAnalysisGeneration() = runBlocking {
        val draft = aiParserApi.generateDraft("Позвонить старосте", "Учеба и вуз")
        assertNotNull(draft)
        assertTrue(draft!!.draftContent.isNotBlank())
        assertTrue(draft.draftType.isNotBlank())
        assertTrue(draft.aiAdvice.isNotBlank())
    }

    @Test
    fun testBigThingsProjectDeconstruction() = runBlocking {
        val transcript = "надо сесть за курсач наконец"
        val existingProjects = fakeRepository.getAllActiveProjects()
        val result = aiParserApi.parseTranscript(transcript, existingProjects)

        assertEquals(1, result.parsedItems.size)
        val bigThing = result.parsedItems.first()

        assertEquals("BIG_THINGS", bigThing.shelf)
        assertEquals("FLOW", bigThing.energyLevel)
        assertEquals(20, bigThing.estimatedMinutes)
        assertEquals(2, bigThing.simplifiedSteps.size)
        // Должен привязаться к существующему проекту "Курсовая работа"
        assertEquals(SeedData.PROJECT_COURSEWORK_ID, bigThing.matchedProjectId)
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
        val coursework = fakeRepository.getAllActiveProjects().first { it.id == SeedData.PROJECT_COURSEWORK_ID }

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

    @Test
    fun testUserVoicePhrasesTimeAndDifficulty() = runBlocking {
        // 1. "сдать курсовую в 9 45 завтра"
        val res1 = aiParserApi.parseTranscript("сдать курсовую в 9 45 завтра", emptyList())
        val task1 = res1.parsedItems.first()
        assertEquals("09:45", task1.scheduledTimeText)
        assertNotNull(task1.scheduledDateMillis)
        assertTrue(task1.title.contains("курсов", ignoreCase = true))
        assertFalse(task1.title.contains("завтра", ignoreCase = true))
        assertFalse(task1.title.contains("9 45"))

        // 2. "завтра в 6 вечера погулять с девушкой"
        val res2 = aiParserApi.parseTranscript("завтра в 6:00 вечера погулять с девушкой", emptyList())
        val task2 = res2.parsedItems.first()
        assertEquals("18:00", task2.scheduledTimeText)
        assertNotNull(task2.scheduledDateMillis)
        assertEquals("EASY", task2.difficulty)

        // 3. "послезавтра начать писать олимпиаду"
        val res3 = aiParserApi.parseTranscript("послезавтра начать писать олимпиаду", emptyList())
        val task3 = res3.parsedItems.first()
        assertNull(task3.scheduledTimeText)
        assertNotNull(task3.scheduledDateMillis)
        assertEquals("HARD", task3.difficulty)
        assertEquals("BIG_THINGS", task3.shelf)
    }
}
