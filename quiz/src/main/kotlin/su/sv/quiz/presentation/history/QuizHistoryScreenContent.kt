package su.sv.quiz.presentation.history

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.terrakok.modo.stack.LocalStackNavigation
import com.github.terrakok.modo.stack.back
import su.sv.commonui.theme.LocalAppDimensions
import su.sv.commonui.ui.components.AppToolbarWithBack
import su.sv.commonui.ui.components.FullScreenEmpty
import su.sv.commonui.ui.components.FullScreenError
import su.sv.commonui.ui.components.FullScreenLoading
import su.sv.quiz.R
import su.sv.quiz.domain.model.QuizResult
import su.sv.quiz.testing.QuizTestTags
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Контент экрана «Мои квизы».
 */
@Composable
fun QuizHistoryScreenContent(modifier: Modifier = Modifier) {
    val viewModel: QuizHistoryViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val stackNavigation = LocalStackNavigation.current

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            AppToolbarWithBack(
                title = stringResource(R.string.quiz_history_title),
                onBackClick = { stackNavigation.back() },
            )
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .testTag(QuizTestTags.HISTORY),
        ) {
            when (val current = state) {
                is QuizHistoryState.Loading -> FullScreenLoading()

                is QuizHistoryState.Error -> FullScreenError(onRetry = viewModel::load)

                is QuizHistoryState.Content -> {
                    if (current.results.isEmpty()) {
                        FullScreenEmpty(
                            title = stringResource(R.string.quiz_history_empty),
                            icon = Icons.Rounded.History,
                        )
                    } else {
                        HistoryList(results = current.results)
                    }
                }
            }
        }
    }
}

/**
 * Список пройденных квизов.
 */
@Composable
private fun HistoryList(results: List<QuizResult>) {
    val dimensions = LocalAppDimensions.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = dimensions.screenPaddingHorizontal)
            .padding(vertical = dimensions.itemSpacingMedium),
    ) {
        results.forEach { result ->
            HistoryCard(
                result = result,
                modifier = Modifier.padding(bottom = dimensions.itemSpacingMedium),
            )
        }
    }
}

/**
 * Карточка результата в истории.
 */
@Composable
private fun HistoryCard(
    result: QuizResult,
    modifier: Modifier = Modifier,
) {
    val dimensions = LocalAppDimensions.current

    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(dimensions.cardPaddingInner)) {
            Text(
                text = result.quizTitle,
                style = MaterialTheme.typography.titleMedium,
            )

            Spacer(modifier = Modifier.height(dimensions.itemSpacingSmall))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(
                        R.string.quiz_result_score,
                        result.correctAnswers,
                        result.totalQuestions,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = stringResource(R.string.quiz_result_percentage, result.percentage),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Spacer(modifier = Modifier.height(dimensions.itemSpacingSmall))

            Text(
                text = formatDate(result.completedAtEpochMillis),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun formatDate(epochMillis: Long): String =
    SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(epochMillis))
