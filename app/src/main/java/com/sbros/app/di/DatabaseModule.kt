package com.sbros.app.di

import android.content.Context
import androidx.room.Room
import com.sbros.app.data.local.ProjectDao
import com.sbros.app.data.local.SbrosDatabase
import com.sbros.app.data.local.TaskDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): SbrosDatabase {
        return Room.databaseBuilder(
            context,
            SbrosDatabase::class.java,
            SbrosDatabase.DATABASE_NAME
        )
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
