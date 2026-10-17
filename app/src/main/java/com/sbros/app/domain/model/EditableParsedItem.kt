package com.sbros.app.domain.model

import java.util.UUID

data class EditableParsedItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val shelf: ShelfType = ShelfType.SIMPLE_STEPS,
    val energyLevel: EnergyLevel = EnergyLevel.MECHANICAL,
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
    val contextTag: String = "home"
)
