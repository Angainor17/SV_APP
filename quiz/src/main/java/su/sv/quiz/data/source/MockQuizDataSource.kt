package su.sv.quiz.data.source

import kotlinx.coroutines.delay
import su.sv.quiz.domain.model.Quiz
import su.sv.quiz.domain.model.QuizAnswer
import su.sv.quiz.domain.model.QuizQuestion
import su.sv.quiz.domain.model.QuizTheme
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Мок-источник данных квизов.
 *
 * Отдаёт захардкоженный набор данных с небольшой искусственной задержкой,
 * чтобы эмулировать сетевой запрос. Позже будет заменён на [ApiQuizDataSource].
 */
@Singleton
class MockQuizDataSource @Inject constructor() : QuizDataSource {

    private val now = System.currentTimeMillis()

    override suspend fun getThemes(): List<QuizTheme> {
        delay(MOCK_DELAY_MS)
        return themes
    }

    override suspend fun getQuizzes(): List<Quiz> {
        delay(MOCK_DELAY_MS)
        return quizzes
    }

    override suspend fun getQuiz(quizId: String): Quiz? {
        delay(MOCK_DELAY_MS)
        return quizzes.firstOrNull { it.id == quizId }
    }

    // ============================================================
    // Мок-данные
    // ============================================================

    private val themes = listOf(
        QuizTheme(id = "marxism", name = "Марксизм-ленинизм", colorHex = "#B71C1C"),
        QuizTheme(id = "history", name = "История", colorHex = "#1565C0"),
    )

    private val professionsQuiz = Quiz(
        id = "professions",
        title = "Кто есть кто по профессии",
        description = "Определите категорию работника по его профессии.",
        themeId = "marxism",
        createdAtEpochMillis = daysAgo(3),
        questions = listOf(
            QuizQuestion(
                id = "professions_q1",
                text = "Кто такой «пролетарий»?",
                answers = listOf(
                    QuizAnswer("a1", "Владелец средств производства"),
                    QuizAnswer("a2", "Наёмный работник, лишённый средств производства", isCorrect = true),
                    QuizAnswer("a3", "Работник государственного аппарата"),
                    QuizAnswer("a4", "Интеллигент"),
                ),
                explanation = "Пролетарий — наёмный работник, лишённый средств производства и вынужденный продавать свою рабочую силу.",
            ),
            QuizQuestion(
                id = "professions_q2",
                text = "К какой категории относится рабочий у станка на заводе?",
                answers = listOf(
                    QuizAnswer("a1", "Пролетарий (рабочий)", isCorrect = true),
                    QuizAnswer("a2", "Служащий"),
                    QuizAnswer("a3", "Буржуазия"),
                    QuizAnswer("a4", "Интеллигент"),
                ),
                explanation = "Рабочий у станка занят материально-производительным трудом — это пролетарий.",
            ),
            QuizQuestion(
                id = "professions_q3",
                text = "Кто такой «интеллигент» с точки зрения классового анализа?",
                answers = listOf(
                    QuizAnswer("a1", "Самостоятельный класс"),
                    QuizAnswer("a2", "Работник преимущественно умственного труда (слой)", isCorrect = true),
                    QuizAnswer("a3", "Владелец капитала"),
                    QuizAnswer("a4", "Наёмный работник физического труда"),
                ),
                explanation = "Интеллигенция — слой работников преимущественно умственного труда, а не самостоятельный класс.",
            ),
            QuizQuestion(
                id = "professions_q4",
                text = "К какой категории относится владелец фабрики, живущий на прибыли?",
                answers = listOf(
                    QuizAnswer("a1", "Мелкий собственник"),
                    QuizAnswer("a2", "Служащий"),
                    QuizAnswer("a3", "Буржуазия (капиталист)", isCorrect = true),
                    QuizAnswer("a4", "Пролетарий"),
                ),
                explanation = "Владелец средств производства, живущий на прибыли, — буржуазия (капиталист).",
            ),
        ),
    )

    private val quotesQuiz = Quiz(
        id = "quotes",
        title = "Цитаты классиков",
        description = "Кто сказал и в какой работе.",
        themeId = "marxism",
        createdAtEpochMillis = daysAgo(10),
        questions = listOf(
            QuizQuestion(
                id = "quotes_q1",
                text = "Кому принадлежит тезис «Бытие определяет сознание»?",
                answers = listOf(
                    QuizAnswer("a1", "В. И. Ленин"),
                    QuizAnswer("a2", "К. Маркс", isCorrect = true),
                    QuizAnswer("a3", "Ф. Энгельс"),
                    QuizAnswer("a4", "И. В. Сталин"),
                ),
                explanation = "Положение о первичности бытия по отношению к сознанию — основа материалистического понимания истории Маркса.",
            ),
            QuizQuestion(
                id = "quotes_q2",
                text = "«Пролетарии всех стран, соединяйтесь!» — финальный призыв какой работы?",
                answers = listOf(
                    QuizAnswer("a1", "«Манифест Коммунистической партии»", isCorrect = true),
                    QuizAnswer("a2", "«Капитал»"),
                    QuizAnswer("a3", "«Государство и революция»"),
                    QuizAnswer("a4", "«Материализм и эмпириокритицизм»"),
                ),
                explanation = "Этот призыв завершает «Манифест Коммунистической партии» Маркса и Энгельса.",
            ),
            QuizQuestion(
                id = "quotes_q3",
                text = "Автор работы «Государство и революция»:",
                answers = listOf(
                    QuizAnswer("a1", "К. Маркс"),
                    QuizAnswer("a2", "Ф. Энгельс"),
                    QuizAnswer("a3", "В. И. Ленин", isCorrect = true),
                    QuizAnswer("a4", "Г. В. Плеханов"),
                ),
                explanation = "«Государство и революция» написана В. И. Лениным в 1917 году.",
            ),
        ),
    )

