# Технический долг SV APP

**Дата создания:** 2026-07-27
**Последнее обновление:** 2026-08-12

---

## Приоритеты

- 🔴 **P0** — Критично, блокирует релиз или вызывает краши
- 🟠 **P1** — Важно, влияет на производительность или UX
- 🟡 **P2** — Средне, улучшает код и поддерживаемость
- 🟢 **P3** — Низко, косметические улучшения

---

## Миграция на Kotlin

### 🟡 P2: Завершить миграцию Java → Kotlin

**Модуль:** bookreader
**Оставшиеся файлы:**

- `app/Storage.java` (1392 строки) - наследуется от внешней Java библиотеки
- `widgets/ScrollWidget.java` (1717 строк) - много внутренних классов
- `widgets/FBReaderView.java` (2158 строк) - декомпозируется

**Сложности:**

- Storage.java наследуется от `com.github.axet.androidlibrary.app.Storage`
- Много статических методов, вызываемых из Kotlin
- Внутренние классы: `Info`, `Progress`, `Bookmark`, etc.

**Решение:**

1. Создать обёртки для статических методов
2. Добавить `@JvmStatic` для companion object методов
3. Использовать `lateinit` для lazy-инициализации

---

## Gradle и сборка

### 🟠 P1: Оптимизация транзитивных зависимостей

**Проблема:** Модули объявляют зависимости, которые приходят транзитивно.

**Действия:**

- [ ] Вынести общие зависимости в `api` блоки базовых модулей
- [ ] Удалить дублирующиеся объявления из feature-модулей
- [ ] Протестировать сборку после каждого изменения

**Риски:** KSP ошибки, missing classes

**Документация:** `docs/transitive-dependencies-guide.md`

---

### 🟠 P1: Удалить deprecated настройки AGP

**Проблема:** В `gradle.properties` есть deprecated настройки.

**Настройки для удаления (AGP 10.0):**

```properties
android.enableJetifier=true
android.defaults.buildfeatures.resvalues=true
android.sdk.defaultTargetSdkToCompileSdkIfUnset=false
android.enableAppCompileTimeRClass=false
android.usesSdkInManifest.disallowed=false
android.r8.optimizedResourceShrinking=false
```

**Уже удалены (AGP 9.0 built-in Kotlin):**

- `android.builtInKotlin=false`
- `android.newDsl=false`

**Блокирует:** Миграция на AGP 10.0

---

### ✅ Обновление версий зависимостей (2026-08-11)

Все основные зависимости обновлены до актуальных версий:

| Зависимость | Было       | Стало                |
|-------------|------------|----------------------|
| AGP         | 9.2.0      | 9.3.1                |
| Kotlin      | 2.2.20     | 2.4.10               |
| Compose BOM | 2025.06.01 | 2026.06.01           |
| Core KTX    | 1.17.0     | 1.19.0               |
| Lifecycle   | 2.9.1      | 2.11.0               |
| Hilt        | 2.59.2     | 2.60.1               |
| ktlint      | 12.2.0     | 14.2.0               |
| OkHttp      | 4.12.0     | 5.4.0                |
| Navigation  | —          | 2.9.8 (2.10.0-alpha) |

---

## Архитектура

### 🟡 P2: Объединить модуль models с books

**Проблема:** Модуль `models` содержит только 2 файла и используется только в `books`.

**Файлы:**

- `UiBook.kt`
- `UIBookState.kt`

**Решение:** Переместить файлы в модуль `books/catalog/domain/model/`

**Риски:** Минимальные

---

### 🟢 P3: Разделить модуль managers

**Проблема:** Модуль содержит разные типы менеджеров.

**Предложение:**

```
managers/
├── theme/          → theme-manager модуль
│   ├── ThemeViewModel
│   ├── ThemeRepository
│   └── CustomColorsRepository
└── books/          → data-managers модуль
    └── OnBookPagerManager
```

**Риски:** Циклические зависимости

---

## Производительность

### 🟠 P1: Оптимизация Build Features

**Проблема:** Build features включены по умолчанию во всех модулях.

**Решение:** Отключить ненужные:

```kotlin
android {
    buildFeatures {
        buildConfig = false  // Если не используется BuildConfig
        resValues = false    // Если нет generated res
        shaders = false      // Если нет OpenGL
    }
}
```

---

