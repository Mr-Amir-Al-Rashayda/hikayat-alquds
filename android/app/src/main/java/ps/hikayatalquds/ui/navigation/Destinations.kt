package ps.hikayatalquds.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.serialization.Serializable
import ps.hikayatalquds.R

/**
 * Every place the app can be, as a type.
 *
 * Type-safe routes rather than string paths: a destination that needs a
 * location id cannot be navigated to without one, which is checked by the
 * compiler instead of at runtime on somebody's phone.
 */
sealed interface Destination

@Serializable data object HomeRoute : Destination

@Serializable data object MapRoute : Destination

@Serializable data object PlanTourRoute : Destination

@Serializable data object ConnectionsRoute : Destination

/** [locationId] preselects the place when arriving from a location screen. */
@Serializable data class ContributeRoute(val locationId: String? = null) : Destination

@Serializable data class LocationRoute(val locationId: String) : Destination

@Serializable data object SearchRoute : Destination

@Serializable data object SettingsRoute : Destination

@Serializable data object AboutRoute : Destination

@Serializable data object KeepsakeRoute : Destination

/** The five destinations on the bottom bar, in reading order. */
enum class TopLevelDestination(
    val route: Destination,
    @StringRes val label: Int,
    val selectedIcon: ImageVector,
    val icon: ImageVector,
) {
    HOME(HomeRoute, R.string.nav_home, Icons.Filled.Home, Icons.Outlined.Home),
    MAP(MapRoute, R.string.nav_map, Icons.Filled.Map, Icons.Outlined.Map),
    PLAN(PlanTourRoute, R.string.nav_plan, Icons.Filled.Route, Icons.Outlined.Route),
    CONNECTIONS(ConnectionsRoute, R.string.nav_connections, Icons.Filled.Hub, Icons.Outlined.Hub),
    CONTRIBUTE(
        ContributeRoute(),
        R.string.nav_contribute,
        Icons.Filled.EditNote,
        Icons.Outlined.EditNote,
    ),
}

/** Deep links: `hikayat://locations/silwan`, `hikayat://map`, `hikayat://plan`. */
object DeepLinks {
    const val SCHEME = "hikayat"
    const val LOCATION_PATTERN = "$SCHEME://locations/{locationId}"
    const val MAP_PATTERN = "$SCHEME://map"
    const val PLAN_PATTERN = "$SCHEME://plan"
}
