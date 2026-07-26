# CommonArchitecture Module

Базовая архитектура приложения.

## Обзор

Модуль `commonarchitecture` содержит базовые классы и утилиты для архитектуры приложения, включая базовые ViewModel, Activity, DI модули и обработку ошибок.

## Основные классы

### BaseViewModel
Базовый класс для всех ViewModel:

```kotlin
abstract class BaseViewModel : ViewModel()
```

Все ViewModel в приложении наследуются от этого класса.

### BaseActivity
Базовый класс для Activity:

```kotlin
abstract class BaseActivity : AppCompatActivity()
```

### NetworkError
Класс ошибки сети:

```kotlin
class NetworkError(cause: Throwable) : Exception(cause)
```

### runCatchingHttpRequest
Обёртка для API запросов с обработкой ошибок:

```kotlin
val result = runCatchingHttpRequest {
    api.getPosts(...)
}

when {
    result.isSuccess -> { /* Успех */ }
    result.exception is NetworkError -> { /* Ошибка сети */ }
    else -> { /* Другая ошибка */ }
}
```

### ResourcesRepository
Доступ к ресурсам приложения (перемещён в commonarchitecture):

```kotlin
class ResourcesRepository @Inject constructor(context: Context) {
    fun getString(resId: Int): String
    fun getString(resId: Int, vararg args: Any): String
}
```

## Mock-система

Модуль содержит систему моков для сетевых запросов:

- `MockConfig` — конфигурация включения моков
- `MockDataProvider` — провайдер мок-данных для API
- `MockInterceptor` — OkHttp интерцептор для подмены ответов

## DI Модули

### ApiServiceModule
Модуль для предоставления API сервисов.

### CoroutineModule
Модуль для предоставления корутин:

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object CoroutineModule {
    @Provides
    fun provideCoroutineScope(): CoroutineScope
}
```

## Use Cases

### UseCases
Базовые интерфейсы для use cases:

```kotlin
interface UseCase<T> {
    suspend operator fun invoke(): T
}

interface ParamUseCase<P, T> {
    suspend operator fun invoke(param: P): T
}
```

## Структура файлов

```
commonarchitecture/src/main/java/su/sv/commonarchitecture/
├── data/
│   ├── NetworkError.kt              # Класс ошибки сети
│   └── RunCatchingApiRequest.kt     # Обёртка для API запросов
├── di/
│   ├── ApiServiceModule.kt          # DI модуль для API
│   └── module/
│       └── CoroutineModule.kt       # DI модуль для корутин
├── managers/
│   └── ResourcesRepository.kt       # Доступ к ресурсам (перемещён сюда)
├── mock/
│   ├── MockConfig.kt                # Конфигурация моков
│   ├── MockDataProvider.kt          # Провайдер мок-данных
│   └── MockInterceptor.kt           # OkHttp интерцептор для моков
└── presentation/base/
    ├── BaseActivity.kt              # Базовая Activity
    └── BaseViewModel.kt             # Базовая ViewModel
```
