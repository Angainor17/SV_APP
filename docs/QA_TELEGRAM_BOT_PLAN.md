# Telegram-бот для вопросов/опечаток + лента Вопрос-Ответ

## Статус реализации

- [x] **Раздел 2** — модуль `:qa` (domain + data + Room + Retrofit + DI + моки трёх эндпоинтов) —
  реализовано и собирается (2026-08-04)
- [ ] **Раздел 1** — контракт бэкенда (вне этого репо, зафиксирован ниже как контракт для
  Android-части)
- [ ] **Раздел 3** — синхронизация при запуске приложения (`@ApplicationScope`, `SvApp.onCreate()`)
- [ ] **Раздел 4** — читалка: диалоги подтверждения вместо буфера обмена/TG-ссылок
- [ ] **Раздел 5** — читалка: подсветка текста с отвеченным вопросом (`QuestionsView`)
- [ ] **Раздел 6** — books: открыть книгу на нужной позиции после скачивания (почти целиком уже
  готово, переиспользуется)
- [ ] **Раздел 7** — экран "Новости": фильтр-чипы, карточка вопрос-ответ, слияние ленты, переход в
  книгу

---

## Контекст

Сейчас в читалке при выделении текста есть 2 кнопки — "сообщить об опечатке" и "задать вопрос". Обе
просто копируют текст в буфер обмена и открывают Telegram-канал (
`managers/src/main/java/su/sv/managers/OnBookPagerManager.kt`), где пользователь должен вручную
вставить и отправить сообщение. Данные о книге/странице/тексте до бота реально не доходят.

Цель — заменить это на полноценный цикл:
приложение → бэкенд → Telegram-бот (админ-группа) → админ отвечает в Telegram → бэкенд хранит
вопрос+ответ → приложение синхронизирует и показывает список Вопрос-Ответ (экран "Новости"), с
подсветкой отвеченного текста прямо в читалке.

**Бэкенд размещаем на уже существующем сервере** (`svremya.su`/`svremya.org` — том же, что отдаёт
каталог книг и wiki), а не поднимаем новую инфраструктуру: домен уже доступен пользователям без
VPN (проверено текущим трафиком приложения), в отличие от нового домена, который мог бы попасть под
блокировки РКН вместе с остальной инфраструктурой Telegram. Реализация самого бэкенда (эндпоинты,
БД, вебхук Telegram) — вне этого репозитория; здесь фиксируется только контракт.

Разведка подтвердила, что часть инфраструктуры для "открыть книгу на нужной позиции" **уже
существует** (см. `ReaderScreen.bookmarkPosition`, `BookDetailViewModel.onBookDownloadEnd()`) —
переиспользуем её, а не строим заново.

---

## 1. Контракт бэкенда (REST, вне этого репо)

Единый бот в админ-группе: новые репорты приходят туда с inline-кнопками "Ответить"/"Удалить";
ответ — это текстовое сообщение-реплай админа на сообщение бота (бэкенд сам сопоставляет
`reply_to_message` → report id по сохранённому `tg_message_id`). Редактирование ответа — повторный
реплай/отдельная кнопка "Изменить". Удаление — soft-delete (дубликат/неподходящий), такие записи не
отдаются в `/qa`.

```
POST /reports/typo
  { bookId, bookTitle, page, selectedText, comment? }

POST /reports/question
  { bookId, bookTitle, page, selectedText, authorName }

GET /qa?since=<epochMillis>          // только отвеченные и не удалённые
  -> [{
       id, bookId, bookTitle, page, selectedText, authorName,
       questionCreatedAt, answerText, answerUpdatedAt,
       startParagraph, startElement, startChar,
       endParagraph, endElement, endChar
     }]
```

`bookId` — это **каталожный** id книги (`Book.id: String` из `books/.../domain/model/Book.kt`), а не
локальный MD5-based id файла, которым оперируют текущие заметки/закладки (`BookmarkNote.bookId`). Их
нельзя путать — везде в новом коде это поле называется `catalogBookId`.

Позиция (`start/endParagraph/Element/Char`) — та же схема, что уже использует `BookmarkPosition` (
`bookreader/.../screens/ReaderScreen.kt:19-26`) и `BookmarkNote` — плоское представление
FBReader'овского `ZLTextPosition`. Ответ `/qa` мапится напрямую в `BookmarkPosition` без
дополнительного типа.

Реализовано на стороне Android (модуль `:qa`, см. раздел 2): DTO с этим контрактом (
`ApiTypoReportRequest`, `ApiQuestionReportRequest`, `ApiAnsweredQuestion`), `QaApi` (
`su.sv.qa.data.api.QaApi`), базовый URL `https://svremya.su/`.

