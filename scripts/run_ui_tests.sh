#!/bin/bash

# =====================================================
# Скрипт запуска UI тестов перед релизом
# =====================================================
# Использование:
#   ./scripts/run_ui_tests.sh              - запустить все тесты
#   ./scripts/run_ui_tests.sh smoke        - запустить только smoke-тесты
#   ./scripts/run_ui_tests.sh release      - запустить только release-тесты
# =====================================================

set -e

# Цвета для вывода
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}   SV APP - UI Tests Runner${NC}"
echo -e "${BLUE}========================================${NC}"

# Определяем тип тестов
TEST_TYPE=${1:-"all"}

case $TEST_TYPE in
    "smoke")
        echo -e "${YELLOW}🧪 Running SMOKE tests...${NC}"
        ANNOTATION="su.sv.app.testing.SmokeTest"
        ;;
    "release")
        echo -e "${YELLOW}🧪 Running RELEASE tests...${NC}"
        ANNOTATION="su.sv.app.testing.ReleaseTest"
        ;;
    "navigation")
        echo -e "${YELLOW}🧪 Running NAVIGATION tests...${NC}"
        ANNOTATION="su.sv.app.testing.NavigationTest"
        ;;
    *)
        echo -e "${YELLOW}🧪 Running ALL UI tests...${NC}"
        ANNOTATION=""
        ;;
esac

# Проверяем подключенное устройство/эмулятор
echo -e "\n${BLUE}📱 Checking connected devices...${NC}"
DEVICES=$(adb devices | grep -v "List" | grep "device$" | wc -l)

if [ "$DEVICES" -eq 0 ]; then
    echo -e "${RED}❌ No devices connected!${NC}"
    echo -e "${YELLOW}Please connect a device or start an emulator.${NC}"
    exit 1
fi

echo -e "${GREEN}✅ Found $DEVICES device(s)${NC}"

# Сборка тестового APK
echo -e "\n${BLUE}🔨 Building test APK...${NC}"
./gradlew assembleAndroidTest -q

if [ $? -ne 0 ]; then
    echo -e "${RED}❌ Failed to build test APK${NC}"
    exit 1
fi

echo -e "${GREEN}✅ Test APK built successfully${NC}"

# Запуск тестов
echo -e "\n${BLUE}🚀 Running tests...${NC}"

if [ -n "$ANNOTATION" ]; then
    ./gradlew connectedAndroidTest \n        -Pandroid.testInstrumentationRunnerArguments.annotation="$ANNOTATION" \n        --info
else
    ./gradlew connectedAndroidTest --info
fi

# Проверка результата
TEST_RESULT=$?

echo -e "\n${BLUE}========================================${NC}"

if [ $TEST_RESULT -eq 0 ]; then
    echo -e "${GREEN}✅ All tests passed!${NC}"
    echo -e "${GREEN}Ready for release build.${NC}"
else
    echo -e "${RED}❌ Some tests failed!${NC}"
    echo -e "${YELLOW}Please check the test report:${NC}"
    echo -e "${YELLOW}app/build/reports/androidTests/connected/${NC}"
    exit 1
fi

echo -e "${BLUE}========================================${NC}"