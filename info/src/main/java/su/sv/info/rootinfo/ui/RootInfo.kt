package su.sv.info.rootinfo.ui

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import su.sv.commonui.ui.FullScreenError
import su.sv.commonui.ui.FullScreenLoading
import su.sv.info.rootinfo.RootInfoViewModel
import su.sv.info.rootinfo.model.UiInfoState
import su.sv.info.rootinfo.viewmodel.RootInfoActions

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun RootInfo(viewModel: RootInfoViewModel = hiltViewModel()) {
    val state = viewModel.state.collectAsStateWithLifecycle()

    when (state.value) {
        is UiInfoState.Content -> {
            InfoContent(
                actionsHandler = viewModel,
                state = state.value as UiInfoState.Content,
            )
        }

        UiInfoState.Loading -> {
            FullScreenLoading()
        }

        is UiInfoState.Failure -> {
            FullScreenError {
                viewModel.onAction(RootInfoActions.OnRetryClick)
            }
        }
    }
}
