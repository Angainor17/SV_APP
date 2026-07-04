---
name: zoom-compose-research
description: Research zoom/pinch на Jetpack Compose для bookreader (IMPLEMENTED)
metadata: 
  node_type: memory
  type: project
  originSessionId: 519ee007-1a17-440e-a96b-3f30694ad498
---

# Zoom/Pinch на Jetpack Compose Research

## Дата: 2026-07-03
## Статус: ✅ IMPLEMENTED (2026-07-04)

---

## Implementation Summary

### New Files:
- `ZoomGestureHandler.kt` - ScaleGestureDetector + double tap + pan gesture
- `ZoomTouchAdapter.kt` - Touch coordinate adaptation for zoomed state

### Modified Files:
- `FBReaderView.java` - Removed PinchGesture, added Listener methods, zoom API
- `ScrollWidget.java` - ZoomGestureHandler integration, coordinate adaptation
- `PagerWidget.kt` - ZoomGestureHandler integration
- `ReaderState.kt` - Added zoom state fields
- `ReaderActions.kt` - Added ZoomUpdate/ZoomReset actions
- `ReaderViewModel.kt` - Added zoom handlers
- `ReaderContent.kt` - BackHandler for zoom, listener callbacks

### Features Implemented:
- ✅ **Pinch gesture** - ScaleGestureDetector, no touch interception
- ✅ **Double tap** - Fit-width zoom (calculates based on page width)
- ✅ **Pan gesture** - Horizontal movement when zoomed
- ✅ **Back button** - Exits zoom mode
- ✅ **Touch adaptation** - Coordinates adjusted for zoom
- ✅ **Centering** - Page centered after double tap
- ✅ **Bookmarks visible** - Scale applied to FBReaderView parent
- ✅ **Long press works** - Gesture handler doesn't block

### Zoom Behavior:
| Action | Result |
|--------|--------|
| Pinch in | Zoom increase (max 3.0x) |
| Pinch out | Zoom decrease, exit at 1.0 |
| Double tap | Fit-width zoom (~1.1-1.25x) |
| Double tap again | Reset to 1.0 (toggle) |
| Horizontal pan | Move page left/right when zoomed |
| Back button | Exit zoom mode |

### Zoom Limits:
- MIN_ZOOM = 1.0
- MAX_ZOOM = 3.0 (pinch)
- MAX_FIT_WIDTH_ZOOM = 1.25 (double tap)

### Fit-Width Calculation:
```
if pageWidth < screenWidth:
    zoom = screenWidth / pageWidth (clamped to 1.25)
else:
    zoom = screenWidth / (pageWidth * 0.9) (content ratio)
```

---

## 1. Проблема и ТЗ (Original)

### Current Issues (из [[zoom-mode-issues]]):
1. **Bookmarks исчезают при zoom** - PinchView overlay не содержит bookmarks
2. **Long press не работает при zoom** - PinchView intercepts touch events
3. **Zoom только 1 страницы** - в continuous mode zoom применяется только к touched page

### ТЗ Requirements (из [[zoom-feature-tz]]):
- Scale transformation к FBReaderView вместо PinchView overlay
- Zoom applied to all visible pages
- Bookmarks/notes visible при zoom
- Long press works при zoom (selection/bookmark creation)
- Double tap → fit width zoom
- System back → exit zoom

---

## 2. Architecture Analysis

### View Hierarchy:
```
FBReaderView (RelativeLayout) ← AndroidView in Compose
├── ScrollWidget (RecyclerView) - pages container
│   └── PageView[] - individual pages
├── BookmarksView.WordView[] - added via fb.addView()
├── SelectionView - selection overlay
├── PinchView - CURRENT zoom overlay (intercepts touch!)
└── Other overlays (TTS, Search, etc.)
```

### Touch Flow:
```
MotionEvent
    ↓
ScrollWidget.Gestures.onTouchEvent()
    ↓
pinch.onTouchEvent() → intercepts & returns true ❌
    ↓ (blocked!)
Other handlers (long press, scroll) не вызываются
```

