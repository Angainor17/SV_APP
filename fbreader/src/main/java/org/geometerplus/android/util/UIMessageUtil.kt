/*
 * Copyright (C) 2007-2015 FBReader.ORG Limited <contact@fbreader.org>
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
import android.widget.Toast
import org.geometerplus.zlibrary.core.resources.ZLResource

object UIMessageUtil {
    @JvmStatic
    fun showMessageText(activity: Activity, text: String) {
        activity.runOnUiThread {
            Toast.makeText(activity, text, Toast.LENGTH_LONG).show()
        }
    }

    @JvmStatic
    fun showErrorMessage(activity: Activity, resourceKey: String) {
        showMessageText(
            activity,
            ZLResource.resource("errorMessage").getResource(resourceKey).getValue()
        )
    }

    @JvmStatic
    fun showErrorMessage(activity: Activity, resourceKey: String, parameter: String) {
        showMessageText(
            activity,
            ZLResource.resource("errorMessage").getResource(resourceKey).getValue().replace("%s", parameter)
        )
    }
}
