package com.sbros.app.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.sbros.app.data.local.ProjectDao
import com.sbros.app.data.local.SbrosDatabase
import com.sbros.app.data.local.SeedData
import com.sbros.app.data.local.TaskDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Provider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        databaseProvider: Provider<SbrosDatabase>
    ): SbrosDatabase {
        return Room.databaseBuilder(
            context,
            SbrosDatabase::class.java,
            SbrosDatabase.DATABASE_NAME
        )
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    CoroutineScope(Dispatchers.IO).launch {
                        val database = databaseProvider.get()
                        database.projectDao().insertProjects(SeedData.initialProjects)
                        database.taskDao().insertTasks(SeedData.initialTasks)
                    }
                }

                override fun onDestructiveMigration(db: SupportSQLiteDatabase) {
                    super.onDestructiveMigration(db)
                    CoroutineScope(Dispatchers.IO).launch {
                        val database = databaseProvider.get()
                        database.projectDao().insertProjects(SeedData.initialProjects)
                        database.taskDao().insertTasks(SeedData.initialTasks)
                    }
                }
            })
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideTaskDao(database: SbrosDatabase): TaskDao = database.taskDao()

    @Provides
    @Singleton
    fun provideProjectDao(database: SbrosDatabase): ProjectDao = database.projectDao()
}
