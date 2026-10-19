package com.github.axet.bookreader.screens.integration

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
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
 * Тесты для поиска по книге.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ReaderSearchTest {

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

    @Test
    fun search_button_opensPanel() {
        assumeBookAvailable()
        launchReader()
        waitForTopBar()

        composeRule.onNodeWithTag(ReaderTestTags.TopBar.SEARCH_BUTTON, useUnmergedTree = true)
            .performClick()

        composeRule.waitForIdle()

        composeRule.onNodeWithTag(ReaderTestTags.TopBar.SEARCH_PANEL, useUnmergedTree = true)
            .assertExists()
    }

    @Test
    fun search_field_acceptsInput() {
        assumeBookAvailable()
        launchReader()
        waitForTopBar()

        composeRule.onNodeWithTag(ReaderTestTags.TopBar.SEARCH_BUTTON, useUnmergedTree = true)
            .performClick()

        composeRule.waitForIdle()

        composeRule.onNodeWithTag(ReaderTestTags.TopBar.SEARCH_FIELD, useUnmergedTree = true)
            .performTextInput("и")

        composeRule.waitForIdle()
    }

    @Test
    fun search_close_works() {
        assumeBookAvailable()
        launchReader()
        waitForTopBar()

        composeRule.onNodeWithTag(ReaderTestTags.TopBar.SEARCH_BUTTON, useUnmergedTree = true)
            .performClick()

        composeRule.waitForIdle()

        composeRule.onNodeWithTag(ReaderTestTags.TopBar.SEARCH_CLOSE, useUnmergedTree = true)
            .performClick()

        composeRule.waitForIdle()

        composeRule.onNodeWithTag(ReaderTestTags.TopBar.SEARCH_PANEL, useUnmergedTree = true)
            .assertDoesNotExist()
    }

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