---

## 2. Модуль `:qa` (domain + data) — ✅ реализовано (2026-08-04)

Мирроринг `wiki`-модуля (Room + свой Retrofit-клиент через Hilt `@Qualifier`, см.
`wiki/.../di/WikiApiModule.kt` и `wiki/.../di/WikiDatabaseModule.kt`).

**Добавлено:**

- `qa/build.gradle.kts` — android-library, deps: `commonarchitecture`, Room, Retrofit/Gson, Hilt.
- `qa/src/main/java/su/sv/qa/domain/model/` — `AnsweredQuestion`, `TypoReport`, `QuestionReport`.
- `qa/src/main/java/su/sv/qa/domain/repository/QaRepository.kt`:
  ```kotlin
  interface QaRepository {
      suspend fun submitTypoReport(report: TypoReport): Result<Unit>
      suspend fun submitQuestionReport(report: QuestionReport): Result<Unit>
      suspend fun syncAnsweredQuestions(): Result<Unit>
      fun observeAnsweredQuestions(): Flow<List<AnsweredQuestion>>
      fun observeAnsweredQuestionsForBook(catalogBookId: String): Flow<List<AnsweredQuestion>>
  }
  ```
- `qa/.../domain/usecase/` — `SubmitTypoReportUseCase`, `SubmitQuestionReportUseCase`,
  `SyncAnsweredQuestionsUseCase`, `ObserveAnsweredQuestionsUseCase`,
  `ObserveAnsweredQuestionsForBookUseCase`.
- `qa/.../data/api/QaApi.kt` + DTO-модели — 1:1 с контрактом из раздела 1.
- `qa/.../data/local/entity/AnsweredQuestionEntity.kt` (`@Entity`, поля как в контракте,
  `@PrimaryKey val id`, индексы по `bookId`, `answerUpdatedAt`).
- `qa/.../data/local/dao/AnsweredQuestionDao.kt` — `getAll()/getForBook()` как `Flow`,
  `upsertAll(...)`, `getMaxAnswerUpdatedAt(): Long?` (для расчёта `since`).
- `qa/.../data/local/database/QaDatabase.kt` — по образцу `WikiDatabase.kt`, `version = 1`.
- `qa/.../data/repository/QaRepositoryImpl.kt` — все вызовы через `runCatchingHttpRequest { ... }` (
  `commonarchitecture/.../RunCatchingApiRequest.kt`); `syncAnsweredQuestions()` берёт
  `since = dao.getMaxAnswerUpdatedAt() ?: 0`, апсертит результат.
- `qa/.../di/QaDatabaseModule.kt`, `qa/.../di/QaApiModule.kt` — Retrofit на базовый URL
  `https://svremya.su/`, `@Binds QaRepositoryImpl -> QaRepository`.

**Изменено:**

- `settings.gradle.kts` — `include(":qa")`.
- `commonarchitecture/.../mock/MockInterceptor.kt` + `MockDataProvider.kt` — добавлена ветка для
  новых эндпоинтов (`/reports/typo`, `/reports/question`, `/qa?`), проверяется раньше общего правила
  `svremya.su`, т.к. Qa и Wiki на одном хосте.

**Проверено:** `./gradlew :qa:compileDebugKotlin :commonarchitecture:compileDebugKotlin` — успешно.

**Пока не сделано:** модуль `:qa` ещё никем не используется — `app`/`bookreader`/`news` на него не
ссылаются (это разделы 3–7 ниже).

---

## 3. Синхронизация при запуске приложения

Сейчас в приложении нет ни app-scoped `CoroutineScope`, ни паттерна "sync on launch" — добавляем
впервые.

**Изменить:**

- `commonarchitecture/.../di/module/CoroutineModule.kt` — добавить `@ApplicationScope`
  квалификатор +
  `@Provides @Singleton fun provideApplicationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)`.
- `app/src/main/java/su/sv/app/SvApp.kt` — `@Inject` `SyncAnsweredQuestionsUseCase` и
  `@ApplicationScope CoroutineScope`; в `onCreate()` —
  `appScope.launch { syncAnsweredQuestionsUseCase() }` (best-effort, ошибки проглатываются —
  офлайн-режим остаётся на последних синхронизированных данных в Room).
- `app/build.gradle.kts` — `implementation(project(":qa"))`.

---

## 4. Читалка: диалоги подтверждения вместо буфера обмена + Telegram-ссылки

