---
name: books-screen-tasks-plan
description: План реализации задач на экране "Книги"
metadata:
  type: project
---

# План реализации задач на экране "Книги"

**Дата создания:** 2026-06-21
**Проект:** SV APP
**Модуль:** books

---

## Обзор задач

1. **Горизонтальный список chip-фильтров** — фильтрация книг по категориям и авторам
2. **Экран "Ваши книги"** — список скачанных книг с возможностью удаления
3. **Открытие скачанной книги** — клик на иконку скачанной книги открывает читалку

---

## Анализ данных API

**Запрос:** `https://svremya.org/appws/appws.php`

**Статистика:**
- Всего книг: 34

**Категории (4 уникальные):**
| Количество | Категория |
|------------|-----------|
| 29 | Свободное Время |
| 3 | ХудЛит |
| 1 | ОВЛ-С |
| 1 | СпецЛит |

**Топ авторов (16 уникальных):**
| Количество | Автор |
|------------|-------|
| 16 | Ленин В. И. |
| 11 | Попов М. В. |
| 7 | Удовиченко М. С. |
| 2 | Долгов В. Г. |
| 2 | Ельмеев В. Я. |

**Вывод:** Категорий всего 4, авторов 16. Вместе с chip "Все" — максимум 21 элемент. Ограничение в 20 элементов требует алгоритма выбора наиболее частых.

---

## Задача 1: Горизонтальный список chip-фильтров

### 1.1 Архитектура

**Новые файлы:**

```
books/src/main/java/su/sv/books/catalog/
├── domain/model/
│   └── BookFilter.kt                    # Модель фильтра (sealed class)
├── presentation/root/
│   ├── model/
│   │   └── UiBookFilter.kt              # UI модель фильтра
│   ├── mapper/
│   │   └── BookFiltersMapper.kt         # Маппер для создания списка фильтров
│   └── ui/
│       └── BookFiltersChips.kt          # Composable для отображения chips
```

### 1.2 Модели данных

```kotlin
// BookFilter.kt
sealed class BookFilter {
    object All : BookFilter()
    data class Category(val name: String) : BookFilter()
    data class Author(val name: String) : BookFilter()
}

// UiBookFilter.kt
data class UiBookFilter(
    val filter: BookFilter,
    val displayName: String,
    val count: Int,           // Количество книг с этим фильтром
    val isSelected: Boolean,
    val isAvailable: Boolean, // Доступен ли при текущем выборе
)
```

### 1.3 Алгоритм формирования списка фильтров

**Правила:**
1. Всегда первый chip — "Все"
2. Формируем карту частот: категории + авторы (разбивка по запятой)
3. Сортируем по убыванию частоты
4. Берём топ-19 (вместе с "Все" = 20)
5. Приоритет: сначала категории, потом авторы (категории более значимы)

```kotlin
// BookFiltersMapper.kt
class BookFiltersMapper {

    fun createFilters(books: List<Book>): List<UiBookFilter> {
        val categoryFreq = mutableMapOf<String, Int>()
        val authorFreq = mutableMapOf<String, Int>()

        books.forEach { book ->
            // Категория
            if (book.category.isNotBlank()) {
                categoryFreq[book.category] = categoryFreq.getOrDefault(book.category, 0) + 1
            }
            // Авторы (разбиваем по запятой)
            book.author.split(",").map { it.trim() }.filter { it.isNotBlank() }.forEach { author ->
                authorFreq[author] = authorFreq.getOrDefault(author, 0) + 1
            }
        }

        // Сортируем и ограничиваем
        val sortedCategories = categoryFreq.entries
            .sortedByDescending { it.value }
            .map { BookFilter.Category(it.key) to it.value }

        val sortedAuthors = authorFreq.entries
            .sortedByDescending { it.value }
            .map { BookFilter.Author(it.key) to it.value }

        // Комбинируем: сначала категории, потом авторы, лимит 19
        val allFilters = (sortedCategories + sortedAuthors).take(19)

        return listOf(UiBookFilter(BookFilter.All, "Все", books.size, true, true)) +
            allFilters.map { (filter, count) ->
                UiBookFilter(filter, getDisplayName(filter), count, false, true)
            }
    }
}
```

