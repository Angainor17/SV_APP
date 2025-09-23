package su.sv.news.presentation.root.viewmodel.actions

import su.sv.news.presentation.root.model.UiItemVideo
import su.sv.news.presentation.root.model.UiNewsItem

sealed class RootNewsActions {

    /** Завершилось обновление при помощи SwipeRefresh */
    object OnSwipeRefreshFinished : RootNewsActions()

    /** Обновление списка книг при помощи SwipeRefresh */
    object OnSwipeRefresh : RootNewsActions()

    /** Нажатие на сам элемент списка */
    data class OnNewsClick(val item: UiNewsItem) : RootNewsActions()

    /** Нажатие на видео в списке новости */
    data class OnNewsVideoClick(val item: UiItemVideo) : RootNewsActions()
}