package su.sv.wiki.domain.model

/**
 * Доменная модель статьи Wiki
 */
data class WikiArticle(
    val title: String,
    val pageId: Int,
    val content: String,           // HTML контент
    val links: List<WikiLink>,     // Список внутренних ссылок в тексте
    val externalLinks: List<WikiExternalLink>, // Список внешних ссылок
)

/**
 * Модель внутренней ссылки на другую статью
 */
data class WikiLink(
    val title: String,             // Название связанной статьи
    val exists: Boolean,           // Существует ли статья
)

/**
 * Модель внешней ссылки (видео, источники и т.д.)
 */
data class WikiExternalLink(
    val text: String,              // Отображаемый текст
    val url: String,               // URL ссылки
)

/**
 * Результат поиска
 */
data class WikiSearchResult(
    val title: String,
    val pageId: Int,
    val snippet: String,
)

/**
 * Подсказка для поиска (автодополнение)
 */
data class WikiSearchSuggestion(
    val title: String,             // Название статьи
)
