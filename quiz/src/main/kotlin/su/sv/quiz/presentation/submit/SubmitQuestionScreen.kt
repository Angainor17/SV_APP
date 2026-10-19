package su.sv.quiz.presentation.submit

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.github.terrakok.modo.Screen
import com.github.terrakok.modo.ScreenKey
import com.github.terrakok.modo.generateScreenKey
import kotlinx.parcelize.Parcelize

/**
 * Экран «Предложить вопрос».
 *
 * Заглушка: полная форма (тема, текст, варианты, пояснение) и отправка
 * через Tracer — на этапе 6 плана (см. ТЗ).
 */
@Parcelize
class SubmitQuestionScreen(
    override val screenKey: ScreenKey = generateScreenKey(),
) : Screen {

    @Composable
    override fun Content(modifier: Modifier) {
        SubmitQuestionScreenContent(modifier = modifier)
    }
}
