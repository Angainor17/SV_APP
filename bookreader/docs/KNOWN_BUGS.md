# Известные баги и проблемы bookreader

## Активные баги (IN PROGRESS / OPEN)

---

### BUG-001: PDF-страница не отображается при открытии заметки

**Статус**: IN PROGRESS (задокументирован в `memory/bookmark-navigation-bug.md`)

**Симптом**: При открытии заметки через модуль books (ReaderScreen с bookmarkPosition), книга
открывается, но показывает не ту страницу — либо первую, либо страницу из последней позиции чтения.

**Причина**: Timing issue. `gotoPosition()` вызывается до того как FBReaderView полностью
инициализирован (до первого `drawOnBitmap`), поэтому позиция не применяется или перезаписывается.

**Применённые исправления**:

1. Навигация через `post{}` в `factory{}` (отложенное выполнение после layout)
2. Fallback в `update{}` если `savedPos != null && view.width > 0`

**Текущее состояние**: Баг частично исправлен, но в некоторых сценариях (особенно при первом
открытии книги) может не работать корректно.

**Файлы**: `screens/ReaderContent.kt` lines ~358-393

---

### BUG-002: Мерцание fullscreen в ScrollWidget

**Статус**: OPEN (задокументирован в `memory/fullscreen-flicker-issue.md`)

**Симптом**: При входе/выходе из fullscreen режима (tap по центру экрана) возникает мерцание —
белый/чёрный flash перед появлением/скрытием TopBar.

**Причина**: Race condition между Compose анимацией (fadein/fadeout TopBar) и нативным скрытием
system bars (`WindowInsetsControllerCompat`).

**Применённые исправления**:

1. Уведомление listener перед нативным переключением
2. Использование `Window.decorView.setBackgroundColor(bgColor)` для уменьшения flash

**Файлы**: `widgets/FBReaderView.java` (toggleFullscreen), `screens/ReaderContent.kt` (
AnimatedVisibility)

---

### BUG-003: Zoom — проблемы с режимом pinch

**Статус**: OPEN (задокументирован в `memory/zoom-mode-issues.md`)

**Известные проблемы**:

1. После zoom > 1.0 выделение текста работает некорректно (координаты не адаптированы)
2. При переключении режима (paging ↔ scroll) zoom сбрасывается (корректно, но вызывает confusion)
3. Double-tap fit-width не всегда правильно вычисляет zoom для portrait vs landscape

**Файлы**: `widgets/ZoomGestureHandler.kt`, `widgets/ZoomTouchAdapter.kt`, `widgets/PagerWidget.kt`

---

### BUG-004: Выделение текста в PDF на планшетах

**Статус**: В процессе (задокументирован в `memory/tablet-pdf-selection-fix.md`)

**Симптом**: На планшетах (NativeView) выделение текста требует инициализации pdfium (
`initTextDoc()`), что занимает время. При первом long press pdfium может быть не инициализирован,
выделение не создаётся.

**Причина**: На планшетах используется Android PdfRenderer для рендеринга, но pdfium
инициализируется лениво при первом запросе выделения.

**Файлы**: `app/PDFPlugin.kt` (NativeView.initTextDoc, NativeView.select)

---

## Решённые проблемы (для истории)

### BUG-100: Краш PDF на планшетах (FIXED)

Задокументирован в `memory/tablet-pdf-renderer-fix.md`.

**Симптом**: Краш при открытии PDF на устройствах с большим экраном.

**Решение**: Определение типа устройства (>= 7 дюймов = планшет) при первой инициализации
`PDFPlugin`. На планшетах используется Android PdfRenderer вместо нативного pdfium.

```kotlin
private fun isTabletDevice(info: Storage.Info): Boolean {
    val screenSize = sqrt(widthInches² + heightInches²)
    return screenSize >= 7.0
}
```

---

## Технические долги (Technical Debt)

Дополнительно задокументировано в `memory/technical-debt.md`.

---

### TD-001: Legacy Java файлы

**Файлы**: `app/Storage.java`, `widgets/FBReaderView.java`, `widgets/ScrollWidget.java`

**Проблемы**:

- `FBReaderView.java` — огромный файл (~2000 строк), сложно поддерживать
- `ScrollWidget.java` — много внутренних классов
- `Storage.java` — наследуется от внешней Java библиотеки, сложно мигрировать

**Статус**: Отложено. Задокументировано в `docs/JAVA_TO_KOTLIN_GUIDE.md`.

---

### TD-002: FBReaderView хранит ссылку в ViewModel

```kotlin
// ReaderViewModel
var fbReaderView: FBReaderView? = null  // ОПАСНО! View reference в ViewModel
```

**Риск**: Memory leak если ViewModel переживёт Activity/Compose. На практике `ViewModel.onCleared()`
вызывает `closeBook()` который освобождает ресурсы.

**Почему так**: FBReaderView — не стандартный Android View, это целое приложение (FBReaderApp). Его
невозможно передать в ViewModel через DI.

---

### TD-003: SELECTION_DEBOUNCE hack

В ViewModel задан debounce 500мс для скрытия панели выделения:

```kotlin
private val SELECTION_DEBOUNCE_MS = 500L
```

