package com.sbros.app.di

import com.sbros.app.data.remote.SupabaseConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.serializer.KotlinXSerializer
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.serialization.json.Json
import okhttp3.ConnectionPool
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds

@Module
@InstallIn(SingletonComponent::class)
object SupabaseModule {

    @Provides
    @Singleton
    @OptIn(io.github.jan.supabase.annotations.SupabaseInternal::class)
    fun provideSupabaseClient(): SupabaseClient {
        return createSupabaseClient(
            supabaseUrl = SupabaseConfig.DEFAULT_URL,
            supabaseKey = SupabaseConfig.DEFAULT_ANON_KEY
        ) {
            httpEngine = OkHttp.create {
                config {
                    pingInterval(15, TimeUnit.SECONDS)
                    retryOnConnectionFailure(true)
                    connectionPool(ConnectionPool(5, 30, TimeUnit.SECONDS))
                    connectTimeout(30, TimeUnit.SECONDS)
                    readTimeout(30, TimeUnit.SECONDS)
                    writeTimeout(30, TimeUnit.SECONDS)
                }
            }
            httpConfig {
                install(io.ktor.client.plugins.HttpTimeout) {
                    requestTimeoutMillis = 30_000L
                    connectTimeoutMillis = 30_000L
                    socketTimeoutMillis = 30_000L
                }
            }
            install(Auth)
            install(Postgrest)
            defaultSerializer = KotlinXSerializer(
                Json {
                    ignoreUnknownKeys = true
                    encodeDefaults = true
                }
            )
            requestTimeout = 30.seconds
        }
    }
}
