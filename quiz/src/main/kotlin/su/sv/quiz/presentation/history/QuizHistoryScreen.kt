package su.sv.quiz.presentation.history

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.github.terrakok.modo.Screen
import com.github.terrakok.modo.ScreenKey
import com.github.terrakok.modo.generateScreenKey
import kotlinx.parcelize.Parcelize

/**
 * Экран «Мои квизы» — история прохождений.
 */
@Parcelize
class QuizHistoryScreen(
    override val screenKey: ScreenKey = generateScreenKey(),
) : Screen {

    @Composable
    override fun Content(modifier: Modifier) {
        QuizHistoryScreenContent(modifier = modifier)
    }
}