### 🟡 P2: Gradle оптимизации — оставшиеся

**Уже включено:**

```properties
org.gradle.parallel=true        # Параллельная сборка
org.gradle.caching=true         # Build cache
org.gradle.configuration-cache=true  # Configuration cache
```

**Ещё не включено:**

```properties
kotlin.incremental=true          # Kotlin incremental (может быть включён по умолчанию)
org.gradle.vfs.watch=true        # File system watching
```

**Риски:** Может не работать с текущей версией AGP

---

## Код и качество

### ✅ Удалить hardcoded строки в domain слое (исправлено 2026-08-12)

- `GetLastReadBookUseCase` — `"Книга"` → параметр `defaultTitle`, передаётся из `R.string.default_book_title` (main)
- `BookmarksRepository` — `"Неизвестная книга"` → `context.getString(R.string.sv_unknown_book)` (bookreader)

Обе строки вынесены в Android string resources, в domain-слое не осталось хардкод-строк.

---

### 🟢 P3: Добавить проверку зависимостей в CI

**Задача:** Автоматически проверять устаревшие зависимости.

```kotlin
// build.gradle.kts
tasks.register("checkDependencies") {
    dependsOn("dependencyUpdates")
}
```

---

## Адаптивный UI для планшетов

### ✅ Краш PDF на планшете (решено 2026-08-01)

**Была проблема:** При открытии PDF книги на планшете происходил краш в нативной библиотеке.

**Модуль:** bookreader / fbreader
**Ошибка:** `Fatal signal 5 (SIGTRAP)` в `libmodpdfium.so`

**Стек:**

```
#00 pc 0000000000333e2c  libmodpdfium.so
#01 pc 0000000000335f94  libmodpdfium.so
...
```

**Найденная причина:** `com.github.axet:pdfium:2.0.16` (последняя существующая версия) тянет
`libmodpdfium.so`, собранную с ELF page-alignment **4 KB** (`objdump -p` → `align 2**12`).
Планшеты всё чаще работают с **16 KB page size** (обязательное требование Android 15+ для
Google Play с ноября 2025) — при несовпадении page size нативный аллокатор pdfium падает по
CHECK-assert → `SIGTRAP`. У мейнтейнера библиотеки на GitLab с июля 2024 висит открытый,
не исправленный issue "Requesting for 16kb alignment". Более новой версии `axet/pdfium` не
существует, апгрейд был невозможен.

Более ранние попытки чинили это эвристикой "экран ≥7 дюймов → Android PdfRenderer вместо
pdfium" — эвристика лечила симптом не для той причины (реальный триггер — page size
устройства, а не диагональ экрана) и попутно ломала выделение текста, поиск, TOC и
scroll/two-column режим на затронутых устройствах.

**Решение:** Заменили `com.github.axet:pdfium` на `io.legere:pdfiumandroid:2.0.0` (обёртка над
современной сборкой Google PDFium). Проверено напрямую через `objdump -p` на `.so` из `.aar`:
`libpdfium.so`/`libpdfiumandroid.so` собраны с `align 2**14` (16 KB) на всех ABI. Дополнительно
включили `android.experimental.enableNative16KbAlignment=true` в `gradle.properties`, чтобы
сама упаковка APK тоже выравнивала нативные библиотеки на 16 KB (проверено на собранном
`app-release-unsigned.apk`: смещение данных `.so` внутри zip кратно 16384 для каждого ABI).

Весь PDF-код на pdfium теперь снова единый путь (без tablet/PdfRenderer-развилки) —
`bookreader/src/main/java/com/github/axet/bookreader/app/PDFPlugin.kt`. Полный функционал
(выделение, поиск, TOC) сохранён на всех устройствах.

**Дополнительный фикс:** после замены библиотеки открытие PDF перестало падать, но всплыл второй
краш при создании закладки — сначала `IllegalStateException: Already closed` (поймали конфигом
`AlreadyClosedBehavior.IGNORE`), затем `SIGBUS` прямо в `FPDF_ClosePage` (реальный double-free:
`Selection`/`SelectionPage` намеренно шарит один и тот же `PdfPage`/`PdfTextPage` между
`startPage`/`endPage`/кэшем `map`, и закрывал его больше одного раза — старый pdfium это терпел
молча, новый — нет). Исправлено в `Selection.close()`: закрывать native-ресурсы ровно один раз,
только через `map`.

