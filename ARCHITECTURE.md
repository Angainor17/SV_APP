# Архитектура экранов SV APP

## Обзор

Проект SV APP использует **Clean Architecture** с паттерном **MVI** для построения экранов.

### Ключевые принципы

1. **Разделение слоёв**: Presentation → Domain → Data
2. **MVI**: Actions → ViewModel → State/Effects
3. **DI**: Hilt для внедрения зависимостей
4. **Single Activity**: навигация через Modo
5. **Unit тесты**: обязательны для UseCase и ViewModel
6. **Detekt**: проверка качества кода перед коммитом

---

## Структура экрана

Каждый экран следует единой архитектуре:

```
feature/
├── data/                           # Data слой
│   ├── api/                        # API интерфейсы (Retrofit)
│   ├── local/                      # Локальное хранилище (Room, SharedPreferences)
│   │   ├── dao/                    # DAO интерфейсы
│   │   ├── entity/                 # Entity классы
│   │   └── database/               # База данных
│   ├── repository/                 # Реализация репозиториев
│   └── models/                     # Data модели (DTO)
├── domain/                         # Domain слой
│   ├── model/                      # Доменные модели
│   ├── repository/                 # Интерфейсы репозиториев
│   └── UseCase.kt                  # Use Cases
├── presentation/                   # Presentation слой
│   ├── root/                       # Главный экран фичи
│   │   ├── ui/                     # Compose компоненты
│   │   │   ├── RootScreen.kt       # Главный экран
│   │   │   ├── List.kt             # Список
│   │   │   └── Item.kt             # Элемент списка
│   │   ├── mapper/                 # Мапперы Domain → UI
│   │   ├── model/                  # UI модели (state)
│   │   └── viewmodel/
│   │       ├── ViewModel.kt        # ViewModel
│   │       ├── actions/            # MVI actions
│   │       │   ├── Actions.kt
│   │       │   └── ActionsHandler.kt
│   │       └── effects/            # One-time effects
│   │           └── OneTimeEffect.kt
│   └── detail/                     # Детальный экран (опционально)
├── di/                             # Dependency Injection
│   └── Module.kt                   # Hilt модуль
└── nav/
    └── Screen.kt                   # Modo Screen
```

---

## Слои архитектуры

### 1. Presentation Layer

#### ViewModel

```kotlin
@HiltViewModel
class RootFeatureViewModel @Inject constructor(
    private val getItemsUseCase: GetItemsUseCase,
    private val uiMapper: UiMapper,
) : BaseViewModel(), FeatureActionsHandler {

    /** Состояние экрана */
    private val _state = MutableStateFlow(UiRootState())
    val state: StateFlow<UiRootState> get() = _state

    /** Одноразовые события (навигация, snackbars) */
    private val _oneTimeEffect = Channel<OneTimeEffect>(capacity = Channel.BUFFERED)
    val oneTimeEffect: Flow<OneTimeEffect> get() = _oneTimeEffect.receiveAsFlow()

    init {
        loadItems()
    }

    override fun onAction(action: FeatureActions) {
        when (action) {
            is FeatureActions.OnItemClick -> {
                _oneTimeEffect.trySend(OneTimeEffect.OpenDetail(action.item))
            }
            FeatureActions.OnRetryClick -> {
                loadItems()
            }
        }
    }

    private fun updateState(action: (UiRootState) -> UiRootState) {
        _state.update { action(it) }
    }
}
```

#### Actions (MVI)

```kotlin
// Actions.kt
sealed class FeatureActions {
    object OnRetryClick : FeatureActions()
    data class OnItemClick(val item: UiItem) : FeatureActions()
    object OnSwipeRefresh : FeatureActions()
}

// ActionsHandler.kt
interface FeatureActionsHandler {
    fun onAction(action: FeatureActions)
}
```

#### One-time Effects

```kotlin
sealed class OneTimeEffect {
    data class OpenDetail(val item: UiItem) : OneTimeEffect()
    data class ShowErrorSnackBar(val text: String) : OneTimeEffect()
}
```

#### UI State

```kotlin
sealed class UiRootState {
    object Loading : UiRootState()
    data class Content(
        val items: List<UiItem>,
        val isRefreshing: Boolean = false,
    ) : UiRootState()
    data class Error(val message: String) : UiRootState()
    object Empty : UiRootState()
}
```

