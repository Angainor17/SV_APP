---
name: books-features-implementation
description: Документация по реализованным фичам модуля books (фильтры, скачанные книги, навигация)
metadata:
  type: reference
---

# Books Module — Реализованные фичи

**Дата:** 2026-06-21
**Модуль:** `books`

---

## Обзор

В рамках сессии были реализованы три основные фичи:
1. **Chip-фильтры** — горизонтальный список фильтров для списка книг
2. **Экран "Ваши книги"** — список скачанных книг с возможностью удаления
3. **Открытие скачанной книги** — клик на иконку скачивания открывает читалку

---

## 1. Chip-фильтры для списка книг

### 1.1 Архитектура

```
books/src/main/java/su/sv/books/catalog/
├── domain/
│   ├── model/
│   │   └── BookFilter.kt              # Sealed class типов фильтров
│   └── GetBookFiltersUseCase.kt       # Формирование списка фильтров
├── presentation/
│   └── root/
│       ├── model/
│       │   ├── UiBookFilter.kt        # UI модель фильтра
│       │   └── UiRootBooksState.kt    # Состояние с filteredBooks
│       ├── mapper/
│       │   └── UiBookFilterMapper.kt  # Маппер фильтров
│       ├── viewmodel/
│       │   └── RootBooksCatalogViewModel.kt
│       └── ui/
│           └── BookFiltersChips.kt    # Composable для chips
```

### 1.2 Типы фильтров

```kotlin
// BookFilter.kt
sealed class BookFilter {
    object All : BookFilter()                          // "Все книги"
    data class Category(val name: String) : BookFilter() // По категории
    data class Author(val name: String) : BookFilter()   // По автору
    data class Series(val name: String) : BookFilter()   // По серии (тома, книги)
}
```

### 1.3 Алгоритм формирования фильтров

**GetBookFiltersUseCase** формирует список в следующем порядке:
1. **"Все"** — всегда первый
2. **Категории** — сортировка по убыванию частоты
3. **Серии** — книги с паттернами "том N", "книга N" (только если > 1 книги)
4. **Авторы** — сортировка по убыванию частоты

**Паттерны серий:**
```kotlin
val SERIES_PATTERNS = listOf(
    Regex(",\\s*(том|тома|т\\.|книга|книги|кн\\.)\\s*[\\d\\-]+\\s*$"),
    Regex("\\s+(том|тома|т\\.|книга|книги|кн\\.)\\s*[\\d\\-]+\\s*$"),
)
```

**Примеры серий:**
- "Основное в ленинизме, том 1" → серия "Основное в ленинизме"
- "Капитал. Книга 1" → серия "Капитал"

### 1.4 Логика фильтрации

**Множественный выбор:**
- Можно выбрать несколько фильтров одновременно
- Фильтрация по пересечению (AND)
- "Все" сбрасывает все остальные фильтры

**Доступность фильтров:**
- При выборе фильтра показываются только доступные для отфильтрованных книг
- Недоступные фильтры скрываются

**UI поведение:**
- Выбранные фильтры отображаются с иконкой закрытия (`Icons.Default.Close`)
- При клике на выбранный фильтр — он отменяется
- Горизонтальный скролл сбрасывается в начало только при смене фильтра

### 1.5 Состояние

```kotlin
// UiRootBooksState.Content
data class Content(
    val books: List<UiBook>,           // Все книги
    val filteredBooks: List<UiBook>,   // Отфильтрованные книги
    val filters: List<UiBookFilter>,   // Доступные фильтры
    val selectedFilters: Set<BookFilter>, // Выбранные фильтры
    val filterScrollResetKey: Int,     // Ключ для сброса скролла chips
)
```

### 1.6 SwipeRefresh с сохранением фильтров

```kotlin
// RootBooksCatalogViewModel
private fun refreshList(preserveFilters: Boolean = false) {
    // При preserveFilters = true:
    // - Сохраняем selectedFilters
    // - Сохраняем filters (состояние доступности)
    // - Обновляем только books с бэкенда
}
```

---

## 2. Экран "Ваши книги"

### 2.1 Архитектура

```
books/src/main/java/su/sv/books/catalog/
├── domain/
│   ├── GetDownloadedBooksUseCase.kt   # Получение скачанных книг
│   └── DeleteBookUseCase.kt           # Удаление книги
├── presentation/
│   └── downloaded/
│       ├── ui/
│       │   ├── DownloadedBooksScreen.kt   # Modo Screen
│       │   ├── DownloadedBooksList.kt     # Список со свайпом
│       │   ├── DownloadedBookItem.kt      # Карточка книги
│       │   └── DeleteSwipeBackground.kt   # Фон для свайпа
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
// UiDownloadedBook
data class UiDownloadedBook(
    val id: String,
    val title: String,
    val author: String,
    val category: String,
    val coverUrl: String,
    val fileUri: Uri,
    val currentPage: Int,
    val totalPages: Int,
)

// UiDownloadedBooksState
sealed class UiDownloadedBooksState {
    object Loading : UiDownloadedBooksState()
    data class Content(
        val books: List<UiDownloadedBook>,
        val showSwipeHint: Boolean,  // Анимация подсказки
        val resetKey: Int,           // Сброс состояния свайпа
    ) : UiDownloadedBooksState()
    object Empty : UiDownloadedBooksState()
}
```

