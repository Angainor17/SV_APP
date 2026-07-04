---
name: selection-view-refactoring-plan
description: План рефакторинга SelectionView для упрощения touch handling
metadata: 
  node_type: memory
  type: project
  originSessionId: 475fe7af-7539-4b95-8265-35e25b837b07
---

# SelectionView Refactoring Plan

## Цель

Упростить SelectionView для более легкого maintenance и будущей миграции на Compose.

## Фазы

### Phase 1: State Management Refactoring (приоритет: высокий)

**Задача:** Создать explicit state class для управления состоянием выделения.

**Изменения:**
1. Создать `SelectionState` data class с:
   - `isDragging: Boolean`
   - `activeHandle: HandleType?` (Left/Right/None)
   - `dragOffset: Offset`
   - `startBounds: Rect`
   - `endBounds: Rect`

2. Refactor `HandleRect` и `HotRect` чтобы использовать state вместо mutable fields

3. Добавить `SelectionStateFlow` для reactive updates

**Файлы:**
- `SelectionView.kt` - добавить SelectionState
- `SelectionView.kt` - refactor HandleRect.onTouchEvent

### Phase 2: Touch Handling Simplification (приоритет: высокий)

**Задача:** Упростить onTouchEvent logic.

**Изменения:**
1. Разделить onTouchEvent на отдельные handlers:
   - `handleDragStart()`
   - `handleDragMove()`
   - `handleDragEnd()`

2. Использовать sealed class для drag state:
   ```kotlin
   sealed class DragState {
       object Idle : DragState()
       data class Dragging(val handle: HandleType, val offset: Offset) : DragState()
   }
   ```

3. Добавить better logging для debugging

**Файлы:**
- `SelectionView.kt:336-363` - refactor onTouchEvent

### Phase 3: Error Handling & Null Safety (приоритет: средний)

**Задача:** Улучшить null safety и error handling.

**Изменения:**
1. Проверить все `!!` usages в SelectionView.kt
2. Добавить safe calls или early returns
3. Добавить state validation перед operations

**Файлы:**
- `SelectionView.kt` - null safety improvements
- `PDFPlugin.kt` - уже начато (SelectionBounds)

### Phase 4: Callback Simplification (приоритет: средний)

**Задача:** Упростить onTouchLock/onTouchUnlock callbacks.

**Изменения:**
1. Использовать lambda вместо open functions
2. Добавить explicit callback interface:
   ```kotlin
   interface SelectionCallbacks {
       fun onDragStart(handle: HandleType)
       fun onDragEnd(handle: HandleType)
       fun onBoundsChanged(start: Rect, end: Rect)
   }
   ```

3. Убрать drawer lockMode из SelectionView (responsibility leakage)

**Файлы:**
- `SelectionView.kt:377-382` - refactor callbacks
- `FBReaderView.java:1147-1166` - simplify callback creation

### Phase 5: Coordinate System Cleanup (приоритет: низкий)

**Задача:** Упростить coordinate transformations.

**Изменения:**
1. Создать `SelectionCoordinates` helper class
2. Документировать все coordinate systems:
   - Device coordinates (absolute)
   - Page coordinates (relative to page)
   - View coordinates (relative to SelectionView)
3. Добавить conversion functions

**Файлы:**
- Новый файл `SelectionCoordinates.kt`
- `SelectionView.kt` - использовать helper

### Phase 6: Testing & Documentation (приоритет: низкий)

**Задача:** Добавить unit tests и улучшить документацию.

**Изменения:**
1. Создать tests для SelectionState
2. Создать tests для coordinate conversions
3. Обновить KDoc comments

## Порядок выполнения

1. ✅ Phase 3 (частично) - null safety в PDFPlugin.kt SelectionBounds
2. Phase 1 - SelectionState creation
3. Phase 2 - Touch handling refactor
4. Phase 4 - Callback simplification
5. Phase 3 (остальное) - null safety в SelectionView.kt
6. Phase 5 - Coordinate helper
7. Phase 6 - Tests

## Риски

1. **Coordinate precision** - изменение touch handling может affect precision
2. **ScrollWidget integration** - нужно тестировать с scroll
3. **Backward compatibility** - старые вызовы должны работать

## Success Criteria

- Touch handling code читаемый и понятный
- Нет NPE crashes при drag operations
- Легко добавлять новые features (например, multi-selection)
- Prepared for future Compose migration