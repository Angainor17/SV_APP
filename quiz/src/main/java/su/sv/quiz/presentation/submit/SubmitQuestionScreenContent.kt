package su.sv.quiz.presentation.submit

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.github.terrakok.modo.stack.LocalStackNavigation
import com.github.terrakok.modo.stack.back
import su.sv.commonui.ui.components.AppToolbarWithBack
import su.sv.commonui.ui.components.FullScreenEmpty
import su.sv.quiz.R
import su.sv.quiz.testing.QuizTestTags

/**
 * Контент экрана «Предложить вопрос» (заглушка).
 */
@Composable
fun SubmitQuestionScreenContent(modifier: Modifier = Modifier) {
    val stackNavigation = LocalStackNavigation.current

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            AppToolbarWithBack(
                title = stringResource(R.string.quiz_propose_question),
                onBackClick = { stackNavigation.back() },
            )
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .testTag(QuizTestTags.SUBMIT),
        ) {
            FullScreenEmpty(title = stringResource(R.string.quiz_submit_stub))
        }
    }
}
