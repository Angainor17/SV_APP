package su.sv.news.presentation.debug.ui

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.terrakok.modo.stack.LocalStackNavigation
import com.github.terrakok.modo.stack.back
import su.sv.commonui.theme.CardStrokeDark
import su.sv.commonui.theme.CardStrokeLight
import su.sv.commonui.theme.CustomColorAttribute
import su.sv.commonui.theme.CustomThemeColors
import su.sv.commonui.theme.DarkBackground
import su.sv.commonui.theme.DarkError
import su.sv.commonui.theme.DarkErrorContainer
import su.sv.commonui.theme.DarkInverseOnSurface
import su.sv.commonui.theme.DarkInversePrimary
import su.sv.commonui.theme.DarkInverseSurface
import su.sv.commonui.theme.DarkLinkColor
import su.sv.commonui.theme.DarkNavigationBarColor
import su.sv.commonui.theme.DarkOnBackground
import su.sv.commonui.theme.DarkOnError
import su.sv.commonui.theme.DarkOnErrorContainer
import su.sv.commonui.theme.DarkOnPrimary
import su.sv.commonui.theme.DarkOnPrimaryContainer
import su.sv.commonui.theme.DarkOnSecondary
import su.sv.commonui.theme.DarkOnSecondaryContainer
import su.sv.commonui.theme.DarkOnSurface
import su.sv.commonui.theme.DarkOnSurfaceVariant
import su.sv.commonui.theme.DarkOnTertiary
import su.sv.commonui.theme.DarkOnTertiaryContainer
import su.sv.commonui.theme.DarkOutline
import su.sv.commonui.theme.DarkOutlineVariant
import su.sv.commonui.theme.DarkPrimary
import su.sv.commonui.theme.DarkPrimaryContainer
import su.sv.commonui.theme.DarkSecondary
import su.sv.commonui.theme.DarkSecondaryContainer
import su.sv.commonui.theme.DarkSurface
import su.sv.commonui.theme.DarkSurfaceBright
import su.sv.commonui.theme.DarkSurfaceContainer
import su.sv.commonui.theme.DarkSurfaceContainerHigh
import su.sv.commonui.theme.DarkSurfaceContainerHighest
import su.sv.commonui.theme.DarkSurfaceContainerLow
import su.sv.commonui.theme.DarkSurfaceContainerLowest
import su.sv.commonui.theme.DarkSurfaceDim
import su.sv.commonui.theme.DarkSurfaceVariant
import su.sv.commonui.theme.DarkTertiary
import su.sv.commonui.theme.DarkTertiaryContainer
import su.sv.commonui.theme.LightBackground
import su.sv.commonui.theme.LightError
import su.sv.commonui.theme.LightErrorContainer
import su.sv.commonui.theme.LightInverseOnSurface
import su.sv.commonui.theme.LightInversePrimary
import su.sv.commonui.theme.LightInverseSurface
import su.sv.commonui.theme.LightLinkColor
import su.sv.commonui.theme.LightNavigationBarColor
import su.sv.commonui.theme.LightOnBackground
import su.sv.commonui.theme.LightOnError
import su.sv.commonui.theme.LightOnErrorContainer
import su.sv.commonui.theme.LightOnPrimary
import su.sv.commonui.theme.LightOnPrimaryContainer
import su.sv.commonui.theme.LightOnSecondary
import su.sv.commonui.theme.LightOnSecondaryContainer
import su.sv.commonui.theme.LightOnSurface
import su.sv.commonui.theme.LightOnSurfaceVariant
import su.sv.commonui.theme.LightOnTertiary
import su.sv.commonui.theme.LightOnTertiaryContainer
import su.sv.commonui.theme.LightOutline
import su.sv.commonui.theme.LightOutlineVariant
import su.sv.commonui.theme.LightPrimary
import su.sv.commonui.theme.LightPrimaryContainer
import su.sv.commonui.theme.LightSecondary
import su.sv.commonui.theme.LightSecondaryContainer
import su.sv.commonui.theme.LightSurface
import su.sv.commonui.theme.LightSurfaceBright
import su.sv.commonui.theme.LightSurfaceContainer
import su.sv.commonui.theme.LightSurfaceContainerHigh
import su.sv.commonui.theme.LightSurfaceContainerHighest
import su.sv.commonui.theme.LightSurfaceContainerLow
import su.sv.commonui.theme.LightSurfaceContainerLowest
import su.sv.commonui.theme.LightSurfaceDim
import su.sv.commonui.theme.LightSurfaceVariant
import su.sv.commonui.theme.LightTertiary
import su.sv.commonui.theme.LightTertiaryContainer
import su.sv.commonui.ui.components.AppToolbarWithBack
import su.sv.news.R
import su.sv.news.presentation.debug.ThemeEditorViewModel
import timber.log.Timber

