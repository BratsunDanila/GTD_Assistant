package com.sbros.app.domain.model

import java.util.UUID

data class EditableParsedItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val shelf: ShelfType = ShelfType.SIMPLE_STEPS,
    val difficulty: TaskDifficulty = TaskDifficulty.EASY,
    val energyLevel: TaskDifficulty = difficulty,
    val aiHint: String = "",
    val categoryBadge: String = "",
    val estimatedMinutes: Int = 5,
    val simplifiedSteps: List<String> = emptyList(),
    val matchedProjectId: String? = null,
    val newProjectTitle: String? = null,
    val aiDraftType: String = "",
    val aiDraftContent: String = "",
    val scheduledDateMillis: Long? = null,
    val scheduledTimeText: String? = null,
    val contextTag: String = "home",
    val contextCategory: String = "Дом и быт",
    val substeps: List<SubstepItem> = emptyList(),
    val aiSuggestedSteps: List<String> = emptyList(),
    val aiAdvice: String = ""
)
