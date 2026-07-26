# Gradle Build Improvements

**Дата:** 2026-07-26
**Статус:** Рекомендации (применить после миграции на AGP 10.0+)

---

## Текущее состояние

Проект использует:
- **AGP:** 9.2.0
- **Kotlin:** 2.2.20
- **Gradle:** 9.4.1

Включены:
- Configuration cache ✅
- Parallel GC ✅

---

## Рекомендуемые улучшения

### 1. Удалить deprecated настройки (AGP 10.0)

При миграции на AGP 10.0 удалить из `gradle.properties`:

```properties
# Удалить (deprecated):
android.enableJetifier=true
android.defaults.buildfeatures.resvalues=true
android.sdk.defaultTargetSdkToCompileSdkIfUnset=false
android.enableAppCompileTimeRClass=false
android.usesSdkInManifest.disallowed=false
android.builtInKotlin=false
android.newDsl=false
android.r8.optimizedResourceShrinking=false
```

### 2. Включить параллельную сборку

Добавить в `gradle.properties`:

```properties
# Parallel execution (для multi-module проектов)
org.gradle.parallel=true

# Build cache
org.gradle.caching=true

# File system watching
org.gradle.vfs.watch=true

# Kotlin incremental
kotlin.incremental=true
kotlin.incremental.useClasspathSnapshot=true
```

### 3. Улучшить JVM настройки

Текущие:
```
org.gradle.jvmargs=-Xmx6g -XX:-HeapDumpOnOutOfMemoryError -XX:+PrintGC -XX:+UseParallelGC
```

Рекомендуемые:
```
org.gradle.jvmargs=-Xmx6g -XX:+UseParallelGC -XX:MaxMetaspaceSize=1g -Dfile.encoding=UTF-8
```

Убраны:
- `-XX:-HeapDumpOnOutOfMemoryError` - не нужен при стабильной сборке
- `-XX:+PrintGC` - создаёт лишний лог

### 4. Удалить kotlin-android плагин (AGP 9.0+)

AGP 9.0+ имеет встроенную поддержку Kotlin. Плагин `org.jetbrains.kotlin.android` больше не нужен.

В каждом модуле заменить:
```kotlin
// Было:
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)  // <- удалить
}

// Станет:
plugins {
    alias(libs.plugins.android.library)
}
```

### 5. Обновить зависимости

Текущие версии устарели. Обновить:

```toml
[versions]
agp = "9.3.1"           # было 9.2.0
kotlin = "2.4.10"       # было 2.2.20
composeBom = "2026.06.01"  # было 2025.06.01
coreKtx = "1.19.0"      # было 1.17.0
```

---

## Измерение производительности

### До применения улучшений:

```bash
./gradlew assembleDebug --profile
```

Результат сохранить в `build/reports/profile/`

### После применения:

Сравнить время сборки.

---

## Миграция на AGP 10.0 (Future)

Когда AGP 10.0 выйдет, выполнить:

1. Обновить `gradle.properties` (удалить deprecated)
2. Удалить `kotlin-android` плагин из всех модулей
3. Обновить версии в `libs.versions.toml`
4. Прогнать lint и исправить warnings
5. Запустить тесты

---

## Локальный Gradle Daemon

Для ещё большей скорости можно настроить локальный daemon:

```bash
# В ~/.gradle/gradle.properties
org.gradle.daemon=true
org.gradle.configureondemand=true
```

---

## Примечания

- Текущая конфигурация работает стабильно
- Улучшения можно применять постепенно
- Перед миграцией на AGP 10.0 сделать отдельную ветку