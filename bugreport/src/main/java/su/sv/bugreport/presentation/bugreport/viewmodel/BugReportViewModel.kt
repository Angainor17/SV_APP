package su.sv.bugreport.presentation.bugreport.viewmodel

import android.content.Context
import android.os.Build
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import su.sv.bugreport.domain.SendBugReportUseCase
import su.sv.bugreport.domain.SendEmailReportUseCase
import su.sv.bugreport.domain.model.BugReport
import su.sv.bugreport.presentation.bugreport.viewmodel.model.BugReportAction
import su.sv.bugreport.presentation.bugreport.viewmodel.model.BugReportEffect
import su.sv.bugreport.presentation.bugreport.viewmodel.model.BugReportState
import su.sv.commonarchitecture.presentation.base.BaseViewModel
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class BugReportViewModel @Inject constructor(
    private val sendBugReportUseCase: SendBugReportUseCase,
    private val sendEmailReportUseCase: SendEmailReportUseCase,
    @ApplicationContext private val context: Context,
) : BaseViewModel() {

    private val _state = MutableStateFlow<BugReportState>(BugReportState.Form())
    val state: StateFlow<BugReportState> get() = _state

    private val _effect = Channel<BugReportEffect>(capacity = Channel.BUFFERED)
    val effect: Flow<BugReportEffect> get() = _effect.receiveAsFlow()

    fun onAction(action: BugReportAction) {
        when (action) {
            is BugReportAction.OnDescriptionChange -> {
                updateDescription(action.text)
            }

            is BugReportAction.OnSendEmailForFeedbackChange -> {
                updateSendEmailForFeedback(action.checked)
            }

            is BugReportAction.OnScreenshotsSelected -> {
                addScreenshots(action.uris)
            }

            is BugReportAction.OnRemoveScreenshot -> {
                removeScreenshot(action.uri)
            }

            is BugReportAction.OnSendReport -> {
                sendReport()
            }

            is BugReportAction.OnSuccessDismiss -> {
                _effect.trySend(BugReportEffect.CloseScreen)
            }

            is BugReportAction.OnRetry -> {
                retrySend()
            }
        }
    }

    private fun updateDescription(text: String) {
        _state.update { currentState ->
            if (currentState is BugReportState.Form) {
                val error = if (text.isNotEmpty() && text.length < MIN_DESCRIPTION_LENGTH) {
                    "Минимум $MIN_DESCRIPTION_LENGTH символов"
                } else {
                    null
                }
                currentState.copy(
                    description = text,
                    descriptionError = error,
                    isSendButtonEnabled = text.length >= MIN_DESCRIPTION_LENGTH
                )
            } else {
                currentState
            }
        }
    }

    private fun updateSendEmailForFeedback(checked: Boolean) {
        _state.update { currentState ->
            if (currentState is BugReportState.Form) {
                currentState.copy(sendEmailForFeedback = checked)
            } else {
                currentState
            }
        }
    }

    private fun addScreenshots(uris: List<android.net.Uri>) {
        _state.update { currentState ->
            if (currentState is BugReportState.Form) {
                val newScreenshots = (currentState.screenshots + uris)
                    .distinctBy { it.toString() }
                    .take(BugReportState.MAX_SCREENSHOTS)
                currentState.copy(screenshots = newScreenshots)
            } else {
                currentState
            }
        }
    }

    private fun removeScreenshot(uri: android.net.Uri) {
        _state.update { currentState ->
            if (currentState is BugReportState.Form) {
                currentState.copy(
                    screenshots = currentState.screenshots.filter { it != uri }
                )
            } else {
                currentState
            }
        }
    }

    private fun sendReport() {
        viewModelScope.launch {
            val currentState = _state.value
            if (currentState !is BugReportState.Form) return@launch

            _state.value = BugReportState.Sending

            val report = createBugReport(currentState)

            sendBugReportUseCase.execute(report)
                .onSuccess {
                    Timber.tag("voronin").d("Bug report sent successfully")

                    if (currentState.sendEmailForFeedback) {
                        // Открываем email после успеха
                        val emailIntent = sendEmailReportUseCase.execute(report)
                        _state.value = BugReportState.SuccessWithEmail(emailIntent)
                    } else {
                        _state.value = BugReportState.Success
                    }
                }
                .onFailure { error ->
                    Timber.tag("voronin").e(error, "Failed to send bug report")
                    _state.value = BugReportState.Error(
                        message = error.message ?: "Не удалось отправить отчет"
                    )
                }
        }
    }

    private fun retrySend() {
        _state.value = BugReportState.Form()
    }

    private fun createBugReport(formState: BugReportState.Form): BugReport {
        return BugReport(
            description = formState.description,
            screenshots = formState.screenshots,
            appVersion = getAppVersion(),
            deviceModel = Build.MODEL,
            deviceManufacturer = Build.MANUFACTURER,
            androidVersion = Build.VERSION.RELEASE,
        )
    }

    private fun getAppVersion(): String {
        return try {
            context.packageManager
                .getPackageInfo(context.packageName, 0)
                .versionName ?: "unknown"
        } catch (e: Exception) {
            "unknown"
        }
    }

    companion object {
        const val MIN_DESCRIPTION_LENGTH = 10
    }
}