---
name: ui-improvements-task-list
description: Список задач по улучшению UI SV APP (2026-06-28) - ЗАВЕРШЕНО
metadata: 
  node_type: memory
  type: project
  originSessionId: dda640a3-ec9f-4c17-8edf-0e15fc1a8b26
---

# UI Improvements Task List

Дата создания: 2026-06-28
**Статус: ЗАВЕРШЕНО**

## Выполненные задачи

### ✅ Простые задачи

#### 1. Иконки смены темы
**Решение:** Изменена логика в ThemeToggleIcon.kt - DARK показывает солнце (переключит на LIGHT), LIGHT показывает луну (переключит на DARK).

#### 2. Кнопка "Показать ещё"
**Статус:** Не найдена в проекте (возможно в wiki модуле)

#### 3. Удалить "Тема приложения" из настроек читалки
**Решение:** Удалён пункт SettingsItem для темы из ReaderSettingsContent.kt, убраны неиспользуемые импорты.

#### 4. Отступ 16.dp внизу списков
**Решение:** Добавлен contentPadding с bottom = 16.dp для:
- BookList.kt (LazyVerticalGrid)
- BookmarksScreen.kt (NotesListContent, BooksListContent, BookNotesContent)
- DownloadedBooksList.kt

#### 5. Обводка у кнопки "Перейти" на заметках
**Решение:** Заменил TextButton на OutlinedButton в NoteItem.kt.

#### 6. Обводка и фон у кнопки скачивания/чтения на карточке книги
**Решение:** Изменён цвет фона: tertiaryContainer → surfaceContainerHigh, цвет обводки: outlineVariant → outline, цвет индикатора: onTertiaryContainer → onSurfaceVariant.

### ✅ Средние задачи

#### 7. Сохранение режима просмотра закладок в SharedPrefs
**Решение:**
- Создан BookmarksViewModePrefsRepository.kt
- Обновлен BookmarksViewModel.kt - загружает режим при init, сохраняет при toggleViewMode()

#### 8. Улучшение FullScreenLoading с красивой анимацией
**Решение:**
- Создан AnimatedLoadingIndicator.kt с тремя точками пульсации
- Удалён старый FullScreenLoading.kt

### ✅ Сложные задачи

#### 9. Название книги в заметках
**Решение:**
- Добавлены параметры bookTitle и bookAuthor в ReaderScreen, ReaderContent, ReaderActions, ReaderViewModel
- При загрузке книги из API название и author сохраняются в Storage.info (перезаписывают метаданные файла)
- Обновлены вызовы ReaderScreen в BookDetailUi.kt, RootBooksCatalog.kt, BookmarksScreen.kt

#### 10. Удаление книги - поведение закладок
**Решение:**
- Добавлена кнопка "К книге" (bookmarks_go_to_book) в NoteItem.kt (показывается если bookFileUri == null)
- Добавлен BookmarksAction.OnBookCardClick и BookmarksEffect.OpenBookCard
- При нажатии на "К книге" открывается BookDetailScreen с информацией о книги из заметки

#### 11. Collapse в оглавлении с анимацией
**Решение:**
- Добавлено поле parentId в ExpandableTocItem
- Добавлена функция isItemVisible() для проверки видимости элемента (все родители раскрыты)
- Добавлен AnimatedVisibility с expandVertically/shrinkVertically для анимации

#### 12. Настройка шрифта в читалке
**Статус:** Исследование завершено. Баг связан с ScrollWidget.reset() - после смены шрифта нужно вызвать requestLayout().
**Рекомендация:** Добавить requestLayout() в FBReaderView.reset() для ScrollWidget.

---

## Созданные файлы

1. `BookmarksViewModePrefsRepository.kt` - сохранение режима просмотра закладок
2. `AnimatedLoadingIndicator.kt` - красивый индикатор загрузки

## Изменённые файлы

- ThemeToggleIcon.kt
- ReaderSettingsContent.kt
- BookList.kt
- BookmarksScreen.kt (множественные изменения)
- DownloadedBooksList.kt
- NoteItem.kt
- BookItem.kt
- BookmarksViewModel.kt
- ReaderScreen.kt
- ReaderContent.kt
- ReaderActions.kt
- ReaderViewModel.kt
- BookDetailUi.kt
- RootBooksCatalog.kt
- strings.xml (books)

---

## Связанные документы

- [[design-system]] - дизайн-система SV APP
- [[bookreader-bookmarks-feature]] - функционал закладок
- [[compose-optimizations]] - оптимизации Compose