### Compose Integration (ReaderContent.kt):
```kotlin
Box(modifier = Modifier.fillMaxSize()) {
    AndroidView(
        factory = { FBReaderView(it) },
        modifier = Modifier.fillMaxSize()
    )
    
    // SelectionComposePanel работает поверх AndroidView
    // Но НЕ intercepts touch events!
    if (state.showSelection) {
        SelectionComposePanel(...)  // UI overlay
    }
}
```

**Key insight:** SelectionComposePanel НЕ intercepts touch - он только UI overlay. Touch events идут в AndroidView.

---

## 3. Compose Zoom Solutions Analysis

### ❌ Option A: Pure Compose gestures (detectTransformGestures)

```kotlin
Box(modifier = Modifier
    .pointerInput(Unit) { detectTransformGestures { ... } }
) {
    AndroidView(factory = { FBReaderView(it) })
}
```

**Problem:** AndroidView получает touch events FIRST. Compose `pointerInput` не может intercept events от child AndroidView.

### ❌ Option B: Scale FBReaderView via Compose state

```kotlin
AndroidView(
    factory = { FBReaderView(it) },
    update = { view ->
        view.scaleX = state.zoomScale
        view.scaleY = state.zoomScale
    }
)
```

**Problem:** Touch coordinates не адаптированы. Long press, selection, bookmarks positioning будут wrong.

### ✅ Option C: Hybrid - Gesture detection в Java, State/UI в Compose

**Architecture:**
```
ScaleGestureDetector (Java, in FBReaderView)
    ↓
FBReaderView.Listener.onZoomChange(scale, pivot)
    ↓
ViewModel.onAction(ReaderActions.ZoomUpdate)
    ↓
State update (zoomScale, zoomPivot)
    ↓
AndroidView.update { view.scaleX/Y = state.zoomScale }
    ↓
FBReaderView touch handlers adapt coordinates using zoom state
```

**Это RECOMMENDED подход.**

---

## 4. Recommended Implementation Plan

### Phase 1: Remove PinchView overlay (1h)

**Remove from FBReaderView.java:**
- `PinchGesture.pinchOpen()` - delete
- `PinchGesture.pinchClose()` - delete
- `PinchView` creation - delete

**Modify touch handling:**
```java
// ScrollWidget.Gestures.onTouchEvent()
public boolean onTouchEvent(MotionEvent e) {
    // REMOVE: if (pinch.onTouchEvent(e)) return true;
    
    // Keep other handlers working
    if (scroll.onTouchEvent(e)) return true;
    if (longPressDetector.onTouchEvent(e)) return true;
    ...
}
```

### Phase 2: Add ScaleGestureDetector for zoom (1.5h)

**Create ZoomGestureHandler.java:**
```java
public class ZoomGestureHandler {
    private final ScaleGestureDetector scaleDetector;
    private float currentZoom = 1.0f;
    private float pivotX, pivotY;
    
    public ZoomGestureHandler(Context ctx, FBReaderView.Listener listener) {
        scaleDetector = new ScaleGestureDetector(ctx, 
            new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                @Override
                public boolean onScale(ScaleGestureDetector detector) {
                    float factor = detector.getScaleFactor();
                    float newZoom = Math.max(1.0f, Math.min(3.0f, currentZoom * factor));
                    
                    if (newZoom != currentZoom) {
                        currentZoom = newZoom;
                        pivotX = detector.getFocusX();
                        pivotY = detector.getFocusY();
                        listener.onZoomChange(newZoom, pivotX, pivotY);
                    }
                    return true;
                }
            });
    }
    
    public boolean onTouchEvent(MotionEvent e) {
        return scaleDetector.onTouchEvent(e);
    }
    
    public float getZoom() { return currentZoom; }
    public float getPivotX() { return pivotX; }
    public float getPivotY() { return pivotY; }
}
```

**Integrate into ScrollWidget.Gestures:**
```java
// Before other handlers, but doesn't block them!
public boolean onTouchEvent(MotionEvent e) {
    zoomHandler.onTouchEvent(e);  // Detect zoom gesture
    // Don't return true - let other handlers work too!
    
    if (scroll.onTouchEvent(e)) return true;
    if (longPressDetector.onTouchEvent(e)) return true;
    ...
}
```

### Phase 3: Touch Coordinate Adapter (2h)

