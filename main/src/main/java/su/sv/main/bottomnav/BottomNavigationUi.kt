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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import su.sv.books.catalog.presentation.root.ui.RootBooksCatalog
import su.sv.commonui.theme.ThemeMode
import su.sv.info.rootinfo.ui.RootInfo
import su.sv.main.R
import su.sv.main.Screens
import su.sv.main.badge.BadgeViewModel
import su.sv.main.badge.NewBadge
import su.sv.main.res.BooksVector
import su.sv.main.testing.MainTestTags
import su.sv.managers.theme.ThemeViewModel
import su.sv.news.presentation.root.ui.RootNews
import su.sv.wiki.root.RootWiki

/**
 * Главный экран с нижней навигацией
 *
 * Управляет темой приложения и навигацией между основными разделами.
 *
 * @param themeViewModel ViewModel для управления темой
 * @param badgeViewModel ViewModel для бейджей
 */
@Composable
internal fun BottomNavigationBar(
    themeViewModel: ThemeViewModel = hiltViewModel(),
    badgeViewModel: BadgeViewModel = hiltViewModel(),
) {
    // Состояние темы
    val themeConfig by themeViewModel.themeConfig.collectAsStateWithLifecycle()

    // Состояние бейджа Wiki
    val showWikiBadge by badgeViewModel.showWikiBadge.collectAsStateWithLifecycle()

    BottomNavContent(
        showWikiBadge = showWikiBadge,
        onWikiBadgeClick = { badgeViewModel.markWikiAsVisited() },
        onThemeToggle = { themeViewModel.toggleTheme() },
        currentThemeMode = themeConfig.themeMode
    )
}

@Composable
private fun BottomNavContent(
    showWikiBadge: Boolean,
    onWikiBadgeClick: () -> Unit,
    onThemeToggle: () -> Unit,
    currentThemeMode: ThemeMode
) {
    val navController = rememberNavController()

    // Отслеживаем текущий маршрут
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Определяем индекс текущего элемента
    val navigationSelectedItem = remember(currentRoute) {
        when (currentRoute) {
            Screens.News.route -> 0
            Screens.Books.route -> 1
            Screens.Wiki.route -> 2
            Screens.Info.route -> 3
            else -> 0
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                modifier = Modifier.testTag(MainTestTags.BottomNav.ROOT),
                containerColor = MaterialTheme.colorScheme.background  // Цвет фона приложения
            ) {
                bottomNavigationItems()
                    .forEachIndexed { index, navigationItem ->
                        val testTag = when (navigationItem.route) {
                            Screens.News.route -> MainTestTags.BottomNav.TAB_NEWS
                            Screens.Books.route -> MainTestTags.BottomNav.TAB_BOOKS
                            Screens.Wiki.route -> MainTestTags.BottomNav.TAB_WIKI
                            Screens.Info.route -> MainTestTags.BottomNav.TAB_INFO
                            else -> "tab_unknown"
                        }

                        NavigationBarItem(
                            modifier = Modifier.testTag(testTag),
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
                                    onWikiBadgeClick()
                                }
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
        BottomNavHost(
            navController = navController,
            paddingValues = paddingValues,
            onThemeToggle = onThemeToggle,
            currentThemeMode = currentThemeMode
        )
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
    paddingValues: PaddingValues,
    onThemeToggle: () -> Unit,
    currentThemeMode: ThemeMode
) {
    NavHost(
        navController = navController,
        startDestination = Screens.News.route,
        modifier = Modifier.padding(
            paddingValues = paddingValues,
        ),
    ) {
        composable(Screens.News.route) {
            RootNews(
                onThemeToggle = onThemeToggle,
                currentThemeMode = currentThemeMode
            )
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
