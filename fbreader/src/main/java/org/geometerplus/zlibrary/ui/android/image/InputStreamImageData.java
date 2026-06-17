package org.geometerplus.zlibrary.ui.android.image;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;

import org.geometerplus.zlibrary.core.image.ZLStreamImage;

import java.io.IOException;
import java.io.InputStream;

final class InputStreamImageData extends ZLAndroidImageData {
    private final ZLStreamImage myImage;

    InputStreamImageData(ZLStreamImage image) {
        myImage = image;
    }

    protected Bitmap decodeWithOptions(BitmapFactory.Options options) {
        final InputStream stream = myImage.inputStream();
        if (stream == null) {
            return null;
        }

        final Bitmap bmp = BitmapFactory.decodeStream(stream, new Rect(), options);
        try {
            stream.close();
        } catch (IOException e) {
        }
        return bmp;
    }
}
