package su.sv.news.presentation.root.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter.State
import coil3.request.ImageRequest
import su.sv.commonui.theme.LocalAdaptiveDimensions
import su.sv.commonui.theme.LocalAppDimensions
import su.sv.commonui.theme.LocalDeviceFormFactor
import su.sv.commonui.theme.SVAPPTheme
import su.sv.commonui.theme.SVAPPThemeLightPreview
import su.sv.commonui.theme.ThemeMode
import su.sv.commonui.theme.cardStroke
import su.sv.commonui.ui.ExpandingText
import su.sv.commonui.ui.shimmerBrush
import su.sv.news.R
import su.sv.news.presentation.root.model.UiNewsItem
import su.sv.news.presentation.root.model.UiNewsMedia

/**
 * Карточка новости
 *
 * @param modifier модификатор
 * @param item данные новости
 * @param onItemClick обработчик клика на медиа-контент
 */
@Composable
fun NewsItem(
    modifier: Modifier = Modifier,
    item: UiNewsItem,
    onItemClick: (UiNewsMedia) -> Unit,
) {
    val dimensions = LocalAppDimensions.current
    val adaptiveDims = LocalAdaptiveDimensions.current
    val formFactor = LocalDeviceFormFactor.current

    // На планшетах используем горизонтальный layout
    if (formFactor.shouldUseNavigationRail()) {
        NewsItemTablet(
            modifier = modifier,
            item = item,
            dimensions = dimensions,
            adaptiveDims = adaptiveDims,
            onItemClick = onItemClick,
        )
    } else {
        NewsItemPhone(
            modifier = modifier,
            item = item,
            dimensions = dimensions,
            adaptiveDims = adaptiveDims,
            onItemClick = onItemClick,
        )
    }
}

/**
 * Вертикальный layout для телефонов (Compact)
 * Картинка сверху, текст и дата снизу
 */
@Composable
private fun NewsItemPhone(
    modifier: Modifier,
    item: UiNewsItem,
    dimensions: su.sv.commonui.theme.AppDimensions,
    adaptiveDims: su.sv.commonui.theme.AdaptiveDimensions,
    onItemClick: (UiNewsMedia) -> Unit,
) {
    Card(
        modifier = modifier
            .padding(
                horizontal = dimensions.screenPaddingHorizontal / 2,
                vertical = dimensions.cardPaddingOuter
            ),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(dimensions.borderWidthStandard, MaterialTheme.colorScheme.cardStroke),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            val hasText = item.description.isNotBlank()
            Logo(
                item = item,
                isTablet = false,
                onItemClick = onItemClick,
            )
            if (hasText) {
                SelectionContainer {
                    ExpandingText(
                        text = item.description,
                        minimizedMaxLines = 4,
                        fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                        modifier = Modifier.padding(
                            start = dimensions.cardContentPaddingHorizontal,
                            end = dimensions.cardContentPaddingHorizontal,
                            top = dimensions.cardContentPaddingHorizontal,
                        ),
                    )
                }
            }

            Text(
                text = item.dateFormatted,
                textAlign = TextAlign.End,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = dimensions.cardContentPaddingHorizontal,
                        end = dimensions.cardContentPaddingHorizontal,
                        bottom = dimensions.cardContentPaddingHorizontal,
                        top = if (hasText) dimensions.itemSpacingSmall else 0.dp,
                    ),
            )
        }
    }
}

/**
 * Горизонтальный layout для планшетов (Medium/Expanded)
 * Картинка слева фиксированного размера, текст справа с датой в правом нижнем углу
 */
