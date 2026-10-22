package com.sbros.app.sync

import android.util.Log
import com.sbros.app.data.local.PreferencesManager
import com.sbros.app.data.local.ProjectDao
import com.sbros.app.data.local.TaskDao
import com.sbros.app.data.local.entity.ProjectEntity
import com.sbros.app.data.local.entity.TaskEntity
import com.sbros.app.data.remote.SupabaseConfig
import com.sbros.app.data.remote.dto.ProjectRemoteDto
import com.sbros.app.data.remote.dto.SubstepRemoteDto
import com.sbros.app.data.remote.dto.TaskLegacyRemoteDto
import com.sbros.app.data.remote.dto.TaskRemoteDto
import com.sbros.app.domain.model.AppSyncState
import com.sbros.app.domain.model.ShelfType
import com.sbros.app.domain.model.SubstepItem
import com.sbros.app.domain.model.SyncStatus
import com.sbros.app.domain.model.TaskDifficulty
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
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
    private var syncJob: kotlinx.coroutines.Job? = null
    private var pendingSyncRequested = false
    private var serverSupportsJsonbSteps = false

    private val _syncState = MutableStateFlow(AppSyncState.IDLE)
    val syncState: StateFlow<AppSyncState> = _syncState.asStateFlow()

    fun triggerSync() {
        Log.d(TAG, "triggerSync called")
        scope.launch {
            if (syncMutex.isLocked) {
                Log.d(TAG, "Sync already in progress, queuing pending follow-up sync")
                pendingSyncRequested = true
                return@launch
            }

            syncJob?.cancel()
            syncJob = launch {
                kotlinx.coroutines.delay(500L)
                doSyncWithAutoRetry()
            }
        }
    }

    private suspend fun doSyncWithAutoRetry() {
        var attempts = 0
        while (attempts < 2) {
            attempts++
            val res = syncNow()
            if (res.isSuccess) {
                break
            } else {
                val err = res.exceptionOrNull()
                Log.w(TAG, "Sync attempt $attempts failed: ${err?.message}")
                if (attempts < 2) {
                    Log.d(TAG, "Auto-retrying sync immediately with fresh connection...")
                    kotlinx.coroutines.delay(500L)
                }
            }
        }

        if (pendingSyncRequested) {
            pendingSyncRequested = false
            Log.d(TAG, "Executing queued follow-up sync")
            syncNow()
        }
    }

    suspend fun syncNow(): Result<Unit> = syncMutex.withLock {
        Log.d(TAG, "syncNow started")
        val userId = preferencesManager.getUserId()
        Log.d(TAG, "syncNow userId: $userId, isGuest: ${preferencesManager.isGuest()}")
        if (userId.isNullOrBlank() || preferencesManager.isGuest()) {
            _syncState.value = AppSyncState.NOT_AUTHENTICATED
            return@withLock Result.success(Unit)
        }

        // Check if placeholder URL is still configured
        if (SupabaseConfig.DEFAULT_URL.contains("your-project-id")) {
            Log.d(TAG, "Default demo Supabase URL in use - skipping cloud sync")
            _syncState.value = AppSyncState.IDLE
            return@withLock Result.success(Unit)
        }

        _syncState.value = AppSyncState.SYNCING

        try {
            // 1. Push local pending changes to Supabase
            pushPendingProjects(userId)
            pushPendingTasks(userId)

            // 2. Pull remote changes from Supabase (LWW resolution)
            pullRemoteProjects(userId)
            pullRemoteTasks(userId)

            preferencesManager.updateLastSyncedTime()
            _syncState.value = AppSyncState.IDLE
            Log.d(TAG, "syncNow completed successfully")
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

        if (!serverSupportsJsonbSteps) {
            val legacyDtos = pendingTasks.map { it.toLegacyRemoteDto(userId) }
            supabase.from("tasks").upsert(legacyDtos)
        } else {
            try {
                val dtos = pendingTasks.map { it.toRemoteDto(userId) }
                supabase.from("tasks").upsert(dtos)
            } catch (e: Exception) {
                val msg = e.message ?: ""
                if (msg.contains("ai_suggested_steps", ignoreCase = true) ||
                    msg.contains("simplified_steps", ignoreCase = true)) {
                    Log.w(TAG, "Database missing JSONB step columns, falling back to legacy schema: $msg")
                    serverSupportsJsonbSteps = false
                    val legacyDtos = pendingTasks.map { it.toLegacyRemoteDto(userId) }
                    supabase.from("tasks").upsert(legacyDtos)
                } else {
                    throw e
                }
            }
        }

        // Push substeps to task_substeps
        val substepsToUpsert = pendingTasks.filterNot { it.isDeleted }.flatMap { task ->
            task.substeps.mapIndexed { idx, sub ->
                SubstepRemoteDto(
                    id = sub.id,
                    taskId = task.id,
                    userId = userId,
                    text = sub.text,
                    isDone = sub.isDone,
                    sortOrder = idx,
                    isDeleted = false,
                    createdAt = task.createdAt.toIsoString(),
                    updatedAt = task.updatedAt.toIsoString()
                )
            }
        }

        if (substepsToUpsert.isNotEmpty()) {
            try {
                supabase.from("task_substeps").upsert(substepsToUpsert)
                Log.d(TAG, "Successfully pushed ${substepsToUpsert.size} substeps")
            } catch (e: Exception) {
                Log.w(TAG, "Failed pushing substeps to task_substeps: ${e.message}")
            }
        }

        // Delete substeps for deleted tasks
        val deletedTasks = pendingTasks.filter { it.isDeleted }
        for (delTask in deletedTasks) {
            try {
                supabase.from("task_substeps").delete {
                    filter {
                        eq("task_id", delTask.id)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed deleting substeps for task ${delTask.id}: ${e.message}")
            }
        }

        val taskIds = pendingTasks.map { it.id }
        taskDao.markTasksSynced(taskIds)
        taskDao.purgeDeletedSyncedTasks()
        Log.d(TAG, "Successfully pushed ${pendingTasks.size} tasks")
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
                if (!remote.isDeleted) {
                    projectDao.insertProject(remote.toEntity())
                }
            } else {
                if (local.syncStatus == SyncStatus.SYNCED || remoteUpdated > local.updatedAt) {
                    projectDao.insertProject(remote.toEntity())
                }
            }
        }
    }

    private suspend fun pullRemoteTasks(userId: String) {
        val remoteTasks = try {
            supabase.from("tasks").select {
                filter {
                    eq("user_id", userId)
                }
            }.decodeList<TaskRemoteDto>()
        } catch (e: Exception) {
            Log.w(TAG, "Failed decoding with TaskRemoteDto, falling back to legacy DTO: ${e.message}")
            supabase.from("tasks").select {
                filter {
                    eq("user_id", userId)
                }
            }.decodeList<TaskLegacyRemoteDto>().map { it.toRemoteDto() }
        }

        val remoteSubstepsByTaskId = try {
            supabase.from("task_substeps").select {
                filter {
                    eq("user_id", userId)
                }
            }.decodeList<SubstepRemoteDto>()
                .filterNot { it.isDeleted }
                .groupBy { it.taskId }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to pull task_substeps: ${e.message}")
            emptyMap()
        }

        for (remote in remoteTasks) {
            val local = taskDao.getTaskById(remote.id)
            val remoteUpdated = remote.updatedAt.toEpochMillis()

            val remoteSubsteps = remoteSubstepsByTaskId[remote.id]
                ?.sortedBy { it.sortOrder }
                ?.map {
                    SubstepItem(
                        id = it.id,
                        text = it.text,
                        isDone = it.isDone
                    )
                } ?: emptyList()

            val substepsToKeep = if (remoteSubsteps.isNotEmpty()) {
                remoteSubsteps
            } else {
                local?.substeps ?: emptyList()
            }

            if (local == null) {
                if (!remote.isDeleted) {
                    taskDao.insertTask(remote.toEntity(existingSubsteps = substepsToKeep))
                }
            } else {
                // If local has substeps that remote does not yet have, push them now to catch up
                if (local.substeps.isNotEmpty() && remoteSubsteps.isEmpty()) {
                    try {
                        val pushList = local.substeps.mapIndexed { idx, sub ->
                            SubstepRemoteDto(
                                id = sub.id,
                                taskId = local.id,
                                userId = userId,
                                text = sub.text,
                                isDone = sub.isDone,
                                sortOrder = idx,
                                isDeleted = local.isDeleted,
                                createdAt = local.createdAt.toIsoString(),
                                updatedAt = local.updatedAt.toIsoString()
                            )
                        }
                        supabase.from("task_substeps").upsert(pushList)
                    } catch (e: Exception) {
                        Log.w(TAG, "Could not push local-only substeps: ${e.message}")
                    }
                }

                // Last-Write-Wins: overwrite only if remote is newer or local is already SYNCED
                if (local.syncStatus == SyncStatus.SYNCED || remoteUpdated > local.updatedAt) {
                    taskDao.insertTask(remote.toEntity(existingSubsteps = substepsToKeep))
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

    private fun TaskEntity.toLegacyRemoteDto(userId: String): TaskLegacyRemoteDto = TaskLegacyRemoteDto(
        id = id,
        userId = userId,
        projectId = projectId,
        orderInProject = orderInProject,
        originalQuote = originalQuote,
        title = title,
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
        aiAdvice = aiAdvice,
        isDeleted = isDeleted,
        createdAt = createdAt.toIsoString(),
        updatedAt = updatedAt.toIsoString()
    )

    private fun TaskLegacyRemoteDto.toRemoteDto(): TaskRemoteDto = TaskRemoteDto(
        id = id,
        userId = userId,
        projectId = projectId,
        orderInProject = orderInProject,
        originalQuote = originalQuote,
        title = title,
        simplifiedSteps = emptyList(),
        currentSimplifyIndex = currentSimplifyIndex,
        aiHint = aiHint,
        categoryBadge = categoryBadge,
        estimatedMinutes = estimatedMinutes,
        shelf = shelf,
        difficulty = difficulty,
        isCompleted = isCompleted,
        skippedCount = skippedCount,
        lastShownAt = lastShownAt,
        aiDraftType = aiDraftType,
        aiDraftContent = aiDraftContent,
        scheduledDateMillis = scheduledDateMillis,
        scheduledTimeText = scheduledTimeText,
        contextTag = contextTag,
        contextCategory = contextCategory,
        aiSuggestedSteps = emptyList(),
        aiAdvice = aiAdvice,
        isDeleted = isDeleted,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private fun TaskRemoteDto.toEntity(existingSubsteps: List<SubstepItem> = emptyList()): TaskEntity = TaskEntity(
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
