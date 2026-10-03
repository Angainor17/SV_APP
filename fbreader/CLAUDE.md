# FBReader Module

Библиотека для чтения электронных книг.

## Обзор

Модуль `fbreader` — это порт библиотеки FBReader для Android. Предоставляет полную функциональность
для чтения электронных книг различных форматов.

## Поддерживаемые форматы

- **FB2** (FictionBook 2.0)
- **EPUB** (Electronic Publication)
- **MOBI** (Mobipocket)
- **PDF**
- **RTF** (Rich Text Format)
- **TXT** (Plain Text)
- **HTML** / **XHTML**
- **DOC** (Microsoft Word)

## Архитектура

Модуль разделён на три основных пакета:

### org.geometerplus.fbreader

Основная логика приложения чтения:

```
fbreader/
├── book/           # Модели книг
├── bookmodel/      # Модели для отображения
├── fbreader/       # Ядро читалки
│   └── options/    # Настройки
├── formats/        # Обработчики форматов
│   ├── fb2/        # FB2 формат
│   └── oeb/        # EPUB/OEB формат
├── sort/           # Сортировка (TitledEntity)
└── util/           # Утилиты (TextSnippet)
```

> **Примечание:** Пакеты `library/`, `tree/` удалены (см. раздел "Удалённый функционал")

### org.geometerplus.zlibrary.core

Кроссплатформенное ядро:

```
zlibrary/core/
├── encodings/      # Кодировки
├── drm/            # DRM защита
├── filesystem/     # Файловая система
├── filetypes/      # Типы файлов
├── fonts/          # Шрифты
├── image/          # Работа с изображениями
├── language/       # Языки
├── library/        # Библиотечные функции
├── options/        # Опции/настройки
├── resources/      # Ресурсы
├── tree/           # Деревья
├── util/           # Утилиты
├── xml/            # XML парсинг
└── view/           # Представления
```

> **Примечание:** Пакеты `money/`, `network/` удалены

### org.geometerplus.zlibrary.ui.android

Android-специфичный UI:

```
zlibrary/ui/android/
├── view/           # Кастомные View (ZLAndroidWidget, MainView)
├── image/          # Загрузка изображений
└── library/        # Android библиотека
```

> **Примечание:** Пакет `network/` удалён

### org.geometerplus.zlibrary.text

Текстовый движок:

```
zlibrary/text/
├── model/          # Модели текста
├── view/           # Отображение текста
└── hyphenation/    # Переносы слов
```

## Native код (JNI)

Модуль содержит нативный код для производительности:

```
jni/
├── NativeFormats/           # Нативные обработчики форматов
├── LineBreak/               # Переносы строк
├── DeflatingDecompressor/   # Декомпрессия
└── expat-2.0.1/             # XML парсер
```

## Assets

```
assets/
├── encodings/              # Таблицы кодировок
├── formats/                # Описания форматов
├── hyphenationPatterns/    # Паттерны переносов
└── languagePatterns/       # Языковые паттерны
```

## Ключевые классы

### FBReaderApp

Главный класс приложения чтения:

```java
public class FBReaderApp {
    // Управление открытием книг
    // Навигация по тексту
    // Закладки
    // Поиск
}
```

### Book

Модель книги:

```java
public class Book {
    public String getPath();
    public String getTitle();
    public String getLanguage();
    public BookId getId();
}
```

### BookModel

Модель для отображения книги:

```java
public class BookModel {
    public TOCTree getTOCTree();  // Оглавление
    public Book getBook();
}
```

### FormatPlugin

Базовый класс для плагинов форматов:

```java
public abstract class FormatPlugin {
    public abstract void readMetaInfo(Book book);
    public abstract BookModel readModel(Book book);
}
```

## Расширения

Для добавления нового формата:

1. Создать класс, наследующий `FormatPlugin`
2. Реализовать методы `readMetaInfo()` и `readModel()`
3. Зарегистрировать в `PluginCollection`

## Стили и настройки

FBReader поддерживает:

- Настройки шрифтов
- Цветовые схемы
- Отступы и интервалы
- Переносы слов
- Ориентацию страницы

## Производительность

- Использование JNI для критичных операций
- Ленивая загрузка страниц
- Кэширование изображений
- Фоновая загрузка

## Структура файлов

```
fbreader/src/main/
├── java/org/geometerplus/
│   ├── fbreader/
│   ├── zlibrary/core/
│   ├── zlibrary/ui/android/
│   └── zlibrary/text/
├── jni/                   # Native код
├── assets/                # Ресурсы (переносы, кодировки)
├── aidl/                  # AIDL интерфейсы
└── res/                   # Android ресурсы
```

## Зависимости

Модуль использует:

- `util` — утилиты

---

## Удалённый сетевой функционал (Legacy Network)

> **Дата удаления:** 2026-06-25
> **Причина:** Сетевые функции (OPDS каталоги, Atom feed, RSS) не используются в приложении. Книги
> загружаются через модуль `books`.

### Удалённые пакеты

| Пакет                                              | Описание                | Файлов | Строк кода |
|----------------------------------------------------|-------------------------|--------|------------|
| `org.geometerplus.fbreader.network`                | Основные сетевые классы | ~40    | ~5000      |
| `org.geometerplus.fbreader.network.atom`           | Atom feed парсеры       | ~15    | ~1500      |
| `org.geometerplus.fbreader.network.opds`           | OPDS каталоги           | ~20    | ~2500      |
| `org.geometerplus.fbreader.network.urlInfo`        | URL информация          | ~6     | ~500       |
| `org.geometerplus.fbreader.network.tree`           | Деревья навигации       | ~12    | ~1000      |
| `org.geometerplus.fbreader.network.authentication` | Аутентификация          | ~3     | ~400       |

**Итого:** ~10 000 строк кода, 141 файл.

---

## Удалённый неиспользуемый код (2026-06-25)

> **Причина:** Код не используется в приложении и создаёт лишнюю сложность.

### Удалённые пакеты

| Пакет                                                    | Описание                                 | Файлов |
|----------------------------------------------------------|------------------------------------------|--------|
| `org.geometerplus.fbreader.library`                      | Деревья библиотеки (авторы, серии, теги) | 23     |
| `org.geometerplus.android.fbreader.covers`               | Кэш обложек                              | 3      |
| `org.geometerplus.fbreader.tree`                         | Базовое дерево навигации                 | 1      |
| `org.geometerplus.zlibrary.core.network`                 | Сетевые утилиты                          | 10     |
| `org.geometerplus.zlibrary.ui.android.network`           | Android сетевые утилиты                  | 1      |
| `org.geometerplus.zlibrary.core.money`                   | Работа с валютой                         | 2      |
| `org.geometerplus.fbreader.fbreader.options.EInkOptions` | Опции E-Ink экранов                      | 1      |

