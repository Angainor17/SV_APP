package su.sv.shpargalki.presentation.tree

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import su.sv.commonui.theme.LocalAppDimensions
import su.sv.shpargalki.domain.model.DialecticsNode

/**
 * Подраздел древа: список понятий в порядке выведения.
 */
data class SubsectionGroup(
    val title: String,
    val nodes: List<DialecticsNode>,
)

/**
 * Группирует понятия по подразделам, сохраняя исходный порядок.
 */
internal fun groupBySubsection(nodes: List<DialecticsNode>): List<SubsectionGroup> {
    val result = mutableListOf<SubsectionGroup>()
    var current: MutableList<DialecticsNode>? = null
    var currentTitle: String? = null

    for (node in nodes.sortedBy { it.order }) {
        if (currentTitle != node.subsection) {
            val newList = mutableListOf<DialecticsNode>()
            current = newList
            currentTitle = node.subsection
            result.add(SubsectionGroup(title = node.subsection, nodes = newList))
        }
        current?.add(node)
    }
    return result
}

/**
 * Вертикальное древо понятий. Слева рисуется сплошной «ствол», от которого
 * к каждой карточке идёт короткий горизонтальный отросток. У узлов с двумя
 * родителями (слияние «2→1») и двумя детьми (ветвление «1→2») на стволе
 * рисуется дополнительная линия-вилка.
 */
@Composable
fun DialecticsTree(
    groups: List<SubsectionGroup>,
    onNodeClick: (DialecticsNode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimensions = LocalAppDimensions.current

    // Сколько детей у каждого узла — нужно для отрисовки ветвлений.
    val childrenByParent = remember(groups) {
        val counts = HashMap<String, Int>()
        groups.forEach { group ->
            group.nodes.forEach { node ->
                node.parents.forEach { parentId ->
                    counts[parentId] = (counts[parentId] ?: 0) + 1
                }
            }
        }
        counts
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = dimensions.screenPaddingHorizontal)
            .padding(bottom = dimensions.itemSpacingXLarge),
    ) {
        groups.forEach { group ->
            // Подраздел — отдельный блок с обводкой: «глава» древа.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = dimensions.itemSpacingLarge)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceContainerLowest,
                        shape = MaterialTheme.shapes.medium,
                    )
                    .border(
                        width = dimensions.borderWidthStandard,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        shape = MaterialTheme.shapes.medium,
                    )
                    // Небольшой отступ: контент подраздела чуть «вдвинут» внутрь панели
                    .padding(
                        horizontal = dimensions.itemSpacingLarge,
                        vertical = dimensions.cardPaddingInner,
                    ),
            ) {
                Text(
                    text = group.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = dimensions.itemSpacingMedium),
                )

                group.nodes.forEachIndexed { index, node ->
                    TreeNodeRow(
                        node = node,
                        isFirst = index == 0,
                        isLast = index == group.nodes.lastIndex,
                        childCount = childrenByParent[node.id] ?: 0,
                        onClick = { onNodeClick(node) },
                    )
                }
            }
        }
    }
}

/**
 * Одна вершина древа: слева — «ствол» с отростком, справа — карточка понятия.
 */
