# План рефакторинга: Интеграция BookReader в основное приложение

## Статус выполнения

| Этап | Статус | Дата завершения |
|------|--------|-----------------|
| **1. Подготовка** | ✅ ЗАВЕРШЁН | 2026-06-19 |
| **2. Миграция ReaderFragment** | ✅ ЗАВЕРШЁН | 2026-06-19 |
| **3. Интеграция** | ✅ ЗАВЕРШЁН | 2026-06-20 |
| **4. Settings** | ✅ ЗАВЕРШЁН | 2026-06-20 |
| **5. Очистка и тестирование** | ✅ ЗАВЕРШЁН | 2026-06-20 |

---

## Проблема

Текущая архитектура имеет следующие проблемы:
1. **Два Application класса**: `SvApp` наследуется от `BookApplication`, что создаёт нежелательную связность
2. **Несколько Activity**: `MainActivity` (основное приложение) и `BookReaderMainActivity` (читалка книг)
3. **Смешивание стилей**: Основное приложение использует Jetpack Compose + MVVM + Hilt, а bookreader использует Fragments + XML
4. **Сложность навигации**: Переход между экранами требует запуска новой Activity через Intent

> **Примечание**: Модуль `bookreader` уже интегрирован в проект (не является внешней библиотекой) и находится в `bookreader/`. Модуль `fbreader/` содержит ядро FBReader (644 файла).

---

# ЭТАП 1: Подготовка и анализ

## 1.1 Создание BookReaderInitializer

### Подэтап 1.1.1: Создание базового класса инициализатора
**Файл**: `bookreader/src/main/java/com/github/axet/bookreader/app/BookReaderInitializer.kt`

**Задачи**:
- [ ] Создать `object BookReaderInitializer` (singleton)
- [ ] Добавить приватные поля: `zlib: ZLAndroidApplication?`, `ttf: TTFManager?`
- [ ] Добавить публичное поле: `isInitialized: Boolean`
- [ ] Создать метод `init(context: Context): Boolean`

**Код**:
```kotlin
object BookReaderInitializer {
    private var zlib: ZLAndroidApplication? = null
    private var ttf: TTFManager? = null

    val isInitialized: Boolean get() = zlib != null

    fun init(context: Context) {
        if (zlib != null) return
        // инициализация...
    }

    fun getTTFManager(): TTFManager? = ttf
}
```

### Подэтап 1.1.2: Перенос логики из BookApplication.onCreate()
**Файл**: `bookreader/src/main/java/com/github/axet/bookreader/app/BookApplication.kt`

**Перенести в BookReaderInitializer**:
```kotlin
// Из BookApplication.onCreate():
zlib = object : ZLAndroidApplication() {
    init {
        attachBaseContext(this@BookApplication)
        onCreate()
    }
}
ttf = TTFManager(this)
val shared = PreferenceManager.getDefaultSharedPreferences(this)
val fonts: String = shared.getString(PREFERENCE_FONTS_FOLDER, "").orEmpty()
if (fonts.isNotEmpty()) {
    val u = fonts.toUri()
    Storage.takePersistableUriPermission(this, u, Storage.SAF_RW)
    ttf.setFolder(u)
}
ttf.preloadFonts()
```

### Подэтап 1.1.3: Обновление BookApplication для использования BookReaderInitializer
**Файл**: `bookreader/src/main/java/com/github/axet/bookreader/app/BookApplication.kt`

**Изменения**:
```kotlin
open class BookApplication : MainApplication() {
    val ttf: TTFManager? get() = BookReaderInitializer.getTTFManager()

    override fun onCreate() {
        super.onCreate()
        BookReaderInitializer.init(this)
    }
}
```

### Подэтап 1.1.4: Создание Hilt модуля для BookReaderInitializer (опционально)
**Файл**: `bookreader/src/main/java/com/github/axet/bookreader/di/BookReaderModule.kt`

**Задачи**:
- [ ] Создать `@Module` и `@InstallIn(SingletonComponent::class)`
- [ ] Предоставить `TTFManager` через `@Provides`
- [ ] Предоставить `SharedPreferences` для настроек читалки

---

## 1.2 Создание ReaderScreen (Modo Screen)

### Подэтап 1.2.1: Создание пакета для новых экранов
**Путь**: `bookreader/src/main/java/com/github/axet/bookreader/screens/`

