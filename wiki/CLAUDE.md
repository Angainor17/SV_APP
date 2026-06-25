# Wiki Module

Модуль Wiki-страницы для отображения статей с сайта svremya.su.

## Обзор

Модуль `wiki` предоставляет функционал:
- Поиск статей с автодополнением
- Просмотр статей с кликабельными ссылками
- Избранное (сохранение статей)
- История просмотра
- Кэширование статей для офлайн-доступа

## Навигация

Используется библиотека **Modo** (`com.github.terrakok.modo`):
- `RootWiki` — главный экран (точка входа), применяет тему
- `ArticleScreen` — экран статьи
- `FavoritesScreen` — экран избранного

### Применение темы

Тема применяется на уровне корневого экрана `RootWiki`:

```kotlin
@Composable
fun RootWiki(viewModel: RootWikiViewModel = hiltViewModel()) {
    val themeViewModel: ThemeViewModel = hiltViewModel()
    val themeConfig by themeViewModel.themeConfig.collectAsStateWithLifecycle()

    SVAPPTheme(
        themeMode = themeConfig.themeMode,
        useDynamicColors = themeConfig.useDynamicColors
    ) {
        // Контент
    }
}
```

Дочерние экраны (`ArticleScreen`, `FavoritesScreen`) **не применяют** тему отдельно, так как она уже применена на уровне корневого экрана.

## Архитектура

```
wiki/src/main/java/su/sv/wiki/
├── data/                          # Data слой
│   ├── api/                       # API клиент
│   │   ├── WikiApi.kt             # Retrofit интерфейс
│   │   └── model/                 # API модели
│   ├── local/                     # Локальное хранилище (Room)
│   │   ├── dao/                   # DAO интерфейсы
│   │   │   ├── ArticleCacheDao.kt # Кэш статей
│   │   │   ├── FavoriteDao.kt     # Избранное
│   │   │   └── HistoryDao.kt      # История
│   │   ├── database/              # База данных
│   │   └── entity/                # Entity классы
│   └── repository/                # Реализация репозитория
│       └── WikiRepositoryImpl.kt
├── domain/                        # Domain слой
│   ├── model/                     # Доменные модели
│   │   └── WikiArticle.kt
│   ├── repository/                # Интерфейсы репозиториев
│   │   └── WikiRepository.kt
│   └── usecase/                   # Use Cases
├── presentation/                  # Presentation слой
│   ├── article/                   # Экран статьи
│   │   ├── ArticleScreen.kt       # Modo Screen
│   │   ├── ArticleScreenContent.kt
│   │   └── ArticleViewModel.kt
│   ├── favorites/                 # Экран избранного
│   │   ├── FavoritesScreen.kt
│   │   ├── FavoritesScreenContent.kt
│   │   └── FavoritesViewModel.kt
│   └── root/                      # Главный экран
│       ├── mapper/                # Мапперы
│       ├── model/                 # UI модели
│       ├── ui/                    # UI компоненты
│       │   ├── ArticleContent.kt  # Контент статьи с ссылками
│       │   ├── ArticleView.kt     # Карточка статьи
│       │   ├── HistoryList.kt     # Список истории
│       │   ├── SearchSuggestions.kt
│       │   └── WikiSearchBar.kt   # Поле поиска
│       └── viewmodel/             # ViewModel
│           ├── RootWikiViewModel.kt
│           ├── actions/           # Действия (MVI)
│           └── effects/           # Одноразовые эффекты
├── di/                            # Dependency Injection
│   ├── WikiApiModule.kt
│   └── WikiDatabaseModule.kt
└── root/
    └── RootWiki.kt                # Точка входа
```

## Основные компоненты

### RootWiki
Главный экран Wiki с поиском и историей:
```kotlin
@Composable
fun RootWiki(viewModel: RootWikiViewModel = hiltViewModel())
```

### ArticleScreen
Экран статьи с кэшированием:
```kotlin
@Parcelize
class ArticleScreen(private val title: String) : Screen
```

### FavoritesScreen
Экран списка избранных статей:
```kotlin
@Parcelize
class FavoritesScreen : Screen
```

## Кэширование статей

Статьи автоматически кэшируются при первом просмотре:
1. При запросе статьи сначала проверяется локальный кэш
2. Если статьи нет в кэше — загружается из сети
3. После успешной загрузки статья сохраняется в кэш

Это позволяет открывать статьи из истории/избранного без сетевых запросов.

## База данных (Room)

**Таблицы:**
- `article_cache` — кэш статей (контент, ссылки, URL)
- `favorites` — избранные статьи
- `history` — история поиска (только заголовки)

**Версия БД:** 2

## API

Базовый URL: `https://svremya.su/`

**Методы:**
- `search()` — поиск статей
- `getPage()` — получение статьи по заголовку
- `openSearch()` — подсказки для поиска

## Модели

### WikiArticle (Domain)
```kotlin
data class WikiArticle(
    val title: String,
    val pageId: Int,
    val content: String,           // HTML контент
    val links: List<WikiLink>,     // Внутренние ссылки
    val externalLinks: List<WikiExternalLink>, // Внешние ссылки
    val articleUrl: String,        // URL на сайте
)
```

### UiWikiState (UI)
```kotlin
sealed class UiWikiState {
    object Initial : UiWikiState()
    object Loading : UiWikiState()
    data class Content(...) : UiWikiState()
    object NotFound : UiWikiState()
    data class Error(...) : UiWikiState()
}
```

## Правила разработки

- **Строковые ресурсы**: Все строки выносить в `strings.xml`
- **MVI паттерн**: Actions → ViewModel → State/Effects
- **Кэширование**: Статьи кэшируются автоматически
- **Клавиатура**: Скрывается при кликах вне поля ввода
- **Статус-бар**: Использовать `WindowInsets.statusBars` в Scaffold для корректного отображения
- **Тема**: Применяется на уровне `RootWiki`, дочерние экраны не применяют тему отдельно

## Используемые библиотеки

- **Modo** — навигация
- **Hilt** — DI
- **Room** — локальная база данных
- **Retrofit** — API клиент
- **Timber** — логирование
