package su.sv.commonui.ui.components

import android.content.res.Configuration
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import su.sv.commonui.theme.SVAPPTheme

/**
 * Стиль анимации загрузки
 */
enum class LoadingStyle {
    /** Три точки с пульсацией */
    PULSE_DOTS,
    /** Двойной вращающийся круг */
    DOUBLE_CIRCLE,
    /** Пульсирующий круг */
    PULSE_CIRCLE,
    /** Волновые точки */
    WAVE_DOTS,
    /** Градиентная вращающаяся дуга */
    GRADIENT_ARC
}

/**
 * Анимированный индикатор загрузки с несколькими стилями
 *
 * @param style стиль анимации
 * @param color цвет индикатора (по умолчанию primary)
 * @param backgroundColor цвет фона для элементов (по умолчанию surfaceVariant)
 * @param size размер индикатора
 */
@Composable
fun AnimatedLoadingIndicator(
    style: LoadingStyle = LoadingStyle.GRADIENT_ARC,
    color: Color = MaterialTheme.colorScheme.primary,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    size: Dp = 48.dp,
    modifier: Modifier = Modifier
) {
    when (style) {
        LoadingStyle.PULSE_DOTS -> PulseDotsIndicator(
            color = color,
            modifier = modifier
        )
        LoadingStyle.DOUBLE_CIRCLE -> DoubleCircleIndicator(
            color = color,
            backgroundColor = backgroundColor,
            size = size,
            modifier = modifier
        )
        LoadingStyle.PULSE_CIRCLE -> PulseCircleIndicator(
            color = color,
            size = size,
            modifier = modifier
        )
        LoadingStyle.WAVE_DOTS -> WaveDotsIndicator(
            color = color,
            modifier = modifier
        )
        LoadingStyle.GRADIENT_ARC -> GradientArcIndicator(
            color = color,
            secondaryColor = MaterialTheme.colorScheme.secondary,
            size = size,
            modifier = modifier
        )
    }
}

// ============================================================
// PULSE DOTS - Три точки с пульсацией
// ============================================================

@Composable
private fun PulseDotsIndicator(
    color: Color,
    dotSize: Dp = 12.dp,
    spacing: Dp = 12.dp,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition()

    val scale1 by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing, delayMillis = 0),
            repeatMode = RepeatMode.Reverse
        )
    )

    val scale2 by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing, delayMillis = 200),
            repeatMode = RepeatMode.Reverse
        )
    )

    val scale3 by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing, delayMillis = 400),
            repeatMode = RepeatMode.Reverse
        )
    )

    Row(
        modifier = modifier.padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PulsingDot(scale = scale1, color = color, size = dotSize)
        PulsingDot(scale = scale2, color = color, size = dotSize)
        PulsingDot(scale = scale3, color = color, size = dotSize)
    }
}

@Composable
private fun PulsingDot(
    scale: Float,
    color: Color,
    size: Dp
) {
    Box(
        modifier = Modifier
            .size(size)
            .scale(scale)
            .clip(CircleShape)
            .background(color)
    )
}

// ============================================================
// DOUBLE CIRCLE - Двойной вращающийся круг
// ============================================================

@Composable
private fun DoubleCircleIndicator(
    color: Color,
    backgroundColor: Color,
    size: Dp,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition()

    // Вращение внешнего круга
    val rotation1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing)
        )
    )

    // Вращение внутреннего круга (в другую сторону)
    val rotation2 by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing)
        )
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // Фоновый круг для лучшей видимости на темной теме
        LoadingArc(
            size = size,
            strokeWidth = 3.dp,
            rotation = 0f,
            color = backgroundColor.copy(alpha = 0.3f),
            sweepAngle = 360f
        )

        // Внешний круг
        LoadingArc(
            size = size,
            strokeWidth = 3.dp,
            rotation = rotation1,
            color = color,
            sweepAngle = 90f
        )

        // Внутренний круг
        LoadingArc(
            size = size * 0.6f,
            strokeWidth = 2.dp,
            rotation = rotation2,
            color = color.copy(alpha = 0.7f),
            sweepAngle = 120f
        )
    }
}

// ============================================================
// PULSE CIRCLE - Пульсирующий круг
// ============================================================

@Composable
private fun PulseCircleIndicator(
    color: Color,
    size: Dp,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition()

    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // Внешний пульсирующий круг
        Box(
            modifier = Modifier
                .size(size * 0.8f)
                .scale(scale)
                .clip(CircleShape)
                .background(color.copy(alpha = alpha * 0.2f))
        )

        // Внутренний статичный круг
        Box(
            modifier = Modifier
                .size(size * 0.4f)
                .clip(CircleShape)
                .background(color)
        )
    }
}

// ============================================================
// WAVE DOTS - Волновые точки
// ============================================================

@Composable
private fun WaveDotsIndicator(
    color: Color,
    dotSize: Dp = 10.dp,
    spacing: Dp = 8.dp,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition()

    // Анимация высоты для каждой точки
    val height1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = FastOutSlowInEasing, delayMillis = 0),
            repeatMode = RepeatMode.Reverse
        )
    )

    val height2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = FastOutSlowInEasing, delayMillis = 100),
            repeatMode = RepeatMode.Reverse
        )
    )

    val height3 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = FastOutSlowInEasing, delayMillis = 200),
            repeatMode = RepeatMode.Reverse
        )
    )

    val height4 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = FastOutSlowInEasing, delayMillis = 300),
            repeatMode = RepeatMode.Reverse
        )
    )

    val height5 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = FastOutSlowInEasing, delayMillis = 400),
            repeatMode = RepeatMode.Reverse
        )
    )

    Row(
        modifier = modifier.padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        WaveDot(height = height1, color = color, size = dotSize)
        WaveDot(height = height2, color = color, size = dotSize)
        WaveDot(height = height3, color = color, size = dotSize)
        WaveDot(height = height4, color = color, size = dotSize)
        WaveDot(height = height5, color = color, size = dotSize)
    }
}