@Composable
private fun NewsItemTablet(
    modifier: Modifier,
    item: UiNewsItem,
    dimensions: su.sv.commonui.theme.AppDimensions,
    adaptiveDims: su.sv.commonui.theme.AdaptiveDimensions,
    onItemClick: (UiNewsMedia) -> Unit,
) {
    val hasMedia = item.images.isNotEmpty() || item.videos.isNotEmpty()
    val hasText = item.description.isNotBlank()

    // Состояние для хранения высоты Row
    val rowHeightPxState = remember { mutableIntStateOf(0) }
    val rowHeightPx = rowHeightPxState.value
    val density = LocalDensity.current

    Card(
        modifier = modifier
            .padding(
                horizontal = dimensions.screenPaddingHorizontal / 2,
                vertical = dimensions.cardPaddingOuter
            )
            .fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(dimensions.borderWidthStandard, MaterialTheme.colorScheme.cardStroke),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { size -> rowHeightPxState.value = size.height }
        ) {
            // Картинка слева на всю высоту карточки
            if (hasMedia) {
                val imageHeight = if (rowHeightPx > 0) {
                    with(density) { rowHeightPx.toDp() }
                } else {
                    Dp(180f) // Минимальная высота
                }

                Box(
                    modifier = Modifier
                        .width(240.dp) // Фиксированная ширина для планшета
                        .height(imageHeight)
                ) {
                    Logo(
                        item = item,
                        isTablet = true,
                        onItemClick = onItemClick,
                    )
                }
            }

            // Текст справа с датой в правом нижнем углу
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(dimensions.cardContentPaddingHorizontal)
            ) {
                if (hasText) {
                    SelectionContainer {
                        // Динамический расчёт maxLines на основе высоты
                        // Для планшетов показываем больше текста
                        ExpandingText(
                            text = item.description,
                            minimizedMaxLines = 8, // Увеличено для планшета
                            fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                // Дата в правом нижнем углу
                Text(
                    text = item.dateFormatted,
                    textAlign = TextAlign.End,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = dimensions.itemSpacingSmall),
                )
            }
        }
    }
}

@Composable
private fun Logo(
    item: UiNewsItem,
    isTablet: Boolean,
    onItemClick: (UiNewsMedia) -> Unit,
) {
    val imageSize = item.images.size
    val videosSize = item.videos.size
    val mediaSize = imageSize + videosSize
    val isOnlyOneVideo = mediaSize == 1 && item.videos.size == 1
    val isOnlyOneImage = mediaSize == 1 && imageSize == 1

    when {
        mediaSize == 0 -> return
        isOnlyOneVideo -> SingleVideo(item, isTablet, onItemClick)
        isOnlyOneImage -> SingleImage(item, isTablet, onItemClick)
        else -> MultiImage(
            item = item,
            isTablet = isTablet,
            onItemClick = onItemClick
        )
    }
}

@Composable
private fun SingleImage(
    item: UiNewsItem,
    isTablet: Boolean,
    onItemClick: (UiNewsMedia) -> Unit,
) {
    val showShimmer = remember { mutableStateOf(true) }

    val url = item.images.firstOrNull()?.image.orEmpty()

    AsyncImage(
        modifier = Modifier
            .background(shimmerBrush(targetValue = 1300f, showShimmer = showShimmer.value))
            .then(
                if (isTablet) {
                    Modifier
                        .fillMaxSize()
                        .clip(MaterialTheme.shapes.small)
                } else {
                    Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 200.dp)
                }
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple()
            ) {
                item.images.firstOrNull()?.let { onItemClick(it) }
            },
        model = ImageRequest.Builder(LocalContext.current)
            .data(url)
            .build(),
        contentDescription = stringResource(R.string.news_item_image_content_description),
        contentScale = if (isTablet) ContentScale.Crop else ContentScale.FillWidth,
        onState = { state ->
            if (state is State.Success) {
                showShimmer.value = false
            }
        },
    )
}

@Composable
private fun SingleVideo(
    item: UiNewsItem,
    isTablet: Boolean,
    onVideoClick: (UiNewsMedia) -> Unit,
) {
    val showShimmer = remember { mutableStateOf(true) }
    val isPlayIconVisible = remember { mutableStateOf(false) }

    val video = item.videos.firstOrNull()
    val url = video?.image.orEmpty()

    Box(
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            modifier = Modifier
                .background(shimmerBrush(targetValue = 1300f, showShimmer = showShimmer.value))
                .then(
                    if (isTablet) {
                        Modifier
                            .fillMaxSize()
                            .clip(MaterialTheme.shapes.small)
                    } else {
                        Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 200.dp)
                    }
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple()
                ) {
                    video?.let { onVideoClick(it) }
                },
            model = ImageRequest.Builder(LocalContext.current)
                .data(url)
                .build(),
            contentDescription = stringResource(R.string.news_item_image_content_description),
            contentScale = if (isTablet) ContentScale.Crop else ContentScale.FillWidth,
            onState = { state ->
                if (state is State.Success) {
                    showShimmer.value = false
                    isPlayIconVisible.value = true
                }
            },
        )
        if (isPlayIconVisible.value) {
            Image(
                modifier = Modifier
                    .size(50.dp)
                    .background(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                        shape = CircleShape
                    ),
                imageVector = ImageVector.vectorResource(R.drawable.ic_play_button),
                contentDescription = stringResource(R.string.news_item_play_content_description),
            )
        }
    }
}

