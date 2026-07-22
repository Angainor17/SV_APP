package su.sv.bugreport.presentation.bugreport.ui

import android.net.Uri
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import su.sv.bugreport.R
import su.sv.bugreport.presentation.bugreport.viewmodel.model.BugReportState

@Composable
fun BugReportForm(
    state: BugReportState.Form,
    onDescriptionChange: (String) -> Unit,
    onSendEmailForFeedbackChange: (Boolean) -> Unit,
    onAddScreenshotsClick: () -> Unit,
    onRemoveScreenshot: (Uri) -> Unit,
    onSendReport: () -> Unit,
    contentPadding: PaddingValues,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Описание
        Text(
            text = stringResource(R.string.bug_report_description_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        Text(
            text = stringResource(R.string.bug_report_description_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )

        OutlinedTextField(
            value = state.description,
            onValueChange = onDescriptionChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.bug_report_description_label)) },
            placeholder = { Text(stringResource(R.string.bug_report_description_hint)) },
            isError = state.descriptionError != null,
            supportingText = {
                state.descriptionError?.let { error ->
                    Text(error)
                }
            },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences
            ),
            minLines = 3,
            // Авто-расширение в зависимости от текста
            maxLines = Int.MAX_VALUE,
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Скриншоты
        Text(
            text = stringResource(R.string.bug_report_screenshots_title, state.maxScreenshots),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        // Превью скриншотов
        if (state.screenshots.isNotEmpty()) {
            ScreenshotPreviewRow(
                screenshots = state.screenshots,
                onRemoveClick = onRemoveScreenshot
            )
        }

        // Кнопка добавления скриншотов
        if (state.canAddMoreScreenshots) {
            OutlinedButton(
                onClick = onAddScreenshotsClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    stringResource(
                        R.string.bug_report_add_screenshot,
                        state.screenshotCount,
                        state.maxScreenshots
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Чекбокс для обратной связи
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = state.sendEmailForFeedback,
                onCheckedChange = onSendEmailForFeedbackChange
            )
            Text(
                text = stringResource(R.string.bug_report_send_email_for_feedback),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Кнопка отправки
        Button(
            onClick = onSendReport,
            enabled = state.isSendButtonEnabled && !state.isSending,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (state.isSending) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text(stringResource(R.string.bug_report_send_button))
            }
        }
    }
}

@Composable
private fun ScreenshotPreviewRow(
    screenshots: List<Uri>,
    onRemoveClick: (Uri) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        screenshots.forEach { uri ->
            ScreenshotPreviewItem(
                uri = uri,
                onRemoveClick = { onRemoveClick(uri) }
            )
        }
    }
}

@Composable
private fun ScreenshotPreviewItem(
    uri: Uri,
    onRemoveClick: () -> Unit,
) {
    Card(
        modifier = Modifier.size(80.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = uri,
                contentDescription = stringResource(R.string.bug_report_screenshot_content_description),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            IconButton(
                onClick = onRemoveClick,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.bug_report_remove_screenshot),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}