### 1.4 Логика фильтрации

**Правила:**
1. При выборе chip "Все" — сброс всех фильтров, показ всех книг
2. При выборе любого другого chip — фильтрация на месте (без сети)
3. Выбранный chip перемещается в начало списка (кроме "Все")
4. После выбора показывать только доступные chip (пересечение)
5. Множественный выбор: можно выбрать несколько фильтров одновременно
6. У выбранных chip (кроме "Все") отображается крестик для удаления

**Алгоритм пересечения:**
```kotlin
fun getAvailableFilters(
    allFilters: List<UiBookFilter>,
    selectedFilters: Set<BookFilter>,
    books: List<Book>
): List<UiBookFilter> {
    if (selectedFilters.isEmpty() || selectedFilters.contains(BookFilter.All)) {
        return allFilters // Все доступны
    }

    // Фильтруем книги по выбранным фильтрам
    val filteredBooks = books.filter { book ->
        selectedFilters.all { filter ->
            when (filter) {
                is BookFilter.All -> true
                is BookFilter.Category -> book.category == filter.name
                is BookFilter.Author -> book.author.contains(filter.name)
            }
        }
    }

    // Оставляем только те фильтры, которые применимы к отфильтрованным книгам
    return allFilters.map { filter ->
        val isAvailable = when (filter.filter) {
            is BookFilter.All -> true
            is BookFilter.Category -> filteredBooks.any { it.category == filter.filter.name }
            is BookFilter.Author -> filteredBooks.any { it.author.contains(filter.filter.name) }
        }
        filter.copy(isAvailable = isAvailable)
    }
}
```

### 1.5 UI компоненты

**BookFiltersChips.kt:**
- Использовать `LazyRow` для горизонтального списка
- Material3 `FilterChip` или `InputChip` (для крестика)
- Анимация появления/исчезновения при скролле (связано с TopAppBar)

**Интеграция с TopAppBar:**
- Использовать `TopAppBarDefaults.enterAlwaysScrollBehavior()`
- Скрывать chips вместе с TopAppBar при скролле вниз
- Показывать при свайпе вниз (PullToRefresh) или скролле вверх

### 1.6 Изменения в существующих файлах

| Файл | Изменение |
|------|-----------|
| `UiRootBooksState.kt` | Добавить поля: `allBooks`, `filteredBooks`, `filters`, `selectedFilters` |
| `RootBooksCatalogViewModel.kt` | Добавить логику фильтрации, действия для выбора фильтров |
| `RootBookActions.kt` | Добавить `OnFilterSelect(filter: BookFilter)`, `OnFilterRemove(filter: BookFilter)` |
| `BookList.kt` | Добавить `BookFiltersChips` над списком, интеграция со скроллом |
| `BooksListOneTimeEffect.kt` | Не требуется изменений |

---

## Задача 2: Экран "Ваши книги"

### 2.1 Архитектура

**Новые файлы:**

```
books/src/main/java/su/sv/books/catalog/
├── domain/
│   └── GetDownloadedBooksUseCase.kt     # UseCase для получения скачанных книг
├── presentation/
│   └── downloaded/
│       ├── ui/
│       │   ├── DownloadedBooksScreen.kt # Composable экрана
│       │   ├── DownloadedBookItem.kt    # Карточка книги
│       │   └── DownloadedBooksList.kt   # Список с удалением
│       ├── viewmodel/
│       │   └── DownloadedBooksViewModel.kt
│       ├── model/
│       │   └── UiDownloadedBookState.kt
│       ├── actions/
│       │   └── DownloadedBookActions.kt
│       └── effects/
│           └── DownloadedBookEffect.kt
```

### 2.2 Модели данных

```kotlin
// UiDownloadedBookState.kt
data class UiDownloadedBook(
    val id: String,
    val title: String,
    val author: String,
    val category: String,
    val coverUri: Uri?,
    val fileUri: Uri,
    val currentPage: Int,      // Текущая страница
    val totalPages: Int,       // Всего страниц
)

sealed class UiDownloadedBooksState {
    object Loading : UiDownloadedBooksState()
    data class Content(val books: List<UiDownloadedBook>) : UiDownloadedBooksState()
    object Empty : UiDownloadedBooksState()
}
```

