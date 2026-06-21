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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import su.sv.books.R
import su.sv.books.catalog.presentation.downloaded.model.UiDownloadedBook
import su.sv.commonui.theme.SVAPPTheme

/**
 * Карточка скачанной книги для отображения в списке
 */
@Composable
fun DownloadedBookItem(
    book: UiDownloadedBook,
    onReadClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.primaryContainer) // Непрозрачный бледно-голубой
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Обложка книги
        BookCover(
            coverUrl = book.coverUrl,
            modifier = Modifier.size(width = 80.dp, height = 120.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Информация о книге
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Название без ограничения по строкам
            Text(
                text = book.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Text(
                text = book.author,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Text(
                text = book.category,
                fontSize = 12.sp,
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
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Кнопка "Читать"
            Button(
                onClick = onReadClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.books_downloaded_read_button))
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
        modifier = modifier.clip(RoundedCornerShape(8.dp)),
        model = ImageRequest.Builder(LocalContext.current)
            .data(coverUrl)
            .build(),
        contentDescription = stringResource(R.string.books_item_image_content_description),
        contentScale = ContentScale.Crop,
    )
}

//region Previews

@Preview(showBackground = true)
@Composable
private fun DownloadedBookItemPreview() {
    SVAPPTheme {
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

//endregion