**Структура**:
```
screens/
├── ReaderScreen.kt              # Modo Screen
├── ReaderContent.kt             # Compose контент
├── viewmodel/
│   ├── ReaderViewModel.kt
│   ├── ReaderState.kt
│   └── ReaderActions.kt
└── ui/
    ├── ReaderTopBar.kt
    ├── ReaderBottomBar.kt
    └── components/
        ├── BookmarksDialog.kt
        ├── TocDialog.kt
        └── FontsPopup.kt
```

### Подэтап 1.2.2: Создание ReaderScreen
**Файл**: `bookreader/src/main/java/com/github/axet/bookreader/screens/ReaderScreen.kt`

**Код**:
```kotlin
@Parcelize
class ReaderScreen(
    private val bookUri: Uri,
    private val position: ZLTextIndexPosition? = null,
    override val screenKey: ScreenKey = generateScreenKey(),
) : Screen {

    @Composable
    override fun Content(modifier: Modifier) {
        SVAPPTheme {
            ReaderContent(
                bookUri = bookUri,
                initialPosition = position,
                modifier = modifier,
            )
        }
    }
}
```

### Подэтап 1.2.3: Создание заглушки ReaderContent
**Файл**: `bookreader/src/main/java/com/github/axet/bookreader/screens/ReaderContent.kt`

**Код**:
```kotlin
@Composable
fun ReaderContent(
    bookUri: Uri,
    initialPosition: ZLTextIndexPosition?,
    modifier: Modifier = Modifier,
) {
    // Заглушка - будет реализована в Этапе 2
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("Reader will be here")
    }
}
```

---

# ЭТАП 2: Миграция ReaderFragment в Compose

## 2.1 Создание ReaderViewModel

### Подэтап 2.1.1: Определение состояний (ReaderState)
**Файл**: `bookreader/src/main/java/com/github/axet/bookreader/screens/viewmodel/ReaderState.kt`

**Код**:
```kotlin
sealed class ReaderState {
    object Loading : ReaderState()
    data class Content(
        val book: Storage.Book,
        val position: ZLTextPosition,
        val isFavorite: Boolean = false,
        val isFullscreen: Boolean = false,
        val showToc: Boolean = false,
        val showBookmarks: Boolean = false,
        val showFontSettings: Boolean = false,
        val ttsEnabled: Boolean = false,
        val rtlMode: Boolean = false,
        val viewMode: ViewMode = ViewMode.PAGING,
    ) : ReaderState()
    data class Error(val message: String) : ReaderState()
}

enum class ViewMode {
    PAGING, CONTINUOUS
}
```

### Подэтап 2.1.2: Определение действий (ReaderActions)
**Файл**: `bookreader/src/main/java/com/github/axet/bookreader/screens/viewmodel/ReaderActions.kt`

**Код**:
```kotlin
sealed class ReaderActions {
    data class LoadBook(val uri: Uri, val position: ZLTextIndexPosition?) : ReaderActions()
    data class SavePosition(val position: ZLTextPosition) : ReaderActions()
    object ToggleFullscreen : ReaderActions()
    object ToggleToc : ReaderActions()
    object ToggleBookmarks : ReaderActions()
    object ToggleFontSettings : ReaderActions()
    object ToggleTts : ReaderActions()
    object ToggleRtl : ReaderActions()
    object ToggleViewMode : ReaderActions()
    data class GoToPosition(val position: ZLTextPosition) : ReaderActions()
    data class AddBookmark(val bookmark: Storage.Bookmark) : ReaderActions()
    data class DeleteBookmark(val bookmark: Storage.Bookmark) : ReaderActions()
    data class SetFontSize(val size: Int) : ReaderActions()
    data class SetFontFamily(val family: String) : ReaderActions()
}
```

### Подэтап 2.1.3: Создание ReaderViewModel
**Файл**: `bookreader/src/main/java/com/github/axet/bookreader/screens/viewmodel/ReaderViewModel.kt`

**Задачи**:
- [ ] Создать `@HiltViewModel`
- [ ] Инжектировать `Storage`, `Context`
- [ ] Реализовать `StateFlow<ReaderState>`
- [ ] Реализовать `fun onAction(action: ReaderActions)`
- [ ] Перенести логику сохранения позиции из `ReaderFragment.savePosition()`
- [ ] Реализовать загрузку книги

