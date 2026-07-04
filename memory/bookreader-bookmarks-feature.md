---
name: bookreader-bookmarks-feature
description: Функционал закладок в модуле bookreader
metadata: 
  node_type: memory
  type: project
  originSessionId: 29690ec3-e877-473b-87e2-a47d53f84914
---

# Функционал закладок в BookReader

**Дата реализации:** 2026-06-22

## Обзор

Полнофункциональная система закладок для читалки книг с поддержкой создания, редактирования, удаления и навигации.

## Компоненты

### 1. Модель данных

**Storage.Bookmark** (`bookreader/src/main/java/com/github/axet/bookreader/app/Storage.java`)

```java
public static class Bookmark {
    public long last;          // timestamp последнего изменения
    public String name;        // пользовательское название (опционально)
    public String text;        // текст закладки (выдержка из книги)
    public int color;          // цвет выделения (ARGB)
    public ZLTextPosition start;  // начальная позиция (paragraph, element, char)
    public ZLTextPosition end;    // конечная позиция
}
```

### 2. UI компоненты

#### BookmarksComposeDialog
**Файл:** `bookreader/src/main/java/com/github/axet/bookreader/screens/ui/BookmarksComposeDialog.kt`

Диалог со списком закладок:
- Отображение номера страницы (С. N)
- Текст закладки (до 100 символов)
- Название закладки (если задано)
- Кнопка удаления с анимацией
- Клик по закладке → переход к позиции

#### BookmarkBottomSheet
**Файл:** `bookreader/src/main/java/com/github/axet/bookreader/screens/ui/BookmarkBottomSheet.kt`

BottomSheet для редактирования закладки:
- Заголовок "Закладка"
- Текст закладки (read-only)
- Поле редактирования названия
- Выбор цвета (7 цветов)
- Кнопки "Удалить" и "Сохранить"

### 3. Цвета закладок

```kotlin
val BOOKMARK_COLORS = listOf(
    Color(0xFFFF0000), // Красный
    Color(0xFFFF8000), // Оранжевый
    Color(0xFFFFFF00), // Жёлтый
    Color(0xFF00FF00), // Зелёный
    Color(0xFF0000FF), // Синий
    Color(0xFF3F00FF), // Индиго
    Color(0xFF7F00FF), // Фиолетовый
)
```

## Логика работы

### Создание закладки

1. Пользователь выделяет текст в книге
2. Вызывается ActionCode.SELECTION_BOOKMARK
3. Создаётся `Storage.Bookmark` с текстом, start и end позициями
4. Закладка сохраняется в `FBook.info.bookmarks` и `Storage.Book.info.bookmarks`
5. Выделение перерисовывается с цветом

### Редактирование закладки

1. Пользователь нажимает на выделенную закладку в тексте
2. `FBReaderView.Listener.onEditBookmark(bookmark)` → открывается BookmarkBottomSheet
3. Пользователь редактирует название и выбирает цвет
4. При сохранении — обновляется `bookmark.name`, `bookmark.color`, `bookmark.last`
5. Синхронизация между FBook и Storage.Book

### Удаление закладки

1. Через BookmarkBottomSheet → кнопка "Удалить"
2. Через BookmarksComposeDialog → иконка корзины
3. Удаление из обоих списков: `FBook.info.bookmarks` и `Storage.Book.info.bookmarks`
4. Обновление отображения в читалке

### Навигация к закладке

**Постраничный режим (PagerWidget):**
```kotlin
// Игнорируем offset, открываем страницу как в TOC
val position = ZLTextFixedPosition(
    bookmark.start.paragraphIndex,
    0,  // elementIndex = 0
    0   // charIndex = 0
)
gotoPosition(position)
widget?.reset()
widget?.repaint()
```

**Непрерывный режим (ScrollWidget):**
```kotlin
// Игнорируем offset, открываем страницу по центру
val position = ZLTextFixedPosition(
    bookmark.start.paragraphIndex,
    0,
    0
)
gotoPositionCentered(position)
```

## Архитектура

### Синхронизация данных

Проблема: `FBook.info` — копия `Storage.Book.info`, изменения не синхронизируются автоматически.

Решение:
- `syncBookmarksFromFBook()` — копирует закладки из FBook в Storage.Book
- `onBookmarksUpdate()` — callback при изменении закладок
- При создании/редактировании/удалении — обновляются оба объекта

### Методы FBReaderView

```java
// Обычный переход
public void gotoPosition(ZLTextPosition p)

// Переход с центрированием (для непрерывного режима)
public void gotoPositionCentered(ZLTextPosition p)

// Обновление отображения закладок
public void bookmarksUpdate()
```

### Состояние ViewModel

```kotlin
data class Content(
    // ...
    val showBookmarkEdit: Boolean = false,
    val editingBookmark: Storage.Bookmark? = null,
)
```

### Actions

```kotlin
sealed class ReaderActions {
    data class EditBookmark(val bookmark: Storage.Bookmark) : ReaderActions()
    data class SaveBookmarkEdit(val bookmark: Storage.Bookmark, val name: String, val color: Int) : ReaderActions()
    data class AddBookmark(val bookmark: Storage.Bookmark) : ReaderActions()
    data class DeleteBookmark(val bookmark: Storage.Bookmark) : ReaderActions()
    data class GoToBookmark(val bookmark: Storage.Bookmark) : ReaderActions()
}
```

## Строковые ресурсы

Все строки вынесены в ресурсы с префиксом `sv_`:

```xml
<string name="sv_bookmarks_title">Закладки</string>
<string name="sv_bookmarks_empty">Нет сохранённых закладок</string>
<string name="sv_bookmark_title">Закладка</string>
<string name="sv_bookmark_name_label">Название</string>
<string name="sv_bookmark_name_hint">Введите название закладки</string>
<string name="sv_bookmark_color_label">Цвет выделения</string>
<string name="sv_bookmark_save">Сохранить</string>
<string name="sv_bookmark_page_prefix">С.</string>
<string name="sv_delete">Удалить</string>
```

## Файлы

### Созданные
- `BookmarkBottomSheet.kt` — BottomSheet редактирования
- `BookmarksComposeDialog.kt` — Диалог списка закладок

### Изменённые
- `ReaderState.kt` — добавлены `showBookmarkEdit`, `editingBookmark`
- `ReaderActions.kt` — добавлены действия для закладок
- `ReaderViewModel.kt` — добавлены методы для работы с закладками
- `ReaderContent.kt` — интеграция UI компонентов
- `FBReaderView.java` — добавлен `gotoPositionCentered()`, `scrollCentered`
- `ScrollWidget.java` — логика центрирования страницы
- `strings.xml` — строковые ресурсы с префиксом `sv_`

---

**Связанные задачи:**
- [[fbreader-refactoring-plan]]
- [[fbreader-activity-cleanup]]
