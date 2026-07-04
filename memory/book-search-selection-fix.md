---
name: book-search-selection-fix
description: План исправления выделения текста и реализации поиска в bookreader
metadata: 
  node_type: memory
  type: project
  originSessionId: 6539e1e4-578a-4ff4-830e-c5f512f0f659
---

# Book Search & Selection Fix

## Проблема

### Баг выделения текста в paging mode

**Описание:** При постраничном отображении (свайп страницы в бок) выделение текста остаётся на месте, а текст под ним двигается. Выделение не привязано к тексту.

**Работает:** Непрерывный просмотр с вертикальным скроллом (ScrollWidget)

**Не работает:** Постраничный просмотр (PagerWidget)

### Root Cause (найдено в коде)

```kotlin
// PagerWidget.kt:221-224
if (selectionPage != null && !selectionPage!!.samePositionAs(position)) {
    fb.post { fb.selectionClose() }  // ← Выделение закрывается при смене страницы!
    selectionPage = null
}
```

**Проблема:**
1. `selectionPage` хранит только одну позицию страницы
2. При смене страницы - выделение закрывается вместо обновления
3. Нет поддержки cross-page selection (выделение на нескольких страницах)
4. Координаты не пересчитываются при `pageOffset`

---

## Существующий функционал поиска

### Файлы поиска

| Файл | Назначение |
|------|------------|
| `TextSearchPopup.java` | UI popup с кнопками prev/next/close |
| `ZLSearchUtil.java` | Алгоритм поиска текста |
| `ZLSearchPattern.kt` | Паттерн поиска (case-insensitive) |
| `Plugin.View.Search` | Абстрактный класс поиска для PDF/DJVU |
| `FBReaderView.SearchView` | Отрисовка результатов поиска |
| `PagerWidget.searchPage()` | Навигация к результату в paging mode |
| `ScrollWidget.searchPage()` | Навигация к результату в scroll mode |

### Flow поиска

```
1. User → SEARCH action → pattern
2. pluginview.search(pattern) → Search object
3. Search stores results with page indices
4. searchPage(page) → navigate to result
5. SearchView renders highlights via getBounds(page)
6. TextSearchPopup shows navigation buttons
```

---

## План действий

### Phase 1: Fix Selection Bug (HIGH PRIORITY)

#### 1.1 Анализ coordinate system

**Файл:** `SelectionCoordinates.kt`

Три системы координат:
- **Device** — абсолютные screen координаты
- **Page** — относительно PDF страницы
- **View** — относительно SelectionView

**Задача:** При смене страницы Page координаты должны пересчитываться.

#### 1.2 Modify PagerWidget.kt

**Заменить:**
```kotlin
// OLD - закрывает выделение
if (selectionPage != null && !selectionPage!!.samePositionAs(position)) {
    fb.post { fb.selectionClose() }
}
```

**На:**
```kotlin
// NEW - обновляет выделение для новой страницы
if (selectionPage != null) {
    if (!selectionPage!!.samePositionAs(position)) {
        // Выделение на другой странице - скрыть handles, но сохранить данные
        fb.selection.hideHandles()
        // НЕ закрывать полностью - пользователь может вернуться
    } else {
        // Та же страница - обновить координаты
        fb.selection.updateCoordinates(getPageRect())
    }
}
```

#### 1.3 Добавить SelectionManager

Создать общий менеджер для PagerWidget и ScrollWidget:

```kotlin
class SelectionManager(val fb: FBReaderView) {
    private var selectionData: SelectionData? = null
    
    data class SelectionData(
        val selection: Plugin.View.Selection,
        val startPage: Int,
        val endPage: Int,      // Может быть несколько страниц
        val startOffset: Point,
        val endOffset: Point
    )
    
    fun onPageChanged(newPosition: ZLTextPosition) {
        selectionData?.let { data ->
            if (data.spansPage(newPosition)) {
                // Показать видимую часть выделения
                showVisiblePart(newPosition)
            } else if (!data.isOnPage(newPosition)) {
                // Выделение на другой странице - скрыть handles
                hideHandlesButKeepData()
            }
        }
    }
    
    fun restoreSelection(position: ZLTextPosition) {
        // Вернуться на страницу с выделением - восстановить handles
    }
}
```

#### 1.4 Support multi-page selection

Как в ScrollWidget, но для PagerWidget:

```kotlin
// ScrollWidget.java:931-932 - multiple PageViews
view.selection = new SelectionView.PageView(...);
fb.selection.add(view.selection);

// PagerWidget должен поддерживать то же
```

---

### Phase 2: Compose Search UI (NEW)

#### 2.1 Создать SearchComposePanel.kt

