package su.sv.wiki.presentation.favorites

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.terrakok.modo.stack.LocalStackNavigation
import com.github.terrakok.modo.stack.back
import com.github.terrakok.modo.stack.forward
import su.sv.commonui.theme.SVAPPTheme
import su.sv.commonui.ui.components.AppAlertDialog
import su.sv.commonui.ui.components.AppToolbarWithBack
import su.sv.commonui.ui.components.FullScreenEmpty
import su.sv.wiki.R
import su.sv.wiki.presentation.article.ArticleScreen

/**
 * Контент экрана избранного (для modo)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreenContent(
    viewModel: FavoritesViewModel = hiltViewModel(),
) {
    val stackNavigation = LocalStackNavigation.current
    val favorites by viewModel.favorites.collectAsStateWithLifecycle(initialValue = emptyList())
    var showClearDialog by remember { mutableStateOf(false) }

    // Диалог подтверждения очистки
    if (showClearDialog) {
        ClearFavoritesDialog(
            onConfirm = {
                showClearDialog = false
                viewModel.clearFavorites()
                stackNavigation.back()
            },
            onDismiss = {
                showClearDialog = false
            },
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            AppToolbarWithBack(
                title = stringResource(R.string.wiki_favorites_title),
                onBackClick = { stackNavigation.back() },
                actions = {
                    if (favorites.isNotEmpty()) {
                        IconButton(onClick = { showClearDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(R.string.wiki_favorites_clear),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            )
        },
    ) { paddingValues ->
        if (favorites.isEmpty()) {
            FullScreenEmpty(
                title = stringResource(R.string.wiki_favorites_empty),
                icon = Icons.Default.Favorite,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            ) {
                items(favorites) { title ->
                    FavoriteItem(
                        title = title,
                        onClick = {
                            stackNavigation.forward(ArticleScreen(title = title))
                        },
                    )
                }
            }
        }
    }
}

/**
 * Элемент списка избранного
 */
@Composable
private fun FavoriteItem(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ListItem(
        headlineContent = {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
            )
        },
        leadingContent = {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    )
}

/**
 * Диалог подтверждения очистки избранного
 */
@Composable
private fun ClearFavoritesDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AppAlertDialog(
        title = stringResource(R.string.wiki_favorites_clear_confirm),
        onDismiss = onDismiss,
        onConfirm = onConfirm,
        confirmText = stringResource(R.string.wiki_favorites_clear_yes),
        dismissText = stringResource(R.string.wiki_favorites_clear_no),
    )
}

// ============================================
// Preview
// ============================================

@Composable
@Preview(
    showBackground = true,
    backgroundColor = 0xFFFFFFFF,
)
fun FavoriteItemPreview() {
    SVAPPTheme {
        FavoriteItem(
            title = "Государство и революция",
            onClick = {},
        )
    }
}

@Composable
@Preview(
    showBackground = true,
    backgroundColor = 0xFFFFFFFF,
)
fun ClearFavoritesDialogPreview() {
    SVAPPTheme {
        ClearFavoritesDialog(
            onConfirm = {},
            onDismiss = {},
        )
    }
}
