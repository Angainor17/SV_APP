# Виджеты bookreader

## FBReaderView.java

Файл: `widgets/FBReaderView.java`

Главный View читалки. Наследуется от `RelativeLayout`. Является центральным хабом, к которому подключены все остальные компоненты.

### Поля

```java
public FBReaderApp app;           // ядро FBReader
public ConfigShadow config;       // прокси для ZLOptions
public ZLViewWidget widget;       // текущий виджет (PagerWidget или ScrollWidget)
public int battery;               // уровень заряда (0-100)
public Storage.FBook book;        // текущая книга
public Plugin.View pluginview;    // view плагина (PDF/DjVu/Comics) или null
public Listener listener;         // callback в ReaderContent
public TTSPopup tts;              // TTS (null если не активен)
SelectionView selection;          // текущее выделение (null если нет)
Plugin.View.Search search;        // текущий поиск (null если нет)
private boolean isFullscreenMode; // отслеживание fullscreen
```

### Инициализация (create())
1. Создаёт `FBReaderApp` — ядро FBReader
2. Устанавливает `FBApplicationWindow`
3. Создаёт `CustomView` (extends `FBView`) — view для текстовых форматов
4. Создаёт `FBFooterView` — нижний footer
5. Вызывает `setWidget(PAGING)` — устанавливает первый виджет

### Загрузка книги (loadBook)
```java
public void loadBook(Storage.FBook fbook) {
    plugin = Storage.getPlugin(info, fbook)
    if (plugin instanceof Plugin) {
        // PDF/DjVu/Comics путь
        pluginview = ((Plugin) plugin).create(fbook)
        model = BookModel.createModel(fbook.book, plugin)
        app.BookTextView.setModel(model.getTextModel())
        gotoPluginPosition(book.info.position)
    } else {
        // FBReader путь (EPUB/FB2)
        model = BookModel.createModel(fbook.book, plugin)
        app.BookTextView.setModel(model.getTextModel())
        app.BookTextView.gotoPosition(book.info.position)
        bookmarksUpdate()
    }
    widget.repaint()
}
```

### Переключение виджетов (setWidget)
При смене режима (paging ↔ scroll):
- Закрывает текущее выделение (`selectionClose()`)
- Закрывает все overlays
- Сохраняет текущую позицию
- **Сбрасывает zoom** (scale=1.0, translation=0)
- Удаляет старый widget
- Добавляет новый widget
- Восстанавливает позицию через `gotoPosition(pos)`

### Fullscreen (SHOW_MENU action)
Управляется через `WindowInsetsControllerCompat`:
- `controller.hide(systemBars())` — скрыть status/nav bar
- `controller.show(systemBars())` — показать

Уведомляет `listener.onFullscreenToggle(isFullscreen)` **перед** нативным переключением, чтобы Compose успел обновить Scaffold до анимации.

### Listener интерфейс
```kotlin
interface Listener {
    fun onScrollingFinished(index: ZLViewEnums.PageIndex?)
    fun onSearchClose()
    fun onBookmarksUpdate()
    fun onDismissDialog()
    fun ttsStatus(speaking: Boolean)
    fun onEditBookmark(bookmark: Storage.Bookmark)
    fun onFullscreenToggle(isFullscreen: Boolean)
    fun onNavigationRequest()
    fun onSelectionShow(startY: Int, endY: Int)
    fun onSelectionHide()
    fun onZoomChange(scale: Float, pivotX: Float, pivotY: Float)
    fun onZoomEnd()
}
```

### Сохранение позиции (getPosition)
Логика зависит от режима:

**Plugin (PDF/DjVu/Comics) + ScrollWidget:**
- Ищет первую видимую страницу через RecyclerView
- Вычисляет `offset = top * info.ratio` (пиксели экрана → единицы документа)
- Возвращает `ZLTextFixedPosition(pageNumber, offset, 0)`

**Plugin + PagerWidget:**
- Возвращает `pluginview.getPosition()` → `(current.pageNumber, current.pageOffset, 0)`

