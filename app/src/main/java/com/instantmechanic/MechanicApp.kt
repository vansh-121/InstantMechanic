package com.instantmechanic

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application entry point.
 *
 * `@HiltAndroidApp` triggers Hilt's code generation and creates the singleton component that lives
 * as long as the process — which is what makes `@Singleton` bindings like the OkHttp client and the
 * repository shared across every screen instead of rebuilt per navigation.
 */
@HiltAndroidApp
class MechanicApp : Application()