**Подтверждено на реальном 16KB-устройстве** (эмулятор `sdk_gphone16k_arm64`, тот самый, где
воспроизводился баг): открытие PDF, выделение текста и создание нескольких закладок подряд —
без крашей.

---

### 🟡 P2: Двухстраничный режим читалки

**Проблема:** Читалка книг не поддерживает режим "книжного разворота" для планшетов.

**Модуль:** bookreader
**Файлы:**

- `widgets/ScrollWidget.java` — legacy Java, требует декомпозиции
- `widgets/FBReaderView.java` — legacy Java
- `screens/ReaderScreen.kt` — интеграция

**Решение:**

1. Создать `TwoPageLayout.kt` компонент
2. Модифицировать ScrollWidget для отображения двух страниц
3. Добавить настройку в ReaderSettings для включения/выключения
4. Активировать только для Expanded + landscape

**Сложности:**

- Требует изменений в legacy Java коде (ScrollWidget, FBReaderView)
- Нужно синхронизировать скролл между двумя страницами
- Управление состоянием (текущая страница, режим)

**Риски:** Может сломать существующий функционал чтения

---

### 🟡 P2: Wiki Master-detail layout

**Проблема:** На планшетах можно показать список избранного слева и статью справа.

**Модуль:** wiki
**Файлы:**

- `root/RootWiki.kt` — главный экран
- `presentation/favorites/FavoritesScreen.kt` — список избранного
- `presentation/article/ArticleScreen.kt` — статья

**Решение:**

1. Использовать `MasterDetailLayout` из commonui
2. Создать адаптивную версию RootWiki для Expanded
3. Слева: список избранного (35%)
4. Справа: статья (65%)
5. Синхронизация выбора с навигацией

**Риски:**

- Усложнение навигации (Modo + внутренний state)
- Управление состоянием выбора статьи
- Требует изменения ViewModel для поддержки двухпанельного режима

**Статус:** Отложено - требует значительной переработки навигации

---

### 🟡 P2: Bookmarks Master-detail layout

**Проблема:** На планшетах можно показать список заметок слева и предпросмотр справа.

**Модуль:** books/bookreader
**Файлы:**

- `bookreader/screens/ui/BookmarksComposeDialog.kt`
- `books/catalog/presentation/bookmarks/`

**Решение:**

1. Master-detail layout для Expanded
2. Слева: список заметок с текстом
3. Справа: навигация к заметке или предпросмотр

**Статус:** Отложено - требует переработки навигации

---

### 🟡 P2: DownloadedBooks адаптивный layout

**Проблема:** Список скачанных книг не оптимизирован для планшетов.

**Модуль:** books
**Файлы:**

- `books/catalog/presentation/downloaded/ui/DownloadedBooksScreen.kt`
- `DownloadedBookItem.kt`

**Решение:**

1. Заменить список на плитку (grid) для планшетов
2. Адаптивный layout для удаления (swipe → долгое нажатие или кнопка)
3. На телефонах сохранить текущий список со swipe-to-delete

---

### 🟡 P2: BugReport адаптивный layout

**Проблема:** Поля и кнопки на весь экран планшета - неудобно.

**Модуль:** bugreport
**Файлы:**

- `bugreport/presentation/nav/BugReportScreen.kt`
- `bugreport/presentation/bugreport/ui/BugReportContent.kt`

**Решение:**

1. Ограничить ширину формы (maxWidth = 600dp)
2. Компактный layout для планшетов
3. Центрировать форму на экране

---

## Документация

### 🟢 P3: Обновить ARCHITECTURE.md

**Проблема:** Файл не обновлялся с момента создания.

**Действия:**

- [ ] Проверить актуальность схемы
- [ ] Добавить новые модули
- [ ] Обновить диаграммы

---

## Выполненные задачи

### ✅ Non-suspend Use Cases + Hardcoded строки + Timber (2026-08-12)

- **GetLastReadBookUseCase**: добавлен `suspend`, I/O через `withContext(dispatcherProvider.io)`, `"Книга"` → `R.string.default_book_title`
- **BookmarksRepository**: `"Неизвестная книга"` → `R.string.sv_unknown_book`
- **Timber**: подтверждено, что `CustomColorsRepositoryImpl`, `ContinueReadingViewModel`, `PDFPlugin` уже используют Timber
- **Ложные срабатывания**: QA/Wiki use cases возвращают `Flow` — не требуют `suspend`

