# Система плагинов bookreader

## Обзор

Плагины обеспечивают рендеринг различных форматов файлов. Каждый плагин реализует интерфейс `Plugin` (`app/Plugin.kt`) и наследуется от FBReader-класса `BuiltinFormatPlugin`.

---

## Иерархия классов

```
Plugin (interface)               ← определяет create(FBook): View
BuiltinFormatPlugin (FBReader)   ← базовый класс формата

PDFPlugin   : BuiltinFormatPlugin, Plugin
DjvuPlugin  : BuiltinFormatPlugin, Plugin
ComicsPlugin: BuiltinFormatPlugin, Plugin

Plugin.View    ← базовый класс для отображения
Plugin.Page    ← базовый класс для страницы
Plugin.View.Selection  ← выделение текста
Plugin.View.Search     ← поиск текста
```

---

## Поддерживаемые форматы

| Формат | Плагин | Рендерер | Выделение | Поиск |
|---|---|---|---|---|
| PDF | PDFPlugin | Pdfium (смартфон) или Android PdfRenderer (планшет) | Есть (pdfium) | Есть (pdfium) |
| DjVu | DjvuPlugin | libdjvu (нативная) | Есть | Есть |
| CBZ | ComicsPlugin | BitmapFactory | Нет | Нет |
| CBR | ComicsPlugin | BitmapFactory + junrar | Нет | Нет |
| EPUB/FB2/MOBI | FBReader (нет плагина) | FBReader engine | Есть | Есть |

---

## Plugin.kt — базовые классы

Файл: `app/Plugin.kt`

### Plugin.Box
Прямоугольник в координатах документа:
```
x, y — нижний левый угол
w, h — ширина и высота
```
Метод `toRect(w, h)` конвертирует в Android `Rect`, учитывая инверсию оси Y (координаты PDF снизу вверх).

### Plugin.Page
Представляет одну страницу документа:
- `pageNumber` — номер страницы (0-indexed)
- `pageOffset` — вертикальное смещение внутри страницы (для документов которые не помещаются на экран)
- `pageBox` — размер страницы в единицах документа
- `w`, `h` — размер экрана в пикселях
- `ratio` — коэффициент масштабирования (pageBox.w / w)
- `hh` — высота страницы в экранных координатах
- `pageStep` — шаг прокрутки (hh * (1 - PAGE_OVERLAP_PERCENTS/100))
- `pageOverlap` — перекрытие страниц при прокрутке (5%)
- `dpi` — DPI страницы

Методы:
- `renderPage()` — вычисляет ratio, hh, pageStep, pageOverlap
- `renderRect()` — возвращает `RenderRect` с src/dst прямоугольниками для `canvas.drawBitmap()`
- `next()` / `prev()` — переходы между страницами
- `scale(w, h)` — масштабирует pageBox для zoom

### Plugin.View
Базовый класс для отображения книги:
- `current: Page` — текущая страница
- `wallpaper: Bitmap?` — фон (обои)
- `wallpaperColor: Int` — цвет фона
- `paint: Paint` — краска (включает ColorFilter для инверсии тёмной темы)
- `reflow: Boolean` — режим reflow (переформатирование PDF)
- `reflower: Reflow?` — алгоритм reflow

Ключевые методы:
- `drawOnBitmap()` / `drawOnCanvas()` — рендеринг страницы на Bitmap/Canvas
- `gotoPosition(p)` — переход к позиции
- `getPosition()` — текущая позиция (paragraphIndex=страница, elementIndex=смещение)
- `select(page, point)` — создать выделение по нажатию
- `select(start, end)` — создать выделение по позициям (для открытия заметки)
- `getPageText(pageNum)` — полный текст страницы (PDF)
- `search(text)` — поиск текста, возвращает `Search`
- `getLinks(page)` — ссылки на странице

---

## PDFPlugin

Файл: `app/PDFPlugin.kt`

### Выбор рендерера (смартфон vs планшет)

При первом вызове `PDFPlugin.create(info)` определяется тип устройства:
```kotlin
// Если screenSize >= 7.0 дюймов → планшет → Android PdfRenderer
// Иначе → смартфон → pdfium (нативная библиотека)
private var usePdfRenderer: Boolean? = null
```

**Причина**: pdfium краша на некоторых планшетах с большим экраном. Android PdfRenderer стабилен, но не поддерживает текстовые операции.

**Важный хак**: На планшетах (NativeView) для выделения текста и поиска всё равно используется pdfium через **ленивую инициализацию** `initTextDoc()`. Т.е. PdfRenderer рендерит страницы, а pdfium читает текст.

### NativeView (для планшетов)
```kotlin
inner class NativeView(f: ZLFile) : Plugin.View() {
    var doc: PdfRenderer           // рендерит страницы
    private var textDoc: Pdfium?   // читает текст (ленивая инициализация)
    private var textFd: ParcelFileDescriptor?
}
```

