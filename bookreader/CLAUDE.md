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

## Примечания

Модуль интегрирован с основным приложением через навигацию. При нажатии на скачанную книгу открывается этот модуль.
