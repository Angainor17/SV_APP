---
name: selection-view-architecture
description: Архитектура SelectionView для выделения текста в PDF
metadata: 
  node_type: memory
  type: project
  originSessionId: 475fe7af-7539-4b95-8265-35e25b837b07
---

# SelectionView Architecture

## Обзор

SelectionView отвечает за отображение и управление выделением текста в PDF документах.

## Структура классов

```
SelectionView (FrameLayout) - bookreader/widgets/SelectionView.kt
├── PageView[] - прямоугольники выделенного текста на каждой странице
├── HandleRect startRect - левый/верхний маркер (ползунок)
└── HandleRect endRect - правый/нижний маркер (ползунок)
└── HotRect - Rect с "hot point" для касания
└── HotPoint - Point с offset для drag
```

## Ключевые компоненты

| Класс | Файл | Функция |
|-------|------|---------|
| **SelectionView** | SelectionView.kt | Container, рисует маркеры через drawHandle(), обрабатывает touch events |
| **PageView** | SelectionView.kt:605 | Рисует выделенные прямоугольники текста на странице |
| **HandleRect** | SelectionView.kt:567 | Маркер для тяги, содержит which (Left/Right) и setter |
| **HotRect** | SelectionView.kt:444 | Rect с hot point - точкой касания внутри маркера |
| **HotPoint** | SelectionView.kt:528 | Point с offset для drag operations |

## Touch Handling Flow

```kotlin
// SelectionView.onTouchEvent() строки 336-363

1. Пользователь касается маркера (ACTION_DOWN)
   → startRect.onTouchEvent() или endRect.onTouchEvent() возвращает true
   
2. onTouchLock() вызывается
   → FBReaderView: SELECTION_HIDE_PANEL (панель действий скрывается)
   
3. Координаты корректируются с offset
   → x += touch.offx, yAdj = y + touch.offy
   
4. setter.setStart(x, y) или setter.setEnd(x, y)
   → PDFPlugin.Selection.setStart() / setEnd()
   → Обновляет startPage/endPage и их индексы
   
5. startRect.onTouchRelease(event) при ACTION_UP
   → touch = null
   
6. onTouchUnlock() вызывается
   → FBReaderView: SELECTION_SHOW_PANEL (панель действий показывается)
```

## PDFPlugin.Selection

**Файл:** bookreader/app/PDFPlugin.kt:188-393

```kotlin
class Selection : Plugin.View.Selection {
    var startPage: SelectionPage?  // Страница начала выделения
    var endPage: SelectionPage?    // Страница конца выделения
    
    fun setStart(page: Page, point: Point)  // Установка начала
    fun setEnd(page: Page, point: Point)    // Установка конца
    fun getBounds(page: Page): Bounds       // Получение прямоугольников
    fun isSelected(page: Int): Boolean      // Проверка выделения страницы
    fun close()                             // Закрытие и очистка
}
```

## SelectionBounds

**Файл:** PDFPlugin.kt:408-474

Вычисляет границы выделения для конкретной страницы:
- ss - start index на странице
- ee - end index на странице
- cc - count символов
- reverse - если выделение в обратном направлении

## Взаимодействие с FBReaderView

FBReaderView управляет lifecycle SelectionView:

```java
// FBReaderView.java
selectionOpen(Plugin.View.Selection s) {
    selectionCloseInternal();           // Закрыть предыдущий без уведомления
    selection = new SelectionView(...); // Создать новый
    addView(selection);                 // Добавить в hierarchy
    updateOverlays();                   // Обновить PageViews
    SELECTION_SHOW_PANEL;               // Показать панель действий
}

selectionClose() {
    selectionCloseInternal();           // Очистить SelectionView
    SELECTION_HIDE_PANEL;               // Скрыть панель действий
}
```

## ScrollWidget Integration

ScrollWidget обновляет SelectionView при scroll:

```java
// ScrollWidget.java
void selectionUpdate(PageView view) {
    // Проверяет isSelected(page.page)
    // Создает/обновляет view.selection (SelectionView.PageView)
}

void selectionClose() {
    // Удаляет view.selection из всех PageHolders
}
```

## Bounds Flow

```
1. setStart/setEnd вызываются из touch handler
2. startPage/endPage обновляются
3. SelectionView.update() вызывается
4. Для каждого PageView: PageView.update()
5. PDFPlugin.Selection.getBounds(page) вызывается
6. SelectionBounds вычисляет ss, ee, cc
7. page.text.getBounds(ss, cc) возвращает Rect[]
8. Rect[] конвертируется в device coordinates
9. PageView.onDraw() рисует прямоугольники
```

## Маркеры (Handles)

Рисуются через статические методы:

```kotlin
// SelectionView.kt:77-92
fun drawHandle(canvas, which, x, y, paint) {
    // Left: прямоугольник + круг сверху
    // Right: прямоугольник + круг снизу
}

fun rectHandle(which, x, y): HotRect {
    // Вычисляет Rect для маркера с hot point
}
```

## Known Issues

1. **Race condition** - setWidget может вызвать selectionClose во время drag
   - Решение: null safety в SelectionBounds constructor

2. **Debounce** - hide/show panel могут приходить быстро
   - Решение: debounce в ReaderViewModel.hideSelection()

## Связанные файлы

- [[selection-panel-refactoring]] - Рефакторинг панели действий
- [[fbreader-refactoring-plan]] - План рефакторинга FBReader