### ✅ Вынос хардкод-строк в ресурсы (2026-08-12)

- **GetInfoLinksUseCase** (`info`): 9 текстов ссылок + 9 URL → `info/strings.xml`
- **WikiRepositoryImpl** (`wiki`): `"Ошибка сети"`, `"Неизвестная ошибка"`, `"Пустой ответ от сервера"` → `wiki/strings.xml`
- **GetBookFiltersUseCase** (`books`): `"Все"` → `R.string.books_filter_all`
- **MasterDetailLayout** (`commonui`): `"Выберите элемент для просмотра"` → `R.string.master_detail_select_hint`
- **DownloadedBooksList** (`books`): `"Удалить книгу"` → `R.string.books_downloaded_delete_content_description`

Строки читаются через `ResourcesRepository` (commonarchitecture), а не напрямую через `context.getString`.

### ✅ Lint исправления (2026-07-26)

- Исправлено 16 критических ошибок Range
- Исправлено 91 предупреждение DefaultLocale
- Исправлено NewApi для Splash Screen
- Добавлено POST_NOTIFICATIONS permission

### ✅ Безопасный рефакторинг (2026-07-26)

- Expression body для простых функций
- Упрощение redundant let/when
- Удалено 18 строк кода

### ✅ Строковые ресурсы (2026-07-26)

- Вынесены все user-visible строки
- Добавлены строковые ресурсы в bugreport, bookreader

### ✅ Документация модулей (2026-07-26)

- Актуализированы все 15 CLAUDE.md файлов
- Удалены устаревшие ссылки
- Добавлены новые компоненты

### ✅ AGP 9.0 built-in Kotlin + общий конфиг + тесты (2026-08-11)

- **AGP 9.0 built-in Kotlin миграция:**
    - Убран `kotlin-android` плагин из всех 16 модулей
    - Убраны `android.builtInKotlin=false` и `android.newDsl=false` из `gradle.properties`
    - JVM target задаётся верхнеуровневым блоком `kotlin { compilerOptions { jvmTarget } }` (
      предоставляется AGP)
    - Исправлен `Plugin.Page` → `PluginPage` (2 Java файла)
    - Исправлен `manualResumePause` → убран (modo 0.12.0)
- **Единые SDK-версии:** `buildSrc/ProjectConfig.kt` (compileSdk=37, minSdk=24, targetSdk=37)
- **Общий Android-конфиг:** вынесен в `subprojects` корневого `build.gradle.kts` — убрано 11
  `apply(from = ...)` и удалён `android_feature_commons.kts`
- **Стабильный API:** `androidResources.localeFilters.add()` (@Incubating) →
  `resourceConfigurations += "ru"`
- **Тесты:**
    - `WikiRepositoryImplTest` — исправлены именованные параметры `getPage(title=)` и
      `search(query=, what=)`
    - `commonui` — добавлен `testImplementation(libs.bundles.test)`
- **Версии:** ktlint 14.2.0, OkHttp 5.4.0, material-icons-extended добавлен в 3 модуля, timber в 1
  модуль

### ✅ Настройки Gradle (2026-08-11)

- `resConfigs("ru")` — только русская локализация

### ✅ Адаптивный UI для планшетов (2026-07-27)

- Добавлена зависимость `androidx.window:window-core:1.3.0`
- Создан `DeviceFormFactor` sealed class (Compact/Medium/Expanded)
- Создан `AdaptiveDimensions` для размеров по форм-фактору
- Реализована адаптивная навигация:
    - Compact: BottomNavigation (без изменений)
    - Medium/Expanded: NavigationRail
- Адаптивные колонки в каталоге книг (2/3/4)
- Master-detail компонент для планшетов
- Ограничение ширины контента (Wiki, News, Info)
- Документация: `docs/ADAPTIVE_UI_ARCHITECTURE.md`

### ✅ FullScreenImageViewer (2026-07-28)

- Компонент для полноэкранного просмотра изображений
- HorizontalPager для свайпа между изображениями
- Pinch-to-zoom и pan для масштабирования
- Двойной тап для быстрого зума
- Индикатор страниц и счётчик
- Интеграция в модуль News

### ✅ BugReport адаптивный layout (2026-07-28)

