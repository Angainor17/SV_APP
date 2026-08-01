# Выделение текста в bookreader

## Обзор

Выделение текста работает по-разному для PDF/DjVu (plugin mode) и EPUB/FB2 (FBReader mode). В обоих случаях конечным результатом является `SelectionView` с маркерами и `SelectionComposePanel` с кнопками действий.

---

## Полный поток выделения текста

### 1. Начало выделения (long press)

**Plugin mode (PDF/DjVu):**
```
PagerWidget.onLongClick(v)
    └── pluginview.select(pos, info, dst.w, dst.h, x, y)
        └── selectPoint(info, x, y) → Selection.Point
        └── selectPage(pos, info, w, h) → Selection.Page
        └── (PDF) Selection(pdfium, selPage, point).selectWord()
            ├── text.getIndex(point.x, point.y) → индекс символа
            └── расширяем влево/вправо пока isWord(char) → start..end
```

**FBReader mode (EPUB/FB2):**
Обрабатывается самим FBReader через `MiscOptions.WordTappingAction = startSelecting`.

### 2. Создание SelectionView

```kotlin
// PagerWidget.onLongClick
fb.selectionOpen(s)               // FBReaderView.selectionOpen()
val setter = object : Plugin.View.Selection.Setter {
    override fun setStart(x, y) { s.setStart(page, selectPoint(...)) }
    override fun setEnd(x, y) { s.setEnd(page, selectPoint(...)) }
    override fun getBounds(): Bounds = s.getBounds(page)
}
val view = SelectionView.PageView(context, custom, setter)
fb.selection.add(view)  // добавляем в SelectionView
run()  // fb.selection.update(view, x + dst.left, y + dst.top)
```

### 3. FBReaderView.selectionOpen()

```java
public void selectionOpen(Plugin.View.Selection s) {
    // Закрываем старое выделение
    if (selection != null) selectionClose()
    
    // Создаём новый SelectionView
    selection = new SelectionView(context, app.BookTextView, s, callbacks)
    
    // Настраиваем callbacks
    selection = new SelectionView(..., new SelectionCallbacks() {
        onDragStart(handle) → app.runAction(SELECTION_HIDE_PANEL)
        onDragEnd(handle) → app.runAction(SELECTION_SHOW_PANEL)
    })
    
    // Добавляем в layout
    addView(selection, lp)
}
```

### 4. Показ панели (SELECTION_SHOW_PANEL action)

```java
// FBReaderView регистрирует action в setActivity()
app.addAction(ActionCode.SELECTION_SHOW_PANEL, new FBAction(app) {
    protected void run(Object... params) {
        if (listener != null) {
            listener.onSelectionShow(
                selection.getSelectionStartY(),
                selection.getSelectionEndY()
            )
        }
    }
})
```

`listener.onSelectionShow(startY, endY)` → `viewModel.onAction(ShowSelection(startY, endY))` → `_state = state.copy(showSelection = true, selectionStartY, selectionEndY)`

### 5. SelectionComposePanel появляется

```kotlin
// ReaderContent.kt
if (currentState.showSelection) {
    val showAtBottom = currentState.selectionEndY > currentState.selectionStartY
    Box(
        contentAlignment = if (showAtBottom) BottomCenter else TopCenter
        // Панель показывается снизу или сверху в зависимости от позиции выделения
    ) {
        SelectionComposePanel(
            onBookmark = { viewModel.onAction(SelectionBookmark) },
            onShare = { viewModel.onAction(SelectionShare) },
            onCopy = { viewModel.onAction(SelectionCopy) },
            onQuestion = { viewModel.onAction(SelectionQuestion) },
            onAlert = { viewModel.onAction(SelectionAlert) },
            onClose = { viewModel.onAction(HideSelection) }
        )
    }
}
```

### 6. Drag маркеров выделения

```
SelectionView.onTouchEvent(ACTION_DOWN)
    └── checkHandleHit(startRect, x, y) → hit или нет
    └── checkHandleHit(endRect, x, y) → hit или нет
    └── startDrag(handle, offsetX, offsetY)
        └── _dragState = Dragging(handle, ...)
        └── callbacks.onDragStart(handle)
            └── app.runAction(SELECTION_HIDE_PANEL)
            └── listener.onSelectionHide()  (скрываем Compose панель)

SelectionView.onTouchEvent(ACTION_MOVE)
    └── handleDragMove(x, y)
        └── updateHandlePosition(handle, adjustedX, adjustedY)
            └── setter.setStart/setEnd(x, y)  (обновляем в Plugin)
            └── fb.selection.update(pageView, x, y)  (обновляем рисование)

SelectionView.onTouchEvent(ACTION_UP)
    └── handleDragEnd()
        └── endDrag()
        └── callbacks.onDragEnd(handle)
            └── app.runAction(SELECTION_SHOW_PANEL)
            └── listener.onSelectionShow(...)  (показываем Compose панель)
```

### 7. Действия с выделением

**Копировать:**
```kotlin
// ViewModel
private fun selectionCopy() {
    fbReaderView?.app?.runAction(ActionCode.SELECTION_COPY_TO_CLIPBOARD)
    hideSelectionPanel()
}
```

**Создать закладку:**
```kotlin
private fun selectionBookmark() {
    fbReaderView?.app?.runAction(ActionCode.SELECTION_BOOKMARK)
    hideSelectionPanel()
}
// → FBReaderView обрабатывает SELECTION_BOOKMARK:
// создаёт Storage.Bookmark с текстом выделения
// вызывает listener.onEditBookmark(bookmark) - открывает BookmarkBottomSheet
```