**Create ZoomTouchAdapter.kt:**
```kotlin
class ZoomTouchAdapter(
    private val fbReaderView: FBReaderView
) {
    fun adaptX(rawX: Float): Int {
        val scale = fbReaderView.scaleX
        val pivotX = fbReaderView.pivotX
        return ((rawX - pivotX) / scale + pivotX).toInt()
    }
    
    fun adaptY(rawY: Float): Int {
        val scale = fbReaderView.scaleY
        val pivotY = fbReaderView.pivotY
        return ((rawY - pivotY) / scale + pivotY).toInt()
    }
    
    fun isInZoom(): Boolean = fbReaderView.scaleX > 1.0f
}
```

**Modify ScrollWidget.Gestures.openCursor():**
```java
// Original:
x = (int) (e.getX() - v.getLeft());
y = (int) (e.getY() - v.getTop());

// With zoom adapter:
ZoomTouchAdapter adapter = fb.getZoomAdapter();
x = adapter.adaptX(e.getX() - v.getLeft());
y = adapter.adaptY(e.getY() - v.getTop());
```

**Modify BookmarksView positioning:**
```java
// When adding bookmark view:
View v = new WordView(fb.getContext());
// Apply same scale transformation
v.setScaleX(fb.getScaleX());
v.setScaleY(fb.getScaleY());
// Position needs coordinate adaptation
```

### Phase 4: Compose State & UI (1h)

**Extend FBReaderView.Listener:**
```java
interface Listener {
    // Existing methods...
    void onZoomChange(float scale, float pivotX, float pivotY);
    void onZoomEnd();
}
```

**Add to ReaderViewModel:**
```kotlin
sealed class ReaderActions {
    // Existing...
    data class ZoomUpdate(val scale: Float, val pivotX: Float, val pivotY: Float) : ReaderActions()
    object ZoomReset : ReaderActions()
}

data class ReaderState.Content(
    // Existing...
    val zoomScale: Float = 1.0f,
    val zoomPivotX: Float = 0f,
    val zoomPivotY: Float = 0f,
    val isInZoom: Boolean = false
)
```

**Modify ReaderContent.kt:**
```kotlin
AndroidView(
    factory = { ctx -> FBReaderView(ctx).apply {
        listener = object : FBReaderView.Listener {
            override fun onZoomChange(scale: Float, pivotX: Float, pivotY: Float) {
                viewModel.onAction(ReaderActions.ZoomUpdate(scale, pivotX, pivotY))
            }
            override fun onZoomEnd() {
                viewModel.onAction(ReaderActions.ZoomReset)
            }
            // ...
        }
    }},
    update = { view ->
        // Apply zoom scale
        if (state.zoomScale != view.scaleX) {
            view.scaleX = state.zoomScale
            view.scaleY = state.zoomScale
            view.pivotX = state.zoomPivotX
            view.pivotY = state.zoomPivotY
        }
    }
)

// Optional: Zoom indicator overlay
if (state.isInZoom) {
    ZoomIndicatorCompose(
        scale = state.zoomScale,
        onReset = { viewModel.onAction(ReaderActions.ZoomReset) }
    )
}
```

**Create ZoomIndicatorCompose.kt:**
```kotlin
@Composable
fun ZoomIndicatorCompose(
    scale: Float,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.padding(8.dp),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${(scale * 100).toInt()}%",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = onReset) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Reset zoom"
                )
            }
        }
    }
}
```

### Phase 5: Double tap zoom (0.5h)

**Add GestureDetector for double tap:**
```java
GestureDetector doubleTapDetector = new GestureDetector(ctx,
    new GestureDetector.SimpleOnGestureListener() {
        @Override
        public boolean onDoubleTap(MotionEvent e) {
            if (currentZoom > 1.0f) {
                // Reset zoom
                listener.onZoomEnd();
            } else {
                // Calculate fit-width zoom
                float pageWidth = getPageContentWidth();
                float screenWidth = fb.getWidth();
                float fitWidthZoom = screenWidth / pageWidth;
                fitWidthZoom = Math.max(1.0f, Math.min(3.0f, fitWidthZoom));
                
                listener.onZoomChange(fitWidthZoom, e.getX(), e.getY());
            }
            return true;
        }
    });
```

### Phase 6: BackHandler for zoom exit (0.5h)

