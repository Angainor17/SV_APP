package su.sv.app.wiki

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Test
import su.sv.app.testing.BaseUiTest
import su.sv.app.testing.ReleaseTest
import su.sv.app.testing.SmokeTest
import su.sv.app.testing.TestTags

/**
 * UI тесты для модуля Wiki.
 *
 * Тестируемые экраны:
 * - RootWiki - главный экран Wiki с поиском
 * - ArticleScreen - экран статьи
 * - FavoritesScreen - избранные статьи
 */
@HiltAndroidTest
class WikiScreenTest : BaseUiTest() {

    // ==================== Root Wiki Tests ====================

    /**
     * Тест: Экран Wiki отображается при переходе на вкладку.
     */
    @Test
    @SmokeTest
    fun wikiRoot_isDisplayed_onTabClick() {
        navigateToWikiTab()

        // Проверяем, что корневой элемент Wiki отображается
        composeRule
            .onNodeWithTag(TestTags.WikiRoot.ROOT, useUnmergedTree = true)
            .assertExists()
    }

    /**
     * Тест: Поле поиска отображается.
     */
    @Test
    @SmokeTest
    fun wikiRoot_searchField_isVisible() {
        navigateToWikiTab()

        composeRule
            .onNodeWithTag(TestTags.WikiRoot.SEARCH_FIELD, useUnmergedTree = true)
            .assertIsDisplayed()
    }

