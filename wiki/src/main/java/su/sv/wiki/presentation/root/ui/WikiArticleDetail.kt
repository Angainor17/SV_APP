package su.sv.wiki.presentation.root.ui

import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import su.sv.wiki.presentation.root.model.UiWikiArticle

/**
 * Компонент для отображения статьи в detail панели master-detail layout
 *
 * @param article данные статьи
 * @param isFavorite признак избранного
 * @param onLinkClick обработчик клика на внутреннюю ссылку
 * @param onExternalLinkClick обработчик клика на внешнюю ссылку
 * @param onFavoriteClick обработчик клика на избранное
 * @param modifier модификатор
 */
@Composable
fun WikiArticleDetail(
    article: UiWikiArticle,
    isFavorite: Boolean,
    onLinkClick: (String) -> Unit,
    onExternalLinkClick: (String) -> Unit,
    onFavoriteClick: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        ArticleView(
            article = article,
            isFavorite = isFavorite,
            onLinkClick = onLinkClick,
            onExternalLinkClick = { url ->
                val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                context.startActivity(intent)
            },
            onFavoriteClick = onFavoriteClick,
        )
    }
}