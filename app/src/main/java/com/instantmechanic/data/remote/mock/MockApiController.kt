package com.instantmechanic.data.remote.mock

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Debug switch that makes the next API call fail.
 *
 * Demonstrating a network-error state is otherwise awkward — you have to disable connectivity at
 * exactly the right moment. Exposing it as a toggle in the debug build means the error UI and its
 * Retry path can be shown deliberately, and it also gives the instrumentation-free unit tests a
 * way to exercise the failure branch end to end.
 *
 * Only surfaced in debug builds running against the mock backend.
 */
@Singleton
class MockApiController @Inject constructor() {

    private val _simulateFailure = MutableStateFlow(false)
    val simulateFailure: StateFlow<Boolean> = _simulateFailure.asStateFlow()

    fun setSimulateFailure(enabled: Boolean) {
        _simulateFailure.value = enabled
    }

    fun toggleSimulateFailure() {
        _simulateFailure.value = !_simulateFailure.value
    }
}