**`catalogBookId` нужно довести до `ReaderViewModel`** — сейчас читалка знает только про MD5-based
id файла. Добавить параметр `catalogBookId: String?` в `ReaderScreen` → `ReaderContent` →
`ReaderActions.LoadBook` → `ReaderState.Content`.

Прокинуть на всех точках входа в читалку, где `catalogBookId` уже известен тривиально (
`BookDetailUi.kt`, `RootBooksCatalog.kt`, `DownloadedBooksScreen.kt` — везде это `book.id` из
каталога). Для входов, где сейчас его нет (`BookmarksScreen.kt` — только MD5,
`ContinueReadingViewModel.kt` — только URI) — предусмотреть lookup по существующему списку
каталога (`GetBooksListUseCase`) с сопоставлением по `fileUri`/`fileNameWithExt`; если не
резолвится — `null`.

**Решение:** кнопки "Вопрос"/"Опечатка" в `SelectionComposePanel` должны быть недоступны (
скрыты/задизейблены), если `catalogBookId == null` — отправка репорта с неверным/пустым id
недопустима, это ломает админскую сторону.

### 4a. Замена цепочки extraction → диалог вместо clipboard+Intent

Существующая логика извлечения `(text, title, author, page)` из выделения (`FBReaderView.java` →
`selectionSVAction(...)`) переиспользуется как есть — меняется только то, что она передаёт наружу.

**Изменить `bookreader/.../widgets/FBReaderView.java`:**

- `Listener`: заменить неявную зависимость от `OnBookPagerManager` на два метода —
  `onQuestionSelected(text, title, author, page)` / `onMisspellSelected(text, title, author, page)`.
- `ActionCode.ASK_QUESTION`/`TEL_ABOUT_MISSPELL` — теперь просто зовут эти методы листенера вместо
  `onBookPagerManager.askQuestion/tellAboutMisspell`.
- `setActivity(...)` — убрать параметр `OnBookPagerManager` (обновить вызов в
  `ReaderContent.kt:339`).

**Изменить `ReaderContent.kt`** — реализовать два новых метода `Listener` →
`viewModel.onAction(ReaderActions.SelectionReportReady(kind, text, title, author, page))`.

**Изменить `ReaderActions.kt`** — добавить `SelectionReportReady`, `SubmitTypoReport(comment)`,
`SubmitQuestionReport(authorName)`, `HideReportDialog`, `enum ReportKind { QUESTION, TYPO }`.

**Изменить `ReaderState.kt`** — в `Content` добавить `reportDialog: ReportDialogState?`,
`isSubmittingReport: Boolean` (`ReportDialogState(kind, text, bookTitle, page)`).

**Изменить `ReaderViewModel.kt`:**

- Инжектировать `SubmitTypoReportUseCase`/`SubmitQuestionReportUseCase` вместо `OnBookPagerManager`.
- `SelectionReportReady` → выставить `reportDialog`.
- `SubmitTypoReport`/`SubmitQuestionReport` → собрать `TypoReport`/`QuestionReport` из
  `state.catalogBookId` + данных диалога, вызвать use case, по успеху закрыть диалог и показать
  snackbar-эффект.
- Добавить one-shot эффект-канал (`ReaderOneTimeEffect.ShowSnackbar`) — сейчас в этой VM такого нет,
  во всех остальных модулях паттерн уже есть, здесь заводится впервые.

### 4b. Новые диалоги (по образцу `BookmarkBottomSheet.kt`)

**Добавить:**

- `bookreader/.../screens/ui/TypoReportBottomSheet.kt` — заголовок "Подтвердите информацию об
  опечатке", read-only блок книга/страница/выделенный текст, необязательное поле "Комментарий",
  кнопка отправки.
- `bookreader/.../screens/ui/QuestionReportBottomSheet.kt` — та же форма + обязательное поле "Автор
  вопроса" (кнопка отправки задизейблена, пока пусто).

**Изменить `ReaderContent.kt`** — рендерить нужный sheet по `reportDialog?.kind`.

### 4c. Очистка

**Удалить** `managers/src/main/java/su/sv/managers/OnBookPagerManager.kt` и связанные строки (
`misspell_clip_title`, `question_text_template`, `MISSPELL_TG_LINK`, `QUESTION_TG_LINK` и т.п.) —
других вызывающих кроме читалки нет.

---

## 5. Читалка: подсветка текста с отвеченным вопросом

**Решение: отдельный класс `QuestionsView` в `FBReaderView.java`, копия структуры `BookmarksView` (~
строки 1932-1983), а не флаг типа внутри неё.**