- Ограничение ширины формы (max 600dp)
- Центрирование на планшетах
- Использование LocalAdaptiveDimensions

### ✅ DownloadedBooks адаптивный layout (2026-07-28)

- Grid layout для планшетов (LazyVerticalStaggeredGrid)
- Кнопка удаления в карточке для планшетов
- Долгое нажатие для удаления на планшетах
- Сохранён swipe-to-delete для телефонов

---

## SharedPreferences → DataStore (двойная персистентность)

### 🟠 P1: ThemeRepositoryImpl — dual persistence (SharedPreferences + DataStore)

**Проблема:** Класс хранит `SharedPreferences` для синхронных читалок (тема до создания Activity) и
отдельный DataStore для асинхронных Flow-читалок. Дублируются записи и поддержка двух слоёв.

**Файлы:**

- `managers/src/main/java/su/sv/managers/theme/ThemeRepositoryImpl.kt` (~40-60)

**Решение:** Оставить один DataStore. Синхронное чтение темы вынести в `Application.onCreate()` —
читать DataStore до `Activity`, устанавливать `AppCompatDelegate.setDefaultNightMode()` до
`super.onCreate()`.

---

### 🟠 P1: bookreader — SharedPreferences в Compose (OnSharedPreferenceChangeListener)

**Проблема:** `ReaderSettingsContent` — Composable-компонент — подписывается на
`SharedPreferences.OnSharedPreferenceChangeListener` через `DisposableEffect`. Это нарушение
реактивного паттерна: Compose требует `StateFlow`/`Flow`, а SharedPreferences даёт коллбеки.

**Файлы:**

- `bookreader/screens/ReaderSettingsContent.kt` (~50-100)
- `bookreader/app/BookReaderInitializer.kt`
- `bookreader/widgets/TTSPopup.kt` (~140)
- `bookreader/app/Reflow.kt`
- `bookreader/screens/viewmodel/ReaderViewModel.kt`

**Решение:** Мигрировать `getDefaultSharedPreferences` на `preferencesDataStore`, заменить
`OnSharedPreferenceChangeListener` на `collectAsStateWithLifecycle()`.

---

### 🟡 P2: BadgeManager — SharedPreferences вместо DataStore

**Файл:** `main/src/main/java/su/sv/main/badge/BadgeManager.kt` (строки 14, 17)

**Проблема:** `context.getSharedPreferences()` для хранения одного boolean-флага.

---

### 🟡 P2: BooksApiModule выдаёт SharedPreferences через Hilt

**Файл:** `books/src/main/java/su/sv/books/catalog/di/BooksApiModule.kt`

**Проблема:** DI-модуль предоставляет SharedPreferences вместо DataStore.

---

## Handler/Runnable → Coroutines

### 🟠 P1: TTSPopup — Handler + Runnable (971 строка)

**Проблема:** Много `Handler(Looper.getMainLooper())`, `Runnable`, `postDelayed`, `removeCallbacks`.
Метод `updateGravity` (строка ~551) создаёт бесконечный цикл через `postDelayed`. Класс использует
`java.util.Arrays`, `Collections` вместо Kotlin stdlib.

**Файл:** `bookreader/widgets/TTSPopup.kt`

**Решение:** Конвертировать в `LaunchedEffect` + Flow с `kotlinx.coroutines.delay`.

**Дополнительно:**

---

### 🟡 P2: TimeAnimatorCompat — Handler.postDelayed для анимации

**Файл:** `bookreader/widgets/TimeAnimatorCompat.kt` (строки 5, 15-18)

**Проблема:** Handler-based animation loop на 24 FPS.

**Решение:** `LaunchedEffect(remember { true })` + `delay(1000L / 24)`.

---

## android.util.Log → Timber

### ✅ Use android.util.Log вместо Timber (исправлено ранее)

**Файлы (все уже используют Timber):**

- `managers/src/main/java/su/sv/managers/theme/CustomColorsRepositoryImpl.kt` — `Timber.e()`, `Timber.tag(TAG).w/d()`
- `main/src/main/java/su/sv/main/continuereading/ContinueReadingViewModel.kt` — `Timber.tag(TAG).d()`
- `bookreader/app/PDFPlugin.kt` — `Timber.tag(TAG).e()`

Все три файла уже мигрированы на Timber.

---

## SparseArray → Kotlin Map

### 🟢 P3: SparseArray в Java-style коде

