# Технический долг SV APP

**Дата создания:** 2026-07-27
**Последнее обновление:** 2026-10-18

---

## Приоритеты

- 🔴 **P0** — Критично, блокирует релиз или вызывает краши
- 🟠 **P1** — Важно, влияет на производительность или UX
- 🟡 **P2** — Средне, улучшает код и поддерживаемость
- 🟢 **P3** — Низко, косметические улучшения

---

## Миграция на Kotlin

### ✅ Завершить миграцию Java → Kotlin (выполнено 2026-08-29)

**Модуль:** bookreader
**Исходные оставшиеся файлы** (все мигрированы в Kotlin):

- ~~`app/Storage.java`~~ → `app/Storage.kt`
- ~~`widgets/ScrollWidget.java`~~ → `widgets/ScrollWidget.kt`
- ~~`widgets/FBReaderView.java`~~ → `widgets/FBReaderView.kt`

Дополнительно мигрированы ~100 файлов модуля `fbreader` (пакеты `zip`, `dict`, `book`,
`filetypes`, `fonts`, `language`, `util`, `view`, `xml`, `text`, `drm`, `encodings`).
Подробности миграции и возникшие сложности — в `bookreader/CLAUDE.md` (раздел «Миграция на Kotlin»).

> **Примечание:** в `fbreader` остаётся ~160 Java-файлов — это ядро движка FBReader
> (`ZLTextView`, `ZLTextParagraph` и др.), которое не входило в скоуп данной миграции.

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

### 🟡 P2: Зависимости не обновлены (запись «✅ Обновление версий» не соответствует факту)

**Проблема:** Запись выше «✅ Обновление версий зависимостей (2026-08-11)» ошибочно помечена
выполненной. По факту обновлён только AGP (9.3.1 ✓); остальные версии в
`gradle/libs.versions.toml` остались старыми. `./gradlew lint` (GradleDependency /
NewerVersionAvailable) на 2026-08-13 показывает:

| Зависимость | Сейчас | Доступно |
|-------------|--------|----------|
| Kotlin      | 2.2.20 | 2.4.10   |
| Core KTX    | 1.17.0 | 1.19.0   |
| Lifecycle   | 2.9.1  | 2.11.0   |
| Compose BOM | 2025.06.01 | 2026.08.00 |
| Navigation  | 2.9.1  | 2.9.8    |
| ktlint      | 12.2.0 | 14.2.0   |
| OkHttp      | 4.12.0 | 5.4.0    |
| Activity Compose | 1.10.1 | 1.13.0 |
| Material    | 1.12.0 | 1.14.0   |
| Room        | 2.7.1  | 2.8.4    |
| Hilt nav/work/compiler | 1.2.0 | 1.4.0 |
| WorkManager | 2.9.0  | 2.11.2   |
| Coil        | 3.3.0  | 3.5.0    |
| MockK       | 1.13.13 | 1.14.11 |
| Gradle      | 9.5.0  | 9.7.0    |

**Модуль:** все / `gradle/libs.versions.toml`

**Решение:** Провести реальное обновление версий — по одному, с прогоном тестов после каждого;
либо скорректировать запись «✅ Обновление версий зависимостей», чтобы она не вводила в
заблуждение.

**Риски:** Резкий апгрейд Kotlin 2.2 → 2.4 / ktlint 12 → 14 / OkHttp 4 → 5 ломает сборку —
обновлять постепенно.

---

### ✅ Лишние ABI в APK (исправлено 2026-10-11)

**Было:** В APK попадали мёртвые ABI `armeabi` (нет устройств) и `x86` (нет устройств), а также
`x86_64` (нужен только эмулятору).

**Сделано:**
- `app/build.gradle.kts` — `ndk { abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64") }`;
- `fbreader/src/main/jni/Application.mk` — `APP_ABI := armeabi-v7a arm64-v8a x86_64` (убран `x86`).

