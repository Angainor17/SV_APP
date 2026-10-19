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

package org.geometerplus.android.fbreader.dict

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.view.View

import com.google.android.material.snackbar.Snackbar

import org.geometerplus.android.util.PackageUtil

object InternalUtil {
    @JvmStatic
    fun installDictionaryIfNotInstalled(activity: Activity, info: DictionaryUtil.PackageInfo) {
        if (PackageUtil.canBeStarted(activity, info.getActionIntent("test"), false)) {
            return
        }

        try {
            val intent = info.getActionIntent("install")
            activity.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @JvmStatic
    fun startDictionaryActivity(fbreader: Activity, intent: Intent, info: DictionaryUtil.PackageInfo) {
        try {
            fbreader.startActivity(intent)
            fbreader.overridePendingTransition(0, 0)
        } catch (e: ActivityNotFoundException) {
            installDictionaryIfNotInstalled(fbreader, info)
        }
    }

    @JvmStatic
    fun showSnackbar(activity: Activity, text: String, duration: Int) {
        val rootView = activity.findViewById<View>(android.R.id.content)
        if (rootView != null) {
            Snackbar.make(rootView, text, if (duration == 0) Snackbar.LENGTH_SHORT else Snackbar.LENGTH_LONG)
                .show()
        }
    }

    @JvmStatic
    fun showSnackbarWithAction(activity: Activity, text: String, actionText: String, listener: View.OnClickListener) {
        val rootView = activity.findViewById<View>(android.R.id.content)
        if (rootView != null) {
            Snackbar.make(rootView, text, Snackbar.LENGTH_LONG)
                .setAction(actionText, listener)
                .show()
        }
    }
}
