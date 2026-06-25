# FBReader Module

Библиотека для чтения электронных книг.

## Обзор

Модуль `fbreader` — это порт библиотеки FBReader для Android. Предоставляет полную функциональность для чтения электронных книг различных форматов.

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
├── library/        # Библиотека книг
├── sort/           # Сортировка
├── tips/           # Подсказки
├── tree/           # Деревья навигации
└── util/           # Утилиты
```

> **Примечание:** Пакет `network/` удалён (см. раздел "Удалённый сетевой функционал")

### org.geometerplus.zlibrary.core
Кроссплатформенное ядро:

```
zlibrary/core/
├── encodings/      # Кодировки
├── drm/            # DRM защита
├── filesystem/     # Файловая система
├── filetypes/      # Типы файлов
├── image/          # Работа с изображениями
├── language/       # Языки
├── library/        # Библиотечные функции
├── money/          # Валюты
├── network/        # Сеть
├── options/        # Опции/настройки
├── resources/      # Ресурсы
├── tree/           # Деревья
├── util/           # Утилиты
├── xml/            # XML парсинг
└── constants/      # Константы
```

### org.geometerplus.zlibrary.ui.android
Android-специфичный UI:

```
zlibrary/ui/android/
├── views/          # Кастомные View (ZLAndroidWidget, MainView)
└── resources/      # Android ресурсы
```

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
> **Причина:** Сетевые функции (OPDS каталоги, Atom feed, RSS) не используются в приложении. Книги загружаются через модуль `books`.

### Удалённые пакеты

| Пакет | Описание | Файлов | Строк кода |
|-------|----------|--------|------------|
| `org.geometerplus.fbreader.network` | Основные сетевые классы | ~40 | ~5000 |
| `org.geometerplus.fbreader.network.atom` | Atom feed парсеры | ~15 | ~1500 |
| `org.geometerplus.fbreader.network.opds` | OPDS каталоги | ~20 | ~2500 |
| `org.geometerplus.fbreader.network.urlInfo` | URL информация | ~6 | ~500 |
| `org.geometerplus.fbreader.network.tree` | Деревья навигации | ~12 | ~1000 |
| `org.geometerplus.fbreader.network.authentication` | Аутентификация | ~3 | ~400 |

**Итого:** ~10 000 строк кода, 141 файл.

### Что было удалено

- `NetworkLibrary.java` — управление сетевыми каталогами
- `NetworkBookItem.java` — элемент книги из каталога
- `NetworkCatalogItem.java` — элемент каталога
- `OPDSNetworkLink.java` — OPDS ссылка
- `ATOMFeedHandler.kt` — обработчик Atom feed
- `BookDownloaderInterface.aidl` — AIDL интерфейс для скачивания

### Оставшийся функционал

Скачивание книг реализовано в модуле `books` через системный `DownloadManager`.

---

# Удалённый функционал (Legacy Activity)

> **Дата удаления:** 2026-06-22
> **Причина:** Переход на Compose UI в модуле `bookreader`. Все Activity были legacy кодом от оригинального FBReader, не зарегистрированы в манифесте и заменены на Compose экраны.

## Удалённые Activity и их назначение

### Основные Activity чтения

| Activity | Назначение | Требуется восстановление |
|----------|------------|-------------------------|
| `FBReader.java` | Главная Activity для чтения книг. Управляла открытием книг, навигацией, жестами, настройками экрана. | ❌ Нет — заменён на Compose `ReaderScreen` в bookreader |
| `FBReaderMainActivity.java` | Базовый класс для FBReader. Содержал общую логику для Activity чтения. | ❌ Нет |

### Библиотека книг

| Activity | Назначение | Требуется восстановление |
|----------|------------|-------------------------|
| `LibraryActivity.java` | Экран библиотеки книг. Отображение дерева книг (по авторам, сериям, тегам). Поиск по библиотеке. | ⚠️ Возможно — если понадобится отдельный экран библиотеки вне main модуля |
| `BookInfoActivity.java` | Детальная информация о книге (обложка, описание, авторы, теги). | ⚠️ Возможно — для экрана деталей книги |
| `LibrarySearchActivity.java` | Поиск по локальной библиотеке. | ✅ Да — реализовать в Compose |

### Закладки

| Activity | Назначение | Требуется восстановление |
|----------|------------|-------------------------|
| `BookmarksActivity.java` | Список закладок с фильтрацией (по книге, все книги). Группировка по дате/книге. | ✅ Да — реализовать экран закладок в Compose |
| `EditBookmarkActivity.java` | Редактирование закладки (текст, стиль, цвет). | ✅ Да — диалог редактирования закладки |
| `EditStyleActivity.java` | Редактирование стиля закладки (цвет, шрифт). | ✅ Да — часть диалога закладки |

### Навигация по книге

| Activity | Назначение | Требуется восстановление |
|----------|------------|-------------------------|
| `TOCActivity.java` | Оглавление книги (Table of Contents). Дерево глав с навигацией. | ✅ Да — реализовать как Compose BottomSheet или Dialog |
| `CancelActivity.java` | Меню при выходе из книги (сохранить позицию, выйти). | ⚠️ Возможно — диалог при выходе |

### Настройки

| Activity | Назначение | Требуется восстановление |
|----------|------------|-------------------------|
| `PreferenceActivity.java` | Экран настроек чтения (шрифты, цвета, жесты, страница). | ✅ Да — реализовать `ReaderSettingsScreen` в Compose |
| `ZLPreferenceActivity.java` | Базовый класс для экранов настроек. | ❌ Нет |
| `EditBookInfoActivity.java` | Редактирование метаданных книги (авторы, теги, заголовок). | ⚠️ Возможно — для редактирования метаданных |

### Сетевые функции

| Activity | Назначение | Требуется восстановление |
|----------|------------|-------------------------|
| `NetworkLibraryActivity.java` | Каталоги OPDS, сетевые библиотеки. Скачивание книг из интернета. | ⚠️ Возможно — для OPDS каталогов |
| `NetworkBookInfoActivity.java` | Информация о книге из сетевого каталога. | ⚠️ Зависит от NetworkLibraryActivity |
| `AuthenticationActivity.java` | Авторизация в сетевых библиотеках. | ⚠️ Зависит от NetworkLibraryActivity |
| `AddCustomCatalogActivity.java` | Добавление пользовательского OPDS каталога. | ⚠️ Зависит от NetworkLibraryActivity |
| `BookDownloader.java` | Activity для загрузки книг. | ⚠️ Возможно — если нужен отдельный экран загрузки |
| `NetworkSearchActivity.java` | Поиск по сетевым каталогам. | ⚠️ Зависит от NetworkLibraryActivity |
| `BuyBooksActivity.java` | Покупка книг в каталогах. | ⚠️ Зависит от NetworkLibraryActivity |
| `MenuActivity.java` и наследники | Меню для сетевых каталогов. | ❌ Нет |

### Утилиты и диалоги

| Activity | Назначение | Требуется восстановление |
|----------|------------|-------------------------|
| `EditAuthorsDialogActivity.java` | Диалог редактирования списка авторов. | ✅ Да — Compose Dialog |
| `EditTagsDialogActivity.java` | Диалог редактирования тегов. | ✅ Да — Compose Dialog |
| `EditListDialogActivity.java` | Базовый класс для диалогов редактирования списков. | ❌ Нет |
| `FolderListDialogActivity.java` | Выбор папки для библиотеки. | ✅ Да — Compose Dialog |
| `DictionaryNotInstalledActivity.java` | Диалог при отсутствии словаря. | ⚠️ Возможно — заменить на Toast/Dialog |

### Обработка ошибок

| Activity | Назначение | Требуется восстановление |
|----------|------------|-------------------------|
| `BugReportActivity.java` | Экран отправки отчёта об ошибке при краше. | ⚠️ Возможно — для краш-репортов |
| `FixBooksDirectoryActivity.java` | Исправление директории книг при проблемах. | ⚠️ Возможно |
| `MissingNativeLibraryActivity.java` | Ошибка отсутствия нативной библиотеки. | ⚠️ Возможно |
| `BookReadingErrorActivity.java` | Ошибка при открытии книги. | ⚠️ Возможно |

### Прочее

| Activity | Назначение | Требуется восстановление |
|----------|------------|-------------------------|
| `ImageViewActivity.java` | Просмотр изображений из книги на весь экран. | ✅ Да — Compose экран для просмотра изображений |
| `PluginListActivity.java` | Список установленных плагинов форматов. | ❌ Нет |

---

## Удалённые Actions

Actions — это команды, которые привязывались к жестам и кнопкам в FBReader:

| Action | Назначение | Статус |
|--------|------------|--------|
| `ShowLibraryAction` | Открыть библиотеку | Удалён — библиотека в main модуле |
| `ShowBookmarksAction` | Открыть закладки | ⚠️ Требуется восстановление |
| `ShowTOCAction` | Открыть оглавление | ⚠️ Требуется восстановление |
| `ShowPreferencesAction` | Открыть настройки | ⚠️ Требуется восстановление |
| `ShowBookInfoAction` | Открыть информацию о книге | ⚠️ Требуется восстановление |
| `ShowNetworkLibraryAction` | Открыть сетевой каталог | Удалён |
| `ShowCancelMenuAction` | Меню выхода | Удалён |
| `DisplayBookPopupAction` | Попап с информацией о книге | Удалён |
| `InstallPluginsAction` | Установка плагинов | Удалён |
| `ProcessHyperlinkAction` | Обработка гиперссылок | ⚠️ Частично — нужен для сносок |
| `SelectionBookmarkAction` | Создать закладку из выделения | ⚠️ Требуется восстановление |
| `SelectionTranslateAction` | Перевод выделенного текста | ⚠️ Требуется восстановление |

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

## Примечания

- Код написан на Java (идёт миграция на Kotlin)
- Содержит нативный код (требует NDK для сборки)
- Использует сложную систему опций/настроек (`ZLOption`)
- Поддержка RTL языков
- При восстановлении функционала использовать **Compose** вместо Activity