**Ключевые методы для миграции из ReaderFragment**:
```kotlin
// Из ReaderFragment:
- loadBook() -> ViewModel.loadBook()
- savePosition() -> ViewModel.savePosition()
- updateToolbar() -> ViewModel.updateState()
```

---

## 2.2 Создание ReaderContent (Compose UI)

### Подэтап 2.2.1: Интеграция FBReaderView через AndroidView
**Файл**: `bookreader/src/main/java/com/github/axet/bookreader/screens/ReaderContent.kt`

**Код**:
```kotlin
@Composable
fun ReaderContent(
    bookUri: Uri,
    initialPosition: ZLTextIndexPosition?,
    modifier: Modifier = Modifier,
    viewModel: ReaderViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Состояние для FBReaderView
    var fbReaderView by remember { mutableStateOf<FBReaderView?>(null) }

    Scaffold(
        topBar = {
            if (state is ReaderState.Content && !(state as ReaderState.Content).isFullscreen) {
                ReaderTopBar(state as ReaderState.Content, viewModel)
            }
        },
    ) { paddingValues ->
        when (state) {
            is ReaderState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is ReaderState.Content -> {
                AndroidView(
                    factory = { ctx ->
                        FBReaderView(ctx).apply {
                            setWindow((context as? Activity)?.window)
                            setActivity(context as? Activity, viewModel.onBookPagerManager)
                            fbReaderView = this
                        }
                    },
                    modifier = modifier.padding(paddingValues),
                    update = { view ->
                        // Обновление состояния
                    }
                )
            }
            is ReaderState.Error -> {
                // Обработка ошибки
            }
        }
    }
}
```

### Подэтап 2.2.2: Создание ReaderTopBar
**Файл**: `bookreader/src/main/java/com/github/axet/bookreader/screens/ui/ReaderTopBar.kt`

**Элементы меню (из menu/main.xml)**:
| ID | Icon | Действие |
|----|------|----------|
| action_search | ic_search | Поиск по тексту |
| action_toc | ic_toc | Содержание (TOC) |
| action_bm | ic_bookmark | Закладки |
| action_mode | ic_view_day | Режим просмотра (постраничный/непрерывный) |
| action_rtl | ic_gesture | RTL/LTR |
| action_fontsize | ic_format_size | Размер шрифта |
| action_reflow | ic_wrap_text | Reflow (для PDF) |
| action_tts | - | Text-to-Speech |

**Код**:
```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderTopBar(
    state: ReaderState.Content,
    viewModel: ReaderViewModel,
) {
    TopAppBar(
        title = { Text(state.book.info.title) },
        navigationIcon = {
            IconButton(onClick = { viewModel.onAction(ReaderActions.NavigateBack) }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
            }
        },
        actions = {
            // TOC
            IconButton(onClick = { viewModel.onAction(ReaderActions.ToggleToc) }) {
                Icon(painterResource(R.drawable.ic_toc_white_24dp), "TOC")
            }
            // Bookmarks
            IconButton(onClick = { viewModel.onAction(ReaderActions.ToggleBookmarks) }) {
                Icon(painterResource(R.drawable.ic_bookmark_white_24dp), "Bookmarks")
            }
            // Font size
            IconButton(onClick = { viewModel.onAction(ReaderActions.ToggleFontSettings) }) {
                Icon(painterResource(R.drawable.ic_format_size_white_24dp), "Font")
            }
            // More options
            var showMenu by remember { mutableStateOf(false) }
            IconButton(onClick = { showMenu = true }) {
                Icon(Icons.Default.MoreVert, "More")
            }
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(text = { Text("TTS") }, onClick = {
                    viewModel.onAction(ReaderActions.ToggleTts)
                    showMenu = false
                })
                DropdownMenuItem(text = { Text("Settings") }, onClick = {
                    // Navigate to settings
                    showMenu = false
                })
            }
        }
    )
}
```

### Подэтап 2.2.3: Создание компонентов диалогов

#### BookmarksDialog Compose
**Файл**: `bookreader/src/main/java/com/github/axet/bookreader/screens/ui/components/BookmarksDialog.kt`

**Миграция из**: `widgets/BookmarksDialog.kt`