Хак с PdfRenderer: при вызове `draw()` нужно закрыть страницу перед `NativePage(curr, index, w, h)`, т.к. PdfRenderer бросает `IllegalStateException` при повторном открытии той же страницы. Поэтому `curr.page?.close(); curr.page = null` перед созданием нового `NativePage`.

PdfRenderer требует `Bitmap.Config.ARGB_8888`, не поддерживает `RGB_565`.

### PdfiumView (для смартфонов)
Использует нативную библиотеку pdfium. Ограничение на размер bitmap при reflow:
```kotlin
val maxDimension = 2000
val scaleMultiplier = if (w > maxDimension || h > maxDimension) 1 else 2
```

### PDFTextModel
Реализует `ZLTextModel` + `PdfiumView`. Используется FBReader для хранения количества страниц как "параграфов". Каждая страница PDF = один параграф с типом `END_OF_TEXT_PARAGRAPH`.

### SimplePdfTextModel
Используется в NativeView (планшеты). Содержит только количество страниц, без содержимого (PdfRenderer не предоставляет текст).

### PdfSearch
Поиск текста по всем страницам:
- `setPage(page)` — задаёт начальную страницу, сразу поиск по всем страницам книги
- `next()` / `prev()` — навигация по результатам
- Использует `pdfium.openPage(i).open().getText()` для каждой страницы

### Selection (PDF)
Выделение по символам pdfium:
- `SelectionPage` — открытая страница с текстом (pdfium.Text)
- `startPage.index` / `endPage.index` — индексы начального/конечного символа
- Метод `selectWord()` — выделяет слово по нажатию (расширяет от индекса влево и вправо)
- `getBounds(page)` — возвращает прямоугольники символов, конвертированные в device-координаты через `ppage.toDevice()`

---

## DjvuPlugin

Файл: `app/DjvuPlugin.kt`

Использует нативную библиотеку djvulibre (загружается через `Natives.loadLibraries`).

### Особенности
- `DjvuLibre` — кастомный класс с кэшем `SparseArray<Page>` для информации о страницах (чтобы не перечитывать каждый раз)
- Координатная система DjVu отличается: ось Y инвертирована. Методы `toPage()` и `toDevice()` конвертируют
- Текстовые зоны иерархические: `ZONE_CHARACTER`, `ZONE_WORD`, `ZONE_LINE`, `ZONE_PARAGRAPH` и т.д. Поиск идёт по всем типам, берётся первый непустой

### DjvuView
Метод `render()` использует масштаб x2 (`r.scale(w * 2, h * 2)`) для качества при reflow.

---

## ComicsPlugin

Файл: `app/ComicsPlugin.kt`

### Поддерживаемые архивы
- CBZ = ZIP-архив с изображениями → `ZipDecoder` (zip4j)
- CBR = RAR-архив с изображениями → `RarDecoder` (junrar)

### Декодеры
`Decoder` — базовый класс:
- `pages: ArrayList<ArchiveFile>` — список файлов изображений
- `toc: ArrayList<ArchiveToc>?` — структура папок как оглавление
- `render(p, c)` — декодирует изображение через `BitmapFactory`

`ArchiveFile` — интерфейс для файла в архиве:
- `open(): InputStream` — открыть для чтения
- `copy(os: OutputStream)` — скопировать
- `rect: Plugin.Box?` — размер изображения (ленивая инициализация через `getImageSize()`)

### Особенности
- Страницы сортируются по имени файла (`SortByName`)
- TOC строится автоматически из структуры папок в архиве (если папок > 1)
- ComicsView не поддерживает выделение текста и поиск

---

## Жизненный цикл плагина

```
1. FBReaderView.loadBook(fbook)
   └── Storage.getPlugin(info, fbook) → PDFPlugin / DjvuPlugin / ComicsPlugin
   └── plugin.create(fbook) → Plugin.View (PdfiumView / DjvuView / ComicsView)
       └── Открывает документ (ParcelFileDescriptor / FileInputStream)
       └── current = Page(doc)  ← первая страница

2. widget.drawOnBitmap(bitmap, index)
   └── pluginview.drawOnBitmap(context, bitmap, w, h, index, custom, info)
       └── (если reflow) reflower.render() → bitmap
       └── (иначе) draw(canvas, w, h, index)
           └── PdfiumPage / DjvuPage / ComicsPage
           └── canvas.drawBitmap(bm, src, dst, paint)

3. FBReaderView.closeBook()
   └── pluginview.close()
       └── doc.close() / fd.close()
```

---

## Система reflow

`Reflow` (`app/Reflow.kt`) — алгоритм для переформатирования PDF под размер экрана:
- Рендерит страницу PDF в bitmap
- Использует k2pdfopt для разбиения на колонки
- Результат — несколько "виртуальных" страниц из одной PDF-страницы
- `reflower.page` — номер исходной PDF-страницы
- `reflower.index` — индекс виртуальной страницы

При reflow `Plugin.Page.pageNumber` = номер виртуальной страницы (не номер страницы PDF).
