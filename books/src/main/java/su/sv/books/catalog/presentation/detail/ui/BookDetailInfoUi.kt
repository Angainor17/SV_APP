package su.sv.books.catalog.presentation.detail.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import su.sv.books.R
import su.sv.books.catalog.presentation.detail.actions.DetailBookActions
import su.sv.books.catalog.presentation.detail.actions.DetailBooksActionsHandler
import su.sv.books.catalog.presentation.detail.model.UiBookDetailState
import su.sv.commonui.ui.LoadingButton
import su.sv.models.ui.book.UIBookState
import su.sv.models.ui.book.UiBook

@Composable
fun BookDetailInfoUi(
    state: UiBookDetailState.Content,
    actionsHandler: DetailBooksActionsHandler,
) {
    val uiBook = state.book

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
    ) {
        Box(modifier = Modifier.align(Alignment.CenterHorizontally)) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(uiBook.image)
                    .build(),
                placeholder = painterResource(R.drawable.ic_book_placeholder),
                contentDescription = stringResource(R.string.books_item_image_content_description),
                contentScale = ContentScale.FillWidth,
            )
        }
        Text(
            modifier = Modifier.padding(
                top = 8.dp,
                start = 12.dp,
                end = 12.dp,
            ),
            fontSize = MaterialTheme.typography.bodyLarge.fontSize,
            fontWeight = MaterialTheme.typography.bodyLarge.fontWeight,
            fontFamily = MaterialTheme.typography.bodyLarge.fontFamily,
            text = uiBook.title,
        )

        Text(
            modifier = Modifier.padding(
                start = 12.dp,
                end = 12.dp,
            ),
            fontSize = MaterialTheme.typography.bodyLarge.fontSize,
            text = uiBook.author,
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp),
        ) {
            LoadingButton(
                text = state.actionText,
                loading = state.isActionLoading,
                onClick = {
                    actionsHandler.onAction(DetailBookActions.OnActionClick(uiBook))
                }
            )
        }

        Text(
            modifier = Modifier.padding(
                top = 4.dp,
                start = 12.dp,
                bottom = 32.dp,
                end = 12.dp,
            ),
            fontSize = MaterialTheme.typography.bodyMedium.fontSize,
            text = uiBook.description,
        )
    }
}

@Preview
@Composable
fun BookDetailInfoUiPreview() {
    val book = UiBook(
        id = "id",
        title = "Государство и Революция",
        author = "В. И. Ленин",
        description = "Книга создана в период подготовки социалистической революции, когда " +
                "вопрос о государстве приобрёл для большевиков особую важность.",
        image = "https://picsum.photos/300/300",
        downloadUrl = "link",
        fileNameWithExt = "1.pdf",
        pagesCountFormatted = "323 стр.",
        dateFormatted = "25 февр. 2025",

        downloadState = UIBookState.DOWNLOADED,
        fileUri = null,
    )
    val state = UiBookDetailState.Content(
        book = book,
        isActionLoading = false,
        actionText = "Cкачать",
    )
    val actionsHandler = object : DetailBooksActionsHandler {
        override fun onAction(action: DetailBookActions) = Unit
    }
    BookDetailInfoUi(
        state = state,
        actionsHandler = actionsHandler,
    )
}
