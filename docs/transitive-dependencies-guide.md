# Работа с транзитивными зависимостями

**Дата:** 2026-07-27
**Статус:** Руководство

---

## Обзор

Транзитивные зависимости — зависимости, которые подтягиваются автоматически через другие модули. Понимание их структуры критично для оптимизации сборки.

---

## Структура зависимостей проекта

### Уровень 1: Базовые библиотеки

Модули, от которых зависят все остальные:

```
commonarchitecture
├── Базовые классы: BaseViewModel, BaseActivity
├── Обработка ошибок: NetworkError, runCatchingHttpRequest
├── DI модули: CoroutineModule, ApiServiceModule
└── Зависимости:
    ├── hilt-android
    ├── timber
    └── retrofit

commonui
├── UI компоненты: AppCard, AppButton, AppLoadingIndicator
├── Тема: SVAPPTheme, ColorScheme
├── Менеджеры: DateFormatter
└── Зависимости:
    ├── commonarchitecture (api)
    ├── compose-bom
    ├── coil
    └── modo-compose
```

### Уровень 2: Менеджеры

```
managers
├── Тема: ThemeViewModel, ThemeRepository
├── Книги: OnBookPagerManager
└── Зависимости:
    ├── commonarchitecture
    ├── commonui
    └── api (для данных)
```

### Уровень 3: Feature-модули

```
news, books, wiki, info
├── Зависимости:
│   ├── commonarchitecture (транзитивно из managers)
│   ├── commonui (транзитивно из managers)
│   └── managers
└── Feature-специфичные зависимости
```

### Уровень 4: App

```
app
├── Зависимости:
│   ├── main (все feature-модули транзитивно)
│   └── bookreader
└── Прямые зависимости: Tracer, Hilt, Android Library
```

---

## Правила работы с транзитивными зависимостями

### Правило 1: Используйте `api` для базовых зависимостей

Когда базовый модуль предоставляет зависимость всем downstream-модулям:

```kotlin
// commonarchitecture/build.gradle.kts
dependencies {
    // Базовые зависимости для всех модулей
    api(libs.hilt.android)
    api(libs.timber)
}

// feature-module/build.gradle.kts
dependencies {
    // НЕ нужно объявлять hilt-android и timber - они приходят транзитивно
    implementation(project(":commonarchitecture"))
}
```

### Правило 2: Используйте `implementation` для внутренних зависимостей

Когда зависимость используется только внутри модуля:

```kotlin
// bookreader/build.gradle.kts
dependencies {
    // Только для bookreader, не транзитивно
    implementation(libs.axet.pdfium)
    implementation(libs.axet.djvulibre)
}
```

### Правило 3: Проверяйте транзитивность перед удалением

**Безопасный алгоритм:**

1. Определите зависимость для удаления
2. Проверьте, приходит ли она транзитивно:
   ```bash
   ./gradlew :module:dependencies --configuration debugRuntimeClasspath
   ```
3. Найдите модуль-источник транзитивной зависимости
4. Убедитесь, что версия совпадает
5. Удалите явную зависимость
6. Запустите сборку: `./gradlew :module:assembleDebug`
7. Запустите тесты: `./gradlew :module:test`
8. Проверьте KSP/Annotation processors

### Правило 4: Осторожно с KSP/KAPT

Annotation processors (Hilt, Room, Moshi) требуют особого внимания:

```kotlin
// ❌ НЕЛЬЗЯ удалять, даже если транзитивно:
ksp(libs.hilt.android.compiler)
ksp(libs.room.compiler)

// Эти процессоры должны быть объявлены явно в каждом модуле,
// где используются аннотации @HiltViewModel, @Entity, @Dao
```

### Правило 5: Версии в Version Catalog

Все версии должны быть в `libs.versions.toml`:

```toml
[versions]
hiltAndroid = "2.59.2"

[libraries]
hilt-android = { module = "com.google.dagger:hilt-android", version.ref = "hiltAndroid" }
hilt-android-compiler = { module = "com.google.dagger:hilt-android-compiler", version.ref = "hiltAndroid" }
```

---

## Типичные проблемы

### Проблема 1: "Hilt plugin applied but no hilt-android dependency"

**Причина:** Удалена явная зависимость `hilt-android` из модуля с Hilt плагином.

**Решение:**
```kotlin
// app/build.gradle.kts - ОБЯЗАТЕЛЬНО явно
implementation(libs.hilt.android)
ksp(libs.hilt.android.compiler)
```

### Проблема 2: KSP failed with PROCESSING_ERROR

**Причина:** Annotation processor не может найти классы из удалённой зависимости.

**Решение:** Проверьте все `ksp()` зависимости - они не могут быть транзитивными.

### Проблема 3: Конфликт версий

**Причина:** Разные версии одной библиотеки в транзитивных зависимостях.

**Решение:**
```kotlin
// Используйте BOM или forced versions
implementation(platform(libs.androidx.compose.bom))

// Или force версию
configurations.all {
    resolutionStrategy {
        force("com.squareup.okhttp3:okhttp:4.12.0")
    }
}
```

---

## Команды для анализа

### Просмотр дерева зависимостей модуля:

```bash
# Все зависимости
./gradlew :app:dependencies

# Только runtime зависимости
./gradlew :app:dependencies --configuration releaseRuntimeClasspath

# Найти конкретную библиотеку
./gradlew :app:dependencies --configuration releaseRuntimeClasspath | grep "retrofit"
```

### Проверка на конфликт версий:

```bash
./gradlew dependencyInsight --dependency hilt-android --configuration releaseRuntimeClasspath
```

### Анализ размера зависимостей:

```bash
./gradlew :app:assembleDebug --scan
# Откройте build/reports/dependency-analysis/
```

---

## План оптимизации зависимостей

### Шаг 1: Вынести общие зависимости в api

```kotlin
// commonarchitecture/build.gradle.kts
dependencies {
    api(libs.hilt.android)
    api(libs.timber)
    api(libs.bundles.retrofit)
}
```

### Шаг 2: Удалить дубликаты из feature-модулей

Для каждого модуля выполнить проверку и удаление.

### Шаг 3: Протестировать

```bash
# Сборка всех модулей
./gradlew assembleDebug

# Запуск тестов
./gradlew test

# Проверка lint
./gradlew lint
```

---

## Примеры оптимизации

### До оптимизации:

```kotlin
// books/build.gradle.kts
dependencies {
    implementation(project(":commonarchitecture"))
    implementation(project(":commonui"))
    implementation(project(":managers"))
    
    implementation(libs.hilt.android)
    implementation(libs.timber)
    implementation(libs.bundles.retrofit)
    implementation(libs.bundles.compose)
    implementation(libs.modo.compose)
}
```

### После оптимизации:

```kotlin
// books/build.gradle.kts
dependencies {
    implementation(project(":managers"))
    // commonarchitecture, commonui приходят транзитивно
    // hilt, timber, retrofit приходят транзитивно
    // compose, modo приходят транзитивно
    
    // Только специфика модуля
    implementation(project(":bookreader"))
    implementation(project(":models"))
}
```

---

## Примечания

- Оптимизация требует тщательного тестирования
- Начинать лучше с одного модуля за раз
- Всегда проверяйте KSP процессоры
- Документируйте изменения в коммитах