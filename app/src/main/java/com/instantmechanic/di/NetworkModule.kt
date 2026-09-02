package com.instantmechanic.di

import android.content.Context
import com.instantmechanic.BuildConfig
import com.instantmechanic.data.remote.MechanicApiService
import com.instantmechanic.data.remote.mock.MockApiController
import com.instantmechanic.data.remote.mock.MockApiInterceptor
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.time.Clock
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * The HTTP stack.
 *
 * The only thing that changes between the bundled mock backend and a live server is whether
 * [MockApiInterceptor] is installed — Retrofit, the converter, the service interface and every
 * layer above are identical either way. Flip `instantmechanic.useMockApi` in `gradle.properties`.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val ASSET_MECHANICS = "mock/mechanics.json"
    private const val TIMEOUT_SECONDS = 30L

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        // Tolerate fields the client does not model yet — a server adding a field must not
        // break an already-shipped app.
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
        encodeDefaults = true
    }

    @Provides
    @Singleton
    fun provideMockApiInterceptor(
        @ApplicationContext context: Context,
        json: Json,
        clock: Clock,
        controller: MockApiController,
    ): MockApiInterceptor = MockApiInterceptor(
        json = json,
        clock = clock,
        assetReader = {
            context.assets.open(ASSET_MECHANICS).bufferedReader().use { it.readText() }
        },
        shouldFail = { controller.simulateFailure.value },
    )

    @Provides
    @Singleton
    fun provideOkHttpClient(
        mockApiInterceptor: MockApiInterceptor,
    ): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .apply {
            if (BuildConfig.DEBUG) {
                addInterceptor(
                    HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY },
                )
            }
            // Installed last so the logging interceptor above still sees the traffic.
            if (BuildConfig.USE_MOCK_API) {
                addInterceptor(mockApiInterceptor)
            }
        }
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient, json: Json): Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    @Provides
    @Singleton
    fun provideMechanicApiService(retrofit: Retrofit): MechanicApiService =
        retrofit.create(MechanicApiService::class.java)
}
