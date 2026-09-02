package com.instantmechanic.core.result

/**
 * Result wrapper used by every repository call.
 *
 * The data layer never lets a raw [Throwable] escape to a ViewModel: exceptions are translated
 * into a typed [DataError] so the UI can decide on a message and whether a retry makes sense,
 * without knowing anything about OkHttp, Retrofit or kotlinx.serialization.
 */
sealed interface AppResult<out T> {
    data class Success<T>(val data: T) : AppResult<T>
    data class Failure(val error: DataError) : AppResult<Nothing>
}

/** Categories of failure the UI is prepared to render distinctly. */
enum class DataError {
    /** No connectivity, DNS failure, connection refused. */
    NETWORK,

    /** The request was made but took too long. */
    TIMEOUT,

    /** 5xx — the backend is unhealthy; retrying later is reasonable. */
    SERVER,

    /** 404 — the requested resource does not exist. Retrying will not help. */
    NOT_FOUND,

    /** The body did not match the expected schema. Indicates a client/server contract drift. */
    PARSE,

    /** Anything we did not anticipate. */
    UNKNOWN,
}

/** True when showing a "Retry" affordance is useful for this error. */
val DataError.isRetryable: Boolean
    get() = when (this) {
        DataError.NETWORK, DataError.TIMEOUT, DataError.SERVER, DataError.UNKNOWN -> true
        DataError.NOT_FOUND, DataError.PARSE -> false
    }

inline fun <T, R> AppResult<T>.map(transform: (T) -> R): AppResult<R> = when (this) {
    is AppResult.Success -> AppResult.Success(transform(data))
    is AppResult.Failure -> this
}

inline fun <T> AppResult<T>.onSuccess(action: (T) -> Unit): AppResult<T> = apply {
    if (this is AppResult.Success) action(data)
}

inline fun <T> AppResult<T>.onFailure(action: (DataError) -> Unit): AppResult<T> = apply {
    if (this is AppResult.Failure) action(error)
}
