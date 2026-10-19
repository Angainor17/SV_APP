package su.sv.quiz.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.github.terrakok.modo.Screen
import com.github.terrakok.modo.ScreenKey
import com.github.terrakok.modo.generateScreenKey
import kotlinx.parcelize.Parcelize

/**
 * Экран списка квизов выбранной темы.
 *
 * @param themeId идентификатор темы
 * @param themeName название темы (для тулбара)
 */
@Parcelize
class QuizThemeScreen(
    private val themeId: String,
    private val themeName: String,
    override val screenKey: ScreenKey = generateScreenKey(),
) : Screen {

    @Composable
    override fun Content(modifier: Modifier) {
        QuizThemeScreenContent(
            themeId = themeId,
            themeName = themeName,
            modifier = modifier,
        )
    }
}
