package com.example.cp.cpcoingame;

import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;

/**
 * Created by cp on 2/22/2018.
 */

public class MySimpleGestureListener extends SimpleOnGestureListener {

    private MainActivity parent = null;
    private static final int SWIPE_THRESHOLD = 100;
    private static final int SWIPE_VELOCITY_THRESHOLD = 100;

    public MySimpleGestureListener(MainActivity parent) {
        this.parent = parent;
    }

    @Override
    public boolean onDown(MotionEvent e) {
        return true;
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        boolean result = false;
        // find amount of change in both X and Y axis
        float diffX = e2.getX() - e1.getX();
        float diffY = e2.getY() - e1.getY();
        if (Math.abs(diffX) > Math.abs(diffY)) {
            if (Math.abs(diffX) > SWIPE_THRESHOLD &&
                    Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                if (diffX > 0) {
                    parent.onSwipeRight();
                } else {
                    parent.onSwipeLeft();
                }
            }
            result = true;

        } else if (Math.abs(diffY) > SWIPE_THRESHOLD &&
                Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
            if (diffY > 0) {
                parent.onSwipeBottom();
            } else {
                parent.onSwipeTop();
            }
            result = true;
        }
        return result;
    }
}
