package su.sv.books.catalog.presentation.downloaded.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import su.sv.books.R
import su.sv.commonui.theme.SVAPPTheme

/**
 * Фон для свайпа удаления (бледно-красный с иконкой корзины)
 * Растягивается на всю высоту и ширину родителя
 */
@Composable
fun DeleteSwipeBackground(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
        Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = stringResource(R.string.books_downloaded_delete_content_description),
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(28.dp)
        )
    }
}

//region Previews

@Preview(showBackground = true)
@Composable
private fun DeleteSwipeBackgroundPreview() {
    SVAPPTheme {
        DeleteSwipeBackground(
            modifier = Modifier.fillMaxSize()
        )
    }
}

//endregion
