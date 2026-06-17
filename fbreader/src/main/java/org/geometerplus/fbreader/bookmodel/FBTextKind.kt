package org.geometerplus.fbreader.bookmodel

interface FBTextKind {
    companion object {
        const val REGULAR: Byte = 0
        const val TITLE: Byte = 1
        const val SECTION_TITLE: Byte = 2
        const val POEM_TITLE: Byte = 3
        const val SUBTITLE: Byte = 4
        const val ANNOTATION: Byte = 5
        const val EPIGRAPH: Byte = 6
        const val STANZA: Byte = 7
        const val VERSE: Byte = 8
        const val PREFORMATTED: Byte = 9
        const val IMAGE: Byte = 10
        //const val END_OF_SECTION: Byte = 11
        const val CITE: Byte = 12
        const val AUTHOR: Byte = 13
        const val DATE: Byte = 14
        const val INTERNAL_HYPERLINK: Byte = 15
        const val FOOTNOTE: Byte = 16
        const val EMPHASIS: Byte = 17
        const val STRONG: Byte = 18
        const val SUB: Byte = 19
        const val SUP: Byte = 20
        const val CODE: Byte = 21
        const val STRIKETHROUGH: Byte = 22
        //const val CONTENTS_TABLE_ENTRY: Byte = 23
        //const val LIBRARY_AUTHOR_ENTRY: Byte = 24
        //const val LIBRARY_BOOK_ENTRY: Byte = 25
        //const val LIBRARY_ENTRY: Byte = 25
        //const val RECENT_BOOK_LIST: Byte = 26
        const val ITALIC: Byte = 27
        const val BOLD: Byte = 28
        const val DEFINITION: Byte = 29
        const val DEFINITION_DESCRIPTION: Byte = 30
        const val H1: Byte = 31
        const val H2: Byte = 32
        const val H3: Byte = 33
        const val H4: Byte = 34
        const val H5: Byte = 35
        const val H6: Byte = 36
        const val EXTERNAL_HYPERLINK: Byte = 37
        //const val BOOK_HYPERLINK: Byte = 38

        const val XHTML_TAG_P: Byte = 51
    }
}
