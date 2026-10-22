package com.sbros.app.sync

import android.util.Log
import com.sbros.app.data.local.PreferencesManager
import com.sbros.app.data.local.ProjectDao
import com.sbros.app.data.local.TaskDao
import com.sbros.app.data.local.entity.ProjectEntity
import com.sbros.app.data.local.entity.TaskEntity
import com.sbros.app.data.remote.SupabaseConfig
import com.sbros.app.data.remote.dto.ProjectRemoteDto
import com.sbros.app.data.remote.dto.TaskRemoteDto
import com.sbros.app.domain.model.AppSyncState
import com.sbros.app.domain.model.ShelfType
import com.sbros.app.domain.model.SyncStatus
import com.sbros.app.domain.model.TaskDifficulty
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncManager @Inject constructor(
    private val supabase: SupabaseClient,
    private val taskDao: TaskDao,
    private val projectDao: ProjectDao,
    private val preferencesManager: PreferencesManager
) {
    private val TAG = "SyncManager"
    private val syncMutex = Mutex()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _syncState = MutableStateFlow(AppSyncState.IDLE)
    val syncState: StateFlow<AppSyncState> = _syncState.asStateFlow()

    fun triggerSync() {
        scope.launch {
            syncNow()
        }
    }

    suspend fun syncNow(): Result<Unit> = syncMutex.withLock {
        val userId = preferencesManager.getUserId()
        if (userId.isNullOrBlank() || preferencesManager.isGuest()) {
            _syncState.value = AppSyncState.NOT_AUTHENTICATED
            return Result.success(Unit)
        }

        // Check if placeholder URL is still configured
        val customUrl = preferencesManager.supabaseUrlFlow
        if (SupabaseConfig.DEFAULT_URL.contains("your-project-id")) {
            Log.d(TAG, "Default demo Supabase URL in use - skipping cloud sync")
            _syncState.value = AppSyncState.IDLE
            return Result.success(Unit)
        }

        _syncState.value = AppSyncState.SYNCING

        return try {
            // 1. Push local pending changes to Supabase
            pushPendingProjects(userId)
            pushPendingTasks(userId)

            // 2. Pull remote changes from Supabase (LWW resolution)
            pullRemoteProjects(userId)
            pullRemoteTasks(userId)

            preferencesManager.updateLastSyncedTime()
            _syncState.value = AppSyncState.IDLE
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Sync failed: ${e.message}", e)
            val isNetworkIssue = e is java.net.ConnectException ||
                    e is java.net.UnknownHostException ||
                    e is java.io.IOException ||
                    e.message?.contains("Unable to resolve host", ignoreCase = true) == true

            _syncState.value = if (isNetworkIssue) AppSyncState.OFFLINE else AppSyncState.ERROR
            Result.failure(e)
        }
    }

    private suspend fun pushPendingProjects(userId: String) {
        val pendingProjects = projectDao.getPendingProjects()
        if (pendingProjects.isEmpty()) return

        val dtos = pendingProjects.map { it.toRemoteDto(userId) }
        supabase.from("projects").upsert(dtos)

        val projectIds = pendingProjects.map { it.id }
        projectDao.markProjectsSynced(projectIds)
        projectDao.purgeDeletedSyncedProjects()
        Log.d(TAG, "Successfully pushed ${dtos.size} projects")
    }

    private suspend fun pushPendingTasks(userId: String) {
        val pendingTasks = taskDao.getPendingTasks()
        if (pendingTasks.isEmpty()) return

        val dtos = pendingTasks.map { it.toRemoteDto(userId) }
        supabase.from("tasks").upsert(dtos)

        val taskIds = pendingTasks.map { it.id }
        taskDao.markTasksSynced(taskIds)
        taskDao.purgeDeletedSyncedTasks()
        Log.d(TAG, "Successfully pushed ${dtos.size} tasks")
    }

    private suspend fun pullRemoteProjects(userId: String) {
        val remoteProjects = supabase.from("projects").select {
            filter {
                eq("user_id", userId)
            }
        }.decodeList<ProjectRemoteDto>()

        for (remote in remoteProjects) {
            val local = projectDao.getProjectById(remote.id)
            val remoteUpdated = remote.updatedAt.toEpochMillis()

            if (local == null) {
                // If not locally present and not deleted remotely, insert
                if (!remote.isDeleted) {
                    projectDao.insertProject(remote.toEntity())
                }
            } else {
                // Last-Write-Wins: overwrite only if remote is newer or local is already SYNCED
                if (local.syncStatus == SyncStatus.SYNCED || remoteUpdated > local.updatedAt) {
                    projectDao.insertProject(remote.toEntity())
                }
            }
        }
    }

    private suspend fun pullRemoteTasks(userId: String) {
        val remoteTasks = supabase.from("tasks").select {
            filter {
                eq("user_id", userId)
            }
        }.decodeList<TaskRemoteDto>()

        for (remote in remoteTasks) {
            val local = taskDao.getTaskById(remote.id)
            val remoteUpdated = remote.updatedAt.toEpochMillis()

            if (local == null) {
                // If not locally present and not deleted remotely, insert
                if (!remote.isDeleted) {
                    taskDao.insertTask(remote.toEntity())
                }
            } else {
                // Last-Write-Wins: overwrite only if remote is newer or local is already SYNCED
                if (local.syncStatus == SyncStatus.SYNCED || remoteUpdated > local.updatedAt) {
                    taskDao.insertTask(remote.toEntity(existingSubsteps = local.substeps))
                }
            }
        }
    }

    // --- Helpers & Mappers ---

    private fun Long.toIsoString(): String = Instant.ofEpochMilli(this).toString()

    private fun String?.toEpochMillis(fallback: Long = System.currentTimeMillis()): Long {
        if (this.isNullOrBlank()) return fallback
        return try {
            Instant.parse(this).toEpochMilli()
        } catch (_: Exception) {
            fallback
        }
    }

    private fun ProjectEntity.toRemoteDto(userId: String): ProjectRemoteDto = ProjectRemoteDto(
        id = id,
        userId = userId,
        title = title,
        isArchived = isArchived,
        isDeleted = isDeleted,
        createdAt = createdAt.toIsoString(),
        updatedAt = updatedAt.toIsoString()
    )

    private fun ProjectRemoteDto.toEntity(): ProjectEntity = ProjectEntity(
        id = id,
        userId = userId,
        title = title,
        createdAt = createdAt.toEpochMillis(),
        updatedAt = updatedAt.toEpochMillis(),
        isArchived = isArchived,
        isDeleted = isDeleted,
        syncStatus = SyncStatus.SYNCED
    )

    private fun TaskEntity.toRemoteDto(userId: String): TaskRemoteDto = TaskRemoteDto(
        id = id,
        userId = userId,
        projectId = projectId,
        orderInProject = orderInProject,
        originalQuote = originalQuote,
        title = title,
        simplifiedSteps = simplifiedSteps,
        currentSimplifyIndex = currentSimplifyIndex,
        aiHint = aiHint,
        categoryBadge = categoryBadge,
        estimatedMinutes = estimatedMinutes,
        shelf = shelf.name,
        difficulty = difficulty.name,
        isCompleted = isCompleted,
        skippedCount = skippedCount,
        lastShownAt = lastShownAt,
        aiDraftType = aiDraftType,
        aiDraftContent = aiDraftContent,
        scheduledDateMillis = scheduledDateMillis,
        scheduledTimeText = scheduledTimeText,
        contextTag = contextTag,
        contextCategory = contextCategory,
        aiSuggestedSteps = aiSuggestedSteps,
        aiAdvice = aiAdvice,
        isDeleted = isDeleted,
        createdAt = createdAt.toIsoString(),
        updatedAt = updatedAt.toIsoString()
    )

    private fun TaskRemoteDto.toEntity(existingSubsteps: List<com.sbros.app.domain.model.SubstepItem> = emptyList()): TaskEntity = TaskEntity(
        id = id,
        userId = userId,
        projectId = projectId,
        orderInProject = orderInProject,
        originalQuote = originalQuote,
        title = title,
        simplifiedSteps = simplifiedSteps,
        currentSimplifyIndex = currentSimplifyIndex,
        aiHint = aiHint,
        categoryBadge = categoryBadge,
        estimatedMinutes = estimatedMinutes,
        shelf = try { ShelfType.valueOf(shelf) } catch (_: Exception) { ShelfType.SIMPLE_STEPS },
        difficulty = try { TaskDifficulty.valueOf(difficulty) } catch (_: Exception) { TaskDifficulty.EASY },
        isCompleted = isCompleted,
        skippedCount = skippedCount,
        lastShownAt = lastShownAt,
        createdAt = createdAt.toEpochMillis(),
        updatedAt = updatedAt.toEpochMillis(),
        isDeleted = isDeleted,
        syncStatus = SyncStatus.SYNCED,
        aiDraftType = aiDraftType,
        aiDraftContent = aiDraftContent,
        scheduledDateMillis = scheduledDateMillis,
        scheduledTimeText = scheduledTimeText,
        contextTag = contextTag,
        contextCategory = contextCategory,
        substeps = existingSubsteps,
        aiSuggestedSteps = aiSuggestedSteps,
        aiAdvice = aiAdvice
    )
}
