package su.sv.news.presentation.root.viewmodel.actions

import su.sv.news.presentation.root.model.UiNewsItem
import su.sv.news.presentation.root.model.UiNewsMedia

sealed class RootNewsActions {

    /** Нажатие на сам элемент списка */
    data class OnNewsClick(val item: UiNewsItem) : RootNewsActions()

    /** Нажатие на видео в списке новости */
    data class OnNewsMediaClick(val item: UiNewsMedia) : RootNewsActions()

    /** Нажатие на изображение в новости */
    data class OnNewsImageClick(
        val newsItem: UiNewsItem,
        val imageIndex: Int = 0,
    ) : RootNewsActions()
}