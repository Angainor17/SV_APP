package su.sv.app.testing

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.swipeUp

/**
 * Общие утилиты для UI тестов.
 * Содержит расширения и хелперы для уменьшения дублирования кода.
 */
object TestUtils {

    // ==================== Ожидания ====================

    /**
     * Ждёт появления элемента с указанным тегом.
     *
     * @param tag TestTag элемента
     * @param timeoutMs Максимальное время ожидания в миллисекундах
     * @param useUnmergedTree Использовать unmerged tree для поиска
     */
    fun ComposeTestRule.waitForTag(
        tag: String,
        timeoutMs: Long = 5000,
        useUnmergedTree: Boolean = true,
    ) {
        waitUntil(timeoutMs) {
            try {
                onNodeWithTag(tag, useUnmergedTree).fetchSemanticsNode() != null
            } catch (e: Exception) {
                false
            }
        }
    }

    /**
     * Ждёт появления хотя бы одного элемента с указанным тегом.
     *
     * @param tag TestTag элемента
     * @param timeoutMs Максимальное время ожидания в миллисекундах
     * @param minCount Минимальное количество элементов
     */
    fun ComposeTestRule.waitForItemsWithTag(
        tag: String,
        timeoutMs: Long = 5000,
        minCount: Int = 1,
        useUnmergedTree: Boolean = true,
    ) {
        waitUntil(timeoutMs) {
            onAllNodesWithTag(tag, useUnmergedTree)
                .fetchSemanticsNodes()
                .size >= minCount
        }
    }

    /**
     * Ждёт исчезновения элемента.
     */
    fun ComposeTestRule.waitForTagToDisappear(
        tag: String,
        timeoutMs: Long = 5000,
        useUnmergedTree: Boolean = true,
    ) {
        waitUntil(timeoutMs) {
            try {
                onNodeWithTag(tag, useUnmergedTree).fetchSemanticsNode()
                false // Элемент ещё существует
            } catch (e: Exception) {
                true // Элемент исчез
            }
        }
    }

    // ==================== Поиск элементов ====================

    /**
     * Находит элемент по тегу или возвращает null.
     */
    fun ComposeTestRule.findNodeByTag(
        tag: String,
        useUnmergedTree: Boolean = true,
    ): SemanticsNodeInteraction? {
        return try {
            onNodeWithTag(tag, useUnmergedTree)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Находит все элементы с указанным тегом.
     */
    fun ComposeTestRule.findAllNodesByTag(
        tag: String,
        useUnmergedTree: Boolean = true,
    ): List<SemanticsNodeInteraction> {
        return List(
            onAllNodesWithTag(tag, useUnmergedTree)
                .fetchSemanticsNodes().size) { index ->
            onAllNodesWithTag(tag, useUnmergedTree)[index]
        }
    }

    // ==================== Проверки (Assertions) ====================

    /**
     * Проверяет, что элемент существует.
     */
    fun ComposeTestRule.assertTagExists(
        tag: String,
        useUnmergedTree: Boolean = true,
    ) {
        onNodeWithTag(tag, useUnmergedTree).assertExists()
    }

    /**
     * Проверяет, что элемент отображается.
     */
    fun ComposeTestRule.assertTagIsDisplayed(
        tag: String,
        useUnmergedTree: Boolean = true,
    ) {
        onNodeWithTag(tag, useUnmergedTree).assertIsDisplayed()
    }

    /**
     * Проверяет, что элемент не существует.
     */
    fun ComposeTestRule.assertTagDoesNotExist(
        tag: String,
        useUnmergedTree: Boolean = true,
    ) {
        onNodeWithTag(tag, useUnmergedTree).assertDoesNotExist()
    }

    /**
     * Проверяет, что элемент имеет действие клика.
     */
    fun ComposeTestRule.assertTagHasClickAction(
        tag: String,
        useUnmergedTree: Boolean = true,
    ) {
        onNodeWithTag(tag, useUnmergedTree).assertHasClickAction()
    }

    /**
     * Проверяет, что элемент содержит текст.
     */
    fun ComposeTestRule.assertTagContainsText(
        tag: String,
        text: String,
        substring: Boolean = true,
        useUnmergedTree: Boolean = true,
    ) {
        onNodeWithTag(tag, useUnmergedTree).assertTextContains(text, substring)
    }

    // ==================== Действия (Actions) ====================

    /**
     * Кликает по элементу с указанным тегом.
     */
    fun ComposeTestRule.clickOnTag(
        tag: String,
        useUnmergedTree: Boolean = true,
    ) {
        onNodeWithTag(tag, useUnmergedTree).performClick()
    }

    /**
     * Вводит текст в поле с указанным тегом.
     */
    fun ComposeTestRule.inputTextToTag(
        tag: String,
        text: String,
        useUnmergedTree: Boolean = true,
    ) {
        onNodeWithTag(tag, useUnmergedTree).performTextInput(text)
    }

    /**
     * Выполняет swipe-жест на элементе.
     */
    fun ComposeTestRule.swipeOnTag(
        tag: String,
        direction: SwipeDirection,
        useUnmergedTree: Boolean = true,
    ) {
        onNodeWithTag(tag, useUnmergedTree).performTouchInput {
            when (direction) {
                SwipeDirection.LEFT -> swipeLeft()
                SwipeDirection.RIGHT -> swipeRight()
                SwipeDirection.UP -> swipeUp()
                SwipeDirection.DOWN -> swipeDown()
            }
        }
    }

    /**
     * Направление swipe-жеста.
     */
    enum class SwipeDirection {
        LEFT, RIGHT, UP, DOWN
    }

    // ==================== Безопасные операции ====================

    /**
     * Выполняет действие, игнорируя исключения.
     *
     * @return true если действие успешно, false если произошла ошибка
     */
    fun ComposeTestRule.safeAction(
        action: () -> Unit,
    ): Boolean {
        return try {
            action()
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Выполняет действие с повторными попытками.
     *
     * @param times Количество попыток
     * @param delayMs Задержка между попытками в миллисекундах
     * @param action Действие для выполнения
     */
    fun ComposeTestRule.retryAction(
        times: Int = 3,
        delayMs: Long = 1000,
        action: () -> Unit,
    ) {
        var lastException: Exception? = null
        repeat(times) {
            try {
                action()
                return
            } catch (e: Exception) {
                lastException = e
                if (it < times - 1) {
                    Thread.sleep(delayMs)
                }
            }
        }
        throw lastException ?: IllegalStateException("Unknown error during retry")
    }
}

// ==================== Расширения для SemanticsNodeInteraction ====================

/**
 * Проверяет, что элемент существует и отображается.
 */
fun SemanticsNodeInteraction.assertExistsAndDisplayed(): SemanticsNodeInteraction {
    return this.assertExists().assertIsDisplayed()
}

/**
 * Проверяет, что элемент существует и имеет действие клика.
 */
fun SemanticsNodeInteraction.assertExistsAndClickable(): SemanticsNodeInteraction {
    return this.assertExists().assertHasClickAction()
}

/**
 * Безопасно кликает на элемент, возвращая false при ошибке.
 */
fun SemanticsNodeInteraction.safeClick(): Boolean {
    return try {
        performClick()
        true
    } catch (e: Exception) {
        false
    }
}