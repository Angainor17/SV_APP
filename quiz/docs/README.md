# Техническое задание: Модуль квизов

**Дата создания:** 2026-07-25
**Статус:** В разработке

---

## 1. Общее описание

### 1.1 Цель
Добавление нового функционального блока - квизы (тесты, опросники) с вопросами по темам. Квизы привязаны к существующей MediaWiki инсталляции svremya.su.

### 1.2 Ключевые решения (из интервью)

| Параметр | Решение |
|----------|---------|
| Тип ответов | Микс: множественный выбор + поле ввода |
| Авторизация | Не требуется, прогресс сохраняется локально |
| Предложение вопроса | Форма в приложении → отправка через Tracer (email) |
| История | Отдельный раздел "Мои квизы" |
| Результаты | Сохраняется лучший результат |
| Изменение ответа | Нельзя, ответ финальный |
| Формат ответов | Только текст |
| Количество вопросов | Зависит от квиза (задаётся при создании) |
| Источник данных | MediaWiki шаблоны |
| Новые квизы | За последние 14 дней |
| Темы | Динамические, создаются редакторами |
| Шаринг результатов | Не нужен |
| Пропуск вопроса | Можно пропустить, но для завершения нужно ответить на все |
| Срок кеша | 14 дней |
| Множественные ответы | Только single choice (один правильный) |
| Сложность | Без уровней сложности |

---

## 2. Экраны приложения

### 2.1 Корневой экран квизов

**Точка входа:** Иконка квиза в тулбаре экрана новостей

#### Элементы экрана:

**Тулбар:**
- Название: "Квизы" или "Тесты"
- Иконка справа: "Предложить свой вопрос" → открывает форму

**Анимация иконки в тулбаре новостей:**
- Интервальная анимация для привлечения внимания
- Варианты: пульсация, покачивание, bounce-эффект
- Периодичность: каждые N секунд при наличии непройденных квизов

**Контент:**

1. **Блок "Новые квизы"** (подзаголовок)
   - Карточки последних новых квизов (максимум 3)
   - Определяются по дате создания
   - Горизонтальный скролл или сетка 1-3 карточки

2. **Блок "Темы"** (подзаголовок)
   - Список тем квизов
   - Каждая тема показывает:
     - Название темы
     - Количество квизов в теме
     - Возможно: иконка/цвет темы

**Карточка квиза:**
```
┌─────────────────────────────┐
│ Название квиза              │
│ 8 вопросов                  │
│ ●●●○○○○○ 3/8 (прогресс)     │
│ или                         │
│ ★ Лучший: 6/8 (75%)         │
└─────────────────────────────┘
```

---

### 2.2 Экран списка квизов по теме

**Тулбар:** Название темы + стрелка назад

**Контент:**
- Список квизов данной темы
- Карточки аналогичны корневому экрану

---

### 2.3 Экран прохождения квиза

**Тулбар:** Название квиза + счётчик "1 / 8"

#### Структура экрана:

```
┌─────────────────────────────────────────┐
│ ← Название квиза            1 / 8      │ ← Тулбар
├─────────────────────────────────────────┤
│                                         │
│     Текст вопроса                       │
│     (возможно с изображением)           │
│                                         │
├─────────────────────────────────────────┤
│                                         │
│   ┌─────────────────────────────────┐   │
│   │ Вариант ответа A                │   │
│   └─────────────────────────────────┘   │
│   ┌─────────────────────────────────┐   │
│   │ Вариант ответа B                │   │ ← Варианты
│   └─────────────────────────────────┘   │   ответов
│   ┌─────────────────────────────────┐   │
│   │ Вариант ответа C                │   │
│   └─────────────────────────────────┘   │
│                                         │
├─────────────────────────────────────────┤
│                                         │
│   ✓ Верно! Пояснение: текст...          │ ← Блок после ответа
│   или                                   │
│   ✗ Неверно. Правильный ответ: A.       │
│     Пояснение: текст...                 │
│                                         │
├─────────────────────────────────────────┤
│                                         │
│   ← Назад          Вопрос 1 из 8   Вперёд → │ ← Навигация
│                                         │
├─────────────────────────────────────────┤
│   ● ● ○ ○ ○ ○ ○ ○                       │ ← Индикатор прогресса
└─────────────────────────────────────────┘
```

