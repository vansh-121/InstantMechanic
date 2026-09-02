package com.instantmechanic.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.instantmechanic.R
import com.instantmechanic.core.result.DataError
import com.instantmechanic.core.result.isRetryable

/** Full-bleed spinner, used only for a screen's very first load. */
@Composable
fun LoadingState(modifier: Modifier = Modifier) {
    val label = stringResource(R.string.loading)
    Box(
        modifier = modifier
            .fillMaxSize()
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

/**
 * Error state with a Retry affordance.
 *
 * Retry is only offered when the error is actually retryable — repeatedly re-issuing a request
 * that returned 404 just teaches the user the button does nothing.
 */
@Composable
fun ErrorState(
    error: DataError,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MessageBlock(
        icon = Icons.Outlined.CloudOff,
        title = error.title(),
        body = error.body(),
        modifier = modifier,
        action = {
            if (error.isRetryable) {
                Button(onClick = onRetry) {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = stringResource(R.string.error_retry),
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
        },
    )
}

/** Shown when a search or filter combination legitimately matches nothing. */
@Composable
fun EmptyState(
    onClearFilters: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    MessageBlock(
        icon = Icons.Outlined.SearchOff,
        title = stringResource(R.string.home_empty_title),
        body = stringResource(R.string.home_empty_body),
        modifier = modifier,
        action = {
            if (onClearFilters != null) {
                TextButton(onClick = onClearFilters) {
                    Text(stringResource(R.string.home_clear_filters))
                }
            }
        },
    )
}

/** Shared layout for the icon + headline + body + action pattern. */
@Composable
private fun MessageBlock(
    icon: ImageVector,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    action: @Composable () -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.size(72.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(32.dp),
                )
            }
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 20.dp),
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        )
        Box(modifier = Modifier.padding(top = 20.dp)) { action() }
    }
}
