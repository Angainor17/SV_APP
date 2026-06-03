# Managers Module

Общие менеджеры приложения.

## Обзор

Модуль `managers` содержит общие менеджеры, используемые в разных частях приложения.

## Основные классы

### OnBookPagerManager
Менеджер для работы с книгами в приложении чтения:

```kotlin
class OnBookPagerManager @Inject constructor(
    private val resourcesRepository: ResourcesRepository
) {
    // Сообщить об опечатке
    fun tellAboutMisspell(context, text, title, author, page)

    // Задать вопрос
    fun askQuestion(context, text, title, author, page)
}
```

#### tellAboutMisspell
Копирует информацию об опечатке в буфер обмена и открывает Telegram-чат.

```kotlin
onBookPagerManager.tellAboutMisspell(
    context = context,
    text = "текст опечатки",
    title = "Название книги",
    author = "Автор",
    page = 42
)
```

#### askQuestion
Копирует вопрос в буфер обмена и открывает Telegram-чат.

```kotlin
onBookPagerManager.askQuestion(
    context = context,
    text = "текст вопроса",
    title = "Название книги",
    author = "Автор",
    page = 42
)
```

## Ссылки

- Telegram чат для опечаток: `https://t.me/SVremya/9244`
- Telegram чат для вопросов: `https://t.me/SVremya`

## Структура файлов

```
managers/src/main/java/su/sv/managers/
└── OnBookPagerManager.kt
```
