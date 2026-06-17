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
import android.content.Intent

import org.geometerplus.zlibrary.ui.android.library.ZLAndroidApplication

internal class ColorDict(id: String, title: String) : DictionaryUtil.PackageInfo(id, title) {

    override fun open(text: String, outliner: Runnable?, fbreader: Activity, frameMetrics: DictionaryUtil.PopupFrameMetric) {
        val intent = getActionIntent(text)
        intent.putExtra(ColorDict3.HEIGHT, frameMetrics.height)
        intent.putExtra(ColorDict3.GRAVITY, frameMetrics.gravity)
        intent.putExtra(ColorDict3.FULLSCREEN, !(fbreader.application as ZLAndroidApplication).library().ShowStatusBarOption.value)
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
        InternalUtil.startDictionaryActivity(fbreader, intent, this)
    }

    private object ColorDict3 {
        const val ACTION = "colordict.intent.action.SEARCH"
        const val QUERY = "EXTRA_QUERY"
        const val HEIGHT = "EXTRA_HEIGHT"
        const val WIDTH = "EXTRA_WIDTH"
        const val GRAVITY = "EXTRA_GRAVITY"
        const val MARGIN_LEFT = "EXTRA_MARGIN_LEFT"
        const val MARGIN_TOP = "EXTRA_MARGIN_TOP"
        const val MARGIN_BOTTOM = "EXTRA_MARGIN_BOTTOM"
        const val MARGIN_RIGHT = "EXTRA_MARGIN_RIGHT"
        const val FULLSCREEN = "EXTRA_FULLSCREEN"
    }
}
