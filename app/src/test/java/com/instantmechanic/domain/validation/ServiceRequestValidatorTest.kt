package com.instantmechanic.domain.validation

import com.instantmechanic.domain.model.ServiceType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The validation rules are the app's only real business logic that a user can break on purpose,
 * so they get the most exhaustive coverage — including the awkward inputs (leading zero, `+91`,
 * spaced phone numbers, lowercase plates, whitespace-only fields) that a happy-path demo misses.
 */
class ServiceRequestValidatorTest {

    private val validator = ServiceRequestValidator()

    // ------------------------------------------------------------------ name

    @Test
    fun `name is required`() {
        assertEquals(ValidationReason.NAME_REQUIRED, validator.validateName("")?.reason)
    }

    @Test
    fun `whitespace-only name is treated as empty`() {
        assertEquals(ValidationReason.NAME_REQUIRED, validator.validateName("   ")?.reason)
    }

    @Test
    fun `single character name is too short`() {
        assertEquals(ValidationReason.NAME_TOO_SHORT, validator.validateName("A")?.reason)
    }

    @Test
    fun `name rejects digits and symbols`() {
        assertEquals(
            ValidationReason.NAME_INVALID_CHARACTERS,
            validator.validateName("Vansh99")?.reason,
        )
    }

    @Test
    fun `name accepts spaces and common punctuation`() {
        assertNull(validator.validateName("Vansh Sharma"))
        assertNull(validator.validateName("D'Souza"))
        assertNull(validator.validateName("Dr. A P J Abdul Kalam"))
        assertNull(validator.validateName("Mary-Anne"))
    }

    @Test
    fun `name accepts non-latin scripts`() {
        // \p{L} rather than A-Za-z: an Indian user typing their name in Devanagari is not an error.
        // Note the combining marks — U+0902 in वंश and U+093E in शर्मा are category M, not L, so
        // this fails against a letters-only class even though it looks like plain text.
        assertNull(validator.validateName("वंश शर्मा"))
        assertNull(validator.validateName("সৌরভ"))
        assertNull(validator.validateName("José Álvarez"))
    }

    @Test
    fun `name error is reported against the name field`() {
        assertEquals(FormField.NAME, validator.validateName("")?.field)
    }

    // ------------------------------------------------------------------ phone

    @Test
    fun `phone is required`() {
        assertEquals(ValidationReason.PHONE_REQUIRED, validator.validatePhone("")?.reason)
    }

    @Test
    fun `plain ten digit mobile is accepted`() {
        assertNull(validator.validatePhone("9876543210"))
    }

    @Test
    fun `phone accepts the plus91 and leading zero prefixes`() {
        assertNull(validator.validatePhone("+919876543210"))
        assertNull(validator.validatePhone("09876543210"))
    }

    @Test
    fun `phone tolerates spaces and dashes`() {
        assertNull(validator.validatePhone("+91 98765-43210"))
        assertNull(validator.validatePhone("98765 43210"))
    }

    @Test
    fun `phone rejects a landline-style leading digit`() {
        // Indian mobile numbers start 6-9; 2xxxxxxxxx is a landline and cannot be texted an OTP.
        assertEquals(ValidationReason.PHONE_INVALID, validator.validatePhone("2876543210")?.reason)
    }

    @Test
    fun `phone rejects wrong length`() {
        assertEquals(ValidationReason.PHONE_INVALID, validator.validatePhone("987654321")?.reason)
        assertEquals(ValidationReason.PHONE_INVALID, validator.validatePhone("98765432101")?.reason)
    }

    @Test
    fun `phone rejects letters`() {
        assertEquals(ValidationReason.PHONE_INVALID, validator.validatePhone("98765abcde")?.reason)
    }

    // ------------------------------------------------------------------ vehicle number

    @Test
    fun `vehicle number is required`() {
        assertEquals(
            ValidationReason.VEHICLE_NUMBER_REQUIRED,
            validator.validateVehicleNumber("")?.reason,
        )
    }

    @Test
    fun `vehicle number accepts the common plate shapes`() {
        assertNull(validator.validateVehicleNumber("MH12AB1234"))
        assertNull(validator.validateVehicleNumber("DL8CAF5023"))
        assertNull(validator.validateVehicleNumber("KA01A1234"))
        assertNull(validator.validateVehicleNumber("MH121234"))
    }

    @Test
    fun `vehicle number is case and separator insensitive`() {
        assertNull(validator.validateVehicleNumber("mh12ab1234"))
        assertNull(validator.validateVehicleNumber("MH 12 AB 1234"))
        assertNull(validator.validateVehicleNumber("MH-12-AB-1234"))
    }

