---
name: zoom-feature-tz
description: Техническое задание для нового zoom режима
metadata: 
  node_type: memory
  type: project
  originSessionId: 475fe7af-7539-4b95-8265-35e25b837b07
---

# ТЗ: Zoom/Pinch режим для bookreader

## Дата: 2026-07-03
## Статус: Draft

---

## 1. Обзор

### 1.1 Цель
Реализовать zoom (увеличение) режима для bookreader с сохранением всей функциональности:
- Bookmarks/заметки видимы при zoom
- Long press работает при zoom (создание заметок/выделение)
- Zoom применяется ко всем видимым страницам

### 1.2 Подход
Scale transformation к FBReaderView вместо PinchView overlay.

---

## 2. Функциональные требования

### 2.1 Zoom gestures

| Gesture | Действие |
|---------|----------|
| Pinch in (сведение пальцев) | Увеличение (zoom in) |
| Pinch out (разведение пальцев) | Уменьшение (zoom out) до 1.0 = выход из zoom |
| System back button | Выход из zoom (reset scale = 1.0) |

### 2.2 Zoom limits

| Parameter | Value |
|-----------|-------|
| Минимальный zoom | 1.0 (normal view) |
| Максимальный zoom | 3.0 (или configurable) |
| Default zoom | 1.0 |

### 2.3 Double tap zoom

| Event | Action |
|-------|--------|
| Double tap (быстрый двойной клик) | Увеличение до "fit width" - убрать боковые отступы, текст максимально занимает экран |

**Fit width calculation:**
```java
// Вычислить zoom factor чтобы текст занимал всю ширину
float fitWidthZoom = screenWidth / pageContentWidth;
// Clamp: минимум 1.0, максимум 3.0
fitWidthZoom = Math.max(1.0f, Math.min(3.0f, fitWidthZoom));
```

**Behavior:**
- Double tap → zoom to fit width (обычно ~1.2-1.5x)
- Если уже zoomed → reset to 1.0 (toggle behavior)
- Pivot point: center of tapped page

### 2.4 Удалённый функционал

**Убрать из текущего PinchView:**
- ❌ Rotation (повороты влево/вправо) - не нужен
- ❌ Close button - не нужен (закрытие через pinch out или back)

### 2.4 Сохраняемый функционал

| Функция | Требование |
|---------|------------|
| Bookmarks visibility | ✅ Bookmarks/заметки видны при любом zoom |
| Long press | ✅ Работает при zoom для создания заметок |
| Selection | ✅ Выделение текста работает при zoom |
| Navigation | ✅ Scroll работает при zoom |
| All pages zoom | ✅ Zoom применяется ко всем видимым страницам |

---

## 3. UI требования

### 3.1 Zoom controls (optional)

Если gesture недостаточно, добавить простые controls:

| Control | Placement | Action |
|---------|-----------|--------|
| Zoom indicator | Top-right corner | Показывает текущий zoom % |
| Reset button | В zoom indicator | Reset zoom = 1.0 |

### 3.2 Visual feedback

| Event | Feedback |
|-------|----------|
| Zoom change | Smooth animation (200ms) |
| Exit zoom | Smooth animation back to 1.0 |

---

## 4. Технические требования

### 4.1 Scale implementation

```java
// Применить scale к FBReaderView
fbReaderView.setScaleX(zoomFactor);
fbReaderView.setScaleY(zoomFactor);

// Pivot point - точка pinch center
fbReaderView.setPivotX(pivotX);
fbReaderView.setPivotY(pivotY);
```

### 4.2 Touch coordinate adaptation

При zoom touch coordinates нужно адаптировать:

```java
// TouchCoordinateAdapter
public int adaptX(float rawX, View target) {
    float scale = fbReaderView.getScaleX();
    float pivotX = fbReaderView.getPivotX();
    return (int) ((rawX - pivotX) / scale + pivotX);
}

public int adaptY(float rawY, View target) {
    float scale = fbReaderView.getScaleY();
    float pivotY = fbReaderView.getPivotY();
    return (int) ((rawY - pivotY) / scale + pivotY);
}
```

### 4.3 Gesture detection

