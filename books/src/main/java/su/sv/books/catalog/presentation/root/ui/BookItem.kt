package su.sv.books.catalog.presentation.root.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import su.sv.books.R
import su.sv.books.catalog.presentation.root.viewmodel.actions.RootBookActions
import su.sv.books.catalog.presentation.root.viewmodel.actions.RootBooksActions
import su.sv.commonui.theme.DarkSurfaceContainerHighest
import su.sv.commonui.theme.LocalAppDimensions
import su.sv.commonui.theme.SVAPPThemeLightPreview
import su.sv.commonui.ui.components.AppLoadingIndicator
import su.sv.models.ui.book.UIBookState
import su.sv.models.ui.book.UiBook

/**
 * Карточка книги в каталоге
 *
 * @param item данные книги
 * @param actions обработчик действий
 */
@Composable
fun BookItem(item: UiBook, actions: RootBooksActions) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple()
            ) {
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
    val dimensions = LocalAppDimensions.current

    // Более тёмный фон для кнопки скачивания, чтобы быть видимым на светлой обложке
    val containerColor = DarkSurfaceContainerHighest

    Button(
        onClick = {
            when (item.downloadState) {
                UIBookState.DOWNLOADED -> {
                    actions.onAction(RootBookActions.OnOpenDownloadedBook(item))
                }

                UIBookState.AVAILABLE_TO_DOWNLOAD -> {
                    actions.onAction(RootBookActions.OnDownloadBookClick(item))
                }

                UIBookState.DOWNLOADING -> {
                    // Ничего не делаем при клике во время загрузки
                }
            }
        },
        contentPadding = PaddingValues(all = 3.dp),
        shape = CircleShape,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = containerColor,
        ),
        modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(top = dimensions.itemSpacingMedium, end = dimensions.itemSpacingMedium)
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
                AppLoadingIndicator(
                    size = 26.dp,
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun InfoFooter(item: UiBook) {
    val dimensions = LocalAppDimensions.current

    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.tertiaryContainer)
            .padding(all = dimensions.cardPaddingInner)
    ) {
        Spacer(Modifier.height(dimensions.itemSpacingSmall))

        Text(
            text = item.title,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 2,
            minLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onTertiary,
        )

        Spacer(Modifier.width(dimensions.itemSpacingSmall))

        Text(
            text = item.author,
            style = MaterialTheme.typography.bodyMedium,
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
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                color = MaterialTheme.colorScheme.onTertiary,
            )
        }
    }
}

// ============================================================
// Previews
// ============================================================

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
    SVAPPThemeLightPreview {
        InfoFooter(item)
    }
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
    SVAPPThemeLightPreview {
        BookItem(item, actions)
    }
}
