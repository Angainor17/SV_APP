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
import su.sv.main.continuereading.ui.ContinueReadingSnackbar

/**
 * Unit тесты для UI компонента ContinueReadingSnackbar.
 *
 * Тестирует:
 * - Отображение snackbar с книгой
 * - Отображение обложки/placeholder'а
 * - Клик на "Продолжить"
 * - Клик на "Закрыть"
 */
class ContinueReadingSnackbarTest {

    @get:Rule
    val composeRule = createComposeRule()

    // ==================== Тестовые данные ====================

    private val testBookWithCover = LastReadBookInfo(
        title = "Тестовая книга",
        authors = "Тестовый автор",
        coverUrl = "https://example.com/cover.jpg",
        bookFileUri = "content://test/book.epub",
    )

    private val testBookWithoutCover = LastReadBookInfo(
        title = "Книга без обложки",
        authors = null,
        coverUrl = null,
        bookFileUri = "content://test/book2.epub",
    )

    private val testBookLongTitle = LastReadBookInfo(
        title = "Очень длинное название книги которое должно обрезаться при отображении",
        authors = "Автор с очень длинным именем которое тоже должно обрезаться",
        coverUrl = null,
        bookFileUri = "content://test/book3.epub",
    )

    // ==================== Тесты отображения ====================

    /**
     * Тест: Snackbar отображается с полной информацией о книге.
     */
    @Test
    fun snackbar_withFullInfo_isDisplayed() {
        var continueClicked = false
        var dismissClicked = false

        composeRule.setContent {
            ContinueReadingSnackbar(
                bookInfo = testBookWithCover,
                onContinueClick = { continueClicked = true },
                onDismissClick = { dismissClicked = true },
            )
        }

        // Проверяем что snackbar отображается
        composeRule
            .onNodeWithTag(TestTags.ContinueReading.ROOT, useUnmergedTree = true)
            .assertIsDisplayed()

        // Проверяем название
        composeRule
            .onNodeWithTag(TestTags.ContinueReading.TITLE, useUnmergedTree = true)
            .assertIsDisplayed()

        // Проверяем автора
        composeRule
            .onNodeWithTag(TestTags.ContinueReading.AUTHOR, useUnmergedTree = true)
            .assertIsDisplayed()

        // Проверяем обложку
        composeRule
            .onNodeWithTag(TestTags.ContinueReading.COVER, useUnmergedTree = true)
            .assertIsDisplayed()

        // Проверяем кнопки
        composeRule
            .onNodeWithTag(TestTags.ContinueReading.CONTINUE_BUTTON, useUnmergedTree = true)
            .assertIsDisplayed()

        composeRule
            .onNodeWithTag(TestTags.ContinueReading.DISMISS_BUTTON, useUnmergedTree = true)
            .assertIsDisplayed()
    }

    /**
     * Тест: Snackbar отображается без автора и обложки.
     */
    @Test
    fun snackbar_withoutAuthorAndCover_isDisplayed() {
        composeRule.setContent {
            ContinueReadingSnackbar(
                bookInfo = testBookWithoutCover,
                onContinueClick = {},
                onDismissClick = {},
            )
        }

        // Проверяем что snackbar отображается
        composeRule
            .onNodeWithTag(TestTags.ContinueReading.ROOT, useUnmergedTree = true)
            .assertIsDisplayed()

        // Проверяем название
        composeRule
            .onNodeWithTag(TestTags.ContinueReading.TITLE, useUnmergedTree = true)
            .assertIsDisplayed()

        // Автор не должен отображаться (тега нет)
        composeRule
            .onNodeWithTag(TestTags.ContinueReading.AUTHOR, useUnmergedTree = true)
            .assertIsNotDisplayed()

        // Placeholder обложки должен отображаться
        composeRule
            .onNodeWithTag(TestTags.ContinueReading.COVER, useUnmergedTree = true)
            .assertIsDisplayed()
    }

    /**
     * Тест: Длинный текст обрезается.
     */
    @Test
    fun snackbar_withLongTitle_isDisplayed() {
        composeRule.setContent {
            ContinueReadingSnackbar(
                bookInfo = testBookLongTitle,
                onContinueClick = {},
                onDismissClick = {},
            )
        }

        // Проверяем что snackbar отображается
        composeRule
            .onNodeWithTag(TestTags.ContinueReading.ROOT, useUnmergedTree = true)
            .assertIsDisplayed()

        // Название и автор должны отображаться (даже если обрезаны)
        composeRule
            .onNodeWithTag(TestTags.ContinueReading.TITLE, useUnmergedTree = true)
            .assertIsDisplayed()

        composeRule
            .onNodeWithTag(TestTags.ContinueReading.AUTHOR, useUnmergedTree = true)
            .assertIsDisplayed()
    }

    // ==================== Тесты взаимодействия ====================

    /**
     * Тест: Клик на кнопку "Продолжить".
     */
    @Test
    fun snackbar_continueClick_triggersCallback() {
        var continueClicked = false

        composeRule.setContent {
            ContinueReadingSnackbar(
                bookInfo = testBookWithCover,
                onContinueClick = { continueClicked = true },
                onDismissClick = {},
            )
        }

        // Кликаем на кнопку "Продолжить"
        composeRule
            .onNodeWithTag(TestTags.ContinueReading.CONTINUE_BUTTON, useUnmergedTree = true)
            .performClick()

        // Проверяем что callback был вызван
        assert(continueClicked) { "Continue button click should trigger callback" }
    }

    /**
     * Тест: Клик на кнопку закрытия.
     */
    @Test
    fun snackbar_dismissClick_triggersCallback() {
        var dismissClicked = false

        composeRule.setContent {
            ContinueReadingSnackbar(
                bookInfo = testBookWithCover,
                onContinueClick = {},
                onDismissClick = { dismissClicked = true },
            )
        }

        // Кликаем на кнопку закрытия
        composeRule
            .onNodeWithTag(TestTags.ContinueReading.DISMISS_BUTTON, useUnmergedTree = true)
            .performClick()

        // Проверяем что callback был вызван
        assert(dismissClicked) { "Dismiss button click should trigger callback" }
    }

    /**
     * Тест: Клик на весь snackbar вызывает "Продолжить".
     */
    @Test
    fun snackbar_rootClick_triggersContinueCallback() {
        var continueClicked = false

        composeRule.setContent {
            ContinueReadingSnackbar(
                bookInfo = testBookWithCover,
                onContinueClick = { continueClicked = true },
                onDismissClick = {},
            )
        }

        // Кликаем на весь snackbar
        composeRule
            .onNodeWithTag(TestTags.ContinueReading.ROOT, useUnmergedTree = true)
            .performClick()

        // Проверяем что callback был вызван
        assert(continueClicked) { "Snackbar click should trigger continue callback" }
    }
}