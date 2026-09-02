package com.instantmechanic.ui.request

import androidx.lifecycle.SavedStateHandle
import com.instantmechanic.core.result.AppResult
import com.instantmechanic.core.result.DataError
import com.instantmechanic.domain.model.ServiceType
import com.instantmechanic.domain.validation.FormField
import com.instantmechanic.domain.validation.ServiceRequestValidator
import com.instantmechanic.domain.validation.ValidationReason
import com.instantmechanic.ui.navigation.Routes
import com.instantmechanic.util.FakeMechanicRepository
import com.instantmechanic.util.MainDispatcherRule
import com.instantmechanic.util.TestData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Covers the two decisions this ViewModel actually makes, since the rules themselves are tested in
 * [com.instantmechanic.domain.validation.ServiceRequestValidatorTest]:
 *
 * - **When** errors become visible. They stay hidden until the first submit attempt, then update
 *   live per field. Showing "name is required" against an empty form the user hasn't filled in yet
 *   is the classic bad form UX, and `showErrors` is what prevents it.
 * - **How** input is normalised on the way in, so a pasted phone number or a lowercase plate is
 *   accepted rather than rejected on a technicality.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RequestServiceViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeMechanicRepository()
    private val validator = ServiceRequestValidator()

    // ------------------------------------------------------------ loading the garage

    @Test
    fun `loads the garage named by the navigation argument`() = runTest {
        val viewModel = createViewModel(mechanicId = "m-042")

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoadingMechanic)
        assertEquals("Sharma Auto Works", state.mechanicName)
        assertNull(state.loadError)
    }

    @Test
    fun `offers only the services that garage performs`() = runTest {
        repository.detailResponder = {
            AppResult.Success(
                TestData.detail(services = listOf(ServiceType.TOWING, ServiceType.BRAKE_REPAIR)),
            )
        }
        val viewModel = createViewModel()

        advanceUntilIdle()

        // A towing-only operator must not be offered for a wheel alignment.
        assertEquals(
            listOf(ServiceType.TOWING, ServiceType.BRAKE_REPAIR),
            viewModel.uiState.value.availableServices,
        )
    }

    @Test
    fun `prefills the service when the garage does exactly one thing`() = runTest {
        repository.detailResponder = {
            AppResult.Success(TestData.detail(services = listOf(ServiceType.TOWING)))
        }
        val viewModel = createViewModel()

        advanceUntilIdle()

        assertEquals(ServiceType.TOWING, viewModel.uiState.value.input.serviceType)
    }

    @Test
    fun `does not guess the service when the garage offers several`() = runTest {
        val viewModel = createViewModel()

        advanceUntilIdle()

        // Two services on offer, so picking one for the user would be a guess about their problem.
        assertNull(viewModel.uiState.value.input.serviceType)
    }

    @Test
    fun `a failed load is reported as a load error and blocks submission`() = runTest {
        repository.failDetail(DataError.NETWORK)
        val viewModel = createViewModel()

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(DataError.NETWORK, state.loadError)
        // No point letting someone fill in a form we cannot address to a garage.
        assertFalse(state.canSubmit)
    }

    @Test
    fun `retrying the load can recover`() = runTest {
        repository.failDetail()
        val viewModel = createViewModel()
        advanceUntilIdle()
        assertEquals(DataError.NOT_FOUND, viewModel.uiState.value.loadError)

        repository.detailResponder = { AppResult.Success(TestData.detail()) }
        viewModel.retryLoad()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.loadError)
        assertEquals("Sharma Auto Works", viewModel.uiState.value.mechanicName)
    }

    // ------------------------------------------------------------ when errors appear

    @Test
    fun `errors are hidden before the first submit attempt`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onNameChange("A") // too short, so an error exists internally

        val state = viewModel.uiState.value
        assertTrue(state.errors.containsKey(FormField.NAME))
        // ...but the screen must not shout at someone who is still typing their first field.
        assertFalse(state.showErrors)
        assertNull(state.errorFor(FormField.NAME))
    }

    @Test
    fun `a failed submit reveals every problem at once`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.submit()

        val state = viewModel.uiState.value
        assertTrue(state.showErrors)
        assertEquals(FormField.entries.size, state.errors.size)
        assertEquals(ValidationReason.NAME_REQUIRED, state.errorFor(FormField.NAME))
        assertEquals(ValidationReason.PHONE_REQUIRED, state.errorFor(FormField.PHONE))
        assertTrue(repository.submitted.isEmpty())
    }

    @Test
    fun `after the first attempt each field clears as it is corrected`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.submit()

        viewModel.onNameChange("Vansh Sharma")

        val state = viewModel.uiState.value
        assertNull(state.errorFor(FormField.NAME))
        // The other fields keep their errors — correcting one does not hide the rest.
        assertEquals(ValidationReason.PHONE_REQUIRED, state.errorFor(FormField.PHONE))
    }

    @Test
    fun `a field that is corrected and then broken again errors live`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.submit()
        viewModel.onPhoneChange("9876543210")
        assertNull(viewModel.uiState.value.errorFor(FormField.PHONE))

        viewModel.onPhoneChange("987654321")

        assertEquals(ValidationReason.PHONE_INVALID, viewModel.uiState.value.errorFor(FormField.PHONE))
    }

    // ------------------------------------------------------------ input normalisation

    @Test
    fun `phone input drops characters a phone number cannot contain`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        // What pasting from a contacts app typically yields.
        viewModel.onPhoneChange("+91 (98765) 43210")

        assertEquals("+919876543210", viewModel.uiState.value.input.phone)
        assertNull(viewModel.uiState.value.errors[FormField.PHONE])
    }

    @Test
    fun `phone input is capped at plus91 and ten digits`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onPhoneChange("+919876543210999")

        assertEquals(13, viewModel.uiState.value.input.phone.length)
    }

    @Test
    fun `plate input is uppercased and stripped of separators`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onVehicleNumberChange("mh-12 ab 1234")

        assertEquals("MH12AB1234", viewModel.uiState.value.input.vehicleNumber)
        assertNull(viewModel.uiState.value.errors[FormField.VEHICLE_NUMBER])
    }

    @Test
    fun `description is capped at the validator's maximum`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onDescriptionChange("x".repeat(ServiceRequestValidator.MAX_DESCRIPTION_LENGTH + 50))

        // Capping at the edge means the "too long" error is unreachable through the UI, which is
        // better UX than letting someone type 200 characters they will be told to delete.
        assertEquals(
            ServiceRequestValidator.MAX_DESCRIPTION_LENGTH,
            viewModel.uiState.value.input.description.length,
        )
        assertNull(viewModel.uiState.value.errors[FormField.DESCRIPTION])
    }

    // ------------------------------------------------------------ submitting

    @Test
    fun `a valid form submits and produces a receipt`() = runTest {
        val viewModel = createViewModel(mechanicId = "m-007")
        advanceUntilIdle()
        fillValidForm(viewModel)

        viewModel.submit()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isSubmitting)
        assertTrue(state.isConfirmed)
        assertEquals(TestData.receipt, state.receipt)
        assertNull(state.submitError)

        val sent = repository.submitted.single()
        assertEquals("m-007", sent.mechanicId)
        assertEquals("Vansh Sharma", sent.customerName)
        assertEquals("MH12AB1234", sent.vehicleNumber)
        assertEquals(ServiceType.BRAKE_REPAIR, sent.serviceType)
    }

    @Test
    fun `the submitted request is trimmed`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        fillValidForm(viewModel)
        viewModel.onNameChange("  Vansh Sharma  ")
        viewModel.onDescriptionChange("  Front brakes squeal badly.  ")

        viewModel.submit()
        advanceUntilIdle()

        val sent = repository.submitted.single()
        assertEquals("Vansh Sharma", sent.customerName)
        assertEquals("Front brakes squeal badly.", sent.description)
    }

    @Test
    fun `the button reports progress while the request is in flight`() = runTest {
        repository.submitDelayMillis = 100
        val viewModel = createViewModel()
        advanceUntilIdle()
        fillValidForm(viewModel)

        viewModel.submit()
        advanceTimeBy(1)

        val state = viewModel.uiState.value
        assertTrue(state.isSubmitting)
        // canSubmit gates the button, so a second tap cannot double-book.
        assertFalse(state.canSubmit)
        assertFalse(state.isConfirmed)
    }

    @Test
    fun `a second tap while submitting is ignored`() = runTest {
        repository.submitDelayMillis = 100
        val viewModel = createViewModel()
        advanceUntilIdle()
        fillValidForm(viewModel)

        viewModel.submit()
        advanceTimeBy(1)
        viewModel.submit()
        advanceUntilIdle()

        assertEquals(1, repository.submitted.size)
    }

    @Test
    fun `a failed submit keeps the form and surfaces a submit error`() = runTest {
        repository.failSubmit(DataError.SERVER)
        val viewModel = createViewModel()
        advanceUntilIdle()
        fillValidForm(viewModel)

        viewModel.submit()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(DataError.SERVER, state.submitError)
        assertFalse(state.isSubmitting)
        assertFalse(state.isConfirmed)
        // The whole point of separating submitError from loadError: the typed-in form survives, so
        // the failure can be a snackbar rather than a screen replacement.
        assertEquals("Vansh Sharma", state.input.customerName)
        assertEquals("MH12AB1234", state.input.vehicleNumber)
        // And it can be retried without retyping anything.
        assertTrue(state.canSubmit)
    }

    @Test
    fun `dismissing the submit error clears it so the snackbar does not reappear`() = runTest {
        repository.failSubmit()
        val viewModel = createViewModel()
        advanceUntilIdle()
        fillValidForm(viewModel)
        viewModel.submit()
        advanceUntilIdle()

        viewModel.dismissSubmitError()

        assertNull(viewModel.uiState.value.submitError)
    }

    @Test
    fun `editing a field clears a stale submit error`() = runTest {
        repository.failSubmit()
        val viewModel = createViewModel()
        advanceUntilIdle()
        fillValidForm(viewModel)
        viewModel.submit()
        advanceUntilIdle()

        viewModel.onDescriptionChange("Front brakes squeal badly when stopping.")

        // The message referred to an attempt the user has since moved on from.
        assertNull(viewModel.uiState.value.submitError)
    }

    @Test
    fun `submitting is blocked until the garage has loaded`() = runTest {
        repository.detailDelayMillis = 100
        val viewModel = createViewModel()
        advanceTimeBy(1)

        viewModel.submit()

        // Nothing sent, and no errors flashed at a form the user has not been shown yet.
        assertTrue(repository.submitted.isEmpty())
        assertFalse(viewModel.uiState.value.showErrors)
    }

    // ------------------------------------------------------------ helpers

    private fun fillValidForm(viewModel: RequestServiceViewModel) {
        viewModel.onNameChange("Vansh Sharma")
        viewModel.onPhoneChange("9876543210")
        viewModel.onVehicleNumberChange("MH12AB1234")
        viewModel.onServiceChange(ServiceType.BRAKE_REPAIR)
        viewModel.onDescriptionChange("Front brakes squeal badly when stopping.")
    }

    private fun createViewModel(mechanicId: String = "m-001") = RequestServiceViewModel(
        repository = repository,
        validator = validator,
        savedStateHandle = SavedStateHandle(mapOf(Routes.ARG_MECHANIC_ID to mechanicId)),
    )
}