/**
 * Контент экрана редактирования темы
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeEditorContent(
    viewModel: ThemeEditorViewModel = hiltViewModel()
) {
    val navigation = LocalStackNavigation.current
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Диалоги
    var showResetDialog by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }
    var selectedAttribute by remember { mutableStateOf<CustomColorAttribute?>(null) }

    // Устанавливаем callback для перезапуска
    LaunchedEffect(Unit) {
        viewModel.onRestartApp = {
            // Сначала закрываем экран редактора
            navigation.back()
            // Потом перезапускаем Activity
            (context as? Activity)?.recreate()
        }
    }

    Scaffold(
        topBar = {
            AppToolbarWithBack(
                title = stringResource(R.string.theme_editor_title),
                onBackClick = { navigation.back() },
                actions = {
                    IconButton(onClick = { showResetDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.theme_editor_reset)
                        )
                    }
                }
            )
        },
        bottomBar = {
            BottomApplyButton(
                onApply = { viewModel.applyChanges() }
            )
        }
    ) { paddingValues ->
        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Селектор темы
                ThemeModeSelector(
                    selectedMode = state.themeMode,
                    onModeSelected = { viewModel.setThemeMode(it) }
                )

                // Список атрибутов
                ColorAttributeList(
                    themeMode = state.themeMode,
                    customColors = state.customColors,
                    onColorClick = { attribute ->
                        selectedAttribute = attribute
                        showColorPicker = true
                    }
                )
            }
        }

        // Диалог сброса
        if (showResetDialog) {
            ResetThemeDialog(
                onDismiss = { showResetDialog = false },
                onConfirm = {
                    showResetDialog = false
                    viewModel.resetToDefault()
                }
            )
        }

        // Диалог выбора цвета
        if (showColorPicker && selectedAttribute != null) {
            val originalColor = getOriginalColor(selectedAttribute!!, state.themeMode)
            val currentColor = state.customColors.getColor(selectedAttribute!!.attributeName)
                ?: originalColor

            // Логирование для отладки
            Timber.tag("ThemeEditor").d(
                "Dialog: customColors size=%d, attr=%s, currentColor=%s",
                state.customColors.colors.size,
                selectedAttribute?.attributeName,
                currentColor
            )

            ColorPickerDialog(
                attributeName = selectedAttribute!!.attributeName,
                originalColor = originalColor,
                currentColor = currentColor,
                onColorSelected = { color ->
                    Timber.tag("ThemeEditor").d("Color selected: %s = %s", selectedAttribute?.attributeName, color)
                    viewModel.setColor(selectedAttribute!!, color)
                    showColorPicker = false
                },
                onDismiss = { showColorPicker = false }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeModeSelector(
    selectedMode: String,
    onModeSelected: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        SingleChoiceSegmentedButtonRow {
            SegmentedButton(
                selected = selectedMode == "LIGHT",
                onClick = { onModeSelected("LIGHT") },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
            ) {
                Text(stringResource(R.string.theme_editor_light))
            }
            SegmentedButton(
                selected = selectedMode == "DARK",
                onClick = { onModeSelected("DARK") },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
            ) {
                Text(stringResource(R.string.theme_editor_dark))
            }
        }
    }
}

@Composable
private fun ColorAttributeList(
    themeMode: String,
    customColors: CustomThemeColors,
    onColorClick: (CustomColorAttribute) -> Unit
) {
    val groupedAttributes = CustomColorAttribute.groupedByCategory()

    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        groupedAttributes.forEach { (group, attributes) ->
            // Заголовок группы
            item {
                Text(
                    text = getGroupTitle(group),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // Элементы группы
            items(attributes) { attribute ->
                ColorAttributeItem(
                    attribute = attribute,
                    originalColor = getOriginalColor(attribute, themeMode),
                    customColor = customColors.getColor(attribute.attributeName),
                    onClick = { onColorClick(attribute) }
                )
            }
        }
    }
}

@Composable
private fun ColorAttributeItem(
    attribute: CustomColorAttribute,
    originalColor: Color,
    customColor: Color?,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = attribute.attributeName,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = attribute.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Цвета
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Исходный цвет
                ColorBox(
                    color = originalColor,
                    label = stringResource(R.string.color_label_original),
                    onClick = null
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Кастомный цвет (или null)
                ColorBox(
                    color = customColor ?: MaterialTheme.colorScheme.surfaceVariant,
                    label = if (customColor != null) stringResource(R.string.color_label_new) else stringResource(R.string.color_label_not_set),
                    onClick = onClick,
                    isCustom = true
                )
            }
        }
    }
}

@Composable
private fun ColorBox(
    color: Color,
    label: String,
    onClick: (() -> Unit)?,
    isCustom: Boolean = false
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(color)
                .then(
                    if (isCustom && onClick != null) {
                        Modifier.border(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape
                        )
                    } else {
                        Modifier
                    }
                )
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

@Composable
private fun BottomApplyButton(
    onApply: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            FilledTonalButton(
                onClick = onApply,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.theme_editor_apply))
            }
        }
    }
}

@Composable
private fun ResetThemeDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.reset_dialog_title)) },
        text = { Text(stringResource(R.string.reset_dialog_message)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.reset_dialog_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.reset_dialog_cancel))
            }
        }
    )
}

// ============================================================
// Helper functions
// ============================================================

@Composable
private fun getGroupTitle(group: CustomColorAttribute.ColorGroup): String {
    return when (group) {
        CustomColorAttribute.ColorGroup.PRIMARY -> stringResource(R.string.color_group_primary)
        CustomColorAttribute.ColorGroup.SECONDARY -> stringResource(R.string.color_group_secondary)
        CustomColorAttribute.ColorGroup.TERTIARY -> stringResource(R.string.color_group_tertiary)
        CustomColorAttribute.ColorGroup.BACKGROUND -> stringResource(R.string.color_group_background)
        CustomColorAttribute.ColorGroup.SURFACE -> stringResource(R.string.color_group_surface)
        CustomColorAttribute.ColorGroup.OUTLINE -> stringResource(R.string.color_group_outline)
        CustomColorAttribute.ColorGroup.ERROR -> stringResource(R.string.color_group_error)
        CustomColorAttribute.ColorGroup.INVERSE -> stringResource(R.string.color_group_inverse)
        CustomColorAttribute.ColorGroup.ADDITIONAL -> stringResource(R.string.color_group_additional)
    }
}

private fun getOriginalColor(attribute: CustomColorAttribute, themeMode: String): Color {
    return when (themeMode) {
        "LIGHT" -> getLightColor(attribute)
        "DARK" -> getDarkColor(attribute)
        else -> Color.Gray
    }
}

private fun getLightColor(attribute: CustomColorAttribute): Color {
    return when (attribute) {
        CustomColorAttribute.PRIMARY -> LightPrimary
        CustomColorAttribute.ON_PRIMARY -> LightOnPrimary
        CustomColorAttribute.PRIMARY_CONTAINER -> LightPrimaryContainer
        CustomColorAttribute.ON_PRIMARY_CONTAINER -> LightOnPrimaryContainer
        CustomColorAttribute.SECONDARY -> LightSecondary
        CustomColorAttribute.ON_SECONDARY -> LightOnSecondary
        CustomColorAttribute.SECONDARY_CONTAINER -> LightSecondaryContainer
        CustomColorAttribute.ON_SECONDARY_CONTAINER -> LightOnSecondaryContainer
        CustomColorAttribute.TERTIARY -> LightTertiary
        CustomColorAttribute.ON_TERTIARY -> LightOnTertiary
        CustomColorAttribute.TERTIARY_CONTAINER -> LightTertiaryContainer
        CustomColorAttribute.ON_TERTIARY_CONTAINER -> LightOnTertiaryContainer
        CustomColorAttribute.BACKGROUND -> LightBackground
        CustomColorAttribute.ON_BACKGROUND -> LightOnBackground
        CustomColorAttribute.SURFACE -> LightSurface
        CustomColorAttribute.ON_SURFACE -> LightOnSurface
        CustomColorAttribute.SURFACE_VARIANT -> LightSurfaceVariant
        CustomColorAttribute.ON_SURFACE_VARIANT -> LightOnSurfaceVariant
        CustomColorAttribute.SURFACE_DIM -> LightSurfaceDim
        CustomColorAttribute.SURFACE_BRIGHT -> LightSurfaceBright
        CustomColorAttribute.SURFACE_CONTAINER_LOWEST -> LightSurfaceContainerLowest
        CustomColorAttribute.SURFACE_CONTAINER_LOW -> LightSurfaceContainerLow
        CustomColorAttribute.SURFACE_CONTAINER -> LightSurfaceContainer
        CustomColorAttribute.SURFACE_CONTAINER_HIGH -> LightSurfaceContainerHigh
        CustomColorAttribute.SURFACE_CONTAINER_HIGHEST -> LightSurfaceContainerHighest
        CustomColorAttribute.OUTLINE -> LightOutline
        CustomColorAttribute.OUTLINE_VARIANT -> LightOutlineVariant
        CustomColorAttribute.ERROR -> LightError
        CustomColorAttribute.ON_ERROR -> LightOnError
        CustomColorAttribute.ERROR_CONTAINER -> LightErrorContainer
        CustomColorAttribute.ON_ERROR_CONTAINER -> LightOnErrorContainer
        CustomColorAttribute.INVERSE_SURFACE -> LightInverseSurface
        CustomColorAttribute.INVERSE_ON_SURFACE -> LightInverseOnSurface
        CustomColorAttribute.INVERSE_PRIMARY -> LightInversePrimary
        CustomColorAttribute.NAVIGATION_BAR -> LightNavigationBarColor
        CustomColorAttribute.LINK -> LightLinkColor
        CustomColorAttribute.CARD_STROKE -> CardStrokeLight
    }
}

private fun getDarkColor(attribute: CustomColorAttribute): Color {
    return when (attribute) {
        CustomColorAttribute.PRIMARY -> DarkPrimary
        CustomColorAttribute.ON_PRIMARY -> DarkOnPrimary
        CustomColorAttribute.PRIMARY_CONTAINER -> DarkPrimaryContainer
        CustomColorAttribute.ON_PRIMARY_CONTAINER -> DarkOnPrimaryContainer
        CustomColorAttribute.SECONDARY -> DarkSecondary
        CustomColorAttribute.ON_SECONDARY -> DarkOnSecondary
        CustomColorAttribute.SECONDARY_CONTAINER -> DarkSecondaryContainer
        CustomColorAttribute.ON_SECONDARY_CONTAINER -> DarkOnSecondaryContainer
        CustomColorAttribute.TERTIARY -> DarkTertiary
        CustomColorAttribute.ON_TERTIARY -> DarkOnTertiary
        CustomColorAttribute.TERTIARY_CONTAINER -> DarkTertiaryContainer
        CustomColorAttribute.ON_TERTIARY_CONTAINER -> DarkOnTertiaryContainer
        CustomColorAttribute.BACKGROUND -> DarkBackground
        CustomColorAttribute.ON_BACKGROUND -> DarkOnBackground
        CustomColorAttribute.SURFACE -> DarkSurface
        CustomColorAttribute.ON_SURFACE -> DarkOnSurface
        CustomColorAttribute.SURFACE_VARIANT -> DarkSurfaceVariant
        CustomColorAttribute.ON_SURFACE_VARIANT -> DarkOnSurfaceVariant
        CustomColorAttribute.SURFACE_DIM -> DarkSurfaceDim
        CustomColorAttribute.SURFACE_BRIGHT -> DarkSurfaceBright
        CustomColorAttribute.SURFACE_CONTAINER_LOWEST -> DarkSurfaceContainerLowest
        CustomColorAttribute.SURFACE_CONTAINER_LOW -> DarkSurfaceContainerLow
        CustomColorAttribute.SURFACE_CONTAINER -> DarkSurfaceContainer
        CustomColorAttribute.SURFACE_CONTAINER_HIGH -> DarkSurfaceContainerHigh
        CustomColorAttribute.SURFACE_CONTAINER_HIGHEST -> DarkSurfaceContainerHighest
        CustomColorAttribute.OUTLINE -> DarkOutline
        CustomColorAttribute.OUTLINE_VARIANT -> DarkOutlineVariant
        CustomColorAttribute.ERROR -> DarkError
        CustomColorAttribute.ON_ERROR -> DarkOnError
        CustomColorAttribute.ERROR_CONTAINER -> DarkErrorContainer
        CustomColorAttribute.ON_ERROR_CONTAINER -> DarkOnErrorContainer
        CustomColorAttribute.INVERSE_SURFACE -> DarkInverseSurface
        CustomColorAttribute.INVERSE_ON_SURFACE -> DarkInverseOnSurface
        CustomColorAttribute.INVERSE_PRIMARY -> DarkInversePrimary
        CustomColorAttribute.NAVIGATION_BAR -> DarkNavigationBarColor
        CustomColorAttribute.LINK -> DarkLinkColor
        CustomColorAttribute.CARD_STROKE -> CardStrokeDark
    }
}