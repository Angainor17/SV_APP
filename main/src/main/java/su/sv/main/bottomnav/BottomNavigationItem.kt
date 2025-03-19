package su.sv.main.bottomnav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.ui.graphics.vector.ImageVector
import su.sv.main.Screens
import su.sv.main.res.BooksVector
import su.sv.main.res.WikipediaVector

data class BottomNavigationItem(
    val label: String,
    val icon: ImageVector,
    val route: String,
)

fun bottomNavigationItems(): List<BottomNavigationItem> {
    return listOf(
        BottomNavigationItem(
            label = "News",
            icon = Icons.Filled.Home,
            route = Screens.News.route
        ),
        BottomNavigationItem(
            label = "Books",
            icon = Icons.Filled.BooksVector,
            route = Screens.Books.route
        ),
        BottomNavigationItem(
            label = "Wiki",
            icon = Icons.Filled.WikipediaVector,
            route = Screens.Wiki.route
        ),
        BottomNavigationItem(
            label = "Info",
            icon = Icons.Filled.Info,
            route = Screens.Info.route
        ),
    )
}
