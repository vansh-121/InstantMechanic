package com.instantmechanic.core.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import com.instantmechanic.R
import com.instantmechanic.core.result.DataError

/** The user-facing wording for a [DataError]. */
data class ErrorMessage(
    @StringRes val titleRes: Int,
    @StringRes val bodyRes: Int,
)

/**
 * Maps a typed data-layer error to copy the user can act on.
 *
 * Centralised so every screen phrases the same failure the same way, and so the data layer never
 * has to know about `R.string`.
 */
fun DataError.toMessage(): ErrorMessage = when (this) {
    DataError.NETWORK -> ErrorMessage(R.string.error_title_network, R.string.error_body_network)
    DataError.TIMEOUT -> ErrorMessage(R.string.error_title_timeout, R.string.error_body_timeout)
    DataError.SERVER -> ErrorMessage(R.string.error_title_server, R.string.error_body_server)
    DataError.NOT_FOUND -> ErrorMessage(R.string.error_title_not_found, R.string.error_body_not_found)
    DataError.PARSE -> ErrorMessage(R.string.error_title_parse, R.string.error_body_parse)
    DataError.UNKNOWN -> ErrorMessage(R.string.error_title_unknown, R.string.error_body_unknown)
}

@Composable
fun DataError.title(): String = stringResource(remember(this) { toMessage() }.titleRes)

@Composable
fun DataError.body(): String = stringResource(remember(this) { toMessage() }.bodyRes)