### 2.3 UseCase для получения скачанных книг

```kotlin
// GetDownloadedBooksUseCase.kt
class GetDownloadedBooksUseCase @Inject constructor(
    private val getBooksListUseCase: GetBooksListUseCase,
    private val getBookUriUseCase: GetBookUriUseCase,
) {
    suspend fun execute(): Result<List<UiDownloadedBook>> {
        return getBooksListUseCase.execute().map { books ->
            books.mapNotNull { book ->
                val uri = getBookUriUseCase.execute(book.fileNameWithExt)
                if (uri != null) {
                    UiDownloadedBook(
                        id = book.id,
                        title = book.title,
                        author = book.author,
                        category = book.category,
                        coverUri = Uri.parse(book.image),
                        fileUri = uri,
                        currentPage = 0, // TODO: из хранилища прогресса
                        totalPages = 0,  // TODO: определить из файла
                    )
                } else null
            }
        }
    }
}
```

### 2.4 UI компоненты

**DownloadedBooksScreen.kt:**
- TopAppBar с кнопкой "Назад" (стрелочка)
- Список `LazyColumn` с карточками

**DownloadedBookItem.kt:**
- Card с обложкой слева
- Текстовые поля справа: название, автор, категория, страница
- Кнопка "Читать"
- Свайп влево для удаления

**Свайп для удаления:**
```kotlin
@Composable
fun DownloadedBookItem(
    book: UiDownloadedBook,
    onReadClick: () -> Unit,
    onDeleteClick: () -> Unit,
    showSwipeHint: Boolean, // Для анимации подсказки
) {
    val dismissState = rememberDismissState(
        confirmValueChange = { value ->
            if (value == DismissValue.DismissedToStart) {
                onDeleteClick()
                true
            } else false
        }
    )

    SwipeToDismiss(
        state = dismissState,
        background = { /* Красный фон с иконкой удаления */ },
        dismissContent = { /* Карточка книги */ }
    )
}
```

### 2.5 AlertDialog удаления

```kotlin
AlertDialog(
    onDismissRequest = { /* Нет */ },
    title = { Text("Подтверждение") },
    text = { Text("Вы точно хотите удалить \"${book.title}\"?") },
    confirmButton = {
        TextButton(onClick = { /* Удалить */ }) {
            Text("Да")
        }
    },
    dismissButton = {
        TextButton(onClick = { /* Закрыть */ }) {
            Text("Нет")
        }
    }
)
```

### 2.6 Удаление книги

**Требуется добавить в BookDownloadRepository:**
```kotlin
fun deleteBook(uri: Uri): Boolean {
    return try {
        context.contentResolver.delete(uri, null, null)
        true
    } catch (e: Exception) {
        Timber.e(e, "Error deleting book")
        false
    }
}
```

### 2.7 Анимация подсказки свайпа

**Логика:**
1. При первом открытии экрана — показать анимацию свайпа на первой карточке
2. После показа AlertDialog — сохранить флаг в SharedPreferences
3. Если флаг уже установлен — не показывать анимацию

**SharedPreferences ключ:** `"downloaded_books_swipe_hint_shown"`

```kotlin
// В ViewModel
private fun checkAndShowSwipeHint() {
    val wasShown = sharedPreferences.getBoolean(KEY_SWIPE_HINT_SHOWN, false)
    if (!wasShown && books.isNotEmpty()) {
        _showSwipeHint.value = true
    }
}

fun onSwipeHintShown() {
    sharedPreferences.edit().putBoolean(KEY_SWIPE_HINT_SHOWN, true).apply()
    _showSwipeHint.value = false
}
```

### 2.8 Условие отображения кнопки в тулбаре