#### Поведение при ответе:

**Верный ответ:**
1. Фон карточки ответа → зелёный
2. Анимация успеха (салют/конфетти)
3. Появляется блок с пояснением
4. Можно перейти к следующему вопросу

**Неверный ответ:**
1. Фон карточки ответа → красный
2. Короткая вибрация
3. Показать правильный ответ (подсветить зелёным)
4. Появляется блок с пояснением
5. Можно перейти к следующему вопросу

#### Кнопки навигации:
- **"Назад"** - неактивна на первом вопросе
- **"Вперёд"** - неактивна на последнем вопросе (если не все отвечены)
- На последнем вопросе, когда все отвечены → кнопка "Результат"

#### Индикатор прогресса (внизу):
- Зелёный кружок - верный ответ
- Красный кружок - неверный ответ
- Серый кружок - без ответа
- Кликабельный, можно перейти к конкретному вопросу

---

### 2.4 Экран результата

**Отображается после ответа на все вопросы**

```
┌─────────────────────────────────────────┐
│                                         │
│         🎉 Ваш результат!               │
│                                         │
│            6 из 8                       │
│            75%                          │
│                                         │
│   ● ● ● ● ● ● ○ ○                       │
│                                         │
│   ┌─────────────────────────────────┐   │
│   │   Пройти заново                 │   │
│   └─────────────────────────────────┘   │
│   ┌─────────────────────────────────┐   │
│   │   К списку квизов               │   │
│   └─────────────────────────────────┘   │
│                                         │
└─────────────────────────────────────────┘
```

---

### 2.5 Экран "Мои квизы" (история)

**Тулбар:** "Мои квизы" + стрелка назад

**Контент:**
- Список пройденных квизов
- Карточка показывает:
  - Название квиза
  - Лучший результат (N/M, %)
  - Тема (если есть)
  - Дата последнего прохождения

---

### 2.6 Форма предложения вопроса

**Тулбар:** "Предложить вопрос" + крестик закрытия

**Поля формы:**
1. Тема вопроса (выпадающий список существующих тем)
2. Текст вопроса (многострочное поле)
3. Варианты ответов (минимум 2, можно добавить до 6)
4. Правильный ответ (выбор одного из вариантов)
5. Пояснение к ответу (опционально)

**Кнопки:**
- Отправить
- Отмена

**Отправка:**
- Переиспользовать механизм из `bugreport` модуля
- Отправка через Tracer API: `TracerCrashReport.report(e, issueKey = "quiz_question_suggestion")`
- Опционально: также отправить на email `angainor17@gmail.com`
- Формат данных: JSON с полями формы

**Связанный файл:** `bugreport/presentation/bugreport/viewmodel/BugReportViewModel.kt`

---

## 3. Модели данных

### 3.1 Domain модели

