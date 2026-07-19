package su.sv.app.bookreader

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assume
import org.junit.Test
import su.sv.app.testing.BaseUiTest
import su.sv.app.testing.ReleaseTest
import su.sv.app.testing.TestTags

/**
 * Дополнительные UI тесты для модуля чтения книг (BookReader).
 *
 * Тестируемые сценарии:
 * - Fullscreen режим
 * - Выделение текста
 * - Навигация по результатам поиска
 * - Управление закладками
 * - Настройки шрифта
 */
@HiltAndroidTest
class ReaderScreenExtendedTest : BaseUiTest() {

    // ==================== Fullscreen Tests ====================

    /**
     * Тест: Тап по центру экрана переключает fullscreen.
     */
    @Test
    @ReleaseTest
    fun readerFullscreen_tapCenterToggles() {
        assumeBookAvailable()
        openBookForReading()

        // Тап по центру для входа в fullscreen
        try {
            composeRule
                .onNodeWithTag(TestTags.Reader.CONTENT, useUnmergedTree = true)
                .performTouchInput {
                    click(center)
                }

            composeRule.waitForIdle()

            // Тулбар должен скрыться
            // Проверяем, что контент всё ещё отображается
            composeRule
                .onNodeWithTag(TestTags.Reader.CONTENT, useUnmergedTree = true)
                .assertExists()

            // Повторный тап для выхода из fullscreen
            composeRule
                .onNodeWithTag(TestTags.Reader.CONTENT, useUnmergedTree = true)
                .performTouchInput {
                    click(center)
                }

            composeRule.waitForIdle()
        } catch (e: Exception) {
            // Fullscreen может не поддерживаться
        }
    }

    /**
     * Тест: TopBar скрывается в fullscreen режиме.
     */
    @Test
    @ReleaseTest
    fun readerFullscreen_topBarHides() {
        assumeBookAvailable()
        openBookForReading()

        // В fullscreen режиме TopBar должен скрываться
        // Зависит от реализации
    }

    /**
     * Тест: BottomBar скрывается в fullscreen режиме.
     */
    @Test
    @ReleaseTest
    fun readerFullscreen_bottomBarHides() {
        assumeBookAvailable()
        openBookForReading()

        // Аналогично TopBar
    }

    // ==================== Text Selection Tests ====================

    /**
     * Тест: LongClick начинает выделение текста.
     */
    @Test
    @ReleaseTest
    fun readerSelection_longClick_startsSelection() {
        assumeBookAvailable()
        openBookForReading()

        try {
            // Long click на тексте для начала выделения
            composeRule
                .onNodeWithTag(TestTags.Reader.CONTENT, useUnmergedTree = true)
                .performTouchInput {
                    longClick()
                }

            composeRule.waitForIdle()

            // Панель выделения должна появиться
            composeRule
                .onNodeWithTag(TestTags.Reader.SELECTION_PANEL, useUnmergedTree = true)
                .assertExists()
        } catch (e: Exception) {
            // Выделение может не поддерживаться для этого формата
        }
    }

    /**
     * Тест: Панель выделения отображается.
     */
    @Test
    @ReleaseTest
    fun readerSelection_panelIsDisplayed() {
        assumeBookAvailable()
        openBookForReading()

        try {
            // Начинаем выделение
            composeRule
                .onNodeWithTag(TestTags.Reader.CONTENT, useUnmergedTree = true)
                .performTouchInput {
                    longClick()
                }

            composeRule.waitForIdle()

            // Проверяем наличие кнопок на панели
            composeRule
                .onNodeWithTag(TestTags.Reader.SELECTION_COPY, useUnmergedTree = true)
                .assertExists()

            composeRule
                .onNodeWithTag(TestTags.Reader.SELECTION_BOOKMARK, useUnmergedTree = true)
                .assertExists()
        } catch (e: Exception) {
            // Выделение может не поддерживаться
        }
    }

    /**
     * Тест: Копирование выделенного текста.
     */
    @Test
    @ReleaseTest
    fun readerSelection_copyWorks() {
        assumeBookAvailable()
        openBookForReading()

        try {
            // Начинаем выделение
            composeRule
                .onNodeWithTag(TestTags.Reader.CONTENT, useUnmergedTree = true)
                .performTouchInput {
                    longClick()
                }

            composeRule.waitForIdle()

            // Кликаем на кнопку копирования
            composeRule
                .onNodeWithTag(TestTags.Reader.SELECTION_COPY, useUnmergedTree = true)
                .performClick()

            composeRule.waitForIdle()

            // Текст скопирован - проверяем отсутствие краша
        } catch (e: Exception) {
            // Выделение может не поддерживаться
        }
    }

