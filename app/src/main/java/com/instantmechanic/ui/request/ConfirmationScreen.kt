package com.instantmechanic.ui.request

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.instantmechanic.R
import com.instantmechanic.domain.model.ServiceRequestReceipt
import com.instantmechanic.ui.theme.InstantMechanicTheme
import com.instantmechanic.ui.theme.LocalStatusColors

/**
 * Post-submit confirmation.
 *
 * Shows the two things the backend told us and the user can't guess — the request id and the ETA —
 * because a confirmation that only says "success" gives the user nothing to quote on the phone.
 *
 * There is deliberately no back arrow: the request has already been placed, so the only sensible
 * exit is "Done", which clears the whole booking flow off the back stack.
 */
@Composable
fun ConfirmationScreen(
    receipt: ServiceRequestReceipt,
    mechanicName: String,
    onDone: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            SuccessMark()

            Text(
                text = stringResource(R.string.confirmation_headline),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 24.dp),
            )
            Text(
                text = stringResource(
                    R.string.confirmation_body,
                    mechanicName.ifBlank { receipt.mechanicName },
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )

            ReceiptCard(receipt)

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = onDone,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text(
                    text = stringResource(R.string.confirmation_done),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}

@Composable
private fun SuccessMark() {
    val status = LocalStatusColors.current
    Box(
        modifier = Modifier
            .size(76.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(shape = CircleShape, color = status.open, modifier = Modifier.fillMaxSize()) {}
        Icon(
            imageVector = Icons.Outlined.Check,
            contentDescription = null,
            tint = status.onOpen,
            modifier = Modifier.size(40.dp),
        )
    }
}

@Composable
private fun ReceiptCard(receipt: ServiceRequestReceipt) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 28.dp),
    ) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            ReceiptRow(
                label = stringResource(R.string.confirmation_request_id),
                value = receipt.requestId,
            )
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            ReceiptRow(
                label = stringResource(R.string.confirmation_eta),
                value = receipt.etaDisplay,
                icon = true,
            )
        }
    }
}

@Composable
private fun ReceiptRow(label: String, value: String, icon: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon) {
                Icon(
                    imageVector = Icons.Outlined.Schedule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(16.dp)
                        .padding(end = 0.dp),
                )
                Spacer(modifier = Modifier.size(6.dp))
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ConfirmationScreenPreview() {
    InstantMechanicTheme {
        ConfirmationScreen(
            receipt = ServiceRequestReceipt(
                requestId = "IM-7KQ4TB",
                status = "confirmed",
                etaMinutes = 75,
                mechanicName = "Sharma Auto Works",
            ),
            mechanicName = "Sharma Auto Works",
            onDone = {},
        )
    }
}
