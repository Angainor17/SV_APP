package su.sv.main.bottomnav

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.github.axet.bookreader.screens.ReaderScreen
import com.github.terrakok.modo.stack.LocalStackNavigation
import com.github.terrakok.modo.stack.forward
import su.sv.books.catalog.presentation.root.ui.RootBooksCatalog
import su.sv.commonui.theme.DeviceFormFactor
import su.sv.commonui.theme.LocalDeviceFormFactor
import su.sv.commonui.theme.ThemeMode
import su.sv.commonui.ui.adaptive.navigation.AdaptiveNavigation
import su.sv.commonui.ui.adaptive.navigation.NavigationItem
import su.sv.commonui.util.ProvideAdaptiveDimensions
import su.sv.info.rootinfo.ui.RootInfo
import su.sv.main.R
import su.sv.main.Screens
import su.sv.main.badge.BadgeViewModel
import su.sv.main.continuereading.ContinueReadingEffect
import su.sv.main.continuereading.ContinueReadingState
import su.sv.main.continuereading.ContinueReadingViewModel
import su.sv.main.continuereading.ui.ContinueReadingSnackbarHost
import su.sv.main.res.BooksVector
import su.sv.main.testing.MainTestTags
import su.sv.managers.theme.ThemeViewModel
import su.sv.news.presentation.root.ui.RootNews
import su.sv.wiki.root.RootWiki

/**
 * Главный экран с адаптивной навигацией
 *
 * - Compact: BottomNavigation (снизу)
 * - Medium/Expanded: NavigationRail (слева)
 *
 * @param themeViewModel ViewModel для управления темой
 * @param badgeViewModel ViewModel для бейджей
 * @param continueReadingViewModel ViewModel для snackbar "Продолжить чтение"
 */
@Composable
internal fun BottomNavigationBar(
    themeViewModel: ThemeViewModel = hiltViewModel(),
    badgeViewModel: BadgeViewModel = hiltViewModel(),
    continueReadingViewModel: ContinueReadingViewModel = hiltViewModel(),
) {
    // Предоставляем адаптивные размеры через CompositionLocal
    ProvideAdaptiveDimensions {
        AdaptiveBottomNavContent(
            themeViewModel = themeViewModel,
            badgeViewModel = badgeViewModel,
            continueReadingViewModel = continueReadingViewModel,
        )
    }
}

@Composable
private fun AdaptiveBottomNavContent(
    themeViewModel: ThemeViewModel,
    badgeViewModel: BadgeViewModel,
    continueReadingViewModel: ContinueReadingViewModel,
) {
    // Состояние темы
    val themeConfig by themeViewModel.themeConfig.collectAsStateWithLifecycle()

    // Состояние бейджа Wiki
    val showWikiBadge by badgeViewModel.showWikiBadge.collectAsStateWithLifecycle()

    // Состояние snackbar "Продолжить чтение"
    val continueReadingState by continueReadingViewModel.state.collectAsStateWithLifecycle()

    // Modo навигация для открытия книги
    val stackNavigation = LocalStackNavigation.current

    // Загружаем данные о последней книге при запуске
    LaunchedEffect(Unit) {
        continueReadingViewModel.loadAndCheck()
    }

    // Обработка эффектов от snackbar
    LaunchedEffect(continueReadingViewModel.effect) {
        continueReadingViewModel.effect.collect { effect ->
            when (effect) {
                is ContinueReadingEffect.OpenBook -> {
                    stackNavigation.forward(
                        ReaderScreen(
                            bookUri = effect.bookUri,
                            bookTitle = effect.bookTitle,
                            bookAuthor = effect.bookAuthor,
                            bookCoverUrl = effect.coverUrl,
                        )
                    )
                }
            }
        }
    }

    BottomNavContent(
        showWikiBadge = showWikiBadge,
        onWikiBadgeClick = { badgeViewModel.markWikiAsVisited() },
        onThemeToggle = { themeViewModel.toggleTheme() },
        currentThemeMode = themeConfig.themeMode,
        continueReadingState = continueReadingState,
        onContinueReadingClick = { continueReadingViewModel.onContinueClick() },
        onContinueReadingDismiss = { continueReadingViewModel.onDismissClick() },
    )
}

