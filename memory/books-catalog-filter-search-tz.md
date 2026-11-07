---
name: books-catalog-filter-search-tz
description: ТЗ — фильтр «Скачанное» и поиск в тулбаре каталога книг
metadata:
  node_type: memory
  type: project
---

# ТЗ: Фильтр «Скачанное» и поиск в тулбаре каталога книг

**Дата:** 2026-11-07
**Статус:** ✅ Implemented

---

## 1. Обзор

Две доработки экрана каталога книг (модуль `books`):

1. **Фильтр «Скачанное»** — новый пункт фильтра после «Все»; показывает тот же список, что и «Все», но скачанные книги первыми.
2. **Поиск в тулбаре** — поле поиска перенесено из-под тулбара в сам тулбар по Material-паттерну «search in app bar».

---

## 2. Фильтр «Скачанное»

### 2.1 Требования

- Пункт располагается после фильтра «Все».
- Отображает тот же список, что и «Все», но скачанные книги — первыми.
- Сортировка скачанных: от самой новой скачанной до самой старой.
- Пункт имеет иконку скачивания (`Icons.Filled.Download`).

### 2.2 Сортировка

Стабильная сортировка через `compareByDescending` (в `UiRootBooksState.create()`):

```kotlin
val categoryFilteredBooks = when {
    selectedFilters.contains(BookFilter.Downloaded) -> {
        books.sortedWith(
            compareByDescending<UiBook> { it.fileUri != null }
                .thenByDescending { it.downloadedAt ?: Long.MIN_VALUE }
        )
    }
    // ...
}
```

### 2.3 Источник метки времени скачивания

- **При загрузке списка:** `DownloadManager.COLUMN_LAST_MODIFIED_TIMESTAMP` (API 24+ / `VERSION_CODES.N`).
- **При скачивании в рантайме:** `System.currentTimeMillis()` (в `BaseBookViewModel.handleDownloadedBook`).

### 2.4 Изменения модели

| Слой   | Изменение                                                    |
|--------|--------------------------------------------------------------|
| domain | `Book.downloadedAt: Long?`                                   |
| domain | `BookFilter.Downloaded` (sealed, между `All` и `Category`)   |
| data   | `DownloadInfo(uri, lastModified)` + `getDownloadInfo()`      |
| ui     | `UiBook.downloadedAt: Long?` (`@Parcelize`)                  |

---

## 3. Поиск в тулбаре

### 3.1 Требования (Material «search in app bar»)

- Поле поиска скрыто по умолчанию; открывается по иконке поиска в тулбаре.
- Режим поиска — отдельный `TopAppBar` (`BooksCatalogSearchTopBar`):
  - стрелка «назад» (`ArrowBack`) — закрывает поиск и сбрасывает запрос;
  - `TextField` в роли `title` (прозрачный контейнер и индикатор);
  - крестик `Close` для очистки — показывается только при непустом запросе;
  - автофокус при открытии (`FocusRequester` + `LaunchedEffect`).
- Иконка поиска (лупа) внутри поля **отсутствует** — начало ввода ближе к стрелке «назад».
- Placeholder — одна строка, обрезается многоточием (`maxLines = 1`, `TextOverflow.Ellipsis`).
- Шрифт ввода и placeholder — `MaterialTheme.typography.bodyMedium`.
- Подсветка нижней линии при вводе: `focusedIndicatorColor = colorScheme.primary`.
- Фильтры (chips) скрыты в режиме поиска.

### 3.2 Поведение

| Событие                     | Действие                                      |
|-----------------------------|-----------------------------------------------|
| Тап по иконке поиска        | Открыть режим поиска (`isSearchVisible = true`) |
| Тап по стрелке «назад»      | Сбросить запрос, закрыть поиск                 |
| Тап по крестику `Close`     | Очистить запрос (остаться в режиме поиска)     |
| Ввод текста                 | Подсветить нижнюю линию (`primary`)            |

---

## 4. Затронутые файлы

### 4.1 Модуль `books`

- `data/repo/BookDownloadRepository.kt` — `DownloadInfo`, чтение `COLUMN_LAST_MODIFIED_TIMESTAMP`.
- `domain/model/Book.kt` — поле `downloadedAt`.
- `domain/model/BookFilter.kt` — `object Downloaded`.
- `domain/GetBooksListUseCase.kt` — заполнение `fileUri` + `downloadedAt`.
- `domain/GetBookFiltersUseCase.kt` — фильтр `Downloaded` + счётчик скачанных.
- `presentation/base/BaseBookViewModel.kt` — метка времени при скачивании.
- `presentation/root/mapper/UiBookMapper.kt` — проброс `downloadedAt`.
- `presentation/root/mapper/UiBookFilterMapper.kt` — доступность фильтра.
- `presentation/root/model/UiRootBooksState.kt` — `when`-сортировка.
- `presentation/root/viewmodel/RootBooksCatalogViewModel.kt` — выбор/доступность фильтров.
- `presentation/root/ui/BookFiltersChips.kt` — чип «Скачанное» с иконкой.
- `presentation/root/ui/BookList.kt` — скрытие фильтров в режиме поиска.
- `presentation/root/ui/RootBooksCatalog.kt` — `BooksCatalogSearchTopBar`, ветвление тулбара.
- `testing/BooksTestTags.kt` — теги `SEARCH_BUTTON`, `SEARCH_CLEAR_BUTTON`, `SEARCH_CLOSE_BUTTON`.
- `src/main/res/values/strings.xml` — строки фильтра и поиска.

### 4.2 Модуль `models`

- `ui/book/UiBook.kt` — поле `downloadedAt`.

---

## 5. Критерии приёмки

- [x] Фильтр «Скачанное» идёт после «Все».
- [x] Скачанные книги идут первыми, отсортированы от новых к старым.
- [x] Чип «Скачанное» имеет иконку скачивания.
- [x] Поле поиска скрыто по умолчанию, открывается по иконке поиска.
- [x] Поиск находится в тулбаре, стрелка «назад» закрывает и сбрасывает запрос.
- [x] Крестик очищает поле (только при непустом запросе).
- [x] Placeholder в одну строку с многоточием, шрифт уменьшен.
- [x] Нижняя линия подсвечивается при вводе.
- [x] Фильтры скрыты в режиме поиска.

---

## 6. Связанные документы

- [[books-catalog-filter-search-tz]] — настоящий документ.
- `books/CLAUDE.md` — обзор модуля книг.
