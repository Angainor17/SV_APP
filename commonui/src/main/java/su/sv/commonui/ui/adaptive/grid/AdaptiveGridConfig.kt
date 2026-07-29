package su.sv.commonui.ui.adaptive.grid

import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import su.sv.commonui.theme.LocalAdaptiveDimensions

/**
 * Адаптивная конфигурация сетки
 *
 * Предоставляет количество колонок в зависимости от форм-фактора:
 * - Compact: 2 колонки
 * - Medium: 3 колонки
 * - Expanded: 4 колонки
 */

/**
 * Создать GridCells с адаптивным количеством колонок
 *
 * Использование:
 * ```kotlin
 * LazyVerticalGrid(
 *     columns = adaptiveGridCells(),
 *     ...
 * )
 * ```
 */
@Composable
fun adaptiveGridCells(): GridCells {
    val adaptiveDims = LocalAdaptiveDimensions.current
    return GridCells.Fixed(adaptiveDims.gridColumns)
}

/**
 * Получить количество колонок для текущего форм-фактора
 */
@Composable
fun rememberGridColumns(): Int {
    val adaptiveDims = LocalAdaptiveDimensions.current
    return adaptiveDims.gridColumns
}

/**
 * Адаптивная вертикальная сетка
 *
 * Автоматически настраивает количество колонок в зависимости от размера экрана.
 *
 * @param modifier модификатор
 * @param content содержимое сетки
 */
@Composable
fun AdaptiveLazyVerticalGrid(
    modifier: Modifier = Modifier,
    content: LazyGridScope.() -> Unit,
) {
    val gridState = rememberLazyGridState()
    val columns = adaptiveGridCells()

    LazyVerticalGrid(
        columns = columns,
        modifier = modifier,
        state = gridState,
        content = content,
    )
}