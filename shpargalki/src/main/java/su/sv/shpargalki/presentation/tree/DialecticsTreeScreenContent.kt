package su.sv.shpargalki.presentation.tree

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.terrakok.modo.stack.LocalStackNavigation
import com.github.terrakok.modo.stack.back
import su.sv.commonui.ui.components.AppToolbarWithBack
import su.sv.commonui.ui.components.FullScreenError
import su.sv.commonui.ui.components.FullScreenLoading
import su.sv.shpargalki.R
import su.sv.shpargalki.domain.model.DialecticsDocument
import su.sv.shpargalki.domain.model.DialecticsNode
import su.sv.shpargalki.presentation.ShpargalkiViewModel
import su.sv.shpargalki.presentation.model.ShpargalkiState
import su.sv.shpargalki.presentation.ui.NodeDetailSheet
import su.sv.shpargalki.testing.ShpargalkiTestTags

/**
 * Контент экрана древа: вертикальная цепочка понятий раздела,
 * сгруппированная по подразделам. Клик по понятию открывает шторку.
 */
@Composable
fun DialecticsTreeScreenContent(
    sectionKey: String,
    modifier: Modifier = Modifier,
) {
    val viewModel: ShpargalkiViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val stackNavigation = LocalStackNavigation.current
    var selectedNode by remember { mutableStateOf<DialecticsNode?>(null) }

    val document = (state as? ShpargalkiState.Content)?.document

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            AppToolbarWithBack(
                title = sectionTitle(sectionKey, document),
                onBackClick = { stackNavigation.back() },
            )
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .testTag(ShpargalkiTestTags.TREE),
        ) {
            when (val current = state) {
                is ShpargalkiState.Loading -> FullScreenLoading()

                is ShpargalkiState.Error -> FullScreenError(onRetry = viewModel::load)

                is ShpargalkiState.Content -> {
                    val groups = groupBySubsection(
                        current.document.nodes.filter { it.section == sectionKey }
                    )
                    DialecticsTree(
                        groups = groups,
                        onNodeClick = { selectedNode = it },
                    )
                }
            }
        }
    }

    NodeDetailSheet(
        node = selectedNode,
        onDismiss = { selectedNode = null },
    )
}

/**
 * Русское название раздела: из документа, если он загружен,
 * иначе — из строковых ресурсов по известному ключу.
 */
@Composable
private fun sectionTitle(sectionKey: String, document: DialecticsDocument?): String {
    document?.sections?.get(sectionKey)?.let { return it }
    return when (sectionKey) {
        "bytie" -> stringResource(R.string.shpargalki_section_being)
        "suschnost" -> stringResource(R.string.shpargalki_section_essence)
        "ponyatie" -> stringResource(R.string.shpargalki_section_concept)
        else -> sectionKey
    }
}
