# DragSortListView

Модуль для drag-and-drop сортировки элементов в ListView. Основан на библиотеке [DragSortListView](https://github.com/CarlBauer/DragSortListView) от Carl Bauer (Apache 2.0).

## Структура модуля

```
dragSortListview/
├── src/main/java/com/mobeta/android/dslv/
│   ├── DragSortListView.kt       # Главный компонент (Java ~3000 строк)
│   ├── DragSortController.kt     # Обработчик touch-жестов
│   ├── DragSortCursorAdapter.kt  # Базовый adapter с поддержкой reorder
│   ├── ResourceDragSortCursorAdapter.kt
│   ├── SimpleDragSortCursorAdapter.kt
│   ├── SimpleFloatViewManager.kt # Менеджер floating view
│   ├── DragSortItemView.kt       # Контейнер для list items
│   └── DragSortItemViewCheckable.kt
└── src/main/res/values/dslv_attrs.xml
```

## Основные компоненты

### DragSortListView

Главный компонент - наследник `ListView` с поддержкой drag-and-drop.

**Ключевые методы:**
- `startDrag(position, dragFlags, deltaX, deltaY)` - начать перетаскивание
- `stopDrag(withVelocity)` - остановить перетаскивание
- `cancelDrag()` - отменить перетаскивание
- `removeItem(position)` - удалить элемент с анимацией
- `moveItem(from, to)` - переместить элемент программно
- `setDragEnabled(boolean)` - включить/выключить drag
- `setFloatViewManager(manager)` - настроить floating view
- `setDropListener(listener)` - слушатель drop событий

**Drag flags:**
- `DRAG_POS_X` - движение вправо
- `DRAG_NEG_X` - движение влево
- `DRAG_POS_Y` - движение вниз
- `DRAG_NEG_Y` - движение вверх

### DragSortController

Обработчик touch-жестов для управления drag-and-drop.

**Режимы инициализации drag:**
- `ON_DOWN` - при нажатии
- `ON_DRAG` - при движении пальца
- `ON_LONG_PRESS` - при долгом нажатии

**Режимы удаления:**
- `CLICK_REMOVE` - удаление по клику
- `FLING_REMOVE` - удаление свайпом

**Пример использования:**
```kotlin
val controller = DragSortController(
    listView,
    R.id.drag_handle,      // ID view для захвата
    DragSortController.ON_DRAG,
    DragSortController.FLING_REMOVE
)
controller.setSortEnabled(true)
controller.setRemoveEnabled(true)
listView.setFloatViewManager(controller)
listView.setOnTouchListener(controller)
```

### DragSortCursorAdapter

CursorAdapter с поддержкой reordering. Отслеживает соответствие между позициями в списке и позициями в Cursor.

**Ключевые методы:**
- `getCursorPosition(position)` - получить позицию в Cursor по позиции в списке
- `getListPosition(cursorPosition)` - обратное преобразование
- `getCursorPositions()` - получить текущий порядок позиций
- `reset()` - сбросить mapping

## XML Attributes

| Attribute | Type | Default | Description |
|-----------|------|---------|-------------|
| `collapsed_height` | dimension | 1dp | Высота свернутого элемента при drag |
| `drag_scroll_start` | float | 0.33 | Начало зоны автоскролла |
| `max_drag_scroll_speed` | float | - | Максимальная скорость скролла |
| `float_background_color` | color | BLACK | Фон floating view |
| `remove_mode` | enum | - | Режим удаления (clickRemove/flingRemove) |
| `float_alpha` | float | 1.0 | Прозрачность floating view |
| `slide_shuffle_speed` | float | 0.7 | Скорость анимации shuffle |
| `remove_animation_duration` | integer | - | Длительность анимации удаления |
| `drop_animation_duration` | integer | - | Длительность анимации drop |
| `drag_enabled` | boolean | true | Включить drag |
| `sort_enabled` | boolean | true | Включить сортировку |
| `remove_enabled` | boolean | false | Включить удаление |
| `drag_start_mode` | enum | onDown | Режим начала drag |
| `drag_handle_id` | id | 0 | ID view для захвата |
| `fling_handle_id` | id | 0 | ID view для fling |
| `click_remove_id` | id | 0 | ID кнопки удаления |

## Пример интеграции

### XML Layout

```xml
<com.mobeta.android.dslv.DragSortListView
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:dslv="http://schemas.android.com/apk/res-auto"
    android:id="@android:id/list"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    dslv:drag_enabled="true"
    dslv:drag_start_mode="onDown"
    dslv:drag_handle_id="@id/drag_handle"
    dslv:float_alpha="0.6"
    dslv:slide_shuffle_speed="0.3" />
```

### Activity/Fragment

```kotlin
class MyActivity : AppCompatActivity() {
    
    private lateinit var listView: DragSortListView
    private lateinit var adapter: MyAdapter
    private lateinit var controller: DragSortController
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        listView = findViewById(R.id.list)
        adapter = MyAdapter(items)
        
        // Настройка controller
        controller = DragSortController(
            listView,
            R.id.drag_handle,
            DragSortController.ON_DRAG,
            DragSortController.FLING_REMOVE
        )
        
        listView.setFloatViewManager(controller)
        listView.setOnTouchListener(controller)
        
        // Слушатель drop событий
        listView.setDropListener { from, to ->
            adapter.moveItem(from, to)
            adapter.notifyDataSetChanged()
        }
        
        // Слушатель удаления
        listView.setRemoveListener { position ->
            adapter.removeItem(position)
            adapter.notifyDataSetChanged()
        }
        
        listView.adapter = adapter
    }
}
```

## Listeners

### DragListener

Вызывается при каждом изменении позиции floating view.

```kotlin
listView.setDragListener { from, to ->
    // from - начальная позиция
    // to - текущая позиция floating view
}
```

### DropListener

Вызывается при drop (завершении drag).

```kotlin
listView.setDropListener { from, to ->
    // Переместить элемент в adapter
}
```

### RemoveListener

Вызывается при удалении элемента.

```kotlin
listView.setRemoveListener { which ->
    // Удалить элемент из adapter
}
```

### DragSortListener

Комбинированный интерфейс для всех событий.

```kotlin
listView.setDragSortListener(object : DragSortListView.DragSortListener {
    override fun drag(from: Int, to: Int) { }
    override fun drop(from: Int, to: Int) { }
    override fun remove(which: Int) { }
})
```

## Важные замечания

1. **Header/Footer views** - позиции в listeners не включают header/footer views
2. **Adapter** - adapter должен корректно обрабатывать `notifyDataSetChanged()`
3. **View recycling** - drag handle view должен корректно обрабатываться при recycling
4. **Touch events** - DragSortController перехватывает touch события; при необходимости используйте `setOnTouchListener` после controller

## Известные проблемы

- Использует deprecated API (`getDrawingCache`) - при обновлении Android может потребоваться изменения
- Не совместим с RecyclerView (только ListView)

## Использование в проекте

Модуль используется в `:fbreader` для сортировки каталогов в `CatalogManagerActivity`.
