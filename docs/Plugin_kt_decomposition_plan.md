# Plugin.kt — анализ и план декомпозиции

## Текущее состояние

**Файл:** `bookreader/src/main/java/com/github/axet/bookreader/app/Plugin.kt`
**Размер:** 1101 строка
**Роль:** API-интерфейс плагинов читалки. Определяет контракт для всех форматов книг (PDF, DjVu,
Comics).

## Структура файла

```
Plugin (interface)                        — 1101 строк
├── Box (open class)                      —   38 строк  | строки 37-65
├── RenderRect (class)                    —    5 строк  | строки 70-74
├── Page (abstract class)                 —  210 строк  | строки 78-287
├── View (open class)                     —  809 строк  | строки 292-1100
│   ├── Companion object                 — константы, odd()
│   ├── Свойства (wallpaper, paint, current, reflower)
│   ├── Методы рендеринга (drawWallpaper, render, drawOnCanvas, drawPage)
│   ├── Навигация (gotoPosition, onScrollingFinished, canScroll, getPosition)
│   ├── Zoom/ref-low логика
│   ├── Selection (open class)           — строки 897-1033
│   │   ├── Setter (interface)
│   │   ├── Bounds (class)
│   │   ├── Page (class)
│   │   └── Point (class)
│   ├── Link (class)                     — строки 1038-1050
│   └── Search (open class)              — строки 1055-1099
│       └── Bounds (class)
```

## Почему файл проблемно разбить

### 1. Глубокая вложенность классов в интерфейс

Все классы вложены в интерфейс `Plugin` — они формируют неймспейс `Plugin.Box`, `Plugin.Page`,
`Plugin.View.Selection` и т.д. Kotlin не позволяет «вынести» реализацию вложенного класса в
отдельный файл, сохранив его принадлежность интерфейсу.

### 2. Массовое использование из Java-кода (основная проблема)

Java-код использует полные квалифицированные имена:

| Java-класс          | Что использует                                                                                                                                                                                                 | Кол-во обращений |
|---------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------|
| `FBReaderView.java` | `Plugin.View.Search`, `Plugin.View.Selection`, `Plugin.View.Selection.Page`, `Plugin.View.Selection.Bounds`, `Plugin.View.Link`                                                                                | ~15              |
| `ScrollWidget.java` | `Plugin.View.Selection`, `Plugin.View.Selection.Page`, `Plugin.View.Selection.Point`, `Plugin.View.Selection.Bounds`, `Plugin.View.Selection.Setter`, `Plugin.View.Search.Bounds`, `Plugin.Box`, `Plugin.Page` | ~40              |
| `Storage.java`      | `Plugin.View.Selection.Page`                                                                                                                                                                                   | 1                |

**Всего Java-файлов:** 3 (FBReaderView.java: 2253 строки, ScrollWidget.java: 1700+ строк,
Storage.java: 1414 строк)

### 3. Наследование с сильной связностью

Три плагина наследуют внутренние классы:

| Плагин                    | Наследует               |
|---------------------------|-------------------------|
| `PDFPlugin.PdfView`       | `Plugin.View`           |
| `PDFPlugin.Selection`     | `Plugin.View.Selection` |
| `PDFPlugin.PdfSearch`     | `Plugin.View.Search`    |
| `PDFPlugin.PdfiumPage`    | `Plugin.Page`           |
| `DjvuPlugin.DjvuView`     | `Plugin.View`           |
| `DjvuPlugin.Selection`    | `Plugin.View.Selection` |
| `DjvuPlugin.DjvuSearch`   | `Plugin.View.Search`    |
| `DjvuPlugin.DjvuPage`     | `Plugin.Page`           |
| `ComicsPlugin.ComicsView` | `Plugin.View`           |
| `ComicsPlugin.ComicsPage` | `Plugin.Page`           |

### 4. Циклическая логическая зависимость

`Plugin.View` ссылается на `Reflow.Info` (из соседнего файла `Reflow.kt`), а `Reflow` в свою очередь
зависит от `Plugin.View` через `ScrollWidget`. Извлечение `View` наружу может создать циклические
импорты.

### 5. Java-файлы — легаси без тестов

`FBReaderView.java` и `ScrollWidget.java` — legacy Java-виджеты по 2000+ строк без unit-тестов.
Любое изменение в API Plugin.kt требует изменений в этих файлах с риском регрессии.

## Карта зависимостей

