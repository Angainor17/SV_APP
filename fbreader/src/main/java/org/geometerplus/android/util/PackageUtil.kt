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

package org.geometerplus.android.util

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri

object PackageUtil {
    private fun marketUri(pkg: String): Uri = Uri.parse("market://details?id=$pkg")

    @JvmStatic
    fun canBeStarted(context: Context, intent: Intent, checkSignature: Boolean): Boolean {
        val manager = context.applicationContext.packageManager
        val info = manager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        if (info == null) {
            return false
        }
        val activityInfo = info.activityInfo
        if (activityInfo == null) {
            return false
        }
        if (!checkSignature) {
            return true
        }
        return PackageManager.SIGNATURE_MATCH == manager.checkSignatures(context.packageName, activityInfo.packageName)
    }

    @JvmStatic
    fun installFromMarket(activity: Activity, pkg: String): Boolean {
        return try {
            activity.startActivity(Intent(Intent.ACTION_VIEW, marketUri(pkg)))
            true
        } catch (e: ActivityNotFoundException) {
            false
        }
    }
}
