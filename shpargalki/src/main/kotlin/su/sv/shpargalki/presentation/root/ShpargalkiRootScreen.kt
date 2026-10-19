package su.sv.shpargalki.presentation.root

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.github.terrakok.modo.Screen
import com.github.terrakok.modo.ScreenKey
import com.github.terrakok.modo.generateScreenKey
import kotlinx.parcelize.Parcelize

/**
 * Экран «Шпаргалки» — список тем с раскрывающимися подпунктами.
 */
@Parcelize
class ShpargalkiRootScreen(
    override val screenKey: ScreenKey = generateScreenKey(),
) : Screen {

    @Composable
    override fun Content(modifier: Modifier) {
        ShpargalkiRootScreenContent(modifier = modifier)
    }
}