```
Plugin.kt (API definition)
    │
    ├── PDFPlugin.kt ─── наследует Selection, Search, Page
    ├── DjvuPlugin.kt ── наследует Selection, Search, Page
    ├── ComicsPlugin.kt ─ наследует Page
    │
    ├── FBReaderView.java ── использует Search, Selection, Link
    ├── ScrollWidget.java ── использует Selection, Page, Box (40+ обращений!)
    │
    ├── Storage.java ──────── использует Selection.Page
    ├── PagerWidget.kt ────── использует Page, Selection.Setter
    ├── SelectionView.kt ──── использует Selection, Setter, Bounds
    └── TTSPopup.kt ───────── использует Selection
```

**Затронуто файлов при разбиении:** 10 (4 Kotlin + 3 Java + 3 Kotlin widgets)

---

## Поэтапный план декомпозиции

План рассчитан на последовательное выполнение — каждый этап можно делать отдельным PR.

### Этап 1: Подготовка — покрытие тестами (1-2 дня)

**Цель:** Обезопасить рефакторинг регрессионными тестами.

1. **Написать integration-тесты для Plugin.View**
    - Создать мок-реализацию `Plugin.View` с фиктивным `Page`
    - Покрыть тестами: `renderRect()`, `gotoPosition()`, `onScrollingFinished()`, `canScroll()`,
      `getPosition()`, `getNextPosition()`, `pagePosition()`
    - Цель: ≥80% coverage методов `Plugin.View`

2. **Написать тесты для Plugin.Page**
    - Покрыть: `renderPage()`, `next()`, `prev()`, `scale()`, `renderRect()`
    - Использовать мок-страницу с фиктивным `pageBox`

3. **Написать тесты для Plugin.View.Selection**
    - Покрыть: `isWord()`, логику `Bounds`
    - Создать мок-реализацию с тестовыми данными

**Файлы:**

- `bookreader/src/test/java/com/github/axet/bookreader/app/PluginViewTest.kt`
- `bookreader/src/test/java/com/github/axet/bookreader/app/PluginPageTest.kt`
- `bookreader/src/test/java/com/github/axet/bookreader/app/PluginSelectionTest.kt`

---

### Этап 2: Извлечение Box и RenderRect (30 минут)

**Цель:** Вынести простые data-классы без зависимостей.

1. Создать `bookreader/.../app/PluginBox.kt`:
    - Перенести `Box` и `RenderRect` как top-level классы
    - `RenderRect` наследует `Box` — оставить в том же файле

2. В `Plugin.kt` добавить typealias для обратной совместимости:
   ```kotlin
   interface Plugin {
       // ⚠️ Deprecated: use PluginBox directly
       typealias Box = PluginBox
       typealias RenderRect = PluginRenderRect
       // ...
   }
   ```

3. Обновить usage в 4 файлах:
    - `DjvuPlugin.kt`: `Plugin.Box(...)` → `PluginBox(...)`
    - `PDFPlugin.kt`: `Plugin.Box(...)` → `PluginBox(...)`
    - `ComicsPlugin.kt`: `Plugin.Box` → `PluginBox`
    - `ScrollWidget.java`: `new Plugin.Box()` → `new PluginBox()`

**Риск:** Низкий. Классы простые, без логики.

**Экономия:** ~43 строки из Plugin.kt

---

### Этап 3: Извлечение Plugin.Page (2-3 часа)

**Цель:** Вынести абстрактный класс `Page` (210 строк) в отдельный файл.

1. Создать `bookreader/.../app/PluginPage.kt`:
    - Перенести класс `Page` как top-level abstract class
    - Он не зависит от `Plugin.View` — только от `Box` (уже вынесен), `ZLViewEnums.PageIndex`,
      `FBReaderView` (константа `PAGE_OVERLAP_PERCENTS`)

2. В `Plugin.kt` добавить typealias:
   ```kotlin
   interface Plugin {
       // ⚠️ Deprecated: use PluginPage directly
       typealias Page = PluginPage
       // ...
   }
   ```
   **Важно:** Проверить, что `typealias` в интерфейсе работает для Java-кода (`ScrollWidget.java`
   использует `Plugin.Page`).

3. Обновить 3 плагина:
    - `PDFPlugin.PdfiumPage : Plugin.Page` → `: PluginPage`
    - `DjvuPlugin.DjvuPage : Plugin.Page` → `: PluginPage`
    - `ComicsPlugin.ComicsPage : Plugin.Page` → `: PluginPage`

4. Обновить usage в:
    - `Plugin.View` — свойство `current: Page?` → `current: PluginPage?`
    - `ScrollWidget.java` — `Plugin.Page info` → `PluginPage info`
    - `PagerWidget.kt` — `object : Plugin.Page(...)` → `object : PluginPage(...)`
    - `FBReaderView.java` — при использовании `Plugin.Page`

