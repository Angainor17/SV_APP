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

package org.geometerplus.android.fbreader.preferences.fileChooser

import android.app.Activity
import android.content.Context
import android.content.Intent

import org.geometerplus.android.util.FileChooserUtil
import org.geometerplus.zlibrary.core.options.ZLStringOption
import org.geometerplus.zlibrary.core.resources.ZLResource
import org.geometerplus.zlibrary.core.util.MiscUtil

internal class FileChooserSinglePreference(
    context: Context,
    rootResource: ZLResource,
    resourceKey: String,
    private val myOption: ZLStringOption,
    requestCode: Int,
    onValueSetAction: Runnable?
) : FileChooserPreference(context, rootResource, resourceKey, true, requestCode, onValueSetAction) {

    init {
        summary = getStringValue()
    }

    override fun onClick() {
        FileChooserUtil.runDirectoryChooser(
            context as Activity,
            myRequestCode,
            myResource.getResource("chooserTitle").value,
            getStringValue(),
            myChooseWritableDirectoriesOnly
        )
    }

    override fun getStringValue(): String = myOption.value

    internal override fun setValueFromIntent(data: Intent) {
        val value = FileChooserUtil.folderPathFromData(data)
        if (MiscUtil.isEmptyString(value)) {
            return
        }

        val currentValue = myOption.value
        if (currentValue != value) {
            myOption.value = value ?: ""
            summary = value
        }

        myOnValueSetAction?.run()
    }
}