**Проверено на `:app:assembleRelease`:** в APK осталось 3 ABI (по 13 `.so`), `zipalign -c -P 16` → OK.
`x86_64` оставлен намеренно — для эмулятора на Intel Mac.

---

### ✅ Устаревшие флаги в gradle.properties (частично исправлено 2026-10-11)

**Сделано:** удалён `android.uniquePackageNames=false` (свойство удалено ещё в AGP 8.0, эффекта не
имело).

**Оставлен намеренно:** `android.experimental.enableNative16KbAlignment=true` — в AGP 9.x это уже
дефолт (флаг фактически no-op), но оставлен как страховка от случайного регресса выравнивания
нативных библиотек. Безвреден.

---

### 🟢 P3: x86_64 внешние библиотеки с выравниванием 4 KB

**Проблема:** `libdjvu.so`, `libk2pdfopt.so`, `libwillus.so` из AAR (`com.github.axet`) на `x86_64`
выровнены на 4 KB (0x1000), а не 16 KB. Теоретический риск краша на x86_64-эмуляторе с 16 KB
page size; на реальных arm64-устройствах не встречается.

**Решение:** Не критично (x86_64 — только эмулятор). При необходимости — пересобрать AAR.

---

### 🟢 P3: TODO `// TODO check voronin` в fbreader

**Файл:** `fbreader/build.gradle.kts` (блок `externalNativeBuild → ndkBuild → path`)

**Проблема:** Незакрытый TODO-маркер без описания задачи.

**Решение:** Проверить, что путь `src/main/jni/Android.mk` корректен и сборка нативных библиотек
работает, после чего убрать комментарий.

---

## Безопасность и подпись релиза

### 🟠 P1: Release keystore (SV.jks) закоммичен в git

**Проблема:** `SV.jks` (2572 байта) лежит в корне репозитория и отслеживается git
(`git ls-files` → `SV.jks`), при этом в `.gitignore` нет ни `*.jks`, ни `*.keystore`. Ключ
подписи релиза находится в системе контроля версий — любой с доступом к репозиторию может
подписывать/перевыпускать приложение.

**Модуль:** корень репозитория
**Файлы:** `SV.jks`, `.gitignore`

**Решение:**
1. Убрать из git: `git rm --cached SV.jks` и добавить `*.jks` / `*.keystore` в `.gitignore`.
2. Хранить keystore в защищённом хранилище (CI secrets / менеджер паролей).
3. Если ключ мог утечь — сгенерировать новый keystore.

**Риски:** Ключ, попавший в историю git, останется в истории даже после `git rm` — при утечке
репозитория нужен новый ключ.

---

### 🟠 P1: Хардкод API-токенов tracer в `app/build.gradle.kts`

**Проблема:** `pluginToken` и `appToken` (2 токена × 3 блока `tracer { create(...) }`) зашиты
открытым текстом в `app/build.gradle.kts` и попадают в git. Токены отчётов/крашей
(`ru.ok.tracer`) раскрыты в истории репозитория.

**Файл:** `app/build.gradle.kts` (строки 62–83)

**Решение:** Вынести токены в `local.properties` / Gradle properties / env и не коммитить:
```kotlin
tracer {
    create("defaultConfig") {
        pluginToken = providers.gradleProperty("tracer.pluginToken").get()
        appToken = providers.gradleProperty("tracer.appToken").get()
    }
}
```

**Риски:** Токены в истории git останутся — при утечке репозитория сгенерировать новые токены.

---

### 🟠 P1: Не настроена подпись release-сборки

**Проблема:** `release` buildType не имеет `signingConfig`, ни одного `signingConfigs` в проекте
нет. `SV.jks` лежит рядом, но нигде не подключён. Итог: `assembleRelease` даёт
unsigned/непубликуемый APK — релиз в RuStore невозможен без ручной подписи.

**Файл:** `app/build.gradle.kts`

