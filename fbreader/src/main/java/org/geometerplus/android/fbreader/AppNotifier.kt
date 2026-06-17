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

import android.app.Activity
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.app.NotificationCompat
import org.geometerplus.R
import org.geometerplus.android.fbreader.network.BookDownloaderService
import org.geometerplus.android.fbreader.sync.MissingBookActivity
import org.geometerplus.fbreader.Paths
import org.geometerplus.fbreader.fbreader.FBReaderApp
import org.geometerplus.fbreader.network.NetworkImage
import org.geometerplus.fbreader.network.sync.SyncData
import org.geometerplus.fbreader.network.urlInfo.UrlInfo
import org.geometerplus.zlibrary.ui.android.network.SQLiteCookieDatabase

internal class AppNotifier(private val activity: Activity) : FBReaderApp.Notifier {
    private val latestHashes = mutableListOf<String>()
    @Volatile
    private var latestNotificationStamp: Long = 0

    override fun showMissingBookNotification(info: SyncData.ServerBookInfo) {
        synchronized(this) {
            latestHashes.retainAll(info.hashes)
            if (latestHashes.isNotEmpty() &&
                latestNotificationStamp > System.currentTimeMillis() - 5 * 60 * 1000) {
                return
            }
            latestHashes.addAll(info.hashes)
            latestNotificationStamp = System.currentTimeMillis()
        }
        Thread {
            showMissingBookNotificationInternal(info)
        }.start()
    }

    private fun showMissingBookNotificationInternal(info: SyncData.ServerBookInfo) {
        val errorTitle = MissingBookActivity.errorTitle()

        val notificationManager =
            activity.getSystemService(Activity.NOTIFICATION_SERVICE) as NotificationManager
        val builder = NotificationCompat.Builder(activity)
            .setSmallIcon(R.drawable.fbreader)
            .setTicker(errorTitle)
            .setContentTitle(errorTitle)
            .setContentText(info.title)

        if (info.thumbnailUrl != null) {
            SQLiteCookieDatabase.init(activity)
            val thumbnail = NetworkImage(info.thumbnailUrl, Paths.systemInfo(activity))
            thumbnail.synchronize()
            try {
                builder.setLargeIcon(
                    BitmapFactory.decodeStream(thumbnail.realImage?.inputStream())
                )
            } catch (t: Throwable) {
                // ignore
            }
        }

        val notificationId = if (info.hashes.isNotEmpty())
            info.hashes[0].hashCode()
        else
            NotificationUtil.MISSING_BOOK_ID

        var uri: Uri? = null
        try {
            uri = Uri.parse(info.downloadUrl)
        } catch (e: Exception) {
        }
        builder.setAutoCancel(uri == null)
        if (uri != null) {
            val downloadIntent = Intent(activity, MissingBookActivity::class.java)
            downloadIntent
                .setData(uri)
                .putExtra(BookDownloaderService.Key.FROM_SYNC, true)
                .putExtra(BookDownloaderService.Key.BOOK_MIME, info.mimetype)
                .putExtra(BookDownloaderService.Key.BOOK_KIND, UrlInfo.Type.Book)
                .putExtra(BookDownloaderService.Key.BOOK_TITLE, info.title)
                .putExtra(BookDownloaderService.Key.NOTIFICATION_TO_DISMISS_ID, notificationId)
            builder.setContentIntent(PendingIntent.getActivity(activity, 0, downloadIntent, 0))
        } else {
            builder.setContentIntent(PendingIntent.getActivity(activity, 0, Intent(), 0))
        }
        notificationManager.notify(notificationId, builder.build())
    }
}
