package su.sv.news.presentation.root.viewmodel.actions

import su.sv.news.presentation.root.model.UiNewsItem

sealed class RootNewsActions {

    /** Нажатие на кнопку "Повторить", которая отобразилась из-за ошибки */
    object OnRetryClick : RootNewsActions()

    /** Обновление списка книг при помощи SwipeRefresh */
    object OnSwipeRefresh : RootNewsActions()

    /** Нажатие на сам элемент списка */
    data class OnNewsClick(val item: UiNewsItem) : RootNewsActions()
}