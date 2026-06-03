# Main Module

Навигация и нижняя панель приложения.

## Обзор

Модуль `main` отвечает за навигацию между основными разделами приложения и отображение нижней панели навигации (Bottom Navigation).

## Основные классы

### Screens
Определение маршрутов экранов:

```kotlin
sealed class Screens(val route: String) {
    object News : Screens("news_route")
    object Books : Screens("books_route")
    object Wiki : Screens("wiki_route")
    object Info : Screens("info_route")
}
```

### BottomNavigationItem
Элемент нижней навигации:

```kotlin
data class BottomNavigationItem(
    val screen: Screens,
    val icon: ImageVector,
    val label: String
)
```

### BottomNavigationUi
Compose-компонент нижней навигации.

### BottomNavScreen
Главный экран с нижней навигацией:

```kotlin
@Composable
fun BottomNavScreen(
    navController: NavHostController
)
```

### BooksVector
Векторные иконки для раздела книг.

## Навигация

Навигация построена на Jetpack Navigation Compose:

```kotlin
NavHost(
    navController = navController,
    startDestination = Screens.News.route
) {
    composable(Screens.News.route) { NewsScreen() }
    composable(Screens.Books.route) { BooksScreen() }
    composable(Screens.Wiki.route) { WikiScreen() }
    composable(Screens.Info.route) { InfoScreen() }
}
```

## Структура файлов

```
main/src/main/java/su/sv/main/
├── Screens.kt                      # Маршруты экранов
├── res/
│   └── BooksVector.kt              # Векторные иконки
└── bottomnav/
    ├── BottomNavigationItem.kt     # Модель элемента
    ├── BottomNavigationUi.kt       # UI компонент
    └── BottomNavScreen.kt          # Главный экран
```
