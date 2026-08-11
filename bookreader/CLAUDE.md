# BookReader Module

Модуль чтения книг.

## Обзор

Модуль `bookreader` отвечает за чтение книг в различных форматах (PDF, EPUB, FB2 и др.). Основан на
библиотеке FBReader.

## Основные компоненты

### Activities

#### BookReaderMainActivity

Главная Activity приложения чтения:

```kotlin
class BookReaderMainActivity : AppCompatActivity()
```

#### FullscreenActivity

Полноэкранный режим чтения.

#### SettingsActivity

Настройки приложения чтения.

### Fragments

#### ReaderFragment

Фрагмент для отображения книги:

```kotlin
class ReaderFragment : Fragment()
```

Отвечает за:

- Отображение текста книги
- Навигацию по страницам
- Управление закладками

#### LibraryFragment

Фрагмент библиотеки книг:

```kotlin
class LibraryFragment : Fragment()
```

### Поддерживаемые форматы

- PDF
- EPUB
- FB2
- MOBI
- RTF
- И другие

### PermissionHelper

Помощник для работы с разрешениями:

```kotlin
object PermissionHelper {
    fun checkStoragePermission(activity: Activity): Boolean
    fun requestStoragePermission(activity: Activity)
}
```

### BookApplication

Application класс для инициализации:

```kotlin
class BookApplication : Application()
```

## Закладки

### Storage.Bookmark

Java класс для хранения закладок:

```java
public static class Bookmark {
    public long last;
    public String name;
    public String text;           // Текст закладки (с FBReader markers)
    public int color;
    public ZLTextPosition start;
    public ZLTextPosition end;
    public String coverUrl;
    public String bookFileUri;
}
```

### Функция очистки текста

FBReader вставляет специальные символы в текст закладок:

- `U+FFFE` (65534) — маркер переноса слов
- Управляющие символы
- Маркеры `[image]`, `[1]`, `[2]`

Функция `cleanBookmarkText()` в domain слое очищает текст:

```kotlin
// domain/BookmarkTextUtils.kt
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

Используется при отображении:

- `screens/ui/BookmarksComposeDialog.kt` — список закладок
- `screens/ui/BookmarkBottomSheet.kt` — редактирование закладки

### Диалог закладок

```kotlin
@Composable
fun BookmarksComposeDialog(
    book: Storage.Book?,
    fbReaderView: FBReaderView?,
    onDismiss: () -> Unit,
    onDelete: (Storage.Bookmark) -> Unit,
)
```

## Структура файлов

```
bookreader/src/main/java/com/github/axet/bookreader/
├── app/
│   ├── BookReaderInitializer.kt  # Инициализация читалки
│   ├── ComicsPlugin.kt
│   ├── DjvuPlugin.kt
│   ├── PDFPlugin.kt
│   ├── PermissionHelper.kt
│   ├── Plugin.kt                # API-интерфейс (71 строка) с backward-compatible врапперами
│   ├── PluginBox.kt             # PluginBox, PluginRenderRect — вынесены из Plugin.kt
│   ├── PluginPage.kt            # PluginPage — abstract class страницы, вынесен из Plugin.kt
│   ├── PluginView.kt            # PluginView + Selection/Link/Search — вынесены из Plugin.kt
│   ├── ReaderPreferences.kt
│   ├── Reflow.kt
│   ├── Storage.java            # Legacy Java class с Bookmark
│   ├── TTFManager.kt
│   └── TextFormatter.kt
├── domain/
│   ├── BookmarkTextUtils.kt     # cleanBookmarkText()
│   ├── BookmarksRepository.kt
│   └── GetLastReadBookUseCase.kt
├── screens/
│   ├── ReaderScreen.kt          # Главный экран чтения
│   ├── ReaderContent.kt
│   ├── ReaderSettingsScreen.kt
│   ├── ReaderSettingsContent.kt
│   ├── testing/
│   │   └── ReaderTestTags.kt
│   └── ui/
│       ├── BookmarksComposeDialog.kt  # Диалог списка закладок
│       ├── BookmarkBottomSheet.kt     # BottomSheet редактирования
│       ├── NavigationComposeDialog.kt
│       ├── ReaderTopBar.kt
│       ├── SearchComposePanel.kt
│       └── SelectionComposePanel.kt
├── viewmodel/
│   ├── ReaderViewModel.kt
│   ├── ReaderActions.kt
│   ├── ReaderState.kt
│   └── SearchState.kt
├── widgets/
│   ├── ActiveAreasView.kt
│   ├── BrightnessGesture.kt
│   ├── FBFooterView.kt
│   ├── FBReaderView.java        # Legacy Java
│   ├── PagerWidget.kt
│   ├── ScrollWidget.java        # Legacy Java
│   ├── SelectionCoordinates.kt
│   ├── SelectionState.kt
│   ├── SelectionView.kt
│   ├── TTSPopup.kt
│   ├── TimeAnimatorCompat.kt
│   ├── WallpaperLayout.kt
│   ├── ZLBookmark.kt
│   ├── ZLTextIndexPosition.kt
│   ├── ZoomGestureHandler.kt
│   └── ZoomTouchAdapter.kt
└── services/
    └── ImagesProvider.kt
