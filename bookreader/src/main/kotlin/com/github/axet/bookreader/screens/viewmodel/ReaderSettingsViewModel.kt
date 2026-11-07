package com.github.axet.bookreader.screens.viewmodel

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.preference.PreferenceManager
import com.github.axet.bookreader.app.ReaderPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * ViewModel экрана настроек читалки.
 *
 * Хранилище остаётся [SharedPreferences] — его напрямую читает нативный код FBReader
 * (`FBReaderView`, `Reflow`, `ConfigShadow` и др.), поэтому миграция на DataStore здесь
 * невозможна без переписывания всего слоя конфигурации. Долг убирается иначе:
 * подписка `OnSharedPreferenceChangeListener` вынесена из Composable сюда, а наружу
 * отдаются [StateFlow], которые экран собирает через `collectAsStateWithLifecycle()`.
 */
@HiltViewModel
class ReaderSettingsViewModel @Inject constructor(
    @param:ApplicationContext context: Context,
) : ViewModel() {

    private val shared: SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(context)

    private val _viewMode = MutableStateFlow(readViewMode())
    val viewMode: StateFlow<String> = _viewMode.asStateFlow()

    private val _volumeKeys = MutableStateFlow(
        shared.getBoolean(ReaderPreferences.PREFERENCE_VOLUME_KEYS, false)
    )
    val volumeKeys: StateFlow<Boolean> = _volumeKeys.asStateFlow()

    private val _twoColumnView = MutableStateFlow(readTwoColumnView())
    val twoColumnView: StateFlow<String> = _twoColumnView.asStateFlow()

    private val _toolbarVisibility = MutableStateFlow(readToolbarVisibility())
    val toolbarVisibility: StateFlow<ToolbarVisibility> = _toolbarVisibility.asStateFlow()

    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
        when (key) {
            ReaderPreferences.PREFERENCE_VIEW_MODE ->
                _viewMode.value = prefs.getString(key, "PAGING") ?: "PAGING"

            ReaderPreferences.PREFERENCE_VOLUME_KEYS ->
                _volumeKeys.value = prefs.getBoolean(key, false)

            ReaderPreferences.PREFERENCE_TWO_COLUMN_VIEW ->
                _twoColumnView.value = normalizeTwoColumn(prefs.getString(key, "auto"))

            ReaderPreferences.PREFERENCE_TOOLBAR_SEARCH,
            ReaderPreferences.PREFERENCE_TOOLBAR_TOC,
            ReaderPreferences.PREFERENCE_TOOLBAR_BOOKMARKS,
            ReaderPreferences.PREFERENCE_TOOLBAR_FONT,
            ReaderPreferences.PREFERENCE_TOOLBAR_PAGE_INDICATOR ->
                _toolbarVisibility.value = readToolbarVisibility()
        }
    }

    init {
        shared.registerOnSharedPreferenceChangeListener(listener)
    }

    override fun onCleared() {
        shared.unregisterOnSharedPreferenceChangeListener(listener)
        super.onCleared()
    }

    /** Переключить режим просмотра: PAGING ⇄ CONTINUOUS. */
    fun toggleViewMode() {
        val newMode = if (viewMode.value == "PAGING") "CONTINUOUS" else "PAGING"
        shared.edit { putString(ReaderPreferences.PREFERENCE_VIEW_MODE, newMode) }
    }

    /** Включить/выключить перелистывание кнопками громкости. */
    fun setVolumeKeys(enabled: Boolean) {
        shared.edit { putBoolean(ReaderPreferences.PREFERENCE_VOLUME_KEYS, enabled) }
    }

    /** Циклически переключить двухстраничный режим: auto → true → false → auto. */
    fun cycleTwoColumnView() {
        val newValue = when (twoColumnView.value) {
            "auto" -> "true"
            "true" -> "false"
            else -> "auto"
        }
        shared.edit { putString(ReaderPreferences.PREFERENCE_TWO_COLUMN_VIEW, newValue) }
    }

    /** Включить/выключить видимость кнопки «Поиск» в тулбаре читалки. */
    fun setToolbarSearch(enabled: Boolean) {
        shared.edit { putBoolean(ReaderPreferences.PREFERENCE_TOOLBAR_SEARCH, enabled) }
    }

    /** Включить/выключить видимость кнопки «Оглавление» в тулбаре читалки. */
    fun setToolbarToc(enabled: Boolean) {
        shared.edit { putBoolean(ReaderPreferences.PREFERENCE_TOOLBAR_TOC, enabled) }
    }

    /** Включить/выключить видимость кнопки «Закладки» в тулбаре читалки. */
    fun setToolbarBookmarks(enabled: Boolean) {
        shared.edit { putBoolean(ReaderPreferences.PREFERENCE_TOOLBAR_BOOKMARKS, enabled) }
    }

    /** Включить/выключить видимость кнопки «Шрифт» в тулбаре читалки. */
    fun setToolbarFont(enabled: Boolean) {
        shared.edit { putBoolean(ReaderPreferences.PREFERENCE_TOOLBAR_FONT, enabled) }
    }

    /** Включить/выключить видимость индикатора страницы в тулбаре читалки. */
    fun setToolbarPageIndicator(enabled: Boolean) {
        shared.edit { putBoolean(ReaderPreferences.PREFERENCE_TOOLBAR_PAGE_INDICATOR, enabled) }
    }

    private fun readViewMode(): String =
        shared.getString(ReaderPreferences.PREFERENCE_VIEW_MODE, "PAGING") ?: "PAGING"

    private fun readTwoColumnView(): String =
        normalizeTwoColumn(shared.getString(ReaderPreferences.PREFERENCE_TWO_COLUMN_VIEW, "auto"))

    private fun readToolbarVisibility(): ToolbarVisibility = ToolbarVisibility(
        search = shared.getBoolean(ReaderPreferences.PREFERENCE_TOOLBAR_SEARCH, true),
        toc = shared.getBoolean(ReaderPreferences.PREFERENCE_TOOLBAR_TOC, true),
        bookmarks = shared.getBoolean(ReaderPreferences.PREFERENCE_TOOLBAR_BOOKMARKS, true),
        font = shared.getBoolean(ReaderPreferences.PREFERENCE_TOOLBAR_FONT, true),
        pageIndicator = shared.getBoolean(ReaderPreferences.PREFERENCE_TOOLBAR_PAGE_INDICATOR, true),
    )

    private fun normalizeTwoColumn(raw: String?): String = when (raw) {
        "true" -> "true"
        "false" -> "false"
        else -> "auto"
    }
}
