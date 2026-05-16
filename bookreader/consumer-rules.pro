-keep class com.github.axet.bookreader.** { *; }

# Сохранить имена методов (если они вызываются через рефлексию или извне)
-keepclassmembers class com.github.axet.bookreader.** {
    *;
}

-dontwarn org.geometerplus.**
-dontwarn org.apache.**
-dontwarn yuku.ambilwarna.**
-dontwarn org.spongycastle.**
-dontwarn com.github.axet.**
