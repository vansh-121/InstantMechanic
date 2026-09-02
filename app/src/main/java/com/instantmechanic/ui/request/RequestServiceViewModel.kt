package com.instantmechanic.ui.request

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.instantmechanic.core.result.AppResult
import com.instantmechanic.domain.model.ServiceRequest
import com.instantmechanic.domain.model.ServiceType
import com.instantmechanic.domain.repository.MechanicRepository
import com.instantmechanic.domain.validation.FormField
import com.instantmechanic.domain.validation.ServiceRequestInput
import com.instantmechanic.domain.validation.ServiceRequestValidator
import com.instantmechanic.domain.validation.ValidationError
import com.instantmechanic.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Drives the service-request form.
 *
 * Two responsibilities worth separating in an interview: it loads the garage (for the header and to
 * offer only the services that garage actually performs), and it owns the validate-then-submit
 * cycle. Validation lives in [ServiceRequestValidator] — a pure domain class — so this ViewModel
 * only decides *when* to run it, never *what* the rules are.
 */
@HiltViewModel
class RequestServiceViewModel @Inject constructor(
    private val repository: MechanicRepository,
    private val validator: ServiceRequestValidator,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val mechanicId: String = checkNotNull(savedStateHandle[Routes.ARG_MECHANIC_ID]) {
        "RequestServiceViewModel requires a '${Routes.ARG_MECHANIC_ID}' navigation argument"
    }

    private val _uiState = MutableStateFlow(RequestUiState())
    val uiState: StateFlow<RequestUiState> = _uiState.asStateFlow()

    init {
        loadMechanic()
    }

    fun retryLoad() = loadMechanic()

    private fun loadMechanic() {
        _uiState.update { it.copy(isLoadingMechanic = true, loadError = null) }
        viewModelScope.launch {
            when (val result = repository.getMechanic(mechanicId)) {
                is AppResult.Success -> _uiState.update {
                    it.copy(
                        mechanicName = result.data.name,
                        availableServices = result.data.services,
                        isLoadingMechanic = false,
                        // Prefill when the garage does exactly one thing; otherwise make them choose.
                        input = it.input.copy(
                            serviceType = it.input.serviceType
                                ?: result.data.services.singleOrNull(),
                        ),
                    )
                }

                is AppResult.Failure -> _uiState.update {
                    it.copy(isLoadingMechanic = false, loadError = result.error)
                }
            }
        }
    }

    fun onNameChange(value: String) = updateField(FormField.NAME, validator.validateName(value)) {
        it.copy(customerName = value)
    }

    fun onPhoneChange(value: String) {
        // Keep only what a phone number can contain, so paste-from-contacts doesn't fail validation.
        val filtered = value.filter { it.isDigit() || it == '+' }.take(MAX_PHONE_LENGTH)
        updateField(FormField.PHONE, validator.validatePhone(filtered)) {
            it.copy(phone = filtered)
        }
    }

    fun onVehicleNumberChange(value: String) {
        val normalised = value.filter { it.isLetterOrDigit() }.uppercase().take(MAX_PLATE_LENGTH)
        updateField(FormField.VEHICLE_NUMBER, validator.validateVehicleNumber(normalised)) {
            it.copy(vehicleNumber = normalised)
        }
    }

    fun onServiceChange(value: ServiceType) =
        updateField(FormField.SERVICE, validator.validateService(value)) {
            it.copy(serviceType = value)
        }

    fun onDescriptionChange(value: String) {
        val capped = value.take(ServiceRequestValidator.MAX_DESCRIPTION_LENGTH)
        updateField(FormField.DESCRIPTION, validator.validateDescription(capped)) {
            it.copy(description = capped)
        }
    }

    /**
     * Applies an edit and re-runs *only that field's* rule.
     *
     * Re-validating the whole form on every keystroke would light up errors for fields the user
     * hasn't reached yet, so each field is corrected independently once it has been touched.
     */
    private fun updateField(
        field: FormField,
        error: ValidationError?,
        edit: (ServiceRequestInput) -> ServiceRequestInput,
    ) {
        _uiState.update { state ->
            state.copy(
                input = edit(state.input),
                errors = if (error == null) state.errors - field else state.errors + (field to error.reason),
                submitError = null,
            )
        }
    }

    fun submit() {
        val state = _uiState.value
        if (!state.canSubmit) return

        val problems = validator.validate(state.input).associate { it.field to it.reason }
        if (problems.isNotEmpty()) {
            // First failed attempt is what unlocks error display — see RequestUiState.showErrors.
            _uiState.update { it.copy(errors = problems, showErrors = true, submitError = null) }
            return
        }

        val serviceType = state.input.serviceType ?: return
        _uiState.update { it.copy(isSubmitting = true, submitError = null, showErrors = true) }

        viewModelScope.launch {
            val request = ServiceRequest(
                mechanicId = mechanicId,
                customerName = state.input.customerName.trim(),
                phone = state.input.phone.trim(),
                vehicleNumber = state.input.vehicleNumber.trim().uppercase(),
                serviceType = serviceType,
                description = state.input.description.trim(),
            )
            when (val result = repository.submitServiceRequest(request)) {
                is AppResult.Success -> _uiState.update {
                    it.copy(isSubmitting = false, receipt = result.data)
                }

                is AppResult.Failure -> _uiState.update {
                    it.copy(isSubmitting = false, submitError = result.error)
                }
            }
        }
    }

    fun dismissSubmitError() = _uiState.update { it.copy(submitError = null) }

    private companion object {
        /** "+91" plus ten digits. */
        const val MAX_PHONE_LENGTH = 13

        /** The longest valid Indian plate, e.g. "MH12AB1234". */
        const val MAX_PLATE_LENGTH = 11
    }
}