@Composable
private fun MultiImage(
    item: UiNewsItem,
    isTablet: Boolean,
    onItemClick: (UiNewsMedia) -> Unit,
) {
    val mediaCount = item.allMedia.size

    if (mediaCount <= 4) {
        // Сетка для 2-4 медиа элементов
        MediaGrid(
            mediaItems = item.allMedia,
            onItemClick = onItemClick,
        )
    } else {
        // Карусель для 5+ медиа элементов
        ImageCarousel(
            item = item,
            isTablet = isTablet,
            onItemClick = onItemClick,
        )
    }
}

/**
 * Сетка для отображения 2-4 медиа элементов
 * 2 элемента: рядом в ряд (50/50)
 * 3 элемента: одно большое слева, два маленьких справа
 * 4 элемента: сетка 2x2
 */
@Composable
private fun MediaGrid(
    mediaItems: List<UiNewsMedia>,
    onItemClick: (UiNewsMedia) -> Unit,
) {
    val dimensions = LocalAppDimensions.current
    val spacing = dimensions.itemSpacingSmall

    when (mediaItems.size) {
        2 -> {
            // Два элемента рядом с адаптивной высотой
            TwoImagesRow(
                media1 = mediaItems[0],
                media2 = mediaItems[1],
                spacing = spacing,
                onItemClick = onItemClick,
            )
        }

        3 -> {
            // Одно большое слева, два маленьких справа сверху вниз
            Row(
                modifier = Modifier.fillMaxWidth(),
            ) {
                MediaGridItem(
                    media = mediaItems[0],
                    modifier = Modifier
                        .weight(2f)
                        .aspectRatio(1f),
                    onItemClick = onItemClick,
                )
                Spacer(modifier = Modifier.width(spacing))
                Column(modifier = Modifier.weight(1f)) {
                    MediaGridItem(
                        media = mediaItems[1],
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f),
                        onItemClick = onItemClick,
                    )
                    Spacer(modifier = Modifier.height(spacing))
                    MediaGridItem(
                        media = mediaItems[2],
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f),
                        onItemClick = onItemClick,
                    )
                }
            }
        }

        4 -> {
            // Сетка 2x2
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    MediaGridItem(
                        media = mediaItems[0],
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f),
                        onItemClick = onItemClick,
                    )
                    Spacer(modifier = Modifier.width(spacing))
                    MediaGridItem(
                        media = mediaItems[1],
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f),
                        onItemClick = onItemClick,
                    )
                }
                Spacer(modifier = Modifier.height(spacing))
                Row(modifier = Modifier.fillMaxWidth()) {
                    MediaGridItem(
                        media = mediaItems[2],
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f),
                        onItemClick = onItemClick,
                    )
                    Spacer(modifier = Modifier.width(spacing))
                    MediaGridItem(
                        media = mediaItems[3],
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f),
                        onItemClick = onItemClick,
                    )
                }
            }
        }
    }
}

/**
 * Элемент сетки медиа
 */
@Composable
private fun MediaGridItem(
    media: UiNewsMedia,
    modifier: Modifier,
    onItemClick: (UiNewsMedia) -> Unit,
) {
    val showShimmer = remember { mutableStateOf(true) }
    val isPlayIconVisible = remember { mutableStateOf(false) }
    val isVideo = media is UiNewsMedia.ItemVideo

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(media.image)
                .build(),
            contentDescription = "",
            modifier = Modifier
                .fillMaxSize()
                .background(
                    shimmerBrush(
                        targetValue = 1300f,
                        showShimmer = showShimmer.value
                    )
                )
                .clip(MaterialTheme.shapes.small)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple()
                ) { onItemClick.invoke(media) },
            contentScale = ContentScale.Crop,
            onState = { state ->
                if (state is State.Success) {
                    showShimmer.value = false
                    if (isVideo) isPlayIconVisible.value = true
                }
            },
        )

        if (isVideo && isPlayIconVisible.value) {
            Image(
                modifier = Modifier
                    .size(50.dp)
                    .background(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                        shape = CircleShape
                    ),
                imageVector = ImageVector.vectorResource(R.drawable.ic_play_button),
                contentDescription = stringResource(R.string.news_item_play_content_description),
            )
        }
    }
}

