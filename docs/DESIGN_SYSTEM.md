# SV APP Design System

## Обзор

Дизайн-система SV APP построена на базе **Material Design 3** (Material You) для Jetpack Compose. Система обеспечивает единообразный внешний вид приложения с поддержкой светлой и тёмной темы.

### Принципы

1. **Единая точка конфигурации** — все визуальные параметры определены в теме
2. **Поддержка тем** — светлая и тёмная тема с возможностью ручного переключения
3. **Переиспользуемые компоненты** — общие UI-компоненты в модуле `commonui`
4. **Адаптивность** — корректное отображение на разных размерах экранов (целевые — смартфоны)
5. **Доступность** — соблюдение контрастности и размеров касания

---

## Структура модуля темы

```
commonui/src/main/java/su/sv/commonui/theme/
├── Color.kt           # Цветовая палитра
├── Theme.kt           # Главная тема (SVAPPTheme)
├── Type.kt            # Типографика
├── Shape.kt           # Формы (радиусы скругления)
├── Dimensions.kt      # Отступы и размеры
└── ThemeConfig.kt     # Конфигурация темы (режим, настройки)
```

---

## Цветовая система

### Основные цвета (Primary)

| Назначение | Light Theme | Dark Theme | Описание |
|------------|-------------|------------|----------|
| `primary` | `#3F51B5` (Indigo 500) | `#D0BCFF` (Purple 80) | Основной цвет бренда |
| `onPrimary` | `#FFFFFF` | `#381E72` | Текст/иконки на primary |
| `primaryContainer` | `#E8E8F0` | `#4F378B` | Контейнер primary |
| `onPrimaryContainer` | `#1A1A2E` | `#EADDFF` | Текст на primaryContainer |

### Вторичные цвета (Secondary)

| Назначение | Light Theme | Dark Theme | Описание |
|------------|-------------|------------|----------|
| `secondary` | `#5C6BC0` (Indigo 400) | `#B2ACB2` (Grey 80) | Акцентный цвет |
| `onSecondary` | `#FFFFFF` | `#1E1E1E` | Текст/иконки на secondary |
| `secondaryContainer` | `#E8EAF6` | `#4A4458` | Контейнер secondary |
| `onSecondaryContainer` | `#1A237E` | `#E8DEF8` | Текст на secondaryContainer |

### Tertiary цвета (для карточек книг)

| Назначение | Light Theme | Dark Theme | Описание |
|------------|-------------|------------|----------|
| `tertiary` | `#5C6BC0` | `#B2ACB2` | Акцент для карточек |
| `onTertiary` | `#1A1A2E` (тёмный) | `#E6E1E5` (светлый) | **Текст на карточке книги** |
| `tertiaryContainer` | `#E8EAF6` (светлый) | `#2D2D3A` (тёмный) | **Фон карточки книги** |
| `onTertiaryContainer` | `#1A237E` | `#E6E1E5` | Текст на tertiaryContainer |

### Фоновые цвета (Surface)

| Назначение | Light Theme | Dark Theme | Описание |
|------------|-------------|------------|----------|
| `background` | `#FFFBFE` | `#1C1B1F` | Фон экрана |
| `onBackground` | `#1C1B1F` | `#E6E1E5` | Основной текст |
| `surface` | `#FFFBFE` | `#1C1B1F` | Поверхность (карточки) |
| `onSurface` | `#1C1B1F` | `#E6E1E5` | Текст на surface |
| `surfaceVariant` | `#E7E0EC` | `#49454F` | Вариант поверхности |
| `onSurfaceVariant` | `#49454F` | `#CAC4D0` | Вторичный текст |

### Функциональные цвета

| Назначение | Цвет | Описание |
|------------|------|----------|
| `error` | `#B3261E` | Ошибки, удаление (одинаково для обеих тем) |
| `onError` | `#FFFFFF` | Текст на error |
| `errorContainer` | `#F9DEDC` | Контейнер ошибки |
| `onErrorContainer` | `#410E0B` | Текст на errorContainer |
| `success` | `#2E7D32` | Успешные действия (зелёный) |
| `warning` | `#F57C00` | Предупреждения (оранжевый) |
| `info` | `#1976D2` | Информация (синий) |

### Цвета ссылок и интерактивных элементов

