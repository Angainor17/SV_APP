---
name: zoom-scale-research
description: Research findings для zoom via scale approach
metadata: 
  node_type: memory
  type: project
  originSessionId: 475fe7af-7539-4b95-8265-35e25b837b07
---

# Zoom via Scale Research

## Date: 2026-07-03

## Architecture Analysis

### View Hierarchy
```
FBReaderView (RelativeLayout)
├── ScrollWidget (RecyclerView) - pages container
│   └── PageView[] (View) - individual pages
│       ├── draws page content via onDraw()
│       └── contains overlays refs (selection, links, tts, search)
│       └── BUT bookmarks are NOT here!
├── BookmarksView.WordView[] - added to FBReaderView directly via fb.addView()
├── SelectionView - PDF selection overlay
├── PinchView - current zoom overlay (intercepts touch)
└── Other overlays...
```

### Key Finding: Bookmarks Architecture

**BookmarksView.java (line 1857-1860):**
```java
public void addView(View v) {
    bookmarks.add(v);
    fb.addView(v);  // <-- Added to FBReaderView, NOT PageView!
}
```

Bookmarks Views добавляются в FBReaderView как direct children, не в PageView!

### Touch Handling

**ScrollWidget.Gestures.openCursor() (line 1587-1588):**
```java
x = (int) (e.getX() - v.getLeft());
y = (int) (e.getY() - v.getTop());
```

Coordinates вычисляются relative to PageView. При scale нужно адаптировать:
```java
x = (int) ((e.getX() - v.getLeft()) / v.getScaleX());
y = (int) ((e.getY() - v.getTop()) / v.getScaleY());
```

### PageView Content

**PageView.onDraw() - draws:**
- Page bitmap (from pluginview.render())
- Selection overlay (if selection != null)
- Bookmarks overlay is NOT drawn here!

## Scale Approaches

### Approach 1: Scale FBReaderView
```java
fbReaderView.setScaleX(zoomFactor);
fbReaderView.setScaleY(zoomFactor);
```

**Pros:**
- All children scale automatically (including bookmarks)
- Simple implementation
- ScrollWidget, PageViews, BookmarksView all scale

**Cons:**
- Need to adapt ALL touch coordinates
- Scale affects entire FBReaderView including topBar area
- Need to scale pivot point (where pinch started)

### Approach 2: Scale ScrollWidget only
```java
scrollWidget.setScaleX(zoomFactor);
scrollWidget.setScaleY(zoomFactor);
```

**Pros:**
- Only pages scale, topBar unaffected
- Bookmarks in PageView would scale

**Cons:**
- Bookmarks Views are NOT in ScrollWidget! They're in FBReaderView!
- Need separate handling for bookmarks
- More complex touch coordinate adaptation

### Approach 3: Scale PageViews individually
```java
for (PageView page : visiblePages) {
    page.setScaleX(zoomFactor);
    page.setScaleY(zoomFactor);
}
```

**Pros:**
- Fine-grained control
- Can scale only visible pages

**Cons:**
- Bookmarks Views still in FBReaderView, not scaled
- RecyclerView layout issues (PageViews overlap)
- Complex touch handling

### Approach 4: Matrix transformation
```java
canvas.save();
canvas.scale(zoomFactor, zoomFactor);
// draw content
canvas.restore();
```

**Pros:**
- No actual view scale
- Touch coordinates unchanged (event.getX/Y unchanged)
- Bookmarks positioning needs manual matrix

**Cons:**
- Applied in onDraw() of each PageView
- Bookmarks Views use absolute coordinates - need manual update
- More complex implementation

## Recommended Approach

**Approach 1: Scale FBReaderView** with:

1. **Pivot point** - scale around pinch center
2. **Touch coordinate adapter** - divide by scale factor
3. **Gesture detector** - pinch gesture without PinchView overlay
4. **Zoom controls UI** - +/- buttons, reset button

### Implementation Steps

1. Remove PinchGesture overlay creation
2. Add scale gesture detection to FBReaderView
3. Apply scale to FBReaderView with pivot
4. Create TouchCoordinateAdapter class
5. Modify all touch handlers to use adapter
6. Add zoom controls UI
7. Handle scroll at zoomed scale

### Touch Coordinate Adapter

```java
public class ZoomTouchAdapter {
    private float zoomFactor = 1.0f;
    private float pivotX = 0;
    private float pivotY = 0;

    public float adaptX(float rawX) {
        // Scale relative coordinates
        return (rawX - pivotX) / zoomFactor + pivotX;
    }

    public float adaptY(float rawY) {
        return (rawY - pivotY) / zoomFactor + pivotY;
    }

    public void setZoom(float factor, float pivotX, float pivotY) {
        this.zoomFactor = factor;
        this.pivotX = pivotX;
        this.pivotY = pivotY;
    }
}
```

## Estimated Complexity

| Component | Difficulty | Time |
|-----------|------------|------|
| Scale gesture detection | Medium | 1h |
| FBReaderView scale application | Low | 30min |
| Touch coordinate adapter | High | 2h |
| Zoom controls UI | Low | 1h |
| Testing + fixes | Medium | 2h |
| **Total** | | **6.5h** |

## Risks

1. **Bookmarks positioning** - need to verify they scale correctly
2. **Selection handling** - PDF selection may need coordinate fixes
3. **Scroll behavior** - zoomed scroll needs velocity adaptation
4. **Performance** - large zoom factors may affect rendering

## Next Step

Create prototype with basic scale gesture and verify bookmarks visibility.