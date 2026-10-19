package su.sv.info.domain.model

sealed class LinkItem(
    val text: String,
    val url: String,
) {

    /** Купить бумажную книгу */
    class BuyBook(text: String, url: String) : LinkItem(text, url)

    /** Скачать электронную книгу */
    class DownloadBook(text: String, url: String) : LinkItem(text, url)

    /** Группа Свободное время */
    class VkGroupSV(text: String, url: String) : LinkItem(text, url)

    /** Наука Побеждать */
    class WinScience(text: String, url: String) : LinkItem(text, url)

    /** Красный университет. II отделение */
    class RedUniversity(text: String, url: String) : LinkItem(text, url)

    /** Академия Смыслов Lobbyo */
    class VkLobbyo(text: String, url: String) : LinkItem(text, url)

    /** YouTube канал Академия Смыслов Lobbyo */
    class YouTubeLobbyo(text: String, url: String) : LinkItem(text, url)

    /** Дзен канал группы Свободное время */
    class DzenSv(text: String, url: String) : LinkItem(text, url)

    /** Телеграм канал группы Свободное время */
    class TelegramSv(text: String, url: String) : LinkItem(text, url)
}