```kotlin
@Composable
fun SearchComposePanel(
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    resultsCount: Int,
    currentResultIndex: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        // Search input
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onQueryChange,
            placeholder = { Text("Поиск в книге") },
            singleLine = true,
            modifier = Modifier.weight(1f)
        )
        
        // Results counter
        if (resultsCount > 0) {
            Text(
                text = "${currentResultIndex + 1}/${resultsCount}",
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            
            // Navigation buttons
            IconButton(onClick = onPrevious) {
                Icon(Icons.Default.ArrowUp, "Previous")
            }
            IconButton(onClick = onNext) {
                Icon(Icons.Default.ArrowDown, "Next")
            }
        }
        
        // Close button
        IconButton(onClick = onClose) {
            Icon(Icons.Default.Close, "Close")
        }
    }
}
```

#### 2.2 Интеграция в ReaderContent.kt

```kotlin
// Добавить state для поиска
var searchState by remember { mutableStateOf(SearchState()) }

// Показать panel когда поиск активен
if (searchState.isActive) {
    SearchComposePanel(
        searchQuery = searchState.query,
        onQueryChange = { query -> 
            searchState = searchState.copy(query = query)
            performSearch(query)
        },
        resultsCount = searchState.resultsCount,
        currentResultIndex = searchState.currentResult,
        onPrevious = { navigateSearchResult(-1) },
        onNext = { navigateSearchResult(1) },
        onClose = { closeSearch() },
        modifier = Modifier.align(Alignment.TopCenter)
    )
}
```

#### 2.3 SearchState data class

```kotlin
@Immutable
data class SearchState(
    val isActive: Boolean = false,
    val query: String = "",
    val resultsCount: Int = 0,
    val currentResult: Int = 0,
    val results: List<SearchResult> = emptyList(),
)

@Immutable
data class SearchResult(
    val pageIndex: Int,
    val bounds: Rect,
    val text: String,
)
```

---

### Phase 3: Unified Search for All Modes

#### 3.1 Abstract Search Interface

```kotlin
interface BookSearchEngine {
    suspend fun search(pattern: String, caseInsensitive: Boolean): List<SearchResult>
    fun navigateToResult(result: SearchResult)
    fun highlightResults(results: List<SearchResult>)
    fun clearHighlights()
}

// Implementations for different formats
class PdfSearchEngine(view: Plugin.View) : BookSearchEngine
class DjvuSearchEngine(view: Plugin.View) : BookSearchEngine
class EpubSearchEngine(fbReader: FBReaderApp) : BookSearchEngine
class Fb2SearchEngine(fbReader: FBReaderApp) : BookSearchEngine
```

#### 3.2 Работа в paging и scroll mode

```kotlin
fun navigateToResult(result: SearchResult) {
    // PagerWidget
    if (widget is PagerWidget) {
        widget.searchPage(result.pageIndex)
        widget.updateOverlaysReset()  // Refresh highlights
    }
    
    // ScrollWidget
    if (widget is ScrollWidget) {
        widget.scrollToPage(result.pageIndex)
        widget.selectionUpdate(widget.findPage(result.pageIndex))
    }
}
```

---

### Phase 4: Tests & Validation

#### 4.1 Unit tests

```kotlin
@Test
fun `selection persists when page changes`() {
    val manager = SelectionManager(fbReaderView)
    manager.startSelection(page1)
    manager.onPageChanged(page2)
    
    // Selection data still exists
    assertNotNull(manager.selectionData)
    // Handles hidden but not closed
    assertFalse(manager.isHandlesVisible)
}

@Test
fun `selection restores when returning to original page`() {
    val manager = SelectionManager(fbReaderView)
    manager.startSelection(page1)
    manager.onPageChanged(page2)
    manager.onPageChanged(page1)  // Return
    
    // Handles restored
    assertTrue(manager.isHandlesVisible)
}
```

#### 4.2 Manual testing checklist

- [ ] Paging mode: long press → selection appears
- [ ] Paging mode: swipe page → **panel hidden**, selection hidden (data kept)
- [ ] Paging mode: return to original page → panel restored, selection restored
- [ ] Paging mode: drag handle near edge → prepare for cross-page
- [ ] **Mode switch (paging ↔ scroll): panel hidden if selection was active**
- [ ] **Fullscreen toggle: selection coordinates recalculated correctly**
- [ ] Scroll mode: selection works (current behavior)
- [ ] Search: open panel → type query → results highlighted
- [ ] Search: navigate prev/next → correct pages
- [ ] Search: works in paging mode
- [ ] Search: works in scroll mode
- [ ] Search: works in reflow mode

---

## Требования к UI

### Selection Panel Visibility