```kotlin
// Квиз
data class Quiz(
    val id: String,                    // Уникальный ID
    val title: String,                 // Название
    val description: String? = null,   // Описание (опционально)
    val theme: QuizTheme?,             // Тема (опционально)
    val questions: List<QuizQuestion>, // Список вопросов
    val createdAt: LocalDateTime,      // Дата создания
    val updatedAt: LocalDateTime,      // Дата обновления
    val imageUrl: String? = null,      // Изображение (опционально)
)

// Тема квиза
data class QuizTheme(
    val id: String,
    val name: String,
    val color: String? = null,         // Цвет темы (hex)
    val iconUrl: String? = null,       // Иконка (опционально)
    val quizCount: Int = 0,            // Количество квизов
)

// Вопрос
data class QuizQuestion(
    val id: String,
    val questionText: String,          // Текст вопроса
    val imageUrl: String? = null,      // Изображение к вопросу (опционально)
    val answers: List<QuizAnswer>,     // Варианты ответов
    val explanation: String? = null,   // Пояснение
    val questionType: QuestionType,    // Тип вопроса
)

// Тип вопроса
enum class QuestionType {
    SINGLE_CHOICE,    // Выбор одного варианта
    MULTIPLE_CHOICE,  // Выбор нескольких вариантов
    TEXT_INPUT,       // Ввод текста
}

// Ответ
data class QuizAnswer(
    val id: String,
    val text: String,                  // Текст ответа
    val isCorrect: Boolean,            // Правильный ли
)

// Результат прохождения
data class QuizResult(
    val quizId: String,
    val totalQuestions: Int,
    val correctAnswers: Int,
    val percentage: Int,
    val completedAt: LocalDateTime,
    val bestResult: Boolean = false,   // Является ли лучшим результатом
)

// Прогресс прохождения
data class QuizProgress(
    val quizId: String,
    val currentQuestionIndex: Int,
    val answers: Map<String, String>,  // questionId -> answerId
    val startedAt: LocalDateTime,
)
```

### 3.2 UI модели

```kotlin
@Immutable
data class UiQuiz(
    val id: String,
    val title: String,
    val questionCount: Int,
    val theme: UiQuizTheme?,
    val progress: UiQuizProgress?,
    val bestResult: UiQuizBestResult?,
    val isNew: Boolean,
    val imageUrl: String?,
)

@Immutable
data class UiQuizTheme(
    val id: String,
    val name: String,
    val color: Color?,
    val quizCount: Int,
)

@Immutable
data class UiQuizProgress(
    val answered: Int,
    val total: Int,
)

@Immutable
data class UiQuizBestResult(
    val correct: Int,
    val total: Int,
    val percentage: Int,
)

@Immutable
data class UiQuizQuestion(
    val id: String,
    val questionNumber: Int,
    val totalQuestions: Int,
    val questionText: String,
    val imageUrl: String?,
    val answers: List<UiQuizAnswer>,
    val explanation: String?,
    val isAnswered: Boolean,
    val selectedAnswerId: String?,
    val isCorrectAnswer: Boolean?,
)

@Immutable
data class UiQuizAnswer(
    val id: String,
    val text: String,
    val state: AnswerState,  // DEFAULT, SELECTED_CORRECT, SELECTED_WRONG, CORRECT
)

enum class AnswerState {
    DEFAULT,
    SELECTED_CORRECT,
    SELECTED_WRONG,
    CORRECT,  // Показываем правильный при неверном выборе
}
```

### 3.3 Room Entity (локальное хранение)

```kotlin
@Entity(tableName = "quiz_cache")
data class QuizCacheEntity(
    @PrimaryKey val id: String,
    val title: String,
    val themeId: String?,
    val themeName: String?,
    val questions: String,         // JSON
    val createdAt: Long,
    val cachedAt: Long,            // Дата кеширования
)

@Entity(tableName = "quiz_result")
data class QuizResultEntity(
    @PrimaryKey val id: String,
    val quizId: String,
    val correctAnswers: Int,
    val totalQuestions: Int,
    val percentage: Int,
    val completedAt: Long,
    val isBestResult: Boolean,
)

@Entity(tableName = "quiz_progress")
data class QuizProgressEntity(
    @PrimaryKey val quizId: String,
    val currentQuestionIndex: Int,
    val answers: String,           // JSON Map<String, String>
    val startedAt: Long,
)

@Entity(tableName = "quiz_themes")
data class QuizThemeEntity(
    @PrimaryKey val id: String,
    val name: String,
    val color: String?,
    val quizCount: Int,
)
```

---

## 4. API и MediaWiki

### 4.1 Структура Wiki-страниц (предложение)

**Namespace:** `Quiz:`

**Страницы:**