**Add to ReaderContent.kt:**
```kotlin
// Additional BackHandler for zoom
BackHandler(enabled = state.isInZoom) {
    viewModel.onAction(ReaderActions.ZoomReset)
    fbReaderView?.resetZoom()
}
```

---

## 5. Estimated Time

| Phase | Time | Complexity |
|-------|------|------------|
| Remove PinchView | 1h | Low |
| Add ScaleGestureDetector | 1.5h | Medium |
| Touch coordinate adapter | 2h | High |
| Compose state & UI | 1h | Medium |
| Double tap zoom | 0.5h | Low |
| BackHandler | 0.5h | Low |
| Testing | 2h | Medium |
| **Total** | **8h** | |

---

## 6. Risks & Mitigation

| Risk | Impact | Mitigation |
|------|--------|------------|
| Touch coordinates wrong in selection | High | Unit tests for adapter, visual testing |
| Bookmarks positioning broken | High | Verify bookmarks scale with FBReaderView |
| Scroll velocity at zoom | Medium | Adjust scroll factor by zoom |
| Performance at high zoom | Low | Limit max zoom to 3.0, optimize rendering |
| Conflicting gestures (zoom vs scroll) | Medium | Priority handling, threshold detection |

---

## 7. Alternative: Full Compose Rewrite (Long-term)

**If decide to fully migrate FBReaderView to Compose:**

### Architecture:
```kotlin
@Composable
fun ZoomablePdfContainer(
    pdfRenderer: PdfRenderer,
    pages: List<Int>,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    
    val bounds = remember { mutableStateOf(Rect(0, 0, 0, 0)) }
    
    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectTransformGestures { change, gesture ->
                    change.consume()
                    scale *= gesture.zoomFactor
                    scale = scale.coerceIn(1f, 3f)
                    
                    // Adjust offset for pivot
                    offset += gesture.pan
                }
            }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = offset.x
                translationY = offset.y
            }
    ) {
        // PDF pages as LazyColumn
        LazyColumn {
            items(pages) { page ->
                PdfPageCompose(
                    renderer = pdfRenderer,
                    pageNumber = page,
                    scale = scale
                )
            }
        }
        
        // Bookmarks overlay
        BookmarksOverlayCompose(
            bookmarks = bookmarks,
            scale = scale,
            offset = offset
        )
    }
}
```

**Requirements:**
- Rewrite entire FBReaderView overlay system
- Create Compose PDF rendering wrapper
- Implement Compose touch handling for all gestures
- Estimated: **40+ hours**

**Not recommended for now.**

---

## 8. Key Files to Modify

| File | Changes |
|------|---------|
| `FBReaderView.java` | Remove PinchGesture, add Listener.onZoomChange |
| `ScrollWidget.java` | Remove pinch touch interception, add ZoomGestureHandler |
| `ReaderContent.kt` | Add zoom state, AndroidView.update for scale, BackHandler |
| `ReaderViewModel.kt` | Add ZoomUpdate/ZoomReset actions |
| `ReaderState.kt` | Add zoomScale, zoomPivot fields |
| `ZoomTouchAdapter.kt` | New file - coordinate adaptation |
| `ZoomGestureHandler.java` | New file - ScaleGestureDetector wrapper |
| `ZoomIndicatorCompose.kt` | New file - optional zoom indicator UI |

---

## 9. Conclusion

**Recommended: Hybrid Approach (Option C)**

- **Gesture detection:** ScaleGestureDetector in Java (FBReaderView)
- **State management:** ViewModel with Compose state
- **UI overlays:** Optional zoom indicator in Compose
- **Touch adaptation:** ZoomTouchAdapter for coordinate correction
- **Scale application:** scaleX/Y on FBReaderView via AndroidView.update

**Why:**
- Minimal changes to existing Java code
- Compose for state management and optional UI
- Preserves all existing functionality (bookmarks, selection, scroll)
- Solves all 3 issues from TZ

**Next step:** Create implementation plan and start with Phase 1.

---

## 10. Related Documents

- [[zoom-feature-tz]] - Technical specification
- [[zoom-mode-issues]] - Current problems
- [[zoom-scale-research]] - Previous research
- [[bookreader-bookmarks-feature]] - Bookmarks architecture
- [[selection-view-architecture]] - Selection overlay architecture