**FBReader (EPUB/FB2):**
- В ScrollWidget: итерирует по элементам, находит первый видимый
- В PagerWidget: `app.BookTextView.getStartCursor()`

### Ключевые вспомогательные классы

**CustomView** (inner class, extends FBView):
- Отображает текстовые книги (EPUB, FB2, MOBI)
- Реализует интерфейс `Plugin.View.Selection.Setter` для выделения
- Методы: `selectionBackgroundColor`, `backgroundColor`

**ZLTextIndexPosition** (`widgets/ZLTextIndexPosition.kt`):
- `Parcelable` класс с `start` и `end` позициями
- Используется для передачи позиции заметки через Modo (Parcel)

**BrightnessGesture**:
- Управление яркостью экрана жестом по левому краю
- Используется в PagerWidget и ScrollWidget

---

## PagerWidget.kt

Файл: `widgets/PagerWidget.kt`

Виджет постраничного просмотра. Наследуется от `ZLAndroidWidget` (FBReader) и реализует `ZoomGestureHandler.ZoomListener`.

### Режим работы
- Один экран = одна страница (или часть страницы для высоких PDF)
- Свайп влево/вправо = следующая/предыдущая страница
- Tap по зонам = навигация (настраивается через TapZoneMap)

### Zoom
```kotlin
init {
    zoomHandler = ZoomGestureHandler(fb.context, this)
}

override fun onTouchEvent(event: MotionEvent): Boolean {
    // Zoom только для Plugin (не для EPUB/FB2 reflow)
    if (fb.pluginview != null && !fb.pluginview!!.reflow) {
        zoomHandler.onTouchEvent(event)
    }
    return super.onTouchEvent(event)
}

// ZoomListener callbacks:
override fun onZoomChange(scale, pivotX, pivotY) {
    fb.scaleX = scale; fb.scaleY = scale
    fb.pivotX = pivotX; fb.pivotY = pivotY
}
```

### Overlays
PagerWidget управляет несколькими "слоями" поверх страницы:
- `infos` — `ReflowMap<Reflow.Info>` — данные reflow
- `links` — `ReflowMap<LinksView>` — кликабельные ссылки
- `bookmarks` — `ReflowMap<BookmarksView>` — подсветка закладок
- `tts` — `ReflowMap<TTSView>` — подсветка TTS
- `searchs` — `ReflowMap<SearchView>` — подсветка поиска

`ReflowMap<V>` — HashMap с ограничением до 9 записей (предотвращает утечку памяти при reflow). При добавлении 10-й записи удаляется старейшая.

### Управление выделением при смене страницы
```kotlin
fun updateOverlays() {
    val position = getPosition()

    if (selectionPage != null && !selectionPage.samePositionAs(position)) {
        // Перешли на другую страницу - скрыть handles, НО НЕ ЗАКРЫВАТЬ выделение
        fb.selection?.hideHandles()
        fb.app.runAction(ActionCode.SELECTION_HIDE_PANEL)
    } else if (selectionPage != null && selectionPage.samePositionAs(position)) {
        // Вернулись на страницу с выделением
        fb.selection?.restoreHandles()
        fb.app.runAction(ActionCode.SELECTION_SHOW_PANEL)
    }
}
```

### onLongClick — создание выделения
```kotlin
override fun onLongClick(v: View): Boolean {
    if (fb.pluginview != null) {
        val s = fb.pluginview!!.select(pos, info, dst.width, dst.height, x - dst.left, y - dst.top)
        if (s != null) {
            selectionPage = pos
            fb.selectionOpen(s)
            // Создаём PageView с Setter
            val setter = object : Plugin.View.Selection.Setter { ... }
            val view = SelectionView.PageView(context, custom, setter)
            fb.selection.add(view)
            run()  // обновляем координаты
        }
    }
}
```

### getPageRect()
Возвращает прямоугольник контентной области страницы в экранных координатах:
- Учитывает `pageOffset` (частичная страница)
- Учитывает center-aligned страницы (когда страница меньше экрана)
- Используется для преобразования touch-координат в координаты страницы

---

## ScrollWidget.java

Файл: `widgets/ScrollWidget.java` (Legacy Java)

