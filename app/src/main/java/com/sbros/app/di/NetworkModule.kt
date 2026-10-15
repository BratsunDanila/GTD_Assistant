package com.sbros.app.di

import com.sbros.app.data.remote.AiParserApi
import com.sbros.app.data.remote.AiParserApiImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class NetworkModule {

    @Binds
    @Singleton
    abstract fun bindAiParserApi(
        aiParserApiImpl: AiParserApiImpl
    ): AiParserApi
}