### 2.3 Свайп для удаления

**Реализация через SwipeToDismissBox:**

```kotlin
val dismissState = rememberSwipeToDismissBoxState(
    confirmValueChange = { dismissValue ->
        if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
            onDeleteRequest(book)
            true
        } else false
    },
    positionalThreshold = { totalDistance ->
        totalDistance * 0.3f  // Срабатывает при 30% ширины
    },
)
```

**Структура слоёв:**
1. **DeleteSwipeBackground** — бледно-красный фон с иконкой корзины
2. **DownloadedBookItem** — карточка с непрозрачным бледно-голубым фоном

### 2.4 Анимация подсказки свайпа

**Логика:**
- При первом открытии экрана — показать анимацию свайпа на первой карточке
- Флаг `KEY_SWIPE_HINT_SHOWN` хранится в SharedPreferences
- Анимация: показать фон → скрыть → повторить

### 2.5 AlertDialog подтверждения

```kotlin
AlertDialog(
    title = { Text("Подтверждение") },
    text = { Text("Вы точно хотите удалить \"${book.title}\"?") },
    confirmButton = { TextButton(onClick = { /* удалить */ }) { Text("Да") } },
    dismissButton = { TextButton(onClick = { /* отменить */ }) { Text("Нет") } },
)
```

**При отмене:** сбрасывается состояние свайпа через `resetKey`

### 2.6 Навигация при клике на книгу

При клике на книгу открывается **BookDetailScreen** (не ReaderScreen):

```kotlin
// Конвертация UiDownloadedBook → UiBook
val uiBook = UiBook(
    id = effect.book.id,
    title = effect.book.title,
    author = effect.book.author,
    // ... остальные поля
    downloadState = UIBookState.DOWNLOADED,
)
stackNavigation.forward(BookDetailScreen(uiBook = uiBook))
```

### 2.7 Условие отображения кнопки в тулбаре

Кнопка "Ваши книги" в тулбаре показывается только если есть скачанные книги:

```kotlin
// UiRootBooksState.Content
val hasDownloadedBooks: Boolean

// BookList.kt
if (hasDownloadedBooks) {
    IconButton(...) { Icon(Icons.Filled.Download, ...) }
}
```

---

## 3. Открытие скачанной книги при клике на иконку

### 3.1 Изменения в BookItem.kt

```kotlin
Button(onClick = {
    when (item.downloadState) {
        UIBookState.DOWNLOADED -> {
            actions.onAction(RootBookActions.OnOpenDownloadedBook(item))
        }
        UIBookState.AVAILABLE_TO_DOWNLOAD -> {
            actions.onAction(RootBookActions.OnDownloadBookClick(item))
        }
        UIBookState.DOWNLOADING -> { /* ничего */ }
    }
})
```

### 3.2 Эффект открытия

```kotlin
// BooksListOneTimeEffect
object OpenReader(val book: UiBook) : BooksListOneTimeEffect()

// RootBooksCatalog.kt
is BooksListOneTimeEffect.OpenReader -> {
    stackNavigation.forward(ReaderScreen(bookUri = effect.book.fileUri))
}
```

---

## 4. Bottom Navigation — исправление бага

### 4.1 Проблема

При возврате с BookDetailScreen на экран списка книг выделение в bottom navigation пропадало.

### 4.2 Решение

Использовать `currentBackStackEntryAsState()` для отслеживания текущего маршрута:

```kotlin
// BottomNavigationUi.kt
val navBackStackEntry by navController.currentBackStackEntryAsState()
val currentRoute = navBackStackEntry?.destination?.route

val navigationSelectedItem = remember(currentRoute) {
    when (currentRoute) {
        Screens.News.route -> 0
        Screens.Books.route -> 1
        Screens.Wiki.route -> 2
        Screens.Info.route -> 3
        else -> 0
    }
}
```

---

## 5. Зависимости

### Добавленные зависимости

```kotlin
// books/build.gradle.kts
implementation(libs.androidx.material.icons.extended)

// gradle/libs.versions.toml
androidx-material-icons-extended = { 
    group = "androidx.compose.material", 
    name = "material-icons-extended" 
}
```

---

## 6. Известные проблемы и TODO

### TODO
- [ ] Прогресс чтения (currentPage, totalPages) — сейчас всегда 0
- [ ] Определение totalPages из файла книги
- [ ] Хранение прогресса чтения

### Возможные улучшения
- [ ] Кэширование списка фильтров
- [ ] Pull-to-refresh на экране "Ваши книги"
- [ ] Поиск по скачанным книгам

---

## Связанные документы

- [[bookreader-migration-complete]]
- [[bookreader-post-migration-fixes]]
- [[books-screen-tasks-plan]]
