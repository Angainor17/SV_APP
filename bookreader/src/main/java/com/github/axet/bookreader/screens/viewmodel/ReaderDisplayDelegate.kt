package com.github.axet.bookreader.screens.viewmodel

import android.content.SharedPreferences
import androidx.core.content.edit
import com.github.axet.bookreader.app.ReaderPreferences
import com.github.axet.bookreader.widgets.FBReaderView
import timber.log.Timber

/**
 * Делегат для управления режимами отображения читалки.
 *
 * Отвечает за переключение fullscreen, view mode (paging/continuous),
 * reflow, и показ подсказок зон касания.
 *
 * Выделен из [ReaderViewModel] для уменьшения размера God-класса.
 */
class ReaderDisplayDelegate(
    private val getState: () -> ReaderState.Content?,
    private val updateState: (ReaderState.Content) -> Unit,
    private val getFBReaderView: () -> FBReaderView?,
    private val sharedPreferences: SharedPreferences,
    private val onHideSelectionPanel: () -> Unit,
) {
    fun toggleFullscreen() {
        val currentState = getState() ?: return
        updateState(currentState.copy(isFullscreen = !currentState.isFullscreen))
    }

    fun setFullscreen(isFullscreen: Boolean) {
        val currentState = getState() ?: return
        Timber.tag("voronin").d("=== ViewModel: setFullscreen($isFullscreen) ===")
        if (currentState.isFullscreen != isFullscreen) {
            updateState(currentState.copy(isFullscreen = isFullscreen))
        }
    }

    fun toggleViewMode() {
        val currentState = getState() ?: return
        val newMode = if (currentState.viewMode == ViewMode.PAGING) {
            ViewMode.CONTINUOUS
        } else {
            ViewMode.PAGING
        }
        onHideSelectionPanel()
        sharedPreferences.edit { putString(ReaderPreferences.PREFERENCE_VIEW_MODE, newMode.toString()) }
        getFBReaderView()?.setWidget(
            if (newMode == ViewMode.CONTINUOUS) FBReaderView.Widgets.CONTINUOUS
            else FBReaderView.Widgets.PAGING
        )
        updateState(currentState.copy(viewMode = newMode))
    }

    fun toggleReflow() {
        val currentState = getState() ?: return
        onHideSelectionPanel()
        val fbv = getFBReaderView() ?: return
        fbv.setReflow(!fbv.isReflow)
        val newReflow = !currentState.isReflow
        val canChange = fbv.canChangeFont()
        updateState(currentState.copy(isReflow = newReflow, canChangeFont = canChange))
    }

    fun markControlsHintShown() {
        val currentState = getState() ?: return
        if (!currentState.hasShownControlsHint) {
            updateState(currentState.copy(hasShownControlsHint = true))
        }
    }

    fun getViewModeFromPrefs(): ViewMode {
        val mode = sharedPreferences.getString(ReaderPreferences.PREFERENCE_VIEW_MODE, "") ?: ""
        return if (mode == FBReaderView.Widgets.CONTINUOUS.toString()) {
            ViewMode.CONTINUOUS
        } else {
            ViewMode.PAGING
        }
    }
}
