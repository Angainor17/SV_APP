#!/bin/bash

# Генерация скриншотов с бордово-красными оттенками Material Design
# Цвет применяется к: toolbar, bottomNav (4 пункта), фон под списком

COLOR_FILE="commonui/src/main/java/su/sv/commonui/theme/Color.kt"
SCREENSHOT_DIR="background_colors_red"
PACKAGE="su.sv.app"
ACTIVITY="$PACKAGE/.MainActivity"

mkdir -p "$SCREENSHOT_DIR"

# Бордово-красные оттенки (имя:hex)
COLORS=(
    # Red palette
    "Red_50:0xFFFFEBEE"
    "Red_100:0xFFFFCDD2"
    "Red_200:0xFFEF9A9A"
    # Pink palette
    "Pink_50:0xFFFCE4EC"
    "Pink_100:0xFFF8BBD0"
    "Pink_200:0xFFF48FB1"
    # Deep Orange palette (бордовый)
    "DeepOrange_50:0xFFFBE9E7"
    "DeepOrange_100:0xFFFFCCBC"
    "DeepOrange_200:0xFFE57373"
    # Purple palette (фиолетово-красный)
    "Purple_50:0xFFF3E5F5"
    "Purple_100:0xFFE1BEE7"
    "Purple_200:0xFFCE93D8"
    # Deep Purple (индиго с красным)
    "DeepPurple_50:0xFFEDE7F6"
    "DeepPurple_100:0xFFD1C4E9"
    # Rose (Material 3)
    "Rose_50:0xFFFFF0F0"
    "Rose_100:0xFFFFD9E0"
    # Brown palette (коричнево-красный)
    "Brown_100:0xFFD7CCC8"
    "Brown_200:0xFFBCAAA4"
    # Миксы
    "Mauve_50:0xFFF4E4F4"
    "Wine_100:0xFFE8D0D0"
)

echo "=========================================="
echo "Генерация 20 скриншотов (красные оттенки)"
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