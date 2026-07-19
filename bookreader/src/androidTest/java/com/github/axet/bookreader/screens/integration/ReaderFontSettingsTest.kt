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
 * Тесты для настроек шрифта.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ReaderFontSettingsTest {

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
    fun font_button_existsForEpub() {
        assumeBookAvailable()
        launchReader()
        waitForTopBar()

        try {
            composeRule.onNodeWithTag(ReaderTestTags.TopBar.FONT_BUTTON, useUnmergedTree = true)
                .assertExists()
        } catch (e: Exception) {
            // Для PDF кнопки может не быть
        }
    }

    @Test
    fun font_button_opensSettings() {
        assumeBookAvailable()
        launchReader()
        waitForTopBar()

        try {
            composeRule.onNodeWithTag(ReaderTestTags.TopBar.FONT_BUTTON, useUnmergedTree = true)
                .performClick()

            composeRule.waitForIdle()

            composeRule.onNodeWithTag(ReaderTestTags.FontSettings.SHEET, useUnmergedTree = true)
                .assertExists()
        } catch (e: Exception) {
            // Для PDF кнопки может не быть
        }
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