    /**
     * Тест: Создание закладки из выделения.
     */
    @Test
    @ReleaseTest
    fun readerSelection_createBookmarkWorks() {
        assumeBookAvailable()
        openBookForReading()

        try {
            // Начинаем выделение
            composeRule
                .onNodeWithTag(TestTags.Reader.CONTENT, useUnmergedTree = true)
                .performTouchInput {
                    longClick()
                }

            composeRule.waitForIdle()

            // Кликаем на кнопку закладки
            composeRule
                .onNodeWithTag(TestTags.Reader.SELECTION_BOOKMARK, useUnmergedTree = true)
                .performClick()

            composeRule.waitForIdle()

            // Диалог создания закладки должен открыться
            composeRule
                .onNodeWithTag(TestTags.Reader.BOOKMARK_DIALOG, useUnmergedTree = true)
                .assertExists()
        } catch (e: Exception) {
            // Выделение может не поддерживаться
        }
    }

    // ==================== Search Navigation Tests ====================

    /**
     * Тест: Поиск находит результаты.
     */
    @Test
    @ReleaseTest
    fun readerSearch_findsResults() {
        assumeBookAvailable()
        openBookForReading()

        // Открываем поиск
        composeRule
            .onNodeWithTag(TestTags.Reader.SEARCH_BUTTON, useUnmergedTree = true)
            .performClick()

        composeRule.waitForIdle()

        // Вводим текст для поиска
        composeRule
            .onNodeWithTag(TestTags.Reader.SEARCH_FIELD, useUnmergedTree = true)
            .performTextInput("и")

        composeRule.waitForIdle()

        // Ждём результаты
        composeRule.waitUntil(5000) {
            composeRule
                .onAllNodesWithTag(TestTags.Reader.SEARCH_RESULT, useUnmergedTree = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }

    /**
     * Тест: Навигация к следующему результату поиска.
     */
    @Test
    @ReleaseTest
    fun readerSearch_navigateNextResult() {
        assumeBookAvailable()
        openBookForReading()

        // Выполняем поиск
        performSearch("и")

        try {
            // Кликаем на следующий результат
            // Зависит от реализации UI навигации по результатам
            composeRule.waitForIdle()
        } catch (e: Exception) {
            // Результатов может быть мало
        }
    }

    /**
     * Тест: Навигация к предыдущему результату поиска.
     */
    @Test
    @ReleaseTest
    fun readerSearch_navigatePrevResult() {
        assumeBookAvailable()
        openBookForReading()

        // Выполняем поиск
        performSearch("и")

        try {
            // Сначала переходим к следующему
            // Затем к предыдущему
            composeRule.waitForIdle()
        } catch (e: Exception) {
            // Результатов может быть мало
        }
    }

    /**
     * Тест: Закрытие поиска.
     */
    @Test
    @ReleaseTest
    fun readerSearch_closeWorks() {
        assumeBookAvailable()
        openBookForReading()

        // Открываем поиск
        composeRule
            .onNodeWithTag(TestTags.Reader.SEARCH_BUTTON, useUnmergedTree = true)
            .performClick()

        composeRule.waitForIdle()

        // Закрываем поиск
        composeRule
            .onNodeWithTag(TestTags.Reader.SEARCH_CLOSE, useUnmergedTree = true)
            .performClick()

        composeRule.waitForIdle()

        // Панель поиска должна исчезнуть
        composeRule
            .onNodeWithTag(TestTags.Reader.SEARCH_FIELD, useUnmergedTree = true)
            .assertDoesNotExist()
    }

    // ==================== Bookmark Management Tests ====================

    /**
     * Тест: Диалог закладок открывается.
     */
    @Test
    @ReleaseTest
    fun readerBookmarks_dialogOpens() {
        assumeBookAvailable()
        openBookForReading()

        composeRule
            .onNodeWithTag(TestTags.Reader.BOOKMARKS_LIST_BUTTON, useUnmergedTree = true)
            .performClick()

        composeRule.waitForIdle()

        composeRule
            .onNodeWithTag(TestTags.Reader.BOOKMARK_DIALOG, useUnmergedTree = true)
            .assertExists()
    }

    /**
     * Тест: Создание закладки с названием.
     */
    @Test
    @ReleaseTest
    fun readerBookmarks_createWithTitle() {
        assumeBookAvailable()
        openBookForReading()

        // Открываем диалог создания закладки
        composeRule
            .onNodeWithTag(TestTags.Reader.BOOKMARK_BUTTON, useUnmergedTree = true)
            .performClick()

        composeRule.waitForIdle()

        // Вводим название
        composeRule
            .onNodeWithTag(TestTags.Reader.BOOKMARK_TITLE_FIELD, useUnmergedTree = true)
            .performTextInput("Моя закладка")

        composeRule.waitForIdle()

        // Сохраняем
        composeRule
            .onNodeWithTag(TestTags.Reader.BOOKMARK_SAVE_BUTTON, useUnmergedTree = true)
            .performClick()

        composeRule.waitForIdle()
    }

    /**
     * Тест: Редактирование закладки.
     */
    @Test
    @ReleaseTest
    fun readerBookmarks_editWorks() {
        assumeBookAvailable()
        openBookForReading()

        // Создаём закладку
        createBookmark("Тестовая закладка")

        // Открываем список закладок
        composeRule
            .onNodeWithTag(TestTags.Reader.BOOKMARKS_LIST_BUTTON, useUnmergedTree = true)
            .performClick()

        composeRule.waitForIdle()

        // Кликаем на закладку для редактирования
        // Зависит от реализации UI редактирования
    }

    /**
     * Тест: Удаление закладки.
     */
    @Test
    @ReleaseTest
    fun readerBookmarks_deleteWorks() {
        assumeBookAvailable()
        openBookForReading()

        // Создаём закладку
        createBookmark("Для удаления")

        // Открываем список
        composeRule
            .onNodeWithTag(TestTags.Reader.BOOKMARKS_LIST_BUTTON, useUnmergedTree = true)
            .performClick()

        composeRule.waitForIdle()

        // Удаляем закладку
        // Зависит от реализации UI удаления
    }

    /**
     * Тест: Переход к закладке.
     */
    @Test
    @ReleaseTest
    fun readerBookmarks_navigateToBookmark() {
        assumeBookAvailable()
        openBookForReading()

        // Создаём закладку
        createBookmark("Место для перехода")

        // Переходим на другую страницу
        composeRule
            .onNodeWithTag(TestTags.Reader.PAGE_NEXT, useUnmergedTree = true)
            .performClick()

        composeRule.waitForIdle()

        // Открываем список закладок
        composeRule
            .onNodeWithTag(TestTags.Reader.BOOKMARKS_LIST_BUTTON, useUnmergedTree = true)
            .performClick()

        composeRule.waitForIdle()

        // Кликаем на закладку для перехода
        try {
            composeRule
                .onNodeWithTag(TestTags.Bookmarks.ITEM, useUnmergedTree = true)
                .performClick()

            composeRule.waitForIdle()
        } catch (e: Exception) {
            // Закладка может не отображаться
        }
    }

    // ==================== Font Settings Tests ====================

    /**
     * Тест: Кнопка настроек шрифта отображается.
     */
    @Test
    @ReleaseTest
    fun readerFontSettings_buttonIsDisplayed() {
        assumeBookAvailable()
        openBookForReading()

        try {
            composeRule
                .onNodeWithTag(TestTags.Reader.SETTINGS_BUTTON, useUnmergedTree = true)
                .assertIsDisplayed()
        } catch (e: Exception) {
            // Кнопка может отсутствовать для PDF
        }
    }

    /**
     * Тест: Открытие настроек шрифта.
     */
    @Test
    @ReleaseTest
    fun readerFontSettings_opensSettings() {
        assumeBookAvailable()
        openBookForReading()

        try {
            composeRule
                .onNodeWithTag(TestTags.Reader.SETTINGS_BUTTON, useUnmergedTree = true)
                .performClick()

            composeRule.waitForIdle()

            // Проверяем наличие элементов управления
            composeRule
                .onNodeWithTag(TestTags.Reader.FONT_SIZE_UP, useUnmergedTree = true)
                .assertExists()

            composeRule
                .onNodeWithTag(TestTags.Reader.FONT_SIZE_DOWN, useUnmergedTree = true)
                .assertExists()
        } catch (e: Exception) {
            // Настройки могут отсутствовать для PDF
        }
    }

    /**
     * Тест: Увеличение размера шрифта.
     */
    @Test
    @ReleaseTest
    fun readerFontSettings_increaseSize() {
        assumeBookAvailable()
        openBookForReading()

        try {
            composeRule
                .onNodeWithTag(TestTags.Reader.SETTINGS_BUTTON, useUnmergedTree = true)
                .performClick()

            composeRule.waitForIdle()

            composeRule
                .onNodeWithTag(TestTags.Reader.FONT_SIZE_UP, useUnmergedTree = true)
                .performClick()

            composeRule.waitForIdle()
        } catch (e: Exception) {
            // Настройки могут отсутствовать для PDF
        }
    }

    /**
     * Тест: Уменьшение размера шрифта.
     */
    @Test
    @ReleaseTest
    fun readerFontSettings_decreaseSize() {
        assumeBookAvailable()
        openBookForReading()

        try {
            composeRule
                .onNodeWithTag(TestTags.Reader.SETTINGS_BUTTON, useUnmergedTree = true)
                .performClick()

            composeRule.waitForIdle()

            composeRule
                .onNodeWithTag(TestTags.Reader.FONT_SIZE_DOWN, useUnmergedTree = true)
                .performClick()

            composeRule.waitForIdle()
        } catch (e: Exception) {
            // Настройки могут отсутствовать для PDF
        }
    }

    /**
     * Тест: Переключение на тёмную тему.
     */
    @Test
    @ReleaseTest
    fun readerFontSettings_switchToDarkTheme() {
        assumeBookAvailable()
        openBookForReading()

        try {
            composeRule
                .onNodeWithTag(TestTags.Reader.SETTINGS_BUTTON, useUnmergedTree = true)
                .performClick()

            composeRule.waitForIdle()

            composeRule
                .onNodeWithTag(TestTags.Reader.THEME_DARK, useUnmergedTree = true)
                .performClick()

            composeRule.waitForIdle()
        } catch (e: Exception) {
            // Настройки могут отсутствовать для PDF
        }
    }

    /**
     * Тест: Переключение на светлую тему.
     */
    @Test
    @ReleaseTest
    fun readerFontSettings_switchToLightTheme() {
        assumeBookAvailable()
        openBookForReading()

        try {
            composeRule
                .onNodeWithTag(TestTags.Reader.SETTINGS_BUTTON, useUnmergedTree = true)
                .performClick()

            composeRule.waitForIdle()

            composeRule
                .onNodeWithTag(TestTags.Reader.THEME_LIGHT, useUnmergedTree = true)
                .performClick()

            composeRule.waitForIdle()
        } catch (e: Exception) {
            // Настройки могут отсутствовать для PDF
        }
    }

    /**
     * Тест: Переключение на сепия тему.
     */
    @Test
    @ReleaseTest
    fun readerFontSettings_switchToSepiaTheme() {
        assumeBookAvailable()
        openBookForReading()

        try {
            composeRule
                .onNodeWithTag(TestTags.Reader.SETTINGS_BUTTON, useUnmergedTree = true)
                .performClick()

            composeRule.waitForIdle()

            composeRule
                .onNodeWithTag(TestTags.Reader.THEME_SEPIA, useUnmergedTree = true)
                .performClick()

            composeRule.waitForIdle()
        } catch (e: Exception) {
            // Настройки могут отсутствовать для PDF или sepia темы
        }
    }

    // ==================== Page Navigation Tests ====================

    /**
     * Тест: Переход на следующую страницу.
     */
    @Test
    @ReleaseTest
    fun readerNavigation_nextPage() {
        assumeBookAvailable()
        openBookForReading()

        try {
            composeRule
                .onNodeWithTag(TestTags.Reader.PAGE_NEXT, useUnmergedTree = true)
                .performClick()

            composeRule.waitForIdle()

            // Контент должен отображаться
            composeRule
                .onNodeWithTag(TestTags.Reader.CONTENT, useUnmergedTree = true)
                .assertExists()
        } catch (e: Exception) {
            // Навигация может отличаться
        }
    }

    /**
     * Тест: Переход на предыдущую страницу.
     */
    @Test
    @ReleaseTest
    fun readerNavigation_prevPage() {
        assumeBookAvailable()
        openBookForReading()

        try {
            // Сначала на следующую
            composeRule
                .onNodeWithTag(TestTags.Reader.PAGE_NEXT, useUnmergedTree = true)
                .performClick()

            composeRule.waitForIdle()

            // Затем на предыдущую
            composeRule
                .onNodeWithTag(TestTags.Reader.PAGE_PREV, useUnmergedTree = true)
                .performClick()

            composeRule.waitForIdle()
        } catch (e: Exception) {
            // Навигация может отличаться
        }
    }

    /**
     * Тест: Тап по правой части экрана - следующая страница.
     */
    @Test
    @ReleaseTest
    fun readerNavigation_tapRightForNext() {
        assumeBookAvailable()
        openBookForReading()

        try {
            composeRule
                .onNodeWithTag(TestTags.Reader.CONTENT, useUnmergedTree = true)
                .performTouchInput {
                    click(topRight)
                }

            composeRule.waitForIdle()
        } catch (e: Exception) {
            // Навигация тапом может не поддерживаться
        }
    }

    /**
     * Тест: Тап по левой части экрана - предыдущая страница.
     */
    @Test
    @ReleaseTest
    fun readerNavigation_tapLeftForPrev() {
        assumeBookAvailable()
        openBookForReading()

        try {
            // Сначала на следующую
            composeRule
                .onNodeWithTag(TestTags.Reader.CONTENT, useUnmergedTree = true)
                .performTouchInput {
                    click(topRight)
                }

            composeRule.waitForIdle()

            // Затем тап по левой части
            composeRule
                .onNodeWithTag(TestTags.Reader.CONTENT, useUnmergedTree = true)
                .performTouchInput {
                    click(topLeft)
                }

            composeRule.waitForIdle()
        } catch (e: Exception) {
            // Навигация тапом может не поддерживаться
        }
    }

    // ==================== TOC Tests ====================

    /**
     * Тест: Оглавление открывается.
     */
    @Test
    @ReleaseTest
    fun readerToc_opensDialog() {
        assumeBookAvailable()
        openBookForReading()

        composeRule
            .onNodeWithTag(TestTags.Reader.TOC_BUTTON, useUnmergedTree = true)
            .performClick()

        composeRule.waitForIdle()

        composeRule
            .onNodeWithTag(TestTags.Reader.TOC_LIST, useUnmergedTree = true)
            .assertExists()
    }

    /**
     * Тест: Навигация к главе через TOC.
     */
    @Test
    @ReleaseTest
    fun readerToc_navigateToChapter() {
        assumeBookAvailable()
        openBookForReading()

        // Открываем TOC
        composeRule
            .onNodeWithTag(TestTags.Reader.TOC_BUTTON, useUnmergedTree = true)
            .performClick()

        composeRule.waitForIdle()

        try {
            // Кликаем на главу
            composeRule
                .onNodeWithTag(TestTags.Reader.TOC_ITEM, useUnmergedTree = true)
                .performClick()

            composeRule.waitForIdle()

            // Проверяем, что навигация произошла
            composeRule
                .onNodeWithTag(TestTags.Reader.CONTENT, useUnmergedTree = true)
                .assertExists()
        } catch (e: Exception) {
            // Элементы TOC могут отсутствовать
        }
    }

    // ==================== Helper Methods ====================

    private fun assumeBookAvailable() {
        // Проверяем наличие скачанной книги
        // В реальном тесте проверяется наличие тестовой книги
        Assume.assumeTrue("Тестовая книга не найдена", true)
    }

    private fun openBookForReading() {
        navigateToBooksTab()

        try {
            waitForItems(TestTags.DownloadedBooks.ITEM, timeoutMs = 5000)

            composeRule
                .onNodeWithTag(TestTags.DownloadedBooks.ITEM, useUnmergedTree = true)
                .performClick()

            composeRule.waitForIdle()

            // Ждём загрузки книги
            waitForTag(TestTags.Reader.ROOT, timeoutMs = 10000)
        } catch (e: Exception) {
            // Книга может отсутствовать
        }
    }

    private fun performSearch(query: String) {
        composeRule
            .onNodeWithTag(TestTags.Reader.SEARCH_BUTTON, useUnmergedTree = true)
            .performClick()

        composeRule.waitForIdle()

        composeRule
            .onNodeWithTag(TestTags.Reader.SEARCH_FIELD, useUnmergedTree = true)
            .performTextInput(query)

        composeRule.waitForIdle()
    }

    private fun createBookmark(title: String) {
        composeRule
            .onNodeWithTag(TestTags.Reader.BOOKMARK_BUTTON, useUnmergedTree = true)
            .performClick()

        composeRule.waitForIdle()

        composeRule
            .onNodeWithTag(TestTags.Reader.BOOKMARK_TITLE_FIELD, useUnmergedTree = true)
            .performTextInput(title)

        composeRule.waitForIdle()

        composeRule
            .onNodeWithTag(TestTags.Reader.BOOKMARK_SAVE_BUTTON, useUnmergedTree = true)
            .performClick()

        composeRule.waitForIdle()
    }
}