**Итого:** ~41 файл, ~3000 строк кода.

### Что было удалено

- `LibraryTree.java`, `AuthorTree.java`, `BookTree.java` и др. — дерево библиотеки
- `CoverCache.java`, `CoverManager.java` — кэш обложек
- `FBTree.java` — базовый класс дерева
- `ZLNetworkManager.java`, `ZLNetworkRequest.java` — сетевой менеджер
- `Money.java`, `MoneyException.kt` — работа с валютой

### Оставшийся функционал

Скачивание книг реализовано в модуле `books` через системный `DownloadManager`.

---

# Удалённый функционал (Legacy Activity)

> **Дата удаления:** 2026-06-22
> **Причина:** Переход на Compose UI в модуле `bookreader`. Все Activity были legacy кодом от
> оригинального FBReader, не зарегистрированы в манифесте и заменены на Compose экраны.

## Удалённые Activity и их назначение

### Основные Activity чтения

| Activity                    | Назначение                                                                                           | Требуется восстановление                               |
|-----------------------------|------------------------------------------------------------------------------------------------------|--------------------------------------------------------|
| `FBReader.java`             | Главная Activity для чтения книг. Управляла открытием книг, навигацией, жестами, настройками экрана. | ❌ Нет — заменён на Compose `ReaderScreen` в bookreader |
| `FBReaderMainActivity.java` | Базовый класс для FBReader. Содержал общую логику для Activity чтения.                               | ❌ Нет                                                  |

### Библиотека книг

| Activity                     | Назначение                                                                                       | Требуется восстановление                                                  |
|------------------------------|--------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------|
| `LibraryActivity.java`       | Экран библиотеки книг. Отображение дерева книг (по авторам, сериям, тегам). Поиск по библиотеке. | ⚠️ Возможно — если понадобится отдельный экран библиотеки вне main модуля |
| `BookInfoActivity.java`      | Детальная информация о книге (обложка, описание, авторы, теги).                                  | ⚠️ Возможно — для экрана деталей книги                                    |
| `LibrarySearchActivity.java` | Поиск по локальной библиотеке.                                                                   | ✅ Да — реализовать в Compose                                              |

### Закладки

| Activity                    | Назначение                                                                      | Требуется восстановление                    |
|-----------------------------|---------------------------------------------------------------------------------|---------------------------------------------|
| `BookmarksActivity.java`    | Список закладок с фильтрацией (по книге, все книги). Группировка по дате/книге. | ✅ Да — реализовать экран закладок в Compose |
| `EditBookmarkActivity.java` | Редактирование закладки (текст, стиль, цвет).                                   | ✅ Да — диалог редактирования закладки       |
| `EditStyleActivity.java`    | Редактирование стиля закладки (цвет, шрифт).                                    | ✅ Да — часть диалога закладки               |

### Навигация по книге

| Activity              | Назначение                                                      | Требуется восстановление                              |
|-----------------------|-----------------------------------------------------------------|-------------------------------------------------------|
| `TOCActivity.java`    | Оглавление книги (Table of Contents). Дерево глав с навигацией. | ✅ Да — реализовать как Compose BottomSheet или Dialog |
| `CancelActivity.java` | Меню при выходе из книги (сохранить позицию, выйти).            | ⚠️ Возможно — диалог при выходе                       |

### Настройки

| Activity                    | Назначение                                                 | Требуется восстановление                            |
|-----------------------------|------------------------------------------------------------|-----------------------------------------------------|
| `PreferenceActivity.java`   | Экран настроек чтения (шрифты, цвета, жесты, страница).    | ✅ Да — реализовать `ReaderSettingsScreen` в Compose |
| `ZLPreferenceActivity.java` | Базовый класс для экранов настроек.                        | ❌ Нет                                               |
| `EditBookInfoActivity.java` | Редактирование метаданных книги (авторы, теги, заголовок). | ⚠️ Возможно — для редактирования метаданных         |

### Сетевые функции

| Activity                         | Назначение                                                       | Требуется восстановление                          |
|----------------------------------|------------------------------------------------------------------|---------------------------------------------------|
| `NetworkLibraryActivity.java`    | Каталоги OPDS, сетевые библиотеки. Скачивание книг из интернета. | ⚠️ Возможно — для OPDS каталогов                  |
| `NetworkBookInfoActivity.java`   | Информация о книге из сетевого каталога.                         | ⚠️ Зависит от NetworkLibraryActivity              |
| `AuthenticationActivity.java`    | Авторизация в сетевых библиотеках.                               | ⚠️ Зависит от NetworkLibraryActivity              |
| `AddCustomCatalogActivity.java`  | Добавление пользовательского OPDS каталога.                      | ⚠️ Зависит от NetworkLibraryActivity              |
| `BookDownloader.java`            | Activity для загрузки книг.                                      | ⚠️ Возможно — если нужен отдельный экран загрузки |
| `NetworkSearchActivity.java`     | Поиск по сетевым каталогам.                                      | ⚠️ Зависит от NetworkLibraryActivity              |
| `BuyBooksActivity.java`          | Покупка книг в каталогах.                                        | ⚠️ Зависит от NetworkLibraryActivity              |
| `MenuActivity.java` и наследники | Меню для сетевых каталогов.                                      | ❌ Нет                                             |

### Утилиты и диалоги

| Activity                              | Назначение                                         | Требуется восстановление               |
|---------------------------------------|----------------------------------------------------|----------------------------------------|
| `EditAuthorsDialogActivity.java`      | Диалог редактирования списка авторов.              | ✅ Да — Compose Dialog                  |
| `EditTagsDialogActivity.java`         | Диалог редактирования тегов.                       | ✅ Да — Compose Dialog                  |
| `EditListDialogActivity.java`         | Базовый класс для диалогов редактирования списков. | ❌ Нет                                  |
| `FolderListDialogActivity.java`       | Выбор папки для библиотеки.                        | ✅ Да — Compose Dialog                  |
| `DictionaryNotInstalledActivity.java` | Диалог при отсутствии словаря.                     | ⚠️ Возможно — заменить на Toast/Dialog |

### Обработка ошибок

| Activity                            | Назначение                                 | Требуется восстановление        |
|-------------------------------------|--------------------------------------------|---------------------------------|
| `BugReportActivity.java`            | Экран отправки отчёта об ошибке при краше. | ⚠️ Возможно — для краш-репортов |
| `FixBooksDirectoryActivity.java`    | Исправление директории книг при проблемах. | ⚠️ Возможно                     |
| `MissingNativeLibraryActivity.java` | Ошибка отсутствия нативной библиотеки.     | ⚠️ Возможно                     |
| `BookReadingErrorActivity.java`     | Ошибка при открытии книги.                 | ⚠️ Возможно                     |

