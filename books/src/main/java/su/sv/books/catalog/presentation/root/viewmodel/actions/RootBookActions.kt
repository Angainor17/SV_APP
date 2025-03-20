package su.sv.books.catalog.presentation.root.viewmodel.actions

import su.sv.books.catalog.presentation.root.model.UiBook

sealed class RootBookActions {

    /** Нажатие на кнопку "Повторить", которая отобразилась из-за ошибки */
    object OnRetryClick : RootBookActions()

    /** Нажатие на иконку загрузки у книги */
    data class OnDownloadBookClick(val book: UiBook) : RootBookActions()

    /** Нажатие на сам элемент */
    data class OnBookClick(val book: UiBook) : RootBookActions()
}