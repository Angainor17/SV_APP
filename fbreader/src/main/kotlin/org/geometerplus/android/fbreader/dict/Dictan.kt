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

import org.geometerplus.zlibrary.core.resources.ZLResource

class Dictan(id: String?, title: String?) : DictionaryUtil.PackageInfo(id, title) {
    override fun open(text: String, outliner: Runnable, fbreader: Activity, frameMetrics: DictionaryUtil.PopupFrameMetric) {
        val intent = getActionIntent(text)
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
        intent.putExtra("article.mode", 20)
        intent.putExtra("article.text.size.max", MAX_LENGTH_FOR_TOAST)
        try {
            fbreader.startActivityForResult(intent, REQUEST_DICTIONARY)
            fbreader.overridePendingTransition(0, 0)
            if (outliner != null) {
                outliner.run()
            }
        } catch (e: ActivityNotFoundException) {
            InternalUtil.installDictionaryIfNotInstalled(fbreader, this)
        }
    }

    override fun onActivityResult(fbreader: Activity, resultCode: Int, data: Intent) {
        if (data == null) {
            return
        }

        val errorCode = data.getIntExtra("error.code", -1)
        if (resultCode != Activity.RESULT_OK || errorCode != -1) {
            showError(fbreader, errorCode, data)
            return
        }

        var text = data.getStringExtra("article.text")
        if (text == null) {
            showError(fbreader, -1, data)
            return
        }

        // a hack for obsolete (before 5.0 beta) dictan versions
        val index = text.indexOf("\u0000")
        if (index >= 0) {
            text = text.substring(0, index)
        }

        val hasExtraData: Boolean
        if (text.length == MAX_LENGTH_FOR_TOAST) {
            text = trimArticle(text)
            hasExtraData = true
        } else {
            hasExtraData = data.getBooleanExtra("article.resources.contains", false)
        }

        if (hasExtraData) {
            InternalUtil.showSnackbarWithAction(
                fbreader,
                text,
                ZLResource.resource("toast").getResource("more").getValue()
            ) { _ ->
                val word = data.getStringExtra("article.word")
                val intent = getActionIntent(word)
                try {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                    fbreader.startActivity(intent)
                    fbreader.overridePendingTransition(0, 0)
                } catch (e: ActivityNotFoundException) {
                    // ignore
                }
            }
        } else {
            InternalUtil.showSnackbar(
                fbreader,
                text,
                DictionaryUtil.TranslationToastDurationOption.getValue().Value
            )
        }
    }

    private companion object {
        const val MAX_LENGTH_FOR_TOAST = 180
        const val REQUEST_DICTIONARY = 100

        private fun trimArticle(text: String): String {
            val len = text.length
            val eolIndex = text.lastIndexOf("\n")
            val spaceIndex = text.lastIndexOf(" ")
            return if (spaceIndex < eolIndex || eolIndex >= len * 2 / 3) {
                text.substring(0, eolIndex)
            } else {
                text.substring(0, spaceIndex)
            }
        }

        private fun showError(fbreader: Activity, code: Int, data: Intent) {
            val resource = ZLResource.resource("dictanErrors")
            val message: String = when (code) {
                100 -> {
                    val word = data.getStringExtra("article.word")
                    resource.getResource("noArticle").getValue().replace("%s", word!!)
                }
                130 -> resource.getResource("cannotOpenDictionary").getValue()
                131 -> resource.getResource("noDictionarySelected").getValue()
                else -> data.getStringExtra("error.message")
                    ?: resource.getResource("unknown").getValue()
            }

            InternalUtil.showSnackbar(
                fbreader,
                "Dictan: $message",
                DictionaryUtil.ErrorToastDurationOption.getValue().Value
            )
        }
    }
}
