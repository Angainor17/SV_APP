package su.sv.news.presentation.root.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import su.sv.commonui.theme.SVAPPTheme
import su.sv.commonui.ui.ExpandingText
import su.sv.commonui.ui.ImageCarousel
import su.sv.commonui.ui.shimmerBrush
import su.sv.news.R
import su.sv.news.presentation.root.model.UiNewsItem

@Composable
fun NewsItem(item: UiNewsItem) {
    Card(modifier = Modifier.padding(horizontal = 8.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Logo(item)
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
                color = Color.DarkGray,
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
private fun Logo(item: UiNewsItem) {
    when {
        item.images.isEmpty() -> return
        item.images.size == 1 -> SingleImage(item)
        else -> MultiImage(item)
    }
}

@Composable
private fun SingleImage(item: UiNewsItem) {
    val showShimmer = remember { mutableStateOf(true) }

    AsyncImage(
        modifier = Modifier
            .background(shimmerBrush(targetValue = 1300f, showShimmer = showShimmer.value))
            .fillMaxWidth()
            .heightIn(min = 220.dp),
        model = ImageRequest.Builder(LocalContext.current)
            .data(item.images.firstOrNull().orEmpty())
            .build(),
        contentDescription = stringResource(R.string.news_item_image_content_description),
        contentScale = ContentScale.Crop,
    )
}

@Composable
private fun MultiImage(item: UiNewsItem) {
    ImageCarousel(
        images = item.images,
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
            "https://picsum.photos/300/300"
        ),
    )
    SVAPPTheme {
        NewsItem(item)
    }
}