**Решение:** Настроить `signingConfigs { create("release") { ... } }` с `SV.jks` (или новым
ключом), подключить его в `release`, а сам keystore хранить вне git (см. пункт про SV.jks).

**Риски:** Подпись release и debug уже различаются; пароль/alias не коммитить.

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

## Foldables и новые версии Android

**Контекст (актуально на 2026-10):** compileSdk/targetSdk = 37 (Android 17). Адаптивный UI уже есть
(`DeviceFormFactor`, `NavigationRail`, `MasterDetailLayout`), форм-фактор определяется через Jetpack
`WindowSizeClass` по размеру **окна** (не экрана). Не покрыт только hinge (`WindowLayoutInfo`/`FoldingFeature`).

### ✅ P2 (решено): `androidx.window:window-core` — WindowSizeClass внедрён

**Сделано (2026-10-11):** Форм-фактор теперь определяется через `WindowSizeClass.compute(...)` по
фактическому размеру окна (`LocalWindowInfo.current.containerSize`), а не по
`LocalConfiguration.current.screenWidthDp`. Добавлен `rememberWindowSizeClass()` в
`commonui/util/WindowSizeClassProvider.kt`; `rememberDeviceFormFactor()` маппит
`WindowWidthSizeClass` (COMPACT/MEDIUM/EXPANDED) → существующий `DeviceFormFactor`, поэтому все
существующие точки вызова (`ProvideAdaptiveDimensions` / `LocalDeviceFormFactor`) не изменились.
В split-screen/multi-window и на foldable в позе «book» форм-фактор теперь определяется по окну.

**Осталось (hinge, отложено):** `WindowLayoutInfo`/`FoldingFeature` (позиция сгиба) не используются.
Это нужно только для будущего двухстраничного режима, чтобы не размещать контент поперёк сгиба.
Подписка на `WindowInfoTracker` сейчас не заводится — нет экрана, который её использовал бы.

### 🟡 P2: Потеря позиции чтения при fold/unfold (recreate Activity)

**Проблема:** В манифестах нет `android:configChanges` и `android:screenOrientation` — приложение
resizeable (это хорошо для foldable/multi-window), но при складывании/раскладывании Activity
пересоздаётся по умолчанию, а позиция чтения/скролла не сохраняется (`savedInstanceState`/
`rememberSaveable` для неё не заведён) — после fold читалка открывается не на той странице.

**Пересечение:** «Потенциальный crash при смене конфигурации» (P2) и «PDF-страница не применяется»
(P1) в этом же документе.

**Решение:** Хранить позицию (глава/абзац/смещение) в `ReaderViewModel` (он переживает config
change) и восстанавливать после пересоздания `FBReaderView`; для прочих экранов — штатный
`rememberSaveable`.

### 🟢 P3: Predictive back (Android 16+) — жесты читалки у краёв экрана

**Проблема:** targetSdk 37 → predictive back включён принудительно. Навигация назад корректна
(`BackHandler` в `ReaderContent.kt:418-432`), но читалка обрабатывает собственные touch-жесты
(зум/пан/выделение) у краёв экрана — потенциальный конфликт с системной зоной back-жеста.

**Решение:** Ручная проверка на устройстве Android 16+ (fullscreen/zoom/search + back-жест);
убедиться, что `FBReaderView` не перехватывает системный back-жест.

### ℹ️ P3 (разобрано, не баг): `WindowInsets(0.dp)` в root-экранах — осознанный паттерн

**Вывод (2026-10-11):** `contentWindowInsets = WindowInsets(0.dp)` / `windowInsets = WindowInsets(0.dp)`
в `RootWiki`/`RootBooksCatalog`/`RootInfo`/`RootNews` — не баг, а защита от двойных отступов. Эти
четыре экрана вложены во внешний `Scaffold` в `main/.../BottomNavigationUi.kt` (`CompactNavLayout` /
`RailNavLayout`), который уже применяет системные инсеты: дефолтный `contentWindowInsets` →
`paddingValues` → `Modifier.padding(paddingValues)` на `NavHost`. Поэтому вложенные
`Scaffold`/`TopAppBar` зануляют инсеты, чтобы статус-бар не отступал дважды.