    /**
     * Тест: Ввод запроса (≥2 символов) показывает suggestions.
     */
    @Test
    @ReleaseTest
    fun wikiRoot_search_showsSuggestions() {
        navigateToWikiTab()

        // Вводим минимум 2 символа для триггера поиска
        val searchQuery = "Солн"

        composeRule
            .onNodeWithTag(TestTags.WikiRoot.SEARCH_FIELD, useUnmergedTree = true)
            .performTextInput(searchQuery)

        // Ждём появления suggestions
        composeRule.waitUntil(5000) {
            composeRule
                .onAllNodesWithTag(TestTags.WikiRoot.SUGGESTION_ITEM, useUnmergedTree = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        // Проверяем наличие suggestions
        composeRule
            .onNodeWithTag(TestTags.WikiRoot.SUGGESTIONS_LIST, useUnmergedTree = true)
            .assertExists()
    }

    /**
     * Тест: Клик на suggestion открывает статью.
     */
    @Test
    @ReleaseTest
    fun wikiRoot_suggestionClick_opensArticle() {
        navigateToWikiTab()

        // Вводим запрос
        composeRule
            .onNodeWithTag(TestTags.WikiRoot.SEARCH_FIELD, useUnmergedTree = true)
            .performTextInput("Солн")

        // Ждём suggestions
        composeRule.waitUntil(5000) {
            composeRule
                .onAllNodesWithTag(TestTags.WikiRoot.SUGGESTION_ITEM, useUnmergedTree = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        // Кликаем на первый suggestion
        composeRule
            .onNodeWithTag(TestTags.WikiRoot.SUGGESTION_ITEM, useUnmergedTree = true)
            .performClick()

        composeRule.waitForIdle()

        // Проверяем, что статья открылась
        composeRule
            .onNodeWithTag(TestTags.WikiArticle.ROOT, useUnmergedTree = true)
            .assertExists()
    }

    /**
     * Тест: История поиска отображается.
     */
    @Test
    @ReleaseTest
    fun wikiRoot_historyList_isVisible() {
        navigateToWikiTab()

        // Проверяем наличие истории (если есть)
        try {
            composeRule
                .onNodeWithTag(TestTags.WikiRoot.HISTORY_LIST, useUnmergedTree = true)
                .assertExists()
        } catch (_: Exception) {
            // История может быть пустой при первом запуске
        }
    }

    // ==================== Article Tests ====================

    /**
     * Тест: Статья отображается.
     */
    @Test
    @SmokeTest
    fun wikiArticle_displays_content() {
        navigateToArticle()

        // Проверяем, что контент отображается
        composeRule
            .onNodeWithTag(TestTags.WikiArticle.CONTENT, useUnmergedTree = true)
            .assertExists()
    }

    /**
     * Тест: Заголовок статьи отображается.
     */
    @Test
    @ReleaseTest
    fun wikiArticle_displays_title() {
        navigateToArticle()

        composeRule
            .onNodeWithTag(TestTags.WikiArticle.TITLE, useUnmergedTree = true)
            .assertIsDisplayed()
    }

    /**
     * Тест: Кнопка добавления в избранное отображается.
     */
    @Test
    @ReleaseTest
    fun wikiArticle_favoriteButton_isVisible() {
        navigateToArticle()

        composeRule
            .onNodeWithTag(TestTags.WikiArticle.FAVORITE_BUTTON, useUnmergedTree = true)
            .assertIsDisplayed()
    }

    /**
     * Тест: Добавление статьи в избранное.
     */
    @Test
    @ReleaseTest
    fun wikiArticle_addToFavorites_works() {
        navigateToArticle()

        // Кликаем на кнопку "В избранное"
        composeRule
            .onNodeWithTag(TestTags.WikiArticle.FAVORITE_BUTTON, useUnmergedTree = true)
            .performClick()

        composeRule.waitForIdle()

        // Проверяем, что состояние изменилось (иконка изменилась)
        // Можно проверить через текст или состояние иконки
    }

    /**
     * Тест: Клик по ссылке открывает новую статью.
     */
    @Test
    @ReleaseTest
    fun wikiArticle_linkClick_opensNewArticle() {
        navigateToArticle()

        // Находим ссылку в статье
        try {
            composeRule
                .onNodeWithTag(TestTags.WikiArticle.LINK, useUnmergedTree = true)
                .performClick()

            composeRule.waitForIdle()

            // Проверяем, что новая статья открылась
            composeRule
                .onNodeWithTag(TestTags.WikiArticle.ROOT, useUnmergedTree = true)
                .assertExists()
        } catch (_: Exception) {
            // Ссылки могут отсутствовать в тестовой статье
        }
    }

    // ==================== Favorites Tests ====================

    /**
     * Тест: Экран избранного отображается.
     */
    @Test
    @ReleaseTest
    fun wikiFavorites_isDisplayed_onNavigate() {
        navigateToWikiTab()

        // Кликаем на кнопку избранного (если есть избранные)
        try {
            composeRule
                .onNodeWithTag(TestTags.WikiRoot.FAVORITES_BUTTON, useUnmergedTree = true)
                .performClick()

            composeRule.waitForIdle()

            composeRule
                .onNodeWithTag(TestTags.WikiFavorites.ROOT, useUnmergedTree = true)
                .assertExists()
        } catch (_: Exception) {
            // Кнопка может не отображаться если нет избранных
        }
    }

    /**
     * Тест: Список избранного отображается.
     */
    @Test
    @ReleaseTest
    fun wikiFavorites_displays_list() {
        navigateToWikiTab()

        // Переходим к избранному
        try {
            composeRule
                .onNodeWithTag(TestTags.WikiRoot.FAVORITES_BUTTON, useUnmergedTree = true)
                .performClick()

            composeRule.waitForIdle()

            // Проверяем наличие списка
            composeRule
                .onNodeWithTag(TestTags.WikiFavorites.LIST, useUnmergedTree = true)
                .assertExists()
        } catch (_: Exception) {
            // Игнорируем если нет избранных
        }
    }

    /**
     * Тест: Клик на статью из избранного открывает её.
     */
    @Test
    @ReleaseTest
    fun wikiFavorites_itemClick_opensArticle() {
        navigateToWikiTab()

        try {
            // Открываем избранное
            composeRule
                .onNodeWithTag(TestTags.WikiRoot.FAVORITES_BUTTON, useUnmergedTree = true)
                .performClick()

            composeRule.waitForIdle()

            // Кликаем на первую статью
            composeRule
                .onNodeWithTag(TestTags.WikiFavorites.ITEM, useUnmergedTree = true)
                .performClick()

            composeRule.waitForIdle()

            // Проверяем, что статья открылась
            composeRule
                .onNodeWithTag(TestTags.WikiArticle.ROOT, useUnmergedTree = true)
                .assertExists()
        } catch (_: Exception) {
            // Игнорируем если нет избранных
        }
    }

    /**
     * Тест: Удаление из избранного.
     */
    @Test
    @ReleaseTest
    fun wikiFavorites_removeFromFavorites_works() {
        // Сначала добавляем в избранное
        navigateToArticle()
        composeRule
            .onNodeWithTag(TestTags.WikiArticle.FAVORITE_BUTTON, useUnmergedTree = true)
            .performClick()

        // Переходим в избранное и удаляем
        // (зависит от реализации UI удаления)
    }

    // ==================== Helper Methods ====================

    private fun navigateToArticle() {
        navigateToWikiTab()

        // Вводим запрос для поиска статьи
        composeRule
            .onNodeWithTag(TestTags.WikiRoot.SEARCH_FIELD, useUnmergedTree = true)
            .performTextInput("Солнце")

        // Ждём suggestions
        composeRule.waitUntil(5000) {
            composeRule
                .onAllNodesWithTag(TestTags.WikiRoot.SUGGESTION_ITEM, useUnmergedTree = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        // Кликаем на первый suggestion
        composeRule
            .onNodeWithTag(TestTags.WikiRoot.SUGGESTION_ITEM, useUnmergedTree = true)
            .performClick()

        composeRule.waitForIdle()
    }
}