package su.sv.news.presentation.root.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import su.sv.commonui.theme.SVAPPTheme
import su.sv.news.R
import su.sv.news.presentation.root.model.UiNewsItem
import su.sv.news.presentation.root.model.UiNewsMedia
import kotlin.math.absoluteValue

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ImageCarousel(
    modifier: Modifier = Modifier,
    item: UiNewsItem,
    onItemClick: (UiNewsMedia) -> Unit,
) {
    val allMedia = item.allMedia
    val pagerState = rememberPagerState { allMedia.size }

    Column(
        modifier
            .defaultMinSize(minHeight = 220.dp)
            .fillMaxWidth()
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth(),
            pageSpacing = 10.dp,
            contentPadding = PaddingValues(horizontal = 30.dp),
        ) { page ->
            val item = allMedia[page]
            val isVideo = item is UiNewsMedia.ItemVideo

            Box(
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(item.image)
                        .build(),
                    contentDescription = "",
                    modifier = Modifier
                        .fillMaxWidth()
                        .apply {
                            if (isVideo) clickable {
                                onItemClick.invoke(item)
                            }
                        }
                        .graphicsLayer {
                            val pageOffset =
                                (pagerState.currentPage - page + pagerState.currentPageOffsetFraction).absoluteValue

                            lerp(
                                start = 75.dp,
                                stop = 100.dp,
                                fraction = 1f - pageOffset.coerceIn(0f, 1f)
                            ).also { scale ->
                                scaleY = scale / 100.dp
                            }
                        },
                    contentScale = ContentScale.Crop,
                )

                if (isVideo) {
                    Image(
                        modifier = Modifier
                            .size(50.dp)
                            .background(Color.LightGray.copy(alpha = 0.6f), shape = CircleShape),
                        imageVector = ImageVector.vectorResource(R.drawable.ic_play_button),
                        contentDescription = stringResource(R.string.news_item_play_content_description),
                    )
                }
            }
        }
    }
}

@Composable
@Preview(
    showBackground = true,
    backgroundColor = 0xFFFFFFFF,
)
fun ImageCarouselPreview() {
    val video = UiNewsMedia.ItemVideo(
        id = "id",
        image = "image",
        link = "link",
    )
    val item = UiNewsItem(
        id = "id",
        dateFormatted = "2 февраля",
        description = "В. И. Ленин",
        images = listOf(
            UiNewsMedia.ItemImage(
                "https://picsum.photos/300/300"
            )
        ),
        videos = listOf(video),
        allMedia = listOf(video)
    )

    SVAPPTheme {
        ImageCarousel(
            item = item,
        ) {}
    }
}
