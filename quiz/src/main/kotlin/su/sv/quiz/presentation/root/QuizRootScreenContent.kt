package su.sv.quiz.presentation.root

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.terrakok.modo.stack.LocalStackNavigation
import com.github.terrakok.modo.stack.back
import com.github.terrakok.modo.stack.forward
import su.sv.commonui.theme.LocalAppDimensions
import su.sv.commonui.ui.components.AppToolbarWithBack
import su.sv.commonui.ui.components.FullScreenError
import su.sv.commonui.ui.components.FullScreenLoading
import su.sv.quiz.R
import su.sv.quiz.domain.model.Quiz
import su.sv.quiz.domain.model.QuizTheme
import su.sv.quiz.presentation.detail.QuizDetailScreen
import su.sv.quiz.presentation.submit.SubmitQuestionScreen
import su.sv.quiz.presentation.theme.QuizThemeScreen
import su.sv.quiz.presentation.ui.QuizCard
import su.sv.quiz.testing.QuizTestTags

/**
 * Контент корневого экрана квизов.
 */
@Composable
fun QuizRootScreenContent(modifier: Modifier = Modifier) {
    val viewModel: QuizRootViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val stackNavigation = LocalStackNavigation.current

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            AppToolbarWithBack(
                title = stringResource(R.string.quiz_title),
                onBackClick = { stackNavigation.back() },
                actions = {
                    IconButton(onClick = { stackNavigation.forward(SubmitQuestionScreen()) }) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(R.string.quiz_propose_question),
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .testTag(QuizTestTags.ROOT),
        ) {
            when (val current = state) {
                is QuizRootState.Loading -> FullScreenLoading()

                is QuizRootState.Error -> FullScreenError(onRetry = viewModel::load)

                is QuizRootState.Content -> QuizRootContent(
                    themes = current.themes,
                    newQuizzes = current.newQuizzes,
                    onThemeClick = { theme ->
                        stackNavigation.forward(
                            QuizThemeScreen(themeId = theme.id, themeName = theme.name)
                        )
                    },
                    onQuizClick = { quiz ->
                        stackNavigation.forward(QuizDetailScreen(quizId = quiz.id))
                    },
                )
            }
        }
    }
}

/**
 * Списки «Новые квизы» и «Темы».
 */
@Composable
private fun QuizRootContent(
    themes: List<QuizTheme>,
    newQuizzes: List<Quiz>,
    onThemeClick: (QuizTheme) -> Unit,
    onQuizClick: (Quiz) -> Unit,
) {
    val dimensions = LocalAppDimensions.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = dimensions.screenPaddingHorizontal)
            .padding(vertical = dimensions.itemSpacingMedium),
    ) {
        if (newQuizzes.isNotEmpty()) {
            SectionHeader(title = stringResource(R.string.quiz_new_quizzes))
            newQuizzes.forEach { quiz ->
                QuizCard(
                    quiz = quiz,
                    isNew = true,
                    onClick = { onQuizClick(quiz) },
                    modifier = Modifier.padding(bottom = dimensions.itemSpacingMedium),
                )
            }
            Spacer(modifier = Modifier.height(dimensions.itemSpacingMedium))
        }

        SectionHeader(title = stringResource(R.string.quiz_themes))
        themes.forEach { theme ->
            ThemeRow(
                theme = theme,
                onClick = { onThemeClick(theme) },
            )
        }
    }
}

/**
 * Заголовок блока («Новые квизы», «Темы»).
 */
@Composable
private fun SectionHeader(title: String) {
    val dimensions = LocalAppDimensions.current

    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = dimensions.itemSpacingSmall),
    )
}

/**
 * Строка темы в списке «Темы».
 */
@Composable
private fun ThemeRow(
    theme: QuizTheme,
    onClick: () -> Unit,
) {
    val dimensions = LocalAppDimensions.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(
                horizontal = dimensions.listItemPaddingHorizontal,
                vertical = dimensions.listItemPaddingVertical,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = theme.name,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = pluralStringResource(R.plurals.quiz_count, theme.quizCount, theme.quizCount),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
