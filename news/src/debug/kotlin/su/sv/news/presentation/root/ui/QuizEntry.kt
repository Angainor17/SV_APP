package su.sv.news.presentation.root.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.github.terrakok.modo.stack.LocalStackNavigation
import com.github.terrakok.modo.stack.forward
import su.sv.news.R
import su.sv.quiz.presentation.root.QuizRootScreen

/**
 * Кнопка квиза в тулбаре новостей.
 *
 * Компилируется только в debug-сборке (в release — no-op из `src/release`).
 * Модуль `:quiz` подключён через `debugImplementation`.
 */
@Composable
internal fun QuizEntryAction() {
    val stackNavigation = LocalStackNavigation.current
    IconButton(onClick = { stackNavigation.forward(QuizRootScreen()) }) {
        Icon(
            imageVector = Icons.Filled.Quiz,
            contentDescription = stringResource(R.string.news_open_quiz),
        )
    }
}
