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

### Статус миграции

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

**Оставшиеся Java файлы:**
- `app/Storage.java` - основное хранилище, зависимости: Plugin, Reflow, Bookmarks
- `widgets/TTSPopup.java` - TTS функционал, зависит от FBReaderView, ScrollWidget
- `widgets/ScrollWidget.java` - скролл виджет, зависит от FBReaderView, Plugin, Reflow
- `widgets/FBReaderView.java` - главный виджет чтения, зависит от всех компонентов

### Порядок миграции (от простого к сложному)

1. DjvuPlugin.java → DjvuPlugin.kt (зависит от Plugin.View)
2. PDFPlugin.java → PDFPlugin.kt (зависит от Plugin.View)
3. Storage.java → Storage.kt (базовый класс)
4. ScrollWidget.java → ScrollWidget.kt (зависит от FBReaderView)
5. TTSPopup.java → TTSPopup.kt (зависит от FBReaderView, ScrollWidget)
6. FBReaderView.java → FBReaderView.kt (самый сложный)

### Особенности миграции

- Использовать `lateinit` для свойств, инициализируемых в `create()`
- Обратите внимание на nullable типы в интерфейсах FBReader
- Внутренние классы должны быть `inner class` если обращаются к внешнему классу
- Companion object для static методов и свойств
- Использовать `@JvmStatic` для совместимости с Java кодом
- Использовать `@JvmField` для static полей

## Примечания

Модуль интегрирован с основным приложением через навигацию. При нажатии на скачанную книгу открывается этот модуль.
