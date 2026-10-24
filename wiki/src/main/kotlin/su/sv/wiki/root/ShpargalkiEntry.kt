package su.sv.wiki.root

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.github.terrakok.modo.stack.LocalStackNavigation
import com.github.terrakok.modo.stack.forward
import su.sv.commonui.theme.LocalAppDimensions
import su.sv.featureflags.FeatureFlags
import su.sv.shpargalki.presentation.root.ShpargalkiRootScreen
import su.sv.wiki.R
import su.sv.wiki.testing.WikiTestTags

/**
 * Кнопка «Шпаргалки» в строке поиска.
 *
 * Доступность управляется флагом `shpargalki_enabled` через RuStore Remote Config
 * (см. [su.sv.featureflags.FeatureFlags]). По умолчанию выключено — кнопка скрыта.
 */
@Composable
internal fun ShpargalkiEntryAction() {
    val flags by FeatureFlags.flags.collectAsState()
    if (!flags.shpargalkiEnabled) return

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
