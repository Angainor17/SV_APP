package su.sv.qa.domain.usecase

import kotlinx.coroutines.withContext
import su.sv.commonarchitecture.di.module.DispatcherProvider
import su.sv.qa.domain.repository.QaRepository
import javax.inject.Inject

/**
 * Use Case для синхронизации отвеченных вопросов с бэкендом
 */
class SyncAnsweredQuestionsUseCase @Inject constructor(
    private val dispatcherProvider: DispatcherProvider,
    private val repository: QaRepository,
) {

    suspend operator fun invoke(): Result<Unit> {
        return withContext(dispatcherProvider.io) {
            repository.syncAnsweredQuestions()
        }
    }
}
