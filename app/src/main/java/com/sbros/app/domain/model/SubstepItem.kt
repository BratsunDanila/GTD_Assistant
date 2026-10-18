package com.sbros.app.domain.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class SubstepItem(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isDone: Boolean = false
)
