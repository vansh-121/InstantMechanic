package com.instantmechanic.ui.home

import com.instantmechanic.core.result.DataError
import com.instantmechanic.domain.model.Mechanic
import com.instantmechanic.domain.model.MechanicSort
import com.instantmechanic.domain.model.ServiceType

/**
 * Everything the Home screen needs to draw itself, in one immutable value.
 *
 * The list's own condition is modelled as a sealed [HomeContent] rather than as a set of
 * independent booleans, which makes the impossible states (loading *and* errored, empty *and*
 * populated) unrepresentable instead of merely unlikely.
 */
data class HomeUiState(
    val searchText: String = "",
    val selectedService: ServiceType? = null,
    val openNowOnly: Boolean = false,
    val sort: MechanicSort = MechanicSort.RATING,
    val content: HomeContent = HomeContent.Loading,
    /**
     * True while re-querying with results already on screen. Drives a slim inline progress bar
     * instead of a full-screen spinner, so changing a filter doesn't make the list flash.
     */
    val isRefreshing: Boolean = false,
    val totalItems: Int = 0,
    val currentPage: Int = 1,
    val totalPages: Int = 1,
    val isLoadingMore: Boolean = false,
    /** Debug-only mirror of [com.instantmechanic.data.remote.mock.MockApiController]. */
    val simulateFailure: Boolean = false,
) {
    val hasActiveFilters: Boolean
        get() = searchText.isNotBlank() || selectedService != null || openNowOnly

    val canLoadMore: Boolean
        get() = currentPage < totalPages && !isLoadingMore && !isRefreshing && content is HomeContent.Success

    /**
     * The "N garages near you" subtitle describes the list, so it is only true while a list is
     * actually on screen. [totalItems] survives an error on purpose — the retry needs the previous
     * query intact — but announcing a count above an error message describes nothing the user
     * can see.
     */
    val subtitleCount: Int?
        get() = totalItems.takeIf { it > 0 && content is HomeContent.Success }
}

sealed interface HomeContent {
    /** First load for this screen — nothing to show yet. */
    data object Loading : HomeContent

    data class Success(val mechanics: List<Mechanic>) : HomeContent

    /** The query succeeded but matched nothing. Distinct from [Loading] and from [Error]. */
    data object Empty : HomeContent

    data class Error(val error: DataError) : HomeContent
}
