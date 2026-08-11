# Gradle KTS Improvements

**Дата:** 2026-07-26
**Статус:** Рекомендации (без breaking changes)

---

## Текущая организация

Проект использует хорошую практику:

- ✅ `android_feature_commons.kts` - общие настройки для library модулей
- ✅ Version catalogs (`libs.versions.toml`) - централизованное управление версиями
- ✅ Bundles - группы зависимостей (compose, retrofit, coil, room)

---

## Рекомендуемые улучшения

### 1. Расширить `android_feature_commons.kts`

Добавить оптимизации build features:

```kotlin
android {
    compileSdk = 37

    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }

    // Добавить: отключить ненужные build features
    buildFeatures {
        buildConfig = false  // Если не используется BuildConfig
        resValues = false    // Если не используются generated res values
        shaders = false      // Если нет OpenGL шейдеров
    }
}
```

### 2. Создать дополнительные общие файлы

**`android_compose_commons.kts`:**

```kotlin
android {
    buildFeatures {
        compose = true
    }

    // Оптимизация compose
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.15"
    }
}
```

**`android_hilt_commons.kts`:**

```kotlin
dependencies {
    // DI - общие зависимости
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
}
```

### 3. Создать файл с общими зависимостями

**`common_dependencies.kts`:**

```kotlin
dependencies {
    // Logging - используется везде
    implementation(libs.timber)

    // Compose - базовый набор
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.bundles.compose)

    // Navigation
    implementation(libs.modo.compose)

    // Image loading
    implementation(libs.bundles.coil)
}
```

### 4. Упростить build.gradle.kts в модулях

**Было:**

```kotlin
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    id("kotlin-parcelize")
}

apply(from = "${project.rootDir}/android_feature_commons.kts")

android {
    namespace = "su.sv.books"
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":commonarchitecture"))
    implementation(project(":commonui"))
    implementation(project(":managers"))

    implementation(libs.bundles.coil)
    implementation(libs.bundles.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.material.icons.extended)
    debugImplementation(libs.androidx.ui.tooling)

    implementation(libs.timber)

    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    implementation(libs.modo.compose)
    implementation(libs.bundles.retrofit)
}
```

**Станет:**

```kotlin
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    id("kotlin-parcelize")
}

apply(from = "${project.rootDir}/android_feature_commons.kts")
apply(from = "${project.rootDir}/android_compose_commons.kts")
apply(from = "${project.rootDir}/common_dependencies.kts")

android {
    namespace = "su.sv.books"
}

dependencies {
    // Только специфичные для модуля зависимости
    implementation(project(":bookreader"))
    implementation(project(":models"))
    implementation(libs.bundles.retrofit)
}
```

### 5. Добавить задачу для проверки зависимостей

В корневой `build.gradle.kts`:

```kotlin
// Задача для поиска устаревших зависимостей
tasks.register("checkDependencies") {
    group = "verification"
    description = "Check for outdated dependencies"

    doLast {
        exec {
            commandLine("./gradlew", "dependencyUpdates")
        }
    }
}
```

### 6. Улучшить libs.versions.toml

Добавить документацию:

```toml
[versions]
# Android Gradle Plugin
agp = "9.2.0"  # AGP version

# Kotlin
kotlin = "2.2.20"  # Kotlin version

# Compose
composeBom = "2025.06.01"  # Compose BOM version

# Network
retrofit = "3.0.0"  # Retrofit version

# DI
hiltAndroid = "2.59.2"  # Hilt version
```

---

## Преимущества

| Улучшение              | Выгода                              |
|------------------------|-------------------------------------|
| Общие build features   | Меньше дублирования, быстрее сборка |
| Вынесенные зависимости | Легче обновлять версии              |
| Документация в toml    | Проще понимать структуру            |
| Задачи проверки        | Автоматизация проверки              |

---

## Примечания

- Все изменения обратимы
- Не требуют миграции AGP
- Можно внедрять постепенно