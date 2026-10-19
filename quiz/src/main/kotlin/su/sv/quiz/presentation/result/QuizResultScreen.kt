package su.sv.quiz.presentation.result

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.github.terrakok.modo.Screen
import com.github.terrakok.modo.ScreenKey
import com.github.terrakok.modo.generateScreenKey
import kotlinx.parcelize.Parcelize
import su.sv.quiz.domain.model.QuizResult

/**
 * Экран результата прохождения квиза.
 *
 * @param result результат прохождения
 */
@Parcelize
class QuizResultScreen(
    private val result: QuizResult,
    override val screenKey: ScreenKey = generateScreenKey(),
) : Screen {

    @Composable
    override fun Content(modifier: Modifier) {
        QuizResultScreenContent(
            result = result,
            modifier = modifier,
        )
    }
}
