package su.sv.books.catalog.presentation.detail.model

import androidx.compose.runtime.Immutable

/**
 * Состояние блока заметок на карточке книги
 */
@Immutable
sealed class UiNotesBlockState {

    /**
     * Загрузка заметок
     */
    @Immutable
    object Loading : UiNotesBlockState()

    /**
     * Заметки скрыты (нет заметок или книга не скачана)
     */
    @Immutable
    object Hidden : UiNotesBlockState()

    /**
     * Контент - список заметок с контекстом
     *
     * @param notes Список заметок (максимум 5)
     * @param totalCount Общее количество заметок для книги
     * @param hasMore true если заметок больше чем показано
     */
    @Immutable
    data class Content(
        val notes: List<UiNoteWithContext>,
        val totalCount: Int,
        val hasMore: Boolean,
    ) : UiNotesBlockState()
}