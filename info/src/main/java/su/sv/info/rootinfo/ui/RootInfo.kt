package su.sv.info.rootinfo.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import su.sv.commonui.ui.components.AppToolbarSimple
import su.sv.commonui.ui.components.FullScreenError
import su.sv.commonui.ui.components.FullScreenLoading
import su.sv.info.R
import su.sv.info.rootinfo.RootInfoViewModel
import su.sv.info.rootinfo.model.UiInfoState
import su.sv.info.rootinfo.viewmodel.RootInfoActions

/**
 * Информационный экран
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RootInfo(viewModel: RootInfoViewModel = hiltViewModel()) {
    val state = viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AppToolbarSimple(
                title = stringResource(R.string.info_toolbar_title),
                windowInsets = WindowInsets(0.dp),
            )
        }
    ) { contentPadding ->
        when (val currentState = state.value) {
            is UiInfoState.Content -> {
                InfoContent(
                    actionsHandler = viewModel,
                    state = currentState,
                    contentPadding = contentPadding,
                )
            }

            UiInfoState.Loading -> {
                FullScreenLoading()
            }

            is UiInfoState.Failure -> {
                FullScreenError(
                    onRetry = { viewModel.onAction(RootInfoActions.OnRetryClick) }
                )
            }
        }
    }
}
