package org.geometerplus.zlibrary.text.view

abstract class ZLTextElement {
    companion object {
        @JvmField
        val HSpace: ZLTextElement = object : ZLTextElement() {}

        @JvmField
        val NBSpace: ZLTextElement = object : ZLTextElement() {}

        @JvmField
        val AfterParagraph: ZLTextElement = object : ZLTextElement() {}

        @JvmField
        val Indent: ZLTextElement = object : ZLTextElement() {}

        @JvmField
        val StyleClose: ZLTextElement = object : ZLTextElement() {}
    }
}
