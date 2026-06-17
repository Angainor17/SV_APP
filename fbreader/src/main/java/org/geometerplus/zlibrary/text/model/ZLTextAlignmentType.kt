package org.geometerplus.zlibrary.text.model

interface ZLTextAlignmentType {
    companion object {
        const val ALIGN_UNDEFINED: Byte = 0
        const val ALIGN_LEFT: Byte = 1
        const val ALIGN_RIGHT: Byte = 2
        const val ALIGN_CENTER: Byte = 3
        const val ALIGN_JUSTIFY: Byte = 4
        const val ALIGN_LINESTART: Byte = 5 // left for LTR languages and right for RTL
    }
}