@Composable
private fun WaveDot(
    height: Float,
    color: Color,
    size: Dp
) {
    val scale = 0.5f + height * 0.5f
    val alpha = 0.3f + height * 0.7f

    Box(
        modifier = Modifier
            .size(size)
            .scale(scale)
            .clip(CircleShape)
            .background(color.copy(alpha = alpha))
    )
}

// ============================================================
// GRADIENT ARC - Градиентная вращающаяся дуга
// ============================================================

@Composable
private fun GradientArcIndicator(
    color: Color,
    secondaryColor: Color,
    size: Dp,
    strokeWidth: Dp = 4.dp,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition()

    // Вращение дуги
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing)
        )
    )

    // Анимация длины дуги (sweep angle)
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 30f,
        targetValue = 270f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        GradientArcCanvas(
            size = size,
            strokeWidth = strokeWidth,
            rotation = rotation,
            sweepAngle = sweepAngle,
            color = color,
            secondaryColor = secondaryColor
        )
    }
}

@Composable
private fun GradientArcCanvas(
    size: Dp,
    strokeWidth: Dp,
    rotation: Float,
    sweepAngle: Float,
    color: Color,
    secondaryColor: Color
) {
    Canvas(
        modifier = Modifier.size(size)
    ) {
        val stroke = Stroke(
            width = strokeWidth.toPx(),
            cap = StrokeCap.Round
        )

        // Градиент от primary к secondary по кругу
        val gradient = Brush.sweepGradient(
            colors = listOf(
                color.copy(alpha = 0.1f),
                color,
                secondaryColor,
                color.copy(alpha = 0.1f)
            ),
            center = center
        )

        drawArc(
            brush = gradient,
            startAngle = rotation - 90f,
            sweepAngle = sweepAngle,
            useCenter = false,
            style = stroke
        )
    }
}

// ============================================================
// Helpers
// ============================================================

@Composable
private fun LoadingArc(
    size: Dp,
    strokeWidth: Dp,
    rotation: Float,
    color: Color,
    sweepAngle: Float
) {
    Canvas(
        modifier = Modifier.size(size)
    ) {
        val stroke = Stroke(
            width = strokeWidth.toPx(),
            cap = StrokeCap.Round
        )

        drawArc(
            color = color,
            startAngle = rotation - 90f,
            sweepAngle = sweepAngle,
            useCenter = false,
            style = stroke
        )
    }
}

// ============================================================
// Previews
// ============================================================

//region Previews

@Preview(showBackground = true, name = "Pulse Dots - Light")
@Composable
private fun PulseDotsLightPreview() {
    SVAPPTheme {
        AnimatedLoadingIndicator(style = LoadingStyle.PULSE_DOTS)
    }
}

@Preview(showBackground = true, name = "Pulse Dots - Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PulseDotsDarkPreview() {
    SVAPPTheme {
        AnimatedLoadingIndicator(style = LoadingStyle.PULSE_DOTS)
    }
}

@Preview(showBackground = true, name = "Double Circle - Light")
@Composable
private fun DoubleCircleLightPreview() {
    SVAPPTheme {
        AnimatedLoadingIndicator(style = LoadingStyle.DOUBLE_CIRCLE)
    }
}

@Preview(showBackground = true, name = "Double Circle - Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DoubleCircleDarkPreview() {
    SVAPPTheme {
        AnimatedLoadingIndicator(style = LoadingStyle.DOUBLE_CIRCLE)
    }
}

@Preview(showBackground = true, name = "Pulse Circle - Light")
@Composable
private fun PulseCircleLightPreview() {
    SVAPPTheme {
        AnimatedLoadingIndicator(style = LoadingStyle.PULSE_CIRCLE)
    }
}

@Preview(showBackground = true, name = "Pulse Circle - Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PulseCircleDarkPreview() {
    SVAPPTheme {
        AnimatedLoadingIndicator(style = LoadingStyle.PULSE_CIRCLE)
    }
}

@Preview(showBackground = true, name = "Wave Dots - Light")
@Composable
private fun WaveDotsLightPreview() {
    SVAPPTheme {
        AnimatedLoadingIndicator(style = LoadingStyle.WAVE_DOTS)
    }
}

@Preview(showBackground = true, name = "Wave Dots - Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun WaveDotsDarkPreview() {
    SVAPPTheme {
        AnimatedLoadingIndicator(style = LoadingStyle.WAVE_DOTS)
    }
}

@Preview(showBackground = true, name = "Gradient Arc - Light")
@Composable
private fun GradientArcLightPreview() {
    SVAPPTheme {
        AnimatedLoadingIndicator(style = LoadingStyle.GRADIENT_ARC)
    }
}

@Preview(showBackground = true, name = "Gradient Arc - Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun GradientArcDarkPreview() {
    SVAPPTheme {
        AnimatedLoadingIndicator(style = LoadingStyle.GRADIENT_ARC)
    }
}

@Preview(showBackground = true, name = "Full Screen Loading - Light")
@Composable
private fun FullScreenLoadingLightPreview() {
    SVAPPTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            AnimatedLoadingIndicator(style = LoadingStyle.PULSE_DOTS)
        }
    }
}

@Preview(showBackground = true, name = "Full Screen Loading - Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun FullScreenLoadingDarkPreview() {
    SVAPPTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            AnimatedLoadingIndicator(style = LoadingStyle.PULSE_DOTS)
        }
    }
}

//endregion