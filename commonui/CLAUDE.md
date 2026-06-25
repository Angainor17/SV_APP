# CommonUI Module

Общие UI компоненты и тема приложения.

## Обзор

Модуль `commonui` содержит переиспользуемые UI компоненты, тему, расширения и менеджеры для всего приложения.

---

## Дизайн-система

Полное описание дизайн-системы находится в `docs/DESIGN_SYSTEM.md`.

### Структура темы

```
commonui/src/main/java/su/sv/commonui/theme/
├── Color.kt           # Цветовая палитра
├── Theme.kt           # Главная тема (SVAPPTheme)
├── Type.kt            # Типографика
├── Shape.kt           # Формы (радиусы скругления)
├── Dimensions.kt      # Отступы и размеры
└── ThemeConfig.kt     # Конфигурация темы (режим, настройки)
```

### SVAPPTheme

Главная тема приложения с поддержкой светлой/тёмной темы:

```kotlin
SVAPPTheme(
    themeMode = ThemeMode.SYSTEM,  // LIGHT, DARK, SYSTEM
    useDynamicColors = false       // Material You dynamic colors
) {
    // Контент
}
```

### ThemeConfig

```kotlin
data class ThemeConfig(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val useDynamicColors: Boolean = false
)

enum class ThemeMode {
    LIGHT, DARK, SYSTEM
}
```

### AppDimensions

Отступы и размеры через CompositionLocal:

```kotlin
val dimensions = LocalAppDimensions.current

// Использование
Modifier.padding(dimensions.screenPaddingHorizontal)
```

---

## UI Компоненты

### AppCard

Базовая карточка для списков и контейнеров:

```kotlin
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
)
```

### AppButton

Кнопка с поддержкой состояния загрузки:

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
    Filled, Outlined, Text, Tonal
}
```

### AppLoadingIndicator

Индикатор загрузки:

```kotlin
@Composable
fun AppLoadingIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    color: Color = MaterialTheme.colorScheme.primary
)
```

### AppSwipeRefresh

Pull-to-refresh контейнер:

```kotlin
@Composable
fun AppSwipeRefresh(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
)
```

### AppErrorView / FullScreenError

Экран/вид ошибки с кнопкой повтора:

```kotlin
@Composable
fun FullScreenError(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    retryText: String = "Повторить"
)
```

### AppEmptyState / FullScreenEmpty

Состояние пустого списка:

```kotlin
@Composable
fun AppEmptyState(
    title: String,
    description: String? = null,
    icon: ImageVector? = null,
    action: @Composable (() -> Unit)? = null
)
```

### AppAlertDialog

Диалоговое окно:

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

### AppToolbar

Тулбар с поддержкой иконок темы:

```kotlin
@Composable
fun AppToolbar(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
)

@Composable
fun AppToolbarWithBack(
    title: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {}
)
```

### ThemeToggleIcon

Иконка переключения темы:

```kotlin
@Composable
fun ThemeToggleIcon(
    currentMode: ThemeMode,
    onToggle: (ThemeMode) -> Unit
)
```

### FullScreenLoading

Полноэкранный индикатор загрузки.

### LoadingIndicator

Индикатор загрузки (прогресс бар).

### ButtonWithLoader

Кнопка с индикатором загрузки:

```kotlin
ButtonWithLoader(
    text = "Скачать",
    isLoading = false,
    onClick = { }
)
```

### ExpandingText

Раскрывающийся текст с ограничением по высоте.

### LoadableResultDialog

Диалог с поддержкой состояний загрузки/ошибки.

### OneTimeEffect

Обработка side-эффектов в Compose:

```kotlin
OneTimeEffect(oneTimeEffectFlow) { effect ->
    // Обработка эффекта
}
```

### ShimmerBrush

Shimmer-эффект для плейсхолдеров загрузки.

---

## Linkify

Модуль для отображения текста с кликабельными ссылками:

- `LinkifyContent` — компонент с поддержкой ссылок
- `LinkifyTextKt` — текст с авто-линковкой
- `LinkMatcher` — поиск ссылок в тексте
- `LinkifyContentDefaults` — настройки по умолчанию

---

## Менеджеры

### ResourcesRepository

Доступ к ресурсам приложения:

```kotlin
class ResourcesRepository @Inject constructor(context: Context) {
    fun getString(resId: Int): String
    fun getString(resId: Int, vararg args: Any): String
}
```

### DateFormatter

Форматирование дат:

```kotlin
class DateFormatter @Inject constructor() {
    fun formatDate(timestamp: Long): String
}
```

---

## Расширения

### IntExt

Расширения для Int (dp в px и т.д.).

### LongExt

Расширения для Long (форматирование времени и т.д.).

---

## Структура файлов

```
commonui/src/main/java/su/sv/commonui/
├── ext/
│   ├── IntExt.kt
│   └── LongExt.kt
├── managers/
│   ├── DateFormatter.kt
│   └── ResourcesRepository.kt
├── theme/
│   ├── Color.kt
│   ├── Dimensions.kt
│   ├── Shape.kt
│   ├── Theme.kt
│   ├── ThemeConfig.kt
│   └── Type.kt
└── ui/
    ├── components/
    │   ├── AppButton.kt
    │   ├── AppCard.kt
    │   ├── AppDialog.kt
    │   ├── AppLoadingIndicator.kt
    │   ├── AppStates.kt
    │   ├── AppSwipeRefresh.kt
    │   ├── AppToolbar.kt
    │   └── ThemeToggleIcon.kt
    ├── ButtonWithLoader.kt
    ├── ExpandingText.kt
    ├── FullScreenError.kt
    ├── FullScreenLoading.kt
    ├── LoadableResultDialog.kt
    ├── LoadingIndicator.kt
    ├── OneTimeEffect.kt
    ├── shimmerBrush.kt
    └── linkify/
        ├── LinkifyContent.kt
        ├── LinkifyContentDefaults.kt
        ├── LinkifyTextKt.kt
        └── LinkMatcher.kt
```