@Composable
private fun TreeNodeRow(
    node: DialecticsNode,
    isFirst: Boolean,
    isLast: Boolean,
    childCount: Int,
    onClick: () -> Unit,
) {
    val dimensions = LocalAppDimensions.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Max),
    ) {
        TrunkConnector(
            isFirst = isFirst,
            isLast = isLast,
            parentCount = node.parents.size,
            childCount = childCount,
            modifier = Modifier
                .width(TrunkWidth)
                .fillMaxHeight(),
        )

        // Карточка понятия (clickable Card — рипл обрезается по скруглению карточки)
        Card(
            onClick = onClick,
            modifier = Modifier
                .weight(1f)
                .padding(bottom = dimensions.itemSpacingSmall),
        ) {
            Column(modifier = Modifier.padding(dimensions.cardPaddingInner)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${node.order}.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.width(dimensions.itemSpacingSmall))
                    Text(
                        text = node.term,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                    )
                }

                if (node.german.isNotBlank()) {
                    Spacer(modifier = Modifier.height(dimensions.itemSpacingSmall))
                    Text(
                        text = node.german,
                        style = MaterialTheme.typography.bodySmall,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (node.derivation.isNotBlank()) {
                    Spacer(modifier = Modifier.height(dimensions.itemSpacingSmall))
                    Text(
                        text = node.derivation,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/**
 * «Ствол» дерева: вертикальная линия слева и горизонтальная стрелка
 * к карточке, показывающая зависимость «данное понятие выведено из ствола».
 *
 * Цвет стрелки зависит от контекста зависимости:
 *  - [transitionColor] (`outline`) — обычный линейный переход (1 родитель);
 *  - [synthesisColor] (`primary`) — триадический синтез / слияние «2→1»;
 *  - [branchColor] (`tertiary`) — ветвление «1→2» (у тезиса два ребёнка).
 */
@Composable
private fun TrunkConnector(
    isFirst: Boolean,
    isLast: Boolean,
    parentCount: Int,
    childCount: Int,
    modifier: Modifier = Modifier,
) {
    val spineColor = MaterialTheme.colorScheme.outlineVariant
    val transitionColor = MaterialTheme.colorScheme.outline
    val synthesisColor = MaterialTheme.colorScheme.primary
    val branchColor = MaterialTheme.colorScheme.tertiary

    Canvas(modifier = modifier) {
        val stroke = TrunkStroke.toPx()
        val spineX = TrunkSpineX.toPx()
        val secondX = TrunkSecondX.toPx()
        val stubY = TrunkStubOffset.toPx()
        val dotRadius = TrunkDotRadius.toPx()
        val arrowLen = TrunkArrowLen.toPx()
        val arrowHalf = TrunkArrowHalf.toPx()

        // Вертикальный ствол: сплошная нейтральная линия от первого до последнего узла.
        val top = if (isFirst) stubY else 0f
        val bottom = if (isLast) stubY else size.height
        drawLine(
            color = spineColor,
            start = Offset(spineX, top),
            end = Offset(spineX, bottom),
            strokeWidth = stroke,
        )

        // Стрелка-зависимость в карточку: цвет по контексту.
        val arrowColor = if (parentCount > 1) synthesisColor else transitionColor
        val tipX = size.width
        val baseX = tipX - arrowLen
        drawLine(
            color = arrowColor,
            start = Offset(spineX, stubY),
            end = Offset(baseX, stubY),
            strokeWidth = stroke,
        )
        drawPath(
            path = Path().apply {
                moveTo(tipX, stubY)
                lineTo(baseX, stubY - arrowHalf)
                lineTo(baseX, stubY + arrowHalf)
                close()
            },
            color = arrowColor,
        )

        // Слияние «2→1»: вторая линия приходит сверху (синтез).
        if (parentCount > 1) {
            drawLine(
                color = synthesisColor,
                start = Offset(secondX, 0f),
                end = Offset(secondX, stubY),
                strokeWidth = stroke,
            )
            drawLine(
                color = synthesisColor,
                start = Offset(secondX, stubY),
                end = Offset(spineX, stubY),
                strokeWidth = stroke,
            )
        }

        // Ветвление «1→2»: вторая линия уходит вниз.
        if (childCount > 1) {
            drawLine(
                color = branchColor,
                start = Offset(secondX, stubY),
                end = Offset(secondX, size.height),
                strokeWidth = stroke,
            )
            drawLine(
                color = branchColor,
                start = Offset(secondX, stubY),
                end = Offset(spineX, stubY),
                strokeWidth = stroke,
            )
        }

        // Узел на стволе — цветом стрелки.
        drawCircle(
            color = arrowColor,
            radius = dotRadius,
            center = Offset(spineX, stubY),
        )
    }
}

private val TrunkWidth = 32.dp
private val TrunkSpineX = 18.dp
private val TrunkSecondX = 5.dp
private val TrunkStubOffset = 28.dp
private val TrunkStroke = 2.dp
private val TrunkDotRadius = 3.dp
private val TrunkArrowLen = 7.dp
private val TrunkArrowHalf = 4.dp
