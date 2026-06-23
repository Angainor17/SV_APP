package su.sv.books.catalog.presentation.bookmarks.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import su.sv.books.R
import su.sv.commonui.theme.SVAPPTheme

/**
 * Диалог подтверждения удаления заметки
 *
 * @param noteText Текст заметки для отображения в диалоге
 * @param onConfirm Callback при подтверждении удаления
 * @param onDismiss Callback при отмене удаления
 */
@Composable
fun DeleteNoteDialog(
    noteText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.bookmarks_delete_dialog_title))
        },
        text = {
            Text(
                text = noteText.take(100) + if (noteText.length > 100) "..." else "",
                maxLines = 3
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.bookmarks_delete_dialog_yes))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.bookmarks_delete_dialog_no))
            }
        }
    )
}

//region Previews

@Preview(showBackground = true)
@Composable
private fun DeleteNoteDialogPreview() {
    SVAPPTheme {
        DeleteNoteDialog(
            noteText = "Это пример текста заметки, который будет удалён.",
            onConfirm = {},
            onDismiss = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DeleteNoteDialogLongTextPreview() {
    SVAPPTheme {
        DeleteNoteDialog(
            noteText = "Это очень длинный текст заметки, который должен обрезаться до 100 символов и показывать многоточие в конце, чтобы пользователь видел только начало заметки в диалоге удаления.",
            onConfirm = {},
            onDismiss = {}
        )
    }
}

//endregion