/**
 * Ряд из двух изображений с адаптивной высотой
 * Высота определяется по размерам из API
 */
@Composable
private fun TwoImagesRow(
    media1: UiNewsMedia,
    media2: UiNewsMedia,
    spacing: Dp,
    onItemClick: (UiNewsMedia) -> Unit,
) {
    // Получаем aspectRatio из размеров API
    val width1 = media1.width
    val height1 = media1.height
    val width2 = media2.width
    val height2 = media2.height

    val aspectRatio1 = if (width1 != null && height1 != null && width1 > 0) {
        width1.toFloat() / height1.toFloat()
    } else null

    val aspectRatio2 = if (width2 != null && height2 != null && width2 > 0) {
        width2.toFloat() / height2.toFloat()
    } else null

    // Если оба размера известны - используем адаптивную высоту
    if (aspectRatio1 != null && aspectRatio2 != null) {
        // Берём min aspectRatio (самую "высокую" картинку)
        // Для Row: высота = ширина / (2 * aspectRatio), т.к. две картинки в ряд
        val minAspectRatio = minOf(aspectRatio1, aspectRatio2)
        val rowAspectRatio = 2f * minAspectRatio

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(rowAspectRatio),
        ) {
            AdaptiveMediaItem(
                media = media1,
                modifier = Modifier.weight(1f),
                onItemClick = onItemClick,
            )
            Spacer(modifier = Modifier.width(spacing))
            AdaptiveMediaItem(
                media = media2,
                modifier = Modifier.weight(1f),
                onItemClick = onItemClick,
            )
        }
    } else {
        // Fallback: квадраты если размеры неизвестны
        Row(
            modifier = Modifier.fillMaxWidth(),
        ) {
            MediaGridItem(
                media = media1,
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f),
                onItemClick = onItemClick,
            )
            Spacer(modifier = Modifier.width(spacing))
            MediaGridItem(
                media = media2,
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f),
                onItemClick = onItemClick,
            )
        }
    }
}

/**
 * Адаптивный элемент медиа
 */
@Composable
private fun AdaptiveMediaItem(
    media: UiNewsMedia,
    modifier: Modifier,
    onItemClick: (UiNewsMedia) -> Unit,
) {
    val showShimmer = remember { mutableStateOf(true) }
    val isPlayIconVisible = remember { mutableStateOf(false) }
    val isVideo = media is UiNewsMedia.ItemVideo

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(media.image)
                .build(),
            contentDescription = "",
            modifier = Modifier
                .fillMaxSize()
                .background(
                    shimmerBrush(
                        targetValue = 1300f,
                        showShimmer = showShimmer.value
                    )
                )
                .clip(MaterialTheme.shapes.small)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple()
                ) { onItemClick.invoke(media) },
            contentScale = ContentScale.Crop,
            onState = { state ->
                if (state is State.Success) {
                    showShimmer.value = false
                    if (isVideo) isPlayIconVisible.value = true
                }
            },
        )

        if (isVideo && isPlayIconVisible.value) {
            Image(
                modifier = Modifier
                    .size(50.dp)
                    .background(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                        shape = CircleShape
                    ),
                imageVector = ImageVector.vectorResource(R.drawable.ic_play_button),
                contentDescription = stringResource(R.string.news_item_play_content_description),
            )
        }
    }
}

// ============================================================
// Previews
// ============================================================

