---
name: fbreader-activity-cleanup
description: Удаление legacy Activity из fbreader модуля
metadata: 
  node_type: memory
  type: project
  originSessionId: d4db43b1-b497-4882-9a95-230fffb17201
---

# Удаление Legacy Activity из fbreader

**Дата:** 2026-06-22

## Что было сделано

Удалено ~40 Activity файлов и ~20 layout файлов из модуля fbreader. Это был legacy код от оригинального FBReader, который не использовался в текущей архитектуре (Activity не были зарегистрированы в манифесте).

## Причина удаления

- Все Activity были заменены на Compose экраны в модуле bookreader
- Не зарегистрированы в AndroidManifest.xml
- Усложняли архитектуру и увеличивали размер кодовой базы

## Что нужно восстановить (приоритет)

### Приоритет 1 (для полноценной читалки)
1. TOCActivity → Compose BottomSheet с оглавлением
2. BookmarksActivity → Compose экран закладок
3. EditBookmarkActivity → Compose Dialog для закладки
4. PreferenceActivity → Compose ReaderSettingsScreen
5. ImageViewActivity → Compose просмотр изображений

### Приоритет 2
1. LibrarySearchActivity → Поиск в библиотеке
2. BookInfoActivity → Информация о книге
3. ProcessHyperlinkAction → Обработка сносок

## Оставшиеся компоненты

- PopupPanel, SelectionPopup, TextSearchPopup, NavigationPopup — используются в bookreader
- DictionaryUtil — интеграция со словарями
- BookCollectionShadow, LibraryService — сервис для книг
- ZLAndroidWidget, MainView — View для отображения

## Ссылки

- [[fbreader-refactoring-plan]] — план рефакторинга fbreader
- Документация: `/fbreader/CLAUDE.md` — содержит полный список удалённого функционала
