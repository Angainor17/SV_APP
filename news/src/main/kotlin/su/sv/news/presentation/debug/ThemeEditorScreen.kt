package su.sv.news.presentation.debug

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.github.terrakok.modo.Screen
import com.github.terrakok.modo.ScreenKey
import com.github.terrakok.modo.generateScreenKey
import kotlinx.parcelize.Parcelize
import su.sv.news.presentation.debug.ui.ThemeEditorContent

/**
 * Экран редактирования темы (отладочный)
 *
 * Открывается при долгом нажатии на иконку темы в тулбаре новостей.
 */
@Parcelize
class ThemeEditorScreen(
    override val screenKey: ScreenKey = generateScreenKey()
) : Screen {

    @Composable
    override fun Content(modifier: Modifier) {
        ThemeEditorContent()
    }
}