### Прочее

| Activity                  | Назначение                                   | Требуется восстановление                       |
|---------------------------|----------------------------------------------|------------------------------------------------|
| `ImageViewActivity.java`  | Просмотр изображений из книги на весь экран. | ✅ Да — Compose экран для просмотра изображений |
| `PluginListActivity.java` | Список установленных плагинов форматов.      | ❌ Нет                                          |

---

## Удалённые Actions

Actions — это команды, которые привязывались к жестам и кнопкам в FBReader:

| Action                     | Назначение                    | Статус                            |
|----------------------------|-------------------------------|-----------------------------------|
| `ShowLibraryAction`        | Открыть библиотеку            | Удалён — библиотека в main модуле |
| `ShowBookmarksAction`      | Открыть закладки              | ⚠️ Требуется восстановление       |
| `ShowTOCAction`            | Открыть оглавление            | ⚠️ Требуется восстановление       |
| `ShowPreferencesAction`    | Открыть настройки             | ⚠️ Требуется восстановление       |
| `ShowBookInfoAction`       | Открыть информацию о книге    | ⚠️ Требуется восстановление       |
| `ShowNetworkLibraryAction` | Открыть сетевой каталог       | Удалён                            |
| `ShowCancelMenuAction`     | Меню выхода                   | Удалён                            |
| `DisplayBookPopupAction`   | Попап с информацией о книге   | Удалён                            |
| `InstallPluginsAction`     | Установка плагинов            | Удалён                            |
| `ProcessHyperlinkAction`   | Обработка гиперссылок         | ⚠️ Частично — нужен для сносок    |
| `SelectionBookmarkAction`  | Создать закладку из выделения | ⚠️ Требуется восстановление       |
| `SelectionTranslateAction` | Перевод выделенного текста    | ⚠️ Требуется восстановление       |

---

## Удалённые ресурсы

### Layout файлы (~20 файлов)

- `bookmarks.xml`, `bookmark_item.xml` — экран закладок
- `book_info.xml`, `book_info_pair.xml` — информация о книге
- `library_tree_item.xml` — элемент дерева библиотеки
- `toc_tree_item.xml` — элемент оглавления
- `edit_bookmark.xml`, `style_item.xml` — редактирование закладки
- `network_book.xml`, `authentication.xml` — сетевые каталоги
- `cancel_item.xml` — меню выхода
- `simple_dialog.xml`, `bug_report_view.xml` — диалоги

### Drawable ресурсы

Иконки для меню и попапов (частично оставлены для SelectionPopup).

---

## Рекомендации по восстановлению функционала

### Приоритет 1 (необходимо для полноценной читалки)

1. **TOCActivity** → Compose BottomSheet с деревом глав
2. **BookmarksActivity** → Compose экран со списком закладок
3. **EditBookmarkActivity** → Compose Dialog для создания/редактирования закладки
4. **PreferenceActivity** → Compose `ReaderSettingsScreen`
5. **ImageViewActivity** → Compose Dialog для просмотра изображений

### Приоритет 2 (желательно)

1. **LibrarySearchActivity** → Поиск в библиотеке
2. **BookInfoActivity** → Экран информации о книге
3. **ProcessHyperlinkAction** → Обработка сносок и ссылок

### Приоритет 3 (опционально)

1. **NetworkLibraryActivity** → OPDS каталоги
2. **BugReportActivity** → Краш-репортинг
3. **EditBookInfoActivity** → Редактирование метаданных

---

## Оставшиеся компоненты

### Используются в bookreader

- `PopupPanel.java`, `SelectionPopup.kt` — попап при выделении текста
- `TextSearchPopup.java` — поиск по тексту
- `NavigationPopup.java` — навигация по страницам
- `DictionaryUtil.java` — интеграция со словарями
- `BookCollectionShadow.java`, `LibraryService.java` — сервис для работы с книгами
- `ZLAndroidWidget.java`, `MainView.java` — View для отображения текста

### Ядро (осталось без изменений)

- `org.geometerplus.fbreader.*` — логика чтения, форматы, модели
- `org.geometerplus.zlibrary.core.*` — кроссплатформенное ядро
- `org.geometerplus.zlibrary.text.*` — текстовый движок

---

## Миграция на Kotlin

> **Статус:** выборочная миграция leaf-классов (не трогая ядро: `FBView`, `ZLTextView`,
> `FBReaderApp`, `ZLApplication` и т.п.). Миграция ведётся поэтапно, каждый батч проверяется
> компиляцией `:fbreader:compileDebugKotlin :fbreader:compileDebugJavaWithJavac`.

### Мигрированные файлы (2026-08-30)

**`org.geometerplus.fbreader.book`**

- `Author.kt` — `@JvmField val DisplayName/SortKey`, статика `create`/`hashCode`/`NULL`.
- `Tag.kt` — `@JvmField val Parent/Name`, private-конструктор, статика `getTag`/`NULL`.
- `Label.kt` — `@JvmField val Uid/Name`, вторичный конструктор `Label(name)`.
- `Series.kt` — наследует Java `TitledEntity<Series>`, переопределяет `getLanguage()`.
- `SeriesInfo.kt` — `@JvmField val Series/Index` (nullable `Index`), статика `createSeriesInfo`/`createIndex`.
- `HighlightingStyle.kt` — `@JvmField val Id/LastUpdateTimestamp`, getter/setter-методы сохранены для
  Java-вызывающих (`getNameOrNull`, `get/setBackgroundColor`, `get/setForegroundColor`).
- `Filter.kt` — `abstract class Filter` с вложенными `class`-ами (`Empty`, `ByAuthor`, `ByTag`,
  `ByLabel`, `ByPattern`, `ByTitlePrefix`, `BySeries`, `HasBookmark`, `HasPhysicalFile`, `And`, `Or`,
  `Not`). Поля фильтров — `@JvmField` (Java-доступ `((Filter.ByAuthor) f).Author` в `XMLSerializer`).
  Тень имени поля/класса (`ByAuthor.Author` vs класс `Author`) разрешается полным именем
  `org.geometerplus.fbreader.book.Author.NULL`.
