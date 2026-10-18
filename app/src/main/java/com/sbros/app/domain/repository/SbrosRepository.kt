package com.sbros.app.domain.repository

import com.sbros.app.data.local.entity.ProjectEntity
import com.sbros.app.data.local.entity.TaskEntity
import com.sbros.app.domain.model.EnergyLevel
import com.sbros.app.domain.model.ProjectWithFirstStep
import com.sbros.app.domain.model.ShelfType
import com.sbros.app.domain.model.TaskItem
import com.sbros.app.domain.model.TaskDifficulty
import kotlinx.coroutines.flow.Flow

interface SbrosRepository {
    fun getAllTasks(): Flow<List<TaskItem>>
    fun getFocusTasksByDifficulty(difficulty: TaskDifficulty): Flow<List<TaskItem>>
    fun getFocusTasksByEnergy(energyLevel: EnergyLevel): Flow<List<TaskItem>>
    fun getTasksByShelf(shelf: ShelfType): Flow<List<TaskItem>>
    fun getProjectsWithFirstStep(): Flow<List<ProjectWithFirstStep>>
    suspend fun getTaskById(taskId: String): TaskItem?
    suspend fun getAllActiveProjects(): List<ProjectEntity>
    suspend fun findProjectByTitle(title: String): ProjectEntity?
    suspend fun insertProject(project: ProjectEntity)
    suspend fun insertTasks(tasks: List<TaskEntity>)
    suspend fun updateTask(task: TaskItem)
    suspend fun getTasksForProject(projectId: String): List<TaskItem>
    suspend fun simplifyTask(taskId: String, nextIndex: Int)
    suspend fun skipTask(taskId: String)
    suspend fun completeTask(taskId: String)
    suspend fun applySoftAmnesty(): Int
    fun getScheduledTasks(): Flow<List<TaskItem>>
    fun getTasksForDate(startOfDay: Long, endOfDay: Long): Flow<List<TaskItem>>
    suspend fun updateTaskSchedule(taskId: String, dateMillis: Long?, timeText: String?)
    fun getQuickWinTask(): Flow<TaskItem?>
}

