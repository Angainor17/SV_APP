package su.sv.shpargalki.presentation.root

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.terrakok.modo.stack.LocalStackNavigation
import com.github.terrakok.modo.stack.back
import com.github.terrakok.modo.stack.forward
import su.sv.commonui.theme.LocalAppDimensions
import su.sv.commonui.ui.components.AppToolbarWithBack
import su.sv.commonui.ui.components.FullScreenError
import su.sv.commonui.ui.components.FullScreenLoading
import su.sv.shpargalki.R
import su.sv.shpargalki.domain.model.DialecticsDocument
import su.sv.shpargalki.presentation.ShpargalkiViewModel
import su.sv.shpargalki.presentation.model.ShpargalkiState
import su.sv.shpargalki.presentation.tree.DialecticsTreeScreen
import su.sv.shpargalki.testing.ShpargalkiTestTags

/**
 * Контент экрана «Шпаргалки»: список тем (пока одна — «Диалектика»)
 * с раскрывающимися подпунктами-разделами.
 */
@Composable
fun ShpargalkiRootScreenContent(modifier: Modifier = Modifier) {
    val viewModel: ShpargalkiViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val stackNavigation = LocalStackNavigation.current

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            AppToolbarWithBack(
                title = stringResource(R.string.shpargalki_title),
                onBackClick = { stackNavigation.back() },
            )
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .testTag(ShpargalkiTestTags.ROOT),
        ) {
            when (val current = state) {
                is ShpargalkiState.Loading -> FullScreenLoading()

                is ShpargalkiState.Error -> FullScreenError(onRetry = viewModel::load)

                is ShpargalkiState.Content -> ThemesList(
                    document = current.document,
                    onSectionClick = { sectionKey ->
                        stackNavigation.forward(DialecticsTreeScreen(sectionKey = sectionKey))
                    },
                )
            }
        }
    }
}

/**
 * Список тем. Каждая тема раскрывается в список разделов,
 * выбор раздела открывает древо понятий.
 */
@Composable
private fun ThemesList(
    document: DialecticsDocument,
    onSectionClick: (String) -> Unit,
) {
    val dimensions = LocalAppDimensions.current
    var expanded by remember { mutableStateOf(true) }

    // Порядок разделов берём из данных (по первому появлению в nodes)
    val sections = remember(document) {
        document.nodes.map { it.section }.distinct()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = dimensions.screenPaddingHorizontal)
            .padding(vertical = dimensions.itemSpacingMedium),
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column {
                // Заголовок темы — клик сворачивает/разворачивает подпункты
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expanded = !expanded }
                        .padding(
                            horizontal = dimensions.listItemPaddingHorizontal,
                            vertical = dimensions.listItemPaddingVertical,
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.shpargalki_theme_dialectics),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        imageVector = if (expanded) {
                            Icons.Default.KeyboardArrowUp
                        } else {
                            Icons.Default.KeyboardArrowDown
                        },
                        contentDescription = null,
                    )
                }

                if (expanded) {
                    sections.forEach { sectionKey ->
                        val title = document.sections[sectionKey] ?: sectionKey
                        val subtitle = document.nodes
                            .filter { it.section == sectionKey }
                            .map { it.subsection }
                            .distinct()
                            .joinToString(" · ")

                        SectionRow(
                            title = title,
                            subtitle = subtitle,
                            onClick = { onSectionClick(sectionKey) },
                        )
                    }
                }
            }
        }
    }
}

/**
 * Строка раздела внутри темы.
 */
@Composable
private fun SectionRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    val dimensions = LocalAppDimensions.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(
                horizontal = dimensions.cardContentPaddingHorizontal,
                vertical = dimensions.cardContentPaddingVertical,
            ),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
        )
        if (subtitle.isNotBlank()) {
            Spacer(modifier = Modifier.height(dimensions.itemSpacingSmall))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
