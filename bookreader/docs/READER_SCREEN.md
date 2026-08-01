# Экран чтения книги

## ReaderScreen.kt

Файл: `screens/ReaderScreen.kt`

Modo `Screen` — точка входа в модуль. Parcelable для передачи через navigation stack.

```kotlin
@Parcelize
class ReaderScreen(
    val bookUri: Uri,
    val bookCoverUrl: String? = null,   // из API для сохранения в заметках
    val bookTitle: String? = null,      // из API (перезаписывает метаданные файла)
    val bookAuthor: String? = null,     // из API
    val bookmarkPosition: BookmarkPosition? = null,  // позиция заметки
    override val screenKey: ScreenKey = generateScreenKey(),
) : Screen, Parcelable
```

### BookmarkPosition
```kotlin
@Parcelize
data class BookmarkPosition(
    val startParagraph: Int,
    val startElement: Int,
    val startChar: Int,
    val endParagraph: Int,
    val endElement: Int,
    val endChar: Int,
) : Parcelable
```

Содержит полный адрес позиции в книге (аналог `ZLTextPosition`). Передаётся через Parcel чтобы не зависеть от FBReader-типов в публичном API.

---

## ReaderContent.kt

Файл: `screens/ReaderContent.kt`

Главный Compose-контент читалки. Занимает ~950 строк.

### Параметры
```kotlin
@Composable
fun ReaderContent(
    bookUri: Uri,
    bookCoverUrl: String?,
    bookTitle: String?,
    bookAuthor: String?,
    bookmarkPosition: BookmarkPosition?,
    onNavigateBack: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: ReaderViewModel = hiltViewModel(),
    themeViewModel: ThemeViewModel = hiltViewModel(),
)
```

### Структура UI

```
ReaderContent
├── LaunchedEffect(bookUri) → LoadBook action
├── BatteryReceiver
├── VolumeKeysHandler
├── DisposableEffect → savePosition() при выходе
├── ReaderState.Loading → CircularProgressIndicator
├── ReaderState.Error → Text
└── ReaderState.Content →
    ├── TocComposeDialog (если showToc)
    ├── BookmarksComposeDialog (если showBookmarks)
    ├── FontsComposeBottomSheet (если showFontSettings)
    ├── NavigationComposeDialog (если showNavigation)
    ├── BookmarkBottomSheet (если showBookmarkEdit)
    └── Scaffold(topBar = ReaderTopBar) {
            Box {
                AndroidView(FBReaderView)
                SelectionComposePanel (если showSelection)
            }
            BackHandler(isFullscreen → exitFullscreen)
            BackHandler(isInZoom → resetZoom)
            BackHandler(searchState.isActive → searchClose)
        }
```

### AndroidView factory

```kotlin
AndroidView(
    factory = { ctx ->
        FBReaderView(ctx).apply {
            // 1. Устанавливаем listener
            listener = object : FBReaderView.Listener { ... }

            // 2. Устанавливаем window и activity
            setWindow(context.window)
            setActivity(context, viewModel.getOnBookPagerManager())

            // 3. Сохраняем ссылку
            fbReaderView = this
            viewModel.fbReaderView = this

            // 4. Загружаем книгу
            val fbook = viewModel.getFBook()
            loadBook(fbook)

            // 5. Режим просмотра
            setWidget(if (viewMode == CONTINUOUS) CONTINUOUS else PAGING)

            // 6. Позиция закладки (если открываем на заметке)
            val savedPos = viewModel.getSavedPosition()
            if (savedPos != null) {
                viewModel.clearSavedPosition()
                post { gotoPosition(savedPos) }  // через post!
            }

            // 7. Обновляем canChangeFont
            viewModel.updateCanChangeFont()

            // 8. Миграция контекста старых заметок
            viewModel.migrateBookmarksContextAsync()
        }
    },
    update = { view ->
        // Не пересоздаём, только обновляем если нужно
        val savedPos = viewModel.getSavedPosition()
        if (savedPos != null && view.width > 0) {
            view.post { view.gotoPosition(savedPos) }
            viewModel.clearSavedPosition()
        }

        // Переключаем widget только если изменился режим
        val currentWidget = view.getWidgetType()
        if (currentWidget != desiredWidget) {
            view.setWidget(desiredWidget)
        }

        // Показываем подсказки зон первый раз
        if (!currentState.hasShownControlsHint && view.width > 0) {
            view.postDelayed({ view.showControls() }, 300)
        }
    }
)
```

**Критически важно**: позиция закладки применяется через `post{}` в factory, а не в update. Это гарантирует, что view имеет размеры и книга загружена.

### FBReaderView.Listener в ReaderContent

