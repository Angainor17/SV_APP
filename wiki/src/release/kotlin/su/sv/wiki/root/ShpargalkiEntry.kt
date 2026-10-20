package su.sv.wiki.root

import androidx.compose.runtime.Composable

/**
 * В release-сборке «Шпаргалки» недоступны: модуль `:shpargalki` не подключён
 * (`debugImplementation`), поэтому кнопка в строке поиска не отображается.
 */
@Composable
internal fun ShpargalkiEntryAction() = Unit
