package com.github.axet.bookreader.widgets;

import android.animation.TimeAnimator;
import android.animation.ValueAnimator;
import android.os.Handler;

public class TimeAnimatorCompat {
    Handler handler = new Handler();
    TimeListener listener;
    ValueAnimator v;

    public TimeAnimatorCompat() {
        v = new TimeAnimator();
    }

    Runnable run = new Runnable() {
        @Override
        public void run() {
            if (listener != null)
                listener.onTimeUpdate(TimeAnimatorCompat.this, 0, 0);
            handler.postDelayed(run, 1000 / 24); // 24 FPS
        }
    };

    public void start() {
        v.start();
    }

    public void cancel() {
        v.cancel();
    }

    public void setTimeListener(TimeListener l) {
        ((TimeAnimator) v).setTimeListener((animation, totalTime, deltaTime) -> {
            if (listener != null)
                listener.onTimeUpdate(TimeAnimatorCompat.this, totalTime, deltaTime);
        });
        listener = l;
    }

    interface TimeListener {
        void onTimeUpdate(TimeAnimatorCompat animation, long totalTime, long deltaTime);
    }


}
