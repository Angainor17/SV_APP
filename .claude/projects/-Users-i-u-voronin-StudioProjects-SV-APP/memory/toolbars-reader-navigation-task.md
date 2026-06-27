---
name: toolbars-reader-navigation-task
description: Задача по исправлению поведения тулбаров и навигации в читалке
metadata:
  type: project
---

# Тулбары и навигация в читалке

**Дата создания:** 2026-06-26
**Дата завершения:** 2026-06-27
**Статус:** ✅ ЗАВЕРШЁНО

---

## 1. Тулбары экранов Книги, Новости, Информация

### Постановка задачи
Тулбары должны скрываться при скролле списка под ними. Они должны быть видны только когда список проскроллен в самое начало.

### Решение ✅

| Модуль | Файл | Скрытие при скролле |
|--------|------|---------------------|
| **books** | `books/src/main/java/su/sv/books/catalog/presentation/root/ui/RootBooksCatalog.kt` | ✅ Уже было реализовано |
| **news** | `news/src/main/java/su/sv/news/presentation/root/ui/RootNews.kt` | ✅ Добавлено `TopAppBarDefaults.enterAlwaysScrollBehavior()` |
| **info** | `info/src/main/java/su/sv/info/rootinfo/ui/RootInfo.kt` | ✅ Добавлено `TopAppBarDefaults.enterAlwaysScrollBehavior()` |

**Изменённые файлы:**
- `commonui/src/main/java/su/sv/commonui/ui/components/AppToolbar.kt` — добавлен `scrollBehavior` в `AppToolbarWithThemeToggle` и `AppToolbarSimple`
- `news/src/main/java/su/sv/news/presentation/root/ui/RootNews.kt`
- `info/src/main/java/su/sv/info/rootinfo/ui/RootInfo.kt`

---

## 2. Читалка книги (fbreader/bookreader)

### 2.1 Показ подсказок "Навигация" и "Полный экран" при открытии ✅

**Постановка задачи:**
При открытии читалки книги должны на пару секунд показываться подсказки:
- "Навигация" — при нажатии на верхнюю область экрана
- "Полный экран" — при нажатии на нижнюю область

**Решение:**
- Добавлен флаг `hasShownControlsHint` в `ReaderState.Content`
- Добавлен action `MarkControlsHintShown` и `SetFullscreen` в `ReaderActions`
- Вызов `showControls()` в `update` блоке AndroidView после проверки `view.width > 0`

**Изменённые файлы:**
- `bookreader/src/main/java/com/github/axet/bookreader/screens/viewmodel/ReaderState.kt`
- `bookreader/src/main/java/com/github/axet/bookreader/screens/viewmodel/ReaderActions.kt`
- `bookreader/src/main/java/com/github/axet/bookreader/screens/viewmodel/ReaderViewModel.kt`
- `bookreader/src/main/java/com/github/axet/bookreader/screens/ReaderContent.kt`

### 2.2 Полноэкранный режим и тулбар ✅

**Проблема:**
- SHOW_MENU action отправлял broadcast вместо переключения fullscreen
- Тулбар не скрывался при fullscreen режиме

**Решение:**
- `SHOW_MENU` action теперь напрямую переключает System UI visibility через `Window.getDecorView()`
- Добавлен метод `onFullscreenToggle(boolean)` в `FBReaderView.Listener`
- Listener обновляет ViewModel state через `SetFullscreen` action
- Тулбар скрывается когда `state.isFullscreen = true`

**Изменённые файлы:**
- `bookreader/src/main/java/com/github/axet/bookreader/widgets/FBReaderView.java`
- `bookreader/src/main/java/com/github/axet/bookreader/screens/ReaderContent.kt`
- `bookreader/src/main/java/com/github/axet/bookreader/screens/viewmodel/ReaderActions.kt`
- `bookreader/src/main/java/com/github/axet/bookreader/screens/viewmodel/ReaderViewModel.kt`

### 2.3 Выход из fullscreen при закрытии читалки ✅

**Решение:**
- Добавлен метод `exitFullscreen()` в FBReaderView
- В `DisposableEffect.onDispose` вызывается `fbReaderView?.exitFullscreen()`

### 2.4 Блок навигации - кнопки OK/Cancel ✅

**Проблема:**
Текст кнопок OK/Cancel не отображался (ZLResource не загружался).

