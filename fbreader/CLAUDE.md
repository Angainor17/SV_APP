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
├── network/        # Сетевые функции
│   ├── opds/       # OPDS каталоги
│   ├── atom/       # Atom feed
│   ├── rss/        # RSS feed
│   ├── sync/       # Синхронизация
│   └── authentication/
├── sort/           # Сортировка
├── tips/           # Подсказки
├── tree/           # Деревья навигации
└── util/           # Утилиты
```

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
├── activities/     # Activity
├── views/          # Кастомные View
├── dialogs/        # Диалоги
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
- `dragSortListview` — для списка библиотеки

## Примечания

- Код написан на Java (не Kotlin)
- Содержит нативный код (требует NDK для сборки)
- Использует сложную систему опций/настроек
- Поддержка RTL языков
