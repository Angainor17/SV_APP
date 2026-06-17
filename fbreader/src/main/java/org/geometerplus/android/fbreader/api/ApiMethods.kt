/*
 * This code is in the public domain.
 */

package org.geometerplus.android.fbreader.api

internal interface ApiMethods {
    // fbreader information
    companion object {
        const val GET_FBREADER_VERSION = 1

        // library information

        // network library information

        // bookmarks information

        // preferences
        const val LIST_OPTION_GROUPS = 401
        const val LIST_OPTION_NAMES = 402
        const val GET_OPTION_VALUE = 403
        const val SET_OPTION_VALUE = 404

        // book information
        const val GET_BOOK_LANGUAGE = 501
        const val GET_BOOK_TITLE = 502
        const val LIST_BOOK_AUTHORS = 503
        const val LIST_BOOK_TAGS = 504
        const val GET_BOOK_FILE_PATH = 505
        const val GET_BOOK_HASH = 506
        const val GET_BOOK_UNIQUE_ID = 507
        const val GET_BOOK_LAST_TURNING_TIME = 508
        // book information: read progress
        const val GET_BOOK_PROGRESS = 509

        // text information
        const val GET_PARAGRAPHS_NUMBER = 601
        const val GET_PARAGRAPH_ELEMENTS_COUNT = 602
        const val GET_PARAGRAPH_TEXT = 603
        const val GET_PARAGRAPH_WORDS = 604
        const val GET_PARAGRAPH_WORD_INDICES = 605

        // page information
        const val GET_PAGE_START = 701
        const val GET_PAGE_END = 702
        const val IS_PAGE_END_OF_TEXT = 703
        const val IS_PAGE_END_OF_SECTION = 704

        // view management
        const val SET_PAGE_START = 801
        const val HIGHLIGHT_AREA = 802
        const val CLEAR_HIGHLIGHTING = 803
        const val GET_BOTTOM_MARGIN = 804
        const val SET_BOTTOM_MARGIN = 805
        const val GET_TOP_MARGIN = 806
        const val SET_TOP_MARGIN = 807
        const val GET_LEFT_MARGIN = 808
        const val SET_LEFT_MARGIN = 809
        const val GET_RIGHT_MARGIN = 810
        const val SET_RIGHT_MARGIN = 811

        // action control
        const val LIST_ACTIONS = 901
        const val LIST_ACTION_NAMES = 902

        const val GET_KEY_ACTION = 911
        const val SET_KEY_ACTION = 912

        const val LIST_ZONEMAPS = 921
        const val GET_ZONEMAP = 922
        const val SET_ZONEMAP = 923
        const val GET_ZONEMAP_HEIGHT = 924
        const val GET_ZONEMAP_WIDTH = 925
        const val CREATE_ZONEMAP = 926
        const val IS_ZONEMAP_CUSTOM = 927
        const val DELETE_ZONEMAP = 928

        const val GET_TAPZONE_ACTION = 931
        const val SET_TAPZONE_ACTION = 932
        const val GET_TAP_ACTION_BY_COORDINATES = 933

        // for format plugins
        const val GET_MAIN_MENU_CONTENT = 1001
        const val GET_RESOURCE_STRING = 1002
        const val GET_BITMAP = 1003
    }
}