| Назначение | Light Theme | Dark Theme | Описание |
|------------|-------------|------------|----------|
| `linkColor` | `#1976D2` | `#64B5F6` | Цвет ссылок в тексте |
| `rippleColor` | `#1F1C1B1F` | `#1FE6E1E5` | Ripple-эффект |

### Цвета карточек с действиями

| Назначение | Light Theme | Dark Theme | Описание |
|------------|-------------|------------|----------|
| `cardStroke` | `#40000000` (тёмная) | `#40FFFFFF` (светлая) | Обводка карточки |
| `swipeDeleteBackground` | `#B3261E` | `#B3261E` | Фон при свайпе удаления |

---

## Типографика

Используется Material Typography с базовым шрифтом `Roboto`.

### Текстовые стили

| Стиль | Размер | Weight | Line Height | Использование |
|-------|--------|--------|-------------|---------------|
| `displayLarge` | 57sp | Regular | 64sp | Заголовки больших экранов |
| `displayMedium` | 45sp | Regular | 52sp | — |
| `displaySmall` | 36sp | Regular | 44sp | — |
| `headlineLarge` | 32sp | Regular | 40sp | Главные заголовки экранов |
| `headlineMedium` | 28sp | Regular | 36sp | Заголовки секций |
| `headlineSmall` | 24sp | Regular | 32sp | Подзаголовки |
| `titleLarge` | 22sp | Regular | 28sp | Заголовки карточек |
| `titleMedium` | 16sp | Medium | 24sp | Заголовки списков |
| `titleSmall` | 14sp | Medium | 20sp | Малые заголовки |
| `bodyLarge` | 16sp | Regular | 24sp | Основной текст |
| `bodyMedium` | 14sp | Regular | 20sp | Вторичный текст |
| `bodySmall` | 12sp | Regular | 16sp | Вспомогательный текст |
| `labelLarge` | 14sp | Medium | 20sp | Кнопки |
| `labelMedium` | 12sp | Medium | 16sp | Малые кнопки, chips |
| `labelSmall` | 11sp | Medium | 16sp | Метки |

### Исключения

Читалка книг (`bookreader`, `fbreader`) может использовать собственную типографику, определяемую настройками книги.

---

## Отступы (Dimensions)

### Стандартные отступы от краёв экрана

```kotlin
object AppDimensions {
    // Горизонтальные отступы
    val screenPaddingHorizontal = 16.dp  // Стандартный отступ от краёв экрана
    val screenPaddingLarge = 24.dp        // Увеличенный отступ (планшеты)
    
    // Вертикальные отступы
    val screenPaddingVertical = 16.dp
    
    // Внутренние отступы карточек
    val cardPaddingInner = 12.dp
    val cardPaddingOuter = 8.dp
    
    // Отступы между элементами
    val itemSpacingSmall = 4.dp
    val itemSpacingMedium = 8.dp
    val itemSpacingLarge = 16.dp
    val itemSpacingXLarge = 24.dp
    
    // Отступы списков
    val listItemPaddingVertical = 8.dp
    val listItemPaddingHorizontal = 16.dp
    
    // Высота компонентов
    val toolbarHeight = 56.dp
    val bottomNavHeight = 80.dp
    val buttonHeight = 48.dp
    val buttonHeightSmall = 36.dp
    val iconSizeStandard = 24.dp
    val iconSizeLarge = 32.dp
    
    // Минимальная область касания (Accessibility)
    val minTouchTarget = 48.dp
}
```

### Применение через CompositionLocal

```kotlin
val LocalAppDimensions = staticCompositionLocalOf { AppDimensions }
```

---

## Формы (Shapes)

### Радиусы скругления

```kotlin
object AppShapes {
    val cornerExtraSmall = 4.dp   // Малые элементы (chips)
    val cornerSmall = 8.dp        // Кнопки, текстовые поля
    val cornerMedium = 12.dp      // Карточки
    val cornerLarge = 16.dp       // Большие карточки, диалоги
    val cornerExtraLarge = 28.dp  // Bottom sheets
    val cornerFull = 50%          // Круглые элементы (FAB)
}
```

### Material Shapes

```kotlin
val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp)
)
```

---

## Компоненты

### AppCard

Базовая карточка для списков и контейнеров.

```kotlin
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.padding(
            horizontal = AppDimensions.screenPaddingHorizontal,
            vertical = AppDimensions.cardPaddingOuter
        ),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        onClick = onClick
    ) {
        content()
    }
}
```

### AppButton

Кнопка с поддержкой состояния загрузки.