- `BookmarkQuery.kt` — primary-конструктор `(Book?, Visible, Limit, Page)` с `@JvmField`-полями + три
  вторичных конструктора (Java-варианты `(limit)`, `(book, limit)`, `(book, visible, limit)`); `next()`
  возвращает `BookmarkQuery(Book, Visible, Limit, Page + 1)`. 4-арг. конструктор был package-private в
  Java, в Kotlin public (вызывается из `XMLSerializer` того же пакета).
- `CoverUtil.kt` — `object` с `@JvmStatic fun getCover(...)` (перегрузки `(AbstractBook?, IFormatPluginCollection)`
  и `(ZLFile, IFormatPluginCollection)`); кэш `WeakHashMap<ZLFile, WeakReference<ZLImage>>` + сентинел
  `NULL_IMAGE` (`WeakReference<ZLImage>(null)`), сравнение через `===` (identity). `getPlugin(file)!!.readCover(file)`.
- `BookmarkUtil.kt` — `object` с `@JvmStatic` (`getStyleName`/`setStyleName`/`findEnd`); `findEnd` использует
  меченый цикл `mainLoop@` (`break@mainLoop`), smart-cast `element is ZLTextWord` и поле `element.Length`;
  заканчивается `bookmark.setEnd(cursor.getParagraphIndex(), cursor.getElementIndex(), word.Length)`.
- `SerializerUtil.kt` — `abstract class` → `object` с `@JvmStatic`; чистый делегат к `XMLSerializer`
  (private `defaultSerializer`). Дженерики сохранены: `<B : AbstractBook> deserializeBook(...)` и
  `deserializeBookList(...)`, `BookCreator<out AbstractBook>` для `deserializeBookmarkQuery`. Возвращаемые
  типы nullable (`BookQuery?`/`Bookmark?`/`HighlightingStyle?` и т.п.) — Java-вызывающие получают platform-типы.
- `DuplicateResolver.kt` — package-private `class` → public `class`; `myMap` =
  `Collections.synchronizedMap(HashMap<String, MutableList<ZLFile>>())`, блоки `synchronized(...)`;
  `getPhysicalFile()`/`getShortName()`/`size()` — platform-типы (`ZLPhysicalFile?`), null-проверки как в Java.
  Локальная переменная `entry` переименована, чтобы не конфликтовать с private-методом `entryName`.
- `FileInfoSet.kt` — `public final class`, три конструктора сведены к private-primary
  `(BooksDatabase, MutableCollection<FileInfo>)` + три вторичных (делегируют `database.loadFileInfos(...)`).
  `loadFileInfos`/`removeFileInfo`/`saveFileInfo`/`executeAsTransaction` — `protected`-члены `BooksDatabase`;
  Kotlin в том же пакете обращается к ним (проверено пробой). `!!` на `get(file)!!` там, где Java полагалась
  на «non-null, т.к. вход non-null». `executeAsTransaction { ... }` — SAM-конверсия Java `Runnable`.
  Параметр коллекции — `MutableCollection<FileInfo>` (Java `Collection` маппится в `MutableCollection` в
  контексте делегирования конструктора).
- `BookMergeHelper.kt` — package-private `class` → public `class`; `myCollection` из primary-конструктора.
  `merge` воспроизводит Java `result |= ...` через последовательные `if (mergeX(...)) result = true` (все
  mergeX-методы выполняются всегда — `||` здесь неприменим из-за короткого замыкания). `DbBook(ZLFile,
  FormatPlugin)` (package-private) и `BookUtil.getPlugin` вызываются из того же пакета; `BookReadingException`
  (checked в Java) ловится в Kotlin явно без объявления `throws`.

**`org.geometerplus.fbreader.bookmodel`**

- `FBTextKind.kt` — `interface` с `const val` константами (`Byte`). Важно: константы используются как
  `case`-метки в Java `switch` (`AutoTextSnippet.java`), поэтому именно `const val`, а не `@JvmField`.

**`org.geometerplus.zlibrary.core.util`**

- `ZLColor.kt` — `@JvmField val Red/Green/Blue: Short`, конструкторы `(r,g,b)` и `(intValue)`.
- `RationalNumber.kt` — `@JvmField val Numerator/Denominator: Long`, статика `create`.
- `MiscUtil.kt` — `abstract class` → `object` с `@JvmStatic`; `split` воспроизводит Java-семантику
  (отбрасывание хвостовых пустых строк через `dropLastWhile`).
- `MimeType.kt` — `class` с `private constructor`, `@JvmField val Name: String?`; статика в `companion`
  (`@JvmField` для `NULL`/всех `APP_*`/`TYPES_*`, `const val IMAGE_PREFIX`); `get` парсит
  `name;key=value` с `dropLastWhile` для воспроизведения `String.split(regex, 0)`.
- `ZLArrayUtils.kt` — `abstract class` → `object` с `@JvmStatic` перегрузками `createCopy`
  (`BooleanArray`/`ByteArray`/`CharArray`/`IntArray`/`Array<String?>`).
- `ZLSearchUtil.kt` — `abstract class` → `object`; `@JvmStatic find`, вложенный `class Result`
  с `@JvmField val Start/Length`.
- `ZLNetworkUtil.kt` — `class` → `object` с `@JvmStatic`; nullable-параметры, где Java допускала
  `null` (`url`/`appendParameter`).
- `SliceInputStream.kt` — `public class extends InputStreamWithOffset` → `class SliceInputStream(base,
  start, length) : InputStreamWithOffset(base)`. Java-конструктор `throws IOException` не декларируется
  (Kotlin не имеет checked exceptions). `baseSkip(start)` (в super-конструкторе) → `init { baseSkip(
  start.toLong()) }`. `Math.min/max(int,int)` → `minOf`/`maxOf`; `Math.max(..., 0)` с `long`-приведением
  `.toLong()` в `skip`.

**`org.geometerplus.zlibrary.core.options`**

- `StringPair.kt` — `@JvmField val Group/Name`, `intern()` в конструкторе.

**`org.geometerplus.zlibrary.core.language`**

- `Language.kt` — `@JvmField val Code/Name`, `const val`-константы `ANY_CODE`/`OTHER_CODE`/`MULTI_CODE`/`SYSTEM_CODE`.
- `ZLLanguageUtil.kt` — `abstract class` → `object` с `@JvmStatic` (`defaultLanguageCode`/`languageCodes`/`patternsFile`).

**`org.geometerplus.zlibrary.core.drm`**

- `FileEncryptionInfo.kt` — `@JvmField val Uri/Method/Algorithm/ContentId` (nullable). Конструируется из нативного
  кода через JNI (`Constructor_FileEncryptionInfo` с сигнатурой `(String, String, String, String)`), поэтому
  порядок/сигнатура конструктора сохранены.

**`org.geometerplus.zlibrary.core.drm.embedding`**

