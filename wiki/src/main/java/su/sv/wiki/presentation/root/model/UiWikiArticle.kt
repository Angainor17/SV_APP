package su.sv.wiki.presentation.root.model

/**
 * UI модель статьи Wiki
 */
data class UiWikiArticle(
    val title: String,
    val content: String,           // HTML контент
    val links: List<UiWikiLink>,
)

/**
 * UI модель ссылки на другую статью
 */
data class UiWikiLink(
    val text: String,              // Отображаемый текст
    val targetTitle: String,       // Заголовок целевой статьи
    val exists: Boolean,           // Существует ли статья
)
