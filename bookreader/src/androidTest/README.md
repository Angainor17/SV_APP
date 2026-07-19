# BookReader UI Tests

## Структура тестов

```
bookreader/src/androidTest/
├── assets/
│   └── test_book.epub            # Тестовая книга для интеграционных тестов
├── README.md
└── java/com/github/axet/bookreader/screens/
    ├── testing/
    │   ├── ReaderTestTags.kt         # TestTags для всех элементов
    │   ├── MockReaderViewModel.kt    # Мок ViewModel для изолированного тестирования
    │   └── BaseReaderTest.kt         # Базовый класс для тестов
    ├── integration/
    │   └── ReaderIntegrationTest.kt  # Интеграционные тесты с реальной книгой
    ├── ReaderTopBarTest.kt           # Тесты тулбара (24 теста)
    ├── SelectionPanelTest.kt         # Тесты панели выделения (14 тестов)
    ├── BookmarksDialogTest.kt        # Тесты закладок (6 тестов)
    ├── TocDialogTest.kt              # Тесты оглавления (6 тестов)
    ├── FontSettingsTest.kt           # Тесты настроек шрифта (10 тестов)
    └── NavigationDialogTest.kt       # Тесты навигации по страницам (8 тестов)
```

## Подготовка тестовой книги

Тесты автоматически ищут книгу:
1. **Assets**: `bookreader/src/androidTest/assets/test_book.epub`
2. **Downloads**: первая книга из `/storage/emulated/0/Download/`

```bash
# Подготовить книгу из файла
./scripts/prepare_test_book.sh /path/to/book.epub

# Или просто скачайте любую книгу в формате EPUB/FB2/PDF
# и она автоматически будет использована в тестах
```

**Если книга не найдена** — интеграционные тесты будут пропущены (Assume.assumeTrue).

**Поддерживаемые форматы:**
- EPUB (.epub)
- FB2 (.fb2)
- PDF (.pdf)
- MOBI (.mobi)
- RTF (.rtf)

## Изолированное тестирование

ReaderScreen можно тестировать изолированно без загрузки всего приложения:

```kotlin
@Test
fun myTest() {
    val mockViewModel = MockReaderViewModel()

    composeRule.setContent {
        ReaderContent(
            bookUri = Uri.parse("content://test/book.epub"),
            onNavigateBack = { mockViewModel.onAction(ReaderActions.NavigateBack) },
            onNavigateToSettings = {}
        )
    }

    // Тесты...
}
```

## Покрытие тестами

### TopBar (24 теста)
- ✅ Отображение всех кнопок
- ✅ Кнопка "Шрифт" скрыта для PDF без reflow
- ✅ Режим поиска
- ✅ Навигация по результатам поиска
- ✅ Fullscreen режим

### Selection Panel (14 тестов)
- ✅ Отображение кнопок
- ✅ Создание закладки
- ✅ Копирование текста
- ✅ Поделиться
- ✅ Вопрос AI
- ✅ Сообщить об опечатке

### Bookmarks (6 тестов)
- ✅ Отображение списка
- ✅ Переход к закладке
- ✅ Удаление закладки
- ✅ Пустой список

## Запуск тестов

```bash
# Через Gradle
./gradlew :bookreader:connectedAndroidTest

# Через Android Studio
# ПКМ на тест-класс → Run
```

## Добавление TestTags в UI

Для работы тестов добавьте `testTag` в Compose компоненты:

```kotlin
// ReaderTopBar.kt
TopAppBar(
    modifier = Modifier.testTag(ReaderTestTags.TopBar.ROOT)
) {
    IconButton(
        modifier = Modifier.testTag(ReaderTestTags.TopBar.BACK_BUTTON),
        onClick = onNavigateBack
    ) { ... }
}

// SelectionComposePanel.kt
Row(
    modifier = Modifier.testTag(ReaderTestTags.Selection.PANEL)
) {
    IconButton(
        modifier = Modifier.testTag(ReaderTestTags.Selection.BOOKMARK_BUTTON),
        onClick = onBookmark
    ) { ... }
}
```

## Тестовые данные

MockReaderViewModel позволяет:
- Устанавливать начальное состояние
- Симулировать результаты поиска
- Логировать действия для проверок
- Проверять флаги вызовов

```kotlin
val mockViewModel = MockReaderViewModel()

// Установка состояния
mockViewModel.setState(ReaderState.Content(
    bookTitle = "Тестовая книга",
    chapterTitle = "Глава 1",
    currentPage = 50,
    totalPages = 100
))

// Симуляция поиска
mockViewModel.simulateSearchResults(count = 5)

// Проверка действий
assert(mockViewModel.wasNavigatedBack)
assert(mockViewModel.actionsLog.contains(ReaderActions.SearchNext))
```

## Best Practices

1. **Используйте MockReaderViewModel** для изолированного тестирования
2. **Добавляйте TestTags** в все интерактивные элементы
3. **Группируйте тесты** по функциональным областям
4. **Проверяйте лог действий** вместо проверки UI состояния