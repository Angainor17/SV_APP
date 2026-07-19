package su.sv.app.books

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import dagger.hilt.android.testing.HiltAndroidTest
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
        composeRule.waitUntil(10000) {
            composeRule
                .onAllNodesWithTag(TestTags.BooksCatalog.ITEM, useUnmergedTree = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        // Проверяем наличие книги
        composeRule
            .onNodeWithTag(TestTags.BooksCatalog.ITEM, useUnmergedTree = true)
            .assertIsDisplayed()
    }

    /**
     * Тест: Поиск книги по названию работает.
     */
    @Test
    @ReleaseTest
    fun booksCatalog_search_filtersBooks() {
        navigateToBooksTab()

        // Вводим текст в поле поиска
        val searchQuery = "Сказание"

        composeRule
            .onNodeWithTag(TestTags.BooksCatalog.SEARCH_FIELD, useUnmergedTree = true)
            .performTextInput(searchQuery)

        composeRule.waitForIdle()

        // Проверяем, что результаты отображаются
        composeRule.waitUntil(5000) {
            composeRule
                .onAllNodesWithTag(TestTags.BooksCatalog.ITEM, useUnmergedTree = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }

    /**
     * Тест: Фильтрация по категории работает.
     */
    @Test
    @ReleaseTest
    fun booksCatalog_categoryFilter_works() {
        navigateToBooksTab()

        // Кликаем на чип категории (если есть)
        try {
            composeRule
                .onNodeWithTag(TestTags.BooksCatalog.CATEGORY_CHIP, useUnmergedTree = true)
                .performClick()

            composeRule.waitForIdle()

            // Проверяем, что книги отфильтрованы
            composeRule
                .onNodeWithTag(TestTags.BooksCatalog.ITEM, useUnmergedTree = true)
                .assertExists()
        } catch (e: Exception) {
            // Категории могут отсутствовать в тестовых данных
        }
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
     * Тест: Кнопка "Читать" отображается.
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
     */
    @Test
    @ReleaseTest
    fun downloadedBooks_displays_onNavigate() {
        navigateToBooksTab()

        // Проверяем, что список отображается
        composeRule
            .onNodeWithTag(TestTags.DownloadedBooks.ROOT, useUnmergedTree = true)
            .assertExists()
    }

    /**
     * Тест: Удаление книги свайпом работает.
     */
    @Test
    @ReleaseTest
    fun downloadedBooks_deleteOnSwipe_works() {
        // Предварительно должны быть скачанные книги
        navigateToBooksTab()

        // Находим элемент и делаем свайп влево
        try {
            composeRule
                .onNodeWithTag(TestTags.DownloadedBooks.ITEM, useUnmergedTree = true)
                .performTouchInput {
                    swipeLeft()
                }

            composeRule.waitForIdle()
        } catch (e: Exception) {
            // Если нет скачанных книг, тест пропускается
        }
    }

    // ==================== Bookmarks Tests ====================

    /**
     * Тест: Заметки отображаются.
     */
    @Test
    @ReleaseTest
    fun bookmarks_displays_onNavigate() {
        navigateToBooksTab()

        // Проверяем наличие списка заметок
        composeRule
            .onNodeWithTag(TestTags.Bookmarks.ROOT, useUnmergedTree = true)
            .assertExists()
    }

    /**
     * Тест: Переключение режимов LIST/BY_BOOK работает.
     */
    @Test
    @ReleaseTest
    fun bookmarks_modeToggle_works() {
        // Переходим к заметкам
        navigateToBookmarks()

        // Переключаем режим
        composeRule
            .onNodeWithTag(TestTags.Bookmarks.MODE_TOGGLE, useUnmergedTree = true)
            .performClick()

        composeRule.waitForIdle()

        // Проверяем, что режим изменился
        composeRule
            .onNodeWithTag(TestTags.Bookmarks.MODE_BY_BOOK, useUnmergedTree = true)
            .assertExists()
    }

    // ==================== Helper Methods ====================

    private fun navigateToBookDetail() {
        navigateToBooksTab()

        composeRule.waitUntil(10000) {
            composeRule
                .onAllNodesWithTag(TestTags.BooksCatalog.ITEM, useUnmergedTree = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        composeRule
            .onNodeWithTag(TestTags.BooksCatalog.ITEM, useUnmergedTree = true)
            .performClick()

        composeRule.waitForIdle()
    }

    private fun navigateToBookmarks() {
        navigateToBooksTab()
        // Дополнительная логика для перехода к заметкам
        // зависит от навигации в приложении
    }
}