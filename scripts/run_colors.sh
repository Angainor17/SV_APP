#!/bin/bash

# Генерация скриншотов с разными цветами фона Material Design
# Цвет применяется к: toolbar, bottomNav (4 пункта), фон под списком

COLOR_FILE="commonui/src/main/java/su/sv/commonui/theme/Color.kt"
SCREENSHOT_DIR="background_colors"
PACKAGE="su.sv.app"
ACTIVITY="$PACKAGE/.MainActivity"

mkdir -p "$SCREENSHOT_DIR"

# Цвета (имя:hex)
COLORS=(
    "Grey_50:0xFFFAFAFA"
    "Grey_100:0xFFF5F5F5"
    "Grey_200:0xFFEEEEEE"
    "BlueGrey_50:0xFFECEFF1"
    "BlueGrey_100:0xFFCFD8DC"
    "Blue_50:0xFFE3F2FD"
    "Blue_100:0xFFBBDEFB"
    "LightBlue_50:0xFFE1F5FE"
    "LightBlue_100:0xFFB3E5FC"
    "Indigo_50:0xFFE8EAF6"
    "Cyan_50:0xFFE0F7FA"
    "Cyan_100:0xFFB2EBF2"
    "Teal_50:0xFFE0F2F1"
    "Teal_100:0xFFB2DFDB"
    "Green_50:0xFFE8F5E9"
    "LightGreen_50:0xFFF1F8E9"
    "Amber_50:0xFFFFF8E1"
    "DeepOrange_50:0xFFFBE9E7"
    "Purple_50:0xFFF3E5F5"
    "DeepPurple_50:0xFFEDE7F6"
)

echo "=========================================="
echo "Генерация 20 скриншотов"
echo "=========================================="

counter=1
total=20

for entry in "${COLORS[@]}"; do
    name="${entry%%:*}"
    hex="${entry##*:}"

    echo "[$counter/$total] $name ($hex)"

    # Заменяем цвет
    sed -i.bak "s/val LightBackgroundBase = Color(0x[A-Fa-f0-9]*)/val LightBackgroundBase = Color($hex)/" "$COLOR_FILE"

    # Билд
    ./gradlew assembleDebug --quiet 2>&1 | tail -1

    # Установка и запуск
    adb install -r app/build/outputs/apk/debug/app-debug.apk 2>&1 >/dev/null
    adb shell am start -n "$ACTIVITY" >/dev/null

    # Ждём 15 секунд для полной загрузки новостной ленты
    sleep 15

    # Скриншот
    adb shell screencap -p /sdcard/screen.png
    adb pull /sdcard/screen.png "$SCREENSHOT_DIR/$name.png" >/dev/null 2>&1
    adb shell rm /sdcard/screen.png
    adb shell am force-stop "$PACKAGE"

    rm -f "$COLOR_FILE.bak"
    echo "  ✓ $name.png"
    counter=$((counter + 1))
done

# Восстанавливаем Blue 100
sed -i.bak "s/val LightBackgroundBase = Color(0x[A-Fa-f0-9]*)/val LightBackgroundBase = Color(0xFFBBDEFB)/" "$COLOR_FILE"
rm -f "$COLOR_FILE.bak"

echo ""
echo "✓ ЗАВЕРШЕНО! Скриншоты в: $SCREENSHOT_DIR/"
ls -la "$SCREENSHOT_DIR"