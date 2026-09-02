package com.instantmechanic.ui.home

import com.instantmechanic.core.result.AppResult
import com.instantmechanic.core.result.DataError
import com.instantmechanic.data.remote.mock.MockApiController
import com.instantmechanic.domain.model.MechanicPage
import com.instantmechanic.domain.model.MechanicSort
import com.instantmechanic.domain.model.ServiceType
import com.instantmechanic.util.FakeMechanicRepository
import com.instantmechanic.util.MainDispatcherRule
import com.instantmechanic.util.TestData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
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
 * The Home ViewModel is the app's most stateful class, so these tests target its three genuinely
 * tricky behaviours rather than its getters:
 *
 * 1. **Debouncing** — a burst of keystrokes must cost one request, not one per character.
 * 2. **Staleness** — a slow answer to an old query must never overwrite a newer one.
 * 3. **State reduction** — loading with results on screen is a *refresh*, not a blank spinner.
 *
 * A `StandardTestDispatcher` (installed by [MainDispatcherRule]) means nothing runs until the test
 * advances virtual time, which is what makes assertions about intermediate states possible at all.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeMechanicRepository()
    private val controller = MockApiController()

    // ------------------------------------------------------------ first load

    @Test
    fun `starts in the loading state before anything resolves`() = runTest {
        val viewModel = createViewModel()

        // Nothing has been allowed to run yet, so this is the state the very first frame draws.
        assertEquals(HomeContent.Loading, viewModel.uiState.value.content)
        assertFalse(viewModel.uiState.value.isRefreshing)
    }

    @Test
    fun `loads the list on init without any user interaction`() = runTest {
        val viewModel = createViewModel()

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(HomeContent.Success(TestData.mechanics), state.content)
        assertEquals(2, state.totalItems)
        assertFalse(state.isRefreshing)
        assertEquals(1, repository.queries.size)
    }

    @Test
    fun `a successful but empty response is Empty rather than Success with no items`() = runTest {
        repository.returnList(emptyList())
        val viewModel = createViewModel()

        advanceUntilIdle()

        // The distinction drives a completely different screen: "no matches, clear the filters?"
        // instead of a blank area under a heading.
        assertEquals(HomeContent.Empty, viewModel.uiState.value.content)
    }

    @Test
    fun `a failed response becomes an Error carrying the typed cause`() = runTest {
        repository.failList(DataError.SERVER)
        val viewModel = createViewModel()

        advanceUntilIdle()

        assertEquals(HomeContent.Error(DataError.SERVER), viewModel.uiState.value.content)
        assertFalse(viewModel.uiState.value.isRefreshing)
    }

    // ------------------------------------------------------------ search debouncing

    @Test
    fun `the text field updates immediately even though the query is debounced`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onSearchTextChange("bra")

        // The field is state-hoisted in the ViewModel, so typing must never feel laggy...
        assertEquals("bra", viewModel.uiState.value.searchText)
        // ...but no request has gone out yet.
        assertEquals(1, repository.queries.size)
    }

    @Test
    fun `a burst of typing issues a single query`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        val afterFirstLoad = repository.queries.size

        val typed = "brake"
        typed.indices.forEach { index ->
            viewModel.onSearchTextChange(typed.take(index + 1))
            advanceTimeBy(100) // each keystroke lands well inside the 350 ms window
        }
        advanceUntilIdle()

        assertEquals(1, repository.queries.size - afterFirstLoad)
        assertEquals("brake", repository.queries.last().search)
    }

    @Test
    fun `pausing longer than the debounce window issues a second query`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onSearchTextChange("brake")
        advanceUntilIdle()
        viewModel.onSearchTextChange("brake pads")
        advanceUntilIdle()

        assertEquals(listOf("", "brake", "brake pads"), repository.queries.map { it.search })
    }

    @Test
    fun `a stale response cannot overwrite a newer one`() = runTest {
        // The answer to "bra" is slow and different from the answer to "brake". Without
        // flatMapLatest cancelling the superseded call, whichever resolved last would win — and
        // the user would be staring at results for a prefix they have already finished typing.
        val stale = listOf(TestData.mechanic(id = "stale", name = "Stale Result"))
        val fresh = listOf(TestData.mechanic(id = "fresh", name = "Fresh Result"))
        repository.listResponder = { query ->
            val slow = query.search == "bra"
            delay(if (slow) 1_000 else 10)
            AppResult.Success(
                MechanicPage(
                    items = if (slow) stale else fresh,
                    page = 1,
                    totalPages = 1,
                    totalItems = 1,
                ),
            )
        }
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onSearchTextChange("bra")
        advanceTimeBy(400) // the debounce elapses and the slow request is now in flight
        viewModel.onSearchTextChange("brake")
        advanceUntilIdle()

        assertEquals(HomeContent.Success(fresh), viewModel.uiState.value.content)
    }

    @Test
    fun `clearing the search resets the text and re-queries`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onSearchTextChange("brake")
        advanceUntilIdle()

        viewModel.onClearSearch()
        advanceUntilIdle()

        assertEquals("", viewModel.uiState.value.searchText)
        assertEquals("", repository.queries.last().search)
    }

    // ------------------------------------------------------------ filters and sorting

    @Test
    fun `selecting a service sends it as a query parameter`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onServiceSelected(ServiceType.TOWING)
        advanceUntilIdle()

        // The point of the assertion: the filter reached the *repository*. Nothing in the
        // ViewModel filters the list it already holds.
        assertEquals(ServiceType.TOWING, repository.queries.last().service)
        assertEquals(ServiceType.TOWING, viewModel.uiState.value.selectedService)
    }

    @Test
    fun `filter changes are not debounced`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        val afterFirstLoad = repository.queries.size

        viewModel.onOpenNowToggled(true)
        advanceTimeBy(50) // far short of the 350 ms typing debounce

        // Tapping a chip is a deliberate act; it should not wait out a delay meant for typing.
        assertEquals(1, repository.queries.size - afterFirstLoad)
        assertTrue(repository.queries.last().openNowOnly)
    }

    @Test
    fun `changing the sort order re-queries the backend`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onSortSelected(MechanicSort.DISTANCE)
        advanceUntilIdle()

        assertEquals(MechanicSort.DISTANCE, repository.queries.last().sort)
        assertEquals(MechanicSort.DISTANCE, viewModel.uiState.value.sort)
    }

    @Test
    fun `clearing filters resets search service and openNow but keeps the sort`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onSearchTextChange("brake")
        viewModel.onServiceSelected(ServiceType.BRAKE_REPAIR)
        viewModel.onOpenNowToggled(true)
        viewModel.onSortSelected(MechanicSort.DISTANCE)
        advanceUntilIdle()

        viewModel.onClearFilters()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("", state.searchText)
        assertNull(state.selectedService)
        assertFalse(state.openNowOnly)
        assertFalse(state.hasActiveFilters)
        // Sort is a presentation preference, not a filter — clearing filters should not undo it.
        assertEquals(MechanicSort.DISTANCE, state.sort)

        val lastQuery = repository.queries.last()
        assertEquals("", lastQuery.search)
        assertNull(lastQuery.service)
        assertFalse(lastQuery.openNowOnly)
        assertEquals(MechanicSort.DISTANCE, lastQuery.sort)
    }

    // ------------------------------------------------------------ refresh vs spinner

    @Test
    fun `re-querying with results on screen shows the inline bar not the spinner`() = runTest {
        repository.listDelayMillis = 100
        val viewModel = createViewModel()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.content is HomeContent.Success)

        viewModel.onOpenNowToggled(true)
        advanceTimeBy(1) // the Loading step has run; the response has not arrived

        val state = viewModel.uiState.value
        assertTrue("results should stay on screen", state.content is HomeContent.Success)
        assertTrue("the inline bar should be showing", state.isRefreshing)
    }

    @Test
    fun `re-querying after an error falls back to the full screen loading state`() = runTest {
        repository.listDelayMillis = 100
        repository.failList()
        val viewModel = createViewModel()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.content is HomeContent.Error)

        viewModel.onOpenNowToggled(true)
        advanceTimeBy(1)

        // There is nothing worth keeping on screen, so a spinner is the honest thing to show.
        assertEquals(HomeContent.Loading, viewModel.uiState.value.content)
        assertFalse(viewModel.uiState.value.isRefreshing)
    }

    // ------------------------------------------------------------ retry

    @Test
    fun `retry re-runs the current query and can recover`() = runTest {
        repository.failListUntil(attempt = 2)
        val viewModel = createViewModel()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.content is HomeContent.Error)

        viewModel.retry()
        advanceUntilIdle()

        assertEquals(HomeContent.Success(TestData.mechanics), viewModel.uiState.value.content)
    }

    @Test
    fun `retry preserves the filters that were active when the call failed`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onSearchTextChange("brake")
        viewModel.onServiceSelected(ServiceType.BRAKE_REPAIR)
        advanceUntilIdle()
        repository.failList()

        viewModel.retry()
        advanceUntilIdle()

        val retried = repository.queries.last()
        assertEquals("brake", retried.search)
        assertEquals(ServiceType.BRAKE_REPAIR, retried.service)
    }

    // ------------------------------------------------------------ debug failure switch

    @Test
    fun `the simulated failure switch is mirrored into the ui state`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.simulateFailure)

        viewModel.onToggleSimulatedFailure()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.simulateFailure)
        assertTrue(controller.simulateFailure.value)
    }

    @Test
    fun `toggling the simulated failure switch immediately re-runs the query`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        val afterFirstLoad = repository.queries.size

        viewModel.onToggleSimulatedFailure()
        advanceUntilIdle()

        // Otherwise the user flips the switch and sees no change until they type something.
        assertEquals(1, repository.queries.size - afterFirstLoad)
    }

    // ------------------------------------------------------------ subtitle

    @Test
    fun `the subtitle counts the results on screen`() = runTest {
        val viewModel = createViewModel()

        advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.subtitleCount)
    }

    @Test
    fun `the subtitle is hidden while an error is on screen`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        assertEquals(2, viewModel.uiState.value.subtitleCount)
        repository.failList()

        viewModel.retry()
        advanceUntilIdle()

        // totalItems is deliberately kept for the retry, but "2 garages near you" over an error
        // message would be counting results the user cannot see.
        assertEquals(2, viewModel.uiState.value.totalItems)
        assertNull(viewModel.uiState.value.subtitleCount)
    }

    @Test
    fun `the subtitle is hidden when nothing matched`() = runTest {
        repository.returnList(emptyList())
        val viewModel = createViewModel()

        advanceUntilIdle()

        assertNull(viewModel.uiState.value.subtitleCount)
    }

    private fun createViewModel() = HomeViewModel(repository, controller)
}
