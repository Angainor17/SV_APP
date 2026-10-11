package su.sv.shpargalki.domain.usecase

import kotlinx.coroutines.withContext
import su.sv.commonarchitecture.di.module.DispatcherProvider
import su.sv.shpargalki.data.source.ShpargalkiDataSource
import su.sv.shpargalki.domain.model.DialecticsDocument
import javax.inject.Inject

/**
 * Получить полный документ диалектики (все понятия).
 * Main-safe: чтение выполняется на IO dispatcher.
 */
class GetDialecticsUseCase @Inject constructor(
    private val dispatcherProvider: DispatcherProvider,
    private val dataSource: ShpargalkiDataSource,
) {

    suspend fun execute(): Result<DialecticsDocument> =
        withContext(dispatcherProvider.io) {
            dataSource.getDialectics()
        }
}