    private val dialecticsQuiz = Quiz(
        id = "dialectics",
        title = "Диалектика Гегеля",
        description = "Категории «Науки логики».",
        themeId = "marxism",
        createdAtEpochMillis = daysAgo(20),
        questions = listOf(
            QuizQuestion(
                id = "dialectics_q1",
                text = "С какой категории начинается «Наука логики» Гегеля?",
                answers = listOf(
                    QuizAnswer("a1", "Бытие", isCorrect = true),
                    QuizAnswer("a2", "Сущность"),
                    QuizAnswer("a3", "Понятие"),
                    QuizAnswer("a4", "Ничто"),
                ),
                explanation = "Логика Гегеля начинается с категории «бытие» как исходной, ещё бессодержательной категории.",
            ),
            QuizQuestion(
                id = "dialectics_q2",
                text = "Сколько основных разделов в «Науке логики»?",
                answers = listOf(
                    QuizAnswer("a1", "Два"),
                    QuizAnswer("a2", "Три", isCorrect = true),
                    QuizAnswer("a3", "Четыре"),
                    QuizAnswer("a4", "Пять"),
                ),
                explanation = "Три раздела: учение о бытии, учение о сущности и учение о понятии.",
            ),
        ),
    )

    private val historyDatesQuiz = Quiz(
        id = "history_dates",
        title = "История России: даты",
        description = "Проверьте знание ключевых дат.",
        themeId = "history",
        createdAtEpochMillis = daysAgo(2),
        questions = listOf(
            QuizQuestion(
                id = "history_dates_q1",
                text = "В каком году был основан Санкт-Петербург?",
                answers = listOf(
                    QuizAnswer("a1", "1703", isCorrect = true),
                    QuizAnswer("a2", "1712"),
                    QuizAnswer("a3", "1721"),
                    QuizAnswer("a4", "1612"),
                ),
                explanation = "Санкт-Петербург основан 27 мая 1703 года Петром I.",
            ),
            QuizQuestion(
                id = "history_dates_q2",
                text = "В каком году произошла Октябрьская революция?",
                answers = listOf(
                    QuizAnswer("a1", "1905"),
                    QuizAnswer("a2", "1914"),
                    QuizAnswer("a3", "1917", isCorrect = true),
                    QuizAnswer("a4", "1921"),
                ),
                explanation = "Октябрьская революция произошла в 1917 году.",
            ),
            QuizQuestion(
                id = "history_dates_q3",
                text = "В каком году был образован СССР?",
                answers = listOf(
                    QuizAnswer("a1", "1917"),
                    QuizAnswer("a2", "1922", isCorrect = true),
                    QuizAnswer("a3", "1924"),
                    QuizAnswer("a4", "1936"),
                ),
                explanation = "СССР был образован 30 декабря 1922 года.",
            ),
        ),
    )

    private val warQuiz = Quiz(
        id = "war",
        title = "Великая Отечественная война",
        description = "Основные события 1941–1945 годов.",
        themeId = "history",
        createdAtEpochMillis = daysAgo(30),
        questions = listOf(
            QuizQuestion(
                id = "war_q1",
                text = "В каком году началась Великая Отечественная война?",
                answers = listOf(
                    QuizAnswer("a1", "1939"),
                    QuizAnswer("a2", "1940"),
                    QuizAnswer("a3", "1941", isCorrect = true),
                    QuizAnswer("a4", "1942"),
                ),
                explanation = "Великая Отечественная война началась 22 июня 1941 года.",
            ),
            QuizQuestion(
                id = "war_q2",
                text = "Какая битва считается коренным переломом в войне?",
                answers = listOf(
                    QuizAnswer("a1", "Московская"),
                    QuizAnswer("a2", "Сталинградская", isCorrect = true),
                    QuizAnswer("a3", "Курская"),
                    QuizAnswer("a4", "Берлинская"),
                ),
                explanation = "Сталинградская битва (1942–1943) считается началом коренного перелома.",
            ),
        ),
    )

    // Собираем список после объявления отдельных квизов, чтобы
    // ссылки на них не приводили к «Variable must be initialized».
    private val quizzes: List<Quiz> = listOf(
        professionsQuiz,
        quotesQuiz,
        dialecticsQuiz,
        historyDatesQuiz,
        warQuiz,
    )

    private fun daysAgo(days: Int): Long = now - days * MILLIS_PER_DAY

    private companion object {
        const val MOCK_DELAY_MS = 600L
        const val MILLIS_PER_DAY = 24L * 60 * 60 * 1000
    }
}
