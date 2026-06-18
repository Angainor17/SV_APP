package su.sv.main.bottomnav

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import su.sv.books.catalog.presentation.root.ui.RootBooksCatalog
import su.sv.info.rootinfo.ui.RootInfo
import su.sv.main.R
import su.sv.main.Screens
import su.sv.main.badge.BadgeViewModel
import su.sv.main.badge.NewBadge
import su.sv.main.res.BooksVector
import su.sv.news.presentation.root.ui.RootNews
import su.sv.wiki.root.RootWiki

@Composable
internal fun BottomNavigationBar(
    badgeViewModel: BadgeViewModel = hiltViewModel(),
) {
    var navigationSelectedItem by remember {
        mutableIntStateOf(0)
    }
    val navController = rememberNavController()
    val showWikiBadge by badgeViewModel.showWikiBadge.collectAsStateWithLifecycle()

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
                                NavigationIcon(
                                    icon = navigationItem.icon,
                                    label = navigationItem.label,
                                    showBadge = navigationItem.route == Screens.Wiki.route && showWikiBadge,
                                )
                            },
                            onClick = {
                                // Скрываем бейдж при клике на Wiki
                                if (navigationItem.route == Screens.Wiki.route && showWikiBadge) {
                                    badgeViewModel.markWikiAsVisited()
                                }
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
private fun NavigationIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    showBadge: Boolean,
) {
    Box {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(24.dp),
        )
        if (showBadge) {
            NewBadge(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 4.dp, y = 4.dp),
            )
        }
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
        composable(Screens.Wiki.route) {
            RootWiki()
        }
        composable(Screens.Info.route) {
            RootInfo()
        }
    }
}

@Composable
fun bottomNavigationItems(): List<BottomNavigationItem> {
    return listOf(
        BottomNavigationItem(
            label = stringResource(R.string.nav_bar_news),
            icon = Icons.Filled.Home,
            route = Screens.News.route
        ),
        BottomNavigationItem(
            label = stringResource(R.string.nav_bar_books),
            icon = Icons.Filled.BooksVector,
            route = Screens.Books.route
        ),
        BottomNavigationItem(
            label = stringResource(R.string.nav_bar_wiki),
            icon = Icons.Filled.Search,
            route = Screens.Wiki.route
        ),
        BottomNavigationItem(
            label = stringResource(R.string.nav_bar_info),
            icon = Icons.Filled.Info,
            route = Screens.Info.route
        ),
    )
}