#### Compose Screen

```kotlin
@Composable
fun RootFeature(
    viewModel: RootFeatureViewModel = hiltViewModel()
) {
    val state = viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    HandleEffects(viewModel, snackbarHostState)

    when (val currentState = state.value) {
        is UiRootState.Loading -> FullScreenLoading()
        is UiRootState.Content -> ContentList(currentState, viewModel)
        is UiRootState.Error -> FullScreenError { viewModel.onAction(FeatureActions.OnRetryClick) }
        is UiRootState.Empty -> EmptyContent()
    }
}

@Composable
private fun HandleEffects(
    viewModel: RootFeatureViewModel,
    snackbarHostState: SnackbarHostState,
) {
    val scope = rememberCoroutineScope()
    val stackNavigation = LocalStackNavigation.current

    OneTimeEffect(viewModel.oneTimeEffect) { effect ->
        when (effect) {
            is OneTimeEffect.OpenDetail -> {
                stackNavigation.forward(DetailScreen(effect.item))
            }
            is OneTimeEffect.ShowErrorSnackBar -> {
                scope.launch {
                    snackbarHostState.showSnackbar(effect.text)
                }
            }
        }
    }
}
```

### 2. Domain Layer

#### UseCase

```kotlin
class GetItemsUseCase @Inject constructor(
    private val repository: ItemsRepository,
) {
    suspend fun execute(): Result<List<Item>> {
        return repository.getItems()
    }
}
```

#### Domain Model

```kotlin
data class Item(
    val id: String,
    val title: String,
    val description: String,
)
```

#### Repository Interface

```kotlin
interface ItemsRepository {
    suspend fun getItems(): Result<List<Item>>
}
```

### 3. Data Layer

#### Repository Implementation

```kotlin
class ItemsRepositoryImpl @Inject constructor(
    private val api: ItemsApi,
    private val dao: ItemDao,
) : ItemsRepository {

    override suspend fun getItems(): Result<List<Item>> = runCatching {
        val apiItems = api.getItems()
        dao.insertAll(apiItems.map { it.toEntity() })
        apiItems.map { it.toDomain() }
    }
}
```

#### API

```kotlin
interface ItemsApi {
    @GET("items")
    suspend fun getItems(): List<ApiItem>
}
```

#### Room Entity & DAO

```kotlin
@Entity(tableName = "items")
data class ItemEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
)

@Dao
interface ItemDao {
    @Query("SELECT * FROM items")
    suspend fun getAll(): List<ItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ItemEntity>)
}
```

---

## DI (Hilt)

### Module

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object FeatureModule {

    @Provides
    @Singleton
    fun provideItemsApi(retrofit: Retrofit): ItemsApi {
        return retrofit.create(ItemsApi::class.java)
    }

    @Provides
    @Singleton
    fun provideItemsRepository(
        api: ItemsApi,
        dao: ItemDao,
    ): ItemsRepository {
        return ItemsRepositoryImpl(api, dao)
    }
}
```

---

## Навигация (Modo)

### Screen

```kotlin
@Parcelize
class DetailScreen(
    private val itemId: String,
) : Screen, Parcelable {

    @Composable
    override fun Content(modifier: Modifier) {
        DetailFeature(itemId = itemId)
    }
}
```

### Навигация

```kotlin
// Переход вперёд
stackNavigation.forward(DetailScreen(itemId = "123"))

// Назад
stackNavigation.back()

// Замена текущего экрана
stackNavigation.replace(NewScreen())

