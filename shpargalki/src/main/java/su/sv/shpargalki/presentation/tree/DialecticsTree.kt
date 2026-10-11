package su.sv.shpargalki.presentation.tree

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
 * Вертикальное древо понятий: цепочка, где каждое следующее понятие
 * проистекает из предыдущего (стрелка вниз по «стволу»).
 */
@Composable
fun DialecticsTree(
    groups: List<SubsectionGroup>,
    onNodeClick: (DialecticsNode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimensions = LocalAppDimensions.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = dimensions.screenPaddingHorizontal)
            .padding(bottom = dimensions.itemSpacingXLarge),
    ) {
        groups.forEach { group ->
            Text(
                text = group.title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(
                    top = dimensions.itemSpacingLarge,
                    bottom = dimensions.itemSpacingSmall,
                ),
            )

            group.nodes.forEachIndexed { index, node ->
                TreeNodeRow(
                    node = node,
                    isLast = index == group.nodes.lastIndex,
                    onClick = { onNodeClick(node) },
                )
            }
        }
    }
}

/**
 * Одна вершина древа: точка на «стволе» + карточка с понятием.
 */
@Composable
private fun TreeNodeRow(
    node: DialecticsNode,
    isLast: Boolean,
    onClick: () -> Unit,
) {
    val dimensions = LocalAppDimensions.current

    Row(modifier = Modifier.fillMaxWidth()) {
        // «Ствол»: точка, соединительная линия и стрелка к следующему понятию
        Column(
            modifier = Modifier.width(dimensions.iconSizeXLarge),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
            )
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(dimensions.itemSpacingLarge)
                        .background(MaterialTheme.colorScheme.outlineVariant),
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(dimensions.iconSizeSmall),
                )
            }
        }

        // Карточка понятия
        Card(
            modifier = Modifier
                .weight(1f)
                .padding(
                    start = dimensions.itemSpacingMedium,
                    bottom = dimensions.itemSpacingMedium,
                )
                .clickable(onClick = onClick),
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
