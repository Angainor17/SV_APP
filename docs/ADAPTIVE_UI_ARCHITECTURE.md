# Адаптивный UI для планшетов и foldables

## Обзор

Документ описывает архитектуру адаптивного UI в SV APP, который автоматически подстраивается под размер экрана:
- **Compact** (< 600dp): Телефоны — BottomNavigation
- **Medium** (600-840dp): Планшеты портрет, foldables сложенные — NavigationRail
- **Expanded** (≥ 840dp): Планшеты ландшафт, foldables разложенные — NavigationRail + Master-Detail

---

## Архитектурный принцип: Separate Components, Not Branching

### Ключевая идея

Не использовать ветвление внутри Composable (`if (isTablet) { ... } else { ... }`), а создавать **отдельные компоненты** для каждого форм-фактора.

### ❌ Плохо (ветвление внутри):

```kotlin
@Composable
fun MyScreen() {
    val isTablet = LocalDeviceFormFactor.current.isExpanded()
    
    if (isTablet) {
        // Tablet layout
        Row { ... }
    } else {
        // Phone layout
        Column { ... }
    }
}
```

### ✅ Хорошо (отдельные компоненты):

```kotlin
@Composable
fun MyScreen() {
    val formFactor = LocalDeviceFormFactor.current
    
    when (formFactor) {
        is DeviceFormFactor.Compact -> CompactMyScreen()
        is DeviceFormFactor.Medium -> MediumMyScreen()
        is DeviceFormFactor.Expanded -> ExpandedMyScreen()
    }
}
```

---

## Form Factors

| Form Factor | Условие | Устройства | Навигация | Grid | Особенности |
|-------------|---------|------------|-----------|------|-------------|
| **Compact** | < 600dp | Телефоны | BottomNavigation | 2 колонки | Без изменений |
| **Medium** | 600-840dp | Планшеты портрет, foldables сложенные | NavigationRail | 3 колонки | maxWidth = 600dp |
| **Expanded** | ≥ 840dp | Планшеты ландшафт, foldables разложенные | NavigationRail | 4 колонки | Master-Detail |

---

## Структура файлов

```
commonui/src/main/java/su/sv/commonui/
├── theme/
│   ├── DeviceFormFactor.kt      # Sealed class для Compact/Medium/Expanded
│   ├── AdaptiveDimensions.kt    # Размеры по форм-фактору
│   └── LocalDeviceFormFactor.kt # CompositionLocal providers
├── util/
│   └── WindowSizeClassProvider.kt # Определение форм-фактора
└── ui/adaptive/
    ├── navigation/
    │   ├── AdaptiveNavigation.kt   # Главный компонент (селектор)
    │   ├── CompactNavigation.kt     # BottomNavigation
    │   └── RailNavigation.kt        # NavigationRail
    ├── grid/
    │   └── AdaptiveGridConfig.kt    # Адаптивные колонки
    └── layout/
        ├── AdaptiveScaffold.kt      # Scaffold с адаптивной навигацией
        ├── AdaptiveContentLayout.kt # Ограничение ширины
        └── MasterDetailLayout.kt    # Two-pane layout
```

---

## Использование

### 1. Получение форм-фактора

```kotlin
@Composable
fun MyScreen() {
    val formFactor = LocalDeviceFormFactor.current
    
    if (formFactor.shouldUseNavigationRail()) {
        // Показать NavigationRail
    }
}
```

### 2. Адаптивные размеры

```kotlin
@Composable
fun BookList() {
    val adaptiveDims = LocalAdaptiveDimensions.current
    
    LazyVerticalGrid(
        columns = GridCells.Fixed(adaptiveDims.gridColumns),
        contentPadding = PaddingValues(
            start = adaptiveDims.screenPadding / 2,
        ),
    ) { ... }
}
```

### 3. Предоставление через CompositionLocal

