package su.sv.app.info

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
 * UI тесты для модуля информации (Info).
 *
 * Тестируемые сценарии:
 * - Отображение экрана информации
 * - Список ссылок
 * - Открытие ссылок в браузере
 */
@HiltAndroidTest
class InfoScreenTest : BaseUiTest() {

    // ==================== Info Screen Tests ====================

    /**
     * Тест: Экран Info отображается при переходе на вкладку.
     */
    @Test
    @SmokeTest
    fun infoScreen_isDisplayed_onTabClick() {
        navigateToInfoTab()

        // Проверяем, что корневой элемент Info отображается
        composeRule
            .onNodeWithTag(TestTags.Info.ROOT, useUnmergedTree = true)
            .assertExists()
    }

    /**
     * Тест: Список ссылок отображается.
     */
    @Test
    @SmokeTest
    fun infoScreen_linksList_isDisplayed() {
        navigateToInfoTab()

        composeRule
            .onNodeWithTag(TestTags.Info.LINKS_LIST, useUnmergedTree = true)
            .assertIsDisplayed()
    }

    /**
     * Тест: Элементы ссылок отображаются.
     */
    @Test
    @ReleaseTest
    fun infoScreen_linkItems_areVisible() {
        navigateToInfoTab()

        // Проверяем наличие хотя бы одной ссылки
        composeRule
            .onNodeWithTag(TestTags.Info.LINK_ITEM, useUnmergedTree = true)
            .assertExists()
    }

    /**
     * Тест: Версия приложения отображается.
     */
    @Test
    @ReleaseTest
    fun infoScreen_version_isDisplayed() {
        navigateToInfoTab()

        composeRule
            .onNodeWithTag(TestTags.Info.VERSION, useUnmergedTree = true)
            .assertExists()
    }

    /**
     * Тест: Клик на ссылку открывает её в браузере.
     *
     * Примечание: Проверяет только клик, не сам переход в браузер.
     */
    @Test
    @ReleaseTest
    fun infoScreen_linkClick_works() {
        navigateToInfoTab()

        // Кликаем на первую ссылку
        composeRule
            .onNodeWithTag(TestTags.Info.LINK_ITEM, useUnmergedTree = true)
            .performClick()

        // После клика должно открыться внешнее приложение (браузер/Telegram)
        // Проверить это в UI тесте сложно, поэтому просто проверяем отсутствие краша
    }
}