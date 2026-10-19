package com.github.axet.bookreader.screens.viewmodel

import com.github.axet.bookreader.widgets.FBReaderView
import org.geometerplus.fbreader.fbreader.ActionCode
import timber.log.Timber

/**
 * Делегат для выделения текста в читалке.
 *
 * Отвечает за показ/скрытие панели выделения и действия с выделенным текстом
 * (копирование, шаринг, создание закладки, сообщение об ошибке/вопросе).
 *
 * Выделен из [ReaderViewModel] для уменьшения размера God-класса.
 */
class ReaderSelectionDelegate(
    private val getState: () -> ReaderState.Content?,
    private val updateState: (ReaderState.Content) -> Unit,
    private val getFBReaderView: () -> FBReaderView?,
) {
    private var lastShowTime: Long = 0L
    private var lastExplicitHideTime: Long = 0L

    companion object {
        private const val DEBOUNCE_MS = 500L
    }

    fun show(startY: Int, endY: Int) {
        Timber.d("showSelection called: startY=$startY, endY=$endY")
        val currentState = getState() ?: return
        if (currentState.showSelection &&
            currentState.selectionStartY == startY &&
            currentState.selectionEndY == endY
        ) {
            Timber.d("showSelection: already showing with same coordinates, skipping")
            return
        }
        val timeSinceExplicitHide = System.currentTimeMillis() - lastExplicitHideTime
        if (timeSinceExplicitHide < DEBOUNCE_MS) {
            Timber.d("showSelection: suppressed (explicitly hidden $timeSinceExplicitHide ms ago)")
            return
        }
        lastShowTime = System.currentTimeMillis()
        updateState(
            currentState.copy(
                showSelection = true,
                selectionStartY = startY,
                selectionEndY = endY
            )
        )
    }

    fun hide() {
        Timber.d("hideSelection called")
        val currentState = getState() ?: return
        val timeSinceShow = System.currentTimeMillis() - lastShowTime
        if (timeSinceShow < DEBOUNCE_MS) {
            Timber.d("hideSelection: debounced (shown $timeSinceShow ms ago)")
            return
        }
        updateState(currentState.copy(showSelection = false))
    }

    fun copy() {
        getFBReaderView()?.app?.runAction(ActionCode.SELECTION_COPY_TO_CLIPBOARD)
        hidePanel()
    }

    fun share() {
        getFBReaderView()?.app?.runAction(ActionCode.SELECTION_SHARE)
        hidePanel()
    }

    fun bookmark() {
        getFBReaderView()?.app?.runAction(ActionCode.SELECTION_BOOKMARK)
        hidePanel()
    }

    fun question() {
        getFBReaderView()?.app?.runAction(ActionCode.ASK_QUESTION)
        hidePanel()
    }

    fun alert() {
        getFBReaderView()?.app?.runAction(ActionCode.TEL_ABOUT_MISSPELL)
        hidePanel()
    }

    /**
     * Скрыть панель выделения (без debounce) и очистить выделение в FBReaderView.
     */
    fun hidePanel() {
        val currentState = getState() ?: return
        lastExplicitHideTime = System.currentTimeMillis()
        updateState(currentState.copy(showSelection = false))
        getFBReaderView()?.app?.runAction(ActionCode.SELECTION_CLEAR)
    }
}
