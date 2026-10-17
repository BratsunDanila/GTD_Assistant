package com.sbros.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DumpResponseDto(
    @SerialName("parsed_items")
    val parsedItems: List<ParsedItemDto> = emptyList()
)

@Serializable
data class ParsedItemDto(
    @SerialName("original_quote")
    val originalQuote: String,
    @SerialName("title")
    val title: String,
    @SerialName("simplified_steps")
    val simplifiedSteps: List<String> = emptyList(),
    @SerialName("ai_hint")
    val aiHint: String = "",
    @SerialName("category_badge")
    val categoryBadge: String = "",
    @SerialName("estimated_minutes")
    val estimatedMinutes: Int = 5,
    @SerialName("shelf")
    val shelf: String, // "SIMPLE_STEPS", "BIG_THINGS", "WAITING", "SOMEDAY", "REFERENCE"
    @SerialName("energy_level")
    val energyLevel: String, // "MECHANICAL", "SINGLE_STEP", "FLOW"
    @SerialName("matched_project_id")
    val matchedProjectId: String? = null,
    @SerialName("new_project_title")
    val newProjectTitle: String? = null,
    @SerialName("ai_draft_type")
    val aiDraftType: String = "",
    @SerialName("ai_draft_content")
    val aiDraftContent: String = "",
    @SerialName("scheduled_date_millis")
    val scheduledDateMillis: Long? = null,
    @SerialName("scheduled_time_text")
    val scheduledTimeText: String? = null,
    @SerialName("context_tag")
    val contextTag: String = "home"
)
