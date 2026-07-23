package su.sv.commonui.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import su.sv.commonui.theme.LocalAppDimensions
import su.sv.commonui.theme.cardStroke

/**
 * Базовая карточка приложения
 *
 * Используется для элементов списков, контейнеров контента.
 * Автоматически применяет цвета темы и стандартные отступы.
 *
 * @param modifier модификатор
 * @param onClick обработчик клика (null для некликабельной карточки)
 * @param hasBorder имеет ли карточка обводку
 * @param content содержимое карточки
 */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    hasBorder: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    val dimensions = LocalAppDimensions.current

    val cardModifier = modifier.padding(
        horizontal = dimensions.screenPaddingHorizontal,
        vertical = dimensions.cardPaddingOuter
    )

    val colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    )

    val border = if (hasBorder) {
        BorderStroke(
            width = dimensions.borderWidthStandard,
            color = MaterialTheme.colorScheme.cardStroke
        )
    } else {
        null
    }

    if (onClick != null) {
        Card(
            modifier = cardModifier,
            shape = MaterialTheme.shapes.medium,
            colors = colors,
            elevation = CardDefaults.cardElevation(
                defaultElevation = dimensions.cardElevation
            ),
            border = border,
            onClick = onClick,
            content = content
        )
    } else {
        Card(
            modifier = cardModifier,
            shape = MaterialTheme.shapes.medium,
            colors = colors,
            elevation = CardDefaults.cardElevation(
                defaultElevation = dimensions.cardElevation
            ),
            border = border,
            content = content
        )
    }
}

/**
 * Карточка без внешних отступов
 *
 * Используется когда отступы управляются внешним контейнером
 */
@Composable
fun AppCardNoPadding(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    hasBorder: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    val dimensions = LocalAppDimensions.current

    val colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    )

    val border = if (hasBorder) {
        BorderStroke(
            width = dimensions.borderWidthStandard,
            color = MaterialTheme.colorScheme.cardStroke
        )
    } else {
        null
    }

    if (onClick != null) {
        Card(
            modifier = modifier,
            shape = MaterialTheme.shapes.medium,
            colors = colors,
            elevation = CardDefaults.cardElevation(
                defaultElevation = dimensions.cardElevation
            ),
            border = border,
            onClick = onClick,
            content = content
        )
    } else {
        Card(
            modifier = modifier,
            shape = MaterialTheme.shapes.medium,
            colors = colors,
            elevation = CardDefaults.cardElevation(
                defaultElevation = dimensions.cardElevation
            ),
            border = border,
            content = content
        )
    }
}

/**
 * Карточка с акцентным цветом (например, для выделения)
 */
@Composable
fun AppCardAccent(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val dimensions = LocalAppDimensions.current

    val cardModifier = modifier.padding(
        horizontal = dimensions.screenPaddingHorizontal,
        vertical = dimensions.cardPaddingOuter
    )

    val colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    )

    if (onClick != null) {
        Card(
            modifier = cardModifier,
            shape = MaterialTheme.shapes.medium,
            colors = colors,
            elevation = CardDefaults.cardElevation(
                defaultElevation = dimensions.cardElevation
            ),
            onClick = onClick,
            content = content
        )
    } else {
        Card(
            modifier = cardModifier,
            shape = MaterialTheme.shapes.medium,
            colors = colors,
            elevation = CardDefaults.cardElevation(
                defaultElevation = dimensions.cardElevation
            ),
            content = content
        )
    }
}
