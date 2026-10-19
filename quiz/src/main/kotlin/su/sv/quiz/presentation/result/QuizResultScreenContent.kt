package su.sv.quiz.presentation.result

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.github.terrakok.modo.stack.LocalStackNavigation
import com.github.terrakok.modo.stack.back
import com.github.terrakok.modo.stack.backTo
import com.github.terrakok.modo.stack.replace
import su.sv.commonui.theme.LocalAppDimensions
import su.sv.commonui.ui.components.AppButton
import su.sv.commonui.ui.components.AppToolbarWithBack
import su.sv.commonui.ui.components.ButtonStyle
import su.sv.quiz.R
import su.sv.quiz.domain.model.QuizResult
import su.sv.quiz.presentation.detail.QuizDetailScreen
import su.sv.quiz.presentation.root.QuizRootScreen
import su.sv.quiz.testing.QuizTestTags

/**
 * Контент экрана результата прохождения квиза.
 */
@Composable
fun QuizResultScreenContent(
    result: QuizResult,
    modifier: Modifier = Modifier,
) {
    val dimensions = LocalAppDimensions.current
    val stackNavigation = LocalStackNavigation.current

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            AppToolbarWithBack(
                title = stringResource(R.string.quiz_result_title),
                onBackClick = { stackNavigation.back() },
            )
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .testTag(QuizTestTags.RESULT),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimensions.screenPaddingHorizontal),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "🎉",
                    style = MaterialTheme.typography.displayMedium,
                )

                Spacer(modifier = Modifier.height(dimensions.itemSpacingMedium))

                Text(
                    text = stringResource(R.string.quiz_result_title),
                    style = MaterialTheme.typography.headlineSmall,
                )

                Spacer(modifier = Modifier.height(dimensions.itemSpacingLarge))

                Text(
                    text = stringResource(
                        R.string.quiz_result_score,
                        result.correctAnswers,
                        result.totalQuestions,
                    ),
                    style = MaterialTheme.typography.displaySmall,
                )

                Spacer(modifier = Modifier.height(dimensions.itemSpacingSmall))

                Text(
                    text = stringResource(R.string.quiz_result_percentage, result.percentage),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(dimensions.itemSpacingXLarge))

                AppButton(
                    text = stringResource(R.string.quiz_retry),
                    onClick = {
                        stackNavigation.replace(QuizDetailScreen(quizId = result.quizId))
                    },
                    style = ButtonStyle.FILLED,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(dimensions.itemSpacingMedium))

                AppButton(
                    text = stringResource(R.string.quiz_back_to_list),
                    onClick = { stackNavigation.backTo<QuizRootScreen>() },
                    style = ButtonStyle.OUTLINED,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
