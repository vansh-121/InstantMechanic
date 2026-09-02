package com.instantmechanic.ui.detail

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CurrencyRupee
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.instantmechanic.R
import com.instantmechanic.core.ui.ErrorState
import com.instantmechanic.core.ui.LoadingState
import com.instantmechanic.core.ui.MechanicImage
import com.instantmechanic.core.ui.OpenClosedChip
import com.instantmechanic.core.ui.RatingRow
import com.instantmechanic.core.ui.ServiceChipRow
import com.instantmechanic.core.ui.VerifiedBadge
import com.instantmechanic.domain.model.DayHours
import com.instantmechanic.domain.model.MechanicDetail
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun DetailRoute(
    onBack: () -> Unit,
    onRequestService: (String) -> Unit,
    viewModel: DetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    DetailScreen(
        state = state,
        onBack = onBack,
        onRetry = viewModel::retry,
        onRequestService = onRequestService,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    state: DetailUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onRequestService: (String) -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.detail_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.detail_back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            // Pinned so the primary action is reachable without scrolling to the end.
            if (state is DetailUiState.Success) {
                RequestServiceBar(
                    onClick = { onRequestService(state.mechanic.id) },
                )
            }
        },
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (state) {
                DetailUiState.Loading -> LoadingState()
                is DetailUiState.Error -> ErrorState(error = state.error, onRetry = onRetry)
                is DetailUiState.Success -> DetailContent(
                    mechanic = state.mechanic,
                    snackbarHostState = snackbarHostState,
                )
            }
        }
    }
}

@Composable
private fun DetailContent(
    mechanic: MechanicDetail,
    snackbarHostState: SnackbarHostState,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        HeaderImage(mechanic)

        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 16.dp),
            ) {
                Text(
                    text = mechanic.name,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (mechanic.isVerified) {
                    VerifiedBadge(modifier = Modifier.padding(start = 8.dp))
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 8.dp),
            ) {
                RatingRow(rating = mechanic.rating, reviewCount = mechanic.reviewCount)
                Spacer(modifier = Modifier.width(12.dp))
                OpenClosedChip(isOpen = mechanic.isOpenNow)
            }

            InfoRow(
                icon = Icons.Outlined.LocationOn,
                primary = mechanic.fullAddress,
                secondary = mechanic.distanceDisplay,
            )

            if (mechanic.priceRange.isNotBlank()) {
                InfoRow(
                    icon = Icons.Outlined.CurrencyRupee,
                    primary = stringResource(R.string.detail_price_range),
                    secondary = mechanic.priceRange,
                )
            }

            CallRow(phone = mechanic.phone, snackbarHostState = snackbarHostState)

            if (mechanic.about.isNotBlank()) {
                SectionHeader(stringResource(R.string.detail_about))
                Text(
                    text = mechanic.about,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            SectionHeader(stringResource(R.string.detail_services_offered))
            ServiceChipRow(services = mechanic.services, modifier = Modifier.fillMaxWidth())

            SectionHeader(stringResource(R.string.detail_working_hours))
            WorkingHoursTable(mechanic.weeklyHours)

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun HeaderImage(mechanic: MechanicDetail) {
    Box {
        MechanicImage(
            imageUrl = mechanic.imageUrl,
            name = mechanic.name,
            initialsTextSize = MaterialTheme.typography.headlineMedium.fontSize,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
        )
        // Scrim so the image never fights the content that follows it.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.18f)),
                    ),
                ),
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(top = 24.dp, bottom = 10.dp),
    )
}

@Composable
private fun InfoRow(
    icon: ImageVector,
    primary: String,
    secondary: String?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
        Column(modifier = Modifier.padding(start = 10.dp)) {
            Text(
                text = primary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (!secondary.isNullOrBlank()) {
                Text(
                    text = secondary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * Tappable phone row.
 *
 * Uses `ACTION_DIAL` rather than `ACTION_CALL`: dialling requires no runtime permission and it
 * leaves the user in control of whether the call is actually placed. The `ActivityNotFoundException`
 * branch matters on tablets and emulators with no dialer installed.
 */
@Composable
private fun CallRow(phone: String, snackbarHostState: SnackbarHostState) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val noDialerMessage = stringResource(R.string.detail_no_dialer)
    val callDescription = stringResource(R.string.detail_call_content_description, phone)

    val dial: () -> Unit = {
        val intent = Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", phone, null))
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            scope.launch { snackbarHostState.showSnackbar(noDialerMessage) }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = dial)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.Phone,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
        Column(
            modifier = Modifier
                .padding(start = 10.dp)
                .weight(1f),
        ) {
            Text(
                text = phone,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(R.string.detail_contact),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        FilledTonalButton(onClick = dial) {
            Icon(
                imageVector = Icons.Outlined.Phone,
                contentDescription = callDescription,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = stringResource(R.string.detail_call),
                modifier = Modifier.padding(start = 6.dp),
            )
        }
    }
}

/** Weekly hours with today's row called out, so "am I too late?" is answerable at a glance. */
@Composable
private fun WorkingHoursTable(hours: List<DayHours>) {
    val today = remember { LocalDate.now().dayOfWeek }
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            hours.forEachIndexed { index, day ->
                val isToday = day.day == today
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (isToday) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                            } else {
                                Color.Transparent
                            },
                        )
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = day.dayLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.width(48.dp),
                    )
                    if (isToday) {
                        Text(
                            text = stringResource(R.string.detail_today),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp),
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = day.displayRange,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isToday) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (day.isClosed) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    )
                }
                if (index != hours.lastIndex) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        modifier = Modifier.padding(horizontal = 14.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun RequestServiceBar(onClick: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp,
    ) {
        Box(modifier = Modifier.padding(PaddingValues(horizontal = 16.dp, vertical = 12.dp))) {
            Button(
                onClick = onClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(),
            ) {
                Text(
                    text = stringResource(R.string.detail_request_service),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}
