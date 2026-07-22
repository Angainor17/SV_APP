package su.sv.wiki.presentation.root.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

/**
 * Анимированный hint с эффектом печатающегося текста
 *
 * @param hints список текстов для показа (сменяются по очереди)
 * @param typingSpeedMs скорость печати одного символа (по умолчанию 110ms)
 * @param pauseBetweenHintsMs пауза между сменой hints (по умолчанию 3000ms)
 * @param initialDelayMs начальная задержка перед началом анимации (по умолчанию 2000ms)
 * @param initialPlaceholder текст, показываемый во время начальной паузы (по умолчанию "Поиск")
 * @param showCursor показывать мигающий курсор в конце (по умолчанию true)
 */
@Composable
fun AnimatedTypingHint(
    hints: List<String>,
    modifier: Modifier = Modifier,
    typingSpeedMs: Long = 110L,
    pauseBetweenHintsMs: Long = 3000L,
    initialDelayMs: Long = 2000L,
    initialPlaceholder: String,
    showCursor: Boolean = true,
) {
    if (hints.isEmpty()) return

    var currentHintIndex by remember { mutableIntStateOf(0) }
    var currentTextLength by remember { mutableIntStateOf(0) }
    var isTyping by remember { mutableStateOf(true) }
    var hasStarted by remember { mutableStateOf(false) }

    val currentHint = hints[currentHintIndex]

    // Анимация длины текста
    val animatedLength by animateIntAsState(
        targetValue = currentTextLength,
        animationSpec = tween(
            durationMillis = (currentTextLength * typingSpeedMs).toInt(),
            easing = LinearEasing
        ),
        label = "text_length"
    )

    // Эффект печати и смены hints
    LaunchedEffect(hints) {
        // Начальная задержка перед началом анимации
        if (!hasStarted) {
            delay(initialDelayMs.milliseconds)
            hasStarted = true
        }

        while (true) {
            // Печатаем текст
            isTyping = true
            currentTextLength = currentHint.length
            delay(currentHint.length * typingSpeedMs)

            // Пауза перед сменой
            isTyping = false
            delay(pauseBetweenHintsMs)

            // Стираем текст (быстро)
            currentTextLength = 0
            delay(typingSpeedMs * 5)

            // Следующий hint
            currentHintIndex = (currentHintIndex + 1) % hints.size
        }
    }

    // До начала анимации показываем placeholder
    if (!hasStarted) {
        Text(
            text = initialPlaceholder,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier
        )
        return
    }

    val displayText = if (animatedLength > 0) {
        currentHint.substring(0, animatedLength.coerceAtMost(currentHint.length))
    } else {
        ""
    }

    val cursor = if (showCursor && isTyping) "|" else ""

    Text(
        text = displayText + cursor,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    )
}