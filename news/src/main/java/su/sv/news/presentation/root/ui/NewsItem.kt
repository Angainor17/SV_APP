package su.sv.news.presentation.root.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import su.sv.commonui.theme.SVAPPTheme
import su.sv.news.R
import su.sv.news.presentation.root.model.UiNewsItem
import su.sv.news.presentation.root.viewmodel.actions.RootNewsActions
import su.sv.news.presentation.root.viewmodel.actions.RootNewsActionsHandler

@Composable
fun NewsItem(item: UiNewsItem, actions: RootNewsActionsHandler) {
    Card(modifier = Modifier.padding(8.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
//            .clickable {
//                actions.onAction(RootBookActions.OnBookClick(item))
//            },
        ) {
            Logo(item, actions)
            Text(text = item.id)
            Text(text = item.title)
            Text(text = item.description)
        }
    }
}

@Composable
private fun Logo(item: UiNewsItem, actions: RootNewsActionsHandler) {
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
    val actions = object : RootNewsActionsHandler {
        override fun onAction(action: RootNewsActions) = Unit
    }
    val item = UiNewsItem(
        id = "id",
        title = "Государство и Революция",
        description = "В. И. Ленин",
        image = "https://picsum.photos/300/300",
    )
    SVAPPTheme {
        NewsItem(item, actions)
    }
}
