package su.sv.app.testing

import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput

/**
 * Общие действия для экранов приложения.
 * Содержит повторяемые операции (навигация, поиск, взаимодействие с UI).
 *
 * Использование:
 * ```kotlin
 * val screenActions = ScreenActions(composeRule)
 * screenActions.navigateToNews()
 * screenActions.searchWiki("Солнце")
 * ```
 */
class ScreenActions(private val rule: ComposeTestRule) {

    // ==================== Навигация ====================

    /** Переход на вкладку News */
    fun navigateToNews() {
        rule.waitForIdle()
        rule.onNodeWithTag(TestTags.BottomNav.TAB_NEWS, useUnmergedTree = true).performClick()
        rule.waitForIdle()
    }

    /** Переход на вкладку Books */
    fun navigateToBooks() {
        rule.waitForIdle()
        rule.onNodeWithTag(TestTags.BottomNav.TAB_BOOKS, useUnmergedTree = true).performClick()
        rule.waitForIdle()
    }

    /** Переход на вкладку Wiki */
    fun navigateToWiki() {
        rule.waitForIdle()
        rule.onNodeWithTag(TestTags.BottomNav.TAB_WIKI, useUnmergedTree = true).performClick()
        rule.waitForIdle()
    }

    /** Переход на вкладку Info */
    fun navigateToInfo() {
        rule.waitForIdle()
        rule.onNodeWithTag(TestTags.BottomNav.TAB_INFO, useUnmergedTree = true).performClick()
        rule.waitForIdle()
    }

    /** Последовательный переход по всем вкладкам */
    fun navigateThroughAllTabs() {
        navigateToNews()
        navigateToBooks()
        navigateToWiki()
        navigateToInfo()
    }

    // ==================== Навигация назад ====================

    /** Нажатие кнопки "Назад" */
    fun pressBack() {
        rule.onNodeWithTag(TestTags.Common.BACK_BUTTON, useUnmergedTree = true).performClick()
        rule.waitForIdle()
    }

    // ==================== Wiki действия ====================