```java
// ScaleGestureDetector для pinch
ScaleGestureDetector scaleDetector = new ScaleGestureDetector(context, 
    new ScaleGestureDetector.SimpleOnScaleGestureListener() {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float scaleFactor = detector.getScaleFactor();
            float newZoom = currentZoom * scaleFactor;
            // Clamp: 1.0 ≤ newZoom ≤ 3.0
            newZoom = Math.max(1.0f, Math.min(3.0f, newZoom));
            
            // Apply zoom
            applyZoom(newZoom, detector.getFocusX(), detector.getFocusY());
            
            // If zoom back to 1.0 → exit zoom mode
            if (newZoom == 1.0f) {
                exitZoomMode();
            }
            return true;
        }
    });
```

### 4.4 Back handler

```kotlin
// BackHandler для выхода из zoom
BackHandler(enabled = currentZoom > 1.0f) {
    resetZoom()
}
```

---

## 5. Удаление PinchView overlay

### 5.1 Remove files/code

- `FBReaderView.PinchGesture` class - удалить или refactor
- `PinchView` overlay creation - удалить
- Pinch gesture detection - заменить на ScaleGestureDetector

### 5.2 Keep for fallback (optional)

Если PDF требует special rendering при zoom, можно оставить:
- `pluginview.render()` для high-res rendering
- Но без PinchView overlay interception

---

## 6. Testing requirements

### 6.1 Test scenarios

| Scenario | Expected result |
|----------|-----------------|
| Pinch in | Zoom increases, bookmarks visible |
| Pinch out to 1.0 | Exit zoom mode smoothly |
| Long press при zoom | Selection/bookmark creation works |
| Tap на bookmark при zoom | Bookmark edit dialog opens |
| Back button при zoom | Exit zoom mode |
| Scroll при zoom | Smooth scroll at zoomed scale |

### 6.2 Edge cases

| Case | Handling |
|------|----------|
| Zoom > 3.0 | Clamp to max 3.0 |
| Zoom < 1.0 | Clamp to min 1.0, exit zoom |
| Rapid pinch | Smooth animation, no flicker |
| Scroll при zoom | Adjust scroll velocity |

---

## 7. Implementation plan

### Phase 1: Remove PinchView overlay (1h)
- Remove PinchGesture class
- Remove pinchOpen/pinchClose calls
- Clean up touch handling order

### Phase 2: Add ScaleGestureDetector (1h)
- Create ZoomGestureHandler class
- Detect pinch gestures
- Apply scale to FBReaderView
- Add double tap detection → zoom to fit width

### Phase 3: Touch coordinate adapter (2h)
- Create TouchCoordinateAdapter class
- Modify all touch handlers in ScrollWidget
- Modify touch handlers in SelectionView

### Phase 4: Zoom controls UI (1h)
- Add zoom indicator (optional)
- Add reset button (optional)
- Add animation for smooth transitions

### Phase 5: Testing (2h)
- Test all scenarios
- Fix edge cases
- Performance optimization

### Total: **7 hours**

---

## 8. Risks and mitigation

| Risk | Mitigation |
|------|------------|
| Touch coordinates wrong | Unit tests for adapter, visual testing |
| Bookmarks positioning broken | Verify BookmarksView.update() with scale |
| Scroll velocity wrong | Adjust scroll factor by zoom |
| Performance at high zoom | Limit max zoom, optimize rendering |

---

## 9. Acceptance criteria

| Criteria | Status |
|----------|--------|
| Zoom in работает | ⬜ |
| Zoom out работает | ⬜ |
| Zoom out до 1.0 = exit | ⬜ |
| Back button = exit | ⬜ |
| Double tap = fit width zoom | ⬜ |
| Double tap при zoom = reset | ⬜ |
| Bookmarks visible при zoom | ⬜ |
| Long press works при zoom | ⬜ |
| Selection works при zoom | ⬜ |
| Scroll works при zoom | ⬜ |
| No rotation UI | ⬜ |
| No close button | ⬜ |
| Smooth animations | ⬜ |

---

## 10. Related documents

- [[zoom-mode-issues]] - текущие проблемы
- [[zoom-scale-research]] - research findings
- [[bookreader-bookmarks-feature]] - bookmarks architecture