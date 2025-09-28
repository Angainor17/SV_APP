package su.sv.news.presentation.root.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import su.sv.commonui.theme.SVAPPTheme
import su.sv.commonui.ui.ExpandingText
import su.sv.commonui.ui.shimmerBrush
import su.sv.news.R
import su.sv.news.presentation.root.model.UiNewsItem
import su.sv.news.presentation.root.model.UiNewsMedia

@Composable
fun NewsItem(
    item: UiNewsItem,
    onItemClick: (UiNewsMedia) -> Unit,
) {
    Card(modifier = Modifier.padding(horizontal = 8.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Logo(
                item = item,
                onItemClick = onItemClick,
            )
            SelectionContainer {
                ExpandingText(
                    text = item.description,
                    minimizedMaxLines = 4,
                    fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                    modifier = Modifier.padding(
                        start = 8.dp,
                        end = 8.dp,
                        top = 8.dp,
                    ),
                )
            }

            Text(
                text = item.dateFormatted,
                textAlign = TextAlign.End,
                color = MaterialTheme.colorScheme.onPrimary,
                fontSize = MaterialTheme.typography.bodySmall.fontSize,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 8.dp,
                        bottom = 8.dp,
                        end = 8.dp,
                        top = 4.dp,
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
            .heightIn(min = 220.dp),
        model = ImageRequest.Builder(LocalContext.current)
            .data(url)
            .build(),
        contentDescription = stringResource(R.string.news_item_image_content_description),
        contentScale = ContentScale.Crop,
    )
}

@Composable
private fun SingleVideo(
    item: UiNewsItem,
    onVideoClick: (UiNewsMedia) -> Unit,
) {
    val showShimmer = remember { mutableStateOf(true) }

    val video = item.videos.firstOrNull()
    val url = video?.image.orEmpty()

    Box(
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            modifier = Modifier
                .background(shimmerBrush(targetValue = 1300f, showShimmer = showShimmer.value))
                .fillMaxWidth()
                .clickable {
                    video?.let { onVideoClick(it) }
                }
                .heightIn(min = 220.dp),
            model = ImageRequest.Builder(LocalContext.current)
                .data(url)
                .build(),
            contentDescription = stringResource(R.string.news_item_image_content_description),
            contentScale = ContentScale.Crop,
        )
        Image(
            modifier = Modifier
                .size(50.dp)
                .background(Color.LightGray.copy(alpha = 0.6f), shape = CircleShape),
            imageVector = ImageVector.vectorResource(R.drawable.ic_play_button),
            contentDescription = stringResource(R.string.news_item_play_content_description),
        )
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

@Composable
@Preview(
    showBackground = true,
    backgroundColor = 0xFFFFFFFF,
)
fun BookItemPreview() {
    val item = UiNewsItem(
        id = "id",
        dateFormatted = "2 февраля",
        description = "В. И. Ленин",
        images = listOf(
            UiNewsMedia.ItemImage(
                "https://picsum.photos/300/300"
            )
        ),
        videos = listOf(),
        allMedia = listOf()
    )
    SVAPPTheme {
        NewsItem(item) {

        }
    }
}
