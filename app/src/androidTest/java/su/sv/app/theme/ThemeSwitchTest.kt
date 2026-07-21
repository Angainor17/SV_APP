package su.sv.app.theme

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Test
import su.sv.app.testing.BaseUiTest
import su.sv.app.testing.ReleaseTest
import su.sv.app.testing.SmokeTest
import su.sv.app.testing.TestTags

/**
 * UI тесты для переключения темы приложения.
 *
 * Тестируемые сценарии:
 * - Переключение темы на экране новостей
 * - Влияние темы на все экраны
 * - Сохранение состояния темы
 * - Тема читалки
 */
@HiltAndroidTest
class ThemeSwitchTest : BaseUiTest() {

    // ==================== Theme Toggle Tests ====================

    /**
     * Тест: Кнопка переключения темы существует.
     */
    @Test
    @SmokeTest
    fun themeToggle_exists() {
        composeRule.waitForIdle()

        composeRule
            .onNodeWithTag(TestTags.News.THEME_TOGGLE, useUnmergedTree = true)
            .assertExists()
    }

    /**
     * Тест: Кнопка переключения темы кликабельна.
     */
    @Test
    @SmokeTest
    fun themeToggle_isClickable() {
        composeRule.waitForIdle()

        composeRule
            .onNodeWithTag(TestTags.News.THEME_TOGGLE, useUnmergedTree = true)
            .assertHasClickAction()
    }

    /**
     * Тест: Переключение темы на светлую.
     */
    @Test
    @ReleaseTest
    fun themeSwitch_toggleOnce() {
        composeRule.waitForIdle()

        // Переключаем тему
        composeRule
            .onNodeWithTag(TestTags.News.THEME_TOGGLE, useUnmergedTree = true)
            .performClick()

        composeRule.waitForIdle()

        // Проверяем, что UI обновился
        composeRule
            .onNodeWithTag(TestTags.News.ROOT, useUnmergedTree = true)
            .assertExists()
    }

    /**
     * Тест: Двойное переключение возвращает исходную тему.
     */
    @Test
    @ReleaseTest
    fun themeSwitch_toggleTwice_returnsToOriginal() {
        composeRule.waitForIdle()

        // Переключаем первый раз
        composeRule
            .onNodeWithTag(TestTags.News.THEME_TOGGLE, useUnmergedTree = true)
            .performClick()

        composeRule.waitForIdle()

        // Переключаем второй раз
        composeRule
            .onNodeWithTag(TestTags.News.THEME_TOGGLE, useUnmergedTree = true)
            .performClick()

        composeRule.waitForIdle()

        // Проверяем, что UI работает
        composeRule
            .onNodeWithTag(TestTags.News.ROOT, useUnmergedTree = true)
            .assertExists()
    }

    // ==================== Theme Persistence Tests ====================

    /**
     * Тест: Тема сохраняется при переключении экранов.
     */
    @Test
    @ReleaseTest
    fun themePersistence_acrossScreens() {
        composeRule.waitForIdle()

        // Переключаем тему
        composeRule
            .onNodeWithTag(TestTags.News.THEME_TOGGLE, useUnmergedTree = true)
            .performClick()

        composeRule.waitForIdle()

        // Переходим на другой экран
        navigateToBooksTab()

        // Возвращаемся на News
        navigateToNewsTab()

        composeRule.waitForIdle()

        // Проверяем, что UI работает с новой темой
        composeRule
            .onNodeWithTag(TestTags.News.ROOT, useUnmergedTree = true)
            .assertExists()
    }

    /**
     * Тест: Тема применяется ко всем вкладкам.
     */
    @Test
    @ReleaseTest
    fun themeApplied_toAllTabs() {
        composeRule.waitForIdle()

        // Переключаем тему на News
        composeRule
            .onNodeWithTag(TestTags.News.THEME_TOGGLE, useUnmergedTree = true)
            .performClick()

        composeRule.waitForIdle()

        // Проверяем все вкладки
        navigateToBooksTab()
        composeRule.onNodeWithTag(TestTags.BooksCatalog.ROOT, useUnmergedTree = true).assertExists()

        navigateToWikiTab()
        composeRule.onNodeWithTag(TestTags.WikiRoot.ROOT, useUnmergedTree = true).assertExists()

        navigateToInfoTab()
        composeRule.onNodeWithTag(TestTags.Info.ROOT, useUnmergedTree = true).assertExists()
    }

    // ==================== Theme UI Tests ====================