**Решение:**
- Текст кнопок теперь берётся из Android ресурсов
- Добавлены строки `navigation_ok` и `navigation_cancel` в `strings.xml`
- Добавлен `textColor="@color/white"` в layout

**Изменённые файлы:**
- `fbreader/src/main/res/values/strings.xml`
- `fbreader/src/main/java/org/geometerplus/android/fbreader/NavigationPopup.java`
- `fbreader/src/main/res/layout/navigation_panel.xml`

### 2.5 Исправление крашей ✅

**Проблема:**
- `NullPointerException: Parameter specified as non-null is null: ExtensionManager`
- `NullPointerException: Attempt to read from field 'BookModel.TOCTree' on a null object reference`

**Решение:**
- `ExtensionManager` в `CursorManager.kt` теперь nullable (`ExtensionElementManager?`)
- Добавлен null check в `getCurrentTOCElement()` — `if (Model == null) return null`

**Изменённые файлы:**
- `fbreader/src/main/java/org/geometerplus/zlibrary/text/view/CursorManager.kt`
- `bookreader/src/main/java/com/github/axet/bookreader/widgets/FBReaderView.java`

---

## 3. Оглавление книги (TOC) ✅

### 3.1 Иконки для глав оглавления
- Добавлена иконка `Icons.Default.List` для каждого элемента оглавления

### 3.2 Вложенная иерархия оглавления
- Создана модель `ExpandableTocItem` с `hasChildren` и `id`
- Добавлены иконки expand/collapse (`KeyboardArrowDown`/`KeyboardArrowUp`)
- Реализована логика раскрытия/закрытия через `mutableStateListOf`
- Добавлены отступы для разных уровней вложенности

**Изменённые файлы:**
- `bookreader/src/main/java/com/github/axet/bookreader/screens/ReaderContent.kt`

---

## 4. Wiki: Отложенный поиск текста ✅

### Постановка задачи
Поиск должен происходить после остановки ввода пользователя, только если под полем поиска не отображаются подсказки.

### Решение:
- Добавлен параметр `isSuggestionsVisible: Boolean` в `WikiSearchBar`
- Debounce поиск запускается только если `!isSuggestionsVisible`
- Передаётся `suggestions.isNotEmpty()` из RootWiki

**Изменённые файлы:**
- `wiki/src/main/java/su/sv/wiki/presentation/root/ui/WikiSearchBar.kt`
- `wiki/src/main/java/su/sv/wiki/root/RootWiki.kt`

---

## 5. Учёт тёмной/светлой темы

При реализации учитывалось, что:
- На всех экранах уже реализована смена темы (светлая/тёмная)
- В читалке fullscreen режим работает через System UI visibility

---

## Сводка всех изменённых файлов

| Модуль | Файл | Изменение |
|--------|------|-----------|
| **commonui** | `AppToolbar.kt` | scrollBehavior в AppToolbarWithThemeToggle и AppToolbarSimple |
| **news** | `RootNews.kt` | scrollBehavior + nestedScroll |
| **info** | `RootInfo.kt` | scrollBehavior + nestedScroll |
| **bookreader** | `ReaderState.kt` | hasShownControlsHint |
| **bookreader** | `ReaderActions.kt` | MarkControlsHintShown, SetFullscreen |
| **bookreader** | `ReaderViewModel.kt` | markControlsHintShown(), setFullscreen() |
| **bookreader** | `ReaderContent.kt` | showControls в update, onFullscreenToggle listener, exitFullscreen |
| **bookreader** | `FBReaderView.java` | SHOW_MENU action, exitFullscreen(), getCurrentTOCElement null check, Listener.onFullscreenToggle |
| **fbreader** | `CursorManager.kt` | ExtensionManager nullable |
| **fbreader** | `NavigationPopup.java` | Текст кнопок из ресурсов |
| **fbreader** | `strings.xml` | navigation_ok, navigation_cancel |
| **fbreader** | `navigation_panel.xml` | textColor white |
| **wiki** | `WikiSearchBar.kt` | isSuggestionsVisible параметр |
| **wiki** | `RootWiki.kt` | Передача suggestions.isNotEmpty() |

---

## Связанные документы
- [[fbreader-refactoring-plan]] — план рефакторинга модуля fbreader
- [[design-system]] — дизайн-система SV APP