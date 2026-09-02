package com.instantmechanic.di

import com.instantmechanic.data.repository.MechanicRepositoryImpl
import com.instantmechanic.domain.repository.MechanicRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Binds the domain-layer abstraction to its data-layer implementation. */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindMechanicRepository(impl: MechanicRepositoryImpl): MechanicRepository
}
