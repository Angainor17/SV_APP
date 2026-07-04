---
name: fbreader-refactoring-plan
description: План поэтапного рефакторинга модуля fbreader для удаления неиспользуемых классов
metadata: 
  node_type: memory
  type: project
  originSessionId: 128b87eb-e4d2-4898-82fb-58ebbfaa4702
---

# План рефакторинга модуля fbreader

## Результат (обновлено 2026-06-22)

**Удалено 53 файла** (653 → 600)

### Выполненные этапы:

| Этап | Статус | Удалено файлов |
|------|--------|----------------|
| Этап 1: Подготовка | ✅ | - |
| Этап A: Sync | ✅ | ~15 |
| Этап B: Tips | ✅ | ~4 |
| Этап C: LitRes | ✅ | ~18 |
| Этап D: RSS | ✅ | ~12 |
| Этап G: DragSortListView | ✅ | ~7 |

### Удалённые пакеты:
- `org.geometerplus.android.fbreader.sync`
- `org.geometerplus.fbreader.network.sync`
- `org.geometerplus.android.fbreader.tips`
- `org.geometerplus.fbreader.tips`
- `org.geometerplus.android.fbreader.network.litres`
- `org.geometerplus.fbreader.network.authentication.litres`
- `org.geometerplus.fbreader.network.rss`
- `com.mobeta.android.dslv`

### Оставшиеся возможности:
- [ ] HTTP Server (DataService) - используется в FBReader и LibraryService
- [ ] API пакет - FBReaderIntents и PluginApi используются
- [ ] Network Library Activities - не зарегистрированы в манифесте, но используются внутри

## Проблема (исходная)

Модуль fbreader содержал **653 файла** с сильными внутренними связями.

## Цель

Уменьшить размер модуля fbreader, удалив неиспользуемые функции:
- Синхронизация (Sync)
- Магазин LitRes
- Подсказки (Tips)
- RSS каталоги
- Внешний API
- Сетевые каталоги (Network Library) - если не используются

## Этапы

### Этап 1: Подготовка ✅ (выполнено)
- [x] Перенести ambilWarna в fbreader
- [x] Перенести dragSortListview в fbreader
- [x] Удалить модули ambilWarna и dragSortListview из проекта
- [x] Оптимизировать зависимости в других модулях

### Этап 2: Создание fbreader-core
**Цель:** Создать новый модуль с только используемыми классами

**Шаги:**
- [ ] Создать модуль `fbreader-core`
- [ ] Определить список используемых классов (анализ импортов из bookreader)
- [ ] Перенести базовые классы (zlibrary/core, zlibrary/text)
- [ ] Перенести форматы книг (fb2, epub, pdf)
- [ ] Перенести модели книг (Book, BookModel)
- [ ] Настроить зависимости

### Этап 3: Перенос UI компонентов
**Цель:** Перенести используемые Activity и View

**Шаги:**
- [ ] Перенести FBReader.java (основная Activity)
- [ ] Перенести виджеты (widgets/)
- [ ] Перенести настройки (preferences/)
- [ ] Перенести закладки (bookmark/) - если используются
- [ ] Перенести DI модули

### Этап 4: Переключение bookreader на fbreader-core
**Цель:** Перевести bookreader на новый модуль

**Шаги:**
- [ ] Обновить зависимости в bookreader
- [ ] Исправить импорты
- [ ] Протестировать сборку
- [ ] Протестировать функционал

### Этап 5: Удаление старого fbreader
**Цель:** Удалить неиспользуемый код

**Шаги:**
- [ ] Убрать зависимость от старого fbreader
- [ ] Удалить модуль fbreader
- [ ] Переименовать fbreader-core в fbreader

### Этап 6: Очистка ресурсов
**Цель:** Удалить неиспользуемые ресурсы

**Шаги:**
- [ ] Удалить неиспользуемые layout файлы
- [ ] Удалить неиспользуемые drawable ресурсы
- [ ] Удалить неиспользуемые строки
- [ ] Проверить размер APK

## Альтернативный подход (постепенное удаление)

Если создание fbreader-core слишком сложно, можно использовать постепенный подход:

### Этап A: Удаление Sync
**Зависимости:** Tips, NetworkLibrary
**Файлы для удаления:**
- `/fbreader/src/main/java/org/geometerplus/android/fbreader/sync/`
- `/fbreader/src/main/java/org/geometerplus/fbreader/network/sync/`

