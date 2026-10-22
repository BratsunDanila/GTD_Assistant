package com.sbros.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.sbros.app.data.local.entity.ProjectEntity
import com.sbros.app.data.local.entity.TaskEntity

@Database(
    entities = [ProjectEntity::class, TaskEntity::class],
    version = 6,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class SbrosDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun projectDao(): ProjectDao

    companion object {
        const val DATABASE_NAME = "sbros_database"
    }
}
