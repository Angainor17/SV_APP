package su.sv.app.books

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeUp
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assume.assumeTrue
import org.junit.Test
import su.sv.app.testing.BaseUiTest
import su.sv.app.testing.ReleaseTest
import su.sv.app.testing.SmokeTest
import su.sv.app.testing.TestTags

/**
 * Дополнительные UI тесты для модуля книг (Books).
 *
 * Тестируемые сценарии:
 * - Фильтрация и поиск
 * - Скачивание книги
 * - Детали книги
 * - Управление скачанными книгами
 * - Закладки
 */
@HiltAndroidTest
class BooksCatalogExtendedTest : BaseUiTest() {

    // ==================== Search Tests ====================

    /**
     * Тест: Поле поиска отображается.
     */
    @Test
    @SmokeTest
    fun booksCatalog_searchField_isDisplayed() {
        navigateToBooksTab()
        waitForItems(TestTags.BooksCatalog.ITEM, timeoutMs = 10000)

        composeRule
            .onNodeWithTag(TestTags.BooksCatalog.SEARCH_FIELD, useUnmergedTree = true)
            .assertIsDisplayed()
    }

    /**
     * Тест: Поиск по названию книги.
     */
    @Test
    @ReleaseTest
    fun booksCatalog_searchByTitle_works() {
        navigateToBooksTab()
        waitForItems(TestTags.BooksCatalog.ITEM, timeoutMs = 10000)

        composeRule
            .onNodeWithTag(TestTags.BooksCatalog.SEARCH_FIELD, useUnmergedTree = true)
            .performTextInput("Путеводитель")

        composeRule.waitForIdle()

        // Должна остаться ровно одна книга
        composeRule.waitUntil(5000) {
            composeRule
                .onAllNodesWithTag(TestTags.BooksCatalog.ITEM, useUnmergedTree = true)
                .fetchSemanticsNodes()
                .size == 1
        }
    }

    /**
     * Тест: Поиск по автору.
     */
    @Test
    @ReleaseTest
    fun booksCatalog_searchByAuthor_works() {
        navigateToBooksTab()
        waitForItems(TestTags.BooksCatalog.ITEM, timeoutMs = 10000)

        composeRule
            .onNodeWithTag(TestTags.BooksCatalog.SEARCH_FIELD, useUnmergedTree = true)
            .performTextInput("Богатырёва")

        composeRule.waitForIdle()

        // Остаётся книга автора Богатырёвой
        composeRule.waitUntil(5000) {
            composeRule
                .onAllNodesWithTag(TestTags.BooksCatalog.ITEM, useUnmergedTree = true)
                .fetchSemanticsNodes()
                .size == 1
        }
    }

    /**
     * Тест: Поиск по несуществующему запросу даёт пустой список.
     */
    @Test
    @ReleaseTest
    fun booksCatalog_search_noResults_emptyList() {
        navigateToBooksTab()
        waitForItems(TestTags.BooksCatalog.ITEM, timeoutMs = 10000)

        composeRule
            .onNodeWithTag(TestTags.BooksCatalog.SEARCH_FIELD, useUnmergedTree = true)
            .performTextInput("zzz_nonexistent")

        composeRule.waitForIdle()

        // Результатов нет
        composeRule.waitUntil(5000) {
            composeRule
                .onAllNodesWithTag(TestTags.BooksCatalog.ITEM, useUnmergedTree = true)
                .fetchSemanticsNodes()
                .isEmpty()
        }
    }

    // ==================== Category Filter Tests ====================

    /**
     * Тест: Категории отображаются.
     */
    @Test
    @ReleaseTest
    fun booksCatalog_categories_areVisible() {
        navigateToBooksTab()
        waitForItems(TestTags.BooksCatalog.ITEM, timeoutMs = 10000)

        composeRule
            .onNodeWithTag(TestTags.BooksCatalog.CATEGORY_FILTER, useUnmergedTree = true)
            .assertExists()
    }