**Файлы:**

- `bookreader/app/PDFPlugin.kt` — `SparseArray<SelectionPage>`,
  `SparseArray<ArrayList<SearchResult>>`
- `bookreader/app/DjvuPlugin.kt` — `SparseArray<Page>`, `SparseArray<SelectionPage>`,
  `SparseArray<DjvuSearchPage>`

**Решение:** Заменить на `Map<Int, T>` или `List<T>` с индексированным доступом.

---

## TODO/Stubs в коде

### 🟠 P1: MockConfig — моки отключены для release

**Файл:** `commonarchitecture/src/main/java/su/sv/commonarchitecture/mock/MockConfig.kt`

**Проблема:** TODO-комментарий: «Не забудьте выключить моки перед release сборкой!». Нет
автоматического выключения моков через flavour/build-variant.

**Риск:** Моки могут попасть в production-сборку.

---

### 🟠 P1: RootNews — TODO open news (functional gap)

**Файл:** `news/src/main/java/su/sv/news/presentation/root/ui/RootNews.kt` (строка 193)

**Проблема:** Effect `NewsListOneTimeEffect.OpenNewsItem` триггерится, но не реализован — ничего не
происходит при нажатии на элемент новостей.

---

### 🟡 P2: GetInfoLinksUseCase — stub с искусственной задержкой

**Файл:** `info/src/main/java/su/sv/info/domain/GetInfoLinksUseCase.kt` (строка 20)

**Проблема:** `delay(500.milliseconds)` для симуляции сетевого запроса. Заготовка под бэкенд без
репозитория.

**Решение:** Либо реализовать репозиторий, либо удалить stub.

---

### 🟡 P2: GetLastReadBookUseCase — non-suspend use case блокирует корутину

**Файл:** `bookreader/domain/GetLastReadBookUseCase.kt`

**Проблема:** `operator fun invoke(): LastReadBookInfo?` без `suspend`. Вызывается из
`viewModelScope.launch { }` — блокирует корутину.

---

### 🟢 P3: Badge support TODO в Navigation

**Файлы:**

- `commonui/src/main/java/su/sv/commonui/ui/adaptive/navigation/RailNavigation.kt`
- `commonui/src/main/java/su/sv/commonui/ui/adaptive/navigation/CompactNavigation.kt`

**Проблема:** `// TODO: Добавить поддержку badge` — `BadgeManager` не интегрирован в навигацию.

---

### 🟢 P3: ReaderSettingsContent — нереализованные настройки

**Файл:** `bookreader/screens/ReaderSettingsContent.kt` (строки 178-182)

**Проблема:** TODO-list с тремя нереализованными настройками: sync folder, TTS language, screen
lock.

---

##findViewById (не ViewBinding не Compose)

### 🟡 P2: TTSPopup — 4× findViewById

**Файл:** `bookreader/widgets/TTSPopup.kt` (строки 228-236)

**Решение:** ViewBinding (`TtsPopupBinding.inflate`).

---

### 🟢 P3: LoadableResultDialog — findViewById в AlertDialog

**Файл:** `commonui/src/main/java/su/sv/commonui/ui/LoadableResultDialog.kt` (строка 23)

**Решение:** М3 `AlertDialog` в Compose.

---

## Архитектурные потенциальные проблемы

### ✅ Non-suspend Use Cases — GetLastReadBookUseCase (исправлено 2026-08-12)

**Файл:** `bookreader/domain/GetLastReadBookUseCase.kt`

**Что сделано:**
- `operator fun invoke()` теперь `suspend` — не блокирует корутину
- I/O операции (`Storage.list()`) выполняются через `withContext(dispatcherProvider.io)`
- Хардкод-строка `"Книга"` вынесена в константу `DEFAULT_BOOK_TITLE`

**Ложные срабатывания (не требуют исправления):**
- `ObserveAnsweredQuestionsUseCase` — возвращает `Flow`, не выполняет блокирующих операций
- `ObserveAnsweredQuestionsForBookUseCase` — возвращает `Flow`
- `GetFavoritesUseCase` — возвращает `Flow`
- `GetHistoryUseCase` — возвращает `Flow`

Возврат `Flow` сам по себе не блокирует корутину — Flow ленивый и исполняется только при сборе.

---

### 🟡 P2: lateinit без safe-инициализации

