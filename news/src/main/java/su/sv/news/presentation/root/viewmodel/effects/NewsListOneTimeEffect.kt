package su.sv.news.presentation.root.viewmodel.effects

import su.sv.news.presentation.root.model.UiNewsMedia
import su.sv.news.presentation.root.model.UiNewsItem

/**
 * Единожды отображаемые события на экране списка новостей
 */
sealed class NewsListOneTimeEffect {

    /** Отображение снека об ошибке */
    data class ShowErrorSnackBar(
        val text: String,
    ) : NewsListOneTimeEffect()

    /** Отображение информации о новости */
    data class OpenNewsItem(
        val newItem: UiNewsItem,
    ) : NewsListOneTimeEffect()

    /** Открыть видео из новости */
    data class OpenNewsVideo(
        val item: UiNewsMedia.ItemVideo,
    ) : NewsListOneTimeEffect()
}