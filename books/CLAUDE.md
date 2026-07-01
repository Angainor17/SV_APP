# Books Module

Модуль каталога книг.

## Обзор

Модуль `books` отвечает за отображение каталога книг, детальную информацию о книге, скачивание файлов и управление заметками.

## Архитектура

```
catalog/
├── data/
│   ├── api/
│   │   ├── BooksApi.kt          # API интерфейс
│   │   └── BooksApiMock.kt      # Мок для тестирования
│   ├── models/ApiBook.kt        # Модель книги от API
│   ├── repo/
│   │   ├── RemoteBooksRepo.kt   # Репозиторий книг
│   │   └── BookDownloadRepository.kt  # Репозиторий скачивания
│   └── receivers/
│       ├── BookDownloadBroadcastReceiver.kt
│       └── BookDownloadedActionHandler.kt
├── domain/
│   ├── GetBooksListUseCase.kt   # Получение списка
│   ├── DownloadBookUseCase.kt   # Скачивание книги
│   ├── GetBookUriUseCase.kt     # Получение URI файла
│   └── model/
│       ├── Book.kt              # Модель книги
│       └── BookmarkNote.kt      # Модель заметки с cleanBookmarkText()
├── presentation/
│   ├── CommonDownloadBookStates.kt
│   ├── base/BaseBookViewModel.kt
│   ├── root/                    # Список книг
│   ├── detail/                  # Детали книги
│   └── bookmarks/               # Заметки
│       ├── mapper/UiBookmarkMapper.kt  # Маппер с очисткой текста
│       ├── ui/
│       │   ├── NoteItem.kt      # Элемент заметки
│       │   └── BookWithNotesItem.kt
│       └── viewmodel/BookmarksViewModel.kt
└── di/BooksApiModule.kt
```

## Основные компоненты

### BooksApi
API для получения списка книг:

```kotlin
interface BooksApi {
    suspend fun getBooks(): List<ApiBook>
}
```

### RemoteBooksRepo
Репозиторий для работы с книгами:

```kotlin
class RemoteBooksRepo @Inject constructor(
    private val api: BooksApi
) {
    suspend fun getBooks(): Result<List<Book>>
}
```

### BookDownloadRepository
Управление скачиванием:

```kotlin
class BookDownloadRepository @Inject constructor() {
    fun download(context, book: Book)
    fun getDownloadState(bookId: String): Flow<UIBookState>
}
```

### RootBooksCatalogViewModel
ViewModel списка книг:

```kotlin
class RootBooksCatalogViewModel : BaseBookViewModel() {
    val state: StateFlow<UiBooksState>
    fun onAction(action: BooksActions)
}
```

### BookDetailViewModel
ViewModel детальной информации:

```kotlin
class BookDetailViewModel : BaseBookViewModel() {
    val state: StateFlow<UiBookDetailState>
    fun onAction(action: DetailBookActions)
}
```

## Модели

### Book
Доменная модель книги:

```kotlin
data class Book(
    val id: String,
    val title: String,
    val author: String,
    val description: String,
    val image: String,
    val downloadUrl: String,
    val fileName: String,
    val category: String
)
```

### BookmarkNote
Доменная модель заметки из книги:

```kotlin
data class BookmarkNote(
    val id: String,
    val bookId: String,
    val bookTitle: String,
    val bookAuthor: String,
    val bookCoverUrl: String,
    val bookFileUri: String?,
    val text: String,           // Текст заметки
    val name: String?,          // Название заметки (опционально)
    val page: Int,
    val createdAt: Long,
    val startParagraph: Int,    // Позиция для навигации
    val startElement: Int,
    val startChar: Int,
    val endParagraph: Int,
    val endElement: Int,
    val endChar: Int,
)
```

### UiBookDetailState
Состояние экрана деталей:

```kotlin
sealed class UiBookDetailState {
    object Loading : UiBookDetailState()
    data class Content(val book: UiBook) : UiBookDetailState()
    data class Error(val message: String) : UiBookDetailState()
}
```

## Заметки

### Функция очистки текста

FBReader вставляет специальные символы в текст заметок:
- `U+FFFE` (65534) — маркер переноса слов
- Управляющие символы

