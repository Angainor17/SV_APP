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
import org.geometerplus.zlibrary.core.options.ZLStringListOption
import org.geometerplus.zlibrary.core.resources.ZLResource
import org.geometerplus.zlibrary.core.util.MiscUtil

internal class FileChooserMultiPreference(
    context: Context,
    rootResource: ZLResource,
    resourceKey: String,
    private val myOption: ZLStringListOption,
    requestCode: Int,
    onValueSetAction: Runnable?
) : FileChooserPreference(context, rootResource, resourceKey, false, requestCode, onValueSetAction) {

    init {
        summary = getStringValue()
    }

    override fun onClick() {
        FileChooserUtil.runFolderListDialog(
            context as Activity,
            myRequestCode,
            myResource.value,
            myResource.getResource("chooserTitle").value,
            myOption.value,
            myChooseWritableDirectoriesOnly
        )
    }

    override fun getStringValue(): String = MiscUtil.join(myOption.value, ", ")

    internal override fun setValueFromIntent(data: Intent) {
        val value = FileChooserUtil.pathListFromData(data)
        if (value.isNullOrEmpty()) {
            return
        }

        myOption.value = value
        summary = getStringValue()

        myOnValueSetAction?.run()
    }
}
