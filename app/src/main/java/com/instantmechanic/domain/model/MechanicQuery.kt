package com.instantmechanic.domain.model

/**
 * The set of filters the Home screen sends to the backend.
 *
 * Search, filtering, sorting and paging are all expressed as *query parameters* and applied
 * server-side rather than by filtering an in-memory list, so the app behaves the same way
 * against a real API as it does against the bundled mock backend.
 */
data class MechanicQuery(
    val search: String = "",
    val service: ServiceType? = null,
    val openNowOnly: Boolean = false,
    val sort: MechanicSort = MechanicSort.RATING,
    val page: Int = 1,
    val pageSize: Int = 20,
) {
    /** True when the user has narrowed the list in any way. */
    val hasActiveFilters: Boolean
        get() = search.isNotBlank() || service != null || openNowOnly
}

/** Sort orders the `sort` query parameter accepts. */
enum class MechanicSort(val apiValue: String, val label: String) {
    RATING("rating", "Top rated"),
    DISTANCE("distance", "Nearest"),
}

/** One page of results plus the metadata the backend reports alongside it. */
data class MechanicPage(
    val items: List<Mechanic>,
    val page: Int,
    val totalPages: Int,
    val totalItems: Int,
)
