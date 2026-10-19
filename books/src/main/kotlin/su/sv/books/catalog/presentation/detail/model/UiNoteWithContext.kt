package su.sv.books.catalog.presentation.detail.model

import androidx.compose.runtime.Immutable
import kotlinx.parcelize.Parcelize

/**
 * UI модель заметки с контекстом из книги
 *
 * @param id Уникальный идентификатор заметки
 * @param text Текст заметки (выделенный фрагмент)
 * @param sentenceBefore Текст предложения до заметки
 * @param sentenceAfter Текст предложения после заметки
 * @param page Номер страницы (приблизительный)
 * @param createdAt Timestamp создания
 */
@Immutable
@Parcelize
data class UiNoteWithContext(
    val id: String,
    val text: String,               // Текст заметки (выделенный фрагмент)
    val sentenceBefore: String?,    // Текст предложения до заметки
    val sentenceAfter: String?,     // Текст предложения после заметки
    val page: Int,                  // Номер страницы
    val createdAt: Long,            // Timestamp создания
    val startParagraph: Int,
    val startElement: Int,
    val startChar: Int,
    val endParagraph: Int,
    val endElement: Int,
    val endChar: Int,
) : android.os.Parcelable