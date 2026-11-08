package com.github.axet.bookreader.screens.integration

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.axet.bookreader.app.Storage
import com.github.axet.bookreader.screens.testing.ReaderTestTags
import com.github.axet.bookreader.screens.ui.BookmarksComposeDialog
import com.github.axet.bookreader.screens.ui.FontsComposeBottomSheet
import com.github.axet.bookreader.screens.ui.ReaderTopBar
import com.github.axet.bookreader.screens.ui.TocComposeDialog
import com.github.axet.bookreader.screens.viewmodel.ReaderActions
import com.github.axet.bookreader.screens.viewmodel.ReaderState
import com.github.axet.bookreader.screens.viewmodel.SearchState
import com.github.axet.bookreader.screens.viewmodel.ToolbarVisibility
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Изолированные UI-тесты «хрома» экрана чтения (топ-бар и диалоги).
 *
 * В отличие от интеграционных тестов, эти тесты НЕ загружают реальную книгу через FBReader
 * (нативный рендеринг на главном потоке вешает фреймворк Compose-тестов). Вместо этого
 * компоненты рендерятся напрямую с заранее собранным [ReaderState.Content], поэтому тесты
 * детерминированы и не зависят от эмулятора/нативных библиотек.
 */
@RunWith(AndroidJUnit4::class)
class ReaderChromeTest {

    @get:Rule
    val composeRule = createComposeRule()

    // ==================== ReaderTopBar ====================

