package com.instantmechanic.domain.validation

import com.instantmechanic.domain.model.ServiceType

/** Which form field a [ValidationError] belongs to. */
enum class FormField { NAME, PHONE, VEHICLE_NUMBER, SERVICE, DESCRIPTION }

/**
 * Why a field was rejected.
 *
 * Typed reasons rather than prose: the domain layer decides *what* is wrong, the UI layer owns
 * *how it is worded* (see `stringResource` mapping in the request screen). That keeps the rules
 * localisable, and lets the unit tests assert on a stable value instead of on a sentence that
 * someone will later reword.
 */
enum class ValidationReason {
    NAME_REQUIRED,
    NAME_TOO_SHORT,
    NAME_INVALID_CHARACTERS,
    PHONE_REQUIRED,
    PHONE_INVALID,
    VEHICLE_NUMBER_REQUIRED,
    VEHICLE_NUMBER_INVALID,
    SERVICE_REQUIRED,
    DESCRIPTION_REQUIRED,
    DESCRIPTION_TOO_SHORT,
    DESCRIPTION_TOO_LONG,
}

/** A single problem with the submitted form. */
data class ValidationError(val field: FormField, val reason: ValidationReason)

/** Raw, unvalidated form input straight from the text fields. */
data class ServiceRequestInput(
    val customerName: String = "",
    val phone: String = "",
    val vehicleNumber: String = "",
    val serviceType: ServiceType? = null,
    val description: String = "",
)

/**
 * Validates the service-request form.
 *
 * Deliberately pure Kotlin with no Android or framework types, so it can be exercised
 * exhaustively in fast JVM unit tests. The same functions back both per-field live feedback and
 * the final submit check, so the two can never drift apart.
 */
class ServiceRequestValidator {

    /** Every problem with [input], in field order. Empty means the form is submittable. */
    fun validate(input: ServiceRequestInput): List<ValidationError> =
        listOfNotNull(
            validateName(input.customerName),
            validatePhone(input.phone),
            validateVehicleNumber(input.vehicleNumber),
            validateService(input.serviceType),
            validateDescription(input.description),
        )

    fun validateName(value: String): ValidationError? {
        val trimmed = value.trim()
        return when {
            trimmed.isEmpty() -> ValidationReason.NAME_REQUIRED
            trimmed.length < MIN_NAME_LENGTH -> ValidationReason.NAME_TOO_SHORT
            !trimmed.matches(NAME_REGEX) -> ValidationReason.NAME_INVALID_CHARACTERS
            else -> null
        }?.let { ValidationError(FormField.NAME, it) }
    }

    fun validatePhone(value: String): ValidationError? {
        val compact = value.filterNot(Char::isWhitespace).replace("-", "")
        return when {
            compact.isEmpty() -> ValidationReason.PHONE_REQUIRED
            !compact.matches(PHONE_REGEX) -> ValidationReason.PHONE_INVALID
            else -> null
        }?.let { ValidationError(FormField.PHONE, it) }
    }

    fun validateVehicleNumber(value: String): ValidationError? {
        val compact = value.filterNot(Char::isWhitespace).replace("-", "").uppercase()
        return when {
            compact.isEmpty() -> ValidationReason.VEHICLE_NUMBER_REQUIRED
            !compact.matches(VEHICLE_REGEX) -> ValidationReason.VEHICLE_NUMBER_INVALID
            else -> null
        }?.let { ValidationError(FormField.VEHICLE_NUMBER, it) }
    }

    fun validateService(value: ServiceType?): ValidationError? =
        if (value == null) ValidationError(FormField.SERVICE, ValidationReason.SERVICE_REQUIRED) else null

    fun validateDescription(value: String): ValidationError? {
        val trimmed = value.trim()
        return when {
            trimmed.isEmpty() -> ValidationReason.DESCRIPTION_REQUIRED
            trimmed.length < MIN_DESCRIPTION_LENGTH -> ValidationReason.DESCRIPTION_TOO_SHORT
            trimmed.length > MAX_DESCRIPTION_LENGTH -> ValidationReason.DESCRIPTION_TOO_LONG
            else -> null
        }?.let { ValidationError(FormField.DESCRIPTION, it) }
    }

    companion object {
        const val MIN_NAME_LENGTH = 2
        const val MIN_DESCRIPTION_LENGTH = 10
        const val MAX_DESCRIPTION_LENGTH = 500

        /**
         * Letters (any script), spaces and the punctuation that legitimately appears in names.
         *
         * `\p{M}` is not decoration: Indic scripts write vowels as combining marks, so "वंश शर्मा"
         * is letters *interleaved with* marks (U+0902 anusvara, U+093E vowel sign aa). A `\p{L}`-only
         * class rejects almost every Devanagari name — which, for an app aimed at Indian users, is
         * the kind of bug that only shows up once a real customer tries to book.
         */
        private val NAME_REGEX = Regex("^[\\p{L}\\p{M} .'-]+$")

        /** 10-digit Indian mobile starting 6-9, with an optional +91 / 0 prefix. */
        private val PHONE_REGEX = Regex("^(\\+91|0)?[6-9]\\d{9}$")

        /**
         * Indian plates: two-letter state, 1-2 digit RTO code, 0-3 letter series, 4 digits
         * (MH12AB1234, DL8CAF5023, KA01A1234).
         */
        private val VEHICLE_REGEX = Regex("^[A-Z]{2}\\d{1,2}[A-Z]{0,3}\\d{4}$")
    }
}
