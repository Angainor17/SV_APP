package su.sv.app.testing

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
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
 * - Вспомогательные методы для навигации и ожиданий
 * - Интеграцию с [ScreenActions] для повторяемых операций
 *
 * Использование:
 * ```kotlin
 * @HiltAndroidTest
 * class MyScreenTest : BaseUiTest() {
 *
 *     @Test
 *     fun myTest() {
 *         // Используйте хелперы
 *         navigateToBooksTab()
 *         waitForItems(TestTags.BooksCatalog.ITEM)
 *
 *         // Или используйте ScreenActions
 *         screenActions.navigateToWiki()
 *         screenActions.searchWiki("Солнце")
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

    // Экземпляр ScreenActions для повторяемых операций
    protected val screenActions: ScreenActions by lazy { ScreenActions(composeRule) }

    @Before
    fun setup() {
        hiltRule.inject()
    }

    // ==================== Ожидания ====================

    /**
     * Ожидание появления элемента с указанным тегом.
     */
    protected fun waitForTag(tag: String, timeoutMs: Long = 5000) {
        composeRule.waitUntil(timeoutMs) {
            try {
                composeRule.onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode() != null
            } catch (e: Exception) {
                false
            }
        }
    }

    /**
     * Ожидание появления элементов с указанным тегом.
     */
    protected fun waitForItems(tag: String, timeoutMs: Long = 10000, minCount: Int = 1) {
        composeRule.waitUntil(timeoutMs) {
            composeRule.onAllNodesWithTag(tag, useUnmergedTree = true)
                .fetchSemanticsNodes()
                .size >= minCount
        }
    }

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

    // ==================== Проверки ====================

    /**
     * Проверить, что элемент существует.
     */
    protected fun assertTagExists(tag: String) {
        composeRule.onNodeWithTag(tag, useUnmergedTree = true).assertExists()
    }

    /**
     * Проверить, что элемент отображается.
     */
    protected fun assertTagIsDisplayed(tag: String) {
        composeRule.onNodeWithTag(tag, useUnmergedTree = true).assertIsDisplayed()
    }

    // ==================== Действия ====================

    /**
     * Кликнуть по элементу с указанным тегом.
     */
    protected fun clickOnTag(tag: String) {
        composeRule.onNodeWithTag(tag, useUnmergedTree = true).performClick()
    }

    // ==================== Navigation Helpers ====================

    /**
     * Переход на вкладку News.
     */
    protected fun navigateToNewsTab() {
        screenActions.navigateToNews()
    }

    /**
     * Переход на вкладку Books.
     */
    protected fun navigateToBooksTab() {
        screenActions.navigateToBooks()
    }

    /**
     * Переход на вкладку Wiki.
     */
    protected fun navigateToWikiTab() {
        screenActions.navigateToWiki()
    }

    /**
     * Переход на вкладку Info.
     */
    protected fun navigateToInfoTab() {
        screenActions.navigateToInfo()
    }
}