```
Quiz:Название_квиза           # Главная страница квиза
Quiz:Название_квиза/data      # JSON с данными квиза
Quiz:Тема/Название_темы       # Страница темы с описанием
Quiz:Config                   # Конфигурация (список тем, настройки)
```

### 4.2 Шаблон для создания квиза

```wiki
{{Quiz
|title=Название квиза
|theme=Тема квиза
|description=Описание квиза
|image=Изображение.jpg
}}

{{QuizQuestion
|question=Текст вопроса?
|type=single
|image=Вопрос_картинка.jpg
|answer1=Вариант A|correct
|answer2=Вариант B
|answer3=Вариант C
|answer4=Вариант D
|explanation=Пояснение к правильному ответу
}}

{{QuizQuestion
|question=Вопрос с вводом текста?
|type=input
|answer=Правильный ответ
|explanation=Пояснение
}}
```

### 4.3 JSON структура (альтернатива)

```json
{
  "id": "quiz-history-001",
  "title": "История России",
  "theme": "history",
  "description": "Тест по истории России",
  "createdAt": "2026-07-25T10:00:00Z",
  "questions": [
    {
      "id": "q1",
      "text": "В каком году был основан Санкт-Петербург?",
      "type": "single",
      "answers": [
        {"id": "a1", "text": "1703", "correct": true},
        {"id": "a2", "text": "1712", "correct": false},
        {"id": "a3", "text": "1721", "correct": false},
        {"id": "a4", "text": "1612", "correct": false}
      ],
      "explanation": "Санкт-Петербург был основан 27 мая 1703 года Петром I."
    },
    {
      "id": "q2",
      "text": "Введите столицу России:",
      "type": "input",
      "answer": "Москва",
      "explanation": "Москва — столица Российской Федерации."
    }
  ]
}
```

### 4.4 API Endpoints

**Получение списка тем:**
```
GET /api.php?action=parse&page=Quiz:Config&prop=text&format=json
```

**Получение квизов по теме:**
```
GET /api.php?action=query&list=categorymembers&cmtitle=Category:Quiz_Тема&cmlimit=50&format=json
```

**Получение квиза:**
```
GET /api.php?action=parse&page=Quiz:Название_квиза&prop=text&format=json
```

---

## 5. Архитектура модуля

### 5.1 Структура модуля

```
quiz/
├── data/
│   ├── api/
│   │   ├── QuizApi.kt              # Retrofit интерфейс
│   │   ├── model/
│   │   │   ├── ApiQuizResponse.kt
│   │   │   └── ApiQuizListResponse.kt
│   ├── local/
│   │   ├── database/
│   │   │   └── QuizDatabase.kt
│   │   ├── dao/
│   │   │   ├── QuizCacheDao.kt
│   │   │   ├── QuizResultDao.kt
│   │   │   └── QuizProgressDao.kt
│   │   └── entity/
│   │       ├── QuizCacheEntity.kt
│   │       ├── QuizResultEntity.kt
│   │       └── QuizProgressEntity.kt
│   └── repository/
│       └── QuizRepositoryImpl.kt
│
├── domain/
│   ├── model/
│   │   ├── Quiz.kt
│   │   ├── QuizQuestion.kt
│   │   ├── QuizTheme.kt
│   │   └── QuizResult.kt
│   ├── repository/
│   │   └── QuizRepository.kt
│   └── usecase/
│       ├── GetQuizListUseCase.kt
│       ├── GetQuizUseCase.kt
│       ├── GetThemesUseCase.kt
│       ├── SaveQuizResultUseCase.kt
│       ├── GetQuizProgressUseCase.kt
│       └── SubmitQuestionUseCase.kt
│
├── presentation/
│   ├── root/
│   │   ├── ui/
│   │   │   ├── QuizRootUi.kt
│   │   │   ├── QuizListItem.kt
│   │   │   └── NewQuizzesBlock.kt
│   │   ├── mapper/
│   │   │   └── UiQuizMapper.kt
│   │   ├── model/
│   │   │   ├── UiQuiz.kt
│   │   │   └── UiQuizRootState.kt
│   │   └── viewmodel/
│   │       └── QuizRootViewModel.kt
│   │
│   ├── theme/
│   │   └── ui/
│   │       └── QuizThemeUi.kt
│   │
│   ├── detail/
│   │   ├── ui/
│   │   │   ├── QuizDetailUi.kt
│   │   │   ├── QuestionCard.kt
│   │   │   ├── AnswerOptions.kt
│   │   │   ├── ExplanationBlock.kt
│   │   │   └── ProgressIndicator.kt
│   │   ├── mapper/
│   │   │   └── UiQuizQuestionMapper.kt
│   │   ├── model/
│   │   │   ├── UiQuizQuestion.kt
│   │   │   └── UiQuizDetailState.kt
│   │   └── viewmodel/
│   │       └── QuizDetailViewModel.kt
│   │
│   ├── result/
│   │   └── ui/
│   │       └── QuizResultUi.kt
│   │
│   ├── history/
│   │   ├── ui/
│   │   │   └── QuizHistoryUi.kt
│   │   └── viewmodel/
│   │       └── QuizHistoryViewModel.kt
│   │
│   └── submit/
│       ├── ui/
│       │   └── SubmitQuestionUi.kt
│       └── viewmodel/
│           └── SubmitQuestionViewModel.kt
│
├── di/
│   └── QuizModule.kt
│
└── navigation/
    └── QuizScreen.kt
```