Виджет непрерывной прокрутки. Наследуется от `RecyclerView`.

### Режим работы
- Вертикальный RecyclerView
- Каждый элемент = одна "виртуальная страница" (может быть больше, чем реальная страница)
- `ScrollAdapter` — адаптер с `PageCursor` (позиция в тексте)

### Особенности
- `findFirstPage()` — находит первый видимый элемент для сохранения позиции
- `findRegionView()` — находит View с элементом (для ссылок)
- `mainAreaHeight` — высота без footer

---

## SelectionView.kt

Файл: `widgets/SelectionView.kt`

View для отображения выделения текста. Наследуется от `FrameLayout`.

### Архитектура
```
SelectionView (FrameLayout)  ←  абсолютные координаты на экране
    └── PageView (View)       ←  выделение на одной странице
        └── selection: Bounds ←  прямоугольники символов
        └── lines: List<Rect> ←  объединённые строки
```

`SelectionView` добавляется в `FBReaderView` как дочерний View и позиционируется через `MarginLayoutParams` (leftMargin, topMargin = абсолютные координаты).

### Поля
```kotlin
var startRect: HandleRect  // левый маркер (начало выделения)
var endRect: HandleRect    // правый маркер (конец выделения)
var margin: Rect?          // абсолютные координаты SelectionView
var clip: Int              // высота отсечения (footer)
private var _dragState: DragState  // Idle | Dragging(handle, offsetX, offsetY)
private var _handlesVisible: Boolean = true
private var _savedSelectionData: SavedSelectionData?
```

### onDraw
Рисует маркеры выделения (`drawHandle()`). Маркеры — это вертикальная линия + кружок снизу (для правого) или сверху (для левого).

### onTouchEvent
```
ACTION_DOWN → checkHandleHit() → если попал в маркер → startDrag()
ACTION_MOVE → handleDragMove() → updateHandlePosition() → setter.setStart/setEnd()
ACTION_UP   → handleDragEnd() → endDrag() → callbacks.onDragEnd()
```

### Скрытие/восстановление handles при смене страницы
- `hideHandles()` — скрывает весь SelectionView (visibility = INVISIBLE), сохраняет данные
- `restoreHandles()` — восстанавливает данные, делает VISIBLE
- Это не удаляет выделение! `selectionPage` в PagerWidget определяет, нужно ли показывать

### PageView
```kotlin
class PageView(context, custom, setter: Plugin.View.Selection.Setter?) : View {
    var margin: Rect     // абсолютные координаты страницы
    var selection: Bounds?  // прямоугольники выделения
    var lines: List<Rect>?  // объединённые строки

    fun update() {
        selection = setter.getBounds()
        lines = lines(selection.rr)  // объединить в строки
        viewBounds = union(lines)
    }

    override fun onDraw(canvas) {
        for (r in lines) canvas.drawRect(r, paint)  // закрашенные прямоугольники
    }
}
```

---

## ZoomGestureHandler.kt

Файл: `widgets/ZoomGestureHandler.kt`

Обрабатывает жесты zoom, pan и double-tap для постраничного режима.

### Детекторы
1. `ScaleGestureDetector` — pinch-to-zoom (standard Android)
2. `tapDetector: GestureDetector` — double-tap для fit-width zoom
3. `panDetector: GestureDetector` — горизонтальный pan при zoom > 1.0

### Ключевые параметры
```kotlin
val minZoom = 1.0f   // минимум (без zoom)
val maxZoom = 3.0f   // максимум (3x)
val maxFitWidthZoom = 1.25f  // максимум для double-tap fit-width
```

### Логика fit-width zoom (double-tap)
1. Получает ширину контента страницы через `listener.getPageContentWidth()`
2. Вычисляет zoom чтобы страница заняла всю ширину экрана
3. Ограничивает до 1.05f..1.25f (чтобы не было слишком большого zoom)
4. Применяет через `setZoom(fitWidthZoom, centerPivotX, centerPivotY)`

### Важная особенность
`onTouchEvent()` всегда возвращает `false`. Zoom-обработчик не перехватывает события — другие обработчики (scroll, long press) тоже получают events.

