package su.sv.books.catalog.presentation.downloaded.ui

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import su.sv.books.R
import su.sv.books.catalog.presentation.downloaded.model.UiDownloadedBook
import su.sv.commonui.theme.LocalAppDimensions
import su.sv.commonui.theme.SVAPPThemeLightPreview

/**
 * Карточка скачанной книги для отображения в списке
 *
 * @param book данные книги
 * @param onReadClick обработчик клика на кнопку "Читать"
 * @param modifier модификатор
 */
@Composable
fun DownloadedBookItem(
    book: UiDownloadedBook,
    onReadClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimensions = LocalAppDimensions.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(
                horizontal = dimensions.itemSpacingMedium,
                vertical = dimensions.itemSpacingMedium
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Обложка книги
        BookCover(
            coverUrl = book.coverUrl,
            modifier = Modifier.size(width = 80.dp, height = 120.dp)
        )

        Spacer(modifier = Modifier.width(dimensions.itemSpacingMedium))

        // Информация о книге
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(dimensions.itemSpacingSmall)
        ) {
            // Название
            Text(
                text = book.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Text(
                text = book.author,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Text(
                text = book.category,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // Прогресс чтения
            if (book.totalPages > 0) {
                Text(
                    text = stringResource(
                        R.string.books_downloaded_page_progress,
                        book.currentPage,
                        book.totalPages
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Spacer(modifier = Modifier.height(dimensions.itemSpacingSmall))

            // Кнопка "Читать"
            OutlinedButton(
                onClick = onReadClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.books_downloaded_read_button)
                )
            }
        }
    }
}

@Composable
private fun BookCover(
    coverUrl: String,
    modifier: Modifier = Modifier,
) {
    AsyncImage(
        modifier = modifier.clip(MaterialTheme.shapes.small),
        model = ImageRequest.Builder(LocalContext.current)
            .data(coverUrl)
            .build(),
        contentDescription = stringResource(R.string.books_item_image_content_description),
        contentScale = ContentScale.Crop,
    )
}

// ============================================================
// Previews
// ============================================================

@Preview(showBackground = true)
@Composable
private fun DownloadedBookItemPreview() {
    SVAPPThemeLightPreview {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DownloadedBookItem(
                book = UiDownloadedBook(
                    id = "1",
                    title = "Краткая история будущего. Научный социализм в популярном изложении",
                    author = "Удовиченко М. С.",
                    description = "Описание книги о будущем человечества",
                    category = "Свободное Время",
                    coverUrl = "https://picsum.photos/200/300",
                    fileUri = Uri.parse("file:///test.pdf"),
                    currentPage = 15,
                    totalPages = 120,
                ),
                onReadClick = {},
            )
            DownloadedBookItem(
                book = UiDownloadedBook(
                    id = "2",
                    title = "Государство и революция",
                    author = "Ленин В. И.",
                    description = "Классический труд о государстве",
                    category = "Свободное Время",
                    coverUrl = "https://picsum.photos/200/300",
                    fileUri = Uri.parse("file:///test.pdf"),
                    currentPage = 0,
                    totalPages = 0,
                ),
                onReadClick = {},
            )
        }
    }
}
