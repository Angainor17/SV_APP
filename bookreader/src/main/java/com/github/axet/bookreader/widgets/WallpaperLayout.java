package com.github.axet.bookreader.widgets;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.github.axet.bookreader.app.Plugin;

public class WallpaperLayout extends FrameLayout {
    Plugin.View bg = new Plugin.View();

    public WallpaperLayout(@NonNull Context context) {
        super(context);
    }

    public WallpaperLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public WallpaperLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public WallpaperLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        bg.drawWallpaper(canvas);
        super.dispatchDraw(canvas);
    }
}
