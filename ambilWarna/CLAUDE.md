# AmbilWarna Module

Модуль для выбора цвета в Android приложениях.

## Обзор

`ambilWarna` — это библиотека для отображения диалогового окна выбора цвета (Color Picker). Позволяет пользователям выбирать цвет с помощью HSV (Hue, Saturation, Value) модели.

## Основные классы

### AmbilWarnaDialog
Диалоговое окно для выбора цвета.

```kotlin
val dialog = AmbilWarnaDialog(
    context,
    currentColor,  // Текущий цвет (Int)
    object : AmbilWarnaDialog.OnAmbilWarnaListener {
        override fun onOk(dialog: AmbilWarnaDialog, color: Int) {
            // Пользователь выбрал цвет
        }

        override fun onCancel(dialog: AmbilWarnaDialog) {
            // Пользователь отменил выбор
        }
    },
    "OK",      // Текст кнопки подтверждения
    "Отмена"   // Текст кнопки отмены
)
dialog.show()
```

### AmbilWarnaKotak
Кастомная View для отображения цветового поля (насыщенность/яркость).

### AmbilWarnaPrefWidgetView
Виджет для использования в PreferenceScreen.

## Как работает

1. Цвет представлен в HSV модели:
   - **Hue (Тон)**: Вертикальная полоса слева (0-360°)
   - **Saturation (Насыщенность)**: Ось X квадратного поля
   - **Value (Яркость)**: Ось Y квадратного поля

2. Пользователь:
   - Двигает курсор по вертикальной полосе для выбора тона
   - Двигает курсор по квадратному полю для выбора насыщенности и яркости

## Структура файлов

```
ambilWarna/src/main/java/yuku/ambilwarna/
├── AmbilWarnaDialog.kt      # Основной диалог
├── AmbilWarnaKotak.kt       # Кастомная View для выбора S/V
└── widget/
    └── AmbilWarnaPrefWidgetView.kt  # Виджет для настроек
```

## Зависимости

Модуль использует стандартные Android SDK классы:
- `AlertDialog` для диалога
- `Canvas`, `Paint` для отрисовки
- `MotionEvent` для обработки касаний