@Composable
private fun BottomNavContent(
    showWikiBadge: Boolean,
    onWikiBadgeClick: () -> Unit,
    onThemeToggle: () -> Unit,
    currentThemeMode: ThemeMode,
    continueReadingState: ContinueReadingState,
    onContinueReadingClick: () -> Unit,
    onContinueReadingDismiss: () -> Unit,
) {
    val navController = rememberNavController()
    val formFactor = LocalDeviceFormFactor.current

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

    // Состояние для хранения высоты NavigationBar
    var navigationBarHeight by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current

    // Элементы навигации
    val bottomItems = bottomNavigationItems()
    val navigationItems = remember(showWikiBadge, bottomItems) {
        bottomItems.map { item ->
            NavigationItem(
                label = item.label,
                icon = item.icon,
                route = item.route,
                showBadge = item.route == Screens.Wiki.route && showWikiBadge,
            )
        }
    }

    // Box для overlay snackbar над NavigationBar
    Box(modifier = Modifier.fillMaxSize()) {
        when (formFactor) {
            is DeviceFormFactor.Compact -> {
                // Compact: BottomNavigation
                CompactNavLayout(
                    navController = navController,
                    navigationItems = navigationItems,
                    navigationSelectedItem = navigationSelectedItem,
                    onItemSelected = { index ->
                        handleNavigation(
                            navController = navController,
                            route = navigationItems[index].route,
                            showWikiBadge = showWikiBadge,
                            onWikiBadgeClick = onWikiBadgeClick,
                        )
                    },
                    onNavigationBarSizeChanged = { size ->
                        navigationBarHeight = size
                    },
                    onThemeToggle = onThemeToggle,
                    currentThemeMode = currentThemeMode,
                )
            }

            is DeviceFormFactor.Medium,
            is DeviceFormFactor.Expanded -> {
                // Medium/Expanded: NavigationRail
                RailNavLayout(
                    navController = navController,
                    navigationItems = navigationItems,
                    navigationSelectedItem = navigationSelectedItem,
                    onItemSelected = { index ->
                        handleNavigation(
                            navController = navController,
                            route = navigationItems[index].route,
                            showWikiBadge = showWikiBadge,
                            onWikiBadgeClick = onWikiBadgeClick,
                        )
                    },
                    onThemeToggle = onThemeToggle,
                    currentThemeMode = currentThemeMode,
                )
                // Для Rail нет bottom padding для snackbar
                navigationBarHeight = 0
            }
        }

        // Snackbar "Продолжить чтение" над NavigationBar (только для Compact)
        if (formFactor.isCompact()) {
            ContinueReadingSnackbarHost(
                state = continueReadingState,
                onContinueClick = onContinueReadingClick,
                onDismissClick = onContinueReadingDismiss,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = with(density) { navigationBarHeight.toDp() })
            )
        }
    }
}

@Composable
private fun CompactNavLayout(
    navController: NavHostController,
    navigationItems: List<NavigationItem>,
    navigationSelectedItem: Int,
    onItemSelected: (Int) -> Unit,
    onNavigationBarSizeChanged: (Int) -> Unit,
    onThemeToggle: () -> Unit,
    currentThemeMode: ThemeMode,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            AdaptiveNavigation(
                items = navigationItems,
                selectedItem = navigationSelectedItem,
                onItemSelected = onItemSelected,
                modifier = Modifier
                    .testTag(MainTestTags.BottomNav.ROOT)
                    .onSizeChanged { size ->
                        onNavigationBarSizeChanged(size.height)
                    },
            )
        },
    ) { paddingValues ->
        BottomNavHost(
            navController = navController,
            paddingValues = paddingValues,
            onThemeToggle = onThemeToggle,
            currentThemeMode = currentThemeMode,
        )
    }
}

@Composable
private fun RailNavLayout(
    navController: NavHostController,
    navigationItems: List<NavigationItem>,
    navigationSelectedItem: Int,
    onItemSelected: (Int) -> Unit,
    onThemeToggle: () -> Unit,
    currentThemeMode: ThemeMode,
) {
    Row(modifier = Modifier.fillMaxSize()) {
        // NavigationRail слева
        AdaptiveNavigation(
            items = navigationItems,
            selectedItem = navigationSelectedItem,
            onItemSelected = onItemSelected,
            modifier = Modifier.fillMaxHeight(),
        )

        // Контент справа
        Scaffold(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
        ) { paddingValues ->
            BottomNavHost(
                navController = navController,
                paddingValues = paddingValues,
                onThemeToggle = onThemeToggle,
                currentThemeMode = currentThemeMode,
            )
        }
    }
}

private fun handleNavigation(
    navController: NavHostController,
    route: String,
    showWikiBadge: Boolean,
    onWikiBadgeClick: () -> Unit,
) {
    // Скрываем бейдж при клике на Wiki
    if (route == Screens.Wiki.route && showWikiBadge) {
        onWikiBadgeClick()
    }
    navController.navigate(route) {
        popUpTo(navController.graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun BottomNavHost(
    navController: NavHostController,
    paddingValues: PaddingValues,
    onThemeToggle: () -> Unit,
    currentThemeMode: ThemeMode,
) {
    NavHost(
        navController = navController,
        startDestination = Screens.News.route,
        modifier = Modifier.padding(paddingValues),
    ) {
        composable(Screens.News.route) {
            RootNews(
                onThemeToggle = onThemeToggle,
                currentThemeMode = currentThemeMode,
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
            route = Screens.News.route,
        ),
        BottomNavigationItem(
            label = stringResource(R.string.nav_bar_books),
            icon = Icons.Filled.BooksVector,
            route = Screens.Books.route,
        ),
        BottomNavigationItem(
            label = stringResource(R.string.nav_bar_wiki),
            icon = Icons.Filled.Search,
            route = Screens.Wiki.route,
        ),
        BottomNavigationItem(
            label = stringResource(R.string.nav_bar_info),
            icon = Icons.Filled.Info,
            route = Screens.Info.route,
        ),
    )
}