package com.sbros.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.sbros.app.domain.model.SyncStatus
import java.util.UUID

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String? = null,
    val title: String,               // Например: "Курсовая работа"
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isArchived: Boolean = false,
    val isDeleted: Boolean = false,
    val syncStatus: SyncStatus = SyncStatus.PENDING_INSERT
)
