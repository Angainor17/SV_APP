#!/bin/bash

# Полный скрипт для генерации скриншотов с разными цветами фона
# Material Design Colors - Light Theme Backgrounds

set -e

COLOR_FILE="commonui/src/main/java/su/sv/commonui/theme/Color.kt"
SCREENSHOT_DIR="background_colors"
PACKAGE="su.sv.app"
ACTIVITY="$PACKAGE/.MainActivity"
PROJECT_ROOT="/Users/i.u.voronin/StudioProjects/SV_APP"

cd "$PROJECT_ROOT"

# Создаем папку для скриншотов
mkdir -p "$SCREENSHOT_DIR"

# Цвета для тестирования (Material Design)
declare -A COLORS=(
    ["Grey_50"]="0xFFFAFAFA"
    ["Grey_100"]="0xFFF5F5F5"
    ["Grey_200"]="0xFFEEEEEE"
    ["BlueGrey_50"]="0xFFECEFF1"
    ["BlueGrey_100"]="0xFFCFD8DC"
    ["Blue_50"]="0xFFE3F2FD"
    ["Blue_100"]="0xFFBBDEFB"
    ["LightBlue_50"]="0xFFE1F5FE"
    ["LightBlue_100"]="0xFFB3E5FC"
    ["Indigo_50"]="0xFFE8EAF6"
    ["Cyan_50"]="0xFFE0F7FA"
    ["Cyan_100"]="0xFFB2EBF2"
    ["Teal_50"]="0xFFE0F2F1"
    ["Teal_100"]="0xFFB2DFDB"
    ["Green_50"]="0xFFE8F5E9"
    ["LightGreen_50"]="0xFFF1F8E9"
    ["Amber_50"]="0xFFFFF8E1"
    ["DeepOrange_50"]="0xFFFBE9E7"
    ["Purple_50"]="0xFFF3E5F5"
    ["DeepPurple_50"]="0xFFEDE7F6"
)

# Описание цветов
declare -A DESC=(
    ["Grey_50"]="Почти белый"
    ["Grey_100"]="Светло-серый"
    ["Grey_200"]="Заметный серый"
    ["BlueGrey_50"]="Холодный серо-голубой"
    ["BlueGrey_100"]="Более заметный BG"
    ["Blue_50"]="Очень светлый голубой"
    ["Blue_100"]="Чистый синий"
    ["LightBlue_50"]="Светлый голубой LB"
    ["LightBlue_100"]="Яркий голубой"
    ["Indigo_50"]="Синий с фиолетовым"
    ["Cyan_50"]="Светлый cyan"
    ["Cyan_100"]="Заметный cyan"
    ["Teal_50"]="Светлый teal"
    ["Teal_100"]="Teal green"
    ["Green_50"]="Светлый зелёный"
    ["LightGreen_50"]="Очень светлый зелёный"
    ["Amber_50"]="Тёплый кремовый"
    ["DeepOrange_50"]="Тёплый peach"
    ["Purple_50"]="Светлый фиолетовый"
    ["DeepPurple_50"]="Индиго-like DP"
)

echo "=========================================="
echo "Генерация скриншотов: ${COLORS[@]}"
echo "=========================================="
echo ""

counter=1
total=${#COLORS[@]}

for name in "${!COLORS[@]}"; do
    hex="${COLORS[$name]}"
    desc="${DESC[$name]}"

    echo "[$counter/$total] $name: $desc ($hex)"
    echo "----------------------------------------"

    # Заменяем цвет в Color.kt
    sed -i.bak "s/val LightBackgroundBase = Color(0x[A-Fa-f0-9]*)/val LightBackgroundBase = Color($hex)/" "$COLOR_FILE"

    # Быстрый билд (только assembleDebug, без тестов)
    echo "  Building..."
    ./gradlew assembleDebug --quiet 2>&1 | tail -3

    if [ $? -eq 0 ]; then
        echo "  Build OK"

        # Установка
        echo "  Installing..."
        adb install -r app/build/outputs/apk/debug/app-debug.apk 2>&1 | grep -v "Performing" | grep -v "Success" || true

        # Запуск приложения
        echo "  Launching..."
        adb shell am start -n "$ACTIVITY" >/dev/null 2>&1

        # Ждём загрузки UI
        sleep 3

        # Скриншот
        echo "  Taking screenshot..."
        adb shell screencap -p /sdcard/temp_screen.png
        adb pull /sdcard/temp_screen.png "$SCREENSHOT_DIR/$name.png" 2>&1 | grep -v "bytes in" || true
        adb shell rm /sdcard/temp_screen.png

        # Останавливаем приложение
        adb shell am force-stop "$PACKAGE"

        echo "  ✓ Saved: $SCREENSHOT_DIR/$name.png"
    else
        echo "  ✗ Build FAILED"
    fi

    # Удаляем backup файл
    rm -f "$COLOR_FILE.bak"

    echo ""
    counter=$((counter + 1))
done

# Восстанавливаем исходный цвет (Blue 100)
sed -i.bak "s/val LightBackgroundBase = Color(0x[A-Fa-f0-9]*)/val LightBackgroundBase = Color(0xFFBBDEFB)/" "$COLOR_FILE"
rm -f "$COLOR_FILE.bak"

# Создаем индексный файл с описаниями
echo "=========================================="
echo "Creating index.html..."
echo "=========================================="

cat > "$SCREENSHOT_DIR/index.html" << 'EOF'
<!DOCTYPE html>
<html>
<head>
    <title>Background Colors Comparison</title>
    <style>
        body { font-family: system-ui; background: #1a1a1a; color: #fff; padding: 20px; }
        h1 { color: #BBDEFB; }
        .grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 20px; }
        .card { background: #2a2a2a; padding: 10px; border-radius: 8px; }
        .card img { width: 100%; border-radius: 4px; }
        .card h3 { margin: 10px 0 5px; color: #BBDEFB; }
        .card p { margin: 0; color: #888; font-size: 12px; }
        .color-box { display: inline-block; width: 20px; height: 20px; border-radius: 4px; margin-right: 8px; }
    </style>
</head>
<body>
    <h1>Material Design Background Colors</h1>
    <div class="grid">
EOF

for name in Grey_50 Grey_100 Grey_200 BlueGrey_50 BlueGrey_100 Blue_50 Blue_100 LightBlue_50 LightBlue_100 Indigo_50 Cyan_50 Cyan_100 Teal_50 Teal_100 Green_50 LightGreen_50 Amber_50 DeepOrange_50 Purple_50 DeepPurple_50; do
    hex="${COLORS[$name]}"
    desc="${DESC[$name]}"
    # Конвертируем hex в CSS цвет (без 0xFF prefix)
    css_color="#${hex:4}"

    echo "        <div class=\"card\">
            <img src=\"$name.png\" alt=\"$name\">
            <h3><span class=\"color-box\" style=\"background:$css_color\"></span>$name</h3>
            <p>$desc</p>
            <p style=\"color:#666\">$hex</p>
        </div>" >> "$SCREENSHOT_DIR/index.html"
done

cat >> "$SCREENSHOT_DIR/index.html" << 'EOF'
    </div>
</body>
</html>
EOF

echo ""
echo "=========================================="
echo "✓ ЗАВЕРШЕНО!"
echo "=========================================="
echo "Скриншоты: $PROJECT_ROOT/$SCREENSHOT_DIR/"
echo "Откройте: $PROJECT_ROOT/$SCREENSHOT_DIR/index.html"
echo "Исходный цвет восстановлен: Blue 100 (0xFFBBDEFB)"