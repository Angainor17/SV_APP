package com.github.axet.bookreader.screens.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val searchState = state.searchState

    TopAppBar(
        title = {
            if (searchState.isActive) {
                // Search mode - show search field
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = searchState.query,
                        onValueChange = { query ->
                            onAction(ReaderActions.Search(query))
                        },
                        placeholder = {
                            Text(
                                stringResource(R.string.search_placeholder),
                                fontSize = 14.sp
                            )
                        },
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 14.sp),
                        modifier = Modifier.width(220.dp)
                    )

                    // Loading indicator or results counter
                    if (searchState.isLoading) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            modifier = Modifier.width(16.dp).height(16.dp)
                        )
                    } else if (searchState.resultsCount > 0) {
                        Text(
                            text = "${searchState.currentResultIndex + 1}/${searchState.resultsCount}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    // If no results and query exists - show nothing (remove "Не найдено")

                    // Spacer to push arrows to the right edge
                    Spacer(modifier = Modifier.weight(1f, fill = true))

                    // Navigation buttons - always at right edge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        // Previous button
                        IconButton(
                            onClick = { onAction(ReaderActions.SearchPrevious) },
                            enabled = searchState.resultsCount > 0 && searchState.currentResultIndex > 0
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowUp,
                                contentDescription = stringResource(R.string.search_previous),
                                tint = if (searchState.resultsCount > 0 && searchState.currentResultIndex > 0)
                                    MaterialTheme.colorScheme.onSurface
                                else
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        // Next button
                        IconButton(
                            onClick = { onAction(ReaderActions.SearchNext) },
                            enabled = searchState.resultsCount > 0 && searchState.currentResultIndex < searchState.resultsCount - 1
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = stringResource(R.string.search_next),
                                tint = if (searchState.resultsCount > 0 && searchState.currentResultIndex < searchState.resultsCount - 1)
                                    MaterialTheme.colorScheme.onSurface
                                else
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            } else {
                // Normal mode - show book title
                Text(
                    text = state.book.info.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = {
                if (searchState.isActive) {
                    onAction(ReaderActions.SearchClose)
                } else {
                    onAction(ReaderActions.NavigateBack)
                }
            }) {
                Icon(
                    imageVector = if (searchState.isActive) Icons.Default.Close else Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = if (searchState.isActive) "Close search" else "Back",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        actions = {
            // Only show other actions when not in search mode
            if (!searchState.isActive) {
                // Search
                IconButton(onClick = {
                    onAction(ReaderActions.Search(""))
                }) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Table of Contents
                IconButton(onClick = { onAction(ReaderActions.ToggleToc) }) {
                    Icon(
                        painter = androidx.compose.ui.res.painterResource(R.drawable.ic_toc_white_24dp),
                        contentDescription = "Table of Contents",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Bookmarks
                IconButton(onClick = { onAction(ReaderActions.ToggleBookmarks) }) {
                    Icon(
                        painter = androidx.compose.ui.res.painterResource(R.drawable.ic_bookmark_white_24dp),
                        contentDescription = "Bookmarks",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Font size - показываем только если шрифт можно менять
                if (state.canChangeFont) {
                    IconButton(onClick = { onAction(ReaderActions.ToggleFontSettings) }) {
                        Icon(
                            painter = androidx.compose.ui.res.painterResource(R.drawable.ic_format_size_white_24dp),
                            contentDescription = "Font Settings",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // More options menu
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                ) {
                    // View mode
                    val viewModeText = if (state.viewMode.name == "PAGING") {
                        stringResource(R.string.sv_view_mode_continuous)
                    } else {
                        stringResource(R.string.sv_view_mode_paging)
                    }
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = viewModeText,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        onClick = {
                            onAction(ReaderActions.ToggleViewMode)
                            showMenu = false
                        },
                    )

                    // Reflow (for PDF)
                    val reflowText = if (state.isReflow) {
                        stringResource(R.string.sv_original_layout)
                    } else {
                        stringResource(R.string.sv_reflow_text)
                    }
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = reflowText,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        onClick = {
                            onAction(ReaderActions.ToggleReflow)
                            showMenu = false
                        },
                    )

                    // Settings
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = stringResource(R.string.sv_menu_settings),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        onClick = {
                            onAction(ReaderActions.NavigateToSettings)
                            showMenu = false
                        },
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}