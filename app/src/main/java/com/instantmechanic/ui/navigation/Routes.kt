package com.instantmechanic.ui.navigation

/**
 * Navigation graph addresses.
 *
 * Argument names are declared once as constants and reused by the route templates, the
 * `navArgument` declarations and the `SavedStateHandle` lookups in the ViewModels — so a typo
 * becomes a compile error in one place rather than a null at runtime in another.
 */
object Routes {
    const val ARG_MECHANIC_ID = "mechanicId"

    const val HOME = "home"

    private const val DETAIL_BASE = "mechanic"
    const val DETAIL = "$DETAIL_BASE/{$ARG_MECHANIC_ID}"
    const val REQUEST = "$DETAIL_BASE/{$ARG_MECHANIC_ID}/request"

    fun detail(mechanicId: String) = "$DETAIL_BASE/$mechanicId"
    fun request(mechanicId: String) = "$DETAIL_BASE/$mechanicId/request"
}
