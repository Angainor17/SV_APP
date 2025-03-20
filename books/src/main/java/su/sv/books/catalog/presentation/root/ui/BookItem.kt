package su.sv.books.catalog.presentation.root.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import su.sv.books.R
import su.sv.books.catalog.presentation.root.model.UiBook
import su.sv.books.catalog.presentation.root.viewmodel.RootBooksActions

@Composable
fun BookItem(item: UiBook, actions: RootBooksActions) {
    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        AsyncImage(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp),
            model = ImageRequest.Builder(LocalContext.current)
                .data(item.image)
                .build(),
            placeholder = painterResource(R.drawable.ic_book_placeholder),
            contentDescription = stringResource(R.string.books_item_image_content_description),
            contentScale = ContentScale.Crop,
        )

        InfoFooter(item)
    }
}

@Composable
private fun InfoFooter(item: UiBook) {
    Column {
        Text(
            text = item.title,
            fontSize = 17.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.width(4.dp))
        Text(item.description)

        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(item.dateFormatted)
            Text(item.pagesCountFormatted)
        }
    }
}

@Composable
@Preview
fun BookItemPreview() {
    val actions = object : RootBooksActions {
        override fun onRetryClick() = Unit
    }
    val item = UiBook(
        id = "id",
        title = "title",
        description = "description",
        image = "image",
        link = "link",
        pagesCountFormatted = "pagesCountFormatted",
        dateFormatted = "dateFormatted",
        isDownloaded = false,
    )
    BookItem(item, actions)
}

