package su.sv.books.catalog.presentation.bookmarks.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import su.sv.books.R
import su.sv.books.catalog.presentation.bookmarks.model.UiBookWithNotes
import su.sv.commonui.theme.SVAPPTheme
import kotlin.math.abs

/**
 * Элемент книги с заметками в режиме отображения "по книгам"
 *
 * @param book Данные книги с заметками
 * @param onClick Callback при нажатии на книгу
 */
@Composable
fun BookWithNotesItem(
    book: UiBookWithNotes,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Обложка с placeholder
            BookCover(
                coverUrl = book.bookCoverUrl,
                modifier = Modifier
                    .size(60.dp, 90.dp)
                    .clip(RoundedCornerShape(8.dp))
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Информация о книге
            BookInfo(
                title = book.bookTitle,
                author = book.bookAuthor,
                notesCount = book.notesCount,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Обложка книги
 */
@Composable
fun BookCover(
    coverUrl: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    // Преобразуем путь в Uri если это локальный файл
    val imageModel = remember(coverUrl) {
        if (coverUrl.isNotBlank()) {
            // Если это локальный путь к файлу, преобразуем в Uri
            if (coverUrl.startsWith("/") || coverUrl.startsWith("file://")) {
                try {
                    java.io.File(coverUrl).toUri()
                } catch (e: Exception) {
                    coverUrl
                }
            } else {
                coverUrl
            }
        } else {
            null
        }
    }

    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(imageModel)
            .build(),
        placeholder = painterResource(R.drawable.ic_book_placeholder),
        error = painterResource(R.drawable.ic_book_placeholder),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
    )
}

/**
 * Информация о книге
 */
@Composable
private fun BookInfo(
    title: String,
    author: String,
    notesCount: Int,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        if (author.isNotBlank()) {
            Text(
                text = author,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = context.resources.getQuantityString(
                R.plurals.bookmarks_notes_count,
                abs(notesCount),
                notesCount
            ),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

//region Previews

@Preview(showBackground = true)
@Composable
private fun BookWithNotesItemPreview() {
    SVAPPTheme {
        BookWithNotesItem(
            book = UiBookWithNotes(
                bookId = "1",
                bookTitle = "Название книги",
                bookAuthor = "Автор Книги",
                bookCoverUrl = "",
                notesCount = 5
            ),
            onClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BookWithNotesItemLongTitlePreview() {
    SVAPPTheme {
        BookWithNotesItem(
            book = UiBookWithNotes(
                bookId = "1",
                bookTitle = "Очень длинное название книги, которое должно обрезаться и показать многоточие в конце",
                bookAuthor = "Автор с очень длинным именем",
                bookCoverUrl = "",
                notesCount = 12
            ),
            onClick = {}
        )
    }
}

//endregion
