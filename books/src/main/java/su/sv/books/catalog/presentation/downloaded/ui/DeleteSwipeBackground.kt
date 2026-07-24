package su.sv.books.catalog.presentation.downloaded.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import su.sv.books.R
import su.sv.commonui.theme.LocalAppDimensions
import su.sv.commonui.theme.SVAPPThemeLightPreview

/**
 * Фон для свайпа удаления (красный с иконкой корзины)
 *
 * Использует функциональный цвет danger из темы.
 * Растягивается на всю высоту и ширину родителя.
 *
 * @param modifier модификатор
 */
@Composable
fun DeleteSwipeBackground(
    modifier: Modifier = Modifier,
) {
    val dimensions = LocalAppDimensions.current

    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(horizontal = dimensions.itemSpacingXLarge),
        contentAlignment = Alignment.CenterEnd
    ) {
        Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = stringResource(R.string.books_downloaded_delete_content_description),
            tint = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.size(dimensions.iconSizeLarge)
        )
    }
}

// ============================================================
// Previews
// ============================================================

@Preview(showBackground = true)
@Composable
private fun DeleteSwipeBackgroundPreview() {
    SVAPPThemeLightPreview {
        DeleteSwipeBackground(
            modifier = Modifier.fillMaxSize()
        )
    }
}
