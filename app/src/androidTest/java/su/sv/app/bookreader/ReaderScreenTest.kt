package su.sv.app.bookreader

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import su.sv.app.MainActivity
import su.sv.app.testing.ReleaseTest
import su.sv.app.testing.SmokeTest
import su.sv.app.testing.TestTags

/**
 * UI тесты для модуля чтения книг (BookReader).
 *
 * Тестируемые сценарии:
 * - Открытие книги
 * - Отображение текста
 * - Настройки шрифта/темы
 * - Навигация по главам (TOC)
 * - Поиск по тексту
 * - Создание закладки
 *
 * Примечание: Для тестирования нужно предварительно скачать книгу.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ReaderScreenTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setup() {
        hiltRule.inject()
    }

    // ==================== Book Opening Tests ====================

    /**
     * Тест: Книга открывается при клике на скачанную книгу.
     *
     * Предварительные условия:
     * - В приложении должна быть скачанная книга
     */
    @Test
    @SmokeTest
    fun readerScreen_opens_onBookClick() {
        // Переходим к скачанным книгам
        navigateToDownloadedBooks()

        // Ждём загрузки списка
        composeRule.waitUntil(5000) {
            composeRule
                .onAllNodesWithTag(TestTags.DownloadedBooks.ITEM, useUnmergedTree = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        // Кликаем на книгу
        composeRule
            .onNodeWithTag(TestTags.DownloadedBooks.ITEM, useUnmergedTree = true)
            .performClick()

        composeRule.waitForIdle()

        // Проверяем, что читалка открылась
        composeRule
            .onNodeWithTag(TestTags.Reader.ROOT, useUnmergedTree = true)
            .assertExists()
    }

    /**
     * Тест: Текст книги отображается.
     */
    @Test
    @SmokeTest
    fun readerScreen_displays_content() {
        openBook()

        // Проверяем, что контент книги отображается
        composeRule
            .onNodeWithTag(TestTags.Reader.CONTENT, useUnmergedTree = true)
            .assertExists()
    }

    /**
     * Тест: Заголовок книги отображается в TopBar.
     */
    @Test
    @ReleaseTest
    fun readerScreen_displays_bookTitle() {
        openBook()

        composeRule
            .onNodeWithTag(TestTags.Reader.TITLE, useUnmergedTree = true)
            .assertIsDisplayed()
    }

    // ==================== Settings Tests ====================

    /**
     * Тест: Кнопка настроек отображается.
     */
    @Test
    @ReleaseTest
    fun readerScreen_settingsButton_isVisible() {
        openBook()

        composeRule
            .onNodeWithTag(TestTags.Reader.SETTINGS_BUTTON, useUnmergedTree = true)
            .assertIsDisplayed()
    }

    /**
     * Тест: Экран настроек открывается.
     */
    @Test
    @ReleaseTest
    fun readerScreen_settings_opens() {
        openBook()

        // Открываем настройки
        composeRule
            .onNodeWithTag(TestTags.Reader.SETTINGS_BUTTON, useUnmergedTree = true)
            .performClick()

        composeRule.waitForIdle()

        // Проверяем, что настройки открылись
        composeRule
            .onNodeWithTag(TestTags.Reader.FONT_SIZE_UP, useUnmergedTree = true)
            .assertExists()
    }

    /**
     * Тест: Изменение размера шрифта.
     */
    @Test
    @ReleaseTest
    fun readerScreen_fontSize_canBeChanged() {
        openBook()

        // Открываем настройки
        composeRule
            .onNodeWithTag(TestTags.Reader.SETTINGS_BUTTON, useUnmergedTree = true)
            .performClick()

        composeRule.waitForIdle()

        // Увеличиваем шрифт
        composeRule
            .onNodeWithTag(TestTags.Reader.FONT_SIZE_UP, useUnmergedTree = true)
            .performClick()

        composeRule.waitForIdle()

        // Уменьшаем шрифт
        composeRule
            .onNodeWithTag(TestTags.Reader.FONT_SIZE_DOWN, useUnmergedTree = true)
            .performClick()

        // Проверяем отсутствие краша
    }

    /**
     * Тест: Переключение темы (светлая/тёмная).
     */
    @Test
    @ReleaseTest
    fun readerScreen_theme_canBeChanged() {
        openBook()

        // Открываем настройки
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
    }

    // ==================== TOC Navigation Tests ====================

    /**
     * Тест: Кнопка оглавления отображается.
     */
    @Test
    @ReleaseTest
    fun readerScreen_tocButton_isVisible() {
        openBook()

        composeRule
            .onNodeWithTag(TestTags.Reader.TOC_BUTTON, useUnmergedTree = true)
            .assertIsDisplayed()
    }

    /**
     * Тест: Навигация по главам работает.
     */
    @Test
    @ReleaseTest
    fun readerScreen_tocNavigation_works() {
        openBook()

        // Открываем оглавление
        composeRule
            .onNodeWithTag(TestTags.Reader.TOC_BUTTON, useUnmergedTree = true)
            .performClick()

        composeRule.waitForIdle()

        // Проверяем, что список глав отображается
        composeRule
            .onNodeWithTag(TestTags.Reader.TOC_LIST, useUnmergedTree = true)
            .assertExists()

        // Кликаем на главу
        composeRule
            .onNodeWithTag(TestTags.Reader.TOC_ITEM, useUnmergedTree = true)
            .performClick()

        composeRule.waitForIdle()

        // Проверяем, что навигация произошла
        composeRule
            .onNodeWithTag(TestTags.Reader.CONTENT, useUnmergedTree = true)
            .assertExists()
    }

    // ==================== Search Tests ====================

    /**
     * Тест: Кнопка поиска отображается.
     */
    @Test
    @ReleaseTest
    fun readerScreen_searchButton_isVisible() {
        openBook()

        composeRule
            .onNodeWithTag(TestTags.Reader.SEARCH_BUTTON, useUnmergedTree = true)
            .assertIsDisplayed()
    }

    /**
     * Тест: Поиск по тексту книги работает.
     */
    @Test
    @ReleaseTest
    fun readerScreen_search_works() {
        openBook()

        // Открываем поиск
        composeRule
            .onNodeWithTag(TestTags.Reader.SEARCH_BUTTON, useUnmergedTree = true)
            .performClick()

        composeRule.waitForIdle()

        // Вводим текст для поиска
        composeRule
            .onNodeWithTag(TestTags.Reader.SEARCH_FIELD, useUnmergedTree = true)
            .performTextInput("Глава")

        composeRule.waitForIdle()

        // Проверяем, что результаты отображаются
        composeRule.waitUntil(5000) {
            composeRule
                .onAllNodesWithTag(TestTags.Reader.SEARCH_RESULT, useUnmergedTree = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }

    // ==================== Bookmark Tests ====================

    /**
     * Тест: Кнопка закладки отображается.
     */
    @Test
    @ReleaseTest
    fun readerScreen_bookmarkButton_isVisible() {
        openBook()

        composeRule
            .onNodeWithTag(TestTags.Reader.BOOKMARK_BUTTON, useUnmergedTree = true)
            .assertIsDisplayed()
    }

    /**
     * Тест: Создание закладки.
     */
    @Test
    @ReleaseTest
    fun readerScreen_createBookmark_works() {
        openBook()

        // Кликаем на кнопку закладки
        composeRule
            .onNodeWithTag(TestTags.Reader.BOOKMARK_BUTTON, useUnmergedTree = true)
            .performClick()

        composeRule.waitForIdle()

        // Проверяем, что диалог создания закладки открылся
        composeRule
            .onNodeWithTag(TestTags.Reader.BOOKMARK_DIALOG, useUnmergedTree = true)
            .assertExists()

        // Вводим название
        composeRule
            .onNodeWithTag(TestTags.Reader.BOOKMARK_TITLE_FIELD, useUnmergedTree = true)
            .performTextInput("Важное место")

        composeRule.waitForIdle()

        // Сохраняем
        composeRule
            .onNodeWithTag(TestTags.Reader.BOOKMARK_SAVE_BUTTON, useUnmergedTree = true)
            .performClick()

        composeRule.waitForIdle()
    }

    /**
     * Тест: Список закладок открывается.
     */
    @Test
    @ReleaseTest
    fun readerScreen_bookmarksList_opens() {
        openBook()

        // Открываем список закладок
        composeRule
            .onNodeWithTag(TestTags.Reader.BOOKMARKS_LIST_BUTTON, useUnmergedTree = true)
            .performClick()

        composeRule.waitForIdle()

        // Проверяем, что список отображается
        composeRule
            .onNodeWithTag(TestTags.Reader.BOOKMARK_DIALOG, useUnmergedTree = true)
            .assertExists()
    }

    // ==================== Page Navigation Tests ====================

    /**
     * Тест: Навигация на следующую страницу.
     */
    @Test
    @ReleaseTest
    fun readerScreen_nextPage_works() {
        openBook()

        // Кликаем на следующую страницу
        composeRule
            .onNodeWithTag(TestTags.Reader.PAGE_NEXT, useUnmergedTree = true)
            .performClick()

        composeRule.waitForIdle()

        // Проверяем, что контент всё ещё отображается
        composeRule
            .onNodeWithTag(TestTags.Reader.CONTENT, useUnmergedTree = true)
            .assertExists()
    }

    /**
     * Тест: Навигация на предыдущую страницу.
     */
    @Test
    @ReleaseTest
    fun readerScreen_prevPage_works() {
        openBook()

        // Сначала переходим на следующую страницу
        composeRule
            .onNodeWithTag(TestTags.Reader.PAGE_NEXT, useUnmergedTree = true)
            .performClick()

        composeRule.waitForIdle()

        // Затем на предыдущую
        composeRule
            .onNodeWithTag(TestTags.Reader.PAGE_PREV, useUnmergedTree = true)
            .performClick()

        composeRule.waitForIdle()
    }

    // ==================== Exit Tests ====================

    /**
     * Тест: Выход из книги возвращает в каталог.
     */
    @Test
    @ReleaseTest
    fun readerScreen_exit_returnsToCatalog() {
        openBook()

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

    // ==================== Helper Methods ====================

    private fun navigateToDownloadedBooks() {
        composeRule.waitForIdle()

        // Переходим на вкладку Books
        composeRule
            .onNodeWithTag(TestTags.BottomNav.TAB_BOOKS, useUnmergedTree = true)
            .performClick()

        composeRule.waitForIdle()

        // Переходим к скачанным книгам
        // Примечание: зависит от навигации в приложении
    }

    private fun openBook() {
        navigateToDownloadedBooks()

        // Ждём загрузки списка
        composeRule.waitUntil(5000) {
            composeRule
                .onAllNodesWithTag(TestTags.DownloadedBooks.ITEM, useUnmergedTree = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        // Кликаем на книгу
        composeRule
            .onNodeWithTag(TestTags.DownloadedBooks.ITEM, useUnmergedTree = true)
            .performClick()

        composeRule.waitForIdle()

        // Ждём загрузки книги
        composeRule.waitUntil(10000) {
            composeRule
                .onAllNodesWithTag(TestTags.Reader.CONTENT, useUnmergedTree = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }
}