```kotlin
// В RootBooksCatalogViewModel
val hasDownloadedBooks: StateFlow<Boolean> get() = _hasDownloadedBooks

// Обновлять при загрузке книг
private fun updateHasDownloadedBooks(books: List<UiBook>) {
    _hasDownloadedBooks.value = books.any { it.downloadState == UIBookState.DOWNLOADED }
}
```

### 2.9 Навигация

```kotlin
// Обновить BooksListOneTimeEffect.kt
sealed class BooksListOneTimeEffect {
    // ...
    object OpenDownloadedBooks : BooksListOneTimeEffect()
}

// В RootBooksCatalog.kt
is BooksListOneTimeEffect.OpenDownloadedBooks -> {
    stackNavigation.forward(DownloadedBooksScreen())
}
```

### 2.10 Удаление старого экрана

**Файлы для удаления (если существуют):**
- `bookreader/src/main/java/com/github/axet/bookreader/fragments/LibraryFragment.kt`
- Связанные XML layouts

---

## Задача 3: Открытие скачанной книги при клике на иконку

### 3.1 Изменения

**BookItem.kt:**
```kotlin
@Composable
private fun BoxScope.BookDownloadStatus(item: UiBook, actions: RootBooksActions) {
    Button(
        onClick = {
            when (item.downloadState) {
                UIBookState.DOWNLOADED -> {
                    actions.onAction(RootBookActions.OnOpenDownloadedBook(item))
                }
                UIBookState.AVAILABLE_TO_DOWNLOAD -> {
                    actions.onAction(RootBookActions.OnDownloadBookClick(item))
                }
                UIBookState.DOWNLOADING -> { /* Ничего не делать */ }
            }
        },
        // ...
    ) {
        // ...
    }
}
```

### 3.2 Новое действие

**RootBookActions.kt:**
```kotlin
sealed class RootBookActions {
    // ...
    data class OnOpenDownloadedBook(val book: UiBook) : RootBookActions()
}
```

### 3.3 Обработка в ViewModel

**RootBooksCatalogViewModel.kt:**
```kotlin
override fun onAction(action: RootBookActions) {
    when (action) {
        // ...
        is RootBookActions.OnOpenDownloadedBook -> {
            if (action.book.fileUri != null) {
                _oneTimeEffect.trySend(BooksListOneTimeEffect.OpenBook(action.book))
            }
        }
    }
}
```

### 3.4 Эффект открытия книги

**BooksListOneTimeEffect.kt** уже содержит `OpenBook(book: UiBook)`, используем его.

---

## Порядок реализации

### Этап 1: Задача 3 (простая)
1. Добавить действие `OnOpenDownloadedBook`
2. Изменить обработку клика в `BookItem.kt`
3. Добавить обработку в ViewModel
4. Протестировать открытие читалки

### Этап 2: Задача 2 (экран "Ваши книги")
1. Создать модели данных
2. Реализовать `GetDownloadedBooksUseCase`
3. Создать ViewModel
4. Создать UI компоненты (карточка, список, экран)
5. Добавить удаление книги в репозиторий
6. Реализовать AlertDialog
7. Добавить анимацию подсказки свайпа
8. Интегрировать навигацию
9. Удалить старый экран библиотеки

### Этап 3: Задача 1 (фильтры)
1. Создать модели фильтров
2. Реализовать `BookFiltersMapper`
3. Добавить фильтры в состояние экрана
4. Создать UI компонент chips
5. Интегрировать в `BookList.kt`
6. Добавить логику фильтрации в ViewModel
7. Реализовать скрытие при скролле
8. Протестировать все сценарии

---

## Зависимости

### Новые зависимости не требуются
- Material3 Chip уже доступен
- SwipeToDismiss есть в Compose Material

---

## Риски и вопросы

1. **Прогресс чтения:** Где хранится текущая страница? Требуется исследование хранилища FBReader.
2. **Удаление книги:** Удаление через ContentResolver может не работать для файлов в Downloads. Может потребоваться другой подход.
3. **Производительность:** Формирование списка фильтров при каждом обновлении списка книг. Можно кэшировать.

---

## Связанные документы

- [[bookreader-migration-complete]]
- [[bookreader-post-migration-fixes]]