| Событие | Panel | Handles | Selection Data |
|---------|-------|---------|----------------|
| Long press (create) | **SHOW** | SHOW | Created |
| Swipe page (paging) | **HIDE** | HIDE | **Kept** |
| Return to selection page | **SHOW** | SHOW | Restored |
| Mode switch (paging ↔ scroll) | **HIDE** | HIDE | **Clear** |
| **Fullscreen toggle** | - | - | **Recalculate coordinates** |
| Close button click | HIDE | HIDE | Clear |
| Bookmark/Copy/Share action | HIDE | HIDE | Clear |

### При смене режима (paging ↔ scroll)

**Требование:** При переключении между paging и scroll mode - панель выделения должна скрываться, если она была видна.

**Реализация:** В методе смены режима вызвать:
```kotlin
fb.app.runAction(ActionCode.SELECTION_HIDE_PANEL)
fb.selection?.hideHandles()
fb.selection?.clearSavedSelection()
```

**Причина:** 
- Paging и scroll имеют разные coordinate systems
- Selection position в paging mode не совпадает с scroll mode
- Пользователь должен создать новое выделение в новом режиме

### При переключении fullscreen режима

**Требование:** При переключении fullscreen - координаты выделения должны пересчитываться.

**Реализация:** После fullscreen toggle вызвать:
```java
// В FBReaderView.toggleFullscreen()
post(() -> {
    // ... toggle system bars ...
    updateSelectionAfterFullscreenChange();
});

private void updateSelectionAfterFullscreenChange() {
    if (selection == null) return;
    
    // Обновляем clip height (mainAreaHeight changed)
    selection.setClipHeight(widget.getMainAreaHeight());
    
    // Пересчитываем координаты
    if (widget instanceof PagerWidget) {
        ((PagerWidget) widget).updateOverlays();
    }
    selection.update();
}
```

**Причина:** 
- `mainAreaHeight` меняется при fullscreen (без toolbar/footer)
- SelectionView coordinates не пересчитываются автоматически
- Подсвеченный текст смещается относительно реального текста

---

## Сценарии тестирования Selection

### Подготовка

1. **Откройте Logcat** в Android Studio
2. **Фильтр:** `voronin` (в поле поиска Logcat)
3. **Откройте книгу** в **paging mode** (постраничный просмотр)

---

### Сценарий 1: Создание выделения

| Шаг | Действие | Ожидаемый лог |
|-----|----------|---------------|
| 1 | Long press на тексте | `PagerWidget onLongClick: attempting selection at position=...` |
| 2 | Появились маркеры | `PagerWidget onLongClick: selection created successfully at ...` |
| 3 | | `PagerWidget onLongClick: selectionPage set to ...` |

**✅ Успех:** Маркеры появились, в Logcat 3 строки логов.

**❌ Ошибка:** Нет логов или `selectionPage` null — long press не работает.

---

### Сценарий 2: Свайп на следующую страницу (panel + selection скрыты)

| Шаг | Действие | Ожидаемый лог |
|-----|----------|---------------|
| 1 | Свайп вправо → следующая страница | `PagerWidget: page changed from [pos1] to [pos2] - hiding handles and panel` |
| 2 | | `SelectionView hideHandles: hiding entire selection view` |
| 3 | | `SelectionView hideHandles: visibility set to INVISIBLE` |

**✅ Успех:** 
- **Panel скрыта** (Compose panel исчезла)
- **Выделение скрыто** (подсвеченный текст исчез)
- В логах `visibility set to INVISIBLE`

**❌ Ошибка:** Panel или выделение остались видны.

---

### Сценарий 3: Возврат на страницу с выделением (panel + selection восстановлены)

| Шаг | Действие | Ожидаемый лог |
|-----|----------|---------------|
| 1 | Свайп влево → предыдущая страница | `PagerWidget: returned to selection page - restoring handles and panel` |
| 2 | | `SelectionView restoreHandles: restoring entire selection view` |
| 3 | | `SelectionView restoreHandles: visibility set to VISIBLE` |

**✅ Успех:** Panel и всё выделение восстановились.

**❌ Ошибка:** Panel или выделение не появились.

---

### Сценарий 4: Много свайпов + возврат

| Шаг | Действие | Ожидаемый результат |
|-----|----------|---------------------|
| 1 | Свайп → стр. 2 | Выделение скрыто |
| 2 | Свайп → стр. 3 | Выделение скрыто |
| 3 | Свайп → стр. 4 | Выделение скрыто |
| 4 | Свайп ← стр. 3 | Выделение скрыто (selectionPage ≠ pos) |
| 5 | Свайп ← стр. 2 | Выделение скрыто |
| 6 | Свайп ← стр. 1 | **Выделение восстановлено** |