```

## Зависимости

Модуль использует:

- FBReader библиотеку (модуль `fbreader`)
- DragSortListView для списка книг

## Миграция на Kotlin

### Статус миграции (обновлено 2026-07-26)

**Мигрированные файлы:**

- app/ (все Kotlin файлы кроме Storage.java)
- domain/ (все файлы)
- screens/ (все Compose экраны)
- viewmodel/ (все файлы)
- services/ImagesProvider.kt
- widgets/ (большинство файлов, кроме FBReaderView.java и ScrollWidget.java)

**Оставшиеся Java файлы (3 файла):**

- `app/Storage.java` - наследуется от внешней Java библиотеки
- `widgets/ScrollWidget.java` - много внутренних классов
- `widgets/FBReaderView.java` - декомпозируется

### Новые компоненты (2026-07-26)

- `screens/ReaderScreen.kt` — главный Compose экран чтения
- `screens/ReaderSettingsScreen.kt` — экран настроек
- `screens/viewmodel/` — ViewModel с MVI паттерном
- `widgets/ZoomGestureHandler.kt` — обработка зума
- `widgets/ZoomTouchAdapter.kt` — адаптер для touch событий зума
- `app/ReaderPreferences.kt` — настройки чтения на DataStore
- `domain/GetLastReadBookUseCase.kt` — use case для последней книги

### Особенности миграции Storage.java

**Сложности:**

- Наследуется от внешней Java библиотеки `com.github.axet.androidlibrary.app.Storage`
- Много статических методов, вызываемых из Kotlin кода
- Внутренние классы: `Info`, `Progress`, `ProgresInputstream`, `FileCbz`, `FileCbr`, `FBook`,
  `Book`, `RecentInfo`, `Bookmark`, `Bookmarks`

**Требуется:**

- Добавить `@JvmStatic` для статических методов в `companion object`
- Добавить `@JvmField` для статических полей
- Создать обёртки для статических методов родительского класса (например, `getFile`, `exists`,
  `getNameNoExt` и т.д.)
- Использовать `open class` для классов которые наследуются (например, `Bookmark`)
- Использовать `lateinit` для `Book.info` и `FBook.book`

### Общие правила миграции

- Использовать `lateinit` для свойств, инициализируемых позже
- Обратите внимание на nullable типы в интерфейсах FBReader
- Внутренние классы должны быть `inner class` если обращаются к внешнему классу
- Companion object для static методов и свойств
- Использовать `@JvmStatic` для совместимости с Java кодом
- Использовать `@JvmField` для static полей

## Примечания

Модуль интегрирован с основным приложением через навигацию. При нажатии на скачанную книгу
открывается этот модуль.