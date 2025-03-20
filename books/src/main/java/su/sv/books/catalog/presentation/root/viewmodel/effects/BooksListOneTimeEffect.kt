package su.sv.books.catalog.presentation.root.viewmodel.effects

import androidx.annotation.StringRes

/**
 * Единожды отображаемые события на экране списка книг
 */
sealed class BooksListOneTimeEffect {

    /** Отображение снека об ошибке */
    data class ShowErrorSnackBar(
        @StringRes val textResId: Int,
    ) : BooksListOneTimeEffect()
}