| Callback | Действие |
|---|---|
| `onScrollingFinished` | `viewModel.savePosition()` |
| `onBookmarksUpdate` | `viewModel.syncBookmarksFromFBook()` |
| `ttsStatus(speaking)` | `viewModel.volumeKeysEnabled = !speaking` |
| `onEditBookmark` | `viewModel.onAction(EditBookmark)` |
| `onFullscreenToggle` | `viewModel.onAction(SetFullscreen)` |
| `onNavigationRequest` | `viewModel.onAction(ToggleNavigation)` |
| `onSelectionShow` | `viewModel.onAction(ShowSelection(startY, endY))` |
| `onSelectionHide` | `viewModel.hideSelection()` |

### TocComposeDialog

Диалог оглавления с раскрываемой иерархией:
- `collectExpandableTocItems()` — рекурсивно собирает все элементы TOC
- `ExpandableTocItem(id, title, position, level, hasChildren, parentId)`
- `expandedStates: SnapshotStateList<String>` — список раскрытых элементов
- `isItemVisible(item)` — проверяет, видим ли элемент (все родители раскрыты)
- `AnimatedVisibility` с `expandVertically` / `shrinkVertically` для анимации

### FontsComposeBottomSheet

BottomSheet настроек шрифтов:
- Slider для размера шрифта (8..48)
- LazyColumn со списком шрифтов (200dp высота)
- Switch "Игнорировать встроенные шрифты"
- Список шрифтов: системные (`sans-serif`, `serif`, `monospace`) + из `TTFManager`

### BatteryReceiver
`DisposableEffect` регистрирует `BroadcastReceiver` для `ACTION_BATTERY_CHANGED`. Обновляет `fbReaderView.battery` и `fbReaderView.invalidateFooter()`.

### VolumeKeysHandler
`DisposableEffect` устанавливает `View.OnKeyListener` если `PREFERENCE_VOLUME_KEYS = true`:
- `VOLUME_DOWN` → `VOLUME_KEY_SCROLL_FORWARD`
- `VOLUME_UP` → `VOLUME_KEY_SCROLL_BACK`
- Не работает если TTS активен (`viewModel.volumeKeysEnabled = false`)

---

## ReaderViewModel.kt

Файл: `screens/viewmodel/ReaderViewModel.kt`

```kotlin
@HiltViewModel
class ReaderViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val onBookPagerManager: OnBookPagerManager,
) : ViewModel()
```

### Поля состояния
```kotlin
private val _state = MutableStateFlow<ReaderState>(ReaderState.Loading)
val state: StateFlow<ReaderState>

private var storage: Storage
private var currentBook: Storage.Book?
private var currentFBook: Storage.FBook?
var fbReaderView: FBReaderView?   // ссылка из Compose (не через DI!)
private var savedPosition: FBReaderView.ZLTextIndexPosition?
var volumeKeysEnabled: Boolean
private var lastSelectionShowTime: Long  // для debounce
```

### Загрузка книги (loadBook)
```kotlin
viewModelScope.launch {
    _state.value = ReaderState.Loading

    // Если передана позиция закладки - сохраняем
    if (position != null) savedPosition = position

    // Проверяем доступность файла
    context.contentResolver.openInputStream(uri)

    // Загружаем метаданные
    currentBook = storage.load(uri)

    // Перезаписываем метаданные из API (если переданы)
    if (bookCoverUrl != null) currentBook.info.coverUrl = bookCoverUrl
    if (bookTitle != null) currentBook.info.title = bookTitle
    if (bookAuthor != null) currentBook.info.authors = bookAuthor

    // Открываем файл
    currentFBook = storage.read(currentBook)

    // Создаём обложку если нужно
    ensureCoverCreated(currentBook, currentFBook)

    _state.value = ReaderState.Content(book = currentBook, ...)
}
```

### Сохранение позиции (savePosition)
```kotlin
fun savePosition() {
    savedPosition = fb.position as? ZLTextIndexPosition  // сохраняем для восстановления

    val save = Storage.RecentInfo(fbBook.info)
    save.position = fb.position

    val uri = storage.recentUri(book)
    if (Storage.exists(context, uri)) {
        val info = Storage.RecentInfo(context, uri)
        // Проверка конфликтов (если файл изменился между сохранениями)
        if (book.info.last != info.last) {
            storage.move(uri, storage.storagePath)
        }
        save.merge(info.fontsizes, info.last)
    }

    book.info = save
    storage.save(book)
}
```

### goToBookmark
```kotlin
private fun goToBookmark(bookmark: Storage.Bookmark) {
    fbReaderView?.gotoPosition(
        FBReaderView.ZLTextIndexPosition(bookmark.start, bookmark.end)
    )
}
```

Использует `ZLTextIndexPosition` который содержит и start и end позиции заметки.

### Debounce для showSelection
Константа `SELECTION_DEBOUNCE_MS = 500` мс — минимальное время между show и hide. Предотвращает мигание панели при создании выделения (сначала идёт hide от предыдущего, потом show для нового).

