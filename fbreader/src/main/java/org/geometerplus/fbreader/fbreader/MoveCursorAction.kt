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

package org.geometerplus.fbreader.fbreader

import org.geometerplus.zlibrary.core.view.ZLViewEnums
import org.geometerplus.zlibrary.text.view.ZLTextRegion
import org.geometerplus.zlibrary.text.view.ZLTextWordRegionSoul
import org.geometerplus.zlibrary.text.view.ZLTextView

internal class MoveCursorAction(
    fbreader: FBReaderApp,
    private val myDirection: ZLViewEnums.Direction,
) : FBAction(fbreader) {

    override fun run(vararg params: Any?) {
        val fbView = Reader.getTextView()
        val filter =
            if (fbView.getOutlinedRegion()?.soul is ZLTextWordRegionSoul ||
                Reader.MiscOptions.NavigateAllWords.getValue()
            ) {
                ZLTextRegion.AnyRegionFilter
            } else {
                ZLTextRegion.ImageOrHyperlinkFilter
            }

        val region = fbView.nextRegion(myDirection, filter)
        if (region != null) {
            fbView.outlineRegion(region)
        } else {
            when (myDirection) {
                ZLViewEnums.Direction.down ->
                    fbView.turnPage(true, ZLTextView.ScrollingMode.SCROLL_LINES, 1)

                ZLViewEnums.Direction.up ->
                    fbView.turnPage(false, ZLTextView.ScrollingMode.SCROLL_LINES, 1)

                else -> Unit
            }
        }

        Reader.getViewWidget().reset()
        Reader.getViewWidget().repaint()
    }
}
