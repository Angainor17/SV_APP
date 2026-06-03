# News Module

Модуль ленты новостей из VK.

## Обзор

Модуль `news` отображает ленту новостей из группы VK. Поддерживает пагинацию, отображение фото и видео, а также текстовые посты с ссылками.

## Архитектура

```
presentation/
├── root/
│   ├── RootNews.kt              # Главный экран
│   ├── RootNewsViewModel.kt     # ViewModel
│   ├── ui/
│   │   ├── NewsList.kt          # Список новостей
│   │   ├── NewsItem.kt          # Элемент новости
│   │   └── ImageCarousel.kt     # Карусель изображений
│   ├── mapper/
│   │   ├── UiNewsMapper.kt      # Маппер новостей
│   │   └── UiNewsMediaMapper.kt # Маппер медиа
│   └── viewmodel/
│       ├── NewsPagingSource.kt  # Источник данных для пагинации
│       ├── actions/
│       │   ├── RootNewsActions.kt
│       │   └── RootNewsActionsHandler.kt
│       └── effects/
│           └── NewsListOneTimeEffect.kt
domain/
├── GetNewsListUseCase.kt        # Use case для получения новостей
├── mapper/VideosMapper.kt       # Маппер видео
└── model/
    ├── NewsItem.kt
    └── NewsMediaItem.kt
```

## Основные компоненты

### RootNewsViewModel
ViewModel экрана новостей:

```kotlin
class RootNewsViewModel : BaseViewModel() {
    val state: StateFlow<UiRootNewsState>
    fun onAction(action: RootNewsActions)
}
```

### NewsPagingSource
Paging 3 источник данных:

```kotlin
class NewsPagingSource(
    private val getNewsListUseCase: GetNewsListUseCase
) : PagingSource<Int, UiNewsItem>()
```

### UiNewsItem
Модель новости для UI:

```kotlin
data class UiNewsItem(
    val id: Int,
    val text: String,
    val date: String,
    val media: List<UiNewsMedia>
)
```

### UiNewsMedia
Модель медиа-контента:

```kotlin
sealed class UiNewsMedia {
    data class Photo(val url: String) : UiNewsMedia()
    data class Video(val url: String, val preview: String) : UiNewsMedia()
}
```

## Actions

```kotlin
sealed class RootNewsActions {
    object LoadMore : RootNewsActions()
    data class OpenLink(val url: String) : RootNewsActions()
    data class OpenVideo(val video: UiNewsMedia.Video) : RootNewsActions()
}
```

## Пагинация

Используется Paging 3:

```kotlin
Pager(
    config = PagingConfig(pageSize = 20),
    pagingSourceFactory = { NewsPagingSource(useCase) }
).flow
```

## Структура файлов

```
news/src/main/java/su/sv/news/
├── domain/
│   ├── GetNewsListUseCase.kt
│   ├── mapper/VideosMapper.kt
│   └── model/
│       ├── NewsItem.kt
│       └── NewsMediaItem.kt
└── presentation/
    ├── utils/BasePagingSource.kt
    └── root/
        ├── RootNewsViewModel.kt
        ├── RootNews.kt
        ├── ui/
        │   ├── NewsList.kt
        │   ├── NewsItem.kt
        │   └── ImageCarousel.kt
        ├── mapper/
        │   ├── UiNewsMapper.kt
        │   └── UiNewsMediaMapper.kt
        ├── model/
        │   ├── UiRootNewsState.kt
        │   ├── UiNewsItem.kt
        │   └── UiNewsMedia.kt
        └── viewmodel/
            ├── NewsPagingSource.kt
            ├── actions/
            ├── effects/
```
