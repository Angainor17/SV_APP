package su.sv.news.presentation.root.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import com.github.terrakok.modo.stack.LocalStackNavigation
import com.github.terrakok.modo.stack.forward
import su.sv.featureflags.FeatureFlags
import su.sv.news.R
import su.sv.quiz.presentation.root.QuizRootScreen

/**
 * Кнопка квиза в тулбаре новостей.
 *
 * Доступность управляется флагом `quiz_enabled` через RuStore Remote Config
 * (см. [su.sv.featureflags.FeatureFlags]). По умолчанию выключено — кнопка скрыта.
 */
@Composable
internal fun QuizEntryAction() {
    val flags by FeatureFlags.flags.collectAsState()
    if (!flags.quizEnabled) return

    val stackNavigation = LocalStackNavigation.current
    IconButton(onClick = { stackNavigation.forward(QuizRootScreen()) }) {
        Icon(
            imageVector = Icons.Filled.Quiz,
            contentDescription = stringResource(R.string.news_open_quiz),
        )
    }
}
