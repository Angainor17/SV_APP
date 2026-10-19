package su.sv.books.catalog.presentation.bookmarks.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import su.sv.books.catalog.presentation.bookmarks.model.UiBookWithNotes
import su.sv.commonui.theme.SVAPPTheme

/**
 * Компактная карточка книги для отображения в режиме заметок одной книги
 *
 * @param book Данные книги с заметками
 */
@Composable
fun CompactBookCard(
    book: UiBookWithNotes,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Обложка с placeholder
            BookCover(
                coverUrl = book.bookCoverUrl,
                modifier = Modifier
                    .size(40.dp, 60.dp)
                    .clip(RoundedCornerShape(4.dp))
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Название и автор
            CompactBookInfo(
                title = book.bookTitle,
                author = book.bookAuthor,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Компактная информация о книге
 */
@Composable
private fun CompactBookInfo(
    title: String,
    author: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        if (author.isNotBlank()) {
            Text(
                text = author,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

//region Previews

@Preview(showBackground = true)
@Composable
private fun CompactBookCardPreview() {
    SVAPPTheme {
        CompactBookCard(
            book = UiBookWithNotes(
                bookId = "1",
                bookTitle = "Название книги",
                bookAuthor = "Автор",
                bookCoverUrl = "",
                notesCount = 5
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CompactBookCardLongTitlePreview() {
    SVAPPTheme {
        CompactBookCard(
            book = UiBookWithNotes(
                bookId = "1",
                bookTitle = "Очень длинное название книги, которое должно обрезаться",
                bookAuthor = "Автор с длинным именем",
                bookCoverUrl = "",
                notesCount = 10
            )
        )
    }
}

//endregion
