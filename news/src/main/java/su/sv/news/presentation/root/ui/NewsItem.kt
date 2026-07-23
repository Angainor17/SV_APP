package su.sv.news.presentation.root.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter.State
import coil3.request.ImageRequest
import su.sv.commonui.theme.LocalAppDimensions
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

    Card(
        modifier = modifier.padding(
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

@Composable
private fun Logo(
    item: UiNewsItem,
    onItemClick: (UiNewsMedia) -> Unit,
) {
    val imageSize = item.images.size
    val videosSize = item.videos.size
    val mediaSize = imageSize + videosSize
    val isOnlyOneVideo = mediaSize == 1 && item.videos.size == 1
    val isOnlyOneImage = mediaSize == 1 && imageSize == 1

    when {
        mediaSize == 0 -> return
        isOnlyOneVideo -> SingleVideo(item, onItemClick)
        isOnlyOneImage -> SingleImage(item)
        else -> MultiImage(
            item = item,
            onItemClick = onItemClick
        )
    }
}

@Composable
private fun SingleImage(item: UiNewsItem) {
    val showShimmer = remember { mutableStateOf(true) }

    val url = item.images.firstOrNull()?.image.orEmpty()

    AsyncImage(
        modifier = Modifier
            .background(shimmerBrush(targetValue = 1300f, showShimmer = showShimmer.value))
            .fillMaxWidth()
            .defaultMinSize(minHeight = 200.dp),
        model = ImageRequest.Builder(LocalContext.current)
            .data(url)
            .build(),
        contentDescription = stringResource(R.string.news_item_image_content_description),
        contentScale = ContentScale.FillWidth,
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
                .defaultMinSize(minHeight = 200.dp)
                .fillMaxWidth()
                .align(Alignment.TopCenter)
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
            contentScale = ContentScale.FillWidth,
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
    onItemClick: (UiNewsMedia) -> Unit,
) {
    ImageCarousel(
        item = item,
        onItemClick = onItemClick,
    )
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
        allMedia = listOf()
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
        allMedia = listOf()
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
        allMedia = listOf()
    )
    SVAPPThemeLightPreview {
        NewsItem(item = item, onItemClick = {})
    }
}
