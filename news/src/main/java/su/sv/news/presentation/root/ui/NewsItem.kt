package su.sv.news.presentation.root.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import su.sv.news.R
import su.sv.news.presentation.root.model.UiNewsItem

@Composable
fun NewsItem(item: UiNewsItem) {
    Card(modifier = Modifier.padding(horizontal = 8.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Logo(item)
            Text(
                text = item.description,
                maxLines = 5,
                fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                modifier = Modifier.padding(
                    start = 8.dp,
                    end = 8.dp,
                    top = 8.dp,
                ),
            )

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
    if (item.image.isEmpty()) return

    Box {
        AsyncImage(
            modifier = Modifier
                .fillMaxWidth(),
            model = ImageRequest.Builder(LocalContext.current)
                .data(item.image)
                .build(),
//            placeholder = painterResource(R.drawable.ic_book_placeholder),
            contentDescription = stringResource(R.string.news_item_image_content_description),
            contentScale = ContentScale.Crop,
        )
    }
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
        image = "https://picsum.photos/300/300",
    )
    SVAPPTheme {
        NewsItem(item)
    }
}
