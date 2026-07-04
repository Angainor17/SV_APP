---
name: selection-panel-refactoring
description: Рефакторинг панели выделения текста - миграция на Compose и исправление мерцания
metadata: 
  node_type: memory
  type: project
  originSessionId: 30672cea-7530-4511-b185-55ca2da252c7
---

# Рефакторинг панели выделения текста (2026-07-02)

## Проблема

При выделении текста панель с действиями (bookmark, translate, share, copy, question, alert) мерцала и пропадала вместе с выделением.

## Причина мерцания

Мерцание происходило из-за лишних вызовов `SELECTION_HIDE_PANEL` перед созданием выделения:

### FBView.onFingerLongPress (строка 251)

1. `SELECTION_HIDE_PANEL` вызывался ПЕРЕД `initSelection`
2. Панель скрывалась, затем создавалось выделение
3. При `ACTION_UP` → `releaseSelectionCursor` → `SELECTION_SHOW_PANEL`
4. Панель показывалась снова → мерцание!

### PDF файлы (FBReaderView.selectionOpen)

1. `selectionOpen()` вызывал `selectionClose()` → `SELECTION_HIDE_PANEL`
2. Затем `SELECTION_SHOW_PANEL` (строка 1196)
3. Панель скрывалась и показывалась подряд → мерцание

### ScrollWidget.onLongPress

1. `onLongPress()` вызывал `onFingerReleaseAfterLongPress()` сразу после `onFingerLongPress()`
2. При реальном `ACTION_UP` → `onReleaseCheck()` → `onFingerRelease()` → `SELECTION_SHOW_PANEL` снова
3. Двойной показ панели

## Решение

### 1. FBView.onFingerLongPress() - КЛЮЧЕВОЕ ИСПРАВЛЕНИЕ

Убран вызов `SELECTION_HIDE_PANEL`. Панель должна скрываться только при нажатии на маркер (через `onFingerPress`):

```java
case startSelecting:
    // SELECTION_HIDE_PANEL не вызываем - панель должна оставаться видимой
    // При нажатии на маркер для движения, SELECTION_HIDE_PANEL вызывается в onFingerPress
    initSelection(x, y);
    ...
```

### 2. ReaderViewModel.showSelection()

Добавлена проверка на повторный вызов с теми же координатами:

```kotlin
private fun showSelection(startY: Int, endY: Int) {
    val currentState = _state.value as? ReaderState.Content ?: return
    if (currentState.showSelection &&
        currentState.selectionStartY == startY &&
        currentState.selectionEndY == endY) {
        return // избегаем лишних обновлений state
    }
    _state.value = currentState.copy(showSelection = true, ...)
}
```

### 3. FBReaderView.selectionOpen()

Убран вызов `selectionClose()` если нет предыдущего выделения:

```java
public void selectionOpen(Plugin.View.Selection s) {
    if (selection != null) { // только если есть предыдущее выделение
        selectionClose();
    }
    ...
}
```

### 4. ScrollWidget.onLongPress()

Убран вызов `onFingerReleaseAfterLongPress()` - он вызывается при реальном `ACTION_UP`:

```java
// onFingerReleaseAfterLongPress будет вызван при ACTION_UP через onReleaseCheck
fb.app.BookTextView.onFingerLongPress(x, y);
```

## Новая архитектура

### Удалённые файлы

- `fbreader/src/main/java/org/geometerplus/android/fbreader/SelectionPopup.kt` - старый popup
- `fbreader/src/main/res/layout/selection_panel.xml` - старый layout

### Новые файлы

- `bookreader/src/main/java/com/github/axet/bookreader/screens/ui/SelectionComposePanel.kt` - Compose панель

## Flow выделения текста (после исправления)

### Создание выделения (long press)

1. ACTION_DOWN → postLongClickRunnable
2. После задержки → onFingerLongPress → initSelection (без SELECTION_HIDE_PANEL!)
3. ACTION_UP → onFingerRelease → releaseSelectionCursor → SELECTION_SHOW_PANEL
4. Панель показывается без мерцания

### Движение маркера

1. ACTION_DOWN на маркере → onFingerPress → SELECTION_HIDE_PANEL → moveSelectionCursorTo
2. ACTION_MOVE → moveSelectionCursorTo (панель скрыта)
3. ACTION_UP → onFingerRelease → releaseSelectionCursor → SELECTION_SHOW_PANEL

### Кнопка Close

1. onClick → ReaderActions.HideSelection → SELECTION_CLEAR
2. clearSelection → selectionClose → SELECTION_HIDE_PANEL

## Связанные файлы

- [[bookreader-bookmarks-feature]] - функционал закладок
- [[fbreader-refactoring-plan]] - план рефакторинга fbreader

**Why:** Миграция на Compose UI и исправление бага мерцания панели выделения

**How to apply:** При работе с выделением текста использовать ReaderActions и ReaderState.showSelection. SELECTION_HIDE_PANEL вызывается только в onFingerPress при нажатии на маркер.