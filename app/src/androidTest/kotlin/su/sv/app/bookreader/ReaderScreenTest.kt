package su.sv.app.bookreader

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import su.sv.app.MainActivity
import su.sv.app.testing.ReleaseTest
import su.sv.app.testing.SmokeTest
import su.sv.app.testing.TestTags

/**
 * UI-тесты навигации к читалке (BookReader) — сквозной сценарий:
 * скачанная книга → экран деталей → кнопка «Читать».
 *
 * ВАЖНО: эти тесты НЕ открывают нативный рендеринг книги (FBReaderView).
 * Нативный рендеринг блокирует главный поток и вешает фреймворк Compose-тестов
 * (это задокументировано в модуле bookreader — см. ReaderChromeTest, где «хром»
 * читалки тестируется изолированно, без загрузки реальной книги). Поэтому здесь
 * проверяется только навигация до точки входа в читалку.
 *
 * Предварительное условие: в приложении должна быть хотя бы одна скачанная книга
 * (см. scripts/prepare_test_book.sh).
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

    // ==================== Навигация к читалке ====================

    /**
     * Тест: клик по скачанной книге открывает экран деталей.
     */
    @Test
    @SmokeTest
    fun readerNavigation_downloadedBook_opensDetail() {
        openDownloadedBookDetail()

        composeRule
            .onNodeWithTag(TestTags.BookDetail.ROOT, useUnmergedTree = true)
            .assertExists()
    }

    /**
     * Тест: на экране деталей скачанной книги отображается кнопка «Читать».
     */
    @Test
    @ReleaseTest
    fun readerNavigation_readButton_isDisplayed() {
        openDownloadedBookDetail()

        composeRule
            .onNodeWithTag(TestTags.BookDetail.READ_BUTTON, useUnmergedTree = true)
            .assertIsDisplayed()
    }

    /**
     * Тест: кнопка «Читать» кликабельна.
     */
    @Test
    @ReleaseTest
    fun readerNavigation_readButton_isClickable() {
        openDownloadedBookDetail()

        composeRule
            .onNodeWithTag(TestTags.BookDetail.READ_BUTTON, useUnmergedTree = true)
            .assertHasClickAction()
    }

    /**
     * Тест: кнопка «Назад» с экрана деталей возвращает к списку скачанных книг.
     */
    @Test
    @ReleaseTest
    fun readerNavigation_back_returnsToDownloadedBooks() {
        openDownloadedBookDetail()

        composeRule
            .onNodeWithTag(TestTags.Common.BACK_BUTTON, useUnmergedTree = true)
            .performClick()

        composeRule.waitUntil(5000) {
            composeRule
                .onAllNodesWithTag(TestTags.DownloadedBooks.ROOT, useUnmergedTree = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }

    // ==================== Helper Methods ====================

    /**
     * Переход к списку скачанных книг и открытие первой книги (экран деталей).
     */
    private fun openDownloadedBookDetail() {
        composeRule.waitForIdle()

        // Переходим на вкладку Books
        composeRule
            .onNodeWithTag(TestTags.BottomNav.TAB_BOOKS, useUnmergedTree = true)
            .performClick()

        composeRule.waitForIdle()

        // Ждём загрузки каталога
        composeRule.waitUntil(10000) {
            composeRule
                .onAllNodesWithTag(TestTags.BooksCatalog.ITEM, useUnmergedTree = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        // Кнопка «Скачанные» видна только при наличии хотя бы одной скачанной книги
        val buttonExists =
            try {
                composeRule
                    .onNodeWithTag(TestTags.BooksCatalog.DOWNLOADED_BUTTON, useUnmergedTree = true)
                    .fetchSemanticsNode() != null
            } catch (e: Throwable) {
                false
            }
        assumeTrue("Нет скачанных книг (тестовая книга не найдена)", buttonExists)

        composeRule
            .onNodeWithTag(TestTags.BooksCatalog.DOWNLOADED_BUTTON, useUnmergedTree = true)
            .performClick()

        composeRule.waitForIdle()

        // Ждём список скачанных книг
        composeRule.waitUntil(5000) {
            composeRule
                .onAllNodesWithTag(TestTags.DownloadedBooks.ITEM, useUnmergedTree = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        // Открываем первую скачанную книгу → экран деталей
        composeRule
            .onAllNodesWithTag(TestTags.DownloadedBooks.ITEM, useUnmergedTree = true)[0]
            .performClick()

        composeRule.waitUntil(10000) {
            composeRule
                .onAllNodesWithTag(TestTags.BookDetail.ROOT, useUnmergedTree = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }
}