    /** Поиск в Wiki с ожиданием результатов */
    fun searchWiki(query: String, waitForResults: Boolean = true) {
        navigateToWiki()

        rule.onNodeWithTag(TestTags.WikiRoot.SEARCH_FIELD, useUnmergedTree = true)
            .performTextInput(query)

        if (waitForResults) {
            rule.waitUntil(5000) {
                rule.onAllNodesWithTag(TestTags.WikiRoot.SUGGESTION_ITEM, useUnmergedTree = true)
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
        }
    }

    /** Открытие первой статьи из результатов поиска Wiki */
    fun openFirstWikiArticle() {
        rule.onNodeWithTag(TestTags.WikiRoot.SUGGESTION_ITEM, useUnmergedTree = true).performClick()
        rule.waitForIdle()
    }

    /** Добавление текущей статьи в избранное */
    fun addToWikiFavorites() {
        rule.onNodeWithTag(TestTags.WikiArticle.FAVORITE_BUTTON, useUnmergedTree = true).performClick()
        rule.waitForIdle()
    }

    /** Переход в избранное Wiki */
    fun openWikiFavorites() {
        rule.onNodeWithTag(TestTags.WikiRoot.FAVORITES_BUTTON, useUnmergedTree = true).performClick()
        rule.waitForIdle()
    }

    // ==================== Books действия ====================

    /** Поиск книги по названию */
    fun searchBooks(query: String) {
        navigateToBooks()

        rule.onNodeWithTag(TestTags.BooksCatalog.SEARCH_FIELD, useUnmergedTree = true)
            .performTextInput(query)

        rule.waitForIdle()
    }

    /** Открытие первой книги из каталога */
    fun openFirstBook(waitForLoad: Boolean = true) {
        navigateToBooks()

        rule.waitUntil(10000) {
            rule.onAllNodesWithTag(TestTags.BooksCatalog.ITEM, useUnmergedTree = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        rule.onNodeWithTag(TestTags.BooksCatalog.ITEM, useUnmergedTree = true).performClick()
        rule.waitForIdle()
    }

    /** Открытие книги для чтения (из скачанных) */
    fun openBookForReading() {
        navigateToBooks()

        // Переходим к скачанным книгам
        // Примечание: зависит от UI навигации

        rule.waitUntil(5000) {
            rule.onAllNodesWithTag(TestTags.DownloadedBooks.ITEM, useUnmergedTree = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        rule.onNodeWithTag(TestTags.DownloadedBooks.ITEM, useUnmergedTree = true).performClick()
        rule.waitForIdle()
    }

    // ==================== Reader действия ====================

    /** Открытие TOC (оглавления) */
    fun openReaderToc() {
        rule.onNodeWithTag(TestTags.Reader.TOC_BUTTON, useUnmergedTree = true).performClick()
        rule.waitForIdle()
    }

    /** Открытие поиска в книге */
    fun openReaderSearch() {
        rule.onNodeWithTag(TestTags.Reader.SEARCH_BUTTON, useUnmergedTree = true).performClick()
        rule.waitForIdle()
    }

    /** Поиск в книге */
    fun searchInBook(query: String) {
        openReaderSearch()
        rule.onNodeWithTag(TestTags.Reader.SEARCH_FIELD, useUnmergedTree = true)
            .performTextInput(query)
        rule.waitForIdle()
    }

    /** Открытие настроек шрифта */
    fun openReaderFontSettings() {
        rule.onNodeWithTag(TestTags.Reader.SETTINGS_BUTTON, useUnmergedTree = true).performClick()
        rule.waitForIdle()
    }

    /** Увеличение размера шрифта */
    fun increaseFontSize() {
        rule.onNodeWithTag(TestTags.Reader.FONT_SIZE_UP, useUnmergedTree = true).performClick()
        rule.waitForIdle()
    }

    /** Уменьшение размера шрифта */
    fun decreaseFontSize() {
        rule.onNodeWithTag(TestTags.Reader.FONT_SIZE_DOWN, useUnmergedTree = true).performClick()
        rule.waitForIdle()
    }

    /** Переключение на тёмную тему читалки */
    fun switchToDarkTheme() {
        rule.onNodeWithTag(TestTags.Reader.THEME_DARK, useUnmergedTree = true).performClick()
        rule.waitForIdle()
    }

    /** Переключение на светлую тему читалки */
    fun switchToLightTheme() {
        rule.onNodeWithTag(TestTags.Reader.THEME_LIGHT, useUnmergedTree = true).performClick()
        rule.waitForIdle()
    }

    /** Открытие диалога закладок */
    fun openBookmarksDialog() {
        rule.onNodeWithTag(TestTags.Reader.BOOKMARKS_LIST_BUTTON, useUnmergedTree = true).performClick()
        rule.waitForIdle()
    }

    /** Создание закладки */
    fun createBookmark(title: String) {
        rule.onNodeWithTag(TestTags.Reader.BOOKMARK_BUTTON, useUnmergedTree = true).performClick()
        rule.waitForIdle()

        rule.onNodeWithTag(TestTags.Reader.BOOKMARK_TITLE_FIELD, useUnmergedTree = true)
            .performTextInput(title)

        rule.onNodeWithTag(TestTags.Reader.BOOKMARK_SAVE_BUTTON, useUnmergedTree = true).performClick()
        rule.waitForIdle()
    }

    /** Переход на следующую страницу */
    fun nextPage() {
        rule.onNodeWithTag(TestTags.Reader.PAGE_NEXT, useUnmergedTree = true).performClick()
        rule.waitForIdle()
    }

    /** Переход на предыдущую страницу */
    fun prevPage() {
        rule.onNodeWithTag(TestTags.Reader.PAGE_PREV, useUnmergedTree = true).performClick()
        rule.waitForIdle()
    }

    // ==================== Info действия ====================

    /** Клик по первой ссылке в Info */
    fun clickFirstInfoLink() {
        navigateToInfo()
        rule.onNodeWithTag(TestTags.Info.LINK_ITEM, useUnmergedTree = true).performClick()
        rule.waitForIdle()
    }
}

/**
 * Функция-расширение для создания ScreenActions.
 */
fun ComposeTestRule.screenActions(): ScreenActions = ScreenActions(this)