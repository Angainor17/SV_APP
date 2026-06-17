package org.geometerplus.fbreader.fbreader.options

import org.geometerplus.zlibrary.core.options.ZLBooleanOption
import org.geometerplus.zlibrary.core.options.ZLEnumOption

class SyncOptions {

    companion object {
        const val DOMAIN = "books.fbreader.org"
        const val BASE_URL = "https://$DOMAIN/"
        const val OPDS_URL = "https://$DOMAIN/opds"
        const val REALM = "FBReader book network"
    }

    val enabled = ZLBooleanOption("Sync", "Enabled", false)
    val uploadAllBooks = ZLEnumOption("Sync", "UploadAllBooks", Condition.viaWifi)
    val positions = ZLEnumOption("Sync", "Positions", Condition.always)
    val changeCurrentBook = ZLBooleanOption("Sync", "ChangeCurrentBook", true)
    val bookmarks = ZLEnumOption("Sync", "Bookmarks", Condition.always)
    val customShelves = ZLEnumOption("Sync", "CustomShelves", Condition.always)
    val metainfo = ZLEnumOption("Sync", "Metainfo", Condition.always)

    enum class Condition {
        never, viaWifi, always
    }
}
