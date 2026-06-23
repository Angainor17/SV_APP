package su.sv.commonui.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateValue
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.progressSemantics
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import su.sv.commonui.theme.LocalAppDimensions

/**
 * Индикатор загрузки приложения
 *
 * @param size размер индикатора
 * @param color цвет индикатора
 * @param strokeWidth толщина линии
 * @param sweepAngle угол дуги индикатора
 */
@Composable
fun AppLoadingIndicator(
    size: Dp? = null,
    color: Color? = null,
    strokeWidth: Dp = ProgressIndicatorDefaults.CircularStrokeWidth,
    sweepAngle: Float = 90f,
    modifier: Modifier = Modifier
) {
    val dimensions = LocalAppDimensions.current
    val actualSize = size ?: dimensions.loadingIndicatorSize
    val actualColor = color ?: MaterialTheme.colorScheme.primary

    // Анимация вращения
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

    // Определение stroke с учётом DPI устройства
    val stroke = with(LocalDensity.current) {
        Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Square)
    }

    Canvas(
        modifier = modifier
            .progressSemantics()
            .size(actualSize)
            .padding(strokeWidth / 2)
    ) {
        // Фоновый круг (серый)
        drawCircle(Color.LightGray, style = stroke)

        // Анимированная дуга
        drawArc(
            color = actualColor,
            startAngle = currentArcStartAngle.toFloat() - 90,
            sweepAngle = sweepAngle,
            useCenter = false,
            style = stroke
        )
    }
}

/**
 * Малый индикатор загрузки
 */
@Composable
fun AppLoadingIndicatorSmall(
    modifier: Modifier = Modifier,
    color: Color? = null
) {
    val dimensions = LocalAppDimensions.current
    AppLoadingIndicator(
        size = dimensions.loadingIndicatorSizeSmall,
        strokeWidth = 2.dp,
        color = color,
        modifier = modifier
    )
}

/**
 * Большой индикатор загрузки
 */
@Composable
fun AppLoadingIndicatorLarge(
    modifier: Modifier = Modifier,
    color: Color? = null
) {
    val dimensions = LocalAppDimensions.current
    AppLoadingIndicator(
        size = dimensions.loadingIndicatorSizeLarge,
        strokeWidth = 4.dp,
        color = color,
        modifier = modifier
    )
}

/**
 * Индикатор загрузки с цветом поверхности
 */
@Composable
fun AppLoadingIndicatorOnSurface(
    modifier: Modifier = Modifier,
    size: Dp? = null
) {
    AppLoadingIndicator(
        size = size,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier
    )
}

/**
 * Индикатор загрузки с цветом ошибки
 */
@Composable
fun AppLoadingIndicatorError(
    modifier: Modifier = Modifier,
    size: Dp? = null
) {
    AppLoadingIndicator(
        size = size,
        color = MaterialTheme.colorScheme.error,
        modifier = modifier
    )
}
