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
│   ├── Storage.kt             # Класс Storage с Bookmark (мигрирован из Java)
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
│   ├── FBReaderView.kt           # Мигрирован из Java (главный виджет)
│   ├── PagerWidget.kt
│   ├── ScrollWidget.kt          # Мигрирован из Java (RecyclerView-виджет прокрутки)
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

### Статус миграции (обновлено 2026-08-29)

**Мигрированные файлы:**

- app/ (все файлы, включая Storage.kt)
- domain/ (все файлы)
- screens/ (все Compose экраны)
- viewmodel/ (все файлы)
- services/ImagesProvider.kt
- widgets/ (все файлы)

**Все Java файлы мигрированы в Kotlin.**

### Новые компоненты (2026-07-26)

- `screens/ReaderScreen.kt` — главный Compose экран чтения
- `screens/ReaderSettingsScreen.kt` — экран настроек
- `screens/viewmodel/` — ViewModel с MVI паттерном
- `widgets/ZoomGestureHandler.kt` — обработка зума
- `widgets/ZoomTouchAdapter.kt` — адаптер для touch событий зума
- `app/ReaderPreferences.kt` — настройки чтения на DataStore
- `domain/GetLastReadBookUseCase.kt` — use case для последней книги

### Миграция Storage.java (завершена 2026-08-29)

**Сложности:**

- Наследуется от внешней Java библиотеки `com.github.axet.androidlibrary.app.Storage`
- Много статических методов, вызываемых из Kotlin кода
- Внутренние классы: `Info`, `Progress`, `ProgresInputstream`, `FileCbz`, `FileCbr`, `FBook`,
  `Book`, `RecentInfo`, `Bookmark`, `Bookmarks`

**Ключевые решения:**

- `@JvmStatic` для собственных статических методов в `companion object`; `@JvmField` для полей.
- Обёртки для статических методов родителя (`getFile`, `exists`, `getName`, `list`,
  `takePersistableUriPermission`, `getTypeByExt`) сделаны **без** `@JvmStatic` — иначе возникает
  "accidental override" с унаследованными статиками. Java и так резолвит унаследованные статики напрямую.
- `open class` для наследуемых внутренних классов (`Bookmark` и др.); публичные поля — `@JvmField`
  с nullable-типами (совместимость `@JvmField` и `lateinit` невозможна).
- `FBView.ImageFitting` — это enum, вложенный в package-private `ZLTextViewBase`. Java резолвит его
  как `FBView.ImageFitting` (наследование вложенного типа), но Kotlin так не умеет, а `ZLTextViewBase`
  недоступен извне пакета. Поэтому поле `RecentInfo.scale` хранится как `Enum<*>` и восстанавливается
  по имени через рефлексию (`imageFittingValueOf`).

### Миграция ScrollWidget.java (завершена 2026-08-29)

**Сложности:**

- Внутренние классы: `ScrollAdapter`, `PageView`, `PageHolder`, `PageCursor`, `Gestures`
- Прямой перевод поля `public ZLTextPosition start` → `@JvmField var start: ZLTextPosition? = null`
  меняет тип для Kotlin-вызывающих с платформенного `ZLTextPosition!` на явный `ZLTextPosition?`,
  поэтому в вызывающих файлах (`ComicsPlugin`, `DjvuPlugin`, `PDFPlugin`, `TTSPopup`) потребовались `!!`.
- `RecyclerView.getChildAt(i)` в Kotlin возвращает `View?` (а не `View`).

**Ключевые решения:**

- `inner class` для всех внутренних классов (обращаются к внешнему `fb`/`adapter`).
- `@JvmField` для полей, читаемых из Java (`adapter`, `gesturesListener`, `PageView.info/text`,
  `PageCursor.start/end`, `Gestures.e`, `PageHolder.page`) — `@JvmField` несовместим с `lateinit`.
- Конфликт имён Java-поле/метод при переводе: в `PageView` анонимный `ProgressBar` имел поле
  `handler` (переименовано в `mHandler`, т.к. `View.getHandler()` уже существует); в `Gestures`
  поле `zoomHandler` + метод `getZoomHandler()` давали clash — явный `getZoomHandler()` удалён,
  остался геттер от `lateinit var zoomHandler`.
- `onScroll`/`onFling` из `GestureDetector.OnGestureListener` имеют **nullable** параметр `e1`;
  в `onScroll` используется поле `e`, поэтому перевод требует `open(e!!)` (не `e2`).

### Миграция FBReaderView.java (завершена 2026-08-29)

**Сложности:**

- Класс наследуется от `RelativeLayout` (а не от `View`): внутренний `CustomView extends FBView` — это
  настоящий текстовый виджет; для ширины/высоты/скроллбара используется `this@FBReaderView.*`.
- Java-геттеры, к которым Kotlin-вызывающие обращались **как к свойствам** (`fb.position`,
  `customview.footer`, `fbv.isReflow`), переведены в Kotlin-свойства (`val position`, `val footer`,
  `val isReflow`) — синтетические свойства работают только для Java-методов, не для Kotlin-функций.
- `FBView.ImageFitting` — package-private enum из `ZLTextViewBase`: восстановление по имени через
  рефлексию (`imageFitting()`) и получение `ImageOptions.FitToScreen` через рефлексию (`fitToScreenOption()`).
- `SearchCallback`/`CustomAction` объявлены как `fun interface` для SAM-конверсии из лямбд.
- `ConfigShadow extends Config` — реализованы все абстрактные методы (`getValueInternal`, `listGroups`,
  `isInitialized` и др.).
- Поля `pluginview`/`book`/`selection`/`footer` стали nullable → в вызывающих (`ScrollWidget`,
  `PagerWidget`, `TTSPopup`) потребовались `!!`; `widget`/`drawer` — `@JvmField var ... = null`
  (конфликт с `setWidget`/`setDrawer`); `app`/`config` — `lateinit`.
- Вложенные `LinksView`/`BookmarksView`/`TTSView`/`SearchView` принимают `info: Reflow.Info?` (nullable),
  чтобы соответствовать платформенному типу `Reflow.Info!` из Java.

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