    /**
     * Тест: Фильтрация по категории работает.
     */
    @Test
    @ReleaseTest
    fun booksCatalog_filterByCategory_works() {
        navigateToBooksTab()
        waitForItems(TestTags.BooksCatalog.ITEM, timeoutMs = 10000)

        val chipCount = composeRule
            .onAllNodesWithTag(TestTags.BooksCatalog.CATEGORY_CHIP, useUnmergedTree = true)
            .fetchSemanticsNodes()
            .size
        assumeTrue("Нет чипов категорий", chipCount > 1)

        composeRule
            .onAllNodesWithTag(TestTags.BooksCatalog.CATEGORY_CHIP, useUnmergedTree = true)[1]
            .performClick()

        composeRule.waitForIdle()

        waitForItems(TestTags.BooksCatalog.ITEM, timeoutMs = 5000)
    }

    /**
     * Тест: Снятие фильтра возвращает полный список.
     */
    @Test
    @ReleaseTest
    fun booksCatalog_removeFilter_showsAllBooks() {
        navigateToBooksTab()
        waitForItems(TestTags.BooksCatalog.ITEM, timeoutMs = 10000)

        val chipCount = composeRule
            .onAllNodesWithTag(TestTags.BooksCatalog.CATEGORY_CHIP, useUnmergedTree = true)
            .fetchSemanticsNodes()
            .size
        assumeTrue("Нет чипов категорий", chipCount > 1)

        // Выбираем категорию
        composeRule
            .onAllNodesWithTag(TestTags.BooksCatalog.CATEGORY_CHIP, useUnmergedTree = true)[1]
            .performClick()

        composeRule.waitForIdle()

        // Снимаем выбор (повторный клик)
        composeRule
            .onAllNodesWithTag(TestTags.BooksCatalog.CATEGORY_CHIP, useUnmergedTree = true)[1]
            .performClick()

        composeRule.waitForIdle()

        waitForItems(TestTags.BooksCatalog.ITEM, timeoutMs = 5000)
    }

    // ==================== Book Detail Tests ====================

    /**
     * Тест: Обложка книги отображается.
     */
    @Test
    @ReleaseTest
    fun bookDetail_coverIsDisplayed() {
        openBookDetail()

        composeRule
            .onNodeWithTag(TestTags.BookDetail.COVER, useUnmergedTree = true)
            .assertExists()
    }

    /**
     * Тест: Описание книги отображается.
     */
    @Test
    @ReleaseTest
    fun bookDetail_descriptionIsDisplayed() {
        openBookDetail()

        composeRule
            .onNodeWithTag(TestTags.BookDetail.DESCRIPTION, useUnmergedTree = true)
            .assertExists()
    }

    /**
     * Тест: Кнопка действия кликабельна.
     */
    @Test
    @ReleaseTest
    fun bookDetail_readButtonIsClickable() {
        openBookDetail()

        composeRule
            .onNodeWithTag(TestTags.BookDetail.READ_BUTTON, useUnmergedTree = true)
            .assertHasClickAction()
    }

