package su.sv.info.rootinfo.model

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable

/**
 * @Immutable - оптимизация Compose recomposition
 */
@Immutable
data class UiLinkItem(

    /** Отображаемый текст */
    val text: String,

    /** Ссылка для перехода */
    val url: String,

    /** логотип ресурса */
    @DrawableRes val logo: Int,
)