**Задачи**:
- [ ] Создать `@Composable fun BookmarksDialog()`
- [ ] Использовать `LazyColumn` вместо `TreeRecyclerView`
- [ ] Реализовать контекстное меню (редактировать, удалить, поделиться)

#### TocDialog Compose
**Файл**: `bookreader/src/main/java/com/github/axet/bookreader/screens/ui/components/TocDialog.kt`

**Миграция из**: `ReaderFragment.showTOC()` и `TOCAdapter`

**Задачи**:
- [ ] Создать `@Composable fun TocDialog()`
- [ ] Использовать `LazyColumn` с вложенностью
- [ ] Реализовать переход к главе

#### FontsPopup Compose
**Файл**: `bookreader/src/main/java/com/github/axet/bookreader/screens/ui/components/FontsPopup.kt`

**Миграция из**: `widgets/FontsPopup.kt`

**Задачи**:
- [ ] Создать `@Composable fun FontsBottomSheet()`
- [ ] Слайдер для размера шрифта
- [ ] Список шрифтов из `TTFManager`
- [ ] Чекбокс "Игнорировать встроенные шрифты"

---

## 2.3 Обработка жестов и клавиш

### Подэтап 2.3.1: Обработка клавиш громкости
**Файл**: `bookreader/src/main/java/com/github/axet/bookreader/screens/ReaderContent.kt`

**Миграция из**: `ReaderFragment.onKeyDown()/onKeyUp()`

**Код**:
```kotlin
@Composable
fun ReaderContent(...) {
    val view = LocalView.current

    // Обработка клавиш громкости
    DisposableEffect(Unit) {
        val keyEventDispatcher = View.OnKeyListener { _, keyCode, event ->
            if (viewModel.volumeKeysEnabled) {
                when {
                    keyCode == KeyEvent.KEYCODE_VOLUME_DOWN && event.action == KeyEvent.ACTION_DOWN -> {
                        fbReaderView?.app?.runAction(ActionCode.VOLUME_KEY_SCROLL_FORWARD)
                        true
                    }
                    keyCode == KeyEvent.KEYCODE_VOLUME_UP && event.action == KeyEvent.ACTION_DOWN -> {
                        fbReaderView?.app?.runAction(ActionCode.VOLUME_KEY_SCROLL_BACK)
                        true
                    }
                    else -> false
                }
            } else false
        }
        view.setFocusableInTouchMode(true)
        view.setOnKeyListener(keyEventDispatcher)
        onDispose {
            view.setOnKeyListener(null)
        }
    }
}
```

### Подэтап 2.3.2: Сохранение позиции при выходе
**Задачи**:
- [ ] Вызвать `viewModel.savePosition()` в `DisposableEffect`
- [ ] Сохранять позицию каждые 60 секунд (как в ReaderFragment)

---

# ЭТАП 3: Интеграция с основным приложением

## 3.1 Изменение навигации в books модуле

### Подэтап 3.1.1: Создание навигационного интерфейса
**Файл**: `managers/src/main/java/su/sv/managers/BookNavigator.kt`

**Код**:
```kotlin
interface BookNavigator {
    fun openBook(uri: Uri, position: ZLTextIndexPosition? = null)
}
```

### Подэтап 3.1.2: Изменение BookDetailUi
**Файл**: `books/src/main/java/su/sv/books/catalog/presentation/detail/ui/BookDetailUi.kt`

**До**:
```kotlin
private fun openBook(context: Context, uiBook: UiBook) {
    val intent = Intent(context, BookReaderMainActivity::class.java).apply {
        action = Intent.ACTION_VIEW
        data = uiBook.fileUri
    }
    context.startActivity(intent)
}
```

**После**:
```kotlin
private fun openBook(navController: NavController, uiBook: UiBook) {
    navController.forward(ReaderScreen(bookUri = uiBook.fileUri))
}
```

### Подэтап 3.1.3: Изменение RootBooksCatalog
**Файл**: `books/src/main/java/su/sv/books/catalog/presentation/root/ui/RootBooksCatalog.kt`

**Аналогичные изменения** для открытия книг из списка.

---

## 3.2 Удаление BookReaderMainActivity

### Подэтап 3.2.1: Удаление из манифеста
**Файл**: `bookreader/src/main/AndroidManifest.xml`

