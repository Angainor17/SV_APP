package su.sv.info.domain

import kotlinx.coroutines.delay
import su.sv.info.domain.model.LinkItem
import su.sv.info.domain.model.LinkItem.BuyBook
import su.sv.info.domain.model.LinkItem.DownloadBook
import su.sv.info.domain.model.LinkItem.DzenSv
import su.sv.info.domain.model.LinkItem.RedUniversity
import su.sv.info.domain.model.LinkItem.TelegramSv
import su.sv.info.domain.model.LinkItem.VkGroupSV
import su.sv.info.domain.model.LinkItem.VkLobbyo
import su.sv.info.domain.model.LinkItem.WinScience
import su.sv.info.domain.model.LinkItem.YouTubeLobbyo
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

class GetInfoLinksUseCase @Inject constructor() {

    /**
     * Возвращает список ссылок на внешние ресурсы (магазин, соцсети, каналы).
     *
     * В данный момент данные захардкожены. В будущем планируется загрузка
     * с бэкенда через API для возможности обновления ссылок без релиза приложения.
     */
    suspend fun execute(): Result<List<LinkItem>> {
        delay(500.milliseconds)

        return Result.success(
            listOf(
                BuyBook(
                    text = "Купить бумажную книгу",
                    url = "https://svg-shop.ru/",
                ),
                DownloadBook(
                    text = "Скачать электронную книгу",
                    url = "https://bibl.fra-mos.ru/category/svobodnoe-vremya/",
                ),
                VkGroupSV(
                    text = "Группа Свободное время",
                    url = "https://vk.com/svobodnoev",
                ),
                WinScience(
                    text = "Наука Побеждать",
                    url = "https://vk.com/kurs.kommunizma",
                ),
                RedUniversity(
                    text = "Красный университет. II отделение",
                    url = "https://vk.com/communism.university",
                ),
                VkLobbyo(
                    text = "Академия Смыслов Lobbyo",
                    url = "https://vk.com/lobbyo",
                ),
                YouTubeLobbyo(
                    text = "YouTube канал Академия Смыслов Lobbyo",
                    url = "https://www.youtube.com/channel/UCCWUurfuoWhXkFpjNZkLc-A",
                ),
                DzenSv(
                    text = "Дзен канал группы Свободное время",
                    url = "https://dzen.ru/svremya",
                ),
                TelegramSv(
                    text = "Телеграм канал группы Свободное время",
                    url = "https://t.me/SVremya",
                ),
            )
        )
    }
}
