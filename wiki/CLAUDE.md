# Wiki Module

Модуль Wiki-страницы для отображения статей с сайта svremya.su.

## Обзор

Модуль `wiki` предоставляет функционал:
- Поиск статей с автодополнением (регистронезависимый, поиск по всему заголовку)
- Просмотр статей с кликабельными ссылками и изображениями
- Избранное (сохранение статей с картинками)
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
│       │   ├── AnimatedTypingHint.kt # Анимированный hint для поиска
│       │   ├── ArticleContent.kt  # Контент статьи с ссылками и картинкой
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

## Поиск

### API поиска
Используется MediaWiki API `search` с параметром `srwhat=title`:
- **Регистронезависимый** — "карл" найдёт "Маркс, Карл"
- **Поиск по всему заголовку** — не только по началу названия
- **Минимальная длина запроса** — 2 символа для suggestions, 3 символа для поиска статьи

### Методы поиска
- `searchArticle(query)` — поиск статьи (возвращает первую найденную)
- `getSearchSuggestions(query, limit)` — автодополнение (возвращает список заголовков)

### Фильтрация suggestions
Suggestions не показываются если:
1. `selectedSuggestion != null` —刚刚 выбрали suggestion и текст в поле
2. Suggestion совпадает с заголовком **текущей отображаемой статьи**

```kotlin
val filteredSuggestions = suggestions.filter { suggestion ->
    suggestion != currentArticleTitle
}
```

### Логирование поиска
Все запросы поиска логируются с тегом `"voronin"` через Timber:

```
searchArticle: query='ленин', results=2, first='Ленин говорит, содержание'
getSearchSuggestions: query='карл', results=2, titles=[Каутский, Карл, Маркс, Карл]
```

## Изображения статей

Изображения извлекаются из HTML-контента статьи:
- **extimg контейнеры** — специфический формат svremya.su (ссылка на JPG/PNG в div с class="extimg")
- **img теги** — стандартные HTML изображения

Изображения отображаются:
- На экране статьи (с placeholder и ограничением высоты 300dp)
- В списке избранного (превью 80x80dp с placeholder)
- Загрузка через **Coil 3** (`SubcomposeAsyncImage`)

URL картинки сохраняется в базе данных явно в полях `imageUrl`.

## Кэширование статей

Статьи автоматически кэшируются при первом просмотре:
1. При запросе статьи сначала проверяется локальный кэш
2. Если статьи нет в кэше — загружается из сети
3. После успешной загрузки статья сохраняется в кэш (включая imageUrl)

Это позволяет открывать статьи из истории/избранного без сетевых запросов.

## База данных (Room)

**Таблицы:**
- `article_cache` — кэш статей (контент, ссылки, URL, imageUrl)
- `favorites` — избранные статьи (контент, ссылки, URL, imageUrl)
- `history` — история поиска (только заголовки)

**Версия БД:** 2

При миграции используется `fallbackToDestructiveMigration()` — данные пересоздаются.

## API

Базовый URL: `https://svremya.su/`

**Методы:**
- `search(query, what="title", limit)` — поиск статей по заголовкам
- `getPage(title)` — получение статьи по заголовку
- `openSearch()` — устаревший метод (не используется)

## Модели

### WikiArticle (Domain)
```kotlin
data class WikiArticle(
    val title: String,
    val pageId: Int,
    val content: String,           // HTML контент (без блока картинки)
    val links: List<WikiLink>,     // Внутренние ссылки
    val externalLinks: List<WikiExternalLink>, // Внешние ссылки
    val articleUrl: String,        // URL на сайте
    val imageUrl: String?,         // URL картинки
)
```

### UiWikiArticle (UI)
```kotlin
data class UiWikiArticle(
    val title: String,
    val content: String,
    val links: List<UiWikiLink>,
    val externalLinks: List<UiExternalLink>,
    val articleUrl: String,
    val imageUrl: String?,         // URL картинки
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

## UI особенности

### Иконка избранного
- Цвет из темы: `MaterialTheme.colorScheme.favorite`
- Красный `#E53935` (Material Red 600) для обеих тем
- `FavoriteBorder` для неактивного состояния

### Карточка избранного
- Фон: `surfaceContainerHigh`
- Elevation: `2.dp`
- Превью картинки: 80x80dp с placeholder

### Картинка статьи
- Ограничение высоты: `heightIn(max = 300.dp)`
- Placeholder: `CircularProgressIndicator` 32dp
- Скругление: `MaterialTheme.shapes.medium`

### Подсказки поиска (SearchSuggestions)
- Анимация: `expandVertically/shrinkVertically`
- Показываются над контентом
- Фильтруются (не показывают текущую статью)

### Анимированный hint поиска
Поле поиска показывает анимированный hint с эффектом печатающегося текста:
- **Без фокуса + поле пустое + прошло 2 сек** → анимированный hint (побуквенное появление)
- **Без фокуса + поле пустое + первые 2 сек** → статичный "Поиск"
- **В фокусе + поле пустое** → статичный "Поиск"
- **Поле не пустое** → введённый текст

**Параметры анимации:**
- `initialDelayMs = 2000ms` — начальная задержка перед началом анимации
- `initialPlaceholder` — текст во время начальной паузы (берётся из `wiki_search_label`)
- `typingSpeedMs = 110ms` — скорость печати одного символа
- `pauseBetweenHintsMs = 3000ms` — пауза между сменой hints

**Список hints** задаётся в `strings.xml` как строковый массив `wiki_search_hints`:
```xml
<string-array name="wiki_search_hints">
    <item>Диктатура пролетариата</item>
    <item>Классовая борьба</item>
    <item>В-себе-бытие</item>
    <item>Явление</item>
</string-array>
```

**Реализация:**
- `AnimatedTypingHint.kt` — компонент анимации
- `WikiSearchBar.kt` — поле поиска с animated hint
- Hints читаются из ресурсов через `context.resources.getStringArray()`

## Правила разработки

- **Строковые ресурсы**: Все строки выносить в `strings.xml`
- **MVI паттерн**: Actions → ViewModel → State/Effects
- **Кэширование**: Статьи кэшируются автоматически с imageUrl
- **Клавиатура**: Скрывается при кликах вне поля ввода
- **Статус-бар**: Использовать `WindowInsets.statusBars` в Scaffold для корректного отображения
- **Тема**: Применяется на уровне `RootWiki`, дочерние экраны не применяют тему отдельно
- **Изображения**: Coil 3 с placeholder через `SubcomposeAsyncImage`
- **Логирование**: Timber с тегом `"voronin"` для запросов поиска

## Используемые библиотеки

- **Modo** — навигация
- **Hilt** — DI
- **Room** — локальная база данных
- **Retrofit** — API клиент
- **Coil 3** — загрузка изображений
- **Timber** — логирование