package su.sv.commonui.ui.adaptive.layout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.sv.commonui.R
import su.sv.commonui.theme.DeviceFormFactor
import su.sv.commonui.theme.LocalDeviceFormFactor

/**
 * Master-Detail Layout для двухпанельного отображения
 *
 * Показывает два компонента рядом на больших экранах (Expanded):
 * - Master (слева): список, занимает 35% ширины
 * - Detail (справа): детали, занимает 65% ширины
 *
 * На Compact и Medium показывается только master с навигацией к detail.
 *
 * @param master контент для левой панели (список)
 * @param detail контент для правой панели (детали)
 * @param modifier модификатор
 * @param isDetailVisible признак видимости деталей (для Compact/Medium)
 * @param detailTitle заголовок detail панели (опционально)
 * @param onCloseDetail обработчик закрытия detail (опционально)
 * @param emptyDetailContent контент для пустого состояния detail панели
 */
@Composable
fun MasterDetailLayout(
    master: @Composable () -> Unit,
    detail: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    isDetailVisible: Boolean = true,
    detailTitle: String? = null,
    onCloseDetail: (() -> Unit)? = null,
    emptyDetailContent: @Composable () -> Unit = { DefaultEmptyDetail() },
) {
    val formFactor = LocalDeviceFormFactor.current

    when (formFactor) {
        is DeviceFormFactor.Expanded -> {
            // Two-pane layout для планшетов ландшафт
            Row(modifier = modifier.fillMaxSize()) {
                // Master панель (слева) - 35%
                Box(
                    modifier = Modifier
                        .weight(0.35f)
                        .fillMaxHeight(),
                ) {
                    master()
                }

                // Detail панель (справа) - 65%
                Box(
                    modifier = Modifier
                        .weight(0.65f)
                        .fillMaxHeight(),
                ) {
                    if (isDetailVisible) {
                        detail()
                    } else {
                        emptyDetailContent()
                    }
                }
            }
        }

        is DeviceFormFactor.Compact,
        is DeviceFormFactor.Medium -> {
            // Single pane для телефонов и планшетов портрет
            // Если есть detailTitle и onCloseDetail - показываем detail
            // иначе - master
            if (isDetailVisible && detailTitle != null && onCloseDetail != null) {
                // Показываем detail с возможностью вернуться
                Column(modifier = modifier.fillMaxSize()) {
                    // Простой TopBar для навигации назад
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        tonalElevation = 2.dp,
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconButton(onClick = onCloseDetail) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.common_navigate_back),
                                )
                            }
                            Text(
                                text = detailTitle,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 48.dp), // Баланс для кнопки
                            )
                        }
                    }
                    // Detail контент
                    Box(modifier = Modifier.fillMaxSize()) {
                        detail()
                    }
                }
            } else {
                // Показываем master
                Box(modifier = modifier.fillMaxSize()) {
                    master()
                }
            }
        }
    }
}

/**
 * Дефолтный пустой контент для detail панели
 */
@Composable
private fun DefaultEmptyDetail() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Выберите элемент для просмотра",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Режим отображения для Master-Detail
 */
enum class MasterDetailMode {
    /** Две панели рядом (Expanded) */
    TWO_PANE,

    /** Одна панель - master */
    MASTER_ONLY,

    /** Одна панель - detail */
    DETAIL_ONLY,
}

/**
 * Определить режим Master-Detail на основе форм-фактора
 */
@Composable
fun rememberMasterDetailMode(): MasterDetailMode {
    val formFactor = LocalDeviceFormFactor.current

    return remember(formFactor) {
        when (formFactor) {
            is DeviceFormFactor.Expanded -> MasterDetailMode.TWO_PANE
            is DeviceFormFactor.Compact,
            is DeviceFormFactor.Medium -> MasterDetailMode.MASTER_ONLY
        }
    }
}

/**
 * Проверка, нужно ли использовать двухпанельный layout
 */
@Composable
fun isTwoPaneLayout(): Boolean {
    val formFactor = LocalDeviceFormFactor.current
    return formFactor.isExpanded()
}