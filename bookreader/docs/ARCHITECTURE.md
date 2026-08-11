# Архитектура модуля bookreader

## Обзор

Модуль `bookreader` отвечает за чтение книг в форматах PDF, EPUB, FB2, DjVu, CBZ/CBR. Основан на
библиотеке **FBReader** (модуль `fbreader`). UI построен на **Jetpack Compose** с паттерном **MVI**.

---

## Слои архитектуры

```
┌─────────────────────────────────────────────────────┐
│                 UI Layer (Compose)                   │
│  ReaderScreen → ReaderContent → Compose-компоненты  │
└────────────────────────┬────────────────────────────┘
                         │ state / actions
┌────────────────────────▼────────────────────────────┐
│               ViewModel Layer (MVI)                  │
│  ReaderViewModel ← ReaderState / ReaderActions       │
└────────────────────────┬────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────┐
│               Domain Layer                           │
│  BookmarksRepository / BookContextService /          │
│  GetLastReadBookUseCase / BookmarkTextUtils           │
└────────────────────────┬────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────┐
│               Data / Widget Layer                    │
│  FBReaderView (Java) + Plugin system                 │
│  Storage.java (хранение)                            │
└─────────────────────────────────────────────────────┘
```

---

## Диаграмма зависимостей ключевых классов

```
ReaderScreen (Modo Screen, Parcelable)
    └── ReaderContent (@Composable)
            ├── ReaderViewModel (HiltViewModel)
            │       ├── Storage (хранение книг)
            │       ├── OnBookPagerManager (внешний менеджер)
            │       └── FBReaderView (ref через var)
            │
            ├── FBReaderView (AndroidView, RelativeLayout)
            │       ├── FBReaderApp (ядро FBReader)
            │       ├── Plugin.View (pluginview: PDF/DjVu/Comics)
            │       ├── ZLViewWidget (widget: PagerWidget | ScrollWidget)
            │       ├── SelectionView (выделение текста)
            │       ├── FBFooterView (нижний footer)
            │       └── TTSPopup (TTS)
            │
            ├── ReaderTopBar - TopAppBar
            ├── SelectionComposePanel - панель над выделением
            ├── TocComposeDialog - оглавление
            ├── BookmarksComposeDialog - список закладок
            ├── BookmarkBottomSheet - редактирование закладки
            ├── NavigationComposeDialog - навигация по страницам
            └── FontsComposeBottomSheet - настройки шрифтов
```

```
Plugin (interface)
    ├── PDFPlugin (BuiltinFormatPlugin)
    │       ├── PdfiumView / NativeView (Plugin.View)
    │       │       └── PdfiumPage / NativePage (Plugin.Page)
    │       ├── Selection (inner class)
    │       └── PdfSearch (inner class)
    ├── DjvuPlugin (BuiltinFormatPlugin)
    │       ├── DjvuView (Plugin.View)
    │       └── DjvuTextModel
    └── ComicsPlugin (BuiltinFormatPlugin)
            ├── ComicsView (Plugin.View)
            └── ZipDecoder / RarDecoder
```

```
BookmarksRepository
    └── Storage (список JSON-файлов в хранилище)
            └── {md5}.json (RecentInfo с bookmarks[])

BookContextService
    └── Storage.read() → FBook → Plugin → BookModel → ZLTextModel

GetLastReadBookUseCase
    └── Storage.list() → сортировка по info.last
```

---

## Внешние зависимости

| Зависимость         | Модуль/Артефакт                | Для чего                                    |
|---------------------|--------------------------------|---------------------------------------------|
| FBReader            | `fbreader` (внутренний модуль) | Ядро отображения EPUB/FB2, парсинг текста   |
| Pdfium              | `com.github.axet.pdfium`       | Рендеринг PDF (смартфоны), выделение текста |
| Android PdfRenderer | Android API                    | Рендеринг PDF (планшеты, стабильный)        |
| DjvuLibre           | `com.github.axet.djvulibre`    | Рендеринг DjVu                              |
| zip4j               | `net.lingala.zip4j`            | Чтение CBZ-архивов                          |
| junrar              | `de.innosystec.unrar`          | Чтение CBR-архивов                          |
| Hilt                | `dagger.hilt.android`          | DI для ViewModel и репозиториев             |
| Modo                | `com.github.terrakok.modo`     | Навигация между экранами                    |
| Timber              | `timber.log.Timber`            | Логирование (всегда тег `"voronin"`)        |

---

## Точки входа в модуль

### Открытие книги (основной путь)

Внешние модули (books) открывают читалку через **Modo навигацию**:

```kotlin
// Открыть книгу
stackNavigation.forward(
    ReaderScreen(
        bookUri = bookUri,
        bookCoverUrl = "https://...",
        bookTitle = "Название",
        bookAuthor = "Автор",
        bookmarkPosition = null  // или позиция для навигации к заметке
    )
)

// Открыть книгу на заметке
stackNavigation.forward(
    ReaderScreen(
        bookUri = bookUri,
        bookmarkPosition = BookmarkPosition(
            startParagraph = 42,
            startElement = 0,
            startChar = 0,
            endParagraph = 42,
            endElement = 10,
            endChar = 0
        )
    )
)
```

