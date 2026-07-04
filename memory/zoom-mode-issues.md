---
name: zoom-mode-issues
description: Проблемы zoom/pinch режима (RESOLVED)
metadata: 
  node_type: memory
  type: project
  originSessionId: 519ee007-1a17-440e-a96b-3f30694ad498
---

# Zoom/Pinch Mode Issues

## Status
**✅ RESOLVED** (2026-07-04)

## Original Problems

### 1. Bookmarks/Notes исчезают при zoom
**Решение:** Scale applied к FBReaderView (parent), bookmarks наследуют scale.

### 2. Нельзя создать заметку при zoom
**Решение:** ZoomGestureHandler returns false из onTouchEvent() - touch events проходят к другим handlers.

### 3. Zoom только 1 страницы в continuous mode
**Решение:** Scale applied ко всему FBReaderView, все visible pages zoomed.

---

## Implementation

See [[zoom-compose-research]] for full implementation details.

### Key Changes:
- Removed PinchView overlay
- Added ZoomGestureHandler (ScaleGestureDetector + double tap + pan)
- Scale applied to FBReaderView via scaleX/Y
- Touch coordinates adapted via ZoomTouchAdapter
- BackHandler for zoom exit