package com.instantmechanic.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.instantmechanic.R
import com.instantmechanic.domain.validation.ServiceRequestValidator
import com.instantmechanic.domain.validation.ValidationReason

/**
 * Wording for a [ValidationReason].
 *
 * The domain layer decided *what* is wrong; this is the only place that decides *how it is said*.
 * The length limits are read back off [ServiceRequestValidator] rather than retyped into the
 * strings, so the rule and the message it produces can never disagree.
 */
@Composable
fun ValidationReason.message(): String = when (this) {
    ValidationReason.NAME_REQUIRED ->
        stringResource(R.string.validation_name_required)

    ValidationReason.NAME_TOO_SHORT ->
        stringResource(R.string.validation_name_too_short, ServiceRequestValidator.MIN_NAME_LENGTH)

    ValidationReason.NAME_INVALID_CHARACTERS ->
        stringResource(R.string.validation_name_invalid)

    ValidationReason.PHONE_REQUIRED ->
        stringResource(R.string.validation_phone_required)

    ValidationReason.PHONE_INVALID ->
        stringResource(R.string.validation_phone_invalid)

    ValidationReason.VEHICLE_NUMBER_REQUIRED ->
        stringResource(R.string.validation_vehicle_required)

    ValidationReason.VEHICLE_NUMBER_INVALID ->
        stringResource(R.string.validation_vehicle_invalid)

    ValidationReason.SERVICE_REQUIRED ->
        stringResource(R.string.validation_service_required)

    ValidationReason.DESCRIPTION_REQUIRED ->
        stringResource(R.string.validation_description_required)

    ValidationReason.DESCRIPTION_TOO_SHORT -> stringResource(
        R.string.validation_description_too_short,
        ServiceRequestValidator.MIN_DESCRIPTION_LENGTH,
    )

    ValidationReason.DESCRIPTION_TOO_LONG -> stringResource(
        R.string.validation_description_too_long,
        ServiceRequestValidator.MAX_DESCRIPTION_LENGTH,
    )
}
