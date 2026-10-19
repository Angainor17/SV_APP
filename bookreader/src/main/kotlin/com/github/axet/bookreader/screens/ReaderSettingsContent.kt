package com.github.axet.bookreader.screens

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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.axet.bookreader.R
import com.github.axet.bookreader.screens.viewmodel.ReaderSettingsViewModel
import com.github.terrakok.modo.stack.LocalStackNavigation
import com.github.terrakok.modo.stack.back
import su.sv.commonui.theme.LocalDeviceFormFactor
import su.sv.commonui.ui.components.AppToolbarWithBack

/**
 * Контент экрана настроек читалки
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderSettingsContent(
    modifier: Modifier = Modifier,
    viewModel: ReaderSettingsViewModel = hiltViewModel(),
) {
    val stackNavigation = LocalStackNavigation.current
    val formFactor = LocalDeviceFormFactor.current

    val viewMode by viewModel.viewMode.collectAsStateWithLifecycle()
    val volumeKeys by viewModel.volumeKeys.collectAsStateWithLifecycle()
    val twoColumnView by viewModel.twoColumnView.collectAsStateWithLifecycle()

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
                onClick = viewModel::toggleViewMode,
            )

            // Кнопки громкости
            SettingsSwitch(
                title = stringResource(R.string.sv_pref_volume_title),
                subtitle = stringResource(R.string.sv_pref_volume_summary),
                checked = volumeKeys,
                onCheckedChange = viewModel::setVolumeKeys,
            )

            // Двухстраничный режим — только для планшетов
            if (!formFactor.isCompact()) {
                SettingsItem(
                    title = stringResource(R.string.sv_pref_two_column_title),
                    subtitle = when (twoColumnView) {
                        "true" -> stringResource(R.string.sv_view_mode_paging) // Включено
                        "false" -> stringResource(R.string.sv_view_mode_continuous) // Выключено (одна колонка)
                        else -> stringResource(R.string.sv_pref_two_column_auto)
                    },
                    onClick = viewModel::cycleTwoColumnView,
                )
            }

            // Планируемые настройки (требуют дополнительные компоненты):
            // - Папка синхронизации — требует интеграции Storage Access Framework (SAF)
            // - TTS язык — требует интеграции TTS движка (TextToSpeech)
            // - Блокировка экрана — требует реализации ScreenlockPreference
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
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}
