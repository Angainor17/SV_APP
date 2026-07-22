package su.sv.info.rootinfo.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.terrakok.modo.stack.LocalStackNavigation
import com.github.terrakok.modo.stack.forward
import su.sv.bugreport.presentation.nav.BugReportScreen
import su.sv.commonui.ui.OneTimeEffect
import su.sv.commonui.ui.components.AppToolbar
import su.sv.commonui.ui.components.FullScreenError
import su.sv.commonui.ui.components.FullScreenLoading
import su.sv.info.R
import su.sv.info.rootinfo.RootInfoViewModel
import su.sv.info.rootinfo.model.RootInfoEffect
import su.sv.info.rootinfo.model.UiInfoState
import su.sv.info.rootinfo.viewmodel.RootInfoActions

/**
 * Информационный экран
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RootInfo(viewModel: RootInfoViewModel = hiltViewModel()) {
    val state = viewModel.state.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val stackNavigation = LocalStackNavigation.current

    // Обработка эффектов навигации
    OneTimeEffect(viewModel.effect) { effect ->
        when (effect) {
            is RootInfoEffect.OpenBugReport -> {
                stackNavigation.forward(BugReportScreen())
            }
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            AppToolbar(
                title = stringResource(R.string.info_toolbar_title),
                windowInsets = WindowInsets(0.dp),
                scrollBehavior = scrollBehavior,
                actions = {
                    IconButton(
                        onClick = { viewModel.onAction(RootInfoActions.OnBugReportClick) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.BugReport,
                            contentDescription = stringResource(R.string.info_bug_report_content_description),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
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
