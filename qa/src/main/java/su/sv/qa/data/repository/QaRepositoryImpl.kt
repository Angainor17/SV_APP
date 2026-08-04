package su.sv.qa.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import su.sv.commonarchitecture.data.runCatchingHttpRequest
import su.sv.qa.data.api.QaApi
import su.sv.qa.data.api.model.ApiAnsweredQuestion
import su.sv.qa.data.api.model.ApiQuestionReportRequest
import su.sv.qa.data.api.model.ApiTypoReportRequest
import su.sv.qa.data.local.dao.AnsweredQuestionDao
import su.sv.qa.data.local.entity.AnsweredQuestionEntity
import su.sv.qa.domain.model.AnsweredQuestion
import su.sv.qa.domain.model.QuestionReport
import su.sv.qa.domain.model.TypoReport
import su.sv.qa.domain.repository.QaRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QaRepositoryImpl @Inject constructor(
    private val api: QaApi,
    private val dao: AnsweredQuestionDao,
) : QaRepository {

    override suspend fun submitTypoReport(report: TypoReport): Result<Unit> {
        return runCatchingHttpRequest {
            api.submitTypoReport(
                ApiTypoReportRequest(
                    bookId = report.catalogBookId,
                    bookTitle = report.bookTitle,
                    page = report.page,
                    selectedText = report.selectedText,
                    comment = report.comment,
                )
            )
        }
    }

    override suspend fun submitQuestionReport(report: QuestionReport): Result<Unit> {
        return runCatchingHttpRequest {
            api.submitQuestionReport(
                ApiQuestionReportRequest(
                    bookId = report.catalogBookId,
                    bookTitle = report.bookTitle,
                    page = report.page,
                    selectedText = report.selectedText,
                    authorName = report.authorName,
                )
            )
        }
    }

    override suspend fun syncAnsweredQuestions(): Result<Unit> {
        return runCatchingHttpRequest {
            val since = dao.getMaxAnswerUpdatedAt() ?: 0L
            val questions = api.getAnsweredQuestions(since = since)
            dao.upsertAll(questions.map { it.toEntity() })
        }
    }

    override fun observeAnsweredQuestions(): Flow<List<AnsweredQuestion>> {
        return dao.getAll().map { entities -> entities.map { it.toDomain() } }
    }

    override fun observeAnsweredQuestionsForBook(catalogBookId: String): Flow<List<AnsweredQuestion>> {
        return dao.getForBook(catalogBookId).map { entities -> entities.map { it.toDomain() } }
    }

    private fun ApiAnsweredQuestion.toEntity(): AnsweredQuestionEntity {
        return AnsweredQuestionEntity(
            id = id,
            bookId = bookId,
            bookTitle = bookTitle,
            page = page,
            selectedText = selectedText,
            authorName = authorName,
            questionCreatedAt = questionCreatedAt,
            answerText = answerText,
            answerUpdatedAt = answerUpdatedAt,
            startParagraph = startParagraph,
            startElement = startElement,
            startChar = startChar,
            endParagraph = endParagraph,
            endElement = endElement,
            endChar = endChar,
        )
    }

    private fun AnsweredQuestionEntity.toDomain(): AnsweredQuestion {
        return AnsweredQuestion(
            id = id,
            catalogBookId = bookId,
            bookTitle = bookTitle,
            page = page,
            selectedText = selectedText,
            authorName = authorName,
            questionCreatedAt = questionCreatedAt,
            answerText = answerText,
            answerUpdatedAt = answerUpdatedAt,
            startParagraph = startParagraph,
            startElement = startElement,
            startChar = startChar,
            endParagraph = endParagraph,
            endElement = endElement,
            endChar = endChar,
        )
    }
}