**Удалить**:
```xml
<activity
    android:name=".activities.BookReaderMainActivity"
    android:configChanges="keyboardHidden|orientation|screenSize"
    android:exported="false"
    android:label="@string/reader_app_name"
    android:launchMode="singleTop"
    android:theme="@style/ReaderTheme"
    android:windowSoftInputMode="adjustNothing">
</activity>
```

### Подэтап 3.2.2: Удаление файла
**Файл**: `bookreader/src/main/java/com/github/axet/bookreader/activities/BookReaderMainActivity.kt`

**Статус**: Удалить файл полностью

**Примечание**: Код из этого файла перенесён в `ReaderViewModel` и `ReaderContent`

---

## 3.3 Удаление наследования SvApp от BookApplication

### Подэтап 3.3.1: Изменение SvApp
**Файл**: `app/src/main/java/su/sv/app/SvApp.kt`

**До**:
```kotlin
@HiltAndroidApp
class SvApp : BookApplication(), SingletonImageLoader.Factory, HasTracerConfiguration {
```

**После**:
```kotlin
@HiltAndroidApp
class SvApp : Application(), SingletonImageLoader.Factory, HasTracerConfiguration {

    override fun onCreate() {
        super.onCreate()
        AndroidThreeTen.init(this)
        Timber.plant(Timber.DebugTree())

        // Инициализация BookReader
        BookReaderInitializer.init(this)
    }
}
```

### Подэтап 3.3.2: Проверка всех использований BookApplication.from()
**Команда**:
```bash
grep -r "BookApplication.from\|BookApplication\." --include="*.kt" .
```

**Заменить на**:
- `BookApplication.from(context)?.ttf` → `BookReaderInitializer.getTTFManager()`

### Подэтап 3.3.3: Удаление или депрекация BookApplication
**Файл**: `bookreader/src/main/java/com/github/axet/bookreader/app/BookApplication.kt`

**Вариант 1**: Удалить файл (рекомендуется)
**Вариант 2**: Добавить `@Deprecated` и оставить для совместимости с внешним кодом

---

# ЭТАП 4: Миграция SettingsActivity

## 4.1 Анализ настроек

### Подэтап 4.1.1: Изучить PreferenceScreen
**Файл**: `bookreader/src/main/res/xml/pref*.xml`

**Найти**:
- [ ] Список всех настроек
- [ ] Ключи настроек (используются в `BookApplication.PREFERENCE_*`)

### Подэтап 4.1.2: Создать модель настроек
**Файл**: `bookreader/src/main/java/com/github/axet/bookreader/screens/settings/ReaderSettings.kt`

**Код**:
```kotlin
data class ReaderSettings(
    val theme: Theme = Theme.System,
    val fontSize: Int = 16,
    val fontFamily: String = "",
    val viewMode: ViewMode = ViewMode.PAGING,
    val screenLock: Boolean = false,
    val volumeKeys: Boolean = false,
    val rotate: Boolean = false,
    val ignoreEmbeddedFonts: Boolean = false,
)
```

## 4.2 Создание SettingsScreen

### Подэтап 4.2.1: Создать ReaderSettingsScreen
**Файл**: `bookreader/src/main/java/com/github/axet/bookreader/screens/ReaderSettingsScreen.kt`

### Подэтап 4.2.2: Создать ReaderSettingsContent
**Файл**: `bookreader/src/main/java/com/github/axet/bookreader/screens/settings/ReaderSettingsContent.kt`

**Элементы**:
- [ ] Выбор темы (Light/Dark/System)
- [ ] Размер шрифта (слайдер)
- [ ] Семейство шрифта (dropdown)
- [ ] Режим просмотра (переключатель)
- [ ] Блокировка экрана
- [ ] Клавиши громкости
- [ ] Автоповорот

---

# ЭТАП 5: Очистка и тестирование

## 5.1 Удаление неиспользуемого кода

### Подэтап 5.1.1: Удалить LibraryFragment
**Причина**: Не используется в SvApp (каталог книг находится в модуле `books`)

**Файлы для удаления**:
- `bookreader/src/main/java/com/github/axet/bookreader/fragments/LibraryFragment.kt`
- `bookreader/src/main/res/layout/fragment_library.xml`
- `bookreader/src/main/res/layout/book_item.xml`
- `bookreader/src/main/res/layout/book_list_item.xml`

### Подэтап 5.1.2: Удалить FullscreenActivity
**Причина**: Полноэкранный режим будет управляться через ViewModel

