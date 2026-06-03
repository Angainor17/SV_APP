# AmbilWarna Module

Модуль для выбора цвета в Android приложениях.

## Обзор

`ambilWarna` — это библиотека для отображения диалогового окна выбора цвета (Color Picker). Позволяет пользователям выбирать цвет с помощью HSV (Hue, Saturation, Value) модели.

## Основные классы

### AmbilWarnaDialog
Диалоговое окно для выбора цвета.

```java
AmbilWarnaDialog dialog = new AmbilWarnaDialog(
    context,
    currentColor,  // Текущий цвет (int)
    new AmbilWarnaDialog.OnAmbilWarnaListener() {
        @Override
        public void onOk(AmbilWarnaDialog dialog, int color) {
            // Пользователь выбрал цвет
        }

        @Override
        public void onCancel(AmbilWarnaDialog dialog) {
            // Пользователь отменил выбор
        }
    },
    "OK",      // Текст кнопки подтверждения
    "Отмена"   // Текст кнопки отмены
);
dialog.show();
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
├── AmbilWarnaDialog.java      # Основной диалог
├── AmbilWarnaKotak.java       # Кастомная View для выбора S/V
└── widget/
    └── AmbilWarnaPrefWidgetView.java  # Виджет для настроек
```

## Зависимости

Модуль использует стандартные Android SDK классы:
- `AlertDialog` для диалога
- `Canvas`, `Paint` для отрисовки
- `MotionEvent` для обработки касаний
