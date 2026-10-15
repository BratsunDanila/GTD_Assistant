package com.sbros.app.domain.model

import java.util.UUID

data class EditableParsedItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val shelf: ShelfType = ShelfType.SIMPLE_STEPS,
    val energyLevel: EnergyLevel = EnergyLevel.EASY,
    val aiHint: String = "",
    val categoryBadge: String = "",
    val estimatedMinutes: Int = 5,
    val simplifiedSteps: List<String> = emptyList(),
    val matchedProjectId: String? = null,
    val newProjectTitle: String? = null
)
