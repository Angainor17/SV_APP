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

package org.geometerplus.android.fbreader.bookmark

import org.geometerplus.fbreader.book.HighlightingStyle
import org.geometerplus.zlibrary.ui.android.util.ZLAndroidColorUtil
import yuku.ambilwarna.widget.AmbilWarnaPrefWidgetView

internal object BookmarksUtil {
    fun setupColorView(colorView: AmbilWarnaPrefWidgetView, style: HighlightingStyle?) {
        var rgb: Int? = null
        if (style != null) {
            val color = style.backgroundColor
            if (color != null) {
                rgb = ZLAndroidColorUtil.rgb(color)
            }
        }

        if (rgb != null) {
            colorView.showCross(false)
            colorView.setBackgroundColor(rgb)
        } else {
            colorView.showCross(true)
            colorView.setBackgroundColor(0)
        }
    }
}
