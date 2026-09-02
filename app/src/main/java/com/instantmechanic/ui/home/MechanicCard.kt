package com.instantmechanic.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.instantmechanic.core.ui.MechanicImage
import com.instantmechanic.core.ui.OpenClosedChip
import com.instantmechanic.core.ui.RatingRow
import com.instantmechanic.core.ui.ServiceChipRow
import com.instantmechanic.core.ui.VerifiedBadge
import com.instantmechanic.domain.model.Mechanic
import com.instantmechanic.domain.model.ServiceType
import com.instantmechanic.ui.theme.InstantMechanicTheme

/** One garage in the Home list. */
@Composable
fun MechanicCard(
    mechanic: Mechanic,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp, pressedElevation = 5.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
    ) {
        Row(modifier = Modifier.padding(12.dp)) {
            Box(
                modifier = Modifier
                    .size(104.dp)
                    .clip(RoundedCornerShape(16.dp)),
            ) {
                MechanicImage(
                    imageUrl = mechanic.imageUrl,
                    name = mechanic.name,
                    initialsTextSize = MaterialTheme.typography.titleLarge.fontSize,
                    modifier = Modifier.matchParentSize(),
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(5.dp),
                ) {
                    OpenClosedChip(isOpen = mechanic.isOpenNow)
                }
            }

            Column(
                modifier = Modifier
                    .padding(start = 12.dp)
                    .weight(1f),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = mechanic.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (mechanic.isVerified) {
                        VerifiedBadge(modifier = Modifier.padding(start = 6.dp))
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp),
                ) {
                    RatingRow(rating = mechanic.rating, reviewCount = mechanic.reviewCount)
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text = "${mechanic.distanceDisplay} · ${mechanic.areaDisplay}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }

                if (mechanic.priceRange.isNotBlank()) {
                    Text(
                        text = mechanic.priceRange,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 3.dp),
                    )
                }

                ServiceChipRow(
                    services = mechanic.services,
                    maxVisible = MAX_SERVICE_CHIPS,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}

/** Two chips plus an overflow pill keeps every card the same height. */
private const val MAX_SERVICE_CHIPS = 2

@Preview(showBackground = true)
@Composable
private fun MechanicCardPreview() {
    InstantMechanicTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            MechanicCard(
                mechanic = Mechanic(
                    id = "m-001",
                    name = "Sharma Auto Works",
                    imageUrl = "",
                    rating = 4.8,
                    reviewCount = 1247,
                    distanceKm = 1.2,
                    locality = "Kothrud",
                    city = "Pune",
                    services = listOf(
                        ServiceType.GENERAL_SERVICE,
                        ServiceType.ENGINE_REPAIR,
                        ServiceType.OIL_CHANGE,
                        ServiceType.BRAKE_REPAIR,
                    ),
                    isOpenNow = true,
                    isVerified = true,
                ),
                onClick = {},
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MechanicCardClosedPreview() {
    InstantMechanicTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MechanicCard(
                mechanic = Mechanic(
                    id = "m-012",
                    name = "Warje Roadside Rescue",
                    imageUrl = "",
                    rating = 3.9,
                    reviewCount = 143,
                    distanceKm = 7.7,
                    locality = "Warje",
                    city = "Pune",
                    services = listOf(ServiceType.TOWING, ServiceType.BATTERY_JUMPSTART),
                    isOpenNow = false,
                    isVerified = false,
                ),
                onClick = {},
            )
            Spacer(modifier = Modifier.height(0.dp))
        }
    }
}