    @Test
    fun topBar_allButtonsDisplayed() {
        composeRule.setContent {
            MaterialTheme {
                ReaderTopBar(
                    state = contentState(),
                    currentPage = 3,
                    totalPages = 42,
                    toolbarVisibility = ToolbarVisibility(),
                    onAction = {}
                )
            }
        }

        composeRule.onNodeWithTag(ReaderTestTags.TopBar.ROOT, useUnmergedTree = true)
            .assertIsDisplayed()
        composeRule.onNodeWithTag(ReaderTestTags.TopBar.BACK_BUTTON, useUnmergedTree = true)
            .assertIsDisplayed()
        composeRule.onNodeWithTag(ReaderTestTags.TopBar.SEARCH_BUTTON, useUnmergedTree = true)
            .assertIsDisplayed()
        composeRule.onNodeWithTag(ReaderTestTags.TopBar.TOC_BUTTON, useUnmergedTree = true)
            .assertIsDisplayed()
        composeRule.onNodeWithTag(ReaderTestTags.TopBar.BOOKMARKS_BUTTON, useUnmergedTree = true)
            .assertIsDisplayed()
        composeRule.onNodeWithTag(ReaderTestTags.TopBar.FONT_BUTTON, useUnmergedTree = true)
            .assertIsDisplayed()
        composeRule.onNodeWithTag(ReaderTestTags.TopBar.MENU_BUTTON, useUnmergedTree = true)
            .assertIsDisplayed()
        composeRule.onNodeWithTag(ReaderTestTags.TopBar.PAGE_INDICATOR, useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun topBar_hidesFontButton_whenFontNotChangeable() {
        composeRule.setContent {
            MaterialTheme {
                ReaderTopBar(
                    state = contentState().copy(canChangeFont = false),
                    currentPage = 1,
                    totalPages = 1,
                    toolbarVisibility = ToolbarVisibility(),
                    onAction = {}
                )
            }
        }

        composeRule.onNodeWithTag(ReaderTestTags.TopBar.FONT_BUTTON, useUnmergedTree = true)
            .assertDoesNotExist()
        // Остальные кнопки остаются на месте
        composeRule.onNodeWithTag(ReaderTestTags.TopBar.TOC_BUTTON, useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun topBar_backButtonInvokesNavigateBack() {
        val actions = mutableListOf<ReaderActions>()
        composeRule.setContent {
            MaterialTheme {
                ReaderTopBar(
                    state = contentState(),
                    currentPage = 1,
                    totalPages = 1,
                    toolbarVisibility = ToolbarVisibility(),
                    onAction = { actions += it }
                )
            }
        }

        composeRule.onNodeWithTag(ReaderTestTags.TopBar.BACK_BUTTON, useUnmergedTree = true)
            .performClick()

        assertEquals(listOf<ReaderActions>(ReaderActions.NavigateBack), actions)
    }

    @Test
    fun topBar_tocButtonInvokesToggleToc() {
        val actions = mutableListOf<ReaderActions>()
        composeRule.setContent {
            MaterialTheme {
                ReaderTopBar(
                    state = contentState(),
                    currentPage = 1,
                    totalPages = 1,
                    toolbarVisibility = ToolbarVisibility(),
                    onAction = { actions += it }
                )
            }
        }

        composeRule.onNodeWithTag(ReaderTestTags.TopBar.TOC_BUTTON, useUnmergedTree = true)
            .performClick()

        assertEquals(listOf<ReaderActions>(ReaderActions.ToggleToc), actions)
    }

    @Test
    fun topBar_bookmarksButtonInvokesToggleBookmarks() {
        val actions = mutableListOf<ReaderActions>()
        composeRule.setContent {
            MaterialTheme {
                ReaderTopBar(
                    state = contentState(),
                    currentPage = 1,
                    totalPages = 1,
                    toolbarVisibility = ToolbarVisibility(),
                    onAction = { actions += it }
                )
            }
        }

        composeRule.onNodeWithTag(ReaderTestTags.TopBar.BOOKMARKS_BUTTON, useUnmergedTree = true)
            .performClick()

        assertEquals(listOf<ReaderActions>(ReaderActions.ToggleBookmarks), actions)
    }

    @Test
    fun topBar_fontButtonInvokesToggleFontSettings() {
        val actions = mutableListOf<ReaderActions>()
        composeRule.setContent {
            MaterialTheme {
                ReaderTopBar(
                    state = contentState(),
                    currentPage = 1,
                    totalPages = 1,
                    toolbarVisibility = ToolbarVisibility(),
                    onAction = { actions += it }
                )
            }
        }

        composeRule.onNodeWithTag(ReaderTestTags.TopBar.FONT_BUTTON, useUnmergedTree = true)
            .performClick()

        assertEquals(listOf<ReaderActions>(ReaderActions.ToggleFontSettings), actions)
    }

    @Test
    fun topBar_searchMode_showsSearchField() {
        composeRule.setContent {
            MaterialTheme {
                ReaderTopBar(
                    state = contentState().copy(
                        searchState = SearchState(isActive = true, query = "hello")
                    ),
                    currentPage = 1,
                    totalPages = 1,
                    toolbarVisibility = ToolbarVisibility(),
                    onAction = {}
                )
            }
        }

        // В режиме поиска обычные кнопки скрыты, показывается поле поиска
        composeRule.onNodeWithTag(ReaderTestTags.TopBar.SEARCH_PANEL, useUnmergedTree = true)
            .assertIsDisplayed()
        composeRule.onNodeWithTag(ReaderTestTags.TopBar.SEARCH_FIELD, useUnmergedTree = true)
            .assertIsDisplayed()
        composeRule.onNodeWithTag(ReaderTestTags.TopBar.SEARCH_CLOSE, useUnmergedTree = true)
            .assertIsDisplayed()
        composeRule.onNodeWithTag(ReaderTestTags.TopBar.SEARCH_BUTTON, useUnmergedTree = true)
            .assertDoesNotExist()
    }

    @Test
    fun topBar_searchClose_invokesSearchClose() {
        val actions = mutableListOf<ReaderActions>()
        composeRule.setContent {
            MaterialTheme {
                ReaderTopBar(
                    state = contentState().copy(
                        searchState = SearchState(isActive = true, query = "abc")
                    ),
                    currentPage = 1,
                    totalPages = 1,
                    toolbarVisibility = ToolbarVisibility(),
                    onAction = { actions += it }
                )
            }
        }

        composeRule.onNodeWithTag(ReaderTestTags.TopBar.SEARCH_CLOSE, useUnmergedTree = true)
            .performClick()

        assertEquals(listOf<ReaderActions>(ReaderActions.SearchClose), actions)
    }

    // ==================== Диалоги ====================

    @Test
    fun tocDialog_emptyStateDisplayed() {
        composeRule.setContent {
            MaterialTheme {
                TocComposeDialog(
                    fbReaderView = null,
                    onDismiss = {},
                    onNavigate = {}
                )
            }
        }

        composeRule.onNodeWithTag(ReaderTestTags.Toc.DIALOG, useUnmergedTree = true)
            .assertExists()
        composeRule.onNodeWithTag(ReaderTestTags.Toc.EMPTY_STATE, useUnmergedTree = true)
            .assertExists()
    }

    @Test
    fun bookmarksDialog_emptyStateDisplayed() {
        composeRule.setContent {
            MaterialTheme {
                BookmarksComposeDialog(
                    book = null,
                    fbReaderView = null,
                    onDismiss = {},
                    onDelete = {}
                )
            }
        }

        composeRule.onNodeWithTag(ReaderTestTags.Bookmarks.DIALOG, useUnmergedTree = true)
            .assertExists()
        composeRule.onNodeWithTag(ReaderTestTags.Bookmarks.EMPTY_STATE, useUnmergedTree = true)
            .assertExists()
    }

    @Test
    fun fontSettings_sheetDisplayed() {
        composeRule.setContent {
            MaterialTheme {
                FontsComposeBottomSheet(
                    onDismiss = {},
                    onFontSizeChange = {},
                    onFontFamilyChange = {},
                    onIgnoreEmbeddedFontsChange = {}
                )
            }
        }

        composeRule.onNodeWithTag(ReaderTestTags.FontSettings.SHEET, useUnmergedTree = true)
            .assertExists()
    }

    // ==================== Helpers ====================

    private fun contentState(): ReaderState.Content = ReaderState.Content(book = testBook())

    private fun testBook(): Storage.Book = Storage.Book().apply {
        info = Storage.RecentInfo().apply { title = "Moby Dick" }
    }
}
