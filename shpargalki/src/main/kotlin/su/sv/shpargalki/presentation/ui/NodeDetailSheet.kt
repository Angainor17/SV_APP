package su.sv.shpargalki.presentation.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.sv.commonui.theme.LocalAppDimensions
import su.sv.shpargalki.R
import su.sv.shpargalki.domain.model.DialecticsNode
import su.sv.shpargalki.testing.ShpargalkiTestTags

/**
 * Шторка с полным описанием понятия. Открывается поверх древа,
 * свайпом вверх растягивается до полного экрана.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NodeDetailSheet(
    node: DialecticsNode?,
    onDismiss: () -> Unit,
) {
    if (node == null) return

    val dimensions = LocalAppDimensions.current
    // skipPartiallyExpanded = false — разрешаем частичную высоту и свайп-растягивание
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.testTag(ShpargalkiTestTags.NODE_SHEET),
    ) {
        // SelectionContainer — текст можно выделять и копировать
        SelectionContainer {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = dimensions.screenPaddingHorizontal)
                    .padding(bottom = dimensions.itemSpacingXXLarge),
            ) {
                Text(
                    text = node.term,
                    style = MaterialTheme.typography.headlineSmall,
                )

                if (node.german.isNotBlank()) {
                    Spacer(modifier = Modifier.height(dimensions.itemSpacingMedium))
                    Text(
                        text = node.german,
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = GothicFontFamily,
                    )
                }

                if (node.pair.isNotBlank()) {
                    SheetSection(
                        label = stringResource(R.string.shpargalki_field_pair),
                        text = node.pair,
                    )
                }

                if (node.derivation.isNotBlank()) {
                    SheetSection(
                        label = stringResource(R.string.shpargalki_field_derivation),
                        text = node.derivation,
                    )
                }

                SheetSection(
                    label = stringResource(R.string.shpargalki_field_popov),
                    text = node.popov,
                )
                SheetSection(
                    label = stringResource(R.string.shpargalki_field_hegel),
                    text = node.hegel,
                )
                SheetSection(
                    label = stringResource(R.string.shpargalki_field_svremya),
                    text = node.svremya,
                )
            }
        }
    }
}

/**
 * Блок «заголовок + текст» внутри шторки.
 */
@Composable
private fun SheetSection(
    label: String,
    text: String,
) {
    val dimensions = LocalAppDimensions.current

    Column(modifier = Modifier.padding(top = dimensions.itemSpacingMedium)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(dimensions.itemSpacingSmall))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