@Composable
@Preview(
    showBackground = true,
    backgroundColor = 0xFFFFFFFF,
)
fun SingleVideoPreview() {
    val item = UiNewsItem(
        id = "id",
        dateFormatted = "2 февраля",
        description = "В. И. Ленин",
        images = listOf(),
        videos = listOf(
            UiNewsMedia.ItemVideo(
                id = "1",
                image = "https://picsum.photos/300/300",
                link = "link"
            )
        ),
        allMedia = listOf(),
        vkPostUrl = "https://vk.com/post"
    )
    SVAPPThemeLightPreview {
        NewsItem(item = item, onItemClick = {})
    }
}

@Composable
@Preview(
    showBackground = true,
    backgroundColor = 0xFF1C1B1F,
)
fun SingleVideoPreviewDark() {
    val item = UiNewsItem(
        id = "id",
        dateFormatted = "2 февраля",
        description = "В. И. Ленин",
        images = listOf(),
        videos = listOf(
            UiNewsMedia.ItemVideo(
                id = "1",
                image = "https://picsum.photos/300/300",
                link = "link"
            )
        ),
        allMedia = listOf(),
        vkPostUrl = "https://vk.com/post"
    )
    SVAPPTheme(themeMode = ThemeMode.DARK) {
        NewsItem(item = item, onItemClick = {})
    }
}

@Composable
@Preview(
    showBackground = true,
    backgroundColor = 0xFFFFFFFF,
)
fun NewsItemPreview() {
    val item = UiNewsItem(
        id = "id",
        dateFormatted = "2 февраля",
        description = "Текст новости с достаточно длинным описанием для проверки отображения",
        images = listOf(
            UiNewsMedia.ItemImage("https://picsum.photos/300/300")
        ),
        videos = listOf(),
        allMedia = listOf(),
        vkPostUrl = "https://vk.com/post"
    )
    SVAPPThemeLightPreview {
        NewsItem(item = item, onItemClick = {})
    }
}

@Composable
@Preview(
    showBackground = true,
    backgroundColor = 0xFFFFFFFF,
    widthDp = 400,
)
fun MediaGrid2ItemsPreview() {
    val item = UiNewsItem(
        id = "id",
        dateFormatted = "2 февраля",
        description = "Текст новости",
        images = listOf(
            UiNewsMedia.ItemImage("https://picsum.photos/300/300"),
            UiNewsMedia.ItemImage("https://picsum.photos/300/300")
        ),
        videos = listOf(),
        allMedia = listOf(
            UiNewsMedia.ItemImage("https://picsum.photos/300/300"),
            UiNewsMedia.ItemImage("https://picsum.photos/300/300")
        ),
        vkPostUrl = "https://vk.com/post"
    )
    SVAPPThemeLightPreview {
        NewsItem(item = item, onItemClick = {})
    }
}

@Composable
@Preview(
    showBackground = true,
    backgroundColor = 0xFFFFFFFF,
    widthDp = 400,
)
fun MediaGrid3ItemsPreview() {
    val item = UiNewsItem(
        id = "id",
        dateFormatted = "2 февраля",
        description = "Текст новости",
        images = listOf(),
        videos = listOf(),
        allMedia = listOf(
            UiNewsMedia.ItemImage("https://picsum.photos/300/300"),
            UiNewsMedia.ItemImage("https://picsum.photos/300/300"),
            UiNewsMedia.ItemImage("https://picsum.photos/300/300")
        ),
        vkPostUrl = "https://vk.com/post"
    )
    SVAPPThemeLightPreview {
        NewsItem(item = item, onItemClick = {})
    }
}

@Composable
@Preview(
    showBackground = true,
    backgroundColor = 0xFFFFFFFF,
    widthDp = 400,
)
fun MediaGrid4ItemsPreview() {
    val item = UiNewsItem(
        id = "id",
        dateFormatted = "2 февраля",
        description = "Текст новости",
        images = listOf(),
        videos = listOf(),
        allMedia = listOf(
            UiNewsMedia.ItemImage("https://picsum.photos/300/300"),
            UiNewsMedia.ItemImage("https://picsum.photos/300/300"),
            UiNewsMedia.ItemImage("https://picsum.photos/300/300"),
            UiNewsMedia.ItemImage("https://picsum.photos/300/300")
        ),
        vkPostUrl = "https://vk.com/post"
    )
    SVAPPThemeLightPreview {
        NewsItem(item = item, onItemClick = {})
    }
}