```kotlin
@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    enabled: Boolean = true,
    style: ButtonStyle = ButtonStyle.Filled
)

enum class ButtonStyle {
    Filled,      // Основная кнопка
    Outlined,    // Вторичная кнопка
    Text,        // Текстовая кнопка
    Tonal        // Приглушённая кнопка
}
```

### AppLoadingIndicator

Индикатор загрузки.

```kotlin
@Composable
fun AppLoadingIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    color: Color = MaterialTheme.colorScheme.primary
)
```

### AppSwipeRefresh

Pull-to-refresh контейнер.

```kotlin
@Composable
fun AppSwipeRefresh(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
)
```

### AppErrorView

Экран/вид ошибки с кнопкой повтора.

```kotlin
@Composable
fun AppErrorView(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    retryText: String = "Повторить"
)
```

### AppAlertDialog

Диалоговое окно.

```kotlin
@Composable
fun AppAlertDialog(
    title: String,
    text: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    confirmText: String = "OK",
    dismissText: String? = "Отмена"
)
```

### AppEmptyState

Состояние пустого списка.

```kotlin
@Composable
fun AppEmptyState(
    title: String,
    description: String? = null,
    icon: ImageVector? = null,
    action: @Composable (() -> Unit)? = null
)
```

### AppToolbar

Тулбар с поддержкой иконок темы.

```kotlin
@Composable
fun AppToolbar(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
)
```

---

## Паттерны

### Списки

```kotlin
// Стандартный список
LazyColumn(
    contentPadding = PaddingValues(
        vertical = AppDimensions.itemSpacingMedium
    ),
    verticalArrangement = Arrangement.spacedBy(
        AppDimensions.itemSpacingMedium
    )
) {
    items(items) { item ->
        AppCard(onClick = { /* навигация */ }) {
            // Контент элемента
        }
    }
}
```

### Состояния экрана

Все экраны должны обрабатывать три состояния:
1. **Loading** — `AppLoadingIndicator()` или `FullScreenLoading()`
2. **Error** — `AppErrorView()` или `FullScreenError()`
3. **Content** — основной контент

```kotlin
when (state) {
    is State.Loading -> FullScreenLoading()
    is State.Error -> FullScreenError(onRetry = viewModel::retry)
    is State.Content -> ContentList(items)
    is State.Empty -> EmptyState(title = "Нет данных")
}
```

### SwipeRefresh

```kotlin
PullToRefreshBox(
    isRefreshing = isRefreshing,
    onRefresh = viewModel::refresh
) {
    LazyColumn { /* ... */ }
}
```

### Ripple-эффект

Все кликабельные элементы должны использовать ripple:

```kotlin
Modifier.clickable(
    interactionSource = remember { MutableInteractionSource() },
    indication = ripple()
) { onClick() }
```

### Иконки в тулбаре

Иконки должны менять цвет в зависимости от темы:

```kotlin
Icon(
    imageVector = icon,
    contentDescription = null,
    tint = MaterialTheme.colorScheme.onSurface
)
```

---

## Переключение темы

### ThemeConfig

```kotlin
data class ThemeConfig(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val useDynamicColors: Boolean = false
)

enum class ThemeMode {
    LIGHT,   // Светлая тема
    DARK,    // Тёмная тема
    SYSTEM   // Системная тема
}
```

### Сохранение настроек

Настройки темы сохраняются в `DataStore`:

```kotlin
interface ThemeRepository {
    val themeMode: Flow<ThemeMode>
    suspend fun setThemeMode(mode: ThemeMode)
}
```

### Иконка переключения темы

```kotlin
@Composable
fun ThemeToggleIcon(
    currentMode: ThemeMode,
    onToggle: (ThemeMode) -> Unit
) {
    val icon = when (currentMode) {
        ThemeMode.LIGHT -> Icons.R.LightMode
        ThemeMode.DARK -> Icons.R.DarkMode
        ThemeMode.SYSTEM -> Icons.R.BrightnessAuto
    }
    
    IconButton(onClick = {
        val nextMode = when (currentMode) {
            ThemeMode.LIGHT -> ThemeMode.DARK
            ThemeMode.DARK -> ThemeMode.SYSTEM
            ThemeMode.SYSTEM -> ThemeMode.LIGHT
        }
        onToggle(nextMode)
    }) {
        Icon(
            imageVector = icon,
            contentDescription = "Сменить тему",
            tint = MaterialTheme.colorScheme.onSurface
        )
    }
}
```

