package org.geometerplus.fbreader.bookmodel

interface FBHyperlinkType {
    companion object {
        const val NONE: Byte = 0
        const val INTERNAL: Byte = 1
        const val FOOTNOTE: Byte = 2
        const val EXTERNAL: Byte = 3
    }
}
