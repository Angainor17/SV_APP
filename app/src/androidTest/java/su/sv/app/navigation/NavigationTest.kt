package su.sv.app.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Test
import su.sv.app.testing.BaseUiTest
import su.sv.app.testing.NavigationTest
import su.sv.app.testing.ReleaseTest
import su.sv.app.testing.SmokeTest
import su.sv.app.testing.TestTags

/**
 * UI тесты для навигации приложения.
 *
 * Тестируемые сценарии:
 * - Переключение между вкладками BottomNav
 * - Кнопка "Назад"
 * - Навигация между экранами
 */
@HiltAndroidTest
class NavigationTest : BaseUiTest() {

    // ==================== Bottom Navigation Tests ====================

    /**
     * Тест: Нижняя навигация отображается.
     */
    @Test
    @SmokeTest
    fun bottomNav_isDisplayed() {
        composeRule.waitForIdle()

        composeRule
            .onNodeWithTag(TestTags.BottomNav.ROOT, useUnmergedTree = true)
            .assertIsDisplayed()
    }

    /**
     * Тест: Все 4 вкладки отображаются.
     */
    @Test
    @SmokeTest
    fun bottomNav_allTabs_areVisible() {
        composeRule.waitForIdle()

        // Проверяем все вкладки
        composeRule
            .onNodeWithTag(TestTags.BottomNav.TAB_NEWS, useUnmergedTree = true)
            .assertIsDisplayed()

        composeRule
            .onNodeWithTag(TestTags.BottomNav.TAB_BOOKS, useUnmergedTree = true)
            .assertIsDisplayed()

        composeRule
            .onNodeWithTag(TestTags.BottomNav.TAB_WIKI, useUnmergedTree = true)
            .assertIsDisplayed()

        composeRule
            .onNodeWithTag(TestTags.BottomNav.TAB_INFO, useUnmergedTree = true)
            .assertIsDisplayed()
    }

    /**
     * Тест: Вкладка News выбрана по умолчанию при запуске.
     */
    @Test
    @SmokeTest
    @NavigationTest
    fun bottomNav_newsTab_selectedByDefault() {
        composeRule.waitForIdle()

        // При запуске должна быть выбрана вкладка News
        composeRule
            .onNodeWithTag(TestTags.News.ROOT, useUnmergedTree = true)
            .assertExists()
    }

    /**
     * Тест: Переключение на вкладку Books.
     */
    @Test
    @NavigationTest
    fun bottomNav_switchToBooksTab() {
        navigateToBooksTab()

        // Проверяем, что экран Books отображается
        composeRule
            .onNodeWithTag(TestTags.BooksCatalog.ROOT, useUnmergedTree = true)
            .assertExists()
    }

    /**
     * Тест: Переключение на вкладку Wiki.
     */
    @Test
    @NavigationTest
    fun bottomNav_switchToWikiTab() {
        navigateToWikiTab()

        // Проверяем, что экран Wiki отображается
        composeRule
            .onNodeWithTag(TestTags.WikiRoot.ROOT, useUnmergedTree = true)
            .assertExists()
    }

    /**
     * Тест: Переключение на вкладку Info.
     */
    @Test
    @NavigationTest
    fun bottomNav_switchToInfoTab() {
        navigateToInfoTab()

        // Проверяем, что экран Info отображается
        composeRule
            .onNodeWithTag(TestTags.Info.ROOT, useUnmergedTree = true)
            .assertExists()
    }

    // ==================== Back Navigation Tests ====================

    /**
     * Тест: Кнопка "Назад" работает на экране деталей книги.
     */
    @Test
    @ReleaseTest
    @NavigationTest
    fun navigation_backFromBookDetail_works() {
        // Переходим в Books
        navigateToBooksTab()

        // Ждём загрузки книг
        composeRule.waitUntil(10000) {
            composeRule
                .onAllNodesWithTag(TestTags.BooksCatalog.ITEM, useUnmergedTree = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        // Открываем детали книги
        composeRule
            .onNodeWithTag(TestTags.BooksCatalog.ITEM, useUnmergedTree = true)
            .performClick()

        composeRule.waitForIdle()

        // Проверяем, что экран деталей отображается
        composeRule
            .onNodeWithTag(TestTags.BookDetail.ROOT, useUnmergedTree = true)
            .assertExists()

        // Нажимаем "Назад"
        composeRule
            .onNodeWithTag(TestTags.Common.BACK_BUTTON, useUnmergedTree = true)
            .performClick()

        composeRule.waitForIdle()

        // Проверяем, что вернулись в каталог
        composeRule
            .onNodeWithTag(TestTags.BooksCatalog.ROOT, useUnmergedTree = true)
            .assertExists()
    }

    /**
     * Тест: Кнопка "Назад" работает на экране статьи Wiki.
     */
    @Test
    @ReleaseTest
    @NavigationTest
    fun navigation_backFromWikiArticle_works() {
        // Переходим в Wiki
        navigateToWikiTab()

        // Ищем статью
        composeRule
            .onNodeWithTag(TestTags.WikiRoot.SEARCH_FIELD, useUnmergedTree = true)
            .performTextInput("Солнце")

        composeRule.waitUntil(5000) {
            composeRule
                .onAllNodesWithTag(TestTags.WikiRoot.SUGGESTION_ITEM, useUnmergedTree = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        // Открываем статью
        composeRule
            .onNodeWithTag(TestTags.WikiRoot.SUGGESTION_ITEM, useUnmergedTree = true)
            .performClick()

        composeRule.waitForIdle()

        // Проверяем, что статья открыта
        composeRule
            .onNodeWithTag(TestTags.WikiArticle.ROOT, useUnmergedTree = true)
            .assertExists()

        // Нажимаем "Назад"
        composeRule
            .onNodeWithTag(TestTags.Common.BACK_BUTTON, useUnmergedTree = true)
            .performClick()

        composeRule.waitForIdle()

        // Проверяем, что вернулись в Wiki
        composeRule
            .onNodeWithTag(TestTags.WikiRoot.ROOT, useUnmergedTree = true)
            .assertExists()
    }

    // ==================== Tab Switching Order Tests ====================

    /**
     * Тест: Последовательное переключение между всеми вкладками.
     */
    @Test
    @ReleaseTest
    @NavigationTest
    fun navigation_switchAllTabs_sequentially() {
        composeRule.waitForIdle()

        // News → Books
        navigateToBooksTab()
        composeRule.onNodeWithTag(TestTags.BooksCatalog.ROOT).assertExists()

        // Books → Wiki
        navigateToWikiTab()
        composeRule.onNodeWithTag(TestTags.WikiRoot.ROOT).assertExists()

        // Wiki → Info
        navigateToInfoTab()
        composeRule.onNodeWithTag(TestTags.Info.ROOT).assertExists()

        // Info → News
        navigateToNewsTab()
        composeRule.onNodeWithTag(TestTags.News.ROOT).assertExists()
    }
}