Полноэкранные экраны, открываемые поверх bottom-nav через Modo-стек (`ArticleScreen`,
`FavoritesScreen`, `BookmarksScreen`, `DownloadedBooksScreen`, `BookDetail`), корректно используют
`contentWindowInsets = WindowInsets.statusBars` — они не под внешним `Scaffold` и сами отвечают за
инсет. Разница в паттернах осознанная.

**Удалять `WindowInsets(0.dp)` из root-экранов нельзя** — это даст двойной отступ сверху (пустая
статус-барная зона над тулбаром).

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

### ✅ P1 (решено 2026-10-12): ThemeRepositoryImpl — dual persistence → один DataStore

**Было:** Класс хранил `SharedPreferences` (синхронное чтение темы при старте) параллельно с
DataStore (асинхронные Flow-читалки) — двойная запись в `setThemeMode`, две пары ключей
(`theme_mode_sync` в SP и `theme_mode` в DataStore), риск рассинхронизации.

**Сделано:** Оставлен один DataStore. Начальное значение темы читается синхронно один раз в `init`
через `runBlocking { dataStore.data.first() }` и кэшируется в `@Volatile`-поле; `ThemeViewModel`
берёт стартовое значение из кэша. `SharedPreferences` полностью убран.

**Примечание:** прежнее «решение» здесь предлагало `AppCompatDelegate.setDefaultNightMode()`, но тема
в проекте применяется через Compose `SVAPPTheme`, а не AppCompatDelegate — оно нерелевантно.
`runBlocking` — осознанное исключение (однократное чтение крошечного файла на старте, до первого
кадра), чтобы не мигать системной темой.

---

### ✅ P1 (решено 2026-10-12): bookreader — SharedPreferences в Compose → StateFlow

**Было:** `ReaderSettingsContent` (Composable) подписывался на
`SharedPreferences.OnSharedPreferenceChangeListener` через `DisposableEffect` — нарушение реактивного
паттерна.

**Сделано:** Создан `ReaderSettingsViewModel` (`@HiltViewModel`), который владеет SharedPreferences,
подписывается на изменения сам (в `init`/`onCleared`) и отдаёт `StateFlow` для `viewMode`,
`volumeKeys`, `twoColumnView`; методы `toggleViewMode()`/`setVolumeKeys()`/`cycleTwoColumnView()`.
Экран собирает их через `collectAsStateWithLifecycle()`.

**Примечание:** хранилище оставлено SharedPreferences (не DataStore, как предлагалось ранее) —
нативный код FBReader (`FBReaderView`, `Reflow`, `ConfigShadow` и др.) читает те же настройки
напрямую из `getDefaultSharedPreferences`, миграция на DataStore сломала бы этот слой.

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

### 🟠 P1: TTSPopup — Handler + Runnable (1035 строк)

**Проблема:** `Handler(Looper.getMainLooper())` + `Runnable` + `postDelayed`/`removeCallbacks`.
Реальный removable-долг — два `postDelayed`: 200мс дебаунс `updateGravity(g)` и отложенный ретрай
`speakRetry` в `ttsShowError`.

**Файл:** `bookreader/widgets/TTSPopup.kt`

**Решение (уточнено 2026-10-12):** класс НЕ Composable, поэтому прежняя рекомендация «LaunchedEffect +
Flow» неверна; «бесконечный цикл» в `updateGravity` — на деле обычный дебаунс (remove + re-post).
Нужен `CoroutineScope(SupervisorJob() + Dispatchers.Main)` + `Job` + `delay`. Часть `Runnable`-ов
убрать нельзя: внешняя библиотека `com.github.axet.androidlibrary.sound.TTS` принимает колбэки
`Runnable` (`playSpeech(..., done)`, `onError(done)`), а `onScrollFinished: ArrayList<Runnable>` —
паттерн «выполнить после завершения скролла». Дополнительно `handler.removeCallbacks(delayed)` чистит
поле `delayed` внешнего TTS — трогать вслепую рискованно. Требует ручной проверки TTS на устройстве
(на эмуляторе TTS-движок ненадёжен).

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