**✅ Успех:** После возврата на страницу 1 выделение восстановилось.

**❌ Ошибка:** Выделение не восстановилось на странице 1.

---

### Сценарий 5: Новое выделение после свайпа

| Шаг | Действие | Ожидаемый результат |
|-----|----------|---------------------|
| 1 | На странице 2: long press | Новое выделение создано |
| 2 | Свайп → стр. 3 | Новое выделение скрыто |
| 3 | Свайп ← стр. 2 | Новое выделение восстановлено |

**✅ Успех:** Новое выделение работает по тому же принципу.

---

### Сценарий 6: Scroll mode (для сравнения)

| Шаг | Действие | Ожидаемый результат |
|-----|----------|---------------------|
| 1 | Переключить в scroll mode | Vertical scrolling |
| 2 | Long press → выделение | Маркеры появились |
| 3 | Scroll вниз | **Выделение остаётся видимым** (это работало раньше) |
| 4 | Scroll вверх к выделению | Маркеры на месте |

**Примечание:** Это работало раньше — для сравнения.

---

### Сценарий 7: Смена режима (paging ↔ scroll)

| Шаг | Действие | Ожидаемый лог |
|-----|----------|---------------|
| 1 | Paging mode: создать выделение | `PagerWidget onLongClick: selection created` |
| 2 | Переключить в scroll mode | `FBReaderView setWidget: closing selection before mode switch` |
| 3 | | Panel скрыта, выделение закрыто |
| 4 | Переключить обратно в paging | Нет ошибки |

**✅ Успех:** Panel скрыта при смене режима, нет crash.

---

### Сценарий 8: Fullscreen toggle (пересчёт координат)

| Шаг | Действие | Ожидаемый лог |
|-----|----------|---------------|
| 1 | Paging mode: создать выделение | `selection created` |
| 2 | Переключить в fullscreen | `Action: ENTER fullscreen` |
| 3 | | `updateSelectionAfterFullscreenChange: updating selection coordinates` |
| 4 | | `updateSelectionAfterFullscreenChange: selection updated successfully` |
| 5 | Выделение на правильной позиции | ✅ |
| 6 | Exit fullscreen | `Action: EXIT fullscreen` |
| 7 | | `updateSelectionAfterFullscreenChange: updating...` |
| 8 | Выделение на правильной позиции | ✅ |

**✅ Успех:** Выделение не смещается при fullscreen toggle.

**❌ Ошибка:** Выделение смещается относительно текста.

---

### Полный чек-лист

```
□ Сценарий 1: Создание выделения ✅
□ Сценарий 2: Свайп вперёд (panel + selection hidden) ✅
□ Сценарий 3: Возврат назад (panel + selection restored) ✅
□ Сценарий 4: Много свайпов + возврат
□ Сценарий 5: Новое выделение после свайпа
□ Сценарий 6: Scroll mode работает
□ Сценарий 7: Смена режима (paging ↔ scroll)
□ Сценарий 8: Fullscreen toggle (coordinates recalculated)
```

---

### Команда для сбора логов при ошибке

```bash
adb logcat -s voronin:D | grep "PagerWidget\|SelectionView\|FBReaderView"
```

---

## Файлы для изменения

### Selection fix

| Файл | Изменение |
|------|-----------|
| `PagerWidget.kt:221-224` | Заменить close на hide handles |
| `PagerWidget.kt:352-412` | Поддержка multi-page selection |
| `SelectionView.kt` | Добавить hideHandles(), restoreHandles() |
| `SelectionManager.kt` | **NEW** — общий менеджер выделения |
| `SelectionCoordinates.kt` | Пересчёт при pageOffset |

### Search UI

| Файл | Изменение |
|------|-----------|
| `SearchComposePanel.kt` | **NEW** — Compose UI panel |
| `ReaderContent.kt` | Добавить search state и panel |
| `SearchState.kt` | **NEW** — state data class |
| `BookSearchEngine.kt` | **NEW** — interface для поиска |

### Remove legacy

| Файл | Действие |
|------|----------|
| `TextSearchPopup.java` | Заменить на SearchComposePanel |

---

## Приоритеты

1. **🔴 HIGH** — Fix selection bug (Phase 1)
2. **🟡 MEDIUM** — Compose Search UI (Phase 2)
3. **🟡 MEDIUM** — Unified search (Phase 3)
4. **🟢 LOW** — Tests & cleanup (Phase 4)

---

## Связанные memories

- [[selection-panel-refactoring]] — рефакторинг панели выделения
- [[compose-optimizations]] — оптимизации Compose
- [[coroutine-optimization]] — корутины для поиска