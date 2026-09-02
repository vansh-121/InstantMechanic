package com.instantmechanic.di

import javax.inject.Qualifier

/** Marks the dispatcher used for disk/network work, so it can be swapped in tests. */
@Retention(AnnotationRetention.BINARY)
@Qualifier
annotation class IoDispatcher