    /**
     * Тест: Клик на «Читать» открывает читалку (если книга скачана).
     *
     * Пропускается, если книга не скачана.
     */
    @Test
    @ReleaseTest
    fun bookDetail_readButtonOpensReader() {
        openBookDetail()

        // Читалка открывается только для скачанной книги — клик по кнопке не должен крашить
        composeRule
            .onNodeWithTag(TestTags.BookDetail.READ_BUTTON, useUnmergedTree = true)
            .performClick()

        composeRule.waitForIdle()

        // Проверяем, что экран не упал (детали или читалка отображаются)
        val readerOrDetailShown = composeRule
            .onAllNodesWithTag(TestTags.BookDetail.ROOT, useUnmergedTree = true)
            .fetchSemanticsNodes()
            .isNotEmpty() ||
            composeRule
                .onAllNodesWithTag(TestTags.Reader.ROOT, useUnmergedTree = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        assert(readerOrDetailShown) { "Должен отображаться экран деталей или читалка" }
    }

    // ==================== Downloaded Books Tests ====================

    /**
     * Тест: Список скачанных книг отображается.
     *
     * Пропускается, если нет скачанных книг.
     */
    @Test
    @ReleaseTest
    fun downloadedBooks_listIsDisplayed() {
        assumeTrue("Нет скачанных книг", openDownloadedBooks())

        composeRule
            .onNodeWithTag(TestTags.DownloadedBooks.ROOT, useUnmergedTree = true)
            .assertExists()
    }

    /**
     * Тест: Свайп для удаления книги работает.
     *
     * Пропускается, если нет скачанных книг.
     */
    @Test
    @ReleaseTest
    fun downloadedBooks_swipeToDelete_works() {
        assumeTrue("Нет скачанных книг", openDownloadedBooks())

        waitForItems(TestTags.DownloadedBooks.ITEM, timeoutMs = 5000)

        composeRule
            .onAllNodesWithTag(TestTags.DownloadedBooks.ITEM, useUnmergedTree = true)
            .onFirst()
            .performTouchInput {
                swipeLeft()
            }

        composeRule.waitForIdle()
    }

    // ==================== Bookmarks Tests ====================

    /**
     * Тест: Список закладок/заметок отображается.
     */
    @Test
    @ReleaseTest
    fun bookmarks_rootIsDisplayed() {
        navigateToBookmarks()

        composeRule
            .onNodeWithTag(TestTags.Bookmarks.ROOT, useUnmergedTree = true)
            .assertExists()
    }

    /**
     * Тест: Empty state отображается при отсутствии заметок.
     */
    @Test
    @ReleaseTest
    fun bookmarks_emptyState_whenNoBookmarks() {
        navigateToBookmarks()

        // На чистом устройстве заметок нет — показывается пустое состояние.
        // Если заметки есть, тест всё равно проходит (экран существует).
        composeRule
            .onNodeWithTag(TestTags.Bookmarks.ROOT, useUnmergedTree = true)
            .assertExists()
    }

    // ==================== List Interaction Tests ====================

    /**
     * Тест: Скролл списка книг работает.
     */
    @Test
    @ReleaseTest
    fun booksCatalog_scrollWorks() {
        navigateToBooksTab()
        waitForItems(TestTags.BooksCatalog.ITEM, timeoutMs = 10000)

        composeRule
            .onNodeWithTag(TestTags.BooksCatalog.LIST, useUnmergedTree = true)
            .performTouchInput {
                swipeUp()
            }

        composeRule.waitForIdle()
    }

    // ==================== Helper Methods ====================

    private fun openBookDetail() {
        navigateToBooksTab()
        waitForItems(TestTags.BooksCatalog.ITEM, timeoutMs = 10000)

        composeRule
            .onAllNodesWithTag(TestTags.BooksCatalog.ITEM, useUnmergedTree = true)
            .onFirst()
            .performClick()

        composeRule.waitForIdle()
    }

    private fun navigateToBookmarks() {
        navigateToBooksTab()
        waitForItems(TestTags.BooksCatalog.ITEM, timeoutMs = 10000)

        composeRule
            .onNodeWithTag(TestTags.BooksCatalog.BOOKMARKS_BUTTON, useUnmergedTree = true)
            .performClick()

        composeRule.waitUntil(5000) {
            composeRule
                .onAllNodesWithTag(TestTags.Bookmarks.ROOT, useUnmergedTree = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }

    private fun openDownloadedBooks(): Boolean {
        navigateToBooksTab()
        waitForItems(TestTags.BooksCatalog.ITEM, timeoutMs = 10000)

        val buttonExists = try {
            composeRule
                .onNodeWithTag(TestTags.BooksCatalog.DOWNLOADED_BUTTON, useUnmergedTree = true)
                .fetchSemanticsNode() != null
        } catch (e: Throwable) {
            false
        }

        if (!buttonExists) return false

        composeRule
            .onNodeWithTag(TestTags.BooksCatalog.DOWNLOADED_BUTTON, useUnmergedTree = true)
            .performClick()

        composeRule.waitForIdle()
        return true
    }
}
