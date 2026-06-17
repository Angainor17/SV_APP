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

import com.github.johnpersano.supertoasts.SuperActivityToast
import com.github.johnpersano.supertoasts.SuperToast
import com.github.johnpersano.supertoasts.util.OnDismissWrapper

import org.geometerplus.android.fbreader.FBReaderMainActivity
import org.geometerplus.android.util.PackageUtil

internal object InternalUtil {
    fun installDictionaryIfNotInstalled(activity: Activity, info: DictionaryUtil.PackageInfo) {
        if (PackageUtil.canBeStarted(activity, info.getActionIntent("test"), false)) {
            return
        }

        val intent = Intent(activity, DictionaryNotInstalledActivity::class.java)
        intent.putExtra(DictionaryNotInstalledActivity.DICTIONARY_NAME_KEY, info.title)
        intent.putExtra(DictionaryNotInstalledActivity.PACKAGE_NAME_KEY, info["package"])
        activity.startActivity(intent)
    }

    fun startDictionaryActivity(fbreader: Activity, intent: Intent, info: DictionaryUtil.PackageInfo) {
        try {
            fbreader.startActivity(intent)
            fbreader.overridePendingTransition(0, 0)
        } catch (e: ActivityNotFoundException) {
            installDictionaryIfNotInstalled(fbreader, info)
        }
    }

    fun showToast(toast: SuperActivityToast?, fbreader: FBReaderMainActivity) {
        if (toast == null) {
            fbreader.hideDictionarySelection()
            return
        }

        toast.setOnDismissWrapper(OnDismissWrapper("dict", object : SuperToast.OnDismissListener {
            override fun onDismiss(view: View) {
                fbreader.hideDictionarySelection()
            }
        }))
        fbreader.showToast(toast)
    }
}
