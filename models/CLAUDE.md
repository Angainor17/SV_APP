# Models Module

Модуль с общими моделями данных для UI.

## Обзор

Модуль `models` содержит базовые модели данных, используемые в UI слое приложения.

## Основные классы

### UiBook
Модель книги для отображения в списке:

```kotlin
@Parcelize
data class UiBook(
    val id: String,              // Идентификатор
    val title: String,           // Заголовок (например: "ОВЛ1")
    val author: String,          // Автор (например: "В.И. Ленин")
    val description: String,     // Описание (например: "8 статей")
    val image: String,           // URL обложки
    val downloadUrl: String,     // Ссылка для скачивания
    val fileNameWithExt: String, // Имя файла с расширением
    val category: String,        // Категория
    val fileUri: Uri?,           // URI скачанного файла
    val downloadState: UIBookState, // Статус скачивания
) : Parcelable
```

### UIBookState
Состояние скачивания книги:

```kotlin
enum class UIBookState {
    NOT_DOWNLOADED,    // Не скачано
    DOWNLOADED,        // Скачано
    DOWNLOADING        // В процессе скачивания
}
```

## Использование

Модели используются для передачи данных между модулями:

```kotlin
// Создание модели
val book = UiBook(
    id = "1",
    title = "ОВЛ1",
    author = "В.И. Ленин",
    description = "8 статей",
    image = "https://...",
    downloadUrl = "https://...",
    fileNameWithExt = "Lenin.pdf",
    category = "Литература",
    fileUri = null,
    downloadState = UIBookState.NOT_DOWNLOADED
)

// Передача через Bundle (Parcelable)
bundle.putParcelable("book", book)
```

## Структура файлов

```
models/src/main/java/su/sv/models/ui/book/
├── UiBook.kt           # Модель книги
└── UIBookState.kt      # Состояние скачивания
```
