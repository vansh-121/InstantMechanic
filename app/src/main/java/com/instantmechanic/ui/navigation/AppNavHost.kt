package com.instantmechanic.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.instantmechanic.ui.detail.DetailRoute
import com.instantmechanic.ui.home.HomeRoute
import com.instantmechanic.ui.request.RequestServiceRoute

/**
 * The whole navigation graph: Home → Detail → Request.
 *
 * Each destination gets its own `hiltViewModel()`, scoped to that back-stack entry, so a
 * ViewModel is created when you arrive and cleared when you leave — no manual lifecycle bookkeeping
 * and no state leaking between garages.
 */
@Composable
fun AppNavHost(
    navController: NavHostController = rememberNavController(),
    isDarkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {},
) {
    NavHost(navController = navController, startDestination = Routes.HOME) {

        composable(Routes.HOME) {
            HomeRoute(
                onMechanicClick = { id -> navController.navigate(Routes.detail(id)) },
                isDarkTheme = isDarkTheme,
                onToggleTheme = onToggleTheme,
            )
        }

        composable(
            route = Routes.DETAIL,
            arguments = listOf(navArgument(Routes.ARG_MECHANIC_ID) { type = NavType.StringType }),
        ) {
            DetailRoute(
                onBack = { navController.popBackStack() },
                onRequestService = { id -> navController.navigate(Routes.request(id)) },
            )
        }

        composable(
            route = Routes.REQUEST,
            arguments = listOf(navArgument(Routes.ARG_MECHANIC_ID) { type = NavType.StringType }),
        ) {
            RequestServiceRoute(
                onBack = { navController.popBackStack() },
                // After a successful booking, Detail and Request are both history: returning to
                // the form via Back would let the user submit the same job twice.
                onDone = {
                    navController.popBackStack(route = Routes.HOME, inclusive = false)
                },
            )
        }
    }
}
