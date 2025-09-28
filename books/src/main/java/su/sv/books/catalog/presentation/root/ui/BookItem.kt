package su.sv.books.catalog.presentation.root.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import su.sv.books.R
import su.sv.books.catalog.presentation.root.viewmodel.actions.RootBookActions
import su.sv.books.catalog.presentation.root.viewmodel.actions.RootBooksActions
import su.sv.commonui.theme.SVAPPTheme
import su.sv.models.ui.book.UIBookState
import su.sv.models.ui.book.UiBook

@Composable
fun BookItem(item: UiBook, actions: RootBooksActions) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                actions.onAction(RootBookActions.OnBookClick(item))
            },
    ) {
        Logo(item, actions)
        InfoFooter(item)
    }
}

@Composable
private fun Logo(item: UiBook, actions: RootBooksActions) {
    Box {
        AsyncImage(
            modifier = Modifier
                .clip(RoundedCornerShape(topEnd = 8.dp, topStart = 8.dp))
                .fillMaxWidth()
                .height(250.dp),
            model = ImageRequest.Builder(LocalContext.current)
                .data(item.image)
                .build(),
            placeholder = painterResource(R.drawable.ic_book_placeholder),
            contentDescription = stringResource(R.string.books_item_image_content_description),
            contentScale = ContentScale.Crop,
        )

        // статус скачивания
        BookDownloadStatus(item, actions)
    }
}

@Composable
private fun BoxScope.BookDownloadStatus(item: UiBook, actions: RootBooksActions) {
    Button(
        onClick = {
            if (item.downloadState == UIBookState.AVAILABLE_TO_DOWNLOAD) {
                actions.onAction(RootBookActions.OnDownloadBookClick(item))
            }
        },
        contentPadding = PaddingValues(all = 3.dp),
        shape = CircleShape,
        border = BorderStroke(1.dp, Color.Gray),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.tertiaryContainer,
        ),
        modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(top = 12.dp, end = 12.dp)
            .size(42.dp),
    ) {
        when (item.downloadState) {
            UIBookState.DOWNLOADED -> {
                Image(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_book_downloaded),
                    contentDescription = stringResource(R.string.books_download_status_content_description),
                    modifier = Modifier
                        .padding(6.dp)
                        .fillMaxSize(),
                )
            }

            UIBookState.AVAILABLE_TO_DOWNLOAD -> {
                Image(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_download),
                    contentDescription = stringResource(R.string.books_download_status_content_description),
                    modifier = Modifier
                        .padding(3.dp)
                        .fillMaxSize(),
                )
            }

            UIBookState.DOWNLOADING -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(26.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.secondary,
                    trackColor = Color.Magenta,
                )
            }
        }
    }
}

@Composable
private fun InfoFooter(item: UiBook) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(bottomEnd = 8.dp, bottomStart = 8.dp))
            .background(MaterialTheme.colorScheme.tertiaryContainer)
            .padding(all = 4.dp)
    ) {
        Spacer(Modifier.height(4.dp))
        Text(
            text = item.title,
            fontSize = 17.sp,
            maxLines = 2,
            minLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onTertiary,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = item.author,
            color = MaterialTheme.colorScheme.onTertiary,
            minLines = 2,
            maxLines = 2,
        )

        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = item.category,
                maxLines = 1,
                color = MaterialTheme.colorScheme.onTertiary,
            )
        }
    }
}

@Composable
@Preview(
    showBackground = true,
    backgroundColor = 0xFFFFFFFF,
)
fun InfoFooterPreview() {
    val item = UiBook(
        id = "id",
        title = "Государство и Революция",
        author = "В. И. Ленин",
        description = "В. И. Ленин",
        image = "https://picsum.photos/300/300",
        downloadUrl = "link",
        fileNameWithExt = "1.pdf",
        category = "Свободное время",
        downloadState = UIBookState.DOWNLOADED,
        fileUri = null,
    )
    InfoFooter(item)
}

@Composable
@Preview(
    showBackground = true,
    backgroundColor = 0xFFFFFFFF,
)
fun BookItemPreview() {
    val actions = object : RootBooksActions {
        override fun onAction(action: RootBookActions) = Unit
    }
    val item = UiBook(
        id = "id",
        title = "Государство и Революция",
        author = "В. И. Ленин",
        description = "В. И. Ленин",
        image = "https://picsum.photos/300/300",
        downloadUrl = "link",
        fileNameWithExt = "1.pdf",
        category = "Свободное время",

        downloadState = UIBookState.DOWNLOADING,
        fileUri = null,
    )
    SVAPPTheme {
        BookItem(item, actions)
    }
}

