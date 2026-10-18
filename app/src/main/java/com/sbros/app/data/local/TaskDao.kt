package com.sbros.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.sbros.app.data.local.entity.TaskEntity
import com.sbros.app.domain.model.EnergyLevel
import com.sbros.app.domain.model.ShelfType
import com.sbros.app.domain.model.TaskDifficulty
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Query("SELECT * FROM tasks WHERE isCompleted = 0 ORDER BY createdAt DESC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Query(
        """
        SELECT * FROM tasks 
        WHERE difficulty = :difficulty 
          AND isCompleted = 0 
          AND shelf IN ('SIMPLE_STEPS', 'BIG_THINGS') 
        ORDER BY lastShownAt ASC
        """
    )
    fun getFocusTasksByDifficulty(difficulty: TaskDifficulty): Flow<List<TaskEntity>>

    @Query(
        """
        SELECT * FROM tasks 
        WHERE difficulty = :energyLevel 
          AND isCompleted = 0 
          AND shelf IN ('SIMPLE_STEPS', 'BIG_THINGS') 
        ORDER BY lastShownAt ASC
        """
    )
    fun getFocusTasksByEnergy(energyLevel: EnergyLevel): Flow<List<TaskEntity>>

    @Query(
        """
        SELECT * FROM tasks 
        WHERE shelf = :shelf 
          AND isCompleted = 0 
        ORDER BY createdAt DESC
        """
    )
    fun getTasksByShelf(shelf: ShelfType): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE projectId = :projectId ORDER BY orderInProject ASC")
    fun getTasksByProject(projectId: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE projectId = :projectId ORDER BY orderInProject ASC")
    suspend fun getTasksForProjectList(projectId: String): List<TaskEntity>

    @Query("SELECT COUNT(*) FROM tasks WHERE projectId = :projectId")
    suspend fun getTaskCountForProject(projectId: String): Int

    @Query(
        """
        SELECT * FROM tasks 
        WHERE projectId = :projectId 
          AND isCompleted = 0 
        ORDER BY orderInProject ASC 
        LIMIT 1
        """
    )
    suspend fun getFirstUncompletedTaskForProject(projectId: String): TaskEntity?

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskById(id: String): TaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<TaskEntity>)

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    @Query("UPDATE tasks SET isCompleted = :isCompleted WHERE id = :taskId")
    suspend fun setTaskCompleted(taskId: String, isCompleted: Boolean = true)

    @Query("UPDATE tasks SET currentSimplifyIndex = :simplifyIndex WHERE id = :taskId")
    suspend fun updateSimplifyIndex(taskId: String, simplifyIndex: Int)

    @Query("UPDATE tasks SET skippedCount = skippedCount + 1, lastShownAt = :now WHERE id = :taskId")
    suspend fun skipTask(taskId: String, now: Long = System.currentTimeMillis())

    @Query(
        """
        UPDATE tasks 
        SET shelf = 'SOMEDAY' 
        WHERE isCompleted = 0 
          AND shelf != 'SOMEDAY' 
          AND (skippedCount >= 5 OR (:now - createdAt >= 1209600000))
        """
    )
    suspend fun applySoftAmnesty(now: Long = System.currentTimeMillis()): Int

    @Query("SELECT * FROM tasks WHERE scheduledDateMillis IS NOT NULL AND isCompleted = 0 ORDER BY scheduledDateMillis ASC")
    fun getScheduledTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE scheduledDateMillis >= :startOfDay AND scheduledDateMillis <= :endOfDay AND isCompleted = 0 ORDER BY scheduledDateMillis ASC")
    fun getTasksForDate(startOfDay: Long, endOfDay: Long): Flow<List<TaskEntity>>

    @Query("UPDATE tasks SET scheduledDateMillis = :dateMillis, scheduledTimeText = :timeText WHERE id = :taskId")
    suspend fun updateTaskSchedule(taskId: String, dateMillis: Long?, timeText: String?)

    @Query("SELECT COUNT(*) FROM tasks")
    suspend fun getTaskCount(): Int

    @Query(
        """
        SELECT * FROM tasks 
        WHERE estimatedMinutes <= 2 
          AND isCompleted = 0 
          AND shelf IN ('SIMPLE_STEPS', 'BIG_THINGS')
        ORDER BY estimatedMinutes ASC, lastShownAt ASC
        LIMIT 1
        """
    )
    fun getQuickWinTask(): Flow<TaskEntity?>
}

