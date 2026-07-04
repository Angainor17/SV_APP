---
name: fullscreen-flicker-issue
description: Проблема мерцания при fullscreen transition в ScrollWidget
metadata: 
  node_type: memory
  type: project
  originSessionId: 475fe7af-7539-4b95-8265-35e25b837b07
---

# Fullscreen Flicker Issue (ScrollWidget)

## Status
**Open** - отложено на потом

## Problem Description
При переходе в полноэкранный режим в **ScrollWidget** (непрерывный режим) происходит мерцание тёмного фона. Цвет мерцания совпадает с тулбаром.

В **PagerWidget** (постраничный режим) - мерцание отсутствует.

## Root Cause Analysis

### Heights Comparison из логов

**ENTER fullscreen (21:02:19):**
```
DecorView size: 1080x2400
FBReaderView size: 1080x2037  ← height НЕ full (учитывает toolbar)
ScrollWidget mainAreaHeight: 2016  ← height НЕ full
```

**EXIT fullscreen (21:02:22):**
```
FBReaderView size: 1080x2400  ← height теперь full (+363px)
ScrollWidget mainAreaHeight: 2379  ← height теперь full (+363px)
```

### Problem

При ENTER fullscreen:
1. System bars скрываются (native)
2. Scaffold topBar скрывается (Compose)
3. **НО**: FBReaderView и ScrollWidget heights НЕ обновляются сразу
4. Heights остаются 2037/2016 вместо 2400/2379
5. Empty space (363px = toolbar height) показывается с тёмным background
6. **Это вызывает мерцание**

### Timing Problem

```
19.070 - Action: ENTER fullscreen
19.071 - State updated - Scaffold topBar will be: false
[BUT heights не изменились!]
22.354 - EXIT fullscreen
22.354 - FBReaderView size: 1080x2400  ← heights обновились только здесь!
```

Heights обновляются только при **EXIT** fullscreen, не при ENTER.

## Technical Details

### View Hierarchy
```
DecorView (1080x2400)
└── FBReaderView (1080x2037 → 1080x2400 при EXIT)
    └── ScrollWidget (2016 → 2379 mainAreaHeight)
        └── RecyclerView
```

### Why PagerWidget Works
PagerWidget вероятно имеет другой layout mechanism который правильно обновляет heights при fullscreen transition.

### Why ScrollWidget Fails
ScrollWidget использует RecyclerView с `mainAreaHeight` который не обновляется сразу при fullscreen.

## Possible Solutions

1. **Force layout update** после fullscreen toggle
   ```java
   // После fullscreen toggle
   if (widget instanceof ScrollWidget) {
       widget.requestLayout();
       widget.invalidate();
   }
   ```

2. **Update padding/margin** вместо waiting for layout
   ```java
   // ScrollWidget должен update padding при fullscreen
   ```

3. **Use animation** для smooth height transition
   ```java
   // Animate height change от 2037 до 2400
   ```

4. **Different approach**: не hide Scaffold, а только hide topBar content

## Related Files

- `FBReaderView.java` - toggleFullscreen(), exitFullscreen()
- `ScrollWidget.java` - getMainAreaHeight(), layout
- `ReaderContent.kt` - Scaffold, isFullscreen state
- `ReaderViewModel.kt` - setFullscreen()

## Related Issues

- [[selection-view-refactoring-plan]] - SelectionView также использует mainAreaHeight для clip

## Next Steps

1. Investigate `ScrollWidget.getMainAreaHeight()` - почему не обновляется при ENTER fullscreen
2. Compare с `PagerWidget` - как он обновляет heights
3. Add `requestLayout()` после fullscreen toggle
4. Или использовать другой подход для fullscreen (hide topBar content instead of Scaffold)

## Debug Commands

```bash
adb logcat -s "voronin:D" | grep -E "Fullscreen|height|size|ScrollWidget"
```