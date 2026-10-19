package su.sv.quiz.presentation.theme

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.terrakok.modo.stack.LocalStackNavigation
import com.github.terrakok.modo.stack.back
import com.github.terrakok.modo.stack.forward
import su.sv.commonui.theme.LocalAppDimensions
import su.sv.commonui.ui.components.AppToolbarWithBack
import su.sv.commonui.ui.components.FullScreenError
import su.sv.commonui.ui.components.FullScreenLoading
import su.sv.quiz.presentation.detail.QuizDetailScreen
import su.sv.quiz.presentation.ui.QuizCard
import su.sv.quiz.testing.QuizTestTags

/**
 * Контент экрана списка квизов по теме.
 */
@Composable
fun QuizThemeScreenContent(
    themeId: String,
    themeName: String,
    modifier: Modifier = Modifier,
) {
    val viewModel: QuizThemeViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val stackNavigation = LocalStackNavigation.current
    val dimensions = LocalAppDimensions.current

    LaunchedEffect(themeId) {
        viewModel.load(themeId)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            AppToolbarWithBack(
                title = themeName,
                onBackClick = { stackNavigation.back() },
            )
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .testTag(QuizTestTags.THEME_LIST),
        ) {
            when (val current = state) {
                is QuizThemeState.Loading -> FullScreenLoading()

                is QuizThemeState.Error -> FullScreenError(
                    onRetry = { viewModel.load(themeId) },
                )

                is QuizThemeState.Content -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = dimensions.screenPaddingHorizontal)
                        .padding(vertical = dimensions.itemSpacingMedium),
                ) {
                    current.quizzes.forEach { quiz ->
                        QuizCard(
                            quiz = quiz,
                            isNew = false,
                            onClick = {
                                stackNavigation.forward(QuizDetailScreen(quizId = quiz.id))
                            },
                            modifier = Modifier.padding(bottom = dimensions.itemSpacingMedium),
                        )
                    }
                }
            }
        }
    }
}