**Файлы:**

- `app/src/main/java/su/sv/app/MainActivity.kt` — `lateinit var customColorsRepository`
- `books/.../BookDownloadBroadcastReceiver.kt` — два `lateinit var`

**Проблема:** Риск NPE в тестах.

---

### 🟡 P2: Потенциальный crash при смене конфигурации

**Файл:** `bookreader/CLAUDE.md` — `var fbReaderView: FBReaderView? = null` в `ReaderViewModel`

**Проблема:** Ссылка на View в ViewModel — при ротации экрана ViewModel остаётся, но контекст может
измениться. `ViewModel.onCleared()` освобождает ресурсы, но если `onSavedInstanceState` срабатывает
преждевременно — возможна утечка.

**Риск:** Memory leak при конфигурационных изменениях.

---

### 🟡 P2: Storage.java — MD5 как идентификатор книги

**Файл:** `bookreader/app/Storage.java`

**Проблема:** Обновление файла = смена MD5 ID = потеря истории чтения. Также обложки ищутся по MD5 в
нескольких местах — fragile если логика кэширования изменится.

---

## Потенциальные баги (edge cases)

### 🟠 P1: PDF-страница не применяется при открытии заметки

**Файл:** `bookreader/screens/ReaderContent.kt` (~358-393)

**Проблема:** `gotoPosition()` вызывается до инициализации `FBReaderView`. Есть fallback-логика (
`factory` + `update`), но в некоторых сценариях (первое открытие) позиция не применяется.

**Статус:** Частично исправлен fallback, но не полностью.

---

### 🟡 P2: Fullscreen flicker — race condition

**Файл:** `bookreader/widgets/FBReaderView.java`, `screens/ReaderContent.kt`

**Проблема:** `Window.decorView.setBackgroundColor()` + `AnimatedVisibility(fadein/fadeout)` не
синхронизированы. На некоторых устройствах белый/чёрный flash перед появлением/скрытием TopBar.

---

### 🟡 P2: SELECTION_DEBOUNCE — race condition панели выделения

**Файл:** `bookreader/screens/viewmodel/ReaderViewModel.kt`

**Проблема:** Debounce 500мс для скрытия панели из-за асинхронности FBReader. Если пользователь
быстро скрывает выделение после создания (<500ms), панель не закроется.

---

### 🟡 P2: На планшетах PdfRenderer.Page не закрывается после draw

**Файл:** `bookreader/app/PDFPlugin.kt`

**Проблема:** Страница закрывается только в начале следующего `draw()`. Если `getPageRect()` в
`PagerWidget` вызывается между `draw` вызовами — может использовать уже закрытую страницу.

---

### 🟢 P3: Xiaomi/MIUI — не обновляются цвета при смене темы

**Файл:** `memory/xiaomi-miui-theme-fix.md`

**Проблема:** На некоторых Xiaomi активность перезапускается при смене темы, цвета не успевают
обновиться.

---

## Утечки памяти (Memory Leaks)

### ✅ LeakCanary в debug-сборке

**Статус:** Добавлено 2026-08-08

**Что сделано:**

- Зависимость: `com.squareup.leakcanary:leakcanary-android:2.14` (только debug)
- `app/src/debug/AndroidManifest.xml` — `android:name=".DebugSvApp"`
- `app/src/debug/…/DebugSvApp.kt` — инициализация LeakCanary + дублирует Hilt/Coil от SvApp
- `app/build.gradle.kts` — `debug { applicationIdSuffix = ".debug" }` и
  `debugImplementation(libs.leakcanary.android)`
- Документация: `docs/memory-leak-detection.md`

**Как использовать:**

1. Собрать/debug APK (`./gradlew assembleDebug`)
2. Установить на устройство
3. Приложение запускается как `su.sv.app.debug` (вместо `su.sv.app`)
4. При утечке Activity — уведомление с heap dump и MAL/Studio Profiler

**Ограничения:**

- LeakCanary автоматически ловит только Activity leaks после destroy()
- Не интегрирован в.instrumentation-тесты (test runner держит референс на Activity)

---

### 🟠 P1: UI-тесты на утечки памяти

**Файл:** `app/src/androidTest/java/su/sv/app/memory/MemoryLeakTest.kt`

**Проблема:** UI-тесты `androidTest` не могут использовать LeakCanary напрямую — `Instrumentation`
держит ссылку на Activity, даже после `finish()`. Heap dump в андроид-тесте взять нельзя.

