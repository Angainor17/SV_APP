# BookReader Module

Модуль чтения книг.

## Обзор

Модуль `bookreader` отвечает за чтение книг в различных форматах (PDF, EPUB, FB2 и др.). Основан на библиотеке FBReader.

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

## Структура файлов

```
bookreader/src/main/java/com/github/axet/bookreader/
├── activities/
│   ├── BookReaderMainActivity.kt
│   ├── FullscreenActivity.kt
│   ├── SettingsActivity.kt
│   └── ActivityExt.kt
├── fragments/
│   ├── ReaderFragment.kt
│   └── LibraryFragment.kt
└── app/
    ├── BookApplication.kt
    └── PermissionHelper.kt
```

## Зависимости

Модуль использует:
- FBReader библиотеку (модуль `fbreader`)
- DragSortListView для списка книг

## Миграция на Kotlin

### Статус миграции (обновлено 2026-06-06)

**Мигрированные файлы:**
- activities/ (все файлы)
- fragments/ (все файлы)
- app/BookApplication.kt
- app/PermissionHelper.kt
- app/Plugin.kt
- app/Reflow.kt
- app/TTFManager.kt
- app/TextFormatter.kt
- app/ComicsPlugin.kt
- app/PDFPlugin.kt
- app/DjvuPlugin.kt
- services/ImagesProvider.kt
- widgets/ActiveAreasView.kt
- widgets/BookmarkPopup.kt
- widgets/BookmarksDialog.kt
- widgets/FBFooterView.kt
- widgets/FontsPopup.kt
- widgets/FullWidthActionView.kt
- widgets/SelectionView.kt
- widgets/TimeAnimatorCompat.kt
- widgets/WallpaperLayout.kt
- widgets/TTSPopup.kt
- widgets/PagerWidget.kt
- widgets/StoragePathPreferenceCompat.kt
- widgets/ZLTextIndexPosition.kt (выделен из FBReaderView.java)
- widgets/ZLBookmark.kt (выделен из FBReaderView.java)
- widgets/BrightnessGesture.kt (выделен из FBReaderView.java)

**Оставшиеся Java файлы (3 файла) - в процессе декомпозиции:**
- `app/Storage.java` (1392 строки) - наследуется от внешней Java библиотеки
- `widgets/ScrollWidget.java` (1717 строк) - много внутренних классов
- `widgets/FBReaderView.java` (2158 строк, было 2294) - декомпозируется

### Декомпозиция Java файлов

Выделенные классы из FBReaderView.java:
- `ZLTextIndexPosition` → `ZLTextIndexPosition.kt`
- `ZLBookmark`, `ZLTTSMark` → `ZLBookmark.kt`
- `BrightnessGesture` → `BrightnessGesture.kt`

### Порядок миграции

1. ~~DjvuPlugin.java → DjvuPlugin.kt~~ ✅
2. ~~PDFPlugin.java → PDFPlugin.kt~~ ✅
3. **Storage.java → Storage.kt** (следующий) - наследуется от `com.github.axet.androidlibrary.app.Storage`
4. ScrollWidget.java → ScrollWidget.kt
5. FBReaderView.java → FBReaderView.kt (самый сложный)

### Особенности миграции Storage.java

**Сложности:**
- Наследуется от внешней Java библиотеки `com.github.axet.androidlibrary.app.Storage`
- Много статических методов, вызываемых из Kotlin кода
- Внутренние классы: `Info`, `Progress`, `ProgresInputstream`, `FileCbz`, `FileCbr`, `FBook`, `Book`, `RecentInfo`, `Bookmark`, `Bookmarks`

**Требуется:**
- Добавить `@JvmStatic` для статических методов в `companion object`
- Добавить `@JvmField` для статических полей
- Создать обёртки для статических методов родительского класса (например, `getFile`, `exists`, `getNameNoExt` и т.д.)
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

Модуль интегрирован с основным приложением через навигацию. При нажатии на скачанную книгу открывается этот модуль.
