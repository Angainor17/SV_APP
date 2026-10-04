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

package org.geometerplus.zlibrary.text.view.style

import org.geometerplus.zlibrary.core.filesystem.ZLFile
import org.geometerplus.zlibrary.core.util.MiscUtil
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.util.HashMap
import java.util.LinkedHashMap

internal class SimpleCSSReader {
    private var myState: State = State.EXPECT_SELECTOR
    private var mySavedState: State = State.EXPECT_SELECTOR
    private var myDescriptionMap = LinkedHashMap<Int, ZLTextNGStyleDescription>()
    private var myCurrentMap: MutableMap<String, String>? = null
    private var mySelector: String? = null
    private var myName: String? = null

    fun read(file: ZLFile): Map<Int, ZLTextNGStyleDescription> {
        myDescriptionMap = LinkedHashMap()
        myState = State.EXPECT_SELECTOR

        var stream: InputStream? = null
        try {
            stream = file.getInputStream()
            val reader = BufferedReader(InputStreamReader(stream!!))
            var line: String? = reader.readLine()
            while (line != null) {
                for (token in MiscUtil.smartSplit(line)) {
                    processToken(token)
                }
                line = reader.readLine()
            }
        } catch (e: IOException) {
        } finally {
            if (stream != null) {
                try {
                    stream!!.close()
                } catch (e: IOException) {
                }
            }
        }

        return myDescriptionMap
    }

    private fun processToken(token: String) {
        if (myState != State.READ_COMMENT && token.startsWith("/*")) {
            mySavedState = myState
            myState = State.READ_COMMENT
            return
        }

        when (myState) {
            State.READ_COMMENT ->
                if (token.endsWith("*/")) {
                    myState = mySavedState
                }
            State.EXPECT_SELECTOR -> {
                mySelector = token
                myState = State.EXPECT_OPEN_BRACKET
            }
            State.EXPECT_OPEN_BRACKET ->
                if ("{" == token) {
                    myCurrentMap = HashMap()
                    myState = State.EXPECT_NAME
                }
            State.EXPECT_NAME -> {
                if ("}" == token) {
                    val selector = mySelector
                    if (selector != null) {
                        try {
                            myDescriptionMap[myCurrentMap!!.get("fbreader-id")!!.toInt()] =
                                ZLTextNGStyleDescription(selector, myCurrentMap!!)
                        } catch (e: Exception) {
                            // ignore
                        }
                    }
                    myState = State.EXPECT_SELECTOR
                } else {
                    myName = token
                    myState = State.EXPECT_VALUE
                }
            }
            State.EXPECT_VALUE -> {
                if (myCurrentMap != null && myName != null) {
                    myCurrentMap!![myName!!] = token
                }
                myState = State.EXPECT_NAME
            }
        }
    }

    private enum class State {
        EXPECT_SELECTOR,
        EXPECT_OPEN_BRACKET,
        EXPECT_NAME,
        EXPECT_VALUE,
        READ_COMMENT,
    }
}