**Скрыть выделение:**
```kotlin
// HideSelection action:
fbReaderView?.app?.runAction(ActionCode.SELECTION_CLEAR)
// → FBReaderView.selectionClose()
// → selection?.close() (закрывает Plugin.View.Selection)
// → removeView(selection)
// → SELECTION_HIDE_PANEL → listener.onSelectionHide()
// → viewModel.hideSelection()
// → _state = state.copy(showSelection = false)
```

---

## SelectionState.kt — состояния

```kotlin
sealed class DragState {
    object Idle : DragState()
    data class Dragging(handle, offsetX, offsetY, startX, startY) : DragState()
}

enum class HandleType { LEFT, RIGHT }
```

Вспомогательные данные:
- `HandleTouchResult(hit, handleType, offsetX, offsetY)` — результат проверки hit test
- `SavedSelectionData(startRectData, endRectData, marginRect)` — сохранённые данные при скрытии

---

## SelectionCoordinates.kt

Три системы координат:
1. **Device** — абсолютные координаты экрана
2. **Page** — относительно страницы PDF (в PDF: origin снизу-слева, Y инвертирован)
3. **View** — относительно SelectionView

Преобразование PDF-координат в device происходит через:
- `ppage.toDevice(0, 0, w, h, 0, rect)` — метод pdfium
- `toDevice(info, w, h, rect)` — метод DjvuPlugin

---

## SelectionView.kt — как рисуется

### PageView.onDraw()
```kotlin
override fun onDraw(canvas: Canvas) {
    for (r in lines!!)
        canvas.drawRect(r, paint)  // paint.color = выделяющий цвет с alpha=0x99
}
```

`lines` — список прямоугольников, объединённых по строкам через `SelectionView.lines()`:
```kotlin
fun lines(rr: List<Rect>): List<Rect> {
    // Объединяет прямоугольники если они пересекаются по вертикали (одна строка)
    // Возвращает горизонтальные полосы
}
```

### SelectionView.onDraw()
Рисует маркеры (handles) поверх `PageView`:
```kotlin
fun drawHandle(canvas, which: SelectionCursor.Which, rect: HandleRect) {
    val dpi = ZLibrary.Instance().displayDPI
    val unit = dpi / 120
    // Вертикальная линия: xCenter ± unit, height = dpi/4
    canvas.drawRect(...)
    // Кружок: Left → сверху, Right → снизу
    canvas.drawCircle(xCenter, y ± dpi/8, unit * 6, paint)
}
```

---

## SelectionComposePanel.kt

Файл: `screens/ui/SelectionComposePanel.kt`

Compose-панель с 6 кнопками:
1. **Закладка** — `FbreaderR.drawable.ic_bookmark_outline_white`
2. **Поделиться** — `FbreaderR.drawable.ic_share_white`
3. **Копировать** — `FbreaderR.drawable.ic_content_copy_white`
4. **Вопрос** — `FbreaderR.drawable.baseline_question_mark_24`
5. **Опечатка** — `FbreaderR.drawable.ic_missplell`
6. **Закрыть** — `FbreaderR.drawable.ic_close_white`

Иконки берутся из модуля fbreader (`FbreaderR`).

Позиционирование:
```kotlin
// Если выделение снизу (selectionEndY > selectionStartY) → панель снизу
val showAtBottom = currentState.selectionEndY > currentState.selectionStartY
```

---

## Разница PDF vs FB2/EPUB

### PDF (Plugin mode)
- Выделение через `pdfium.Text` — по индексам символов
- `startPage.index` / `endPage.index` — индексы символов в документе
- Координаты в PDF-системе (origin снизу-слева), конвертируются через `ppage.toDevice()`
- Поддерживает multi-page selection (startPage.page..endPage.page)
- На планшетах (NativeView) pdfium инициализируется лениво только для текстовых операций

### FB2/EPUB (FBReader mode)
- Выделение через `ZLTextWordCursor` / `ZLTextElementArea`
- Координаты в view-пространстве FBReader
- `ZLTextHighlighting` — для подсветки выделения
- ActionCode.SELECTION_* обрабатываются FBReader-ом напрямую

---

## Известные проблемы выделения

### 1. Debounce для ShowSelection/HideSelection
Есть race condition: при создании выделения сначала срабатывает SELECTION_HIDE_PANEL (из предыдущего состояния), затем SELECTION_SHOW_PANEL. Защита:

```kotlin
// ViewModel
private val SELECTION_DEBOUNCE_MS = 500L
private var lastSelectionShowTime = 0L

fun hideSelection() {
    val timeSinceShow = System.currentTimeMillis() - lastSelectionShowTime
    if (timeSinceShow < SELECTION_DEBOUNCE_MS) return  // debounce
    _state = state.copy(showSelection = false)
}
```

### 2. SelectionView.margin может быть null
Если выделение "повреждено" (PageView удалён до обновления), margin становится null. В этом случае touch events игнорируются.

### 3. Выделение при смене страницы (paging mode)
SelectionView скрывается (INVISIBLE) при переходе на другую страницу, но не закрывается. `selectionPage` в PagerWidget помнит страницу выделения. При возврате на ту же страницу SelectionView восстанавливается.

### 4. Координаты после fullscreen
После входа/выхода из fullscreen координаты маркеров могут сместиться (status bar появляется/исчезает). В `FBReaderView.toggleFullscreen()` вызывается `updateSelectionAfterFullscreenChange()`.
