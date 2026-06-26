package su.sv.app

import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection.Companion.Left
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection.Companion.Right
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import com.github.terrakok.modo.ComposeRendererScope
import com.github.terrakok.modo.DialogScreen
import com.github.terrakok.modo.ExperimentalModoApi
import com.github.terrakok.modo.SaveableContent
import com.github.terrakok.modo.animation.ScreenTransition
import com.github.terrakok.modo.animation.StackTransitionType
import com.github.terrakok.modo.animation.calculateStackTransitionType
import com.github.terrakok.modo.stack.StackState

/**
 * Длительность анимации перехода в миллисекундах
 */
private const val TRANSITION_DURATION_MS = 350

/**
 * Плавная анимация перехода между экранами с эффектом slide.
 *
 * При переходе вперед (forward/push) - новый экран въезжает справа, старый уезжает влево.
 * При возврате назад (back/pop) - предыдущий экран въезжает слева, текущий уезжает вправо.
 *
 * @param modifier Внешний модификатор для контейнера
 * @param screenModifier Модификатор для каждого экрана
 */
@Composable
@OptIn(ExperimentalModoApi::class)
fun ComposeRendererScope<StackState>.AppScreenTransition(
    modifier: Modifier = Modifier,
    screenModifier: Modifier = Modifier
) {
    val backgroundColor = MaterialTheme.colorScheme.background
    ScreenTransition(
        modifier = modifier.background(backgroundColor),
        screenModifier = screenModifier.background(backgroundColor),
        transitionSpec = {
            val transitionType = calculateStackTransitionType(oldState, newState)

            // Для диалогов и replace используем fade
            if (transitionType == StackTransitionType.Replace ||
                oldState?.stack?.lastOrNull() is DialogScreen ||
                newState?.stack?.lastOrNull() is DialogScreen
            ) {
                val fadeSpec = tween<Float>(durationMillis = TRANSITION_DURATION_MS)
                fadeIn(fadeSpec) togetherWith fadeOut(fadeSpec)
            } else {
                val slideSpec: FiniteAnimationSpec<IntOffset> = tween(durationMillis = TRANSITION_DURATION_MS, easing = FastOutSlowInEasing)
                val fadeSpec = tween<Float>(durationMillis = TRANSITION_DURATION_MS / 2)

                when (transitionType) {
                    // Push - новый экран въезжает справа, старый уезжает влево
                    StackTransitionType.Push -> {
                        slideInHorizontally(initialOffsetX = { it }, animationSpec = slideSpec) +
                            fadeIn(fadeSpec) togetherWith
                            slideOutHorizontally(targetOffsetX = { -it / 3 }, animationSpec = slideSpec) +
                            fadeOut(fadeSpec)
                    }
                    // Pop - предыдущий экран въезжает слева, текущий уезжает вправо
                    StackTransitionType.Pop -> {
                        slideInHorizontally(initialOffsetX = { -it / 3 }, animationSpec = slideSpec) +
                            fadeIn(fadeSpec) togetherWith
                            slideOutHorizontally(targetOffsetX = { it }, animationSpec = slideSpec) +
                            fadeOut(fadeSpec)
                    }
                    else -> {
                        fadeIn(fadeSpec) togetherWith fadeOut(fadeSpec)
                    }
                }
            }
        },
        content = { screen -> screen.SaveableContent(screenModifier, manualResumePause = true) }
    )
}

/**
 * Альтернативная анимация с более выраженным эффектом slide (полный выезд экранов).
 * Используйте, если хотите более классический эффект смены экранов.
 */
@Composable
@OptIn(ExperimentalModoApi::class)
fun ComposeRendererScope<StackState>.AppScreenTransitionFull(
    modifier: Modifier = Modifier,
    screenModifier: Modifier = Modifier
) {
    val backgroundColor = MaterialTheme.colorScheme.background
    ScreenTransition(
        modifier = modifier.background(backgroundColor),
        screenModifier = screenModifier.background(backgroundColor),
        transitionSpec = {
            val transitionType = calculateStackTransitionType(oldState, newState)
            val slideSpec: FiniteAnimationSpec<IntOffset> = tween(durationMillis = TRANSITION_DURATION_MS, easing = FastOutSlowInEasing)
            val fadeSpec = tween<Float>(durationMillis = TRANSITION_DURATION_MS / 2)

            when {
                transitionType == StackTransitionType.Replace ||
                    oldState?.stack?.lastOrNull() is DialogScreen ||
                    newState?.stack?.lastOrNull() is DialogScreen -> {
                    fadeIn(fadeSpec) togetherWith fadeOut(fadeSpec)
                }
                transitionType == StackTransitionType.Push -> {
                    slideIntoContainer(Left, animationSpec = slideSpec) togetherWith
                        slideOutOfContainer(Left, animationSpec = slideSpec)
                }
                transitionType == StackTransitionType.Pop -> {
                    slideIntoContainer(Right, animationSpec = slideSpec) togetherWith
                        slideOutOfContainer(Right, animationSpec = slideSpec)
                }
                else -> {
                    fadeIn(fadeSpec) togetherWith fadeOut(fadeSpec)
                }
            }
        },
        content = { screen -> screen.SaveableContent(screenModifier, manualResumePause = true) }
    )
}

/**
 * Альтернативная анимация только с fade (без slide).
 * Более мягкий и быстрый переход.
 */
@Composable
@OptIn(ExperimentalModoApi::class)
fun ComposeRendererScope<StackState>.AppScreenTransitionFade(
    modifier: Modifier = Modifier,
    screenModifier: Modifier = Modifier
) {
    val backgroundColor = MaterialTheme.colorScheme.background
    ScreenTransition(
        modifier = modifier.background(backgroundColor),
        screenModifier = screenModifier.background(backgroundColor),
        transitionSpec = {
            val fadeSpec = tween<Float>(durationMillis = 250, easing = FastOutSlowInEasing)
            fadeIn(fadeSpec) togetherWith fadeOut(fadeSpec)
        },
        content = { screen -> screen.SaveableContent(screenModifier, manualResumePause = true) }
    )
}