package su.sv.wiki.presentation.root.model

/**
 * UI модель статьи Wiki
 */
data class UiWikiArticle(
    val title: String,
    val content: String,           // HTML контент
    val links: List<UiWikiLink>,
    val externalLinks: List<UiExternalLink>,
    val articleUrl: String = "",   // URL статьи на сайте
)

/**
 * UI модель внутренней ссылки на другую статью
 */
data class UiWikiLink(
    val text: String,              // Отображаемый текст
    val targetTitle: String,       // Заголовок целевой статьи
    val exists: Boolean,           // Существует ли статья
)

/**
 * UI модель внешней ссылки (видео, источники и т.д.)
 */
data class UiExternalLink(
    val text: String,              // Отображаемый текст
    val url: String,               // URL ссылки
)
