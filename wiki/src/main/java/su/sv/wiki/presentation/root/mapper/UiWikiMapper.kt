package su.sv.wiki.presentation.root.mapper

import su.sv.wiki.domain.model.WikiArticle
import su.sv.wiki.domain.model.WikiExternalLink
import su.sv.wiki.domain.model.WikiLink
import su.sv.wiki.presentation.root.model.UiExternalLink
import su.sv.wiki.presentation.root.model.UiWikiArticle
import su.sv.wiki.presentation.root.model.UiWikiLink
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Маппер доменных моделей в UI модели
 */
@Singleton
class UiWikiMapper @Inject constructor() {

    /**
     * Маппинг статьи в UI модель
     */
    fun mapToUi(article: WikiArticle, isFavorite: Boolean = false): UiWikiArticle {
        return UiWikiArticle(
            title = article.title,
            content = article.content,
            links = article.links.map { it.toUi() },
            externalLinks = article.externalLinks.map { it.toUi() },
        )
    }

    /**
     * Маппинг внутренней ссылки в UI модель
     */
    private fun WikiLink.toUi(): UiWikiLink {
        return UiWikiLink(
            text = this.title,
            targetTitle = this.title,
            exists = this.exists,
        )
    }

    /**
     * Маппинг внешней ссылки в UI модель
     */
    private fun WikiExternalLink.toUi(): UiExternalLink {
        return UiExternalLink(
            text = this.text,
            url = this.url,
        )
    }
}