**Файлы для удаления**:
- `bookreader/src/main/java/com/github/axet/bookreader/activities/FullscreenActivity.kt`

### Подэтап 5.1.3: Удалить SettingsActivity
**Причина**: Заменён на Compose SettingsScreen

**Файлы для удаления**:
- `bookreader/src/main/java/com/github/axet/bookreader/activities/SettingsActivity.kt`

### Подэтап 5.1.4: Очистить манифест
**Файл**: `bookreader/src/main/AndroidManifest.xml`

**Удалить все Activity** (кроме провайдеров)

---

## 5.2 Чек-лист тестирования

### Функциональное тестирование
- [ ] Открытие книги из списка книг
- [ ] Открытие книги из деталей книги
- [ ] Отображение контента книги
- [ ] Перелистывание страниц (свайп, тап)
- [ ] Прокрутка (непрерывный режим)
- [ ] Переход к главе через TOC
- [ ] Добавление закладки
- [ ] Удаление закладки
- [ ] Переход к закладке
- [ ] Изменение размера шрифта
- [ ] Изменение шрифта
- [ ] Поиск по тексту
- [ ] TTS (Text-to-Speech)
- [ ] RTL режим
- [ ] Сохранение позиции при выходе
- [ ] Восстановление позиции при входе
- [ ] Клавиши громкости для навигации
- [ ] Полноэкранный режим
- [ ] Тема (Light/Dark)

### Тестирование навигации
- [ ] Назад из читалки → возврат в список книг
- [ ] Открытие читалки → корректный стек навигации
- [ ] Deeplink на книгу → корректное открытие

### Тестирование на устройствах
- [ ] Телефон (портрет)
- [ ] Телефон (ландшафт)
- [ ] Планшет
- [ ] Android 8-10
- [ ] Android 11-13
- [ ] Android 14+

---

## Риски и митигация

| Риск | Вероятность | Влияние | Митигация |
|------|-------------|---------|-----------|
| FBReaderView не работает в AndroidView | Средняя | Высокое | Создать промежуточный FragmentWrapper |
| Потеря состояния при повороте | Средняя | Среднее | Использовать ViewModel + rememberSaveable |
| Проблемы с жестами в Compose | Низкая | Среднее | Тестирование на разных устройствах |
| Регрессия в функциональности | Средняя | Высокое | Чек-лист тестирования после каждого этапа |
| TTFManager требует Context приложения | Высокая | Низкое | Передавать Application context в init() |

---

## Зависимости между этапами

```
Этап 1.1 (BookReaderInitializer) ──┬──► Этап 2.1 (ReaderViewModel)
                                   │
Этап 1.2 (ReaderScreen) ───────────┘

Этап 2 (ReaderFragment → Compose)
    ↓
Этап 3.1 (Навигация books)
    ↓
Этап 3.2 (Удаление BookReaderMainActivity)
    ↓
Этап 3.3 (SvApp без BookApplication)
    ↓
Этап 4 (Settings) ← параллельно с Этапом 3
    ↓
Этап 5 (Очистка)
```

---

## Оценка времени

| Этап | Подэтапы | Время |
|------|----------|-------|
| 1.1 | BookReaderInitializer | 4-6 часов |
| 1.2 | ReaderScreen заглушка | 2-3 часа |
| 2.1 | ReaderViewModel | 1-2 дня |
| 2.2 | ReaderContent + TopBar | 1-2 дня |
| 2.3 | Диалоги (Bookmarks, TOC, Fonts) | 1 день |
| 2.4 | Жесты и клавиши | 4-6 часов |
| 3.1 | Навигация books | 2-4 часа |
| 3.2 | Удаление BookReaderMainActivity | 1 час |
| 3.3 | SvApp без BookApplication | 2-4 часа |
| 4 | Settings | 1-2 дня |
| 5 | Очистка + тестирование | 2-3 дня |
| **Итого** | | **9-15 дней** |

---

## Результат

После завершения рефакторинга:

1. ✅ **Одна Activity**: Только `MainActivity`
2. ✅ **Один Application**: `SvApp` наследуется от `Application`
3. ✅ **Единая навигация**: Modo для всех экранов
4. ✅ **Единый стек**: Compose + MVVM + Hilt
5. ✅ **Чистая архитектура**: Легче поддерживать и развивать