- `EmbeddingInputStream.kt` — `public class extends InputStreamWithOffset` → `class EmbeddingInputStream(base,
  uid) : InputStreamWithOffset(base)`. `byte[] myKey` инициализируется через `try/catch`-выражение в property-
  инициализаторе: `MessageDigest.getInstance("SHA").digest(uid.toByteArray(Charsets.UTF_8))` (`getBytes("utf-8")`
  → `toByteArray(Charsets.UTF_8)`); `catch (Exception)` → `throw IOException(e)`. XOR: `bt ^ key[i]` →
  `bt xor myKey[i].toInt()` (byte sign-extended, как в Java); `buffer[i] ^= key[...]` →
  `(buffer[i].toInt() xor key[...].toInt()).toByte()` (нет `^=` для `Byte`).

**`org.geometerplus.zlibrary.core.fonts`**

- `FileInfo.kt` — `@JvmField val Path: String` (non-null) и `@JvmField val EncryptionInfo: FileEncryptionInfo?`.
  Конструируется из нативного кода (`Constructor_FileInfo` `(String, FileEncryptionInfo)`).
- `FontEntry.kt` — `@JvmField val Family`; `private constructor` primary + публичный 5-арг. вторичный конструктор;
  статика `systemEntry` через `synchronized` (был `synchronized (ourSystemEntries)` в Java).
- `FontManager.kt` — `public class` → `class FontManager`. Публичное поле `Map<String, FontEntry> Entries` →
  `@JvmField val Entries: MutableMap<String, FontEntry>` (Java читает `FontManager.Entries.put(...)` как поле).
  `synchronized`-методы → `@Synchronized`. Java `List<String>` → `MutableList<String>` (в `index` и
  `ArrayList<MutableList<String>> myFamilyLists`). `getFamilyEntries` — `try/catch`-выражение, `catch` → `emptyList()`.

**`org.geometerplus.zlibrary.core.filetypes`**

- `FileType.kt` — `abstract` базовый класс, `@JvmField val Id`, `protected constructor`, `abstract`-методы
  переопределяются Java-подклассами.
- `SimpleFileType.kt` — наследует `FileType`.
- `FileTypeFB2.kt`, `FileTypeEpub.kt`, `FileTypeCBZ.kt`, `FileTypeDjVu.kt`, `FileTypeHtml.kt` —
  конкретные типы файлов, наследуют `FileType` (package-private Java → public Kotlin).
- `FileTypePalm.kt` — `abstract`, `protected fun palmFileType` (был `protected static` в Java, вызовы
  только из подклассов, поэтому безопасно сделан инстанс-методом).
- `FileTypeMobipocket.kt` — наследует `FileTypePalm`.
- `FileTypeCollection.kt` — синглтон `class` с `private constructor`, `companion object { @JvmField val
  Instance }` (Java-доступ `FileTypeCollection.Instance` сохранён).

**`org.geometerplus.android.fbreader.libraryService`**

- `PositionWithTimestamp.kt` — `Parcelable` с `@JvmField val ParagraphIndex/ElementIndex/CharIndex/Timestamp`,
  private primary-конструктор `(Int, Int, Int, Long)` + публичный вторичный `(ZLTextPosition)`. `CREATOR`
  перенесён в `companion object` с `@JvmField` (Java/AIDL-доступ `PositionWithTimestamp.CREATOR` сохранён).

**`org.geometerplus.zlibrary.core.encodings`**

- `Encoding.kt`, `EncodingCollection.kt`, `AutoEncodingCollection.kt` — мигрированы ранее.
- `EncodingConverter.kt` — `@JvmField val Name: String`; методы `convert(ByteArray, Int, Int, CharArray): Int`
  и `reset()`. Сигнатуры критичны: вызываются из JNI (`Method_EncodingConverter_convert` `([BII[C)I`,
  `Method_EncodingConverter_reset` `()`, `Field_EncodingConverter_Name`). Параметр `in` переименован в
  `bytes` (`in` — ключевое слово Kotlin), на JVM-сигнатуру это не влияет.
- `FilteredEncodingCollection.kt` — `abstract class` (был package-private), `init`-блок парсит
  `encodings/Encodings.xml`; вложенный `inner class EncodingCollectionReader : DefaultHandler`. Переопределяет
  `encodings()`/`getEncoding(String)`/`getEncoding(Int)`; `providesConverterFor(String)` вызывается из JNI.
- `JavaEncodingCollection.kt` — `private constructor`, синглтон через `companion object { @JvmStatic fun
  Instance() }` (JNI `StaticMethod_JavaEncodingCollection_Instance` `()`), `@Volatile private var ourInstance`.

**`org.geometerplus.android.util`**

- `DeviceType.kt` — `enum class` c `companion object { @JvmStatic fun Instance() }` (Java-доступ
  `DeviceType.Instance()`), `fun hasButtonLightsBug()`.
- `PackageUtil.kt` — `abstract class` → `object` с `@JvmStatic` (`canBeStarted`/`installFromMarket`), private
  `marketUri`.
- `SQLiteUtil.kt` — `abstract class` → `object` с `@JvmStatic` (`bindString`/`bindLong`/`bindDate`/`getDate`),
  nullable-параметры (`String?`/`Long?`/`Date?`) там, где Java допускала `null`.
- `UIMessageUtil.kt` — `abstract class` → `object` с `@JvmStatic` (`showMessageText`/`showErrorMessage`).
- `ViewUtil.kt` — `class` → `object` с `@JvmStatic` (`findView`/`findTextView`/`findImageView`/`setSubviewText`).
  Мёртвый код (нет вызовов в репозитории), мигрирован для единообразия.
- `UIUtil.kt` — `abstract class` → `object`. Статические поля (`ourMonitor`/`ourTaskQueue`/`ourProgress`/
  `ourProgressHandler`) стали свойствами объекта; `synchronized`-блоки → `synchronized(...)`,
  `wait`/`notify` → `(obj as java.lang.Object).wait()/notify()` (Kotlin `Any` не экспонирует их). Вложенный
  `private class ProgressHandler` (был `private static class`) обращается к `ourProgress` напрямую.
  Возврат анонимного `ZLApplication.SynchronousExecutor` через `object : ...`.

**`org.geometerplus.fbreader.formats`**

- `BookReadingException.kt` — `public final class extends Exception` → `class BookReadingException : Exception`
  с `@JvmField val File: ZLFile`. Три вторичных конструктора (Java-варианты `(String, ZLFile, String[])`,
  `(String, ZLFile)`, `(IOException, ZLFile)`); `File` инициализируется в теле конструкторов (нет primary-конструктора).
  `getResourceText` — private-функция в `companion object` (в Java `private static`); Java `String... params` →
  `vararg`, вызов через spread `*params`. `replaceFirst("%s", p)` — литерально идентичен Java (в `%s` нет
  regex-метасимволов).
