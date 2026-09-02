package com.instantmechanic.di

import com.instantmechanic.domain.validation.ServiceRequestValidator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import java.time.Clock
import javax.inject.Singleton

/** App-wide, framework-agnostic collaborators. */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /**
     * Injecting the clock rather than calling `LocalDateTime.now()` is what makes "is this garage
     * open right now?" a testable question instead of a flaky one.
     */
    @Provides
    @Singleton
    fun provideClock(): Clock = Clock.systemDefaultZone()

    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @Singleton
    fun provideServiceRequestValidator(): ServiceRequestValidator = ServiceRequestValidator()
}
