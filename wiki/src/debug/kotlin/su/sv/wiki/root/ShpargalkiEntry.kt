package su.sv.wiki.root

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.github.terrakok.modo.stack.LocalStackNavigation
import com.github.terrakok.modo.stack.forward
import su.sv.commonui.theme.LocalAppDimensions
import su.sv.shpargalki.presentation.root.ShpargalkiRootScreen
import su.sv.wiki.R
import su.sv.wiki.testing.WikiTestTags

/**
 * Кнопка «Шпаргалки» в строке поиска.
 *
 * Компилируется только в debug-сборке (в release — no-op из `src/release`).
 * Модуль `:shpargalki` подключён через `debugImplementation`.
 */
@Composable
internal fun ShpargalkiEntryAction() {
    val focusManager = LocalFocusManager.current
    val stackNavigation = LocalStackNavigation.current
    val dimensions = LocalAppDimensions.current
    IconButton(
        onClick = {
            focusManager.clearFocus()
            stackNavigation.forward(ShpargalkiRootScreen())
        },
        modifier = Modifier
            .padding(end = dimensions.screenPaddingHorizontal / 2)
            .testTag(WikiTestTags.SHPARGALKI_BUTTON),
    ) {
        Icon(
            imageVector = Icons.Outlined.AutoStories,
            contentDescription = stringResource(R.string.wiki_shpargalki),
        )
    }
}
