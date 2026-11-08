package su.sv.quiz.presentation.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import su.sv.quiz.R
import su.sv.quiz.domain.model.Quiz
import su.sv.quiz.domain.model.QuizAnswer
import su.sv.quiz.domain.model.QuizQuestion

/**
 * UI-тесты карточки квиза [QuizCard].
 */
@RunWith(AndroidJUnit4::class)
class QuizCardTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun card_displaysTitleAndQuestionCount() {
        val quiz = quiz(title = "Диалектика Гегеля", questionCount = 3)
        composeRule.setContent {
            MaterialTheme {
                QuizCard(quiz = quiz, isNew = false, onClick = {})
            }
        }

        composeRule.onNodeWithText("Диалектика Гегеля").assertIsDisplayed()
    }

    @Test
    fun card_clickInvokesCallback() {
        val quiz = quiz(title = "Диалектика Гегеля")
        var clicked = false
        composeRule.setContent {
            MaterialTheme {
                QuizCard(quiz = quiz, isNew = false, onClick = { clicked = true })
            }
        }

        composeRule.onNodeWithText("Диалектика Гегеля").performClick()

        assertEquals(true, clicked)
    }

    @Test
    fun card_showsNewBadgeWhenNew() {
        val quiz = quiz(title = "Диалектика Гегеля")
        composeRule.setContent {
            MaterialTheme {
                QuizCard(quiz = quiz, isNew = true, onClick = {})
            }
        }

        composeRule.onNodeWithText(
            InstrumentationRegistry.getInstrumentation().targetContext
                .getString(R.string.quiz_new_badge),
        ).assertIsDisplayed()
    }

    private fun quiz(
        title: String,
        questionCount: Int = 1,
    ): Quiz = Quiz(
        id = "quiz-1",
        title = title,
        description = null,
        themeId = null,
        questions = (1..questionCount).map { i ->
            QuizQuestion(
                id = "q-$i",
                text = "Вопрос $i",
                answers = listOf(
                    QuizAnswer("a1", "Ответ 1", isCorrect = true),
                    QuizAnswer("a2", "Ответ 2", isCorrect = false),
                ),
            )
        },
        createdAtEpochMillis = 0L,
        imageUrl = null,
    )
}
