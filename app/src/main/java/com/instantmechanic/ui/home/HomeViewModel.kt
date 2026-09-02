package com.instantmechanic.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.instantmechanic.core.result.AppResult
import com.instantmechanic.data.remote.mock.MockApiController
import com.instantmechanic.domain.model.MechanicPage
import com.instantmechanic.domain.model.MechanicQuery
import com.instantmechanic.domain.model.MechanicSort
import com.instantmechanic.domain.model.ServiceType
import com.instantmechanic.domain.repository.MechanicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/**
 * Drives the Home screen.
 *
 * The whole screen is one reactive pipeline: user intent goes into [searchText] / [selection], and
 * a query is issued whenever either changes. Text input is debounced so typing "brake" costs one
 * request rather than five, and [flatMapLatest] cancels a request whose answer is already stale —
 * so a slow response for "bra" can never overwrite the results for "brake".
 *
 * Filtering and sorting are *not* done here. They are query parameters sent to the backend, which
 * is what keeps this ViewModel honest about how the app would behave against a real API.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: MechanicRepository,
    private val mockApiController: MockApiController,
) : ViewModel() {

    /** Filter state that should take effect immediately, without debouncing. */
    private data class Selection(
        val service: ServiceType? = null,
        val openNowOnly: Boolean = false,
        val sort: MechanicSort = MechanicSort.RATING,
    )

    /** One step of the load lifecycle, folded into [HomeUiState] by [reduce]. */
    private sealed interface Partial {
        data object Loading : Partial
        data class Loaded(val result: AppResult<MechanicPage>) : Partial
    }

    private val searchText = MutableStateFlow("")
    private val selection = MutableStateFlow(Selection())

    /** Incremented by [retry] to re-run the current query without changing it. */
    private val retryTicker = MutableStateFlow(0)

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        observeQuery()
        observeDevSwitch()
    }

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    private fun observeQuery() {
        combine(
            searchText.debounce(SEARCH_DEBOUNCE_MS).distinctUntilChanged(),
            selection,
            retryTicker,
        ) { text, current, _ ->
            MechanicQuery(
                search = text,
                service = current.service,
                openNowOnly = current.openNowOnly,
                sort = current.sort,
            )
        }
            .flatMapLatest(::load)
            .onEach { partial -> _uiState.update { it.reduce(partial) } }
            .launchIn(viewModelScope)
    }

    private fun observeDevSwitch() {
        mockApiController.simulateFailure
            .onEach { enabled -> _uiState.update { it.copy(simulateFailure = enabled) } }
            .launchIn(viewModelScope)
    }

    private fun load(query: MechanicQuery): Flow<Partial> = flow {
        emit(Partial.Loading)
        emit(Partial.Loaded(repository.getMechanics(query)))
    }

    private fun HomeUiState.reduce(partial: Partial): HomeUiState = when (partial) {
        Partial.Loading ->
            // Keep whatever is on screen and show the inline bar; only fall back to the
            // full-screen spinner when there is genuinely nothing to keep.
            if (content is HomeContent.Success) {
                copy(isRefreshing = true)
            } else {
                copy(content = HomeContent.Loading, isRefreshing = false)
            }

        is Partial.Loaded -> when (val result = partial.result) {
            is AppResult.Success -> copy(
                content = if (result.data.items.isEmpty()) {
                    HomeContent.Empty
                } else {
                    HomeContent.Success(result.data.items)
                },
                totalItems = result.data.totalItems,
                isRefreshing = false,
            )

            is AppResult.Failure -> copy(
                content = HomeContent.Error(result.error),
                isRefreshing = false,
            )
        }
    }

    // ---------------------------------------------------------------- intents

    fun onSearchTextChange(value: String) {
        // The text field is state-hoisted here, so the UI value updates on every keystroke even
        // though the network query behind it is debounced.
        _uiState.update { it.copy(searchText = value) }
        searchText.value = value
    }

    fun onClearSearch() = onSearchTextChange("")

    fun onServiceSelected(service: ServiceType?) {
        _uiState.update { it.copy(selectedService = service) }
        selection.update { it.copy(service = service) }
    }

    fun onOpenNowToggled(enabled: Boolean) {
        _uiState.update { it.copy(openNowOnly = enabled) }
        selection.update { it.copy(openNowOnly = enabled) }
    }

    fun onSortSelected(sort: MechanicSort) {
        _uiState.update { it.copy(sort = sort) }
        selection.update { it.copy(sort = sort) }
    }

    fun onClearFilters() {
        _uiState.update {
            it.copy(searchText = "", selectedService = null, openNowOnly = false)
        }
        searchText.value = ""
        selection.update { it.copy(service = null, openNowOnly = false) }
    }

    fun retry() {
        retryTicker.update { it + 1 }
    }

    /** Debug builds only — see [MockApiController]. */
    fun onToggleSimulatedFailure() {
        mockApiController.toggleSimulateFailure()
        retry()
    }

    companion object {
        /**
         * Long enough to coalesce a burst of typing, short enough that the list still feels
         * responsive to a deliberate pause.
         */
        const val SEARCH_DEBOUNCE_MS = 350L
    }
}
