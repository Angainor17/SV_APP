package su.sv.books.catalog.presentation.detail.ui

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.axet.bookreader.activities.MainActivity
import com.github.terrakok.modo.stack.LocalStackNavigation
import com.github.terrakok.modo.stack.back
import kotlinx.coroutines.launch
import su.sv.books.catalog.presentation.detail.actions.DetailBookActions
import su.sv.books.catalog.presentation.detail.effects.BookDetailOneTimeEffect
import su.sv.books.catalog.presentation.detail.model.UiBookDetailState
import su.sv.books.catalog.presentation.detail.viewmodel.BookDetailViewModel
import su.sv.commonui.ui.OneTimeEffect
import su.sv.models.ui.book.UiBook

@ExperimentalMaterial3Api
@Composable
fun BookDetailUi(
    viewModel: BookDetailViewModel = hiltViewModel(),
    uiBook: UiBook,
    modifier: Modifier
) {
    val state = viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val stackNavigation = LocalStackNavigation.current

    LaunchedEffect(Unit) {
        viewModel.onAction(DetailBookActions.LoadState(uiBook))
    }

    HandleEffects(
        viewModel = viewModel,
        snackbarHostState = snackbarHostState,
    )

    Scaffold(
        modifier = modifier.statusBarsPadding(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiBook.title,
                        maxLines = 1,
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            stackNavigation.back()
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "backIcon")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { contentPadding ->
        Box(
            modifier = Modifier.padding(contentPadding)
        ) {
            when (val value = state.value) {
                is UiBookDetailState.Content -> {
                    BookDetailInfoUi(
                        state = value,
                        actionsHandler = viewModel,
                    )
                }

                UiBookDetailState.NoContent -> Unit
            }
        }
    }
}

@Composable
private fun HandleEffects(
    viewModel: BookDetailViewModel,
    snackbarHostState: SnackbarHostState,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()


    OneTimeEffect(viewModel.oneTimeEffect) { effect ->
        when (effect) {
            is BookDetailOneTimeEffect.OpenBook -> {
                openBook(context, effect.book)
            }

            is BookDetailOneTimeEffect.ShowErrorSnackBar -> {
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = effect.text,
                        duration = SnackbarDuration.Short,
                    )
                }
            }
        }
    }
}

private fun openBook(context: Context, uiBook: UiBook) {
    val intent = Intent(context, MainActivity::class.java).apply {
        action = Intent.ACTION_VIEW
        data = uiBook.fileUri
    }

    context.startActivity(intent)
}