    @Test
    fun `vehicle number rejects malformed plates`() {
        // Missing the state code.
        assertEquals(
            ValidationReason.VEHICLE_NUMBER_INVALID,
            validator.validateVehicleNumber("12AB1234")?.reason,
        )
        // Only three trailing digits.
        assertEquals(
            ValidationReason.VEHICLE_NUMBER_INVALID,
            validator.validateVehicleNumber("MH12AB123")?.reason,
        )
        // Four-letter series.
        assertEquals(
            ValidationReason.VEHICLE_NUMBER_INVALID,
            validator.validateVehicleNumber("MH12ABCD1234")?.reason,
        )
    }

    // ------------------------------------------------------------------ service

    @Test
    fun `service is required`() {
        assertEquals(ValidationReason.SERVICE_REQUIRED, validator.validateService(null)?.reason)
    }

    @Test
    fun `any service type is accepted`() {
        ServiceType.entries.forEach { assertNull(validator.validateService(it)) }
    }

    // ------------------------------------------------------------------ description

    @Test
    fun `description is required`() {
        assertEquals(
            ValidationReason.DESCRIPTION_REQUIRED,
            validator.validateDescription("")?.reason,
        )
    }

    @Test
    fun `description below the minimum is too short`() {
        assertEquals(
            ValidationReason.DESCRIPTION_TOO_SHORT,
            validator.validateDescription("Bad tyre")?.reason,
        )
    }

    @Test
    fun `description exactly at the minimum is accepted`() {
        val exact = "x".repeat(ServiceRequestValidator.MIN_DESCRIPTION_LENGTH)
        assertNull(validator.validateDescription(exact))
    }

    @Test
    fun `description is measured after trimming`() {
        // Padding a short description with spaces must not sneak past the minimum.
        val padded = "        " + "x".repeat(ServiceRequestValidator.MIN_DESCRIPTION_LENGTH - 1)
        assertEquals(
            ValidationReason.DESCRIPTION_TOO_SHORT,
            validator.validateDescription(padded)?.reason,
        )
    }

    @Test
    fun `description above the maximum is too long`() {
        val tooLong = "x".repeat(ServiceRequestValidator.MAX_DESCRIPTION_LENGTH + 1)
        assertEquals(
            ValidationReason.DESCRIPTION_TOO_LONG,
            validator.validateDescription(tooLong)?.reason,
        )
    }

    // ------------------------------------------------------------------ whole form

    @Test
    fun `a fully valid form produces no errors`() {
        assertTrue(validator.validate(validInput()).isEmpty())
    }

    @Test
    fun `an empty form reports one error per field`() {
        val errors = validator.validate(ServiceRequestInput())

        assertEquals(FormField.entries.size, errors.size)
        assertEquals(FormField.entries.toSet(), errors.map { it.field }.toSet())
    }

    @Test
    fun `errors come back in field order`() {
        // Stable ordering means the UI can scroll to the first problem deterministically.
        val errors = validator.validate(ServiceRequestInput())

        assertEquals(
            listOf(
                FormField.NAME,
                FormField.PHONE,
                FormField.VEHICLE_NUMBER,
                FormField.SERVICE,
                FormField.DESCRIPTION,
            ),
            errors.map { it.field },
        )
    }

    @Test
    fun `only the offending field is reported`() {
        val errors = validator.validate(validInput().copy(phone = "12345"))

        assertEquals(1, errors.size)
        assertEquals(FormField.PHONE, errors.first().field)
    }

    @Test
    fun `per-field checks agree with the whole-form check`() {
        // The screen validates one field per keystroke and the whole form on submit; if these two
        // paths ever disagreed, a form could pass field-by-field and still be rejected on submit.
        val input = ServiceRequestInput(
            customerName = "A",
            phone = "12345",
            vehicleNumber = "nope",
            serviceType = null,
            description = "short",
        )

        val fromWholeForm = validator.validate(input)
        val fromFields = listOfNotNull(
            validator.validateName(input.customerName),
            validator.validatePhone(input.phone),
            validator.validateVehicleNumber(input.vehicleNumber),
            validator.validateService(input.serviceType),
            validator.validateDescription(input.description),
        )

        assertEquals(fromFields, fromWholeForm)
    }

    private fun validInput() = ServiceRequestInput(
        customerName = "Vansh Sharma",
        phone = "9876543210",
        vehicleNumber = "MH12AB1234",
        serviceType = ServiceType.BRAKE_REPAIR,
        description = "Front brakes squeal badly when stopping from speed.",
    )
}
