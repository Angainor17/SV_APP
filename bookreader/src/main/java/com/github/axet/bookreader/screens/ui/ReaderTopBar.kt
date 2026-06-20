package com.github.axet.bookreader.screens.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.github.axet.bookreader.R
import com.github.axet.bookreader.screens.viewmodel.ReaderActions
import com.github.axet.bookreader.screens.viewmodel.ReaderState

/**
 * Верхняя панель для экрана чтения книги
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderTopBar(
    state: ReaderState.Content,
    onAction: (ReaderActions) -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            Text(
                text = state.book.info.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        navigationIcon = {
            IconButton(onClick = { onAction(ReaderActions.NavigateBack) }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                )
            }
        },
        actions = {
            // Table of Contents
            IconButton(onClick = { onAction(ReaderActions.ToggleToc) }) {
                Icon(
                    painter = androidx.compose.ui.res.painterResource(R.drawable.ic_toc_white_24dp),
                    contentDescription = "Table of Contents",
                )
            }

            // Bookmarks
            IconButton(onClick = { onAction(ReaderActions.ToggleBookmarks) }) {
                Icon(
                    painter = androidx.compose.ui.res.painterResource(R.drawable.ic_bookmark_white_24dp),
                    contentDescription = "Bookmarks",
                )
            }

            // Font size
            IconButton(onClick = { onAction(ReaderActions.ToggleFontSettings) }) {
                Icon(
                    painter = androidx.compose.ui.res.painterResource(R.drawable.ic_format_size_white_24dp),
                    contentDescription = "Font Settings",
                )
            }

            // More options menu
            IconButton(onClick = { showMenu = true }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More",
                )
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
            ) {
                // View mode
                DropdownMenuItem(
                    text = {
                        Text(
                            if (state.viewMode.name == "PAGING") "Continuous mode"
                            else "Paging mode"
                        )
                    },
                    onClick = {
                        onAction(ReaderActions.ToggleViewMode)
                        showMenu = false
                    },
                )

                // Reflow (for PDF)
                DropdownMenuItem(
                    text = { Text(if (state.isReflow) "Original layout" else "Reflow text") },
                    onClick = {
                        onAction(ReaderActions.ToggleReflow)
                        showMenu = false
                    },
                )

                // Settings
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.menu_settings)) },
                    onClick = {
                        onAction(ReaderActions.NavigateToSettings)
                        showMenu = false
                    },
                )
            }
        },
    )
}
