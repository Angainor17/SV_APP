---
name: design-system
description: Дизайн-система SV APP на Jetpack Compose
metadata: 
  node_type: memory
  type: project
  originSessionId: 10a64a98-46f7-42a9-b51f-46c32d00924e
---

# Дизайн-система SV APP

## Статус миграции

✅ Все экраны мигрированы на дизайн-систему (2026-06-25):
- News — завершено
- Books — завершено
- Wiki — завершено
- Info — завершено
- BookReader — завершено

## Структура

Дизайн-система реализована в модуле `commonui/src/main/java/su/sv/commonui/`:

### Тема (`theme/`)
- `Color.kt` — полная цветовая палитра для светлой/тёмной темы
- `Dimensions.kt` — отступы, размеры, доступ через `LocalAppDimensions`
- `Shape.kt` — формы (радиусы скругления), доступ через `LocalAppShapes`
- `Type.kt` — типографика Material 3
- `Theme.kt` — `SVAPPTheme` с поддержкой `ThemeMode` (LIGHT/DARK/SYSTEM)
- `ThemeConfig.kt` — конфигурация темы, `ThemeRepository` интерфейс

### Компоненты (`ui/components/`)
- `AppCard.kt` — базовая карточка с цветами из темы
- `AppButton.kt` — кнопки (FILLED/OUTLINED/TEXT/TONAL)
- `AppLoadingIndicator.kt` — индикатор загрузки
- `AppStates.kt` — FullScreenLoading, FullScreenError, FullScreenEmpty
- `AppDialog.kt` — диалоговые окна
- `AppSwipeRefresh.kt` — pull-to-refresh
- `AppToolbar.kt` — тулбар с поддержкой темы
- `ThemeToggleIcon.kt` — иконка переключения темы

## Применение темы на Modo Screen

```kotlin
@Composable
override fun Content(modifier: Modifier) {
    val themeViewModel: ThemeViewModel = hiltViewModel()
    val themeConfig by themeViewModel.themeConfig.collectAsStateWithLifecycle()

    SVAPPTheme(
        themeMode = themeConfig.themeMode,
        useDynamicColors = themeConfig.useDynamicColors
    ) {
        Scaffold(
            contentWindowInsets = WindowInsets.statusBars,
            // ...
        ) { padding ->
            // Контент
        }
    }
}
```

## Хранение настроек темы

- `ThemeRepositoryImpl` в модуле `managers/theme/`
- Использует DataStore для персистентного хранения
- `ThemeViewModel` для управления темой на уровне приложения

## Документация

- `docs/DESIGN_SYSTEM.md` — полное описание дизайн-системы
- `docs/MIGRATION_PLAN.md` — план миграции экранов (этап 3 завершён)

## Почему

Единая дизайн-система обеспечивает:
- Цельный внешний вид приложения
- Лёгкое изменение темы в одном месте
- Поддержку светлой/тёмной темы
- Переиспользование компонентов

## Как применять

При создании новых экранов:
1. Использовать цвета из `MaterialTheme.colorScheme`
2. Использовать отступы из `LocalAppDimensions.current`
3. Использовать типографику из `MaterialTheme.typography`
4. Использовать общие компоненты из `ui/components/`
5. Обрабатывать все состояния (Loading, Error, Content, Empty)
6. Добавить `contentWindowInsets = WindowInsets.statusBars` в Scaffold

Связанные документы: [[DESIGN_SYSTEM]], [[MIGRATION_PLAN]]
