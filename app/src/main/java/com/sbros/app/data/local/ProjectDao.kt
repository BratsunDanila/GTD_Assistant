package com.sbros.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.sbros.app.data.local.entity.ProjectEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {

    @Query("SELECT * FROM projects WHERE isArchived = 0 AND isDeleted = 0 ORDER BY createdAt DESC")
    fun getActiveProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE isArchived = 0 AND isDeleted = 0 ORDER BY createdAt DESC")
    suspend fun getActiveProjectsList(): List<ProjectEntity>

    @Query("SELECT * FROM projects WHERE isDeleted = 0 ORDER BY createdAt DESC")
    fun getAllProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun getProjectById(id: String): ProjectEntity?

    @Query("SELECT * FROM projects WHERE LOWER(title) = LOWER(:title) AND isDeleted = 0 LIMIT 1")
    suspend fun findProjectByTitle(title: String): ProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProjects(projects: List<ProjectEntity>)

    @Update
    suspend fun updateProject(project: ProjectEntity)

    @Delete
    suspend fun deleteProject(project: ProjectEntity)

    @Query(
        """
        UPDATE projects 
        SET isArchived = 1, 
            syncStatus = CASE WHEN syncStatus = 'PENDING_INSERT' THEN 'PENDING_INSERT' ELSE 'PENDING_UPDATE' END, 
            updatedAt = :now 
        WHERE id = :id
        """
    )
    suspend fun archiveProject(id: String, now: Long = System.currentTimeMillis())

    @Query(
        """
        UPDATE projects 
        SET isDeleted = 1, 
            syncStatus = 'PENDING_DELETE', 
            updatedAt = :now 
        WHERE id = :projectId
        """
    )
    suspend fun softDeleteProject(projectId: String, now: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM projects WHERE isDeleted = 0")
    suspend fun getProjectCount(): Int

    // --- Offline-First Sync Queries ---

    @Query("SELECT * FROM projects WHERE syncStatus != 'SYNCED' AND userId IS NOT NULL")
    suspend fun getPendingProjects(): List<ProjectEntity>

    @Query("UPDATE projects SET syncStatus = 'SYNCED' WHERE id IN (:ids)")
    suspend fun markProjectsSynced(ids: List<String>)

    @Query("UPDATE projects SET userId = :userId, syncStatus = 'PENDING_INSERT', updatedAt = :now WHERE userId IS NULL")
    suspend fun assignUserToGuestProjects(userId: String, now: Long = System.currentTimeMillis())

    @Query("DELETE FROM projects WHERE isDeleted = 1 AND syncStatus = 'SYNCED'")
    suspend fun purgeDeletedSyncedProjects()
}