### 5.2 Навигация (Modo)

```kotlin
@Parcelize
class QuizRootScreen : Screen {
    @Composable
    override fun Content(modifier: Modifier) { ... }
}

@Parcelize
class QuizThemeScreen(
    private val theme: UiQuizTheme
) : Screen { ... }

@Parcelize
class QuizDetailScreen(
    private val quizId: String
) : Screen { ... }

@Parcelize
class QuizResultScreen(
    private val quizId: String,
    private val result: UiQuizResult
) : Screen { ... }

@Parcelize
class QuizHistoryScreen : Screen { ... }

@Parcelize
class SubmitQuestionScreen : Screen { ... }
```

---

## 6. Кеширование и офлайн-режим

### 6.1 Стратегия кеширования

1. **При первом заходе в квиз** - загрузка и кеширование:
   - Все вопросы с ответами
   - Пояснения к ответам
   - Метаданные (тема, название)

2. **Офлайн-доступ:**
   - Прохождение квиза без интернета
   - Подсчёт результата локально
   - Синхронизация при появлении сети (для отправки предложения вопроса)

3. **Обновление кеша:**
   - Проверка `updatedAt` при наличии интернета
   - Удаление старых записей (хранить последние N квизов)

### 6.2 Room Database

```kotlin
@Database(
    entities = [
        QuizCacheEntity::class,
        QuizResultEntity::class,
        QuizProgressEntity::class,
        QuizThemeEntity::class,
    ],
    version = 1
)
abstract class QuizDatabase : RoomDatabase() {
    abstract fun quizCacheDao(): QuizCacheDao
    abstract fun quizResultDao(): QuizResultDao
    abstract fun quizProgressDao(): QuizProgressDao
    abstract fun quizThemeDao(): QuizThemeDao
}
```

---

## 7. Анимации и UX

### 7.1 Анимация иконки в тулбаре новостей

**Триггер:** Наличие непройденных новых квизов

**Типы анимации:**
- Пульсация (scale 1.0 → 1.2 → 1.0)
- Покачивание (rotation -10° → +10°)
- Bounce (прыжок вверх-вниз)

**Периодичность:** Каждые 10-15 секунд

**Реализация:**
```kotlin
@Composable
fun QuizIconWithAnimation(hasNewQuizzes: Boolean) {
    val infiniteTransition = rememberInfiniteTransition()
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (hasNewQuizzes) 1.2f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        )
    )

    Icon(
        modifier = Modifier.scale(scale),
        imageVector = Icons.Default.Quiz,
        contentDescription = "Квизы"
    )
}
```

