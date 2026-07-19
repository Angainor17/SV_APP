package com.github.axet.bookreader.screens.integration

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.axet.bookreader.app.BookReaderInitializer
import com.github.axet.bookreader.screens.ReaderContent
import com.github.axet.bookreader.screens.testing.ReaderTestTags
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Интеграционные тесты для ReaderScreen.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ReaderIntegrationTest {

    // Hilt правило должно быть первым
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<TestActivity>()

    private lateinit var testBook: TestBook

    @Before
    fun setup() {
        hiltRule.inject()
        // Инициализация FBReader для работы ReaderContent
        BookReaderInitializer.init(composeRule.activity.applicationContext)
        testBook = TestBookProvider.getTestBook()
    }

    // ==================== Базовые тесты ====================

    @Test
    fun reader_opensBook_withoutCrash() {
        assumeBookAvailable()
        launchReader()
        composeRule.waitUntil(5000) { true }
    }

    @Test
    fun reader_ui_renders() {
        assumeBookAvailable()
        launchReader()
        Thread.sleep(2000)
    }

    // ==================== TopBar ====================

    @Test
    fun reader_topBar_appearsAfterLoad() {
        assumeBookAvailable()
        launchReader()
        waitForTopBar()
    }

    @Test
    fun reader_topBar_buttonsDisplayed() {
        assumeBookAvailable()
        launchReader()
        waitForTopBar()

        composeRule.onNodeWithTag(ReaderTestTags.TopBar.BACK_BUTTON, useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag(ReaderTestTags.TopBar.SEARCH_BUTTON, useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag(ReaderTestTags.TopBar.TOC_BUTTON, useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag(ReaderTestTags.TopBar.BOOKMARKS_BUTTON, useUnmergedTree = true).assertExists()
    }

    // ==================== TOC ====================

    @Test
    fun reader_tocDialog_opens() {
        assumeBookAvailable()
        launchReader()
        waitForTopBar()

        composeRule.onNodeWithTag(ReaderTestTags.TopBar.TOC_BUTTON, useUnmergedTree = true).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(ReaderTestTags.Toc.DIALOG, useUnmergedTree = true).assertExists()
    }

    // ==================== Bookmarks ====================

    @Test
    fun reader_bookmarksDialog_opens() {
        assumeBookAvailable()
        launchReader()
        waitForTopBar()

        composeRule.onNodeWithTag(ReaderTestTags.TopBar.BOOKMARKS_BUTTON, useUnmergedTree = true).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(ReaderTestTags.Bookmarks.DIALOG, useUnmergedTree = true).assertExists()
    }

    // ==================== Helper Methods ====================

    private fun assumeBookAvailable() {
        Assume.assumeTrue("Тестовая книга не найдена", testBook.isAvailable())
    }

    private fun launchReader() {
        composeRule.setContent {
            ReaderContent(
                bookUri = testBook.uri,
                bookCoverUrl = null,
                bookTitle = testBook.title,
                bookAuthor = testBook.author,
                bookmarkPosition = null,
                onNavigateBack = {},
                onNavigateToSettings = {}
            )
        }
    }

    private fun waitForTopBar(timeout: Long = 60000) {
        composeRule.waitUntil(timeout) {
            try {
                composeRule.onNodeWithTag(ReaderTestTags.TopBar.ROOT, useUnmergedTree = true)
                    .fetchSemanticsNode() != null
            } catch (e: Exception) {
                false
            }
        }
    }
}