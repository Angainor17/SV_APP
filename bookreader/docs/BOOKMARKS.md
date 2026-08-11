# Система закладок и заметок bookreader

## Обзор

В проекте термины "закладка" и "заметка" используются для одного и того же объекта
`Storage.Bookmark`. Закладка = выделенный текст с опциональным именем и цветом. Хранится в
JSON-файле рядом с книгой.

---

## Storage.Bookmark (Storage.java)

Поля:

```java
public long last;               // timestamp создания/изменения (также ID для ключа)
public String name;             // имя закладки (может быть null)
public String text;             // выделенный текст (может содержать FBReader-маркеры)
public int color;               // цвет подсветки (0 = по умолчанию)
public ZLTextPosition start;    // начало выделения
public ZLTextPosition end;      // конец выделения
public String coverUrl;         // путь к обложке книги (локальный)
public String bookFileUri;      // URI файла книги (для навигации из другого модуля)
public String sentenceBefore;   // предложение до заметки (контекст)
public String sentenceAfter;    // предложение после заметки (контекст)
```

`ZLTextPosition` хранится как `[paragraphIndex, elementIndex, charIndex]` в JSON.

**Для PDF**: `paragraphIndex` = номер страницы (0-indexed), `elementIndex` = смещение в px * ratio.

**Для EPUB/FB2**: `paragraphIndex` = номер параграфа, `elementIndex` = индекс элемента.

### Storage.Bookmarks

Расширяет `ArrayList<Bookmark>`. Сериализуется в JSON внутри `RecentInfo`.

---

## Хранение (BookmarksRepository.kt)

Файл: `domain/BookmarksRepository.kt`

Каждая книга имеет JSON-файл `{md5}.json` в хранилище:

```json
{
    "title": "Название книги",
    "authors": "Автор",
    "coverUrl": "/path/to/cover.jpg",
    "bookFileUri": "content://...",
    "position": [42, 100, 0],
    "bookmarks": [
        {
            "last": 1700000000000,
            "name": "Важная мысль",
            "text": "Выделенный текст с маркерами",
            "color": -65536,
            "start": [42, 0, 0],
            "end": [42, 15, 0],
            "coverUrl": "/path/to/cover.jpg",
            "bookFileUri": "content://...",
            "sentenceBefore": "Текст до выделения",
            "sentenceAfter": "Текст после выделения"
        }
    ]
}
```

### Методы репозитория

- `getAllNotes(sortByDateAscending)` — все заметки из всех книг
- `getNotesForBook(bookId)` — заметки конкретной книги
- `getBooksWithNotes()` — список книг с заметками и метаданными
- `deleteNote(noteId)` — удаляет по `{bookId}_{timestamp}`
- `findBookFileUri(bookId)` — ищет файл книги по MD5

### Поиск JSON-файлов

Репозиторий сканирует хранилище напрямую (не завися от наличия файла книги):

```kotlin
private fun listJsonFiles(): List<JsonFileInfo> {
    when (storageUri.scheme) {
        "content" → DocumentsContract + ContentResolver.query (SAF)
        "file" → File.listFiles()
    }
    // Ищет файлы с именем {32-символа MD5}.json
}
```

### Обложки

Поиск обложки (по приоритету):

1. `coverUrl` из JSON (если сохранён при создании заметки)
2. `CacheImagesAdapter.cacheUri(context, uri)` — кэш по URI книги
3. Поиск файла в `externalCacheDir` и `filesDir` по MD5

---

## BookmarkTextUtils.kt

Файл: `domain/BookmarkTextUtils.kt`

FBReader вставляет специальные символы в текст:

- `U+FFFE` (65534) — маркер переноса слов
- `U+FFFF` (65535) — аналогичный маркер
- Управляющие символы (0x00-0x1F)
- `[image]`, `[1]`, `[2]` — маркеры изображений и сносок

```kotlin
fun cleanBookmarkText(text: String): String {
    return text
        .replace(Regex("[\\uFFFE\\uFFFF]"), "")
        .replace(Regex("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]"), "")
        .replace("\r\n", " ").replace("\n", " ").replace("\r", " ")
        .replace(Regex("\\[image]"), "")
        .replace(Regex("\\[\\d+]"), "")
        .trim()
        .replace(Regex("  +"), " ")
}
```

