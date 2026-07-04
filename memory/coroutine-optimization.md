---
name: coroutine-optimization
description: Best practices для использования корутин и DispatcherProvider в SV APP
metadata: 
  node_type: memory
  type: project
  originSessionId: 6539e1e4-578a-4ff4-830e-c5f512f0f659
---

## DispatcherProvider

Интерфейс для DI-инъекции диспетчеров (testable):

```kotlin
interface DispatcherProvider {
    val io: CoroutineDispatcher      // IO: файлы, сеть, DB
    val main: CoroutineDispatcher    // Main: UI обновления
    val default: CoroutineDispatcher // Default: CPU-intensive (маппинг, сортировка)
}
```

Инъекция через Hilt: `commonarchitecture/di/module/CoroutineModule.kt`

## Правила использования

| Операция | Диспетчер | Пример |
|----------|-----------|--------|
| **IO** (файлы, сеть, DB, DataStore) | `dispatcherProvider.io` | API запросы, Room DAO, JSON parsing |
| **CPU-intensive** | `dispatcherProvider.default` | Маппинг, сортировка, фильтрация |
| **UI** | `Dispatchers.Main` (viewModelScope) | `_state.value = ...`, `_effect.trySend()` |

## UseCase Best Practice

UseCase должны быть **main-safe** — вызываться из Main thread без `withContext`:

```kotlin
class GetArticleUseCase @Inject constructor(
    private val dispatcherProvider: DispatcherProvider,
    private val repository: WikiRepository,
) {
    suspend fun execute(title: String): WikiResult<WikiArticle> {
        return withContext(dispatcherProvider.io) {
            repository.getArticle(title)
        }
    }
}
```

## ViewModel Pattern

```kotlin
@HiltViewModel
class ArticleViewModel @Inject constructor(
    private val dispatcherProvider: DispatcherProvider,
    private val getArticleUseCase: GetArticleUseCase,  // уже main-safe
    private val mapper: UiWikiMapper,
) : BaseViewModel() {

    fun loadArticle(title: String) {
        viewModelScope.launch {
            // UseCase уже main-safe
            val result = getArticleUseCase.execute(title)

            // Маппинг - CPU intensive, на Default
            val uiArticle = withContext(dispatcherProvider.default) {
                mapper.mapToUi(result.data)
            }

            // UI обновление - на Main
            _state.value = ArticleState.Content(uiArticle)
        }
    }
}
```

## Flow и DispatcherProvider

Flow не требует `withContext` — используйте `flowOn()`:

```kotlin
// В Repository
override fun getFavorites(): Flow<List<WikiArticle>> {
    return favoriteDao.getAllFavorites()
        .map { entities -> entities.map { it.toDomain() } }
        .flowOn(dispatcherProvider.io)  // IO для DB + маппинга
}
```

## Оптимизированные модули (2026-07-04)

- `wiki/domain/usecase/` — все UseCase main-safe
- `wiki/presentation/` — ViewModel с DispatcherProvider
- `managers/theme/ThemeViewModel.kt` — DataStore на IO
- `bookreader/domain/BookmarksRepository.kt` — файловые операции на IO
- `books/presentation/downloaded/DownloadedBooksViewModel.kt` — маппинг на Default

## Почему не хардкод Dispatchers.IO

Хардкод затрудняет тестирование:

```kotlin
// ❌ Bad - нельзя подставить TestDispatcher
withContext(Dispatchers.IO) { ... }

// ✅ Good - testable
withContext(dispatcherProvider.io) { ... }

// В тестах:
val testDispatcher = StandardTestDispatcher()
val testProvider = TestDispatcherProvider(io = testDispatcher)
```

## Связанные memories

- [[compose-optimizations]] — оптимизации Compose
- [[mock-system]] — система моков для тестирования