Причина: `BookmarksView` работает с `Storage.Bookmark` — пользовательской, редактируемой,
JSON-персистентной сущностью со своим жизненным циклом. Подсветка вопроса — read-only проекция
синхронизированных с сервера данных (Room), с другим жизненным циклом (обновляется при синке, не
редактируется в читалке) и другим действием по тапу (read-only просмотр вместо редактирования).
Заводить общий флаг означало бы завязать `Storage.java` (и так помечен как legacy/в процессе
декомпозиции — см. `bookreader/CLAUDE.md`) на серверные данные без реальной экономии кода.
`PagerWidget.kt`/`ScrollWidget.java` уже держат несколько независимых overlay-классов на страницу (
`BookmarksView`, `LinksView`, `TTSView` и т.п.) — добавление ещё одного такого же по форме класса —
устоявшийся в этом коде приём, а не новый паттерн.

**Добавить:**

- `bookreader/.../widgets/QuestionHighlight.kt` —
  `data class QuestionHighlight(id, start: ZLTextPosition, end: ZLTextPosition, questionText, answerText, authorName)`.
- В `FBReaderView.java`: поле `List<QuestionHighlight> questionHighlights`, метод листенера
  `onQuestionHighlightClick(QuestionHighlight)`, класс `QuestionsView` — та же логика
  позиционирования через `Plugin.View.Selection.getBounds(page)` + `SelectionView.lines(rr)`, но *
  *отдельная цветовая схема**, явно отличимая от закладок/заметок (не из палитры `BOOKMARK_COLORS`,
  которую выбирает пользователь).

**Изменить `PagerWidget.kt` (~строка 147) и `ScrollWidget.java` (~строка 596)** — по одной строке,
инстанцировать `QuestionsView` рядом с существующим `BookmarksView`.

**Изменить `ReaderViewModel.kt`** — после загрузки книги с известным `catalogBookId`, подписаться на
`ObserveAnsweredQuestionsForBookUseCase(catalogBookId)`, смаппить `AnsweredQuestion.position` →
`ZLTextPosition` (те же поля, что уже конвертируются в `BookmarkPosition` в других местах),
выставить `fbReaderView.questionHighlights`, вызвать существующий механизм перерисовки (тот же, что
у обновления закладок). Добавить `ShowAnsweredQuestion`/`HideAnsweredQuestion` action +
`viewingAnsweredQuestion` в state.

**Добавить `bookreader/.../screens/ui/AnsweredQuestionBottomSheet.kt`** — read-only sheet: текст
вопроса, текст ответа, имя автора.

---

## 6. Books: открыть книгу на нужной позиции после скачивания

Это **почти целиком уже реализовано** для заметок (`BookDetailViewModel.onBookDownloadEnd()` →
`BookDetailOneTimeEffect.OpenBookAtNote` → `ReaderScreen(bookmarkPosition = ...)`, см.
`books/.../detail/viewmodel/BookDetailViewModel.kt:86,175` и
`books/.../detail/ui/BookDetailUi.kt:124-134`). Для Q&A переиспользуем тот же путь один-в-один, без
новой инфраструктуры:

- Экран деталей книги, открытый из карточки Вопрос-Ответ, передаёт туда же `BookmarkPosition`,
  собранный из полей `AnsweredQuestion` (те же 6 int-полей).
- `onBookDownloadEnd()` уже проверяет наличие `fileUri` и эмитит эффект открытия на позиции —
  никаких изменений в `BookDownloadBroadcastReceiver`/`BookDownloadedActionHandler` не требуется.

---

## 7. Экран "Новости"

### 7a. Фильтр-чипы

**Добавить** `news/.../presentation/root/ui/NewsFeedFilterChips.kt` — по образцу
`books/.../BookFiltersChips.kt` (`LazyRow` из `FilterChip`), но проще: 3 фиксированных варианта "
Всё"/"Вопросы"/"Новости", без логики "выбранные чипы вперёд" (это single-select, не multi-select).

**Изменить** `UiRootNewsState.kt` (+`feedFilter`), `RootNewsActions.kt` (+`OnFeedFilterClick`),
`RootNewsViewModel.kt`, `RootNews.kt` (рендер чипов между тулбаром и списком).

### 7b. Карточка вопрос-ответ

