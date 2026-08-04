package su.sv.qa.domain.usecase

import kotlinx.coroutines.withContext
import su.sv.commonarchitecture.di.module.DispatcherProvider
import su.sv.qa.domain.model.TypoReport
import su.sv.qa.domain.repository.QaRepository
import javax.inject.Inject

/**
 * Use Case для отправки репорта об опечатке
 */
class SubmitTypoReportUseCase @Inject constructor(
    private val dispatcherProvider: DispatcherProvider,
    private val repository: QaRepository,
) {

    suspend operator fun invoke(report: TypoReport): Result<Unit> {
        return withContext(dispatcherProvider.io) {
            repository.submitTypoReport(report)
        }
    }
}