### Инициализация при старте приложения

```kotlin
// В Application.onCreate()
BookReaderInitializer.init(context)
```

Инициализирует:

- `ZLAndroidApplication` — ядро FBReader
- `TTFManager` — менеджер шрифтов
- Загружает папку шрифтов из настроек

### GetLastReadBookUseCase (snackbar "Продолжить чтение")

Используется в главном экране приложения для определения последней прочитанной книги. Сканирует
хранилище через `Storage.list()`.

---

## Поток открытия книги

```
1. ReaderScreen.Content() вызывается Modo
2. ReaderContent() запускается
3. LaunchedEffect(bookUri) → viewModel.onAction(LoadBook)
4. ViewModel: storage.load(uri) → Storage.Book
5. ViewModel: storage.read(book) → Storage.FBook
6. ViewModel: _state = ReaderState.Content(book)
7. Compose: AndroidView factory создаёт FBReaderView
8. factory: fbReaderView.loadBook(fbook) →
   8a. Storage.getPlugin(info, fbook) → PDFPlugin / DjvuPlugin / FBReader
   8b. Plugin.create(fbook) → Plugin.View (PdfiumView / DjvuView)
   8c. BookModel.createModel() → ZLTextModel
   8d. app.BookTextView.setModel(textModel)
9. factory: setWidget(PagerWidget | ScrollWidget)
10. factory: post { gotoPosition(savedPos) }  // если открыли на заметке
11. factory: viewModel.migrateBookmarksContextAsync()
12. widget.repaint() → рендеринг первой страницы
```

---

## Структура файлов

```
bookreader/src/main/java/com/github/axet/bookreader/
├── app/                           # Инфраструктура и плагины
│   ├── BookReaderInitializer.kt   # Singleton инициализатор
│   ├── Plugin.kt                  # Интерфейс плагина + базовые классы
│   ├── PDFPlugin.kt               # Плагин PDF
│   ├── DjvuPlugin.kt              # Плагин DjVu
│   ├── ComicsPlugin.kt            # Плагин CBZ/CBR
│   ├── Storage.java               # Legacy хранилище (Java)
│   ├── ReaderPreferences.kt       # Константы SharedPreferences
│   ├── Reflow.kt                  # Алгоритм reflow для PDF
│   ├── TTFManager.kt              # Менеджер TTF-шрифтов
│   └── TextFormatter.kt           # Форматирование текста
├── domain/                        # Бизнес-логика
│   ├── BookmarksRepository.kt     # Репозиторий заметок
│   ├── BookContextService.kt      # Контекст предложения
│   ├── BookmarkTextUtils.kt       # cleanBookmarkText()
│   └── GetLastReadBookUseCase.kt  # Последняя книга
├── screens/                       # UI экраны
│   ├── ReaderScreen.kt            # Modo Screen (точка входа)
│   ├── ReaderContent.kt           # Compose контент экрана
│   ├── ReaderSettingsScreen.kt    # Экран настроек
│   ├── ReaderSettingsContent.kt   # Контент настроек
│   ├── testing/ReaderTestTags.kt  # Теги для UI-тестов
│   └── ui/                        # Compose компоненты
│       ├── ReaderTopBar.kt
│       ├── SelectionComposePanel.kt
│       ├── BookmarksComposeDialog.kt
│       ├── BookmarkBottomSheet.kt
│       ├── NavigationComposeDialog.kt
│       └── SearchComposePanel.kt
├── screens/viewmodel/             # MVI слой
│   ├── ReaderViewModel.kt
│   ├── ReaderState.kt
│   ├── ReaderActions.kt
│   └── SearchState.kt
├── widgets/                       # View компоненты
│   ├── FBReaderView.java          # Главный View (Legacy Java)
│   ├── ScrollWidget.java          # Режим прокрутки (Legacy Java)
│   ├── PagerWidget.kt             # Режим страниц
│   ├── SelectionView.kt           # Отображение выделения
│   ├── SelectionState.kt          # Состояния выделения
│   ├── SelectionCoordinates.kt    # Системы координат
│   ├── ZoomGestureHandler.kt      # Обработка zoom
│   ├── ZoomTouchAdapter.kt        # Адаптация координат при zoom
│   ├── ActiveAreasView.kt         # Подсказки зон касания
│   ├── ZLBookmark.kt              # Highlighting для FBReader
│   ├── ZLTextIndexPosition.kt     # Позиция с началом и концом
│   ├── BrightnessGesture.kt       # Жест яркости
│   ├── FBFooterView.kt            # Нижний footer
│   ├── TTSPopup.kt                # TTS (text-to-speech)
│   ├── WallpaperLayout.kt         # Фон читалки
│   └── TimeAnimatorCompat.kt      # Совместимый аниматор
└── services/
    └── ImagesProvider.kt          # Провайдер изображений
```