Функция `cleanBookmarkText()` в domain слое очищает текст:

```kotlin
// BookmarkNote.kt
fun cleanBookmarkText(text: String): String {
    return text
        .replace(Regex("[\\uFFFE\\uFFFF]"), "")  // FBReader markers
        .replace(Regex("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]"), "")  // Control chars
        .replace("\r\n", " ")
        .replace("\n", " ")
        .replace("\r", " ")
        .replace(Regex("\\[image]"), "")
        .replace(Regex("\\[\\d+]"), "")
        .trim()
        .replace(Regex("  +"), " ")
}
```

### Маппер заметок

Текст очищается при маппинге BookmarkNote → UiBookmarkNote:

```kotlin
// UiBookmarkMapper.kt
fun mapNote(domain: BookmarkNote): UiBookmarkNote {
    return UiBookmarkNote(
        ...
        text = cleanBookmarkText(domain.text),  // Очистка в domain слое
        ...
    )
}
```

### Экран заметок

Два режима отображения:
- **LIST** — список всех заметок в хронологическом порядке
- **BY_BOOK** — список книг с количеством заметок

## Скачивание

Скачивание использует `DownloadManager` системы:

```kotlin
val request = DownloadManager.Request(uri).apply {
    setTitle(book.title)
    setDescription("Скачивание книги")
    setNotificationVisibility(VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
}
downloadManager.enqueue(request)
```

## BroadcastReceiver

Отслеживание завершения скачивания:

```kotlin
class BookDownloadBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context, intent) {
        // Обработка завершения скачивания
    }
}
```

## Структура файлов

```
books/src/main/java/su/sv/books/catalog/
├── di/BooksApiModule.kt
├── data/
│   ├── api/
│   ├── models/
│   ├── repo/
│   └── receivers/
├── domain/
│   ├── model/
│   │   ├── Book.kt
│   │   └── BookmarkNote.kt     # Модель заметки + cleanBookmarkText()
│   ├── GetBooksListUseCase.kt
│   ├── DownloadBookUseCase.kt
│   └── GetBookUriUseCase.kt
└── presentation/
    ├── CommonDownloadBookStates.kt
    ├── base/BaseBookViewModel.kt
    ├── root/
    │   ├── viewmodel/RootBooksCatalogViewModel.kt
    │   ├── ui/BookList.kt
    │   └── ...
    ├── detail/
    │   ├── viewmodel/BookDetailViewModel.kt
    │   ├── ui/BookDetailUi.kt
    │   ├── nav/BookDetailScreen.kt
    │   └── ...
    ├── bookmarks/
    │   ├── nav/BookmarksScreen.kt
    │   ├── model/UiBookmarkNote.kt
    │   ├── mapper/UiBookmarkMapper.kt  # Маппер с очисткой текста
    │   ├── ui/
    │   │   ├── NoteItem.kt
    │   │   ├── BookWithNotesItem.kt
    │   │   └── DeleteNoteDialog.kt
    │   └── viewmodel/BookmarksViewModel.kt
    └── downloaded/
        ├── ui/DownloadedBooksScreen.kt
        └── viewmodel/DownloadedBooksViewModel.kt
```

---

## Дизайн-система

Все экраны модуля применяют дизайн-систему:

### Применение темы на Modo Screen

```kotlin
@Composable
override fun Content(modifier: Modifier) {
    val themeViewModel: ThemeViewModel = hiltViewModel()
    val themeConfig by themeViewModel.themeConfig.collectAsStateWithLifecycle()

    SVAPPTheme(
        themeMode = themeConfig.themeMode,
        useDynamicColors = themeConfig.useDynamicColors
    ) {
        // Контент экрана
    }
}
```

### Экраны с применённой темой

| Экран | Файл |
|-------|------|
| Закладки | `bookmarks/nav/BookmarksScreen.kt` |
| Детали книги | `detail/nav/BookDetailScreen.kt` |
| Скачанные книги | `downloaded/ui/DownloadedBooksScreen.kt` |

### Зависимости

Для работы темы требуется зависимость от модуля `managers`:

```kotlin
// build.gradle.kts
implementation(project(":managers"))
```