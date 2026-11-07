# Асинхронность в проекте

Канонический стандарт реализации асинхронности в SV APP. Всё асинхронное поведение — на
**корутинах**. Handler/Thread/Runnable/AsyncTask/Timer/Executor в коде приложения запрещены.

> Смежные документы: `memory/coroutine-optimization.md` (DispatcherProvider, UseCase, ViewModel),
> `docs/technical-debt.md` (исторический список техдолга).

---

## 1. Правило по умолчанию

| Категория                              | Как делать                                            | Как НЕ делать                     |
|----------------------------------------|-------------------------------------------------------|-----------------------------------|
| Файлы / сеть / БД / DataStore          | `dispatcherProvider.io` / `withContext(Dispatchers.IO)` | `new Thread { ... }`              |
| CPU-интенсивное (маппинг, сортировка)  | `Dispatchers.Default`                                 | `AsyncTask`                       |
| UI-обновления                          | `Dispatchers.Main` / `viewModelScope`                 | `Handler.post` + хранимый Runnable |
| Отложенное/периодическое               | `launch { delay(...) }` + `Job.cancel()`              | `Handler.postDelayed` + tag-Runnable |
| Одноразовый «после layout-прохода»     | `View.post { }` / `View.postDelayed { }`              | (см. §4 — это разрешено)          |

---

## 2. Compose + Hilt слой (ViewModel / UseCase)

Стандарт зафиксирован в `memory/coroutine-optimization.md`. Коротко:

- **DispatcherProvider** (Hilt-модуль `CoroutineModule`) — единственная точка инъекции диспетчеров:
  `io`, `main`, `default`. Тестируемо, без хардкода `Dispatchers.*`.
- **UseCase** — `suspend`, main-safe: сам берёт `withContext(dispatcherProvider.io)` и не заставляет
  вызывающего думать о потоке.

```kotlin
class GetArticleUseCase @Inject constructor(
    private val dispatcherProvider: DispatcherProvider,
    private val repository: WikiRepository,
) {
    suspend fun execute(title: String): WikiResult<WikiArticle> =
        withContext(dispatcherProvider.io) { repository.getArticle(title) }
}
```

- **ViewModel** — запускает в `viewModelScope.launch { }`, маппинг в `withContext(dispatcherProvider.default)`,
  результат — в `StateFlow`.

```kotlin
fun loadArticle(title: String) {
    viewModelScope.launch {
        val result = getArticleUseCase.execute(title)
        val ui = withContext(dispatcherProvider.default) { mapper.mapToUi(result.data) }
        _state.value = ArticleState.Content(ui)
    }
}
```

- **Flow** — не `withContext`, а `flowOn(...)`; в UI собирается через `collectAsStateWithLifecycle()`.

---

## 3. Кастомные View вне lifecycle (модуль `bookreader`)

Классы-наследники `View`/`ViewGroup`/`RecyclerView.Adapter`, которые живут вне
`lifecycleScope`, заводят **собственный** scope и гасят его вручную:

```kotlin
private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

override fun onDetachedFromWindow() {
    super.onDetachedFromWindow()
    scope.cancel()
}
```

Правила:

- `SupervisorJob()` — падение одной корутины не роняет остальные.
- `Dispatchers.Main.immediate` — запуск «уже на Main» без лишней переброски через очередь сообщений.
- Тяжёлая работа — `scope.launch(Dispatchers.Default) { ... }`, мутации общего состояния —
  внутри `withContext(Dispatchers.Main.immediate) { ... }`.
- Отложенный/дебаунс — `Job`: `job?.cancel(); job = scope.launch { delay(...); ... }`.

Примеры в коде: `ScrollWidget.kt` (`idleJob`, `reflowJob`), `FBReaderView.kt`
(`showControls`/`hideControls` через `scope.launch` + tag-`Job`).

---

## 4. Идиоматичный `View.post` / `View.postDelayed`

**Разрешено** как одноразовый «выполнить после текущего layout-прохода». Это НЕ анти-паттерн —
в отличие от `Handler` + хранимый `Runnable`, тут нет ссылки на Runnable, которую надо чистить.

```kotlin
post { reset() }        // дождаться размеров после layout
view.postDelayed({ ... }, 300)   // одноразовая задержка, без removeCallbacks
```

Запрещён только паттерн `Handler + postDelayed(сохранённый Runnable) + removeCallbacks` — его
заменяет `Job` + `delay` + `cancel`.

---

## 5. Контракты фреймворка (оставить как есть)

Эти места НЕ мигрируются — они продиктованы API фреймворка/движка, а не «старым стилем»:

- `ScrollWidget.kt` — `ProgressBar.scheduleDrawable(who, what: Runnable, atTime)` +
  внутренний `Handler().postAtTime` для indeterminate-спиннера. Это контракт `ProgressBar`.
- `FBReaderView.kt` — `override fun runOnConnect(runnable: Runnable)` (интерфейс `Config`) и
  `override fun createExecutor(key): ZLApplication.SynchronousExecutor? = null` (интерфейс
  `ZLApplicationWindow`). Пустые/нулевые реализации — контракт, менять нельзя.
- `fbreader` — `org.geometerplus.zlibrary.core.application` и др. используют `SynchronousExecutor`.

---

## 6. Осознанное исключение: `runBlocking`

Единственный разрешённый `runBlocking` — `ThemeRepositoryImpl.init` (модуль `managers`):

```kotlin
runBlocking { dataStore.data.first() }
```

Причина: однократное синхронное чтение крошечного файла на старте, до первого кадра, чтобы не
мигать системной темой. Значение кэшируется в `@Volatile`-поле. Новые `runBlocking` добавлять
нельзя — это исключение, а не образец.

---

## 7. Вне области: движок `fbreader`

Модуль `fbreader` — вендоренный порт FBReader. Внутри него остаётся Java-асинхронность
(`Thread`/`Executor`/`Handler`/`synchronized`/`Timer`) в ядре (`ZLApplication`, `FBReaderApp`,
`FBView`, форматные плагины). Это **техдолг движка**, отдельная большая работа; в рамках кампании
по корутинизации ядро не трогалось.

---

## 8. Что уже удалено как мёртвый async (2026-11-07)

| Удалено                                          | Почему                                                        |
|--------------------------------------------------|---------------------------------------------------------------|
| `bookreader/widgets/TTSPopup.kt` (+ `tts_popup.xml`) | мёртвый TTS: единственная точка входа `ttsOpen()` имела 0 вызовов |
| `bookreader/widgets/TimeAnimatorCompat.kt`       | мёртвая обёртка с Handler-циклом на 24 FPS                      |
| `fbreader/.../util/UIUtil.kt`                    | `wait()` (Thread + ProgressDialog + wait/notify) и `createExecutor()` — 0 вызовов |
| `FBReaderView.kt` — обработчики `FIND_PREVIOUS`/`FIND_NEXT` | не вызывались ни из keymap, ни из `runAction`; поиск идёт через `performSearchNext`/`performSearchPrevious` |

---

## 9. Запрещено (чек-лист при ревью)

- [ ] `GlobalScope.launch`
- [ ] `new Thread { }` / `Thread { }` в коде приложения
- [ ] `Handler(Looper...)` + хранимый `Runnable` + `postDelayed`/`removeCallbacks`
- [ ] `AsyncTask` / `Timer` / `TimerTask`
- [ ] `runBlocking` вне `ThemeRepositoryImpl`
- [ ] блокирующий вызов (не `suspend`) внутри `viewModelScope.launch`