**Статус:** Заготовка создана, требует Robolectric или manual MAT-анализа.

**План реализации:**

1. **Рекомендуемый путь — Robolectric** (JVM-тесты, без Instrumentation interference):
   ```kotlin
   @RunWith(RobolectricTestRunner::class)
   class ReaderScreenLeakTest {
       @Test
       fun `open and close reader should not leak`() {
           val activity = Robolectric.buildActivity(MainActivity::class.java)
               .create().resume().get()
           // открыть книгу → закрыть
           activity.finish()
           Robolectric.processSync()
           // проверить через LeakCanary или GC
       }
   }
   ```

2. **Альтернатива — ручная проверка** на эмуляторе:
    - Запустить debug APK
    - Открыть читалку → закрыть ← ждать 3 сек
    - Проверить уведомление LeakCanary на предмет утечек

**Что тестировать (очереди):**

- [ ] Открытие читалки → закрытие (bookreader)
- [ ] Навигация по Modo stack (назад-вперёд → проверка stack clean)
- [ ] Bookmark creation/deletion
- [ ] Тест скачивания книги → отмена → повторное скачивание
- [ ] Тест переключения темы (recreate MainActivity)
- [ ] News infinite scroll (LazyColumn/Flow — no stale references)
- [ ] TTS popup open → close
- [ ] Selection panel open → close (race condition SELECTION_DEBOUNCE)
- [ ] Fullscreen toggle (enter/exit → race condition в toggleFullscreen)

**Зависимости:**

- Добавить `org.robolectric:robolectric` для JVM-тестов
- Настроить Robolectric config (`sdk = [34]`, `qualifiers = "w1080dp-h1920dp"` для coverage)

---

### 🟠 P1: Фактические утечки в коде (требуется ручная проверка)

**Места, где утечки вероятнее всего:**

1. **`ReaderViewModel.fbReaderView`** — View reference в ViewModel.
   `ViewModel.onCleared()` освобождает ресурсы, но при ротации экрана — risk.
   Файл: `bookreader/screens/viewmodel/ReaderViewModel.kt`

2. **`TTSPopup`** — 971 строка с Handler/Runnable. `updateGravity` создаёт бесконечный `postDelayed`
   цикл. Если Popup не закрыт корректно — утечка Handler + Runnable.
   Файл: `bookreader/widgets/TTSPopup.kt`

3. **`OneTimeEffect` в MainActivity** — `recreate()` при смене темы. Если эффект срабатывает
   дважды — двойной recreate.

4. **`Modeo.onRootScreenFinished`** в `MainActivity.onDestroy()` — если `recreate()` вызывается в
   процессе, ` onDestroy` не вызывает `onRootScreenFinished` (проверка `isFinishing`). Но после
   `recreate()` — ViewModel уничтожаются и пересоздаются, Modo stack должен быть clean.

---

## Что уже современное (хорошие новости)

- **Нет AsyncTask** нигде в основном коде
- **Нет Picasso/Glide** — используется Coil
- **Нет System.out.println**
- **viewModelScope** используется консистентно во всех ViewModels
- **Dispatchers.IO** только в `BugReportViewModel` — изолирован правильно
- **Paging 3** используется (news module)
- **Hilt DI** настроен правильно (@AndroidEntryPoint, @HiltViewModel, @Inject)
- **@Parcelize** используется (UiBook.kt)
- **Sealed classes** —广泛用于 state/actions/effects (MVI pattern)
- **Jetpack Compose** — основной UI-паттерн
- **Нет runBlocking** в main source
- **Нет synchronized/@Synchronized**
- **Нет магических чисел** — большинство значений вынесены в именованные константы
- **Нет manual Adapter/ViewHolder** — Paging 3 + Lazy lists

---

## Как добавить задачу

1. Определите приоритет (P0-P3)
2. Опишите проблему и влияние
3. Укажите модуль/файлы
4. Опишите возможное решение
5. Укажите риски
6. Добавьте в нужный раздел выше

---

## Шаблон задачи

```markdown
### 🟡 P2: Название задачи

**Проблема:** Описание проблемы.

**Модуль:** Название модуля
**Файлы:** Список файлов

**Решение:** Как решить.

**Риски:** Возможные проблемы.
```