package su.sv.info.rootinfo.mapper

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
import su.sv.info.rootinfo.model.UiLinkItem
import javax.inject.Inject

class InfoUiMapper @Inject constructor() {

    fun fromDomainToUi(links: List<LinkItem>) = links.map { fromDomainToUi(it) }

    private fun fromDomainToUi(link: LinkItem) = UiLinkItem(
        text = link.text,
        url = link.url,
        logo = link.getLogo(),
    )

    private fun LinkItem.getLogo() = when (this) {
        is BuyBook -> R.drawable.ic_sv
        is DownloadBook -> R.drawable.ic_fra
        is VkGroupSV, is WinScience, is RedUniversity, is VkLobbyo -> R.drawable.ic_vk
        is YouTubeLobbyo -> R.drawable.ic_youtube
        is DzenSv -> R.drawable.ic_dzen
        is TelegramSv -> R.drawable.ic_telegram
    }
}
