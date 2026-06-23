package su.sv.commonui.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateValue
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.progressSemantics
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Стиль кнопки
 */
enum class ButtonStyle {
    /** Заполненная кнопка (основная) */
    FILLED,

    /** Контурная кнопка (вторичная) */
    OUTLINED,

    /** Текстовая кнопка */
    TEXT,

    /** Приглушённая кнопка (тонированная) */
    TONAL
}

/**
 * Основная кнопка приложения
 *
 * @param text текст кнопки
 * @param onClick обработчик клика
 * @param modifier модификатор
 * @param isLoading состояние загрузки
 * @param enabled доступность кнопки
 * @param style стиль кнопки
 */
@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    enabled: Boolean = true,
    style: ButtonStyle = ButtonStyle.FILLED
) {
    when (style) {
        ButtonStyle.FILLED -> FilledButton(text, onClick, modifier, isLoading, enabled)
        ButtonStyle.OUTLINED -> OutlinedButton(text, onClick, modifier, isLoading, enabled)
        ButtonStyle.TEXT -> TextButton(text, onClick, modifier, isLoading, enabled)
        ButtonStyle.TONAL -> TonalButton(text, onClick, modifier, isLoading, enabled)
    }
}

@Composable
private fun FilledButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier,
    isLoading: Boolean,
    enabled: Boolean
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled && !isLoading,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        ButtonContent(text, isLoading)
    }
}

@Composable
private fun OutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier,
    isLoading: Boolean,
    enabled: Boolean
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled && !isLoading
    ) {
        ButtonContent(text, isLoading)
    }
}

@Composable
private fun TextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier,
    isLoading: Boolean,
    enabled: Boolean
) {
    TextButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled && !isLoading
    ) {
        ButtonContent(text, isLoading)
    }
}

@Composable
private fun TonalButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier,
    isLoading: Boolean,
    enabled: Boolean
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled && !isLoading
    ) {
        ButtonContent(text, isLoading)
    }
}

@Composable
private fun ButtonContent(
    text: String,
    isLoading: Boolean
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isLoading) {
            AppLoadingIndicatorSmall(
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

/**
 * Малый индикатор загрузки для кнопок
 */
@Composable
private fun AppLoadingIndicatorSmall(
    size: Dp = 16.dp,
    color: Color = MaterialTheme.colorScheme.primary,
    strokeWidth: Dp = 2.dp
) {
    val transition = rememberInfiniteTransition()
    val currentArcStartAngle by transition.animateValue(
        0,
        360,
        Int.VectorConverter,
        infiniteRepeatable(
            animation = tween(
                durationMillis = 1100,
                easing = LinearEasing
            )
        )
    )

    val stroke = with(LocalDensity.current) {
        Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Square)
    }

    Canvas(
        Modifier
            .progressSemantics()
            .size(size)
            .padding(strokeWidth / 2)
    ) {
        drawCircle(Color.LightGray, style = stroke)
        drawArc(
            color,
            startAngle = currentArcStartAngle.toFloat() - 90,
            sweepAngle = 90f,
            useCenter = false,
            style = stroke
        )
    }
}

/**
 * Кнопка с иконкой
 */
@Composable
fun AppIconButton(
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    style: ButtonStyle = ButtonStyle.FILLED
) {
    when (style) {
        ButtonStyle.FILLED -> {
            Button(
                onClick = onClick,
                modifier = modifier,
                enabled = enabled,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                icon()
            }
        }
        ButtonStyle.OUTLINED -> {
            OutlinedButton(
                onClick = onClick,
                modifier = modifier,
                enabled = enabled
            ) {
                icon()
            }
        }
        ButtonStyle.TEXT -> {
            TextButton(
                onClick = onClick,
                modifier = modifier,
                enabled = enabled
            ) {
                icon()
            }
        }
        ButtonStyle.TONAL -> {
            FilledTonalButton(
                onClick = onClick,
                modifier = modifier,
                enabled = enabled
            ) {
                icon()
            }
        }
    }
}