5. **Критическая проверка:** `typealias` в интерфейсе доступен из Java? Если нет — использовать
   `@JvmName` или оставить пустой класс-враппер:
   ```kotlin
   // Запасной план, если typealias не работает из Java:
   interface Plugin {
       // Keep for Java backward compatibility
       abstract class Page : PluginPage()  // пустая обёртка
   }
   ```

**Риск:** Средний. Много обращений, нужна проверка Java-совместимости.

**Экономия:** ~210 строк из Plugin.kt

---

### Этап 4: Извлечение Plugin.View в отдельный файл (4-6 часов)

**Цель:** Вынести 809 строк `View` в `PluginView.kt`.

Это самый сложный этап, т.к. `View` содержит внутренние классы `Selection`, `Link`, `Search`.

1. Создать `bookreader/.../app/PluginView.kt`:
    - Перенести `View` как top-level open class
    - Перенести внутренние классы `Selection`, `Link`, `Search` (пока остаются внутри `View`)

2. В `Plugin.kt` добавить typealias:
   ```kotlin
   interface Plugin {
       typealias View = PluginView
   }
   ```

3. Обновить наследование в 3 плагинах:
    - `PDFPlugin.PdfView : Plugin.View` → `: PluginView`
    - `DjvuPlugin.DjvuView : Plugin.View` → `: PluginView`
    - `ComicsPlugin.ComicsView : Plugin.View` → `: PluginView`

4. Обновить Java-файлы:
    - `FBReaderView.java`: `Plugin.View.Search` → `PluginView.Search`, etc.
    - `ScrollWidget.java`: Все 40+ обращений к `Plugin.View.*` → `PluginView.*`

5. Обновить Kotlin-виджеты:
    - `PagerWidget.kt`, `SelectionView.kt`, `TTSPopup.kt`

**Риск:** Высокий. 40+ обращений только в ScrollWidget.java, нужно тщательное тестирование.

**Экономия:** ~809 строк из Plugin.kt

---

### Этап 5 (опционально): Извлечение Selection, Link, Search из View (4-6 часов)

**Цель:** Разбить оставшиеся 809 строк `PluginView.kt`.

1. **Selection → PluginSelection.kt** (140 строк)
    - Включает `Setter`, `Bounds`, `Page`, `Point`
    - Наследуется: `PDFPlugin.Selection`, `DjvuPlugin.Selection`
    - Используется: `FBReaderView.java`, `ScrollWidget.java`, `SelectionView.kt`, `TTSPopup.kt`,
      `Storage.java`
    - Сохранить typealias `PluginView.Selection = PluginSelection`

2. **Search → PluginSearch.kt** (50 строк)
    - Включает `Bounds`
    - Наследуется: `PDFPlugin.PdfSearch`, `DjvuPlugin.DjvuSearch`
    - Используется: `FBReaderView.java`, `ScrollWidget.java`

3. **Link → PluginLink.kt** (15 строк)
    - Используется: `FBReaderView.java`

**Риск:** Высокий. Множественные изменения в Java-коде.

**Итого после всех этапов:** Plugin.kt сократится с 1101 до ~50 строк (только интерфейс с
typealias).

---

## Оценка трудозатрат

| Этап                     | Часы      | Риск    | Экономия строк |
|--------------------------|-----------|---------|----------------|
| 1. Покрытие тестами      | 8-16      | Низкий  | 0              |
| 2. Box / RenderRect      | 0.5       | Низкий  | 43             |
| 3. Plugin.Page           | 2-3       | Средний | 210            |
| 4. Plugin.View           | 4-6       | Высокий | 809            |
| 5. Selection/Link/Search | 4-6       | Высокий | 0 (из View)    |
| **Итого**                | **18-31** |         | **1062**       |

## Рекомендация

1. **Этапы 1-3 обязательны** — дают 25% сокращение с низким/средним риском
2. **Этап 4 — факультативный** — требует уверенности в Java-легаси
3. **Этап 5 — только после полной миграции FBReaderView.java и ScrollWidget.java на Kotlin**

Ключевой инсайт: основная проблема не в Plugin.kt, а в **легаси Java-виджетах** (FBReaderView.java,
ScrollWidget.java). Пока они на Java — разбиение Plugin.kt будет болезненным. Приоритетнее
мигрировать эти Java-файлы на Kotlin — тогда рефакторинг Plugin.kt станет тривиальным.