### 7.2 Анимация правильного ответа

**Эффект:** Конфетти/салют

**Библиотека:** [Konfetti](https://github.com/DanielMartinus/Konfetti) или кастомная анимация

```kotlin
@Composable
fun SuccessAnimation(trigger: Boolean) {
    if (trigger) {
        KonfettiView(
            parties = listOf(
                Party(
                    speed = 0f,
                    maxSpeed = 30f,
                    damping = 0.9f,
                    spread = 360,
                    colors = listOf(0xfce18a, 0xff726d, 0xf4306d),
                    position = Position.Relative(0.5, 0.3)
                )
            )
        )
    }
}
```

### 7.3 Вибрация при неверном ответе

```kotlin
fun vibrateOnWrongAnswer(context: Context) {
    val vibrator = context.getSystemService<Vibrator>()
    vibrator?.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
}
```

---

## 8. План реализации

### Этап 1: Базовая инфраструктура
- [ ] Создать модуль `quiz`
- [ ] Настроить DI (Hilt модуль)
- [ ] Создать Room Database
- [ ] Создать базовые модели данных

### Этап 2: API и парсинг Wiki
- [ ] Реализовать QuizApi
- [ ] Написать парсер MediaWiki шаблонов
- [ ] Реализовать репозиторий с кешированием

### Этап 3: Корневой экран
- [ ] QuizRootScreen с Modo
- [ ] Список новых квизов
- [ ] Список тем
- [ ] Карточки квизов

### Этап 4: Экран квиза
- [ ] QuizDetailScreen
- [ ] Отображение вопроса
- [ ] Варианты ответов
- [ ] Обработка выбора ответа
- [ ] Показ пояснения
- [ ] Навигация между вопросами

### Этап 5: Результаты и история
- [ ] QuizResultScreen
- [ ] QuizHistoryScreen
- [ ] Сохранение лучшего результата

### Этап 6: Форма предложения вопроса
- [ ] SubmitQuestionScreen
- [ ] Переиспользовать механизм Tracer из bugreport модуля
- [ ] Отправка формы через email: angainor17@gmail.com

### Этап 7: Интеграция
- [ ] Иконка в тулбаре новостей
- [ ] Анимация иконки
- [ ] Тестирование офлайн-режима

---

## 9. Открытые вопросы

Все ключевые вопросы закрыты. Оставшиеся детали для уточнения на этапе реализации:

1. ~~Нужна ли кнопка "Поделиться результатом"?~~ → **Нет**
2. ~~Нужен ли рейтинг сложности квизов?~~ → **Нет**
3. ~~Как обрабатывать вопросы с несколькими правильными ответами?~~ → **Только single choice**
4. ~~Нужна ли возможность пропустить вопрос?~~ → **Да, но для завершения нужно ответить на все**
5. ~~Срок годности кеша - сколько дней хранить?~~ → **14 дней**
6. ~~Отправка предложения вопроса - email или создание Wiki-страницы?~~ → **Email через Tracer**

**Детали для реализации:**

- Пропуск вопроса: пользователь может перейти к следующему вопросу без ответа, вернуться позже. Но квиз считается завершённым только когда все вопросы отвечены.
- Для завершения квиза на последнем вопросе показывать: "Осталось N вопросов без ответа" если есть пропущенные

---

## 10. Анализ аналогичных приложений (Research)

### 10.1 UI/UX паттерны

**Навигация:**
- Bottom navigation с 3-5 табами (Material Design рекомендация)
- Иконки с текстовыми лейблами
- Активный таб подсвечивается
- Navigation hub pattern для task-based приложений

**Списки квизов:**
- Карточки с gradient backgrounds
- Rounded tab indicators
- Category cards с иконками
- Floating Action Button для создания/старта квиза

### 10.2 Геймификация

**Kahoot:**
- Self-paced режим - асинхронное прохождение
- Вопросы: true/false, multi-select, puzzle, type answer, slider, poll, brainstorm, word cloud
- Тiers по количеству участников

**Khan Academy:**
- Mastery-based learning system
- Бейджи и energy points
- Instant feedback и рекомендации
- Прогресс-трекинг

### 10.3 Офлайн-режим

**Архитектура (Android Best Practices):**
- Local database как Single Source of Truth (SSOT)
- Repository pattern с NetworkBoundResource
- Unidirectional Data Flow (UDF)
- WorkManager для синхронизации
- LCE pattern (Loading/Content/Error)

**Паттерны синхронизации:**
- Pull-based: загрузка по требованию (для кратких офлайн-периодов)
- Push-based: проактивная загрузка при старте (для длительного офлайна)

**Реализация в Khan Academy:**
- Bookmark и download контента
- Sync прогресса при восстановлении соединения
- Progressive download strategy

### 10.4 Выводы для SV APP

1. **Навигация:** Использовать существующий bottom nav, добавить иконку квизов
2. **Геймификация:** Бейджи за прохождение, прогресс-индикатор, анимации успеха
3. **Офлайн:** Room DB как SSOT, кеширование при первом заходе, sync при онлайне
4. **Вопросы:** Single choice + input (без multi-select), пояснения после ответа

---

## 11. Рекомендации по бекенду

### 11.1 Создание квизов в MediaWiki

**Рекомендуемый workflow для редакторов:**

1. **Создание страницы квиза:**
   - Namespace: `Quiz:`
   - Использовать шаблон `{{Quiz}}` и `{{QuizQuestion}}`

2. **Пример Wiki-разметки:**
```wiki
{{Quiz
|title=История России: Основные даты
|theme=history
|description=Проверьте свои знания по истории России
|created=2026-07-25
}}

{{QuizQuestion
|question=В каком году был основан Санкт-Петербург?
|type=single
|answer1=1703|correct
|answer2=1712
|answer3=1721
|answer4=1612
|explanation=Санкт-Петербург основан 27 мая 1703 года Петром I на берегу Невы.
}}

{{QuizQuestion
|question=Назовите столицу Древней Руси
|type=input
|answer=Киев
|explanation=Киев был столицей Древней Руси с IX века.
}}
```

3. **Страница темы (опционально):**
```wiki
{{QuizTheme
|name=История
|color=#E53935
|icon=File:History_icon.png
|description=Квизы по истории России и мировой истории
}}
```

### 11.2 Структура Wiki-страниц

```
Quiz:Index                    # Индекс всех квизов
Quiz:Config                   # Конфигурация (темы, настройки)
Quiz:Название_квиза           # Страница квиза с шаблонами
Quiz:Тема:История             # Страницы тем
Category:Quiz                 # Категория для всех квизов
Category:Quiz_История         # Категории по темам
```

### 11.3 Парсинг на клиенте

**Алгоритм:**
1. GET запрос к `api.php?action=parse&page=Quiz:Название`
2. Парсинг HTML для извлечения данных из шаблонов
3. Преобразование в JSON-модели
4. Кеширование в Room

**Альтернатива - JSON-страницы:**
```
Quiz:Название_квиза/data      # Сырой JSON
```

### 11.4 Создание и редактирование

**Для редакторов:**
- Создавать квизы через Wiki-редактор
- Использовать превью шаблонов
- Проверять корректность через спецстраницу `Special:QuizPreview`

**Для пользователей (предложение вопроса):**
- Форма в приложении → отправка через Tracer
- Ручное добавление редактором в Wiki

### 11.5 Валидация данных

**На уровне Wiki-шаблонов:**
- Обязательные поля: title, хотя бы 1 вопрос
- Минимум 2 варианта ответа для single choice
- Проверка наличия correct-маркера

**На клиенте:**
- Валидация JSON-структуры
- Fallback при ошибках парсинга

---

## 12. Связанные документы

- [[design-system]] - Дизайн-система SV APP
- [[wiki-module-task]] - Wiki-модуль
- [[mock-system]] - Система моков для тестирования