**Файлы для исправления:**
- `FBReader.java` - удалить импорты и использование
- `FBReaderApp.java` - удалить SyncData
- `AppNotifier.java` - удалить SyncData
- `PreferenceActivity.java` - удалить экран синхронизации
- `NetworkLibrary.java` - удалить OPDSSyncNetworkLink
- `NetworkLibraryActivity.java` - удалить SignOutAction

### Этап B: Удаление Tips
**Зависимости:** Sync
**Файлы для удаления:**
- `/fbreader/src/main/java/org/geometerplus/android/fbreader/tips/`
- `/fbreader/src/main/java/org/geometerplus/fbreader/tips/`

**Файлы для исправления:**
- `FBReader.java` - удалить TipRunner
- `PreferenceActivity.java` - удалить TipsManager.ShowTipsOption

### Этап C: Удаление LitRes
**Зависимости:** NetworkLibrary
**Файлы для удаления:**
- `/fbreader/src/main/java/org/geometerplus/android/fbreader/network/litres/`
- `/fbreader/src/main/java/org/geometerplus/fbreader/network/authentication/litres/`

**Файлы для исправления:**
- `NetworkAuthenticationManager.java` - удалить импорты LitRes
- `OPDSFeedHandler.java` - удалить LitResBookshelfItem
- `AuthorisationMenuActivity.java` - удалить импорты LitRes
- `SignUpAction.java` - удалить импорты LitRes

### Этап D: Удаление RSS
**Зависимости:** NetworkLibrary
**Файлы для удаления:**
- `/fbreader/src/main/java/org/geometerplus/fbreader/network/rss/`

**Файлы для исправления:**
- `OPDSLinkXMLReader.java` - удалить RSSNetworkLink

### Этап E: Удаление API
**Зависимости:** FBReader
**Файлы для удаления:**
- `/fbreader/src/main/java/org/geometerplus/android/fbreader/api/` (кроме FBReaderIntents, PluginApi)

**Файлы для исправления:**
- Проверить использование FBReaderIntents
- Проверить использование PluginApi

### Этап F: Удаление HTTP Server
**Зависимости:** FBReader, LibraryService
**Файлы для удаления:**
- `/fbreader/src/main/java/org/geometerplus/android/fbreader/httpd/`

**Файлы для исправления:**
- `FBReader.java` - удалить DataService.Connection
- `LibraryService.java` - удалить DataService.Connection

### Этап G: Удаление DragSortListView
**Зависимости:** CatalogManagerActivity
**Файлы для удаления:**
- `/fbreader/src/main/java/com/mobeta/`

**Файлы для исправления:**
- `CatalogManagerActivity.java` - удалить или переписать без DragSortListView

## Оценка времени

| Этап | Время | Сложность |
|------|-------|-----------|
| Этап 1 (подготовка) | ✅ Выполнено | Низкая |
| Этап A (Sync) | 2-3 часа | Средняя |
| Этап B (Tips) | 1 час | Низкая |
| Этап C (LitRes) | 2 часа | Средняя |
| Этап D (RSS) | 1 час | Низкая |
| Этап E (API) | 1 час | Низкая |
| Этап F (HTTP Server) | 1-2 часа | Средняя |
| Этап G (DragSortListView) | 1 час | Низкая |
| **Итого (постепенный подход)** | **9-12 часов** | |

| Этап | Время | Сложность |
|------|-------|-----------|
| Этап 2-5 (fbreader-core) | 16-24 часа | Высокая |
| **Итого (новый модуль)** | **16-24 часа** | |

## Рекомендация

Рекомендуется **постепенный подход** (Этапы A-G), так как:
1. Меньше риск сломать сборку
2. Можно тестировать после каждого этапа
3. Легче откатить изменения
4. Прозрачнее процесс

## Следующий шаг

Начать с **Этапа A: Удаление Sync**, так как:
- Sync имеет меньше всего зависимостей
- Удаление Sync упростит удаление Tips
- Это позволит протестировать подход

## Статус

- [x] Этап 1: Подготовка
- [ ] Этап A: Удаление Sync
- [ ] Этап B: Удаление Tips
- [ ] Этап C: Удаление LitRes
- [ ] Этап D: Удаление RSS
- [ ] Этап E: Удаление API
- [ ] Этап F: Удаление HTTP Server
- [ ] Этап G: Удаление DragSortListView
