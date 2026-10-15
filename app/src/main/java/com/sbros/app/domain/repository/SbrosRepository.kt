package com.sbros.app.domain.repository

import com.sbros.app.data.local.entity.ProjectEntity
import com.sbros.app.data.local.entity.TaskEntity
import com.sbros.app.domain.model.EnergyLevel
import com.sbros.app.domain.model.ProjectWithFirstStep
import com.sbros.app.domain.model.ShelfType
import com.sbros.app.domain.model.TaskItem
import kotlinx.coroutines.flow.Flow

interface SbrosRepository {
    fun getFocusTasksByEnergy(energyLevel: EnergyLevel): Flow<List<TaskItem>>
    fun getTasksByShelf(shelf: ShelfType): Flow<List<TaskItem>>
    fun getProjectsWithFirstStep(): Flow<List<ProjectWithFirstStep>>
    suspend fun getTaskById(taskId: String): TaskItem?
    suspend fun getAllActiveProjects(): List<ProjectEntity>
    suspend fun findProjectByTitle(title: String): ProjectEntity?
    suspend fun insertProject(project: ProjectEntity)
    suspend fun insertTasks(tasks: List<TaskEntity>)
    suspend fun getTasksForProject(projectId: String): List<TaskItem>
    suspend fun simplifyTask(taskId: String, nextIndex: Int)
    suspend fun skipTask(taskId: String)
    suspend fun completeTask(taskId: String)
    suspend fun applySoftAmnesty(): Int
}
