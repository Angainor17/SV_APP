# API Module

Модуль для работы с VK API.

## Обзор

Модуль `api` предоставляет клиент для работы с VK API, в частности для получения новостей (стен сообщества) и видео.

## Основные классы

### VkApi
Retrofit интерфейс для VK API.

```kotlin
interface VkApi {
    suspend fun getPosts(accessToken, domain, offset, count): VkResponse<VkNewsResponse>
    suspend fun getVideo(accessToken, videos, offset, count): VkResponse<VkNewsResponse>
}
```

### VkNewsResponse
Модель ответа VK API с вложенной структурой:
```
VkResponse {
    response: VkNewsResponse {
        items: List<VkResponseNewsAttachment>
    }
}
```

### VkResponseNewsAttachment
Модель элемента новости с вложениями:
- Текст поста
- Фотографии (`VkAttachmentPhoto`)
- Видео (`VkAttachmentVideo`)

### NewsRepo
Репозиторий для получения новостей:

```kotlin
class NewsRepo @Inject constructor(private val vkApi: VkApi) {
    suspend fun getPosts(domain: String, offset: Int, count: Int): Result<List<ApiNewsItem>>
    suspend fun getVideo(videoIds: String): Result<VkResponseNewsAttachment?>
}
```

### ApiNewsItem
Модель элемента новости для UI:
```kotlin
data class ApiNewsItem(
    val id: Int,
    val text: String?,
    val date: Long,
    val photos: List<VkAttachmentPhoto>,
    val video: VkAttachmentVideo?
)
```

## DI

Модуль предоставляет DI через `NewsApiModule`:

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object NewsApiModule {
    @Provides
    fun provideVkApi(): VkApi

    @Provides
    fun provideNewsRepo(vkApi: VkApi): NewsRepo
}
```

## VK API Version

Используется версия API: `5.199`

## Структура файлов

```
api/src/main/java/su/sv/api/
├── data/
│   ├── api/VkApi.kt              # Retrofit интерфейс
│   ├── model/ApiNewsItem.kt      # Модель для UI
│   ├── repo/NewsRepo.kt          # Репозиторий
│   └── response/                 # Модели ответов API
│       ├── VkResponse.kt
│       ├── VkNewsResponse.kt
│       ├── VkResponseNewsAttachment.kt
│       ├── VkAttachmentPhoto.kt
│       ├── VkAttachmentVideo.kt
│       └── VkPhotoSize.kt
└── di/NewsApiModule.kt           # DI модуль
```
