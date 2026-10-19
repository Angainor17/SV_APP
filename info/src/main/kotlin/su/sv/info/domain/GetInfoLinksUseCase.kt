package su.sv.info.domain

import kotlinx.coroutines.delay
import su.sv.commonarchitecture.managers.ResourcesRepository
import su.sv.info.R
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

class GetInfoLinksUseCase @Inject constructor(
    private val resourcesRepository: ResourcesRepository,
) {

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
                    text = resourcesRepository.getString(R.string.info_link_buy_book),
                    url = resourcesRepository.getString(R.string.info_url_buy_book),
                ),
                DownloadBook(
                    text = resourcesRepository.getString(R.string.info_link_download_book),
                    url = resourcesRepository.getString(R.string.info_url_download_book),
                ),
                VkGroupSV(
                    text = resourcesRepository.getString(R.string.info_link_vk_group),
                    url = resourcesRepository.getString(R.string.info_url_vk_group),
                ),
                WinScience(
                    text = resourcesRepository.getString(R.string.info_link_win_science),
                    url = resourcesRepository.getString(R.string.info_url_win_science),
                ),
                RedUniversity(
                    text = resourcesRepository.getString(R.string.info_link_red_university),
                    url = resourcesRepository.getString(R.string.info_url_red_university),
                ),
                VkLobbyo(
                    text = resourcesRepository.getString(R.string.info_link_vk_lobbyo),
                    url = resourcesRepository.getString(R.string.info_url_vk_lobbyo),
                ),
                YouTubeLobbyo(
                    text = resourcesRepository.getString(R.string.info_link_youtube_lobbyo),
                    url = resourcesRepository.getString(R.string.info_url_youtube_lobbyo),
                ),
                DzenSv(
                    text = resourcesRepository.getString(R.string.info_link_dzen_sv),
                    url = resourcesRepository.getString(R.string.info_url_dzen_sv),
                ),
                TelegramSv(
                    text = resourcesRepository.getString(R.string.info_link_telegram_sv),
                    url = resourcesRepository.getString(R.string.info_url_telegram_sv),
                ),
            )
        )
    }
}
