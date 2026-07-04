---
name: timber-logging-rule
description: "Правило логирования в SV APP - всегда использовать Timber.tag(\"voronin\")"
metadata: 
  node_type: memory
  type: reference
  originSessionId: 6539e1e4-578a-4ff4-830e-c5f512f0f659
---

# Timber Logging Rule

## Правило

**ВСЕ логи в приложении должны использовать строго:**
```kotlin
Timber.tag("voronin").d/w/e/i/v(...)
```

## Почему "voronin"?

- Уникальный тег для фильтрации логов в Logcat
- Позволяет быстро находить все логи приложения
- Фильтр: `adb logcat -s voronin:*`

## Примеры

```kotlin
// ✅ Правильно
Timber.tag("voronin").d("SelectionView hideHandles: hiding handles")
Timber.tag("voronin").w("PagerWidget: page changed, hiding selection")
Timber.tag("voronin").e(exception, "Error loading book")

// ❌ Неправильно
Timber.d("message")           // Без тега
Timber.tag("selection").d()   // Другой тег
Timber.tag("voronin_selection").d() // Модифицированный тег
```

## Формат сообщений

Для удобства поиска используйте формат:
```
[ClassName] [method]: [description]
```

Примеры:
```kotlin
Timber.tag("voronin").d("PagerWidget onLongClick: selection created at %s", position)
Timber.tag("voronin").d("SelectionView hideHandles: hiding handles for page change")
Timber.tag("voronin").d("PagerWidget updateOverlays: page changed from %s to %s", oldPage, newPage)
```

## Как искать логи в Logcat

```bash
# Android Studio Logcat filter:
voronin

# adb command:
adb logcat -s voronin:* | grep "Selection"

# Только debug:
adb logcat -s voronin:D

# Исключить warnings:
adb logcat -s voronin:D | grep -v "voronin:W"
```

## Связанные memories

- [[book-search-selection-fix]] — логи для selection fix
- [[coroutine-optimization]] — логи для coroutines