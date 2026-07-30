package su.sv.books.catalog.presentation.detail.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import su.sv.books.R
import su.sv.books.catalog.presentation.detail.actions.DetailBookActions
import su.sv.books.catalog.presentation.detail.actions.DetailBooksActionsHandler
import su.sv.books.catalog.presentation.detail.model.UiBookDetailState
import su.sv.commonui.theme.LocalDeviceFormFactor
import su.sv.commonui.ui.LoadingButton
import su.sv.models.ui.book.UIBookState
import su.sv.models.ui.book.UiBook

/**
 * Адаптивный UI деталей книги
 */
@Composable
fun BookDetailInfoUi(
    state: UiBookDetailState.Content,
    actionsHandler: DetailBooksActionsHandler,
) {
    val formFactor = LocalDeviceFormFactor.current

    // На планшетах (Expanded) в горизонтальном режиме используем двухколоночный layout
    if (formFactor.isExpanded()) {
        BookDetailInfoUiTablet(
            state = state,
            actionsHandler = actionsHandler,
        )
    } else {
        // На телефонах - текущее поведение
        BookDetailInfoUiCompact(
            state = state,
            actionsHandler = actionsHandler,
        )
    }
}

/**
 * Компактный UI для телефонов (вертикальный layout)
 */
@Composable
fun BookDetailInfoUiCompact(
    state: UiBookDetailState.Content,
    actionsHandler: DetailBooksActionsHandler,
) {
    val uiBook = state.book
    SelectionContainer {
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
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = MaterialTheme.typography.bodyLarge.fontFamily,
                text = uiBook.title,
            )

            Text(
                modifier = Modifier.padding(
                    start = 12.dp,
                    end = 12.dp,
                ),
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = MaterialTheme.typography.bodyLarge.fontFamily,
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
}

/**
 * UI для планшетов (горизонтальный layout)
 * Картинка слева, текст справа, кнопка под картинкой
 */
@Composable
fun BookDetailInfoUiTablet(
    state: UiBookDetailState.Content,
    actionsHandler: DetailBooksActionsHandler,
) {
    val uiBook = state.book

    SelectionContainer {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Левая колонка: картинка + кнопка
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Картинка книги (большая для планшета - 1.5x от базового размера)
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(uiBook.image)
                        .build(),
                    placeholder = painterResource(R.drawable.ic_book_placeholder),
                    contentDescription = stringResource(R.string.books_item_image_content_description),
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier
                        .width(300.dp)
                        .height(420.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Кнопка под картинкой (шириной с картинку)
                Box(modifier = Modifier.width(300.dp)) {
                    LoadingButton(
                        text = state.actionText,
                        loading = state.isActionLoading,
                        onClick = {
                            actionsHandler.onAction(DetailBookActions.OnActionClick(uiBook))
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.width(24.dp))

            // Правая колонка: название, автор, описание
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = MaterialTheme.typography.bodyLarge.fontFamily,
                    text = uiBook.title,
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = MaterialTheme.typography.bodyLarge.fontFamily,
                    text = uiBook.author,
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    fontSize = MaterialTheme.typography.bodyLarge.fontSize,
                    text = uiBook.description,
                )
            }
        }
    }
}

// ============================================
// Preview
// ============================================

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
        category = "СВремя",

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
    BookDetailInfoUiCompact(
        state = state,
        actionsHandler = actionsHandler,
    )
}

@Preview(device = "spec:width=1280dp,height=800dp,dpi=240")
@Composable
fun BookDetailInfoUiTabletPreview() {
    val book = UiBook(
        id = "id",
        title = "Государство и Революция",
        author = "В. И. Ленин",
        description = "Книга создана в период подготовки социалистической революции, когда " +
                "вопрос о государстве приобрёл для большевиков особую важность. В ней Ленин " +
                "анализирует учение Маркса и Энгельса о государстве, рассматривает опыт " +
                "русских революций 1905 и 1917 годов.",
        image = "https://picsum.photos/300/300",
        downloadUrl = "link",
        fileNameWithExt = "1.pdf",
        category = "СВремя",

        downloadState = UIBookState.DOWNLOADED,
        fileUri = null,
    )
    val state = UiBookDetailState.Content(
        book = book,
        isActionLoading = false,
        actionText = "Читать",
    )
    val actionsHandler = object : DetailBooksActionsHandler {
        override fun onAction(action: DetailBookActions) = Unit
    }
    BookDetailInfoUiTablet(
        state = state,
        actionsHandler = actionsHandler,
    )
}