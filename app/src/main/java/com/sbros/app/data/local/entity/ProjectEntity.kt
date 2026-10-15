package com.sbros.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,               // Например: "Курсовая работа"
    val createdAt: Long = System.currentTimeMillis(),
    val isArchived: Boolean = false
)
