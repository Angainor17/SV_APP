package su.sv.info.rootinfo.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import su.sv.commonui.theme.AppDimensions
import su.sv.commonui.theme.LocalAppDimensions
import su.sv.info.R
import su.sv.info.rootinfo.model.UiInfoState
import su.sv.info.rootinfo.viewmodel.RootInfoActions
import su.sv.info.rootinfo.viewmodel.RootInfoActionsHandler
import su.sv.info.testing.InfoTestTags

/**
 * Контент информационного экрана
 *
 * @param state состояние с данными
 * @param actionsHandler обработчик действий
 * @param contentPadding отступы от Scaffold
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoContent(
    state: UiInfoState.Content,
    actionsHandler: RootInfoActionsHandler,
    contentPadding: PaddingValues,
) {
    val pullToRefreshState = rememberPullToRefreshState()
    val dimensions = LocalAppDimensions.current
    val appVersion = rememberAppVersion()

    // PullToRefreshBox должен учитывать contentPadding, чтобы индикатор не уходил под тулбар
    PullToRefreshBox(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        isRefreshing = state.isRefreshing,
        onRefresh = {
            actionsHandler.onAction(RootInfoActions.OnSwipeRefresh)
        },
        state = pullToRefreshState,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag(InfoTestTags.LINKS_LIST),
                verticalArrangement = Arrangement.spacedBy(dimensions.listItemSpacing),
            ) {
                items(
                    items = state.items,
                    key = { it.url }
                ) { item ->
                    InfoItem(item)
                }
            }

            // Версия приложения в фиксированном футере (не внутри списка)
            AppVersionText(
                version = appVersion,
                dimensions = dimensions,
            )
        }
    }
}

/**
 * Отображает версию приложения.
 */
@Composable
private fun AppVersionText(
    version: String,
    dimensions: AppDimensions,
) {
    Text(
        text = stringResource(R.string.info_app_version, version),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimensions.screenPaddingHorizontal)
            .padding(vertical = dimensions.itemSpacingMedium)
            .testTag(InfoTestTags.VERSION),
    )
}

/**
 * Получает имя версии приложения через PackageManager.
 */
@Composable
private fun rememberAppVersion(): String {
    val context = LocalContext.current
    return remember {
        try {
            context.packageManager
                .getPackageInfo(context.packageName, 0)
                .versionName ?: "unknown"
        } catch (_: Exception) {
            "unknown"
        }
    }
}
