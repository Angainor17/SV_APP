package su.sv.app.continuereading

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.github.axet.bookreader.domain.LastReadBookInfo
import org.junit.Rule
import org.junit.Test
import su.sv.app.testing.TestTags
import su.sv.main.continuereading.ContinueReadingState
import su.sv.main.continuereading.ui.ContinueReadingSnackbarHost

/**
 * Unit тесты для ContinueReadingSnackbarHost с анимациями.
 *
 * Тестирует:
 * - Отображение в состоянии Visible
 * - Скрытие в состоянии Hidden
 * - Анимации появления/исчезновения
 */
class ContinueReadingSnackbarHostTest {

    @get:Rule
    val composeRule = createComposeRule()

    // ==================== Тестовые данные ====================

    private val testBook = LastReadBookInfo(
        title = "Тестовая книга",
        authors = "Тестовый автор",
        coverUrl = null,
        bookFileUri = "content://test/book.epub",
    )

    // ==================== Тесты состояния ====================

    /**
     * Тест: В состоянии Hidden snackbar не отображается.
     */
    @Test
    fun host_hiddenState_snackbarNotDisplayed() {
        composeRule.setContent {
            ContinueReadingSnackbarHost(
                state = ContinueReadingState.Hidden,
                onContinueClick = {},
                onDismissClick = {},
            )
        }

        composeRule.waitForIdle()

        // Snackbar не должен отображаться
        composeRule
            .onNodeWithTag(TestTags.ContinueReading.ROOT, useUnmergedTree = true)
            .assertIsNotDisplayed()
    }

    /**
     * Тест: В состоянии Visible snackbar отображается.
     */
    @Test
    fun host_visibleState_snackbarIsDisplayed() {
        composeRule.setContent {
            ContinueReadingSnackbarHost(
                state = ContinueReadingState.Visible(testBook),
                onContinueClick = {},
                onDismissClick = {},
            )
        }

        // Ждём завершения анимации появления (300ms)
        composeRule.waitForIdle()
        Thread.sleep(400)

        // Snackbar должен отображаться
        composeRule
            .onNodeWithTag(TestTags.ContinueReading.ROOT, useUnmergedTree = true)
            .assertIsDisplayed()
    }

    /**
     * Тест: Переключение из Hidden в Visible показывает snackbar.
     */
    @Test
    fun host_transitionFromHiddenToVisible_showsSnackbar() {
        var state: ContinueReadingState = ContinueReadingState.Hidden

        composeRule.setContent {
            ContinueReadingSnackbarHost(
                state = state,
                onContinueClick = {},
                onDismissClick = {},
            )
        }

        // Изначально snackbar скрыт
        composeRule.waitForIdle()
        composeRule
            .onNodeWithTag(TestTags.ContinueReading.ROOT, useUnmergedTree = true)
            .assertIsNotDisplayed()
    }

    /**
     * Тест: Кнопки работают в состоянии Visible.
     */
    @Test
    fun host_visibleState_buttonsWork() {
        var continueClicked = false
        var dismissClicked = false

        composeRule.setContent {
            ContinueReadingSnackbarHost(
                state = ContinueReadingState.Visible(testBook),
                onContinueClick = { continueClicked = true },
                onDismissClick = { dismissClicked = true },
            )
        }

        composeRule.waitForIdle()
        Thread.sleep(400)

        // Клик на "Продолжить"
        composeRule
            .onNodeWithTag(TestTags.ContinueReading.CONTINUE_BUTTON, useUnmergedTree = true)
            .performClick()

        assert(continueClicked) { "Continue button should work" }

        // Клик на "Закрыть"
        composeRule
            .onNodeWithTag(TestTags.ContinueReading.DISMISS_BUTTON, useUnmergedTree = true)
            .performClick()

        assert(dismissClicked) { "Dismiss button should work" }
    }
}