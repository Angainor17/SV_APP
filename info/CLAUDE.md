# Info Module

Модуль информационного экрана.

## Обзор

Модуль `info` отображает информационный экран с ссылками на ресурсы (Telegram, сайт и т.д.).

## Архитектура

```
info/
├── domain/
│   ├── GetInfoLinksUseCase.kt   # Use case для получения ссылок
│   └── model/LinkItem.kt        # Модель ссылки
└── rootinfo/
    ├── RootInfoViewModel.kt     # ViewModel
    ├── RootInfo.kt              # Главный экран
    ├── ui/
    │   ├── InfoContent.kt       # Контент
    │   └── InfoItem.kt          # Элемент списка
    ├── mapper/InfoUiMapper.kt   # Маппер
    ├── model/
    │   ├── UiInfoState.kt       # Состояние UI
    │   └── UiLinkItem.kt        # Модель для UI
    └── viewmodel/
        ├── RootInfoActions.kt
        └── RootInfoActionsHandler.kt
```

## Основные компоненты

### RootInfoViewModel
ViewModel информационного экрана:

```kotlin
class RootInfoViewModel : BaseViewModel() {
    val state: StateFlow<UiInfoState>
    fun onAction(action: RootInfoActions)
}
```

### GetInfoLinksUseCase
Use case для получения списка ссылок:

```kotlin
class GetInfoLinksUseCase @Inject constructor() {
    operator fun invoke(): List<LinkItem>
}
```

### UiLinkItem
Модель элемента для UI:

```kotlin
data class UiLinkItem(
    val title: String,
    val description: String,
    val url: String,
    val icon: Int  // Drawable resource
)
```

### UiInfoState
Состояние экрана:

```kotlin
sealed class UiInfoState {
    object Loading : UiInfoState()
    data class Content(val items: List<UiLinkItem>) : UiInfoState()
    data class Error(val message: String) : UiInfoState()
}
```

## Actions

```kotlin
sealed class RootInfoActions {
    data class OpenLink(val url: String) : RootInfoActions()
    data class ShareApp(val title: String) : RootInfoActions()
}
```

## Структура файлов

```
info/src/main/java/su/sv/info/
├── domain/
│   ├── GetInfoLinksUseCase.kt
│   └── model/LinkItem.kt
└── rootinfo/
    ├── RootInfoViewModel.kt
    ├── RootInfo.kt
    ├── ui/
    │   ├── InfoContent.kt
    │   └── InfoItem.kt
    ├── mapper/InfoUiMapper.kt
    ├── model/
    │   ├── UiInfoState.kt
    │   └── UiLinkItem.kt
    └── viewmodel/
        ├── RootInfoActions.kt
        └── RootInfoActionsHandler.kt
```
