package su.sv.wiki.presentation.root.model

import androidx.compose.runtime.Immutable

/**
 * UI модель статьи Wiki
 * @Immutable - оптимизация Compose recomposition
 */
@Immutable
data class UiWikiArticle(
    val title: String,
    val content: String,           // HTML контент
    val links: List<UiWikiLink>,
    val externalLinks: List<UiExternalLink>,
    val articleUrl: String = "",   // URL статьи на сайте
    val imageUrl: String? = null,  // URL картинки из статьи (если есть)
)

/**
 * UI модель внутренней ссылки на другую статью
 * @Immutable - оптимизация Compose recomposition
 */
@Immutable
data class UiWikiLink(
    val text: String,              // Отображаемый текст
    val targetTitle: String,       // Заголовок целевой статьи
    val exists: Boolean,           // Существует ли статья
)

/**
 * UI модель внешней ссылки (видео, источники и т.д.)
 * @Immutable - оптимизация Compose recomposition
 */
@Immutable
data class UiExternalLink(
    val text: String,              // Отображаемый текст
    val url: String,               // URL ссылки
)
