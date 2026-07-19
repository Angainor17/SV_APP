#!/bin/bash

# =====================================================
# Скрипт для подготовки тестовой книги
# =====================================================
# Использование:
#   ./scripts/prepare_test_book.sh [path_to_book]
#
# Если путь не указан, скрипт попробует скачать
# тестовую книгу из открытого источника.
# =====================================================

set -e

BOOK_PATH=${1:-""}
DEST_DIR="bookreader/src/androidTest/assets"
DEST_FILE="$DEST_DIR/test_book.epub"

echo "📚 Подготовка тестовой книги для UI тестов..."

# Создаём директорию если её нет
mkdir -p "$DEST_DIR"

if [ -n "$BOOK_PATH" ] && [ -f "$BOOK_PATH" ]; then
    # Копируем указанную книгу
    echo "📖 Копируем книгу из: $BOOK_PATH"
    cp "$BOOK_PATH" "$DEST_FILE"
    echo "✅ Книга скопирована в: $DEST_FILE"
else
    echo "⚠️  Путь к книге не указан или файл не найден."
    echo ""
    echo "Для подготовки тестовой книги:"
    echo "1. Скачайте любую книгу в формате EPUB/FB2/PDF"
    echo "2. Запустите скрипт с путём к книге:"
    echo "   ./scripts/prepare_test_book.sh /path/to/book.epub"
    echo ""
    echo "Или положите книгу вручную в:"
    echo "   $DEST_FILE"
    echo ""
    echo "Рекомендуемые книги для тестирования:"
    echo "- EPUB: https://www.gutenberg.org/ebooks/"
    echo "- FB2: Любая книга из открытой библиотеки"
    echo "- PDF: Любой PDF документ"
fi

# Проверяем результат
if [ -f "$DEST_FILE" ]; then
    SIZE=$(du -h "$DEST_FILE" | cut -f1)
    echo ""
    echo "✅ Тестовая книга готова!"
    echo "📁 Файл: $DEST_FILE"
    echo "📦 Размер: $SIZE"
else
    echo ""
    echo "❌ Тестовая книга не найдена!"
    echo "Тесты будут использовать заглушку."
fi