#!/bin/bash

# Генерация скриншотов с теплыми коричневыми оттенками (вокруг Brown_200)
# Цвет применяется к: toolbar, bottomNav (4 пункта), фон под списком

COLOR_FILE="commonui/src/main/java/su/sv/commonui/theme/Color.kt"
SCREENSHOT_DIR="background_colors_brown"
PACKAGE="su.sv.app"
ACTIVITY="$PACKAGE/.MainActivity"

mkdir -p "$SCREENSHOT_DIR"

# Тёплые коричневые оттенки вокруг Brown_200 (#BCAAA4)
COLORS=(
    # Brown palette
    "Brown_50:0xFFEFEBE9"
    "Brown_100:0xFFD7CCC8"
    "Brown_200:0xFFBCAAA4"
    "Brown_300:0xFFA1887F"
    # Taupe variations
    "Taupe_Light:0xFFD8CFC4"
    "Taupe_Medium:0xFFC9B8A8"
    "Taupe_Warm:0xFFC4B5A5"
    # Warm gray variations
    "WarmGray_50:0xFFF5F0EB"
    "WarmGray_100:0xFFE8E0D8"
    "WarmGray_150:0xFFDDD3CA"
    # Beige/Coffee tones
    "Latte:0xFFE6DCD0"
    "Cappuccino:0xFFD4C4B0"
    "Mocha:0xFFCDBAAB"
    "Espresso_Light:0xFFC8B8A8"
    # Earth tones
    "Sand:0xFFE8DFD5"
    "Clay:0xFFD1C2B5"
    "Stone:0xFFC5B8AB"
    "Pebble:0xFFBFB0A3"
    # Special mixes
    "Mushroom:0xFFC8BCAD"
    "Fawn:0xFFD2C4B5"
)

echo "=========================================="
echo "Генерация 20 скриншотов (коричневые)"
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