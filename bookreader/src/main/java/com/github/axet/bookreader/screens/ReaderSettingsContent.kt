package com.github.axet.bookreader.screens

import android.content.SharedPreferences
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.axet.bookreader.R
import com.github.axet.bookreader.app.BookApplication
import com.github.terrakok.modo.stack.LocalStackNavigation
import com.github.terrakok.modo.stack.back
import su.sv.commonui.theme.ThemeMode
import su.sv.commonui.ui.components.AppToolbarWithBack
import su.sv.managers.theme.ThemeViewModel

/**
 * Контент экрана настроек читалки
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderSettingsContent(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val stackNavigation = LocalStackNavigation.current
    val themeViewModel: ThemeViewModel = hiltViewModel()
    val themeConfig by themeViewModel.themeConfig.collectAsStateWithLifecycle()

    // Получаем SharedPreferences для остальных настроек
    val shared = remember {
        android.preference.PreferenceManager.getDefaultSharedPreferences(context)
    }

    // Состояния настроек (кроме темы - она через ThemeViewModel)
    var viewMode by remember {
        mutableStateOf(
            shared.getString(BookApplication.PREFERENCE_VIEW_MODE, "PAGING") ?: "PAGING"
        )
    }
    var volumeKeys by remember {
        mutableStateOf(shared.getBoolean(BookApplication.PREFERENCE_VOLUME_KEYS, false))
    }
    var screenLock by remember {
        mutableStateOf(shared.getString(BookApplication.PREFERENCE_SCREENLOCK, "0") ?: "0")
    }
    var rotate by remember {
        mutableStateOf(shared.getBoolean(BookApplication.PREFERENCE_ROTATE, false))
    }

    // Слушатель изменений (без темы)
    val listener = remember {
        SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
            when (key) {
                BookApplication.PREFERENCE_VIEW_MODE ->
                    viewMode = sharedPreferences.getString(key, "PAGING") ?: "PAGING"
                BookApplication.PREFERENCE_VOLUME_KEYS ->
                    volumeKeys = sharedPreferences.getBoolean(key, false)
                BookApplication.PREFERENCE_SCREENLOCK ->
                    screenLock = sharedPreferences.getString(key, "0") ?: "0"
                BookApplication.PREFERENCE_ROTATE ->
                    rotate = sharedPreferences.getBoolean(key, false)
            }
        }
    }

    DisposableEffect(Unit) {
        shared.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            shared.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AppToolbarWithBack(
                title = stringResource(R.string.sv_menu_settings),
                onBackClick = { stackNavigation.back() }
            )
        },
        modifier = modifier,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
        ) {
            // Режим просмотра
            SettingsItem(
                title = stringResource(R.string.sv_view_mode),
                subtitle = if (viewMode == "CONTINUOUS") {
                    stringResource(R.string.sv_view_mode_continuous)
                } else {
                    stringResource(R.string.sv_view_mode_paging)
                },
                onClick = {
                    val newMode = if (viewMode == "PAGING") "CONTINUOUS" else "PAGING"
                    shared.edit { putString(BookApplication.PREFERENCE_VIEW_MODE, newMode) }
                },
            )

            // Кнопки громкости
            SettingsSwitch(
                title = stringResource(R.string.sv_pref_volume_title),
                subtitle = stringResource(R.string.sv_pref_volume_summary),
                checked = volumeKeys,
                onCheckedChange = { checked ->
                    shared.edit { putBoolean(BookApplication.PREFERENCE_VOLUME_KEYS, checked) }
                },
            )

            // Поворот экрана
            SettingsSwitch(
                title = stringResource(R.string.sv_pref_rotate_title),
                subtitle = stringResource(R.string.sv_pref_rotate_summary),
                checked = rotate,
                onCheckedChange = { checked ->
                    shared.edit { putBoolean(BookApplication.PREFERENCE_ROTATE, checked) }
                },
            )

            // Тема (через ThemeViewModel)
            SettingsItem(
                title = stringResource(R.string.sv_pref_theme_title),
                subtitle = when (themeConfig.themeMode) {
                    ThemeMode.DARK -> stringResource(R.string.sv_theme_dark)
                    ThemeMode.LIGHT -> stringResource(R.string.sv_theme_light)
                },
                onClick = { themeViewModel.toggleTheme() },
            )

            // TODO: Добавить настройки:
            // - Папка синхронизации (требует SAF)
            // - TTS язык (требует TTS компонент)
            // - Блокировка экрана (требует ScreenlockPreference)
        }
    }
}

/**
 * Элемент настройки с кликом
 */
@Composable
private fun SettingsItem(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Элемент настройки с переключателем
 */
@Composable
private fun SettingsSwitch(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}
