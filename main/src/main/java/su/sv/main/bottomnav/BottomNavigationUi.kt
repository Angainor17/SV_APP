package su.sv.main.bottomnav

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import su.sv.books.catalog.presentation.root.ui.RootBooksCatalog
import su.sv.info.rootinfo.ui.RootInfo
import su.sv.main.Screens
import su.sv.main.res.BooksVector
import su.sv.news.presentation.root.ui.RootNews

@Composable
internal fun BottomNavigationBar() {
    var navigationSelectedItem by remember {
        mutableIntStateOf(0)
    }
    val navController = rememberNavController()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                bottomNavigationItems()
                    .forEachIndexed { index, navigationItem ->
                        NavigationBarItem(
                            selected = index == navigationSelectedItem,
                            label = {
                                Text(navigationItem.label)
                            },
                            icon = {
                                Icon(
                                    imageVector = navigationItem.icon,
                                    contentDescription = navigationItem.label,
                                    modifier = Modifier.size(24.dp),
                                )
                            },
                            onClick = {
                                navigationSelectedItem = index
                                navController.navigate(navigationItem.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
            }
        },
    ) { paddingValues ->
        BottomNavHost(navController, paddingValues)
    }
}

@Composable
private fun BottomNavHost(
    navController: NavHostController,
    paddingValues: PaddingValues
) {
    NavHost(
        navController = navController,
        startDestination = Screens.News.route,
        modifier = Modifier.padding(
            paddingValues = paddingValues,
        )
    ) {
        composable(Screens.News.route) {
            RootNews()
        }
        composable(Screens.Books.route) {
            RootBooksCatalog()
        }
//        composable(Screens.Wiki.route) {
//            RootWiki()
//        }
        composable(Screens.Info.route) {
            RootInfo()
        }
    }
}

@Composable
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
//        BottomNavigationItem(
//            label = "Wiki",
//            icon = ImageVector.vectorResource(R.drawable.ic_wikipedia),
//            route = Screens.Wiki.route
//        ),
        BottomNavigationItem(
            label = "Info",
            icon = Icons.Filled.Info,
            route = Screens.Info.route
        ),
    )
}