```kotlin
@Composable
fun BottomNavigationBar() {
    ProvideAdaptiveDimensions {
        // Внутри доступны LocalDeviceFormFactor и LocalAdaptiveDimensions
        AdaptiveNavigation(...)
    }
}
```

---

## Создание адаптивного компонента

### Шаг 1: Создать интерфейс/sealed class

```kotlin
sealed class DeviceFormFactor {
    data object Compact : DeviceFormFactor()
    data object Medium : DeviceFormFactor()
    data object Expanded : DeviceFormFactor()
}
```

### Шаг 2: Создать отдельные Composable

```kotlin
@Composable
fun CompactMyComponent() {
    // Реализация для телефонов
}

@Composable
fun MediumMyComponent() {
    // Реализация для планшетов портрет
}

@Composable
fun ExpandedMyComponent() {
    // Реализация для планшетов ландшафт
}
```

### Шаг 3: Создать адаптивный компонент

```kotlin
@Composable
fun AdaptiveMyComponent() {
    when (LocalDeviceFormFactor.current) {
        is DeviceFormFactor.Compact -> CompactMyComponent()
        is DeviceFormFactor.Medium -> MediumMyComponent()
        is DeviceFormFactor.Expanded -> ExpandedMyComponent()
    }
}
```

---

## Master-Detail Layout

Для планшетов в ландшафте (Expanded) используется двухпанельный layout:

```
┌────┬───────────────────────────────────────────────────────┐
│ 🏠 │ ┌─────────────┐ ┌─────────────────────────────────┐   │
│ 📚 │ │   Список    │ │         Детали                   │   │
│ 🔍 │ │   (35%)     │ │           (65%)                  │   │
│ ℹ️ │ └─────────────┘ └─────────────────────────────────┘   │
└────┴───────────────────────────────────────────────────────┘
```

### Использование:

```kotlin
@Composable
fun BooksScreen() {
    MasterDetailLayout(
        master = { BookList(...) },
        detail = { BookDetail(...) },
    )
}
```

---

## Foldables

Складные устройства автоматически поддерживаются через WindowSizeClass:

- **Сложенный** → Medium (NavigationRail, 3 колонки)
- **Разложенный** → Expanded (NavigationRail + Master-Detail)

Для дополнительной работы с hinge (линия сгиба) используйте Jetpack WindowManager:

```kotlin
// Зависимость
implementation("androidx.window:window-core:1.3.0")
```

---

## Тестирование

### Preview для разных размеров

```kotlin
@Preview(device = "spec:width=360dp,height=800dp")  // Phone
@Preview(device = "spec:width=600dp,height=800dp")  // Tablet portrait
@Preview(device = "spec:width=1280dp,height=800dp") // Tablet landscape
@Composable
fun MyScreenPreview() { ... }
```

### Эмуляторы для тестирования

| Размер | AVD Profile |
|--------|-------------|
| Compact | Phone (5.5") |
| Medium | Tablet (7") |
| Expanded | Tablet (10") landscape |

---

## Миграция существующих экранов

1. **Обернуть в ProvideAdaptiveDimensions** в точке входа (уже сделано в BottomNavigationBar)
2. **Заменить GridCells.Fixed(2)** на `GridCells.Fixed(adaptiveDims.gridColumns)`
3. **Добавить maxWidth** для контента: `Modifier.widthIn(max = adaptiveDims.contentMaxWidth)`
4. **Создать Master-Detail** для Expanded если применимо

---

## Связанные файлы

- [Design System](DESIGN_SYSTEM.md) — дизайн-система приложения
- [Memory: Tablet Optimization Plan](../.claude/projects/-Users-i-u-voronin-StudioProjects-SV-APP/memory/tablet-optimization-plan.md)

---

## Ссылки

- [Large screens guide](https://developer.android.com/guide/topics/large-screens/support-large-screens)
- [WindowManager](https://developer.android.com/jetpack/androidx/releases/window)
- [Material3 Adaptive](https://developer.android.com/develop/ui/compose/layouts/adaptive)