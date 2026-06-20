---
name: bookreader-post-migration-fixes
description: Исправление багов после миграции BookReader
metadata:
  type: project
---

# ТЗ: Исправление багов BookReader после миграции

**Дата:** 2026-06-20
**Статус:** ✅ ЗАВЕРШЁНО

---

## Выполненные исправления

### Этап 1: Удаление неиспользуемого кода ✅
- Удалён TTS из ReaderTopBar меню
- Удалён RTL из ReaderTopBar меню
- Удалены ToggleTts и ToggleRtl из ReaderActions
- Удалены ttsEnabled и rtlMode из ReaderState
- Удалены методы toggleTts() и toggleRtl() из ReaderViewModel

### Этап 2: Исправление диалогов ✅
- **TOC:** Переписан на Compose AlertDialog (устранён Resources$NotFoundException)
- **Bookmarks:** Добавлен null-check, переписан на Compose AlertDialog
- **Fonts:** Переписан на Compose ModalBottomSheet

### Этап 3: Исправление навигации и состояния ✅
- **NavigateBack:** Добавлен onNavigateBack callback в ReaderContent и ReaderScreen
- **AndroidView lifecycle:** Улучшено управление состоянием FBReaderView
- **API warning:** Исправлена проверка RECEIVER_NOT_EXPORTED (API 33+)

### Этап 4: Исправление разрешений файлов ✅

#### Проблема
На Android 10+ Scoped Storage ограничивает доступ к файлам:
- `file://` URI недоступны
- DownloadManager добавляет суффиксы (`-1`, `-2`) при дубликатах
- Title в DownloadManager может отличаться от имени файла

#### Решение: Улучшенный поиск файлов (`BookDownloadRepository.getDownloadsUri()`)

**Алгоритм поиска:**
1. **Нормализация имени файла:**
   - Убирает год и спецсимволы: `Краткая.история.Будущего.2024_А6.pdf` → `краткая история будущего`
   - Извлекает ключевые слова (первые 2-3 слова длиннее 2 символов)

2. **Поиск по ключевым словам:**
   - Проверяет совпадение в `title` загрузки
   - Учитывает суффиксы (`-1`, `-2`) в `localUri`
   - Подсчитывает `score` для ранжирования результатов

3. **Получение URI:**
   - `getUriForDownloadedFile(id)` → `content://downloads/all_downloads/{id}`
   - Fallback через MediaStore: `content://media/external/downloads/{id}`

**Примеры:**
```
Ищем: Краткая.история.Будущего.2024_А6.pdf
Title: "Краткая история будущего. Научный социализм в популярном изложении"
localUri: .../Краткая.история.Будущего.2024_А6-2.pdf

Ключевые слова: ["краткая", "история", "будущего"]
Score: 30 (3 слова * 10) → файл найден!
```

#### Изменённые файлы
- `books/src/main/java/su/sv/books/catalog/data/repo/BookDownloadRepository.kt`
- `bookreader/src/main/java/com/github/axet/bookreader/screens/viewmodel/ReaderViewModel.kt` (улучшена обработка ошибок)

### Баг #1: FileNotFoundException (Permission denied)
```
java.io.FileNotFoundException: /storage/emulated/0/Download/В.помощь.товарищу.2025.pdf: open failed: EACCES (Permission denied)
```
**Причина:** Scoped Storage на Android 11+ не даёт доступ к файлам без SAF
**Решение:** Использовать SAF или запросить MANAGE_EXTERNAL_STORAGE

### Баг #2: Кнопка "Назад" не работает
**Причина:** ReaderActions.NavigateBack не обрабатывается в ReaderScreen
**Решение:** Добавить обработку в ReaderScreen для вызова ` LocalStackNavigation.current.back()`

### Баг #3: TOC крэш
```
Resources$NotFoundException: Resource ID #0x0
```
**Причина:** AlertDialog.Builder без темы/resources
**Решение:** Переписать на Compose AlertDialog

### Баг #4: Bookmarks крэш
```
NullPointerException: bookmarks must not be null
```
**Причина:** `book.info.bookmarks` может быть null
**Решение:** Добавить null-check и пустой список по умолчанию

### Баг #5: Удалить TTS/RTL
**Причина:** Неиспользуемый функционал
**Решение:** Удалить из UI и ViewModel

### Баг #6: Поворот устройства
**Причина:** AndroidView пересоздаётся без сохранения состояния
**Решение:** Использовать правильный key для AndroidView, сохранить позицию

### Баг #7: После настроек книга не отображается
**Причина:** FBReaderView теряет состояние при навигации
**Решение:** ViewModel должен сохранять ссылку на FBReaderView

### Баг #8: Fonts popup
**Причина:** PopupWindow не корректно отображается в Compose
**Решение:** Переписать на Compose BottomSheet

---

## Связанные файлы

- `bookreader/src/main/java/com/github/axet/bookreader/screens/ReaderContent.kt`
- `bookreader/src/main/java/com/github/axet/bookreader/screens/ReaderScreen.kt`
- `bookreader/src/main/java/com/github/axet/bookreader/screens/ui/ReaderTopBar.kt`
- `bookreader/src/main/java/com/github/axet/bookreader/screens/viewmodel/ReaderViewModel.kt`
- `bookreader/src/main/java/com/github/axet/bookreader/screens/viewmodel/ReaderState.kt`
- `bookreader/src/main/java/com/github/axet/bookreader/screens/viewmodel/ReaderActions.kt`