- `ExternalFormatPlugin.kt` — `public abstract class extends FormatPlugin` → `abstract class` с `protected
  constructor` (Java `protected ExternalFormatPlugin(SystemInfo, String)`). Абстрактный `packageName()`
  (новый метод); `priority() = 10`; `readCover(file)` возвращает `PluginImage` (covariant, Java-сигнатура
  `PluginImage readCover(ZLFile)`); `supportedEncodings()` → `AutoEncodingCollection()` (Kotlin-класс,
  covariant над `EncodingCollection`); `readAnnotation(file)` → `String?` (Java `return null`).
  `PluginImage(file, this)` — package-private Java-конструктор из того же пакета. `ExternalFormatPlugin`
  используется из Java как тип (`AndroidImageSynchronizer`, `PluginImage.Plugin`, `PluginCollection`,
  `FBReaderApp`) и как `plugin.packageName()` из Kotlin `PluginUtil` — метод-вызов сохранён.
- `ComicBookPlugin.kt` / `DjVuPlugin.kt` — `public class extends ExternalFormatPlugin` → `class` с public
  конструктором `(SystemInfo)`; `new ComicBookPlugin(systemInfo)` из `PluginCollection.java` работает.
  `readMetainfo` пустой; `readUids` — `book.uids().isEmpty()` (`List<UID>`), `book.addUid(BookUtil.createUid(
  book, "SHA-256"))` (`createUid` возвращает `UID`).
- `PDFPlugin.kt` — `public class extends ExternalFormatPlugin` → `class`. `file != file.getPhysicalFile()` →
  `file !== file.getPhysicalFile()` (референсное сравнение, как Java `!=`); `new PDFDocument(...)` →
  `PDFDocument(book.getPath())`; `info.getTitle()/getAuthor()` без явного типа `PDFDocInfo` (инференс).
  `catch (Throwable e)` → `catch (e: Throwable)`.

**`org.geometerplus.fbreader.formats.oeb`**

- `ContainerFileReader.kt` — package-private `class extends ZLXMLReaderAdapter` → `class ContainerFileReader :
  ZLXMLReaderAdapter()`. `private String myRootPath` → `private var myRootPath: String?`; `getRootPath()` →
  `String?` (Java-вызов `reader.getRootPath() != null`). `equalsIgnoreCase` → `equals(tag, ignoreCase = true)`;
  `override fun startElementHandler(...): Boolean`.

**`org.geometerplus.fbreader.fbreader`**

- `VolumeKeyTurnPageAction.kt` — package-private `class extends FBAction` → `internal class
  VolumeKeyTurnPageAction(fbreader: FBReaderApp, private val myForward: Boolean) : FBAction(fbreader)`.
  `run(vararg params: Any?)` использует `Reader.PageTurningOptions.horizontal.getValue()` /
  `animationSpeed.getValue()` и `Reader.getViewWidget().startAnimatedScrolling(PageIndex, Direction, Int)`;
  `ZLView.Direction`/`ZLView.PageIndex` → `ZLViewEnums.Direction`/`ZLViewEnums.PageIndex`.
- `BookmarkHighlighting.kt` — `public final class` → `class` с `internal constructor` (package-private в
  Java). Поля `@JvmField val Collection: IBookCollection<*>` и `@JvmField val Bookmark: Bookmark` —
  читаются как Java-поля (`((BookmarkHighlighting) h).Bookmark` в `FBView.java`, `new
  BookmarkHighlighting(view, Collection, b)` в `FBReaderApp.java`). Java raw `IBookCollection` →
  `IBookCollection<*>`. `startPosition`/`endPosition` — private-функции в `private companion object`
  (вызываются в super-конструкторе). `getBackgroundColor`/`getForegroundColor` →
  `Collection.getHighlightingStyle(Bookmark.getStyleId())?.getXxxColor()` (nullable `ZLColor?`).
- `DictionaryHighlighting.kt` — `public final class` → `class` с `private constructor` и `companion
  object { @JvmStatic fun get(...) }` (Java `public static get`). Мёртвый код (нет внешних вызовов).
  После `getSelectionHighlighting()`/`getStartPosition()`/`getEndPosition()` — nullable-типы
  (`ZLTextHighlighting?`, `ZLTextPosition?`) со smart-cast после null-проверок.

**`org.geometerplus.zlibrary.core.view`**

- `ZLViewEnums.kt` — `interface`-пространство имён с тремя вложенными `enum class` (`PageIndex`, `Direction`,
  `Animation`). Имена констант сохранены в нижнем регистре (Java-доступ `ZLViewEnums.PageIndex.previous` и т.п.)
  с `@Suppress("EnumEntryName")`. `Direction` — `enum class Direction(@JvmField val IsHorizontal: Boolean)`
  (Java читает `myDirection.IsHorizontal` как поле). `getNext()`/`getPrevious()` оставлены как обычные `fun`
  (вызываются из `BitmapManagerImpl.java`), возвращают `PageIndex?` (Java `default: return null`).
- `UnionHull.kt` — `class UnionHull(vararg components: Hull) : Hull`. Java `Hull...` → `vararg`;
  `List<Hull> myComponents` → `listOf(*components)` (неизменяемый — список не мутируется). Реализует три
  метода интерфейса `Hull` (уже Kotlin).
- `HorizontalConvexHull.kt` — `class HorizontalConvexHull(rects: MutableCollection<Rect>) : Hull`. Java
  `Collection<Rect>` → `MutableCollection<Rect>` (иначе `ArrayList<Rect>` из `HullUtil` не подходит, см.
  правило ниже). `LinkedList<Rect>` + явные `ListIterator` (`iter.add`/`iter.previous`/`iter.next` в
  `addRect`/`normalize`). `Math.min/max(int,int)` → `minOf`/`maxOf`; `getFirst()`/`getLast()` → `first()`/`last()`.
  `int[]` → `IntArray` через `LinkedList<Int>.toIntArray()`; `ZLPaintContext.fillPolygon/drawOutline(IntArray,
  IntArray)`. Константы `Hull.DrawMode.None/Fill/Outline` доступны как `Hull.DrawMode.X`.
