# Managers Module

Общие менеджеры приложения.

## Обзор

Модуль `managers` содержит общие менеджеры, используемые в разных частях приложения.

---

## Тема (Theme)

### ThemeViewModel

ViewModel для управления темой приложения:

```kotlin
@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val themeRepository: ThemeRepository
) : ViewModel() {

    /** Конфигурация темы */
    val themeConfig: StateFlow<ThemeConfig>

    /** Установить режим темы */
    fun setThemeMode(mode: ThemeMode)

    /** Установить использование динамических цветов */
    fun setUseDynamicColors(use: Boolean)
}
```

### ThemeRepository

Репозиторий для хранения настроек темы в DataStore:

```kotlin
interface ThemeRepository {
    /** Текущий режим темы */
    val themeMode: Flow<ThemeMode>

    /** Использование динамических цветов (Material You) */
    val useDynamicColors: Flow<Boolean>

    /** Сохранить режим темы */
    suspend fun setThemeMode(mode: ThemeMode)

    /** Сохранить настройку динамических цветов */
    suspend fun setUseDynamicColors(use: Boolean)
}
```

### ThemeRepositoryImpl

Реализация репозитория с использованием DataStore:

```kotlin
class ThemeRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : ThemeRepository {
    // Реализация через DataStore
}
```

### ThemeModule

Hilt модуль для предоставления зависимостей:

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object ThemeModule {

    @Provides
    @Singleton
    fun provideThemeRepository(
        dataStore: DataStore<Preferences>
    ): ThemeRepository {
        return ThemeRepositoryImpl(dataStore)
    }
}
```

### ThemeConfig

Модель конфигурации темы:

```kotlin
data class ThemeConfig(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val useDynamicColors: Boolean = false
)

enum class ThemeMode {
    LIGHT,   // Светлая тема
    DARK,    // Тёмная тема
    SYSTEM   // Системная тема
}
```

---

## OnBookPagerManager

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

### tellAboutMisspell

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

### askQuestion

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

---

## Ссылки

- Telegram чат для опечаток: `https://t.me/SVremya/9244`
- Telegram чат для вопросов: `https://t.me/SVremya`

---

## Использование ThemeViewModel в экранах

Для применения темы на уровне Modo Screen:

```kotlin
@Composable
override fun Content(modifier: Modifier) {
    val themeViewModel: ThemeViewModel = hiltViewModel()
    val themeConfig by themeViewModel.themeConfig.collectAsStateWithLifecycle()

    SVAPPTheme(
        themeMode = themeConfig.themeMode,
        useDynamicColors = themeConfig.useDynamicColors
    ) {
        // Контент экрана
    }
}
```

---

## Структура файлов

```
managers/src/main/java/su/sv/managers/
├── OnBookPagerManager.kt
└── theme/
    ├── ThemeModule.kt
    ├── ThemeRepositoryImpl.kt
    └── ThemeViewModel.kt
```
