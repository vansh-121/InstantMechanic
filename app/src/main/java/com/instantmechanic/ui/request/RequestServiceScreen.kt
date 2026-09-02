package com.instantmechanic.ui.request

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.instantmechanic.R
import com.instantmechanic.core.ui.ErrorState
import com.instantmechanic.core.ui.LoadingState
import com.instantmechanic.core.ui.body
import com.instantmechanic.core.ui.message
import com.instantmechanic.domain.model.ServiceType
import com.instantmechanic.domain.validation.FormField
import com.instantmechanic.domain.validation.ServiceRequestValidator

@Composable
fun RequestServiceRoute(
    onBack: () -> Unit,
    onDone: () -> Unit,
    viewModel: RequestServiceViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    if (state.isConfirmed) {
        ConfirmationScreen(
            receipt = requireNotNull(state.receipt),
            mechanicName = state.mechanicName,
            onDone = onDone,
        )
    } else {
        RequestServiceScreen(
            state = state,
            onBack = onBack,
            onRetryLoad = viewModel::retryLoad,
            onNameChange = viewModel::onNameChange,
            onPhoneChange = viewModel::onPhoneChange,
            onVehicleNumberChange = viewModel::onVehicleNumberChange,
            onServiceChange = viewModel::onServiceChange,
            onDescriptionChange = viewModel::onDescriptionChange,
            onSubmit = viewModel::submit,
            onSubmitErrorShown = viewModel::dismissSubmitError,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestServiceScreen(
    state: RequestUiState,
    onBack: () -> Unit,
    onRetryLoad: () -> Unit,
    onNameChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onVehicleNumberChange: (String) -> Unit,
    onServiceChange: (ServiceType) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onSubmitErrorShown: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }

    // A failed submit is transient: the form is still filled in, so a snackbar is the right weight
    // of feedback. A failed *load* replaces the screen, because there is no form to keep.
    val submitErrorMessage = state.submitError?.body()
    LaunchedEffect(state.submitError) {
        if (submitErrorMessage != null) {
            snackbarHostState.showSnackbar(submitErrorMessage)
            onSubmitErrorShown()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.request_title))
                        if (state.mechanicName.isNotBlank()) {
                            Text(
                                text = stringResource(R.string.request_at_garage, state.mechanicName),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
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
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when {
                state.isLoadingMechanic -> LoadingState()
                state.loadError != null -> ErrorState(error = state.loadError, onRetry = onRetryLoad)
                else -> RequestForm(
                    state = state,
                    onNameChange = onNameChange,
                    onPhoneChange = onPhoneChange,
                    onVehicleNumberChange = onVehicleNumberChange,
                    onServiceChange = onServiceChange,
                    onDescriptionChange = onDescriptionChange,
                    onSubmit = onSubmit,
                )
            }
        }
    }
}

@Composable
private fun RequestForm(
    state: RequestUiState,
    onNameChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onVehicleNumberChange: (String) -> Unit,
    onServiceChange: (ServiceType) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Garage Header Card
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(44.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.Build,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
                Column(modifier = Modifier.padding(start = 12.dp)) {
                    Text(
                        text = state.mechanicName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Booking verified on-demand service",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        LabeledTextField(
            value = state.input.customerName,
            onValueChange = onNameChange,
            label = stringResource(R.string.request_field_name),
            errorText = state.errorFor(FormField.NAME)?.message(),
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next,
            ),
        )

        LabeledTextField(
            value = state.input.phone,
            onValueChange = onPhoneChange,
            label = stringResource(R.string.request_field_phone),
            errorText = state.errorFor(FormField.PHONE)?.message(),
            prefix = { Text(stringResource(R.string.request_field_phone_prefix)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Phone,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Phone,
                imeAction = ImeAction.Next,
            ),
        )

        LabeledTextField(
            value = state.input.vehicleNumber,
            onValueChange = onVehicleNumberChange,
            label = stringResource(R.string.request_field_vehicle),
            placeholder = stringResource(R.string.request_field_vehicle_hint),
            errorText = state.errorFor(FormField.VEHICLE_NUMBER)?.message(),
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.DirectionsCar,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Characters,
                imeAction = ImeAction.Next,
            ),
        )

        ServicePicker(
            selected = state.input.serviceType,
            options = state.availableServices,
            errorText = state.errorFor(FormField.SERVICE)?.message(),
            onSelect = onServiceChange,
        )

        LabeledTextField(
            value = state.input.description,
            onValueChange = onDescriptionChange,
            label = stringResource(R.string.request_field_description),
            errorText = state.errorFor(FormField.DESCRIPTION)?.message(),
            supportingText = stringResource(
                R.string.request_field_description_counter,
                state.input.description.length,
                ServiceRequestValidator.MAX_DESCRIPTION_LENGTH,
            ),
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.EditNote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            },
            singleLine = false,
            minLines = 4,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Default,
            ),
        )

        Spacer(modifier = Modifier.height(4.dp))

        SubmitButton(isSubmitting = state.isSubmitting, enabled = state.canSubmit, onClick = onSubmit)

        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * One labelled text field with inline error support.
 *
 * Material 3 draws the error state itself once `isError` is set; passing the message through
 * `supportingText` keeps the reason next to the field instead of in a toast the user has to
 * remember.
 */
@Composable
private fun LabeledTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    errorText: String?,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    supportingText: String? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    prefix: @Composable (() -> Unit)? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        leadingIcon = leadingIcon,
        prefix = prefix,
        isError = errorText != null,
        supportingText = when {
            errorText != null -> {
                { Text(errorText) }
            }

            supportingText != null -> {
                { Text(supportingText, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.End) }
            }

            else -> null
        },
        singleLine = singleLine,
        minLines = minLines,
        keyboardOptions = keyboardOptions,
        shape = RoundedCornerShape(16.dp),
    )
}

/**
 * Service dropdown.
 *
 * Options come from the garage being booked, not from the full [ServiceType] enum — you cannot ask
 * a towing-only operator for a wheel alignment.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ServicePicker(
    selected: ServiceType?,
    options: List<ServiceType>,
    errorText: String?,
    onSelect: (ServiceType) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        OutlinedTextField(
            value = selected?.label ?: "",
            onValueChange = {},
            readOnly = true,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.request_field_service)) },
            placeholder = { Text(stringResource(R.string.request_service_picker)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Build,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            },
            isError = errorText != null,
            supportingText = errorText?.let { { Text(it) } },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Outlined.ArrowDropDown,
                    contentDescription = stringResource(R.string.request_service_picker),
                )
            },
            shape = RoundedCornerShape(16.dp),
        )
        // A transparent overlay over the read-only field: tapping anywhere on it opens the menu
        // without the field ever taking focus and raising the keyboard.
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickableNoIndication { expanded = true },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { service ->
                DropdownMenuItem(
                    text = { Text(service.label) },
                    onClick = {
                        onSelect(service)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun SubmitButton(isSubmitting: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
    ) {
        // Progress replaces the label in place, so the button never changes size mid-submit.
        if (isSubmitting) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = stringResource(R.string.request_submitting),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 10.dp),
                )
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.request_submit),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(18.dp),
                )
            }
        }
    }
}

/** Click handling with no ripple, for overlays that must not look interactive themselves. */
@Composable
private fun Modifier.clickableNoIndication(onClick: () -> Unit): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    return clickable(
        interactionSource = interactionSource,
        indication = null,
        onClick = onClick,
    )
}
