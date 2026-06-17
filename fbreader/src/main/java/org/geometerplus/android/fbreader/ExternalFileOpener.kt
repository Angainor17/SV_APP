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

package org.geometerplus.android.fbreader

import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import org.geometerplus.android.fbreader.api.FBReaderIntents
import org.geometerplus.android.fbreader.formatPlugin.PluginUtil
import org.geometerplus.android.util.PackageUtil
import org.geometerplus.fbreader.book.Book
import org.geometerplus.fbreader.book.Bookmark
import org.geometerplus.fbreader.fbreader.FBReaderApp
import org.geometerplus.fbreader.formats.ExternalFormatPlugin
import org.geometerplus.zlibrary.core.options.Config
import org.geometerplus.zlibrary.core.options.ZLStringOption
import org.geometerplus.zlibrary.core.resources.ZLResource
import java.math.BigInteger
import java.util.Random

internal class ExternalFileOpener(private val reader: FBReader) : FBReaderApp.ExternalFileOpener {
    private val pluginCode = BigInteger(80, Random()).toString()
    @Volatile
    private var dialog: AlertDialog? = null

    override fun openFile(plugin: ExternalFormatPlugin, book: Book, bookmark: Bookmark) {
        dialog?.let {
            it.dismiss()
            dialog = null
        }

        val intent = PluginUtil.createIntent(plugin, FBReaderIntents.Action.PLUGIN_VIEW)
        FBReaderIntents.putBookExtra(intent, book)
        FBReaderIntents.putBookmarkExtra(intent, bookmark)
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)

        val option = ZLStringOption("PluginCode", plugin.packageName(), "")
        option.value = pluginCode
        intent.putExtra("PLUGIN_CODE", pluginCode)

        Config.Instance()?.runOnConnect {
            try {
                reader.startActivity(intent)
                reader.overridePendingTransition(0, 0)
            } catch (e: ActivityNotFoundException) {
                showErrorDialog(plugin, book)
            }
        }
    }

    private fun showErrorDialog(plugin: ExternalFormatPlugin, book: Book) {
        val rootResource = ZLResource.resource("dialog")
        val buttonResource = rootResource.getResource("button")
        val dialogResource = rootResource.getResource("missingPlugin")
        val builder = AlertDialog.Builder(reader)
            .setTitle(dialogResource.value)
            .setMessage(dialogResource.getResource("message").value.replace("%s", plugin.supportedFileType()))
            .setPositiveButton(buttonResource.getResource("yes").value) { _, _ ->
                PackageUtil.installFromMarket(reader, plugin.packageName())
                dialog = null
            }
            .setNegativeButton(buttonResource.getResource("no").value) { _, _ ->
                reader.onPluginNotFoundInternal(book)
                dialog = null
            }
            .setOnCancelListener {
                reader.onPluginNotFoundInternal(book)
                dialog = null
            }

        val showDialog = Runnable {
            dialog = builder.create()
            dialog?.show()
        }
        if (!reader.isPaused) {
            reader.runOnUiThread(showDialog)
        } else {
            reader.onResumeAction = showDialog
        }
    }
}
