# Технический долг SV APP

**Дата создания:** 2026-07-27
**Последнее обновление:** 2026-07-27

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
android.builtInKotlin=false
android.newDsl=false
android.r8.optimizedResourceShrinking=false
```

**Блокирует:** Миграция на AGP 10.0

---

### 🟡 P2: Обновить версии зависимостей

**Текущие версии vs актуальные:**

| Зависимость | Текущая | Актуальная |
|-------------|---------|------------|
| AGP | 9.2.0 | 9.3.1 |
| Kotlin | 2.2.20 | 2.4.10 |
| Compose BOM | 2025.06.01 | 2026.06.01 |
| Core KTX | 1.17.0 | 1.19.0 |
| Lifecycle | 2.9.1 | 2.11.0 |
| Hilt | 2.59.2 | 2.60.1 |

**Риски:** Breaking changes, API изменения

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

### 🟡 P2: Включить Gradle оптимизации

**Не включены:**
```properties
org.gradle.parallel=true          # Параллельная сборка
org.gradle.caching=true           # Build cache
org.gradle.vfs.watch=true         # File system watching
kotlin.incremental=true           # Kotlin incremental
```

**Риски:** Может не работать с текущей версией AGP

---

## Код и качество

### 🟡 P2: Удалить hardcoded строки в domain слое

**Проблема:** Строки "Книга", "Неизвестная книга" в domain слое.

**Файлы:**
- `GetLastReadBookUseCase.kt:81` — "Книга"
- `BookmarksRepository.kt:141` — "Неизвестная книга"

**Решение:** Передавать строки из presentation слоя через параметры или StringProvider

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

### 🔴 P0: Краш PDF на планшете

**Проблема:** При открытии PDF книги на планшете происходит краш в нативной библиотеке.

**Модуль:** bookreader / fbreader
**Ошибка:** `Fatal signal 5 (SIGTRAP)` в `libmodpdfium.so`

**Стек:**
```
#00 pc 0000000000333e2c  libmodpdfium.so
#01 pc 0000000000335f94  libmodpdfium.so
...
```

**Возможные причины:**
- Проблема с размером экрана/буфером
- Несовместимость PDF плагина с планшетами
- Проблема с memory allocation

**Решение:**
1. Проверить на другом PDF файле
2. Обновить axet/pdfium версию
3. Добавить fallback для планшетов

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

### ✅ Локализация (2026-07-27)

- Настроена локализация только на русский: `resConfigs("ru")`

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