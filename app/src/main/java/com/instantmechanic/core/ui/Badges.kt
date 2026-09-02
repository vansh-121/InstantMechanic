package com.instantmechanic.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.instantmechanic.R
import com.instantmechanic.domain.model.ServiceType
import com.instantmechanic.ui.theme.LocalStatusColors
import java.util.Locale

/**
 * Star + rating + review count.
 *
 * The whole row is collapsed into one semantics node so a screen reader announces
 * "Rated 4.8 out of 5 from 1,247 reviews" instead of spelling out an icon, a number and a
 * bracketed count separately.
 */
@Composable
fun RatingRow(
    rating: Double,
    reviewCount: Int,
    modifier: Modifier = Modifier,
) {
    val ratingText = "%.1f".format(Locale.US, rating)
    val description = pluralStringResource(
        R.plurals.rating_content_description,
        reviewCount,
        ratingText,
        reviewCount,
    )
    Row(
        modifier = modifier.clearAndSetSemantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Star,
            contentDescription = null,
            tint = LocalStatusColors.current.rating,
            modifier = Modifier.size(15.dp),
        )
        Text(
            text = ratingText,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 4.dp),
        )
        Text(
            text = stringResource(R.string.reviews_count, formatCount(reviewCount)),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 3.dp),
        )
    }
}

/** Green "Open now" / neutral "Closed" pill. */
@Composable
fun OpenClosedChip(
    isOpen: Boolean,
    modifier: Modifier = Modifier,
) {
    val status = LocalStatusColors.current
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (isOpen) status.open else status.closed,
        modifier = modifier,
    ) {
        Text(
            text = stringResource(if (isOpen) R.string.status_open else R.string.status_closed),
            style = MaterialTheme.typography.labelSmall,
            color = if (isOpen) status.onOpen else status.onClosed,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
        )
    }
}

/** Small "Verified" mark for garages the platform has vetted. */
@Composable
fun VerifiedBadge(modifier: Modifier = Modifier) {
    val label = stringResource(R.string.verified)
    Row(
        modifier = modifier.clearAndSetSemantics { contentDescription = label },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Verified,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 3.dp),
        )
    }
}

/**
 * Wrapping row of service labels.
 *
 * [maxVisible] caps how many are drawn and appends a "+N more" pill, so a garage offering eight
 * services can't push a list card to twice the height of its neighbours.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ServiceChipRow(
    services: List<ServiceType>,
    modifier: Modifier = Modifier,
    maxVisible: Int = Int.MAX_VALUE,
) {
    val visible = services.take(maxVisible)
    val overflow = services.size - visible.size
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        visible.forEach { ServiceTag(it.label) }
        if (overflow > 0) {
            ServiceTag(stringResource(R.string.more_services, overflow), muted = true)
        }
    }
}

@Composable
private fun ServiceTag(label: String, muted: Boolean = false) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (muted) {
            MaterialTheme.colorScheme.surfaceContainerHigh
        } else {
            MaterialTheme.colorScheme.secondaryContainer
        },
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (muted) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSecondaryContainer
            },
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
        )
    }
}

/** 1247 -> "1,247". Keeps review counts readable without pulling in a formatter dependency. */
private fun formatCount(count: Int): String = "%,d".format(Locale.US, count)