## Неиспользуемые ресурсы

### 🟡 P2: Мёртвые ресурсы fbreader и app

**Проблема:** Android lint (`UnusedResources`) + ручная сверка нашли неиспользуемые ресурсы:
- `fbreader`: ~67 мёртвых drawable — PNG-иконки `ic_arrow_back_*`, `ic_arrow_forward_*`,
  `ic_close_*` (~60 шт.) и 7 xml `background_kitkat_*.xml`; неиспользуемый цвет `light_gray`.
  Java-код fbreader ссылается только на `R.id.content` — ресурсы унаследованы от старого UI FBReader.
- `app`: цвета `R.color.white` и `R.color.splash_icon_tint` не используются.

**Модуль:** fbreader, app
**Файлы:** `fbreader/src/main/res/drawable*/`, `fbreader/src/main/res/values/colors.xml`,
`app/src/main/res/values/colors.xml`

**Решение:** Прогнать `./gradlew lint`, удалить найденное, проверить сборку. Осторожно с
ресурсами, на которые ссылаются по строковому имени в рантайме (`getIdentifier`).

**Риски:** Удаление ресурса, имя которого формируется динамически, ломает рантайм — перед
удалением грепать по коду.

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

## Потенциальные идеи для разработки

> Это не техдолг, а бэклог идей. Каждая идея оформлена так же, как задача, но без приоритета
> P0–P3 — приоритет решается при планировании roadmap'а.

### 💡 Настройки приложения + выбор места сохранения скачанных книг

**Идея:** Добавить в приложение экран «Настройки», а в нём — выбор, куда сохранять скачанные
книги: в общее хранилище устройства (видно пользователю в файловом менеджере, доступно другим
приложениям) либо только во внутренние файлы приложения (изолированно, удаляется вместе с
приложением).

**Модуль:** books (скачивание) + новый экран настроек (main/commonui)
**Файлы:** `books/catalog/.../BookDownload*`, `Storage.kt`/`bookreader/app/Storage.kt`

**Ключевые технические моменты (Scoped Storage, Android 10+):**

- Общее хранилище: `MediaStore.Downloads` / `StorageManager` + `MANAGE_EXTERNAL_STORAGE` (или SAF
  `ACTION_OPEN_DOCUMENT_TREE` для выбора пользователем конкретной папки) — файлы переживают удаление
  приложения, но требуют разрешений и обработки прав.
- Только файлы приложения: `context.getExternalFilesDir(...)` / `context.filesDir` — без разрешений,
  но файлы пропадают при удалении и не видны пользователю.
- Текущее поведение: книги уже скачиваются в app-specific storage — идея лишь добавляет опцию выбора.

**Риски:** разрешения и SAF-флоу усложняют UX; миграция уже скачанных книг при смене настройки;
MD5-идентификация книг (см. `Storage.java — MD5 как идентификатор`) может зависеть от пути.

### 💡 Push-уведомления через RuStore

Оповещение пользователей о новом контенте (статьи Wiki, новости VK, новые книги в каталоге).
Подробности — [[push-notifications-via-rustore]].

### 💡 Блокирующее / неблокирующее обновление приложения через RuStore

Force update (блокирует использование до обновления) и soft update (баннер-рекомендация).
Проверка минимальной/текущей версии при старте. Подробности — [[update-availability-notifications]].

**⚡ Оптимизации и производительность**

### 💡 1. Baseline Profiles

