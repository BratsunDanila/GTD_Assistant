package com.sbros.app.domain.model

import com.sbros.app.data.local.entity.TaskEntity

data class TaskItem(
    val id: String,
    val projectId: String? = null,
    val orderInProject: Int = 0,
    val originalQuote: String,
    val title: String,
    val simplifiedSteps: List<String> = emptyList(),
    val currentSimplifyIndex: Int = 0,
    val aiHint: String = "",
    val categoryBadge: String = "",
    val estimatedMinutes: Int = 5,
    val shelf: ShelfType = ShelfType.SIMPLE_STEPS,
    val energyLevel: EnergyLevel = EnergyLevel.EASY,
    val isCompleted: Boolean = false,
    val skippedCount: Int = 0,
    val lastShownAt: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val aiDraftType: String = "",
    val aiDraftContent: String = "",
    val scheduledDateMillis: Long? = null,
    val scheduledTimeText: String? = null
) {
    /**
     * Возвращает текущий текст шага с учетом нажатий кнопки «Проще».
     * 0 -> исходный title
     * 1 -> simplifiedSteps[0]
     * 2 -> simplifiedSteps[1]
     */
    val currentStepText: String
        get() = when (currentSimplifyIndex) {
            1 -> simplifiedSteps.getOrNull(0) ?: title
            2 -> simplifiedSteps.getOrNull(1) ?: simplifiedSteps.getOrNull(0) ?: title
            else -> title
        }
}

fun TaskEntity.toDomain(): TaskItem = TaskItem(
    id = id,
    projectId = projectId,
    orderInProject = orderInProject,
    originalQuote = originalQuote,
    title = title,
    simplifiedSteps = simplifiedSteps,
    currentSimplifyIndex = currentSimplifyIndex,
    aiHint = aiHint,
    categoryBadge = categoryBadge,
    estimatedMinutes = estimatedMinutes,
    shelf = shelf,
    energyLevel = energyLevel,
    isCompleted = isCompleted,
    skippedCount = skippedCount,
    lastShownAt = lastShownAt,
    createdAt = createdAt,
    aiDraftType = aiDraftType,
    aiDraftContent = aiDraftContent,
    scheduledDateMillis = scheduledDateMillis,
    scheduledTimeText = scheduledTimeText
)

fun TaskItem.toEntity(): TaskEntity = TaskEntity(
    id = id,
    projectId = projectId,
    orderInProject = orderInProject,
    originalQuote = originalQuote,
    title = title,
    simplifiedSteps = simplifiedSteps,
    currentSimplifyIndex = currentSimplifyIndex,
    aiHint = aiHint,
    categoryBadge = categoryBadge,
    estimatedMinutes = estimatedMinutes,
    shelf = shelf,
    energyLevel = energyLevel,
    isCompleted = isCompleted,
    skippedCount = skippedCount,
    lastShownAt = lastShownAt,
    createdAt = createdAt,
    aiDraftType = aiDraftType,
    aiDraftContent = aiDraftContent,
    scheduledDateMillis = scheduledDateMillis,
    scheduledTimeText = scheduledTimeText
)
