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

package org.geometerplus.android.fbreader

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import org.geometerplus.android.fbreader.httpd.DataUtil
import org.geometerplus.android.util.UIMessageUtil
import org.geometerplus.fbreader.fbreader.FBReaderApp
import org.geometerplus.zlibrary.core.util.MimeType
import org.geometerplus.zlibrary.text.view.ZLTextVideoRegionSoul

internal class OpenVideoAction(baseActivity: FBReader, fbreader: FBReaderApp) : FBAndroidAction(baseActivity, fbreader) {

    override fun run(vararg params: Any?) {
        if (params.size != 1 || params[0] !is ZLTextVideoRegionSoul) {
            return
        }

        val element = (params[0] as ZLTextVideoRegionSoul).VideoElement
        var playerNotFound = false
        for (mimeType in MimeType.TYPES_VIDEO) {
            val mime = mimeType.toString()
            val path = element.Sources[mime] ?: continue
            val intent = Intent(Intent.ACTION_VIEW)
            val url = DataUtil.buildUrl(BaseActivity.dataConnection, mime, path)
            if (url == null) {
                UIMessageUtil.showErrorMessage(BaseActivity, "videoServiceNotWorking")
                return
            }
            intent.setDataAndType(Uri.parse(url), mime)
            try {
                BaseActivity.startActivity(intent)
                return
            } catch (e: ActivityNotFoundException) {
                playerNotFound = true
                continue
            }
        }
        if (playerNotFound) {
            UIMessageUtil.showErrorMessage(BaseActivity, "videoPlayerNotFound")
        }
    }
}
