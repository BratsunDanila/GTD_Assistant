package com.sbros.app.domain.model

data class ProjectWithFirstStep(
    val projectId: String,
    val projectTitle: String,
    val firstUncompletedStep: TaskItem?,
    val allSteps: List<TaskItem> = emptyList(),
    val isArchived: Boolean = false
)
