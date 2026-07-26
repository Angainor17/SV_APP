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