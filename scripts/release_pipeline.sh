#!/bin/bash

# =====================================================
# Скрипт для полного релизного цикла
# =====================================================
# Использование:
#   ./scripts/release_pipeline.sh
# =====================================================

set -e

# Цвета для вывода
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}   SV APP - Release Pipeline${NC}"
echo -e "${BLUE}========================================${NC}"

# Шаг 1: Проверка кода (lint)
echo -e "\n${BLUE}📝 Step 1: Running lint checks...${NC}"
./gradlew ktlintCheck --quiet || {
    echo -e "${YELLOW}⚠️  Lint issues found. Fix them before release.${NC}"
    read -p "Continue anyway? (y/n): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
}

# Шаг 2: Сборка release APK
echo -e "\n${BLUE}🔨 Step 2: Building release APK...${NC}"
./gradlew assembleRelease -q

if [ $? -ne 0 ]; then
    echo -e "${RED}❌ Release build failed!${NC}"
    exit 1
fi

echo -e "${GREEN}✅ Release APK built successfully${NC}"

# Шаг 3: Запуск UI тестов (опционально)
echo -e "\n${YELLOW}Run UI tests before release? (recommended)${NC}"
read -p "Run tests? (y/n): " -n 1 -r
echo

if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo -e "\n${BLUE}🧪 Step 3: Running UI tests...${NC}"
    ./scripts/run_ui_tests.sh smoke
fi

# Шаг 4: Информация о сборке
echo -e "\n${BLUE}========================================${NC}"
echo -e "${GREEN}✅ Release pipeline completed!${NC}"
echo -e "${BLUE}========================================${NC}"

# Показываем путь к APK
APK_PATH="app/build/outputs/apk/release/app-release.apk"
if [ -f "$APK_PATH" ]; then
    APK_SIZE=$(du -h "$APK_PATH" | cut -f1)
    echo -e "${GREEN}📱 APK Location: $APK_PATH${NC}"
    echo -e "${GREEN}📦 APK Size: $APK_SIZE${NC}"
fi

# Шаг 5: Предложение загрузки
echo -e "\n${YELLOW}Next steps:${NC}"
echo -e "  1. Test the APK on a device"
echo -e "  2. Upload to Play Console"
echo -e "  3. Create a git tag for this release"