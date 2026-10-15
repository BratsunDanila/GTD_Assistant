package com.sbros.app.domain.usecase

import com.sbros.app.data.local.entity.ProjectEntity
import com.sbros.app.data.local.entity.TaskEntity
import com.sbros.app.data.remote.AiParserApi
import com.sbros.app.domain.model.EnergyLevel
import com.sbros.app.domain.model.ShelfType
import com.sbros.app.domain.repository.SbrosRepository
import java.util.UUID
import javax.inject.Inject

class ProcessMindDumpUseCase @Inject constructor(
    private val repository: SbrosRepository,
    private val aiParserApi: AiParserApi
) {
    suspend operator fun invoke(rawTranscript: String): Int {
        val trimmed = rawTranscript.trim()
        if (trimmed.isBlank()) return 0

        val existingProjects = repository.getAllActiveProjects()
        val responseDto = aiParserApi.parseTranscript(trimmed, existingProjects)

        if (responseDto.parsedItems.isEmpty()) return 0

        val tasksToInsert = mutableListOf<TaskEntity>()

        for (item in responseDto.parsedItems) {
            var finalProjectId: String? = item.matchedProjectId

            if (finalProjectId == null && !item.newProjectTitle.isNullOrBlank()) {
                val existing = repository.findProjectByTitle(item.newProjectTitle)
                if (existing != null) {
                    finalProjectId = existing.id
                } else {
                    val newProject = ProjectEntity(
                        id = UUID.randomUUID().toString(),
                        title = item.newProjectTitle
                    )
                    repository.insertProject(newProject)
                    finalProjectId = newProject.id
                }
            }

            val orderInProject = if (finalProjectId != null) {
                repository.getTasksForProject(finalProjectId).size + 1
            } else {
                0
            }

            val shelfType = try {
                ShelfType.valueOf(item.shelf)
            } catch (e: Exception) {
                ShelfType.SIMPLE_STEPS
            }

            val energyLevel = try {
                EnergyLevel.valueOf(item.energyLevel)
            } catch (e: Exception) {
                EnergyLevel.EASY
            }

            val task = TaskEntity(
                id = UUID.randomUUID().toString(),
                projectId = finalProjectId,
                orderInProject = orderInProject,
                originalQuote = item.originalQuote,
                title = item.title,
                simplifiedSteps = item.simplifiedSteps,
                currentSimplifyIndex = 0,
                aiHint = item.aiHint,
                categoryBadge = item.categoryBadge,
                estimatedMinutes = item.estimatedMinutes,
                shelf = shelfType,
                energyLevel = energyLevel,
                isCompleted = false,
                skippedCount = 0,
                lastShownAt = 0L,
                createdAt = System.currentTimeMillis()
            )

            tasksToInsert.add(task)
        }

        if (tasksToInsert.isNotEmpty()) {
            repository.insertTasks(tasksToInsert)
        }

        return tasksToInsert.size
    }
}
