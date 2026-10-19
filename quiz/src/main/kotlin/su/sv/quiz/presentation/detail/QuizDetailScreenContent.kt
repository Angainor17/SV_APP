package su.sv.quiz.presentation.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.terrakok.modo.stack.LocalStackNavigation
import com.github.terrakok.modo.stack.back
import com.github.terrakok.modo.stack.replace
import su.sv.commonui.theme.LocalAppDimensions
import su.sv.commonui.ui.components.AppButton
import su.sv.commonui.ui.components.AppToolbarWithBack
import su.sv.commonui.ui.components.ButtonStyle
import su.sv.commonui.ui.components.FullScreenError
import su.sv.commonui.ui.components.FullScreenLoading
import su.sv.quiz.R
import su.sv.quiz.domain.model.QuizAnswer
import su.sv.quiz.domain.model.QuizQuestion
import su.sv.quiz.presentation.result.QuizResultScreen
import su.sv.quiz.testing.QuizTestTags

/** Визуальное состояние варианта ответа. */
private enum class AnswerVisualState {
    DEFAULT,
    SELECTED_CORRECT,
    SELECTED_WRONG,
    REVEALED_CORRECT,
}

private val CorrectColor = Color(0xFF2E7D32)
private val WrongColor = Color(0xFFC62828)

/**
 * Контент экрана прохождения квиза.
 */
@Composable
fun QuizDetailScreenContent(
    quizId: String,
    modifier: Modifier = Modifier,
) {
    val viewModel: QuizDetailViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val stackNavigation = LocalStackNavigation.current

    LaunchedEffect(quizId) {
        viewModel.load(quizId)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            AppToolbarWithBack(
                title = state.quiz?.title ?: stringResource(R.string.quiz_title),
                onBackClick = { stackNavigation.back() },
            )
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .testTag(QuizTestTags.DETAIL),
        ) {
            when {
                state.loading -> FullScreenLoading()

                state.error != null -> FullScreenError(
                    onRetry = { viewModel.load(quizId) },
                    message = state.error,
                )

                state.quiz != null -> QuizQuestionContent(
                    state = state,
                    onSelectAnswer = viewModel::selectAnswer,
                    onNavigate = viewModel::goTo,
                    onFinish = {
                        val result = viewModel.computeResult()
                        if (result != null) {
                            viewModel.saveResult(result)
                            stackNavigation.replace(QuizResultScreen(result = result))
                        }
                    },
                )
            }
        }
    }
}

/**
 * Содержимое текущего вопроса: текст, варианты, пояснение, навигация.
 */
@Composable
private fun QuizQuestionContent(
    state: QuizDetailState,
    onSelectAnswer: (String) -> Unit,
    onNavigate: (Int) -> Unit,
    onFinish: () -> Unit,
) {
    val dimensions = LocalAppDimensions.current
    val quiz = state.quiz ?: return
    val question = state.currentQuestion ?: return
    val selectedId = state.answers[question.id]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = dimensions.screenPaddingHorizontal)
            .padding(vertical = dimensions.itemSpacingMedium),
    ) {
        Text(
            text = stringResource(
                R.string.quiz_question_counter,
                state.currentIndex + 1,
                quiz.questions.size,
            ),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(dimensions.itemSpacingMedium))

        Text(
            text = question.text,
            style = MaterialTheme.typography.titleMedium,
        )

        Spacer(modifier = Modifier.height(dimensions.itemSpacingLarge))

        question.answers.forEachIndexed { index, answer ->
            val visual = answerVisualState(answer, selectedId)
            AnswerOption(
                answer = answer,
                label = (('A'.code + index).toChar()).toString(),
                state = visual,
                enabled = selectedId == null,
                onClick = { onSelectAnswer(answer.id) },
                modifier = Modifier.padding(bottom = dimensions.itemSpacingSmall),
            )
        }

        if (selectedId != null) {
            Spacer(modifier = Modifier.height(dimensions.itemSpacingMedium))
            ExplanationBlock(
                isCorrect = question.answers.firstOrNull { it.id == selectedId }?.isCorrect == true,
                correctAnswerText = question.answers.firstOrNull { it.isCorrect }?.text,
                explanation = question.explanation,
            )
        }

        Spacer(modifier = Modifier.height(dimensions.itemSpacingXLarge))

        NavigationRow(
            canGoBack = state.currentIndex > 0,
            isLastQuestion = state.currentIndex == quiz.questions.lastIndex,
            isAllAnswered = state.isAllAnswered,
            unansweredCount = quiz.questions.size - state.answers.size,
            onBack = { onNavigate(state.currentIndex - 1) },
            onNext = { onNavigate(state.currentIndex + 1) },
            onFinish = onFinish,
        )

        Spacer(modifier = Modifier.height(dimensions.itemSpacingLarge))

        ProgressDots(
            questions = quiz.questions,
            answers = state.answers,
            currentIndex = state.currentIndex,
            onDotClick = onNavigate,
        )
    }
}

