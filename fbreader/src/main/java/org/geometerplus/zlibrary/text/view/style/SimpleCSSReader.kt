package org.geometerplus.zlibrary.text.view.style

import org.geometerplus.zlibrary.core.filesystem.ZLFile
import org.geometerplus.zlibrary.core.util.MiscUtil
import java.io.BufferedReader
import java.io.InputStreamReader

internal class SimpleCSSReader {
    private enum class State {
        EXPECT_SELECTOR,
        EXPECT_OPEN_BRACKET,
        EXPECT_NAME,
        EXPECT_VALUE,
        READ_COMMENT
    }

    private var state: State = State.EXPECT_SELECTOR
    private var savedState: State = State.EXPECT_SELECTOR
    private var descriptionMap: LinkedHashMap<Int, ZLTextNGStyleDescription>? = null
    private var currentMap: MutableMap<String, String>? = null
    private var selector: String? = null
    private var name: String? = null

    fun read(file: ZLFile): Map<Int, ZLTextNGStyleDescription> {
        descriptionMap = LinkedHashMap()
        state = State.EXPECT_SELECTOR

        try {
            file.getInputStream().use { stream ->
                val reader = BufferedReader(InputStreamReader(stream))
                var line: String? = reader.readLine()
                while (line != null) {
                    for (token in MiscUtil.smartSplit(line)) {
                        processToken(token)
                    }
                    line = reader.readLine()
                }
            }
        } catch (e: Exception) {
            // ignore
        }

        return descriptionMap!!
    }

    private fun processToken(token: String) {
        if (state != State.READ_COMMENT && token.startsWith("/*")) {
            savedState = state
            state = State.READ_COMMENT
            return
        }

        when (state) {
            State.READ_COMMENT -> {
                if (token.endsWith("*/")) {
                    state = savedState
                }
            }
            State.EXPECT_SELECTOR -> {
                selector = token
                state = State.EXPECT_OPEN_BRACKET
            }
            State.EXPECT_OPEN_BRACKET -> {
                if (token == "{") {
                    currentMap = HashMap()
                    state = State.EXPECT_NAME
                }
            }
            State.EXPECT_NAME -> {
                if (token == "}") {
                    selector?.let { sel ->
                        try {
                            currentMap?.get("fbreader-id")?.toInt()?.let { id ->
                                descriptionMap?.put(id, ZLTextNGStyleDescription(sel, currentMap!!))
                            }
                        } catch (e: Exception) {
                            // ignore
                        }
                    }
                    state = State.EXPECT_SELECTOR
                } else {
                    name = token
                    state = State.EXPECT_VALUE
                }
            }
            State.EXPECT_VALUE -> {
                if (currentMap != null && name != null) {
                    currentMap!![name!!] = token
                }
                state = State.EXPECT_NAME
            }
            else -> {}
        }
    }
}
