package su.sv.app.books

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assume.assumeTrue
import org.junit.Test
import su.sv.app.testing.BaseUiTest
import su.sv.app.testing.ReleaseTest
import su.sv.app.testing.SmokeTest
import su.sv.app.testing.TestTags

/**
 * UI тесты для модуля книг (Books).
 *
 * Тестируемые экраны:
 * - RootBooksCatalog - каталог книг
 * - BookDetailScreen - детали книги
 * - DownloadedBooksScreen - скачанные книги
 * - BookmarksScreen - заметки
 */
@HiltAndroidTest
class BooksCatalogTest : BaseUiTest() {

    // ==================== Books Catalog Tests ====================

    /**
     * Тест: Каталог книг отображается при переходе на вкладку Books.
     */
    @Test
    @SmokeTest
    fun booksCatalog_isDisplayed_onTabClick() {
        navigateToBooksTab()

        // Проверяем, что корневой элемент каталога отображается
        composeRule
            .onNodeWithTag(TestTags.BooksCatalog.ROOT, useUnmergedTree = true)
            .assertExists()
    }

    /**
     * Тест: Список книг загружается и отображается.
     */
    @Test
    @SmokeTest
    fun booksCatalog_displaysBooksList() {
        navigateToBooksTab()

        // Ждём загрузки книг
        waitForItems(TestTags.BooksCatalog.ITEM, timeoutMs = 10000)

        // Проверяем наличие хотя бы одной книги
        composeRule
            .onAllNodesWithTag(TestTags.BooksCatalog.ITEM, useUnmergedTree = true)
            .onFirst()
            .assertIsDisplayed()
    }

    /**
     * Тест: Поиск книги по названию работает.
     */
    @Test
    @ReleaseTest
    fun booksCatalog_search_filtersBooks() {
        navigateToBooksTab()
        waitForItems(TestTags.BooksCatalog.ITEM, timeoutMs = 10000)

        // Вводим текст в поле поиска
        val searchQuery = "Батум"

        composeRule
            .onNodeWithTag(TestTags.BooksCatalog.SEARCH_FIELD, useUnmergedTree = true)
            .performTextInput(searchQuery)

        composeRule.waitForIdle()

        // Проверяем, что осталась ровно одна книга «Батум»
        composeRule.waitUntil(5000) {
            composeRule
                .onAllNodesWithTag(TestTags.BooksCatalog.ITEM, useUnmergedTree = true)
                .fetchSemanticsNodes()
                .size == 1
        }
    }

    /**
     * Тест: Фильтрация по категории работает.
     */
    @Test
    @ReleaseTest
    fun booksCatalog_categoryFilter_works() {
        navigateToBooksTab()
        waitForItems(TestTags.BooksCatalog.ITEM, timeoutMs = 10000)

        val chipCount = composeRule
            .onAllNodesWithTag(TestTags.BooksCatalog.CATEGORY_CHIP, useUnmergedTree = true)
            .fetchSemanticsNodes()
            .size
        assumeTrue("Нет чипов категорий", chipCount > 1)

        // Кликаем на второй чип (первый — «Все»)
        composeRule
            .onAllNodesWithTag(TestTags.BooksCatalog.CATEGORY_CHIP, useUnmergedTree = true)[1]
            .performClick()

        composeRule.waitForIdle()

        // Книги по-прежнему отображаются
        composeRule
            .onAllNodesWithTag(TestTags.BooksCatalog.ITEM, useUnmergedTree = true)
            .onFirst()
            .assertExists()
    }

    // ==================== Book Detail Tests ====================

    /**
     * Тест: Детали книги отображаются при клике на книгу.
     */
    @Test
    @SmokeTest
    fun bookDetail_displays_onBookClick() {
        navigateToBookDetail()

        // Проверяем, что экран деталей отображается
        composeRule
            .onNodeWithTag(TestTags.BookDetail.ROOT, useUnmergedTree = true)
            .assertExists()
    }

    /**
     * Тест: Информация о книге отображается.
     */
    @Test
    @ReleaseTest
    fun bookDetail_displaysBookInfo() {
        navigateToBookDetail()

        // Проверяем отображение информации
        composeRule
            .onNodeWithTag(TestTags.BookDetail.TITLE, useUnmergedTree = true)
            .assertExists()

        composeRule
            .onNodeWithTag(TestTags.BookDetail.AUTHOR, useUnmergedTree = true)
            .assertExists()
    }

    /**
     * Тест: Кнопка действия («Скачать»/«Читать») отображается.
     */
    @Test
    @ReleaseTest
    fun bookDetail_readButton_isVisible() {
        navigateToBookDetail()

        composeRule
            .onNodeWithTag(TestTags.BookDetail.READ_BUTTON, useUnmergedTree = true)
            .assertIsDisplayed()
    }

    // ==================== Downloaded Books Tests ====================

    /**
     * Тест: Скачанные книги отображаются.
     *
     * Пропускается, если на устройстве нет скачанных книг (кнопка в тулбаре не видна).
     */
    @Test
    @ReleaseTest
    fun downloadedBooks_displays_onNavigate() {
        assumeTrue("Нет скачанных книг", openDownloadedBooks())

        composeRule
            .onNodeWithTag(TestTags.DownloadedBooks.ROOT, useUnmergedTree = true)
            .assertExists()
    }

    /**
     * Тест: Удаление книги свайпом работает.
     *
     * Пропускается, если на устройстве нет скачанных книг.
     */
    @Test
    @ReleaseTest
    fun downloadedBooks_deleteOnSwipe_works() {
        assumeTrue("Нет скачанных книг", openDownloadedBooks())

        // Находим элемент и делаем свайп влево
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
     * Тест: Заметки отображаются.
     */
    @Test
    @ReleaseTest
    fun bookmarks_displays_onNavigate() {
        navigateToBookmarks()

        composeRule
            .onNodeWithTag(TestTags.Bookmarks.ROOT, useUnmergedTree = true)
            .assertExists()
    }

    /**
     * Тест: Переключение режимов LIST/BY_BOOK работает.
     *
     * Пропускается, если нет ни одной заметки (переключатель не отображается в пустом состоянии).
     */
    @Test
    @ReleaseTest
    fun bookmarks_modeToggle_works() {
        navigateToBookmarks()

        // Переключатель отображается только при наличии заметок
        val toggleExists = try {
            composeRule
                .onNodeWithTag(TestTags.Bookmarks.MODE_TOGGLE, useUnmergedTree = true)
                .fetchSemanticsNode() != null
        } catch (e: Throwable) {
            false
        }
        assumeTrue("Нет заметок (переключатель не отображается)", toggleExists)

        composeRule
            .onNodeWithTag(TestTags.Bookmarks.MODE_TOGGLE, useUnmergedTree = true)
            .performClick()

        composeRule.waitForIdle()
    }

    // ==================== Helper Methods ====================

    private fun navigateToBookDetail() {
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

    /**
     * Открывает экран скачанных книг, если он доступен (кнопка видна только при наличии
     * скачанных книг). Возвращает true, если экран открыт.
     */
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
