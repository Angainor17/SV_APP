package su.sv.commonui.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Полноэкранный индикатор загрузки с красивой анимацией
 */
@Composable
fun FullScreenLoading() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedLoadingIndicator()
    }
}

/**
 * Анимированный индикатор загрузки - три точки с пульсацией
 */
@Composable
fun AnimatedLoadingIndicator(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    val infiniteTransition = rememberInfiniteTransition()

    // Анимация масштаба для каждой точки с разным delay
    val scale1 by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 600,
                easing = FastOutSlowInEasing,
                delayMillis = 0
            ),
            repeatMode = RepeatMode.Reverse
        )
    )

    val scale2 by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 600,
                easing = FastOutSlowInEasing,
                delayMillis = 200
            ),
            repeatMode = RepeatMode.Reverse
        )
    )

    val scale3 by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 600,
                easing = FastOutSlowInEasing,
                delayMillis = 400
            ),
            repeatMode = RepeatMode.Reverse
        )
    )

    Row(
        modifier = modifier.padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Dot(scale = scale1, color = color)
        Dot(scale = scale2, color = color)
        Dot(scale = scale3, color = color)
    }
}

/**
 * Анимированный индикатор загрузки - двойной вращающийся круг
 */
@Composable
fun DoubleCircleLoadingIndicator(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    size: Dp = 48.dp,
) {
    val infiniteTransition = rememberInfiniteTransition()

    // Вращение внешнего круга
    val rotation1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing)
        )
    )

    // Вращение внутреннего круга (в другую сторону)
    val rotation2 by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing)
        )
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // Внешний круг
        LoadingCircle(
            size = size,
            strokeWidth = 3.dp,
            rotation = rotation1,
            color = color,
            sweepAngle = 90f
        )

        // Внутренний круг
        LoadingCircle(
            size = size * 0.6f,
            strokeWidth = 2.dp,
            rotation = rotation2,
            color = color.copy(alpha = 0.6f),
            sweepAngle = 120f
        )
    }
}

/**
 * Вращающийся круг для лоадинга
 */
@Composable
private fun LoadingCircle(
    size: Dp,
    strokeWidth: Dp,
    rotation: Float,
    color: Color,
    sweepAngle: Float,
) {
    androidx.compose.foundation.Canvas(
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

/**
 * Одна точка в анимации
 */
@Composable
private fun Dot(
    scale: Float,
    color: Color,
    size: Dp = 12.dp,
) {
    Box(
        modifier = Modifier
            .size(size)
            .scale(scale)
            .clip(CircleShape)
            .background(color)
    )
}