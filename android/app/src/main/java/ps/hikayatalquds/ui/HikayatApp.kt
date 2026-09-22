package ps.hikayatalquds.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import androidx.navigation.toRoute
import ps.hikayatalquds.core.designsystem.HikayatTheme
import ps.hikayatalquds.core.ui.TestTags
import ps.hikayatalquds.ui.about.AboutScreen
import ps.hikayatalquds.ui.connections.ConnectionsScreen
import ps.hikayatalquds.ui.contribute.ContributeScreen
import ps.hikayatalquds.ui.detail.LocationDetailScreen
import ps.hikayatalquds.ui.home.HomeScreen
import ps.hikayatalquds.ui.keepsake.KeepsakeScreen
import ps.hikayatalquds.ui.map.MapScreen
import ps.hikayatalquds.ui.navigation.AboutRoute
import ps.hikayatalquds.ui.navigation.ConnectionsRoute
import ps.hikayatalquds.ui.navigation.ContributeRoute
import ps.hikayatalquds.ui.navigation.DeepLinks
import ps.hikayatalquds.ui.navigation.HomeRoute
import ps.hikayatalquds.ui.navigation.KeepsakeRoute
import ps.hikayatalquds.ui.navigation.LocationRoute
import ps.hikayatalquds.ui.navigation.MapRoute
import ps.hikayatalquds.ui.navigation.PlanTourRoute
import ps.hikayatalquds.ui.navigation.SearchRoute
import ps.hikayatalquds.ui.navigation.SettingsRoute
import ps.hikayatalquds.ui.navigation.TopLevelDestination
import ps.hikayatalquds.ui.plan.PlanTourScreen
import ps.hikayatalquds.ui.search.SearchScreen
import ps.hikayatalquds.ui.settings.SettingsScreen

/**
 * The app shell: a bottom bar over a navigation graph.
 *
 * The bar carries the five things somebody does in Jerusalem with this app -
 * read, find, walk, connect, contribute. Search, settings, the keepsake and the
 * about page are reached from the screens that own them rather than competing
 * for a sixth slot nobody would press.
 */
@Composable
fun HikayatApp(
    appState: HikayatAppState,
    navController: NavHostController = rememberNavController(),
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    val showBottomBar = TopLevelDestination.entries.any { destination ->
        currentDestination?.hasRoute(destination.route::class) == true
    }

    Scaffold(
        containerColor = HikayatTheme.colors.pageBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = fadeIn(tween(160)),
                exit = fadeOut(tween(160)),
            ) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp,
                ) {
                    TopLevelDestination.entries.forEach { destination ->
                        val selected =
                            currentDestination?.hasRoute(destination.route::class) == true
                        NavigationBarItem(
                            modifier = Modifier.testTag(TestTags.navItem(destination.name)),
                            selected = selected,
                            onClick = {
                                if (!selected) {
                                    navController.navigate(destination.route) {
                                        popUpTo(HomeRoute) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (selected) {
                                        destination.selectedIcon
                                    } else {
                                        destination.icon
                                    },
                                    contentDescription = null,
                                )
                            },
                            label = { Text(stringResource(destination.label)) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = HikayatTheme.colors.muted,
                                unselectedTextColor = HikayatTheme.colors.muted,
                            ),
                        )
                    }
                }
            }
        },
    ) { padding ->
        val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = padding.calculateBottomPadding()),
        ) {
            HikayatNavHost(
                navController = navController,
                appState = appState,
                snackbarHostState = snackbarHostState,
                contentPadding = padding,
                slideAway = rtl,
            )
        }
    }
}

@Composable
private fun HikayatNavHost(
    navController: NavHostController,
    appState: HikayatAppState,
    snackbarHostState: SnackbarHostState,
    contentPadding: PaddingValues,
    slideAway: Boolean,
) {
    // Forward motion follows the reading direction, so in Arabic a new screen
    // arrives from the left rather than fighting the layout.
    val direction = if (slideAway) -1 else 1

    NavHost(
        navController = navController,
        startDestination = HomeRoute,
        enterTransition = {
            slideInHorizontally(tween(260)) { full -> direction * full / 6 } + fadeIn(tween(200))
        },
        exitTransition = { fadeOut(tween(140)) },
        popEnterTransition = { fadeIn(tween(200)) },
        popExitTransition = {
            slideOutHorizontally(tween(220)) { full -> direction * full / 6 } + fadeOut(tween(160))
        },
    ) {
        composable<HomeRoute> {
            HomeScreen(
                appState = appState,
                contentPadding = contentPadding,
                onOpenLocation = { navController.navigate(LocationRoute(it)) },
                onOpenMap = { navController.navigate(MapRoute) },
                onOpenPlan = { navController.navigate(PlanTourRoute) },
                onOpenSearch = { navController.navigate(SearchRoute) },
                onOpenSettings = { navController.navigate(SettingsRoute) },
                onOpenAbout = { navController.navigate(AboutRoute) },
                onOpenKeepsake = { navController.navigate(KeepsakeRoute) },
            )
        }

        composable<MapRoute>(
            deepLinks = listOf(navDeepLink { uriPattern = DeepLinks.MAP_PATTERN }),
        ) {
            MapScreen(
                appState = appState,
                contentPadding = contentPadding,
                onOpenLocation = { navController.navigate(LocationRoute(it)) },
                onOpenPlan = { navController.navigate(PlanTourRoute) },
            )
        }

        composable<PlanTourRoute>(
            deepLinks = listOf(navDeepLink { uriPattern = DeepLinks.PLAN_PATTERN }),
        ) {
            PlanTourScreen(
                appState = appState,
                contentPadding = contentPadding,
                onOpenLocation = { navController.navigate(LocationRoute(it)) },
                onOpenMap = { navController.navigate(MapRoute) },
            )
        }

        composable<ConnectionsRoute> {
            ConnectionsScreen(
                appState = appState,
                contentPadding = contentPadding,
                onOpenLocation = { navController.navigate(LocationRoute(it)) },
            )
        }

        composable<ContributeRoute> { entry ->
            val route = entry.toRoute<ContributeRoute>()
            ContributeScreen(
                appState = appState,
                contentPadding = contentPadding,
                initialLocationId = route.locationId,
                snackbarHostState = snackbarHostState,
            )
        }

        composable<LocationRoute>(
            deepLinks = listOf(navDeepLink { uriPattern = DeepLinks.LOCATION_PATTERN }),
        ) { entry ->
            val route = entry.toRoute<LocationRoute>()
            LocationDetailScreen(
                locationId = route.locationId,
                appState = appState,
                snackbarHostState = snackbarHostState,
                onBack = { navController.navigateUp() },
                onOpenLocation = { navController.navigate(LocationRoute(it)) },
                onContribute = { navController.navigate(ContributeRoute(route.locationId)) },
            )
        }

        composable<SearchRoute> {
            SearchScreen(
                appState = appState,
                onBack = { navController.navigateUp() },
                onOpenLocation = { navController.navigate(LocationRoute(it)) },
            )
        }

        composable<SettingsRoute> {
            SettingsScreen(
                appState = appState,
                snackbarHostState = snackbarHostState,
                onBack = { navController.navigateUp() },
                onOpenAbout = { navController.navigate(AboutRoute) },
            )
        }

        composable<AboutRoute> {
            AboutScreen(onBack = { navController.navigateUp() })
        }

        composable<KeepsakeRoute> {
            KeepsakeScreen(
                appState = appState,
                snackbarHostState = snackbarHostState,
                onBack = { navController.navigateUp() },
                onOpenLocation = { navController.navigate(LocationRoute(it)) },
            )
        }
    }
}
