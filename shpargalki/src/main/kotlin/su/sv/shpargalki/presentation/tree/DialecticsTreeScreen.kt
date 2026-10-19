package su.sv.shpargalki.presentation.tree

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.github.terrakok.modo.Screen
import com.github.terrakok.modo.ScreenKey
import com.github.terrakok.modo.generateScreenKey
import kotlinx.parcelize.Parcelize

/**
 * Экран вертикального древа понятий для одного раздела диалектики.
 *
 * @param sectionKey ключ раздела (bytie / suschnost / ponyatie)
 */
@Parcelize
class DialecticsTreeScreen(
    private val sectionKey: String,
    override val screenKey: ScreenKey = generateScreenKey(),
) : Screen {

    @Composable
    override fun Content(modifier: Modifier) {
        DialecticsTreeScreenContent(
            sectionKey = sectionKey,
            modifier = modifier,
        )
    }
}
