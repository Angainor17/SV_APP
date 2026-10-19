package su.sv.main.continuereading.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.github.axet.bookreader.domain.LastReadBookInfo
import su.sv.main.R
import su.sv.main.res.BooksVector

/**
 * Snackbar "Продолжить чтение" с информацией о книге.
 *
 * @param bookInfo информация о последней прочитанной книге
 * @param onContinueClick callback при клике на "Продолжить"
 * @param onDismissClick callback при клике на крестик
 * @param modifier модификатор
 */
@Composable
fun ContinueReadingSnackbar(
    bookInfo: LastReadBookInfo,
    onContinueClick: () -> Unit,
    onDismissClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("continue_reading_root"),
        shape = RoundedCornerShape(0.dp), // Без скругления - на всю ширину
    ) {
        Row(
            modifier = Modifier
                .clickable { onContinueClick() }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Обложка книги
            BookCover(
                coverUrl = bookInfo.coverUrl,
                title = bookInfo.title,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .testTag("continue_reading_cover")
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Название и автор
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = bookInfo.title,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("continue_reading_title"),
                )
                val authors = bookInfo.authors
                if (!authors.isNullOrBlank()) {
                    Text(
                        text = authors,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("continue_reading_author"),
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Кнопка "Продолжить"
            TextButton(
                onClick = onContinueClick,
                modifier = Modifier.testTag("continue_reading_button"),
            ) {
                Text(stringResource(R.string.continue_reading_button))
            }

            // Кнопка закрытия
            IconButton(
                onClick = onDismissClick,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("continue_reading_dismiss")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.continue_reading_dismiss),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun BookCover(
    coverUrl: String?,
    title: String,
    modifier: Modifier = Modifier,
) {
    if (coverUrl != null) {
        AsyncImage(
            model = coverUrl,
            contentDescription = title,
            modifier = modifier,
            contentScale = ContentScale.Crop,
        )
    } else {
        // Placeholder если нет обложки
        Box(
            modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.BooksVector,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}