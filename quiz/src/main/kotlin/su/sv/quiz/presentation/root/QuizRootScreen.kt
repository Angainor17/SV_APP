package su.sv.quiz.presentation.root

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.github.terrakok.modo.Screen
import com.github.terrakok.modo.ScreenKey
import com.github.terrakok.modo.generateScreenKey
import kotlinx.parcelize.Parcelize

/**
 * Корневой экран квизов: «Новые квизы» + «Темы».
 */
@Parcelize
class QuizRootScreen(
    override val screenKey: ScreenKey = generateScreenKey(),
) : Screen {

    @Composable
    override fun Content(modifier: Modifier) {
        QuizRootScreenContent(modifier = modifier)
    }
}
