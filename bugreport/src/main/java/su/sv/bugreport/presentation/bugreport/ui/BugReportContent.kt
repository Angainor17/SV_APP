package su.sv.bugreport.presentation.bugreport.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.terrakok.modo.stack.LocalStackNavigation
import com.github.terrakok.modo.stack.back
import su.sv.bugreport.R
import su.sv.bugreport.presentation.bugreport.viewmodel.BugReportViewModel
import su.sv.bugreport.presentation.bugreport.viewmodel.model.BugReportAction
import su.sv.bugreport.presentation.bugreport.viewmodel.model.BugReportEffect
import su.sv.bugreport.presentation.bugreport.viewmodel.model.BugReportState
import su.sv.commonui.ui.OneTimeEffect
import su.sv.commonui.ui.components.AppToolbar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BugReportContent(
    viewModel: BugReportViewModel = hiltViewModel(),
) {
    val state = viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val stackNavigation = LocalStackNavigation.current
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    // Photo Picker для скриншотов
    val pickMediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(BugReportState.MAX_SCREENSHOTS)
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.onAction(BugReportAction.OnScreenshotsSelected(uris))
        }
    }

    // Обработка эффектов
    OneTimeEffect(viewModel.effect) { effect ->
        when (effect) {
            is BugReportEffect.OpenEmailClient -> {
                context.startActivity(effect.intent)
            }

            is BugReportEffect.CloseScreen -> {
                stackNavigation.back()
            }

            is BugReportEffect.ShowError -> {
                // Показать Toast или Snackbar - можно добавить позже
            }
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            AppToolbar(
                title = context.getString(R.string.bug_report_toolbar_title),
                onNavigationClick = { stackNavigation.back() },
                scrollBehavior = scrollBehavior,
            )
        }
    ) { contentPadding ->
        when (val currentState = state.value) {
            is BugReportState.Form -> {
                BugReportForm(
                    state = currentState,
                    onDescriptionChange = { text ->
                        viewModel.onAction(BugReportAction.OnDescriptionChange(text))
                    },
                    onSendEmailForFeedbackChange = { checked ->
                        viewModel.onAction(BugReportAction.OnSendEmailForFeedbackChange(checked))
                    },
                    onAddScreenshotsClick = {
                        pickMediaLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onRemoveScreenshot = { uri ->
                        viewModel.onAction(BugReportAction.OnRemoveScreenshot(uri))
                    },
                    onSendReport = {
                        viewModel.onAction(BugReportAction.OnSendReport)
                    },
                    contentPadding = contentPadding,
                )
            }

            is BugReportState.Sending -> {
                SendingContent()
            }

            is BugReportState.Success -> {
                SuccessScreen(
                    onDismiss = {
                        viewModel.onAction(BugReportAction.OnSuccessDismiss)
                    }
                )
            }

            is BugReportState.SuccessWithEmail -> {
                // Показываем успех и открываем email
                SuccessScreen(
                    onDismiss = {
                        context.startActivity(currentState.emailIntent)
                        viewModel.onAction(BugReportAction.OnSuccessDismiss)
                    }
                )
            }

            is BugReportState.Error -> {
                ErrorContent(
                    message = currentState.message,
                    onRetry = {
                        viewModel.onAction(BugReportAction.OnRetry)
                    }
                )
            }
        }
    }
}