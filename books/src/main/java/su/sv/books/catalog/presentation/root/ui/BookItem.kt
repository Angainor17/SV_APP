package su.sv.books.catalog.presentation.root.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
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
import su.sv.books.catalog.presentation.root.viewmodel.actions.RootBookActions
import su.sv.books.catalog.presentation.root.viewmodel.actions.RootBooksActions
import su.sv.commonui.theme.SVAPPTheme

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
    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.secondary)
    ) {
        Spacer(Modifier.height(4.dp))
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
fun InfoFooterPreview() {
    val item = UiBook(
        id = "id",
        title = "Государство и Революция",
        description = "В. И. Ленин",
        image = "https://picsum.photos/300/300",
        downloadUrl = "link",
        fileNameWithExt = "1.pdf",
        pagesCountFormatted = "323 стр.",
        dateFormatted = "25 февр. 2025",

        isDownloaded = false,
        isDownloading = true,
        fileUri = null,
    )
    SVAPPTheme {
        InfoFooter(item)
    }
}

@Composable
@Preview
fun BookItemPreview() {
    val actions = object : RootBooksActions {
        override fun onAction(action: RootBookActions) = Unit
    }
    val item = UiBook(
        id = "id",
        title = "Государство и Революция",
        description = "В. И. Ленин",
        image = "https://picsum.photos/300/300",
        downloadUrl = "link",
        fileNameWithExt = "1.pdf",
        pagesCountFormatted = "323 стр.",
        dateFormatted = "25 февр. 2025",

        isDownloaded = false,
        isDownloading = false,
        fileUri = null,
    )
    SVAPPTheme {
        BookItem(item, actions)
    }
}

