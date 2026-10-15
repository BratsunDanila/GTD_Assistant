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

@Singleton
class SbrosRepositoryImpl @Inject constructor(
    private val taskDao: TaskDao,
    private val projectDao: ProjectDao
) : SbrosRepository {

    override fun getFocusTasksByEnergy(energyLevel: EnergyLevel): Flow<List<TaskItem>> {
        return taskDao.getFocusTasksByEnergy(energyLevel).map { entities ->
            entities.map { it.toDomain() }
        }
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
}
