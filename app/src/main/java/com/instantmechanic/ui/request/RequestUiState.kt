package com.instantmechanic.ui.request

import com.instantmechanic.core.result.DataError
import com.instantmechanic.domain.model.ServiceRequestReceipt
import com.instantmechanic.domain.model.ServiceType
import com.instantmechanic.domain.validation.FormField
import com.instantmechanic.domain.validation.ServiceRequestInput
import com.instantmechanic.domain.validation.ValidationReason

/**
 * Everything the Request Service screen draws, in one immutable snapshot.
 *
 * [showErrors] is the reason this is a single state rather than a bag of booleans: errors must stay
 * hidden until the first submit attempt, and become live per-field afterwards. Both behaviours fall
 * out of one flag plus the [errors] map.
 */
data class RequestUiState(
    val mechanicName: String = "",
    val availableServices: List<ServiceType> = emptyList(),
    val isLoadingMechanic: Boolean = true,
    val loadError: DataError? = null,
    val input: ServiceRequestInput = ServiceRequestInput(),
    val errors: Map<FormField, ValidationReason> = emptyMap(),
    val showErrors: Boolean = false,
    val isSubmitting: Boolean = false,
    val submitError: DataError? = null,
    val receipt: ServiceRequestReceipt? = null,
) {
    /** An error is only *displayed* once the user has tried to submit at least once. */
    fun errorFor(field: FormField): ValidationReason? = errors[field].takeIf { showErrors }

    /** The submit button stays enabled while invalid — pressing it is how you learn what's wrong. */
    val canSubmit: Boolean get() = !isSubmitting && !isLoadingMechanic && loadError == null

    val isConfirmed: Boolean get() = receipt != null
}