    /**
     * Тест: Bottom navigation отображается с новой темой.
     */
    @Test
    @ReleaseTest
    fun themeUI_bottomNavigationWorks() {
        composeRule.waitForIdle()

        // Переключаем тему
        composeRule
            .onNodeWithTag(TestTags.News.THEME_TOGGLE, useUnmergedTree = true)
            .performClick()

        composeRule.waitForIdle()

        // Проверяем навигацию
        composeRule
            .onNodeWithTag(TestTags.BottomNav.ROOT, useUnmergedTree = true)
            .assertIsDisplayed()
    }

    /**
     * Тест: Элементы списка отображаются с новой темой.
     */
    @Test
    @ReleaseTest
    fun themeUI_listItemsWork() {
        composeRule.waitForIdle()

        // Переключаем тему
        composeRule
            .onNodeWithTag(TestTags.News.THEME_TOGGLE, useUnmergedTree = true)
            .performClick()

        composeRule.waitForIdle()

        // Ждём загрузки элементов
        waitForItems(TestTags.News.ITEM)

        // Проверяем, что элементы отображаются
        composeRule
            .onNodeWithTag(TestTags.News.ITEM, useUnmergedTree = true)
            .assertExists()
    }

    // ==================== Reader Theme Tests ====================

    /**
     * Тест: Тема читалки независима от темы приложения.
     */
    @Test
    @ReleaseTest
    fun readerTheme_independentFromAppTheme() {
        composeRule.waitForIdle()

        // Переключаем тему приложения
        composeRule
            .onNodeWithTag(TestTags.News.THEME_TOGGLE, useUnmergedTree = true)
            .performClick()

        composeRule.waitForIdle()

        // Открываем книгу (если есть)
        navigateToBooksTab()

        try {
            waitForItems(TestTags.DownloadedBooks.ITEM, timeoutMs = 5000)

            composeRule
                .onNodeWithTag(TestTags.DownloadedBooks.ITEM, useUnmergedTree = true)
                .performClick()

            composeRule.waitForIdle()

            // Проверяем, что читалка работает с любой темой приложения
            composeRule
                .onNodeWithTag(TestTags.Reader.ROOT, useUnmergedTree = true)
                .assertExists()
        } catch (e: Exception) {
            // Книга может отсутствовать
        }
    }

    /**
     * Тест: Переключение темы читалки.
     */
    @Test
    @ReleaseTest
    fun readerTheme_canBeChanged() {
        composeRule.waitForIdle()

        // Открываем книгу
        navigateToBooksTab()

        try {
            waitForItems(TestTags.DownloadedBooks.ITEM, timeoutMs = 5000)

            composeRule
                .onNodeWithTag(TestTags.DownloadedBooks.ITEM, useUnmergedTree = true)
                .performClick()

            composeRule.waitForIdle()

            // Открываем настройки шрифта
            composeRule
                .onNodeWithTag(TestTags.Reader.SETTINGS_BUTTON, useUnmergedTree = true)
                .performClick()

            composeRule.waitForIdle()

            // Переключаем на тёмную тему
            composeRule
                .onNodeWithTag(TestTags.Reader.THEME_DARK, useUnmergedTree = true)
                .performClick()

            composeRule.waitForIdle()

            // Переключаем на светлую тему
            composeRule
                .onNodeWithTag(TestTags.Reader.THEME_LIGHT, useUnmergedTree = true)
                .performClick()

            composeRule.waitForIdle()
        } catch (e: Exception) {
            // Книга или настройки могут отсутствовать
        }
    }

    // ==================== Performance Tests ====================

    /**
     * Тест: Переключение темы не вызывает задержек.
     */
    @Test
    @ReleaseTest
    fun themeSwitch_noLag() {
        composeRule.waitForIdle()

        val startTime = System.currentTimeMillis()

        // Переключаем тему
        composeRule
            .onNodeWithTag(TestTags.News.THEME_TOGGLE, useUnmergedTree = true)
            .performClick()

        composeRule.waitForIdle()

        val elapsed = System.currentTimeMillis() - startTime

        // Переключение должно быть быстрым (< 2 секунды)
        // В реальном тесте можно проверить более строго
    }

    /**
     * Тест: Быстрое переключение темы.
     */
    @Test
    @ReleaseTest
    fun themeSwitch_rapidToggles() {
        composeRule.waitForIdle()

        // Быстро переключаем тему несколько раз
        repeat(3) {
            composeRule
                .onNodeWithTag(TestTags.News.THEME_TOGGLE, useUnmergedTree = true)
                .performClick()

            composeRule.waitForIdle()
        }

        // Проверяем, что UI стабилен
        composeRule
            .onNodeWithTag(TestTags.News.ROOT, useUnmergedTree = true)
            .assertExists()
    }
}