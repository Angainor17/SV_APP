APP_ABI := armeabi-v7a arm64-v8a x86 x86_64
APP_STL := c++_static
# 16 KB page size: явно фиксируем выравнивание LOAD-сегментов на 16 KB (align 2**14),
# чтобы не зависеть от дефолта конкретной версии NDK (см. docs/technical-debt.md).
APP_LDFLAGS := -Wl,-z,max-page-size=16384