- `ZLViewWidget.kt` — `public interface` → `interface`. Параметры `ZLView.Direction`/`ZLView.PageIndex` →
  `ZLViewEnums.Direction`/`ZLViewEnums.PageIndex` (вложенные enum-классы из `ZLViewEnums`; `ZLView` их
  наследует). Java-реализация `ZLAndroidWidget` и Kotlin-реализация `ScrollWidget` совместимы. Геттер/сеттер
  `getScreenBrightness()`/`setScreenBrightness()` оставлены как обычные `fun` (Kotlin-вызов
  `widget.screenBrightness` заменён на `widget.getScreenBrightness()` в `BrightnessGesture.kt`).

**`org.geometerplus.zlibrary.text.model`**

- `ExtensionEntry.kt` — `@JvmField val Type: String` и `@JvmField val Data: Map<String, String>`; package-private
  конструктор Java → public primary-конструктор (вызывается из `ZLTextPlainModel` того же пакета); `equals`/`hashCode`
  как в Java.
- `ZLTextMetrics.kt` — `@JvmField val DPI/FullWidth/FullHeight/FontSize: Int`; `equals` сравнивает только
  `DPI`/`FullWidth`/`FullHeight` (без `FontSize`), `hashCode` = `DPI + 13 * (FullHeight + 13 * FullWidth)`.
- `ZLTextModel.kt` — `public interface` → `interface`. Возвращаемые типы nullable (`String?` для
  `getId()`/`getLanguage()`, `ZLTextMark?` для mark-методов, `ZLTextParagraph?` для `getParagraph`) —
  соответствуют Kotlin-переопределениям в `bookreader` (`DjvuTextModel`/`PDFTextModel`/`ComicsTextModel`) и
  защитным проверкам `getParagraph(i) ?: continue` в `BookContextService.kt` / `if (paragraph == null)` в
  `FBReaderView.kt`. `getMarks()` возвращает read-only `List<ZLTextMark>` (Java `List` → `List`).

**`org.geometerplus.zlibrary.text.view`**

- `HullUtil.kt` — package-private `abstract class` со static-методами → `object HullUtil` с `@JvmStatic`.
  Две перегрузки `hull` (`Array<ZLTextElementArea>` и `List<ZLTextElementArea>`); Java-вызовы
  `HullUtil.hull(...)` из `ZLTextHighlighting.java`/`ZLTextRegion.java` (тот же пакет) работают через
  статические bridge-методы. `List<Rect>` → `ArrayList<Rect>`, построение через `Rect(a.XStart, a.YStart,
  a.XEnd, a.YEnd)`. `HorizontalConvexHull` и `UnionHull` вызываются как Kotlin/Java-конструкторы.
- `ZLTextHyperlink.kt` — `class ZLTextHyperlink(@JvmField val Type: Byte, @JvmField val Id: String?)`.
  Package-private конструктор/методы Java → public (вызываются из `ZLTextHyperlinkControlElement.kt` и
  `ZLTextParagraphCursor.java` того же пакета). `List<Integer>` → `MutableList<Int>?` (ленивая
  инициализация), `addElementIndex` без `!!` (локальная `var`). `NO_LINK` — `@JvmField val` в
  `companion object` (Java `ZLTextHyperlink.NO_LINK`); `(byte) 0` → `0.toByte()`. `Collections.unmodifiableList`
  → возвращает `MutableList<Int>` (Java `List`), совместимо с `List<Int>` (read-only) в сигнатуре.
- `ExtensionElementManager.kt` — `abstract class` с package-private `final getElements(ExtensionEntry)` →
  `fun getElements(...)` (final по умолчанию) и `protected abstract getElements(String, Map)`. Java
  `List<? extends ExtensionElement>` → `List<out ExtensionElement>`. Реальных подклассов нет (FBView
  возвращает `null` из `getExtensionManager()`).
- `ZLTextSimpleHighlighting.kt` — `public abstract class` → `abstract class` с `protected constructor`
  и `protected val View: ZLTextView` (Java `protected final ZLTextView View`). `new ZLTextFixedPosition(start)`
  — в property-инициализаторах (`myStartPosition`/`myEndPosition`). `getStartArea`/`getEndArea` вызывают
  package-private `getFirstAfter`/`getLastBefore` из `ZLTextElementAreaVector` (тот же пакет). Параметры
  конструктора non-null `ZLTextPosition` → в `bookreader/ZLBookmark.kt` потребовались `!!` на `b.start`/`b.end`
  (nullable `Storage.Bookmark` поля).
- `ZLTextManualHighlighting.kt` — package-private `class` → `internal class`. Переопределяет
  `getBackgroundColor`/`getForegroundColor`/`getOutlineColor` как nullable `ZLColor?`, читая
  `View.getHighlightingBackgroundColor()`/`getHighlightingForegroundColor()` (Java `abstract`-геттеры
  `ZLTextViewBase`); `getOutlineColor()` возвращает `null`.

**`org.geometerplus.zlibrary.core.resources`**

- `ZLMissingResource.kt` — package-private `final class`-синглтон → `class ZLMissingResource private
  constructor() : ZLResource(Value)`. `Value` — `const val` в `companion object` (доступен как
  `ZLMissingResource.Value` из Java и в super-конструкторе как константа). `Instance` — `@JvmField val`
  в `companion object`. `getResource` возвращает `this`; `hasValue()/getValue()` как в Java.

**`org.geometerplus.zlibrary.core.xml`**

- `ZLXMLReader.kt` — `public interface` → `interface`. `char[]` → `CharArray`; `namespaceMapChangedHandler`
  `Map<String,String>` → `MutableMap<String, String>` (Java `Map` маппится в `MutableMap` в позиции параметра);
  `collectExternalEntities` `HashMap<String, char[]>` → `HashMap<String, CharArray>`; `externalDTDs()`
  возвращает read-only `List<String>`. Java-реализация `ZLXMLReaderAdapter` совместима без изменений.

**`org.geometerplus.zlibrary.ui.android.util`**

- `ZLAndroidColorUtil.kt` — `public abstract class` со static-методами → `object ZLAndroidColorUtil` с
  `@JvmStatic`. `ZLColor.Red/Green/Blue` — `@JvmField val ...: Short`, поэтому в вызовах `Color.argb/rgb`
  нужен явный `.toInt()` (Kotlin не расширяет `Short`→`Int` автоматически, в отличие от Java). Битовые
  операции: `color & 0xFF0000` → `color and 0xFF0000` (Int), накопление в `Long` через `.toLong()`;
  `r >>= 16` → `r = r shr 16`; `(int)(r & 0xFF)` → `(r and 0xFFL).toInt()`.

**`org.geometerplus.android.fbreader.httpd`**

