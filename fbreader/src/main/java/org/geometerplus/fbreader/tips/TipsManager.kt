/*
 * Copyright (C) 2009-2015 FBReader.ORG Limited <contact@fbreader.org>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
 */

package org.geometerplus.fbreader.tips

import org.geometerplus.fbreader.network.NetworkLibrary
import org.geometerplus.fbreader.network.atom.ATOMEntry
import org.geometerplus.fbreader.network.atom.ATOMFeedHandler
import org.geometerplus.fbreader.network.atom.ATOMFeedMetadata
import org.geometerplus.fbreader.network.atom.ATOMXMLReader
import org.geometerplus.zlibrary.core.filesystem.ZLFile
import org.geometerplus.zlibrary.core.network.QuietNetworkContext
import org.geometerplus.zlibrary.core.options.Config
import org.geometerplus.zlibrary.core.options.ZLBooleanOption
import org.geometerplus.zlibrary.core.options.ZLIntegerOption
import org.geometerplus.zlibrary.core.util.SystemInfo
import java.io.File
import java.util.Date

class TipsManager(private val systemInfo: SystemInfo) {

    companion object {
        @JvmField
        val tipsAreInitializedOption = ZLBooleanOption("tips", "tipsAreInitialized", false)

        @JvmField
        val showTipsOption = ZLBooleanOption("tips", "showTips", false)
    }

    // time when last tip was shown, 2^16 milliseconds
    private val lastShownOption = ZLIntegerOption("tips", "shownAt", 0)
    // index of next tip to show
    private val indexOption = ZLIntegerOption("tips", "index", 0)
    private val delay = (24 * 60 * 60 * 1000) shr 16 // 1 day

    @Volatile
    private var downloadInProgress = false
    private var tips: List<Tip>? = null

    private val url: String
        get() = "https://data.fbreader.org/tips/tips.php"

    private val localFilePath: String
        get() = systemInfo.networkCacheDirectory() + "/tips/tips.xml"

    private fun getTips(): List<Tip>? {
        if (tips == null) {
            val file = ZLFile.createFileByPath(localFilePath)
            if (file.exists()) {
                val handler = TipsFeedHandler()
                ATOMXMLReader<ATOMFeedMetadata, ATOMEntry>(NetworkLibrary.Instance(systemInfo), handler as ATOMFeedHandler<ATOMFeedMetadata, ATOMEntry>, false).readQuietly(file)
                val tipsList = handler.tips
                if (tipsList.isNotEmpty()) {
                    tips = tipsList
                }
            }
        }
        return tips
    }

    fun hasNextTip(): Boolean {
        val tipsList = getTips() ?: return false

        val index = indexOption.value
        if (index >= tipsList.size) {
            File(localFilePath).delete()
            indexOption.value = 0
            return false
        }

        return true
    }

    fun getNextTip(): Tip? {
        val tipsList = getTips() ?: return null

        val index = indexOption.value
        if (index >= tipsList.size) {
            File(localFilePath).delete()
            indexOption.value = 0
            return null
        }

        indexOption.value = index + 1
        lastShownOption.value = currentTime()
        return tipsList[index]
    }

    private fun currentTime(): Int = (Date().time shr 16).toInt()

    fun requiredAction(): Action {
        if (showTipsOption.value) {
            if (hasNextTip()) {
                return if (lastShownOption.value + delay < currentTime()) Action.Show else Action.None
            } else {
                return if (downloadInProgress) Action.None else Action.Download
            }
        } else if (!tipsAreInitializedOption.value) {
            //return Action.Initialize
            return Action.None
        }
        return Action.None
    }

    @Synchronized
    fun startDownloading() {
        if (requiredAction() != Action.Download) {
            return
        }

        downloadInProgress = true

        Config.Instance()?.runOnConnect {
            val tipsFile = File(localFilePath)
            tipsFile.parentFile?.mkdirs()
            Thread {
                QuietNetworkContext().downloadToFileQuietly(url, tipsFile)
                downloadInProgress = false
            }.start()
        }
    }

    enum class Action {
        Initialize,
        Show,
        Download,
        None
    }
}