---

## Рекомендации по созданию новых экранов

### Чек-лист

1. ✅ Обернуть контент в `SVAPPTheme`
2. ✅ Использовать `AppDimensions` для отступов
3. ✅ Использовать `MaterialTheme.colorScheme` для цветов
4. ✅ Использовать `MaterialTheme.typography` для текста
5. ✅ Обработать все состояния (Loading, Error, Content, Empty)
6. ✅ Использовать общие компоненты из `commonui/ui`
7. ✅ Добавить PullToRefresh для списков
8. ✅ Использовать ripple для кликабельных элементов

### Структура экрана

```kotlin
@Composable
fun NewScreen(
    viewModel: NewViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    
    SVAPPTheme {
        Scaffold(
            topBar = {
                AppToolbar(
                    title = "Заголовок",
                    actions = {
                        ThemeToggleIcon(...)
                    }
                )
            }
        ) { padding ->
            when (state) {
                is State.Loading -> FullScreenLoading()
                is State.Error -> FullScreenError { viewModel.retry() }
                is State.Content -> ContentList(state.items)
                is State.Empty -> EmptyState("Нет данных")
            }
        }
    }
}
```

---

## Файлы для создания/изменения

### Новые файлы

| Файл | Описание |
|------|----------|
| `theme/Dimensions.kt` | Отступы и размеры |
| `theme/Shape.kt` | Формы (радиусы) |
| `theme/ThemeConfig.kt` | Конфигурация темы |
| `ui/components/AppCard.kt` | Карточка |
| `ui/components/AppButton.kt` | Кнопки |
| `ui/components/AppLoading.kt` | Индикаторы загрузки |
| `ui/components/AppError.kt` | Экраны ошибок |
| `ui/components/AppEmptyState.kt` | Пустые состояния |
| `ui/components/AppToolbar.kt` | Тулбар |
| `ui/components/ThemeToggleIcon.kt` | Иконка смены темы |
| `managers/ThemeRepository.kt` | Хранение настроек темы |

### Изменяемые файлы

| Файл | Изменения |
|------|-----------|
| `theme/Color.kt` | Расширить палитру |
| `theme/Theme.kt` | Добавить ThemeConfig, CompositionLocal |
| `theme/Type.kt` | Полная типографика |
| `RootNews.kt` | Добавить тулбар с иконкой темы |

---

## Цветовые схемы (пример кода)

```kotlin
// Color.kt

// Light Theme Colors
val LightPrimary = Color(0xFF3F51B5)
val LightOnPrimary = Color(0xFFFFFFFF)
val LightPrimaryContainer = Color(0xFFE8E8F0)
val LightOnPrimaryContainer = Color(0xFF1A1A2E)

val LightSecondary = Color(0xFF5C6BC0)
val LightOnSecondary = Color(0xFFFFFFFF)

val LightBackground = Color(0xFFFFFBFE)
val LightOnBackground = Color(0xFF1C1B1F)
val LightSurface = Color(0xFFFFFBFE)
val LightOnSurface = Color(0xFF1C1B1F)

val LightError = Color(0xFFB3261E)
val LightOnError = Color(0xFFFFFFFF)

// Dark Theme Colors
val DarkPrimary = Color(0xFFD0BCFF)
val DarkOnPrimary = Color(0xFF381E72)
val DarkPrimaryContainer = Color(0xFF4F378B)
val DarkOnPrimaryContainer = Color(0xFFEADDFF)

val DarkSecondary = Color(0xFFB2ACB2)
val DarkOnSecondary = Color(0xFF1E1E1E)

val DarkBackground = Color(0xFF1C1B1F)
val DarkOnBackground = Color(0xFFE6E1E5)
val DarkSurface = Color(0xFF1C1B1F)
val DarkOnSurface = Color(0xFFE6E1E5)

val DarkError = Color(0xFFB3261E)  // Same as light for functional colors
val DarkOnError = Color(0xFFFFFFFF)

// Functional Colors (same for both themes)
val FunctionalSuccess = Color(0xFF2E7D32)
val FunctionalWarning = Color(0xFFF57C00)
val FunctionalInfo = Color(0xFF1976D2)

// Link Colors
val LightLinkColor = Color(0xFF3C92DE)
val DarkLinkColor = Color(0xFF64B5F6)
```
