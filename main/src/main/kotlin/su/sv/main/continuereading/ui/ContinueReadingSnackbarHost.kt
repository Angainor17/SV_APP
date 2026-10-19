package su.sv.main.continuereading.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import su.sv.main.continuereading.ContinueReadingState

/**
 * Хост для snackbar "Продолжить чтение" с анимациями.
 * Отображается над NavigationBar.
 *
 * @param state состояние snackbar
 * @param onContinueClick callback при клике на "Продолжить"
 * @param onDismissClick callback при клике на крестик
 * @param modifier модификатор
 */
@Composable
fun ContinueReadingSnackbarHost(
    state: ContinueReadingState,
    onContinueClick: () -> Unit,
    onDismissClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Анимация появления/исчезновения
    AnimatedVisibility(
        visible = state is ContinueReadingState.Visible,
        enter = fadeIn(
            animationSpec = tween(durationMillis = 300)
        ) + slideInVertically(
            animationSpec = tween(durationMillis = 300),
            initialOffsetY = { it } // Слайд снизу вверх
        ),
        exit = fadeOut(
            animationSpec = tween(durationMillis = 200)
        ) + slideOutVertically(
            animationSpec = tween(durationMillis = 200),
            targetOffsetY = { it } // Слайд сверху вниз
        ),
        modifier = modifier.fillMaxWidth(),
    ) {
        if (state is ContinueReadingState.Visible) {
            ContinueReadingSnackbar(
                bookInfo = state.bookInfo,
                onContinueClick = onContinueClick,
                onDismissClick = onDismissClick,
            )
        }
    }
}