**Суть:** Скомпилированный профиль горячих путей (холодный старт, открытие читалки, скролл
новостей/каталога). Кладётся в APK/AAB как `assets/dexopt/baseline.prof`; ART AOT-компилирует
указанные методы уже при установке — холодный старт быстрее, меньше JIT-подтормаживаний и расхода
батареи.

**Инструменты:** `androidx.baselineprofile` (генерация) + `androidx.profileinstaller` (доставка) +
`androidx.benchmark.macro` (замер). Профиль генерируется бенчмарк-тестом на устройстве/эмуляторе и
кладётся в `src/release/baseline-prof.txt`.

**Трудоёмкость:** S–M

### 💡 2. AAB вместо APK (RuStore)

**Суть:** Перейти на `.aab` (Android App Bundle) + per-ABI сплиты. RuStore поддерживает AAB — на
устройство качается только нужный ABI, меньше размер. Продолжение уже сделанного ABI-cleanup.

**Трудоёмкость:** S

### 💡 3. Ленивая инициализация bookreader/fbreader

**Суть:** Нативные библиотеки и тяжёлые модули грузить не в `Application.onCreate`, а при первом
открытии книги. Ускоряет старт для пользователей, которые пришли за новостями/Wiki.

**Трудоёмкость:** M

### 💡 4. Compose stability / recomposition аудит

**Суть:** Прогнать compose compiler metrics, добавить `@Immutable`/`@Stable` на модели, включить
strong skipping. Дешёвый выигрыш на скролле каталога и новостей.

**Трудоёмкость:** M

### 💡 5. R8 full mode + ресурсный шринкинг

**Суть:** Проверить `isMinifyEnabled`/`isShrinkResources` и full mode R8 в release; заодно закрыть
P2 «мёртвые ресурсы fbreader» (~67 drawable).

**Трудоёмкость:** S

### 💡 6. Coil: кастомный disk cache + единый placeholder

**Суть:** Выставить лимиты памяти/диска под обложки книг, единый placeholder. Видимо в каталоге.

**Трудоёмкость:** S

**📖 Фичи для читалки**

### 💡 7. Полнотекстовый поиск по всем форматам

**Суть:** В PDF/DJVU поиск уже есть (`SearchResult` в плагинах). Проверить нативный FBReader-поиск
по ePub/FB2 и вывести единый UI.

**Трудоёмкость:** M–L

### 💡 8. Экспорт/импорт закладок и прогресса

**Суть:** Выгрузка в JSON/файл и восстановление. Работает без бэкенда, чисто локально.

**Трудоёмкость:** S–M

### 💡 9. OPDS-каталог

**Суть:** Подключение сторонних онлайн-библиотек. FBReader умеет это нативно — оценить, сколько
осталось в форке.

**Трудоёмкость:** M

### 💡 10. Виджет «Продолжить чтение»

**Суть:** Home-screen виджет с последней книгой и позицией (данные уже есть в
`GetLastReadBookUseCase`).

**Трудоёмкость:** S–M

### 💡 11. App shortcuts

**Суть:** Долгое нажатие на иконку → «Продолжить чтение» / «Каталог».

**Трудоёмкость:** S

### 💡 12. Ночной режим по расписанию

**Суть:** Тема уже через DataStore — добавить расписание (закат/время) поверх текущего выбора.

**Трудоёмкость:** S

### 💡 13. Авторизация + синхронизация прогресса/закладок

**Суть:** Синхронизация позиции чтения и закладок между устройствами. Требует бэкенда/аккаунта —
это продуктовое решение, не просто идея.

**Трудоёмкость:** L

**🔧 Качество и дистрибуция**

### 💡 14. In-app review + «рассказать другу»

**Суть:** RuStore review API + share-интент. Дёшево для роста аудитории.

**Трудоёмкость:** S

> Часть пунктов из бэклога (закрыть P1-безопасность, обновить зависимости, MockConfig через
> flavour, CI на PR) — это уже заведённые пункты техдолга выше, здесь не дублирую.

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