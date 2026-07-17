#!/bin/bash

# Скрипт для генерации скриншотов с разными цветами фона
# Запускается из корня проекта SV_APP

COLOR_FILE="commonui/src/main/java/su/sv/commonui/theme/Color.kt"
SCREENSHOT_DIR="background_colors"

# Создаем папку для скриншотов
mkdir -p "$SCREENSHOT_DIR"

# Массив цветов (Material Design)
COLORS=(
    "Grey_50:0xFFFAFAFA:Почти белый"
    "Grey_100:0xFFF5F5F5:Светло-серый"
    "Grey_200:0xFFEEEEEE:Заметный серый"
    "BlueGrey_50:0xFFECEFF1:Холодный серо-голубой"
    "BlueGrey_100:0xFFCFD8DC:Более заметный"
    "Blue_50:0xFFE3F2FD:Очень светлый голубой"
    "Blue_100:0xFFBBDEFB:Чистый синий (текущий)"
    "LightBlue_50:0xFFE1F5FE:Светлый голубой"
    "LightBlue_100:0xFFB3E5FC:Яркий голубой"
    "Indigo_50:0xFFE8EAF6:Синий с фиолетовым"
    "Cyan_50:0xFFE0F7FA:Светлый cyan"
    "Cyan_100:0xFFB2EBF2:Заметный cyan"
    "Teal_50:0xFFE0F2F1:Светлый teal"
    "Teal_100:0xFFB2DFDB:Teal green"
    "Green_50:0xFFE8F5E9:Светлый зелёный"
    "LightGreen_50:0xFFF1F8E9:Очень светлый зелёный"
    "Amber_50:0xFFFFF8E1:Тёплый кремовый"
    "DeepOrange_50:0xFFFBE9E7:Тёплый peach"
    "Purple_50:0xFFF3E5F5:Светлый фиолетовый"
    "DeepPurple_50:0xFFEDE7F6:Индиго-like"
)

# Функция для замены цвета в файле
change_color() {
    local color_hex="$1"
    # Заменяем LightBackgroundBase
    sed -i '' "s/val LightBackgroundBase = Color(0x[A-F0-9]*)/val LightBackgroundBase = Color($color_hex)/" "$COLOR_FILE"
}

# Функция для восстановления исходного цвета
restore_color() {
    sed -i '' "s/val LightBackgroundBase = Color(0x[A-F0-9]*)/val LightBackgroundBase = Color(0xFFBBDEFB)/" "$COLOR_FILE"
}

echo "=== Генерация скриншотов для 20 цветов ==="
echo "Папка: $SCREENSHOT_DIR"
echo ""

# Проходим по всем цветам
for color_entry in "${COLORS[@]}"; do
    # Разбираем запись
    IFS=':' read -r name hex desc <<< "$color_entry"

    echo "--- $name: $desc ($hex) ---"

    # Меняем цвет
    change_color "$hex"

    # Собираем (быстро, только изменение цвета)
    echo "Building..."
    ./gradlew assembleDebug -q 2>/dev/null

    if [ $? -eq 0 ]; then
        echo "Build successful"

        # TODO: Запуск приложения и скриншот - делается через /run skill или adb
        echo "Screenshot would be saved as: $SCREENSHOT_DIR/$name.png"

        # Для реального скриншота нужен запуск эмулятора/устройства
        # adb shell screencap -p /sdcard/screen.png
        # adb pull /sdcard/screen.png "$SCREENSHOT_DIR/$name.png"
    else
        echo "Build failed for $name"
    fi

    echo ""
done

# Восстанавливаем исходный цвет
restore_color
echo "=== Завершено. Исходный цвет восстановлен. ==="