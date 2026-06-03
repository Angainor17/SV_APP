# SuperToasts Module

Библиотека для отображения кастомных Toast-уведомлений в Android приложениях.

## Обзор

Модуль `superToasts` предоставляет расширенные возможности для отображения уведомлений поверх стандартного Android Toast API. Библиотека поддерживает различные типы уведомлений, анимации и жесты.

## Основные классы

### SuperToast
Базовый класс для отображения уведомлений поверх всех окон приложения.

```kotlin
// Простое использование
SuperToast.create(context, "Сообщение", SuperToast.Duration.SHORT).show()

// С настройкой стиля
val toast = SuperToast(context, Style.getStyle(Style.BLUE))
toast.setText("Стилизованное сообщение")
toast.show()
```

### SuperActivityToast
Уведомления, привязанные к жизненному циклу Activity. Автоматически уничтожаются при уничтожении Activity.

```kotlin
// Простое использование
SuperActivityToast.create(activity, "Сообщение", SuperToast.Duration.SHORT).show()

// С кнопкой действия
val toast = SuperActivityToast(activity, SuperToast.Type.BUTTON)
toast.setText("Элемент удалён")
toast.setButtonText("ОТМЕНИТЬ")
toast.setOnClickWrapper(OnClickWrapper("undo") { view, token ->
    // Обработка нажатия
})
toast.show()
```

### SuperCardToast
Уведомления в стиле карточек, отображаемые в верхней части Activity. Требует наличия контейнера `card_container` в layout.

```xml
<!-- В layout XML -->
<LinearLayout
    android:id="@+id/card_container"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical" />
```

```kotlin
val toast = SuperCardToast(activity)
toast.setText("Карточное уведомление")
toast.setSwipeToDismiss(true)  // Закрытие свайпом
toast.show()
```

## Типы уведомлений

| Тип | Описание |
|-----|----------|
| `STANDARD` | Стандартное текстовое уведомление |
| `BUTTON` | Уведомление с кнопкой действия |
| `PROGRESS` | Уведомление с круговым индикатором прогресса |
| `PROGRESS_HORIZONTAL` | Уведомление с горизонтальным прогресс-баром |

## Анимации

| Анимация | Описание |
|----------|----------|
| `FADE` | Плавное появление/исчезновение |
| `FLYIN` | Вылет сбоку |
| `SCALE` | Масштабирование |
| `POPUP` | Появление снизу |

## Стили

Предустановленные стили фона:

```kotlin
Style.BLACK   // Чёрный фон
Style.BLUE    // Синий фон
Style.GRAY    // Серый фон (по умолчанию)
Style.GREEN   // Зелёный фон
Style.ORANGE  // Оранжевый фон
Style.PURPLE  // Фиолетовый фон
Style.RED     // Красный фон
Style.WHITE   // Белый фон
```

## Обработка поворота экрана

Для сохранения уведомлений при повороте экрана:

```kotlin
override fun onSaveInstanceState(outState: Bundle) {
    SuperActivityToast.onSaveState(outState)
    // или SuperCardToast.onSaveState(outState)
    super.onSaveInstanceState(outState)
}

override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    SuperActivityToast.onRestoreState(savedInstanceState, this)
    // или SuperCardToast.onRestoreState(savedInstanceState, this)
}
```

Для восстановления слушателей кликов:

```kotlin
val wrappers = Wrappers()
wrappers.add(OnClickWrapper("action_tag") { view, token ->
    // Обработка клика
})

SuperActivityToast.onRestoreState(savedInstanceState, this, wrappers)
```

## Длительность

```kotlin
SuperToast.Duration.VERY_SHORT  // 1500 мс
SuperToast.Duration.SHORT       // 2000 мс
SuperToast.Duration.MEDIUM      // 2750 мс
SuperToast.Duration.LONG        // 3500 мс
SuperToast.Duration.EXTRA_LONG  // 4500 мс
```

## Иконки

```kotlin
// Тёмные иконки (для светлых фонов)
SuperToast.Icon.Dark.EDIT
SuperToast.Icon.Dark.EXIT
SuperToast.Icon.Dark.INFO
SuperToast.Icon.Dark.REDO
SuperToast.Icon.Dark.REFRESH
SuperToast.Icon.Dark.SAVE
SuperToast.Icon.Dark.SHARE
SuperToast.Icon.Dark.UNDO

// Светлые иконки (для тёмных фонов)
SuperToast.Icon.Light.EDIT
SuperToast.Icon.Light.EXIT
// ... и т.д.
```

## Менеджеры

Внутренние классы для управления очередью уведомлений:

- `ManagerSuperToast` — управляет очередью `SuperToast`
- `ManagerSuperActivityToast` — управляет очередью `SuperActivityToast`
- `ManagerSuperCardToast` — управляет списком `SuperCardToast`

## Вспомогательные классы

### Style
Конфигурация стиля уведомления (цвета, анимации, шрифты).

### Wrappers
Контейнер для хранения `OnClickWrapper` и `OnDismissWrapper` при восстановлении состояния.

### OnClickWrapper
Обёртка для слушателя кликов, поддерживающая восстановление после поворота экрана.

### OnDismissWrapper
Обёртка для слушателя закрытия уведомления.

### SwipeDismissListener
Обработчик жеста свайпа для закрытия уведомления.

## Примечания

- `SuperToast` отображается поверх всех окон (использует `WindowManager`)
- `SuperActivityToast` и `SuperCardToast` привязаны к Activity
- Для `SuperCardToast` требуется `LinearLayout` с id `card_container`
- При использовании `PROGRESS` и `PROGRESS_HORIZONTAL` типов управление прогрессом остаётся за разработчиком
