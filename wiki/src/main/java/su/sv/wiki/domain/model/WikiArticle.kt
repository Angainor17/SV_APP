package su.sv.wiki.domain.model

/**
 * Доменная модель статьи Wiki
 */
data class WikiArticle(
    val title: String,
    val pageId: Int,
    val content: String,           // HTML контент
    val links: List<WikiLink>,     // Список ссылок в тексте
)

/**
 * Модель ссылки на другую статью
 */
data class WikiLink(
    val title: String,             // Название связанной статьи
    val exists: Boolean,           // Существует ли статья
)

/**
 * Результат поиска
 */
data class WikiSearchResult(
    val title: String,
    val pageId: Int,
    val snippet: String,
)
