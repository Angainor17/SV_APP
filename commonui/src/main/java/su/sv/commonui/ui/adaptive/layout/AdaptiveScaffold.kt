package su.sv.commonui.ui.adaptive.layout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import su.sv.commonui.theme.DeviceFormFactor
import su.sv.commonui.theme.LocalAdaptiveDimensions
import su.sv.commonui.theme.LocalDeviceFormFactor
import su.sv.commonui.ui.adaptive.navigation.AdaptiveNavigation
import su.sv.commonui.ui.adaptive.navigation.NavigationItem

/**
 * Адаптивный Scaffold с поддержкой разных типов навигации
 *
 * - Compact: Scaffold с BottomNavigation
 * - Medium/Expanded: Row с NavigationRail + Scaffold
 *
 * @param items элементы навигации
 * @param selectedItem индекс выбранного элемента
 * @param onItemSelected callback при выборе элемента
 * @param topBar верхняя панель (опционально)
 * @param content основной контент
 * @param modifier модификатор
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdaptiveScaffold(
    items: List<NavigationItem>,
    selectedItem: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    content: @Composable (padding: androidx.compose.foundation.layout.PaddingValues) -> Unit,
) {
    val formFactor = LocalDeviceFormFactor.current

    when (formFactor) {
        is DeviceFormFactor.Compact -> {
            // Compact: стандартный Scaffold с BottomNavigation
            Scaffold(
                modifier = modifier,
                topBar = topBar,
                bottomBar = {
                    AdaptiveNavigation(
                        items = items,
                        selectedItem = selectedItem,
                        onItemSelected = onItemSelected,
                    )
                },
            ) { paddingValues ->
                content(paddingValues)
            }
        }

        is DeviceFormFactor.Medium,
        is DeviceFormFactor.Expanded -> {
            // Medium/Expanded: Row с NavigationRail + Scaffold
            Row(modifier = modifier.fillMaxSize()) {
                // NavigationRail слева
                AdaptiveNavigation(
                    items = items,
                    selectedItem = selectedItem,
                    onItemSelected = onItemSelected,
                )

                // Scaffold справа
                Scaffold(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    topBar = topBar,
                ) { paddingValues ->
                    // Ограничение ширины контента
                    AdaptiveContentLayout(
                        modifier = Modifier.padding(paddingValues),
                    ) {
                        content(androidx.compose.foundation.layout.PaddingValues())
                    }
                }
            }
        }
    }
}

/**
 * Адаптивный контейнер для ограничения ширины контента
 *
 * Для Medium/Expanded ограничивает ширину контента для лучшей читаемости.
 * Для Compact не ограничивает ширину.
 *
 * @param modifier модификатор
 * @param content контент
 */
@Composable
fun AdaptiveContentLayout(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val formFactor = LocalDeviceFormFactor.current
    val adaptiveDims = LocalAdaptiveDimensions.current

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        val contentModifier = when (formFactor) {
            is DeviceFormFactor.Compact -> Modifier.fillMaxSize()
            is DeviceFormFactor.Medium,
            is DeviceFormFactor.Expanded -> {
                adaptiveDims.contentMaxWidth?.let { maxWidth ->
                    Modifier
                        .fillMaxSize()
                        .widthIn(max = maxWidth)
                } ?: Modifier.fillMaxSize()
            }
        }

        Box(
            modifier = contentModifier,
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
    }
}