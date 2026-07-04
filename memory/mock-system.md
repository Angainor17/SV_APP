---
name: mock-system
description: Система моков для сетевых запросов с единым флагом управления
metadata: 
  node_type: memory
  type: project
  originSessionId: 344460d1-229c-400b-ae6a-b578274a3095
---

# Mock System для сетевых запросов

## Расположение
`commonarchitecture/src/main/java/su/sv/commonarchitecture/mock/`

## Файлы
- **MockConfig.kt** — флаг `IS_MOCK_ENABLED` для включения/выключения моков
- **MockInterceptor.kt** — OkHttp Interceptor для перехвата запросов
- **MockDataProvider.kt** — провайдер мок-данных для всех API

## Как использовать

### Включить моки (offline режим)
```kotlin
// MockConfig.kt
const val IS_MOCK_ENABLED = true
```

### Выключить моки (online режим)
```kotlin
// MockConfig.kt
const val IS_MOCK_ENABLED = false
```

## Поддерживаемые API

| API | URL | Методы |
|-----|-----|--------|
| Wiki | svremya.su | search, getPage, openSearch |
| Books | svremya.org | getBooks |
| VK | api.vk.com | getPosts, getVideo |

## Архитектура
```
MockConfig (const)
    ↓
MockInterceptor (перехватывает запросы)
    ↓
MockDataProvider (возвращает JSON моки)
    ↓
OkHttpClient (встроен в Retrofit)
```

**Почему:** 2026-06-24 — Позволяет разрабатывать приложение без доступа к интернету.
**How to apply:** Изменить `MockConfig.IS_MOCK_ENABLED` на `true` для offline разработки.