Используется везде где отображается текст заметки:

- `BookmarksComposeDialog.kt` — список заметок
- `BookmarkBottomSheet.kt` — редактирование заметки
- В модуле `books` (screens/ui/NoteItem.kt и др.)

---

## BookContextService.kt

Файл: `domain/BookContextService.kt`

Извлекает контекст предложения вокруг заметки из книги.

```kotlin
suspend fun getNoteContext(bookUri, position, noteText): Result<NoteContextResult?>
```

Алгоритм:

1. `storage.load(bookUri)` → `Storage.Book`
2. `storage.read(storageBook)` → `Storage.FBook`
3. `Storage.getPlugin(info, fbook)` → форматный плагин
4. `BookModel.createModel(fbook.book, plugin)` → BookModel
5. `textModel.getParagraph(i).iterator()` — итерация по параграфам
6. Объединяет текст из `position.startParagraph ± 5` параграфов
7. `extractSentenceWithContext(fullText, noteText)` — ищет заметку, находит границы предложения

Границы предложения:

- Начало: ищет назад до `.`, `!`, `?`, `\n`
- Конец: ищет вперёд до `.`, `!`, `?`, `\n`

Результат (`NoteContextResult`):

- `sentenceBefore` — текст до заметки в пределах предложения
- `noteText` — сам выделенный текст
- `sentenceAfter` — текст после заметки в пределах предложения

---

## ZLBookmark.kt

Файл: `widgets/ZLBookmark.kt`

Расширяет `ZLTextSimpleHighlighting` для подсветки закладок в FBReader-view:

```kotlin
open class ZLBookmark(view: FBView, val b: Storage.Bookmark)
    : ZLTextSimpleHighlighting(view, b.start, b.end) {

    override fun getBackgroundColor(): ZLColor {
        if (b.color != 0) return ZLColor(b.color)
        return view.highlightingBackgroundColor  // цвет по умолчанию
    }
}
```

`ZLTTSMark` — аналогичный класс для TTS-подсветки.

Закладки добавляются в FBReader через `app.BookTextView.addHighlighting(ZLBookmark(view, bm))`.

---

## Навигация к закладке

### Из диалога BookmarksComposeDialog

```kotlin
fbReaderView?.apply {
    val position = ZLTextFixedPosition(bookmark.start.paragraphIndex, 0, 0)
    if (widget is PagerWidget) {
        // Постраничный режим
        gotoPosition(position)
        widget.reset()
        widget.repaint()
    } else {
        // Непрерывный режим — центрирование
        gotoPositionCentered(position)
    }
}
```

**Важно**: Используется `paragraphIndex` с `elementIndex = 0`, игнорируя точное смещение. Это
навигирует к началу страницы/параграфа, не к точной позиции заметки.

### Из модуля books (открытие книги на заметке)

```kotlin
// Вызывающий код в модуле books
stackNavigation.forward(
    ReaderScreen(
        bookUri = Uri.parse(bookmark.bookFileUri),
        bookmarkPosition = BookmarkPosition(
            startParagraph = bookmark.startParagraph,
            startElement = bookmark.startElement,
            startChar = bookmark.startChar,
            endParagraph = bookmark.endParagraph,
            endElement = bookmark.endElement,
            endChar = bookmark.endChar,
        )
    )
)
```

В `ReaderContent.factory{}`:

```kotlin
val savedPos = viewModel.getSavedPosition()
if (savedPos != null) {
    viewModel.clearSavedPosition()
    post { gotoPosition(savedPos) }  // через post для ожидания layout
}
```

`gotoPosition()` в FBReaderView:

- Для Plugin: `pluginview.gotoPosition(p)` → `current.load(p.paragraphIndex, p.elementIndex)`
- Для FBReader: `app.BookTextView.gotoPosition(p)`

### Известная проблема навигации к заметке

Баг задокументирован в `memory/bookmark-navigation-bug.md`. При открытии заметки PDF страница может
не отображаться корректно. Применённые исправления:

1. Навигация в `factory` через `post{}` (чтобы дождаться инициализации)
2. Fallback в `update{}` если view уже имеет ширину

---

## BookmarkBottomSheet.kt

Файл: `screens/ui/BookmarkBottomSheet.kt`

BottomSheet для редактирования/создания закладки.

Поля:

- `bookmarkText` — выделенный текст (read-only, очищенный через `cleanBookmarkText`)
- `name` — TextField для имени
- `selectedColor` — выбор цвета из 7 предустановленных

Цвета (`BOOKMARK_COLORS`): красный, оранжевый, жёлтый, зелёный, синий, индиго, фиолетовый.

Кнопки: **Удалить** (красный) | **Сохранить**

---

## BookmarksComposeDialog.kt

Файл: `screens/ui/BookmarksComposeDialog.kt`

AlertDialog со списком закладок книги.

Каждый элемент:

- Номер страницы: `bookmark.start.paragraphIndex + 1`
- Текст: `cleanBookmarkText(bookmark.text).take(100)`
- Имя (если есть)
- Кнопка удаления

Кнопка удаления использует локальный список `bookmarksState` для анимации удаления (
`animateContentSize`) — удаление мгновенное в UI, callback `onDelete()` обновляет реальные данные.

Ключ для LazyColumn: `it.last` (timestamp уникален для каждой закладки).

---

## Жизненный цикл закладки

### Создание

```
1. Пользователь выделяет текст
2. SelectionComposePanel → onBookmark
3. ViewModel.selectionBookmark()
4. fbReaderView.app.runAction(SELECTION_BOOKMARK)
5. FBReaderView создаёт Storage.Bookmark с текстом и позицией
6. listener.onEditBookmark(bookmark) → ViewModel.onAction(EditBookmark)
7. ReaderContent показывает BookmarkBottomSheet
8. Пользователь вводит имя/цвет → Сохранить
9. ViewModel.onAction(AddBookmark(bookmark))
10. ViewModel.addBookmark():
    a. bookmark.coverUrl = book.info?.coverUrl
    b. bookmark.bookFileUri = book.url.toString()
    c. bookmark.sentenceBefore/After = extractSentenceContext()
    d. book.info.bookmarks.add(bookmark)
    e. storage.save(book)
    f. fbReaderView.bookmarksUpdate()
    g. savePosition()
```

### Сохранение

`storage.save(book)` → записывает JSON через `Storage` → файл `{md5}.json` в хранилище.

### Удаление

```
1. BookmarksComposeDialog → onDelete(bookmark)
2. ViewModel.onAction(DeleteBookmark)
3. ViewModel.deleteBookmark():
   a. Ищет по start.samePositionAs()
   b. Удаляет из book.info.bookmarks
   c. Удаляет из fbReaderView.book.info.bookmarks
   d. fbReaderView.bookmarksUpdate()
   e. storage.save(book)
   f. _state = state.copy(book = book)  // триггер рекомпозиции
```

### Миграция контекста (старые заметки)

Заметки созданные до версии с `sentenceBefore/After` не имеют контекста. При открытии книги
запускается `migrateBookmarksContextAsync()`:

```kotlin
fun migrateBookmarksContextAsync() {
    val hasUnmigrated = bookmarks.any {
        it.sentenceBefore == null && it.sentenceAfter == null
    }
    if (!hasUnmigrated) return
    migrateBookmarksContext(currentBook)
}
```

Вызывается после `setWidget()` в `factory{}` (когда FBReaderView полностью инициализирован).

---

## Синхронизация bookmarks (ViewModel ↔ FBook)

FBReader хранит закладки в `FBook.info.bookmarks`. `Storage.Book` хранит их же, но это разные
объекты. При изменении нужна синхронизация:

```kotlin
fun syncBookmarksFromFBook() {
    val fbookBookmarks = fbReaderView?.book?.info?.bookmarks
    if (fbookBookmarks != null) {
        book.info.bookmarks = fbookBookmarks
    }
    _state = state.copy(book = book)  // триггер рекомпозиции
    storage.save(book)
}
```

Вызывается из `listener.onBookmarksUpdate()` → когда FBReader обновляет закладки сам (например после
`SELECTION_BOOKMARK`).