### ZoomListener
Реализуется `PagerWidget`:
```kotlin
override fun onZoomChange(scale, pivotX, pivotY) {
    fb.scaleX = scale
    fb.scaleY = scale
    fb.pivotX = pivotX
    fb.pivotY = pivotY
}
override fun onPanChange(offsetX, offsetY) {
    fb.translationX = offsetX
    fb.translationY = offsetY
}
```

---

## ZoomTouchAdapter.kt

Файл: `widgets/ZoomTouchAdapter.kt`

Конвертирует screen-координаты в content-координаты при zoom.

```kotlin
// Формула:
contentX = (screenX - pivotX) / scale + pivotX

fun adaptX(rawX: Float): Float {
    val scale = fbReaderView.scaleX
    if (scale == 1.0f) return rawX  // нет zoom
    return (rawX - pivotX) / scale + pivotX
}
```

Используется при обработке touch-событий когда FBReaderView масштабирован.

---

## ActiveAreasView.kt

Файл: `widgets/ActiveAreasView.kt`

Показывает подсказки зон касания при первом открытии книги.

### Зоны
| Зона | Действие |
|---|---|
| `menu` | Fullscreen (центральный экран) |
| `navigate` | Навигация (нижняя часть) |
| `nextPage` | Следующая страница |
| `previousPage` | Предыдущая страница |
| `brightness` | Яркость (левый край) |

### Принцип работы
1. `create(app, ww)` — читает `TapZoneMap` из настроек FBReader
2. Строит карту зон в координатах 0..PERC (10000)
3. `onMeasure()` — масштабирует в пиксели экрана
4. Показывается на 3 секунды, затем исчезает с анимацией fade-out

Вызывается из `ReaderContent.update{}` при первом отображении view:
```kotlin
if (!currentState.hasShownControlsHint && view.width > 0) {
    view.postDelayed({ view.showControls() }, 300)
}
```

---

## SelectionCoordinates.kt

Файл: `widgets/SelectionCoordinates.kt`

Helper-объект для работы с тремя системами координат:

1. **Device** — абсолютные координаты экрана (0,0 = top-left экрана)
2. **Page** — относительно страницы PDF (0,0 = top-left страницы)
3. **View** — относительно SelectionView (0,0 = top-left SelectionView)

```kotlin
object SelectionCoordinates {
    fun deviceToPage(rect: Rect, pageRect: Rect)   // - offset страницы
    fun pageToDevice(rect: Rect, pageRect: Rect)   // + offset страницы
    fun deviceToView(rect: Rect, viewRect: Rect)   // - offset SelectionView
    fun viewToDevice(rect: Rect, viewRect: Rect)   // + offset SelectionView
    fun touchToDevice(touchX, touchY, viewLeft, viewTop)  // touch → device
}
```

---

## SelectionState.kt

Файл: `widgets/SelectionState.kt`

Data-классы для состояний выделения:

```kotlin
sealed class DragState {
    data object Idle : DragState()
    data class Dragging(
        val handle: HandleType,
        val offsetX: Int,  // offset от touch до hot point
        val offsetY: Int,
        val startX: Int,
        val startY: Int
    ) : DragState()
}

enum class HandleType { LEFT, RIGHT }

data class SelectionState(
    val dragState: DragState,
    val startHandleBounds: Rect?,
    val endHandleBounds: Rect?,
    val isValid: Boolean
)
```

`SelectionCallbacks` — интерфейс callbacks при drag:
- `onDragStart(handle)` — начало drag (прячем Compose панель)
- `onDragEnd(handle)` — конец drag (показываем Compose панель)
- `onBoundsChanged(start, end)` — изменились bounds

---

## FBFooterView.kt

Файл: `widgets/FBFooterView.kt`

Нижний footer: показывает прогресс чтения (страницы или проценты) и уровень батареи. Рендерится непосредственно на Canvas.

---

## WallpaperLayout.kt

Нестандартный layout для рисования фона (обоев читалки). Используется в CustomView.

---

## TimeAnimatorCompat.kt

Совместимая замена `TimeAnimator` для API < 16.
