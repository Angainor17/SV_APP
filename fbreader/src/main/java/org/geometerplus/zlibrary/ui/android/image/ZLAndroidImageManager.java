package org.geometerplus.zlibrary.ui.android.image;

import org.geometerplus.zlibrary.core.image.ZLImage;
import org.geometerplus.zlibrary.core.image.ZLImageManager;
import org.geometerplus.zlibrary.core.image.ZLImageProxy;
import org.geometerplus.zlibrary.core.image.ZLStreamImage;

public final class ZLAndroidImageManager extends ZLImageManager {
    private ZLAndroidImageLoader myLoader;

    @Override
    public ZLAndroidImageData getImageData(ZLImage image) {
        if (image instanceof ZLImageProxy) {
            return getImageData(((ZLImageProxy) image).getRealImage());
        } else if (image instanceof ZLStreamImage) {
            return new InputStreamImageData((ZLStreamImage) image);
        } else if (image instanceof ZLBitmapImage) {
            return BitmapImageData.get((ZLBitmapImage) image);
        } else {
            // unknown image type or null
            return null;
        }
    }

    public void startImageLoading(ZLImageProxy.Synchronizer syncronizer, ZLImageProxy image, Runnable postLoadingRunnable) {
        if (myLoader == null) {
            myLoader = new ZLAndroidImageLoader();
        }
        myLoader.startImageLoading(syncronizer, image, postLoadingRunnable);
    }
}