// Очистка стека до корня
stackNavigation.resetRoot(RootScreen())
```

---

## Пагинация (Paging 3)

### PagingSource

```kotlin
class ItemsPagingSource(
    private val useCase: GetItemsUseCase,
    private val mapper: UiMapper,
) : PagingSource<Int, UiItem>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, UiItem> {
        return try {
            val offset = params.key ?: 0
            val result = useCase.execute(offset = offset)

            result.fold(
                onSuccess = { items ->
                    LoadResult.Page(
                        data = items.map { mapper.toUi(it) },
                        prevKey = if (offset == 0) null else offset - PAGE_SIZE,
                        nextKey = if (items.size < PAGE_SIZE) null else offset + PAGE_SIZE,
                    )
                },
                onFailure = { LoadResult.Error(it) }
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    companion object {
        const val PAGE_SIZE = 20
    }
}
```

### ViewModel с Paging

```kotlin
@HiltViewModel
class PagedViewModel @Inject constructor(
    private val getItemsUseCase: GetItemsUseCase,
    private val uiMapper: UiMapper,
) : BaseViewModel() {

    val pagingDataFlow: Flow<PagingData<UiItem>> = Pager(
        config = PagingConfig(pageSize = PAGE_SIZE)
    ) {
        ItemsPagingSource(getItemsUseCase, uiMapper)
    }.flow.cachedIn(viewModelScope)
}
```

---

## Тестирование

### Unit тесты UseCase

```kotlin
@RunWith(MockitoJUnitRunner::class)
class GetItemsUseCaseTest {

    @Mock
    private lateinit var repository: ItemsRepository

    private lateinit var useCase: GetItemsUseCase

    @Before
    fun setup() {
        useCase = GetItemsUseCase(repository)
    }

    @Test
    fun `execute returns items from repository`() = runTest {
        // Given
        val expectedItems = listOf(Item("1", "Title", "Description"))
        whenever(repository.getItems()).thenReturn(Result.success(expectedItems))

        // When
        val result = useCase.execute()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(expectedItems, result.getOrNull())
    }
}
```

### Unit тесты ViewModel

```kotlin
@RunWith(MockitoJUnitRunner::class)
class FeatureViewModelTest {

    @Mock
    private lateinit var getItemsUseCase: GetItemsUseCase

    @Mock
    private lateinit var uiMapper: UiMapper

    private lateinit var viewModel: FeatureViewModel

    @Before
    fun setup() {
        viewModel = FeatureViewModel(getItemsUseCase, uiMapper)
    }

    @Test
    fun `initial state is Loading`() = runTest {
        // Given/When
        val state = viewModel.state.first()

        // Then
        assertTrue(state is UiRootState.Loading)
    }

    @Test
    fun `onAction OnRetryClick loads items`() = runTest {
        // Given
        whenever(getItemsUseCase.execute()).thenReturn(Result.success(emptyList()))

        // When
        viewModel.onAction(FeatureActions.OnRetryClick)

        // Then
        verify(getItemsUseCase).execute()
    }
}
```

---

## Detekt

### Конфигурация

Файл `detekt.yml` в корне проекта:

```yaml
complexity:
  LongMethod:
    threshold: 60
  LongParameterList:
    threshold: 6
  CyclomaticComplexMethod:
    threshold: 15

style:
  MaxLineLength:
    maxLineLength: 120

formatting:
  ImportOrdering:
    active: true
```

### Запуск

```bash
# Проверка всего проекта
./gradlew detekt

# Проверка конкретного модуля
./gradlew :feature:detekt

# Автоматическое исправление
./gradlew detekt --auto-correct
```

---

## Логирование

### Правила логирования

**Все логи приложения должны использовать Timber с тегом "voronin"!**

```kotlin
// Правильно
Timber.tag("voronin").d("message: $data")
Timber.tag("voronin").e(exception, "error message")

// Неправильно
Timber.d("message")  // без тега
Log.d("tag", "message")  // устаревший Android Log
println("message")  // консольный вывод
```

### Когда использовать

- **Debug**: для отладки и анализа данных
- **Error**: для ошибок и исключений
- **Info**: для важных событий (запуск, завершение)
- **Warning**: для предупреждений

### Примеры

```kotlin
// В Repository - логирование запросов
Timber.tag("voronin").d("searchArticle: query='$query', results=$count")

// В UseCase - логирование действий
Timber.tag("voronin").d("GetItemsUseCase: loading items")

// В ViewModel - логирование state изменений
Timber.tag("voronin").d("onAction: $action")

// При ошибках
Timber.tag("voronin").e(e, "Failed to load data")
```

### Почему "voronin"

Тег "voronin" позволяет:
- Быстро фильтровать логи в Logcat
- Отделять логи приложения от системных логов
- Унифицировать логирование across все модули

---

## Чек-лист создания нового экрана

1. [ ] Создать структуру папок (`data/`, `domain/`, `presentation/`, `di/`)
2. [ ] Определить Domain модели
3. [ ] Создать Repository интерфейс
4. [ ] Реализовать API и/или Room
5. [ ] Создать UseCase
6. [ ] Создать UI State модели
7. [ ] Создать Actions и ActionsHandler
8. [ ] Создать One-time Effects
9. [ ] Реализовать ViewModel
10. [ ] Создать Mapper (Domain → UI)
11. [ ] Реализовать Compose UI
12. [ ] Создать Modo Screen
13. [ ] Настроить DI модуль
14. [ ] Добавить строковые ресурсы в `strings.xml`
15. [ ] Написать Unit тесты для UseCase
16. [ ] Написать Unit тесты для ViewModel
17. [ ] Запустить `./gradlew detekt` и исправить замечания
18. [ ] Добавить Preview для всех Composable функций
19. [ ] Декомпозировать UI компоненты по файлам

---

## Compose UI правила

### Обязательные Preview

**Каждая Composable функция должна иметь Preview!**

Preview позволяют:
- Быстро проверять внешний вид компонента
- Воспроизводить разные состояния компонента
- Упростить код-ревью
- Документировать expected behavior

```kotlin
// Composable функция
@Composable
fun NoteItem(
    note: UiBookmarkNote,
    onClick: () -> Unit,
) {
    // ... реализация
}

// Обязательный Preview
@Preview(showBackground = true)
@Composable
private fun NoteItemPreview() {
    SVAPPTheme {
        NoteItem(
            note = UiBookmarkNote(
                id = "1",
                text = "Пример текста заметки",
                // ... остальные поля
            ),
            onClick = {}
        )
    }
}

// Preview для разных состояний
@Preview(showBackground = true, name = "Long text")
@Composable
private fun NoteItemLongTextPreview() {
    SVAPPTheme {
        NoteItem(
            note = UiBookmarkNote(
                id = "1",
                text = "Очень длинный текст заметки, который должен корректно отображаться...",
                // ...
            ),
            onClick = {}
        )
    }
}
```

### Декомпозиция UI компонентов

**Каждый UI компонент в отдельном файле!**

Структура папок для экрана:
```
presentation/
└── feature/
    └── ui/
        ├── FeatureScreen.kt          # Главный экран (только структура)
        ├── FeatureTopBar.kt          # Тулбар
        ├── FeatureItem.kt            # Элемент списка
        ├── FeatureDialog.kt          # Диалоги
        └── FeatureComponents.kt      # Вспомогательные компоненты
```

**Правила декомпозиции:**
1. Главный экран (`FeatureScreen.kt`) содержит только структуру и композицию компонентов
2. Каждый переиспользуемый компонент в отдельном файле
3. Вспомогательные private Composable функции в том же файле, что и public
4. Preview в конце каждого файла в блоке `//region Previews ... //endregion`

**Пример структуры файла:**
```kotlin
// NoteItem.kt

/**
 * Элемент заметки с поддержкой свайпа
 */
@Composable
fun NoteItem(
    note: UiBookmarkNote,
    onClick: () -> Unit,
) {
    // Публичная реализация
}

/**
 * Контент элемента заметки
 */
@Composable
private fun NoteItemContent(
    note: UiBookmarkNote,
    onClick: () -> Unit,
) {
    // Приватная реализация
}

//region Previews

@Preview(showBackground = true)
@Composable
private fun NoteItemPreview() {
    SVAPPTheme {
        NoteItem(/* ... */)
    }
}

//endregion
```

---

## Примеры экранов

### Новости (News)

- **Модуль**: `/news`
- **Файлы**: `RootNewsViewModel.kt`, `RootNews.kt`, `GetNewsListUseCase.kt`
- **Особенности**: Paging 3, SwipeRefresh

### Каталог книг (Books)

- **Модуль**: `/books`
- **Файлы**: `RootBooksCatalogViewModel.kt`, `RootBooksCatalog.kt`, `GetBooksListUseCase.kt`
- **Особенности**: Фильтрация, скачивание файлов, BroadcastReceiver

### Wiki

- **Модуль**: `/wiki`
- **Файлы**: `RootWikiViewModel.kt`, `RootWiki.kt`, `WikiRepository.kt`
- **Особенности**: Поиск, кэширование Room, избранное, история
