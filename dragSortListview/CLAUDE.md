# DragSortListView Module

ListView с поддержкой drag & drop.

## Обзор

Модуль `dragSortListview` предоставляет кастомный ListView с возможностью перетаскивания элементов. Изначально был библиотекой mobeta.

## Основные компоненты

### DragSortController
Контроллер для управления перетаскиванием:

```kotlin
class DragSortController(
    private val listView: DragSortListView
) {
    // Управление режимом перетаскивания
    // Обработка touch-событий
}
```

### DragSortItemView
Кастомная View для элемента списка:

```kotlin
class DragSortItemView(context: Context) : ViewGroup
```

### DragSortItemViewCheckable
Элемент списка с поддержкой Checked:

```kotlin
class DragSortItemViewCheckable(context: Context) : DragSortItemView
```

### Адаптеры

#### DragSortCursorAdapter
Адаптер для работы с Cursor:

```kotlin
abstract class DragSortCursorAdapter : CursorAdapter
```

#### ResourceDragSortCursorAdapter
Адаптер с поддержкой ресурсов layout:

```kotlin
class ResourceDragSortCursorAdapter : DragSortCursorAdapter
```

#### SimpleDragSortCursorAdapter
Упрощённый адаптер:

```kotlin
class SimpleDragSortCursorAdapter : ResourceDragSortCursorAdapter
```

### SimpleFloatViewManager
Менеджер для "плавающего" вида при перетаскивании:

```kotlin
class SimpleFloatViewManager(context: Context) : FloatViewManager
```

## Использование

```kotlin
val listView = DragSortListView(context, attrs)
val controller = DragSortController(listView).apply {
    setRemoveEnabled(true)
    setSortEnabled(true)
}

listView.setFloatViewManager(controller)
listView.setOnTouchListener(controller)
```

## Структура файлов

```
dragSortListview/src/main/java/com/mobeta/android/dslv/
├── DragSortController.kt
├── DragSortItemView.kt
├── DragSortItemViewCheckable.kt
├── DragSortCursorAdapter.kt
├── ResourceDragSortCursorAdapter.kt
├── SimpleDragSortCursorAdapter.kt
└── SimpleFloatViewManager.kt
```

## Тесты

Модуль содержит unit-тесты:

```
dragSortListview/src/test/java/com/mobeta/android/dslv/
├── DragSortCursorAdapterTest.kt
├── DragSortControllerTest.kt
└── DragSortItemViewTest.kt
```
