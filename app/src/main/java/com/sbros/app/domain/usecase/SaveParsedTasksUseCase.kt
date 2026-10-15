package com.sbros.app.domain.usecase

import com.sbros.app.data.local.entity.ProjectEntity
import com.sbros.app.data.local.entity.TaskEntity
import com.sbros.app.domain.model.EditableParsedItem
import com.sbros.app.domain.model.ShelfType
import com.sbros.app.domain.repository.SbrosRepository
import java.util.UUID
import javax.inject.Inject

class SaveParsedTasksUseCase @Inject constructor(
    private val repository: SbrosRepository
) {
    suspend operator fun invoke(items: List<EditableParsedItem>): Int {
        val validItems = items.filter { it.title.isNotBlank() }
        if (validItems.isEmpty()) return 0

        val tasksToInsert = mutableListOf<TaskEntity>()

        for (item in validItems) {
            var finalProjectId = item.matchedProjectId
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

            val defaultBadge = when (item.shelf) {
                ShelfType.SIMPLE_STEPS -> "Простые шаги"
                ShelfType.BIG_THINGS -> "Большие дела"
                ShelfType.WAITING -> "Жду от других"
                ShelfType.SOMEDAY -> "На потом"
            }

            val task = TaskEntity(
                id = item.id,
                projectId = finalProjectId,
                orderInProject = orderInProject,
                originalQuote = item.title,
                title = item.title.trim(),
                simplifiedSteps = item.simplifiedSteps,
                currentSimplifyIndex = 0,
                aiHint = item.aiHint,
                categoryBadge = item.categoryBadge.ifBlank { defaultBadge },
                estimatedMinutes = item.estimatedMinutes,
                shelf = item.shelf,
                energyLevel = item.energyLevel,
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
