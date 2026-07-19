package su.sv.app.testing

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith
import su.sv.app.MainActivity

/**
 * Базовый класс для UI тестов с Hilt DI.
 *
 * Предоставляет:
 * - HiltAndroidRule для внедрения зависимостей
 * - ComposeRule с MainActivity для тестирования реального приложения
 * - Вспомогательные методы
 *
 * Использование:
 * ```kotlin
 * @HiltAndroidTest
 * class MyScreenTest : BaseUiTest() {
 *
 *     @Test
 *     fun myTest() {
 *         // navigate и тесты
 *         composeRule.onNodeWithTag("...").assertIsDisplayed()
 *     }
 * }
 * ```
 */
@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
abstract class BaseUiTest {

    // Hilt правило должно быть первым
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    // Compose правило с MainActivity
    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setup() {
        hiltRule.inject()
    }

    // ==================== Helper Methods ====================

    /**
     * Ожидание в миллисекундах.
     * Использовать с осторожностью - только когда нет другого способа.
     */
    protected fun waitUntil(timeoutMs: Long = 5000) {
        Thread.sleep(timeoutMs)
    }

    /**
     * Повторить действие с задержкой при ошибке.
     */
    protected fun <T> retryOnFailure(
        times: Int = 3,
        delayMs: Long = 1000,
        block: () -> T,
    ): T {
        var lastException: Exception? = null
        repeat(times) {
            try {
                return block()
            } catch (e: Exception) {
                lastException = e
                Thread.sleep(delayMs)
            }
        }
        throw lastException ?: IllegalStateException("Unknown error")
    }

    // ==================== Navigation Helpers ====================

    /**
     * Переход на вкладку News.
     */
    protected fun navigateToNewsTab() {
        composeRule.waitForIdle()
        composeRule
            .onNodeWithTag(TestTags.BottomNav.TAB_NEWS, useUnmergedTree = true)
            .performClick()
        composeRule.waitForIdle()
    }

    /**
     * Переход на вкладку Books.
     */
    protected fun navigateToBooksTab() {
        composeRule.waitForIdle()
        composeRule
            .onNodeWithTag(TestTags.BottomNav.TAB_BOOKS, useUnmergedTree = true)
            .performClick()
        composeRule.waitForIdle()
    }

    /**
     * Переход на вкладку Wiki.
     */
    protected fun navigateToWikiTab() {
        composeRule.waitForIdle()
        composeRule
            .onNodeWithTag(TestTags.BottomNav.TAB_WIKI, useUnmergedTree = true)
            .performClick()
        composeRule.waitForIdle()
    }

    /**
     * Переход на вкладку Info.
     */
    protected fun navigateToInfoTab() {
        composeRule.waitForIdle()
        composeRule
            .onNodeWithTag(TestTags.BottomNav.TAB_INFO, useUnmergedTree = true)
            .performClick()
        composeRule.waitForIdle()
    }
}