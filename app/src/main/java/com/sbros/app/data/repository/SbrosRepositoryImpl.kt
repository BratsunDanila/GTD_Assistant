package com.sbros.app.data.repository

import com.sbros.app.data.local.ProjectDao
import com.sbros.app.data.local.TaskDao
import com.sbros.app.data.local.entity.ProjectEntity
import com.sbros.app.data.local.entity.TaskEntity
import com.sbros.app.domain.model.EnergyLevel
import com.sbros.app.domain.model.ProjectWithFirstStep
import com.sbros.app.domain.model.ShelfType
import com.sbros.app.domain.model.TaskItem
import com.sbros.app.domain.model.toDomain
import com.sbros.app.domain.repository.SbrosRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

import com.sbros.app.domain.model.TaskDifficulty
import com.sbros.app.domain.model.toEntity

@Singleton
class SbrosRepositoryImpl @Inject constructor(
    private val taskDao: TaskDao,
    private val projectDao: ProjectDao
) : SbrosRepository {

    override fun getAllTasks(): Flow<List<TaskItem>> {
        return taskDao.getAllTasks().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getFocusTasksByDifficulty(difficulty: TaskDifficulty): Flow<List<TaskItem>> {
        return taskDao.getFocusTasksByDifficulty(difficulty).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getFocusTasksByEnergy(energyLevel: EnergyLevel): Flow<List<TaskItem>> {
        return getFocusTasksByDifficulty(energyLevel)
    }

    override suspend fun updateTask(task: TaskItem) {
        taskDao.updateTask(task.toEntity())
    }

    override fun getTasksByShelf(shelf: ShelfType): Flow<List<TaskItem>> {
        return taskDao.getTasksByShelf(shelf).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getProjectsWithFirstStep(): Flow<List<ProjectWithFirstStep>> {
        return projectDao.getActiveProjects().map { projects ->
            projects.map { project ->
                val firstStepEntity = taskDao.getFirstUncompletedTaskForProject(project.id)
                val allProjectTasks = taskDao.getTasksForProjectList(project.id).map { it.toDomain() }
                ProjectWithFirstStep(
                    projectId = project.id,
                    projectTitle = project.title,
                    firstUncompletedStep = firstStepEntity?.toDomain(),
                    allSteps = allProjectTasks,
                    isArchived = project.isArchived
                )
            }
        }
    }

    override suspend fun getTaskById(taskId: String): TaskItem? {
        return taskDao.getTaskById(taskId)?.toDomain()
    }

    override suspend fun getAllActiveProjects(): List<ProjectEntity> {
        return projectDao.getActiveProjectsList()
    }

    override suspend fun findProjectByTitle(title: String): ProjectEntity? {
        return projectDao.findProjectByTitle(title)
    }

    override suspend fun insertProject(project: ProjectEntity) {
        projectDao.insertProject(project)
    }

    override suspend fun insertTasks(tasks: List<TaskEntity>) {
        taskDao.insertTasks(tasks)
    }

    override suspend fun getTasksForProject(projectId: String): List<TaskItem> {
        return taskDao.getTasksForProjectList(projectId).map { it.toDomain() }
    }

    override suspend fun simplifyTask(taskId: String, nextIndex: Int) {
        taskDao.updateSimplifyIndex(taskId, nextIndex)
    }

    override suspend fun skipTask(taskId: String) {
        taskDao.skipTask(taskId, System.currentTimeMillis())
    }

    override suspend fun completeTask(taskId: String) {
        taskDao.setTaskCompleted(taskId, true)
    }

    override suspend fun applySoftAmnesty(): Int {
        return taskDao.applySoftAmnesty(System.currentTimeMillis())
    }

    override fun getScheduledTasks(): Flow<List<TaskItem>> {
        return taskDao.getScheduledTasks().map { list -> list.map { it.toDomain() } }
    }

    override fun getTasksForDate(startOfDay: Long, endOfDay: Long): Flow<List<TaskItem>> {
        return taskDao.getTasksForDate(startOfDay, endOfDay).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun updateTaskSchedule(taskId: String, dateMillis: Long?, timeText: String?) {
        taskDao.updateTaskSchedule(taskId, dateMillis, timeText)
    }

    override fun getQuickWinTask(): Flow<TaskItem?> {
        return taskDao.getQuickWinTask().map { it?.toDomain() }
    }
}

