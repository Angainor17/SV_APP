package su.sv.quiz.presentation.detail

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.github.terrakok.modo.Screen
import com.github.terrakok.modo.ScreenKey
import com.github.terrakok.modo.generateScreenKey
import kotlinx.parcelize.Parcelize

/**
 * Экран прохождения квиза.
 *
 * @param quizId идентификатор квиза
 */
@Parcelize
class QuizDetailScreen(
    private val quizId: String,
    override val screenKey: ScreenKey = generateScreenKey(),
) : Screen {

    @Composable
    override fun Content(modifier: Modifier) {
        QuizDetailScreenContent(
            quizId = quizId,
            modifier = modifier,
        )
    }
}
