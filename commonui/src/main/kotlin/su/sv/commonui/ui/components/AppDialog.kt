package su.sv.commonui.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import su.sv.commonui.R

/**
 * Диалоговое окно приложения
 *
 * @param title заголовок
 * @param onDismiss обработчик закрытия
 * @param text текст сообщения
 * @param confirmText текст кнопки подтверждения
 * @param onConfirm обработчик подтверждения (null скрывает кнопку)
 * @param dismissText текст кнопки отмены (null скрывает кнопку)
 * @param icon иконка (опционально)
 */
@Composable
fun AppAlertDialog(
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    text: String? = null,
    confirmText: String = stringResource(R.string.common_dialog_confirm),
    onConfirm: (() -> Unit)? = null,
    dismissText: String? = stringResource(R.string.common_dialog_cancel),
    icon: ImageVector? = null,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        icon = icon?.let { { Icon(imageVector = it, contentDescription = null) } },
        title = {
            Text(text = title)
        },
        text = text?.let {
            {
                Text(text = it)
            }
        },
        confirmButton = {
            if (onConfirm != null) {
                TextButton(onClick = onConfirm) {
                    Text(text = confirmText)
                }
            }
        },
        dismissButton = dismissText?.let {
            {
                TextButton(onClick = onDismiss) {
                    Text(text = it)
                }
            }
        }
    )
}