/**
 * Вычислить визуальное состояние варианта ответа.
 */
private fun answerVisualState(
    answer: QuizAnswer,
    selectedId: String?,
): AnswerVisualState {
    if (selectedId == null) return AnswerVisualState.DEFAULT
    return when {
        answer.isCorrect && selectedId == answer.id -> AnswerVisualState.SELECTED_CORRECT
        selectedId == answer.id -> AnswerVisualState.SELECTED_WRONG
        answer.isCorrect -> AnswerVisualState.REVEALED_CORRECT
        else -> AnswerVisualState.DEFAULT
    }
}

/**
 * Вариант ответа.
 */
@Composable
private fun AnswerOption(
    answer: QuizAnswer,
    label: String,
    state: AnswerVisualState,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimensions = LocalAppDimensions.current

    val (containerColor, borderColor) = when (state) {
        AnswerVisualState.SELECTED_CORRECT ->
            CorrectColor.copy(alpha = 0.12f) to CorrectColor

        AnswerVisualState.SELECTED_WRONG ->
            WrongColor.copy(alpha = 0.12f) to WrongColor

        AnswerVisualState.REVEALED_CORRECT ->
            CorrectColor.copy(alpha = 0.12f) to CorrectColor

        AnswerVisualState.DEFAULT ->
            MaterialTheme.colorScheme.surface to MaterialTheme.colorScheme.outlineVariant
    }

    Card(
        onClick = onClick,
        enabled = enabled,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(dimensions.cardPaddingInner),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = borderColor,
                modifier = Modifier.padding(end = dimensions.itemSpacingMedium),
            )
            Text(
                text = answer.text,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

/**
 * Блок пояснения после ответа.
 */
@Composable
private fun ExplanationBlock(
    isCorrect: Boolean,
    correctAnswerText: String?,
    explanation: String?,
) {
    val dimensions = LocalAppDimensions.current
    val color = if (isCorrect) CorrectColor else WrongColor

    Card(
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.10f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(dimensions.cardPaddingInner)) {
            Text(
                text = stringResource(
                    if (isCorrect) R.string.quiz_correct else R.string.quiz_incorrect
                ),
                style = MaterialTheme.typography.titleSmall,
                color = color,
            )

            if (!isCorrect && correctAnswerText != null) {
                Spacer(modifier = Modifier.height(dimensions.itemSpacingSmall))
                Text(
                    text = stringResource(R.string.quiz_correct_answer, correctAnswerText),
                    style = MaterialTheme.typography.bodyMedium,
                    color = color,
                )
            }

            if (!explanation.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(dimensions.itemSpacingSmall))
                Text(
                    text = explanation,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

/**
 * Навигация «Назад / Вперёд (Результат)».
 */
@Composable
private fun NavigationRow(
    canGoBack: Boolean,
    isLastQuestion: Boolean,
    isAllAnswered: Boolean,
    unansweredCount: Int,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onFinish: () -> Unit,
) {
    val dimensions = LocalAppDimensions.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppButton(
            text = stringResource(R.string.quiz_back),
            onClick = onBack,
            enabled = canGoBack,
            style = ButtonStyle.OUTLINED,
            modifier = Modifier.weight(1f),
        )

        Spacer(modifier = Modifier.size(dimensions.itemSpacingMedium))

        if (isLastQuestion) {
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(1f)) {
                if (isAllAnswered.not() && unansweredCount > 0) {
                    Text(
                        text = pluralStringResource(
                            R.plurals.quiz_remaining,
                            unansweredCount,
                            unansweredCount,
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                AppButton(
                    text = stringResource(R.string.quiz_result_button),
                    onClick = onFinish,
                    enabled = isAllAnswered,
                    style = ButtonStyle.FILLED,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        } else {
            AppButton(
                text = stringResource(R.string.quiz_next),
                onClick = onNext,
                style = ButtonStyle.FILLED,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * Индикатор прогресса: кликабельные кружки по числу вопросов.
 */
@Composable
private fun ProgressDots(
    questions: List<QuizQuestion>,
    answers: Map<String, String>,
    currentIndex: Int,
    onDotClick: (Int) -> Unit,
) {
    val dimensions = LocalAppDimensions.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        questions.forEachIndexed { index, question ->
            val answered = answers.containsKey(question.id)
            val correct = question.answers.firstOrNull { it.id == answers[question.id] }?.isCorrect == true
            val color = when {
                answered && correct -> CorrectColor
                answered -> WrongColor
                index == currentIndex -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.outlineVariant
            }

            Box(
                modifier = Modifier
                    .padding(horizontal = dimensions.itemSpacingSmall)
                    .size(12.dp)
                    .clickable { onDotClick(index) }
                    .background(color = color, shape = CircleShape),
            )
        }
    }
}
