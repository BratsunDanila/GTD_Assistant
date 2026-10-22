package com.sbros.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProfileDto(
    val id: String,
    val email: String? = null,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("theme_palette") val themePalette: String? = null,
    @SerialName("is_dark_mode") val isDarkMode: Boolean? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class ContextDto(
    val id: String? = null,
    @SerialName("user_id") val userId: String,
    val tag: String,
    val name: String,
    val icon: String? = null
)

@Serializable
data class ProjectRemoteDto(
    val id: String,
    @SerialName("user_id") val userId: String,
    val title: String,
    @SerialName("is_archived") val isArchived: Boolean = false,
    @SerialName("is_deleted") val isDeleted: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class TaskRemoteDto(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("project_id") val projectId: String? = null,
    @SerialName("order_in_project") val orderInProject: Int = 0,
    @SerialName("original_quote") val originalQuote: String = "",
    val title: String = "",
    @SerialName("simplified_steps") val simplifiedSteps: List<String> = emptyList(),
    @SerialName("current_simplify_index") val currentSimplifyIndex: Int = 0,
    @SerialName("ai_hint") val aiHint: String = "",
    @SerialName("category_badge") val categoryBadge: String = "",
    @SerialName("estimated_minutes") val estimatedMinutes: Int = 5,
    val shelf: String = "SIMPLE_STEPS",
    val difficulty: String = "EASY",
    @SerialName("is_completed") val isCompleted: Boolean = false,
    @SerialName("skipped_count") val skippedCount: Int = 0,
    @SerialName("last_shown_at") val lastShownAt: Long = 0L,
    @SerialName("ai_draft_type") val aiDraftType: String = "",
    @SerialName("ai_draft_content") val aiDraftContent: String = "",
    @SerialName("scheduled_date_millis") val scheduledDateMillis: Long? = null,
    @SerialName("scheduled_time_text") val scheduledTimeText: String? = null,
    @SerialName("context_tag") val contextTag: String = "home",
    @SerialName("context_category") val contextCategory: String = "Дом и быт",
    @SerialName("ai_suggested_steps") val aiSuggestedSteps: List<String> = emptyList(),
    @SerialName("ai_advice") val aiAdvice: String = "",
    @SerialName("is_deleted") val isDeleted: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class SubstepRemoteDto(
    val id: String? = null,
    @SerialName("task_id") val taskId: String,
    @SerialName("user_id") val userId: String,
    val text: String,
    @SerialName("is_completed") val isCompleted: Boolean = false,
    @SerialName("order_in_task") val orderInTask: Int = 0
)
