package org.geometerplus.fbreader.fbreader

interface ActionCode {
    companion object {
        const val SHOW_LIBRARY = "library"
        const val SHOW_PREFERENCES = "preferences"
        const val SHOW_BOOK_INFO = "bookInfo"
        const val SHOW_TOC = "toc"
        const val SHOW_BOOKMARKS = "bookmarks"
        const val SHOW_NETWORK_LIBRARY = "networkLibrary"

        const val SWITCH_TO_NIGHT_PROFILE = "night"
        const val SWITCH_TO_DAY_PROFILE = "day"

        const val SHARE_BOOK = "shareBook"

        const val SEARCH = "search"
        const val FIND_PREVIOUS = "findPrevious"
        const val FIND_NEXT = "findNext"
        const val CLEAR_FIND_RESULTS = "clearFindResults"

        const val SET_TEXT_VIEW_MODE_VISIT_HYPERLINKS = "hyperlinksOnlyMode"
        const val SET_TEXT_VIEW_MODE_VISIT_ALL_WORDS = "dictionaryMode"

        const val TURN_PAGE_BACK = "previousPage"
        const val TURN_PAGE_FORWARD = "nextPage"

        const val MOVE_CURSOR_UP = "moveCursorUp"
        const val MOVE_CURSOR_DOWN = "moveCursorDown"
        const val MOVE_CURSOR_LEFT = "moveCursorLeft"
        const val MOVE_CURSOR_RIGHT = "moveCursorRight"

        const val VOLUME_KEY_SCROLL_FORWARD = "volumeKeyScrollForward"
        const val VOLUME_KEY_SCROLL_BACK = "volumeKeyScrollBackward"
        const val SHOW_MENU = "menu"
        const val SHOW_NAVIGATION = "navigate"

        const val GO_BACK = "goBack"
        const val EXIT = "exit"
        const val SHOW_CANCEL_MENU = "cancelMenu"

        const val SET_SCREEN_ORIENTATION_SYSTEM = "screenOrientationSystem"
        const val SET_SCREEN_ORIENTATION_SENSOR = "screenOrientationSensor"
        const val SET_SCREEN_ORIENTATION_PORTRAIT = "screenOrientationPortrait"
        const val SET_SCREEN_ORIENTATION_LANDSCAPE = "screenOrientationLandscape"
        const val SET_SCREEN_ORIENTATION_REVERSE_PORTRAIT = "screenOrientationReversePortrait"
        const val SET_SCREEN_ORIENTATION_REVERSE_LANDSCAPE = "screenOrientationReverseLandscape"

        const val INCREASE_FONT = "increaseFont"
        const val DECREASE_FONT = "decreaseFont"

        const val DISPLAY_BOOK_POPUP = "displayBookPopup"
        const val PROCESS_HYPERLINK = "processHyperlink"

        const val SELECTION_SHOW_PANEL = "selectionShowPanel"
        const val SELECTION_HIDE_PANEL = "selectionHidePanel"
        const val SELECTION_CLEAR = "selectionClear"
        const val ASK_QUESTION = "ask_question"
        const val TEL_ABOUT_MISSPELL = "tel_about_misspell"
        const val SELECTION_COPY_TO_CLIPBOARD = "selectionCopyToClipboard"
        const val SELECTION_SHARE = "selectionShare"
        const val SELECTION_TRANSLATE = "selectionTranslate"
        const val SELECTION_BOOKMARK = "selectionBookmark"

        const val OPEN_VIDEO = "video"

        const val HIDE_TOAST = "hideToast"
        const val OPEN_START_SCREEN = "openStartScreen"
        const val OPEN_WEB_HELP = "help"
        const val INSTALL_PLUGINS = "plugins"
    }
}
