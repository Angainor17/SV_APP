/*
 * Copyright (C) 2010-2015 FBReader.ORG Limited <contact@fbreader.org>
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

package org.geometerplus.android.fbreader.network.auth

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import org.geometerplus.R
import org.geometerplus.zlibrary.core.resources.ZLResource
import java.net.URI

open class ServiceNetworkContext(private val myService: Service) : AndroidNetworkContext() {

    override fun getContext(): Context = myService

    override fun authenticateWeb(uri: URI, realm: String, authUrl: String, completeUrl: String, verificationUrl: String): Map<String, String> {
        val notificationManager = myService.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val intent = Intent(myService, WebAuthorisationScreen::class.java)
        intent.data = Uri.parse(authUrl)
        intent.putExtra(WebAuthorisationScreen.COMPLETE_URL_KEY, completeUrl)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.addFlags(Intent.FLAG_FROM_BACKGROUND)
        val pendingIntent = PendingIntent.getActivity(myService, 0, intent, 0)
        val text = ZLResource.resource("dialog")
            .getResource("backgroundAuthentication")
            .getResource("message")
            .value
        val notification = NotificationCompat.Builder(myService)
            .setSmallIcon(R.drawable.fbreader)
            .setTicker(text)
            .setContentTitle(realm)
            .setContentText(text)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(0, notification)
        return errorMap("Notification sent")
    }
}
