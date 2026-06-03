# CommonUI Module

Общие UI компоненты и тема приложения.

## Обзор

Модуль `commonui` содержит переиспользуемые UI компоненты, тему, расширения и менеджеры для всего приложения.

## UI Компоненты

### FullScreenLoading
Полноэкранный индикатор загрузки.

### FullScreenError
Полноэкранный экран ошибки с кнопкой повтора.

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
LaunchedEffect(key) {
    oneTimeEffect { effect ->
        // Обработка эффекта
    }
}
```

### ShimmerBrush
Shimmer-эффект для плейсхолдеров загрузки.

## Linkify

Модуль для отображения текста с кликабельными ссылками:

- `LinkifyContent` — компонент с поддержкой ссылок
- `LinkifyTextKt` — текст с авто-линковкой
- `LinkMatcher` — поиск ссылок в тексте
- `LinkifyContentDefaults` — настройки по умолчанию

## Тема (Theme)

### Color
Цветовая палитра приложения в `theme/Color.kt`.

### Theme
Тема приложения в `theme/Theme.kt`:

```kotlin
SVAppTheme {
    // Контент
}
```

### Type
Типографика в `theme/Type.kt`.

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

## Расширения

### IntExt
Расширения для Int (dp в px и т.д.).

### LongExt
Расширения для Long (форматирование времени и т.д.).

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
│   ├── Theme.kt
│   └── Type.kt
└── ui/
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