**Добавить** `UiQaCardItem` (id, dateFormatted, dateEpochMillis, questionText, answerText,
bookTitle, page, selectedText, catalogBookId, позиция) + маппер + `QaCardItem.kt` — визуально
отличная от `NewsItem.kt` карточка (другой цвет контейнера/иконка), с датой, текстом вопроса и
вложенным тапабельным блоком книга/страница/текст (тап реагирует только на этот суб-блок, не на всю
карточку).

### 7c. Объединение с лентой новостей — решение

Новости — удалённый paged источник (Paging 3, `NewsPagingSource`), Q&A — небольшой локальный список
из Room. **Рекомендация: клиентское слияние на уровне ViewModel**, а не отдельный кастомный
`PagingSource`:

- Q&A грузится целиком и eagerly через `observeAnsweredQuestions()` (объём заведомо небольшой).
- Через `PagingData.map`/`insertSeparators`-трансформацию на уже загруженных страницах новостей
  вставляются карточки Q&A по совпадению временного диапазона с соседними новостями (сравнение
  `dateEpochMillis` — это поле нужно добавить в `UiNewsItem`, сейчас там только форматированная
  строка даты, хотя сырое значение уже есть в `NewsItem.date` и просто не долетает до UI-модели).
- Режим "Вопросы" — рендерить `qaFlow` напрямую как `LazyColumn` без пейджинга; режим "Новости" —
  текущее поведение без изменений.

Альтернатива (кастомный `PagingSource`, комбинирующий оба источника в `load()`) отклонена как
избыточная для заведомо маленького второго источника — она не даёт ничего сверх постраничной
вставки, но требует свою логику ключей/инвалидации.

Компромисс подхода: карточка Q&A может появиться не мгновенно, а только когда до её позиции по
времени "доедет" подгруженная страница новостей — приемлемо при небольшом объёме Q&A; чтобы не
терять самые новые записи, свежие Q&A (новее самой новой уже загруженной новости) всегда
показываются первыми на первой странице.

### 7d. Переход в книгу по тапу

**Изменить `RootNewsViewModel.kt`** — по тапу на суб-блок карточки: получить список книг (
`GetBooksListUseCase`, потребует новую зависимость `news -> books`), найти по
`it.id == catalogBookId`:

- скачана (`fileUri != null`) → эффект открытия читалки сразу на нужной позиции (`BookmarkPosition`
  из полей карточки);
- не скачана → эффект перехода в `BookDetailScreen` с той же `BookmarkPosition`, чтобы после
  скачивания сработал уже существующий механизм из раздела 6.

**Изменить:** `NewsListOneTimeEffect.kt` (+2 эффекта), `RootNews.kt` (обработка),
`news/build.gradle.kts` (+`implementation(project(":books"))`,
`+implementation(project(":bookreader"))`).

---

## Критичные файлы

- `bookreader/.../widgets/FBReaderView.java` — сердце и extraction-цепочки, и нового
  `QuestionsView`; почти все reader-изменения проходят через него.
- `bookreader/.../screens/viewmodel/ReaderViewModel.kt` — владеет `catalogBookId`, состоянием
  диалогов репорта, синком подсветок вопросов.
- `qa/.../domain/repository/QaRepository.kt` (+ Impl/DI) — контракт, которым пользуются
  `bookreader`, `news`, `app`.
- `books/.../detail/viewmodel/BookDetailViewModel.kt` — точка расширения `onBookDownloadEnd()`, уже
  готовая.
- `news/.../presentation/root/viewmodel/RootNewsViewModel.kt` — слияние ленты и фильтрация.
- `managers/src/main/java/su/sv/managers/OnBookPagerManager.kt` — удаляется.

## Проверка

- Юнит: `QaRepositoryImpl` (мокнутый API + in-memory/Room DAO) — sync/upsert/`since`-логика.
- Инструментальные (`app/src/androidTest`): обновить существующие тесты читалки (
  `ReaderScreenTest.kt`/`ReaderScreenExtendedTest.kt`), которые сейчас неявно завязаны на
  clipboard/Toast поведение старого флоу — заменить на проверку новых bottom sheet'ов.
- Ручная проверка через запуск приложения: выделить текст → отправить опечатку/вопрос → убедиться,
  что запрос уходит (через мок-режим `MockConfig.IS_MOCK_ENABLED`, если бэкенд ещё не готов) → после
  ответа админа (или мока) — синк на старте → карточка в "Новости" → тап → переход в книгу на нужной
  странице → подсветка в читалке → тап по подсветке → диалог с вопросом/ответом/автором.
- Включить моки трёх новых эндпоинтов в `MockDataProvider` до готовности реального бэкенда, чтобы
  Android-часть разрабатывалась независимо. ✅ сделано.
