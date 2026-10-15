package com.sbros.app.di

import com.sbros.app.data.repository.SbrosRepositoryImpl
import com.sbros.app.domain.repository.SbrosRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindSbrosRepository(
        sbrosRepositoryImpl: SbrosRepositoryImpl
    ): SbrosRepository
}