**Причина**: Race condition между `SELECTION_HIDE_PANEL` (когда выделение завершается) и
`SELECTION_SHOW_PANEL` (когда новое выделение создано). FBReader посылает события асинхронно,
порядок не гарантирован.

**Риск**: Если пользователь очень быстро скрывает выделение после создания (<500мс), панель не
закроется.

---

### TD-004: Нет unit-тестов для Selection логики

Вся логика выделения находится в Java-коде (FBReaderView) и сложных классах (PDFPlugin.Selection).
Нет unit-тестов.

---

## Edge Cases в коде

### PDFPlugin: NullSafety в SelectionBounds

```kotlin
// PDFPlugin.kt, SelectionBounds()
constructor() {
    val sp = startPage  // может быть null если selection закрыт во время touch event!
    val ep = endPage
    if (sp == null || ep == null) {
        // Создаём пустой bounds вместо NPE
        page = openPageNum(0)
        ss = 0; ee = 0; cc = 0; ll = 0
        ...
        return
    }
```

Комментарий: _"Null safety: selection может быть закрыт во время touch event"_ — это race condition
между touch обработкой и закрытием selection.

---

### PagerWidget.ReflowMap: дублирование ключей

```kotlin
// PagerWidget.kt
inner class ReflowMap<V> : HashMap<ZLTextPosition, V>() {
    override fun put(key, value): V? {
        // Добавляем дополнительные ключи для граничных случаев reflow
        // (3,-1,0) == (2,2,0) когда (2,1,0) последний
        if (key.elementIndex == l) {
            super.put(ZLTextFixedPosition(key.paragraphIndex + 1, -1, 0), value)
        }
    }
}
```

Это обходное решение для случая когда граница страницы reflow совпадает с позицией на
следующей/предыдущей странице.

---

### NativeView (планшет): не закрываем страницу после draw

```kotlin
// PDFPlugin.kt, NativeView.draw()
// НЕ закрываем страницу здесь - она нужна для PagerWidget.getPageRect()
// Страница будет закрыта при следующем вызове loadPage() или в close()
```

PdfRenderer.Page должна быть закрыта перед открытием следующей. Если не закрыть сейчас,
`getPageRect()` в PagerWidget может использовать страницу после вызова. Поэтому закрываем в начале
следующего `draw()`:

```kotlin
curr.page?.close()
curr.page = null
val r = NativePage(curr, index, w, h)
```

---

### Storage.java: MD5 как идентификатор книги

```java
public static final int MD5_SIZE = 32;  // длина MD5 hex строки
```

Книга идентифицируется по MD5 содержимого файла. Это означает:

- Два одинаковых файла = одна книга (верно)
- Один файл с разными путями = одна книга (верно)
- Изменение файла = смена ID = потеря истории чтения (проблема при обновлении книги)

---

### FBReaderView: gotoPosition через scrollDelayed

В некоторых местах позиция сохраняется в `scrollDelayed` для применения после рендера:

```java
ZLTextPosition scrollDelayed;
boolean scrollCentered;
```

Это необходимо для случаев когда `gotoPosition()` вызывается до первого `drawOnBitmap()` — размеры
страницы ещё неизвестны.

---

### Обложки: сложная логика поиска

В `BookmarksRepository` обложка ищется в нескольких местах:

1. `json.optString("coverUrl")` — из JSON напрямую
2. `CacheImagesAdapter.cacheUri(context, bookUri)` — кэш по URI
3. Поиск в `externalCacheDir` по имени файла с MD5
4. Поиск в `filesDir` по имени файла с MD5

Это fragile — если изменится логика кэширования обложек в Storage, поиск может не найти обложку.

---

### Xiaomi/MIUI: проблема обновления цветов при смене темы

Задокументировано в `memory/xiaomi-miui-theme-fix.md`.

На Xiaomi устройствах цвета могут не обновляться при смене темы если Activity перезапускается. Это
не специфично для bookreader, но затрагивает отображение читалки.

---

## TODO/FIXME/HACK в коде

### FBReaderView.java

- `// TODO: migrate to Kotlin` — на нескольких методах
- Весь класс помечен в CLAUDE.md как "декомпозируется"

### PDFPlugin.kt

- `// Ограничиваем масштаб на больших экранах, чтобы избежать краша pdfium` — magic number 2000px
- `// На планшетах используем PdfRenderer (стабильный), pdfium крашится при открытии` — временное
  решение

### ReaderContent.kt

-
`// Позиция заметки применяется в factory. Этот fallback срабатывает только если factory по какой-то причине не применил позицию.` —
двойная логика навигации

### PagerWidget.kt

- `// (3,-1,0) == (2,2,0) когда (2,1,0) последний` — неочевидная логика ReflowMap

### SelectionView.kt

- `// Legacy fields (будут удалены после полного refactor)` — в полях touch, startRect, endRect

### ReaderViewModel.kt

- `// ВАЖНО: Должен вызываться ПОСЛЕ инициализации FBReaderView` — на
  `migrateBookmarksContextAsync()`
- `// ВНИМАНИЕ: Должен вызываться синхронно, т.к. extractSentenceContext работает с UI` — нарушает
  принципы архитектуры