- `DataUtil.kt` — `public abstract class` со static-методами → `object DataUtil` с `@JvmStatic`.
  `fileFromEncodedPath` — package-private → public; `encodedPath.split("X")` (Kotlin-`split` по литералу,
  пустые хвосты отбрасываются в цикле `isEmpty()`), `Short.parseShort(item, 16)` → `item.toShort(16)`,
  `(char) ...` → `.toChar()`. `buildUrl` возвращает `String?` (Java `return null`); `String.format(Locale.ROOT,
  "X%X", (short) char)` → `String.format(Locale.ROOT, "X%X", path[i].toShort())`.

**`org.geometerplus.android.fbreader.dict`**

- `InternalUtil.kt` — package-private `abstract class` со static-методами → `object InternalUtil` с
  `@JvmStatic`. Java-вызовы `InternalUtil.startDictionaryActivity(...)`/`showSnackbar(...)` (из `Dictan`,
  `ColorDict`, `DictionaryUtil` того же пакета) работают через статические bridge-методы. `@NonNull`
  аннотации опущены (Kotlin non-null типы по умолчанию). `findViewById` → `findViewById<View>(...)` с
  null-проверкой.
- `ColorDict.kt` — package-private `final class extends DictionaryUtil.PackageInfo` →
  `class ColorDict(id, title) : DictionaryUtil.PackageInfo(id, title)`. Переопределяет package-private
  `abstract open(...)` как public `override fun open(...)` (Kotlin допускает расширение видимости).
  Private-интерфейс `ColorDict3` (Java `String`-константы в `interface`) → `private object ColorDict3` с
  `const val`. `frameMetrics.Height/Gravity` — Java public-поля `PopupFrameMetric` (доступ как свойства).
  Каст `(ZLAndroidApplication) fbreader.getApplication()` → `fbreader.application as ZLAndroidApplication`;
  `ShowStatusBarOption.getValue()` сохранён как вызов геттера.
- `Dictan.kt` — package-private `final class extends DictionaryUtil.PackageInfo` →
  `class Dictan(id, title) : DictionaryUtil.PackageInfo(id, title)`. Переопределяет `open(...)` и
  `onActivityResult(...)` (оба package-private в Java → public в Kotlin). Статика (`MAX_LENGTH_FOR_TOAST`,
  `REQUEST_DICTIONARY`, `trimArticle`, `showError`) → `private companion object`. `switch (code)` →
  `when (code)`; `replaceAll("%s", word)` → `replace("%s", word!!)` (Java полагалась на non-null `word`).
  Octal-escape `"\000"` (NUL) → `" "` (в Kotlin нет octal-escape). Java-lambda `v -> {...}` →
  SAM-конверсия `View.OnClickListener` с `{ _ -> ... }`; неиспользуемая `finalText` отброшена.
  `startActivityForResult` (deprecated) оставлен как есть — предсуществующее поведение.

**`org.amse.ys.zip`**

- `LocalFileHeader.kt` — `public class` с `@JvmField`-полями (`FileName: String?` + 12 `Int`-полей:
  `Signature`/`Version`/`Flags`/`CompressionMethod`/`ModificationTime`/`ModificationDate`/`CRC32`/
  `CompressedSize`/`UncompressedSize`/`NameLength`/`ExtraLength`/`DataOffset`). Java-доступ
  `header.Signature`/`header.FileName`/`header.Flags`/`header.CompressedSize`/`header.UncompressedSize`
  из `ZipFile.java` сохранён через `@JvmField`. `readFrom(stream)` — package-private в Java → public
  (вызывается из `ZipFile.java`); `switch (Signature)` → `when (Signature)` по `const val`-сигнатурам
  (`FILE_HEADER_SIGNATURE`/`FOLDER_HEADER_SIGNATURE`/`END_OF_CENTRAL_DIRECTORY_SIGNATURE`/
  `DATA_DESCRIPTOR_SIGNATURE` в `companion object`).
- `Decompressor.kt` — `public abstract class`. Абстрактные `read(ByteArray?, Int, Int)`/`read()`
  помечены `@Throws(IOException::class)` — иначе Java-подкласс `DeflatingDecompressor` не может их
  override (`throws IOException` в Java). Package-private static `init`/`storeDecompressor` → `@JvmStatic
  fun` (public; `internal`-вариант манглится в `init$module` и `ZipFile.java` не находит символ).
  `init` дополнительно `@Throws(IOException::class)` (оригинал `throws IOException`). Очередь
  `ourDeflators` — `Queue<DeflatingDecompressor>`; мёртвый public-конструктор `(MyBufferedInputStream,
  LocalFileHeader)` сохранён для совместимости API.
- `NoCompressionDecompressor.kt` — `public final class extends Decompressor` → `class ... :
  Decompressor()`. `available()` = `UncompressedSize - myCurrentPosition`; `read(ByteArray?, Int, Int)`
  использует `minOf(len, left)`.
- `ZipInputStream.kt` — package-private `final class extends InputStream` → `internal class ... :
  InputStream()`. `finalize()` → `@Suppress("DEPRECATION") @Throws(Throwable::class) protected fun
  finalize()` (вызывает `close()`, без `super.finalize()`). `Decompressor.init`/`storeDecompressor`
  вызываются как статические (в `init`-блоке и `close()`).
- `MyBufferedInputStream.java` — **не мигрирован**, но сделан `public` (`final class` → `public final
  class`): иначе public-сигнатуры Kotlin (`Decompressor.init`, `LocalFileHeader.readFrom`, конструктор
  `NoCompressionDecompressor`) «протекали» бы package-private тип `MyBufferedInputStream`, а `internal`
  альтернатива манглирует имена и ломает вызовы из `ZipFile.java`/`DeflatingDecompressor.java`.

### Правила межъязыковой совместимости

- Публичные Java-поля (`public final X Field`) → `@JvmField val Field` (иначе Java-доступ `obj.Field`
  сломается на геттере `getField()`).
- Публичные статические методы → `@JvmStatic` в `companion object`; статические поля → `@JvmField`.
- Константы, используемые как `case`-метки в Java `switch`, → `const val` (даёт `static final`),
  а не `@JvmField` (даёт не-final поле).
- Java-геттеры/сеттеры (`getXxx`/`setXxx`), к которым обращаются Java-вызывающие, сохранять как
  обычные `fun` (не переводить в Kotlin-свойства, если поле публичное и читается как поле).
- Package-private конструкторы/классы Java → public (расширение видимости безопасно для компиляции).

---

## Примечания

- Код написан на Java (идёт выборочная миграция на Kotlin)
- Содержит нативный код (требует NDK для сборки)
- Использует сложную систему опций/настроек (`ZLOption`)
- Поддержка RTL языков
- При восстановлении функционала использовать **Compose** вместо Activity
