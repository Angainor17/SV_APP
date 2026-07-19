# UI Testing Guide

## Структура тестов

```
app/src/androidTest/java/su/sv/app/
├── testing/
│   ├── TestAnnotations.kt    # Аннотации для категорий тестов
│   ├── TestTags.kt           # TestTags для UI элементов
│   └── BaseUiTest.kt         # Базовый класс для тестов
├── news/
│   └── NewsScreenTest.kt     # Тесты для экрана новостей
├── books/
│   └── BooksCatalogTest.kt   # Тесты для каталога книг
├── wiki/
│   └── WikiScreenTest.kt     # Тесты для Wiki
├── info/
│   └── InfoScreenTest.kt     # Тесты для Info
├── navigation/
│   └── NavigationTest.kt     # Тесты навигации
└── bookreader/
    └── ReaderScreenTest.kt   # Тесты для читалки
```

## Категории тестов

### @SmokeTest
Критические сценарии, которые должны проходить всегда.
Проверяют базовую функциональность приложения.

### @ReleaseTest
Тесты, которые должны запускаться перед релизом.
Проверяют все основные сценарии использования.

### @NavigationTest
Тесты навигации между экранами.

## Запуск тестов

### Через скрипт (рекомендуется)

```bash
# Все тесты
./scripts/run_ui_tests.sh

# Только smoke-тесты (быстро)
./scripts/run_ui_tests.sh smoke

# Только release-тесты
./scripts/run_ui_tests.sh release

# Только тесты навигации
./scripts/run_ui_tests.sh navigation
```

### Через Gradle

```bash
# Все тесты
./gradlew connectedAndroidTest

# Только smoke-тесты
./gradlew connectedAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.annotation=su.sv.app.testing.SmokeTest

# Только release-тесты
./gradlew connectedAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.annotation=su.sv.app.testing.ReleaseTest
```

### Через Android Studio

1. Откройте класс теста
2. Кликните на зелёную стрелку рядом с классом/методом
3. Выберите "Run"

## Релизный пайплайн

```bash
# Полный цикл перед релизом
./scripts/release_pipeline.sh
```

Скрипт выполняет:
1. Проверку кода (ktlint)
2. Сборку release APK
3. Запуск UI тестов (опционально)
4. Вывод информации о сборке

## Добавление TestTags в UI

Для работы тестов нужно добавить `testTag` в Compose компоненты:

```kotlin
@Composable
fun NewsList(
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.testTag(TestTags.News.LIST)
    ) {
        items(news) { item ->
            NewsItem(
                item = item,
                modifier = Modifier.testTag(TestTags.News.ITEM)
            )
        }
    }
}
```

## Написание нового теста

1. Создайте файл в `app/src/androidTest/java/su/sv/app/`
2. Наследуйте от `BaseUiTest` или используйте стандартный подход
3. Добавьте нужные аннотации (`@SmokeTest`, `@ReleaseTest`)
4. Используйте `TestTags` для поиска элементов

Пример:

```kotlin
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class MyScreenTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    @SmokeTest
    fun myScreen_displaysContent() {
        composeRule.waitForIdle()

        composeRule
            .onNodeWithTag(TestTags.MyScreen.ROOT)
            .assertIsDisplayed()
    }
}
```

## Отчёты тестов

После выполнения тестов отчёты доступны в:
- HTML: `app/build/reports/androidTests/connected/`
- XML: `app/build/test-results/`

## Best Practices

1. **Используйте TestTags** — не полагайтесь на текст или позиции
2. **Добавляйте waitUntil** — для асинхронных операций
3. **Группируйте тесты** — по экранам и функциональности
4. **Минимизируйте зависимости** — тесты должны быть независимы
5. **Используйте аннотации** — для категоризации тестов

## Troubleshooting

### Тесты падают с "No devices connected"

Убедитесь, что:
- Эмулятор запущен или устройство подключено
- `adb devices` показывает устройство

### Тесты не находят элементы

Проверьте:
- `testTag` добавлен в Compose компонент
- Тег соответствует `TestTags.kt`
- Используйте `useUnmergedTree = true` для LazyColumn

### Hilt injection fails

Убедитесь, что:
- Класс аннотирован `@HiltAndroidTest`
- `hiltRule` добавлен как первое правило
- Вызов `hiltRule.inject()` в `@Before`