---

## ReaderState.kt

Файл: `screens/viewmodel/ReaderState.kt`

```kotlin
sealed class ReaderState {
    object Loading : ReaderState()

    data class Content(
        val book: Storage.Book,
        val positionText: String,
        val isFullscreen: Boolean,
        val showToc: Boolean,
        val showBookmarks: Boolean,
        val showFontSettings: Boolean,
        val showBookmarkEdit: Boolean,
        val editingBookmark: Storage.Bookmark?,
        val showNavigation: Boolean,
        val showSelection: Boolean,
        val selectionStartY: Int,
        val selectionEndY: Int,
        val viewMode: ViewMode,     // PAGING | CONTINUOUS
        val isReflow: Boolean,
        val canChangeFont: Boolean,  // false для PDF без reflow
        val hasShownControlsHint: Boolean,
        val zoomScale: Float,        // 1.0 = нормальный
        val zoomPivotX: Float,
        val zoomPivotY: Float,
        val isInZoom: Boolean,
        val searchState: SearchState,
    ) : ReaderState()

    data class Error(val message: String) : ReaderState()
}

enum class ViewMode { PAGING, CONTINUOUS }
```

**Важно**: Только один диалог может быть открыт одновременно. При открытии каждого диалога остальные закрываются:
```kotlin
private fun toggleBookmarks() {
    _state.value = currentState.copy(
        showToc = false,
        showBookmarks = !currentState.showBookmarks,
        showFontSettings = false,
        showNavigation = false
    )
}
```

---

## ReaderActions.kt

Файл: `screens/viewmodel/ReaderActions.kt`

Полный список действий:

**Загрузка и навигация:**
- `LoadBook(uri, position?, bookCoverUrl?, bookTitle?, bookAuthor?)`
- `SavePosition`
- `GoToPosition(position)`
- `NavigateBack`
- `NavigateToSettings`
- `GoToBookmark(bookmark)`

**Отображение:**
- `ToggleFullscreen`
- `SetFullscreen(isFullscreen)`
- `ToggleViewMode`
- `ToggleReflow`
- `MarkControlsHintShown`

**Диалоги:**
- `ToggleToc`
- `ToggleBookmarks`
- `ToggleFontSettings`
- `ToggleNavigation`
- `GoToPage(page)`
- `HideDialogs`

**Выделение:**
- `ShowSelection(startY, endY)`
- `HideSelection`
- `SelectionCopy`
- `SelectionShare`
- `SelectionBookmark`
- `SelectionQuestion`
- `SelectionAlert`

**Закладки:**
- `EditBookmark(bookmark)`
- `SaveBookmarkEdit(bookmark, name, color)`
- `AddBookmark(bookmark)`
- `DeleteBookmark(bookmark)`

**Шрифты:**
- `SetFontSize(size)`
- `SetReflowFontSize(size)`
- `SetFontFamily(family)`
- `SetIgnoreEmbeddedFonts(ignore)`

**Поиск:**
- `Search(query)` — запускает поиск (мин. 2 символа)
- `SearchNext`
- `SearchPrevious`
- `SearchClose`

**Zoom:**
- `ZoomUpdate(scale, pivotX, pivotY)`
- `ZoomReset`

---

## SearchState.kt

Файл: `screens/viewmodel/SearchState.kt`

```kotlin
data class SearchState(
    val isActive: Boolean = false,    // активен ли поиск
    val query: String = "",           // текущий запрос
    val resultsCount: Int = 0,        // количество результатов
    val currentResultIndex: Int = 0,  // текущий индекс (0-based)
    val isLoading: Boolean = false,   // поиск в процессе
)
```

Поиск запускается при `query.length >= 2`. При `query < 2` символов поиск не выполняется, `isLoading = false`.

---

## ReaderTopBar.kt

Файл: `screens/ui/ReaderTopBar.kt`

Два режима:
1. **Normal**: title книги + кнопки (Search, TOC, Bookmarks, Font, More)
2. **Search**: поле поиска + счётчик результатов + стрелки вперёд/назад

Кнопка Шрифт показывается только если `state.canChangeFont = true` (false для PDF без reflow).

Меню "More" содержит:
- Переключение режима просмотра (Paging ↔ Continuous)
- Переключение Reflow (Original ↔ Reflow text)
- Настройки

---

## ReaderSettingsScreen.kt / ReaderSettingsContent.kt

Файлы: `screens/ReaderSettingsScreen.kt`, `screens/ReaderSettingsContent.kt`

Экран настроек читалки (отдельный Modo Screen). Содержит настройки:
- Папка шрифтов
- Блокировка экрана
- Клавиши громкости
- Поворот экрана
- Хранилище
- TTS язык
- Двухстраничный режим
