package org.ahlab.wavesense.ui;

/**
 * Created by Shanaka on 7/6/2015.
 */

import org.ahlab.wavesenselib.R;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;

public class CircularProgressDrawable extends Drawable implements Animatable {
    private final static String TAG = CircularProgressDrawable.class.getSimpleName();

    static public enum Order {
        ASCENDING,
        DESCENDING
    }

    private static final long FRAME_DURATION = 50; // 50 milli sec interval
    private static final float START_ANGLE = 270f;

    private Paint mPaint;
    private boolean mIsRunning;
    private int mMaxValue;
    private Order mOrder;
    private int mValue;
    private float mSweepAngle;
    private Context mContext;

    private BitmapFactory.Options mOptions;

    public CircularProgressDrawable(int currentValue, int maxValue, Order order, Context
            context) {
        super();
        mValue = currentValue * 20;
        mMaxValue = maxValue * 20;
        mOrder = order;
        mPaint = new Paint();
        mContext = context;

        mOptions = new BitmapFactory.Options();
        mOptions.inScaled = false;
    }

    @Override
    public void draw(Canvas canvas) {
        final Rect bounds = getBounds();
        final RectF oval = new RectF(bounds);
        float x, y, radius;

        // Figure out where to draw things.
        // (Just center everything within our current layout's bounds)
        x = y = radius = bounds.bottom / 2.0f;

        // Overlap with a pie bar
        mPaint.setColor(Color.parseColor("#8dc63f"));
        mPaint.setStrokeWidth(10);
        mPaint.setStyle(Paint.Style.FILL);
        canvas.drawArc(oval, START_ANGLE, mSweepAngle, true, mPaint);

        // Draw our image
        Bitmap bitmap = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.refresh_gray,mOptions);
        /*Rect source = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        Rect bitmapRect = new Rect(((bounds.bottom - bitmap.getWidth())/2), ((bounds.bottom - bitmap.getHeight())/2), ((bounds.bottom + bitmap.getWidth())/2), ((bounds.bottom + bitmap.getHeight())/2));*/
        int width = (int) mContext.getResources().getDimension(R.dimen.rescan_button_width);
        int height = (int) mContext.getResources().getDimension(R.dimen.rescan_button_height);
        Rect source = new Rect(0, 0, width, height);
        //Rect bitmapRect = new Rect(((bounds.bottom - bitmap.getWidth())/2), ((bounds.bottom - bitmap.getHeight())/2), ((bounds.bottom + bitmap.getWidth())/2), ((bounds.bottom + bitmap.getHeight())/2));
        Rect bitmapRect = new Rect(((bounds.bottom - width)/2), ((bounds.bottom - height)/2), ((bounds.bottom + width)/2), ((bounds.bottom + height)/2));
        canvas.drawBitmap(bitmap, source, bitmapRect, new Paint());
    }

    @Override
    public int getOpacity() {
        return PixelFormat.OPAQUE;
    }

    // Empty placeholder
    @Override
    public void setAlpha(int arg0) {
    }

    // Empty placeholder
    @Override
    public void setColorFilter(ColorFilter arg0) {
    }

    @Override
    public boolean isRunning() {
        return mIsRunning;
    }

    @Override
    public void start() {
        //Log.w(TAG, "Start");
        if (!isRunning()) {
            //Log.w(TAG, "Start-Inside");
            mIsRunning = true;
            // Schedule an action every 50 milli seconds to update our drawable
            scheduleSelf(mRefreshRunnable, SystemClock.uptimeMillis() + FRAME_DURATION);

            // Calling this will invoke our callback to have our drawable redrawn
            invalidateSelf();
        }
    }

    @Override
    public void stop() {
        //Log.w(TAG, "Stop");
        if (isRunning()) {
            //Log.w(TAG, "Stop-Inside");
            unscheduleSelf(mRefreshRunnable);
            mValue = (mOrder == Order.ASCENDING ? 0 : mMaxValue);
            mIsRunning = false;
        }
    }

    public int getValue() {
        return mValue;
    }

    private final Runnable mRefreshRunnable = new Runnable() {
        @Override
        public void run() {
            mValue = (mOrder == Order.ASCENDING) ? mValue + 1 : mValue - 1;
            //Log.w(TAG, "mValue: " + mValue);
            if (mValue < 0) mValue = 0;

            // Figure out next angle for drawArc()
            mSweepAngle = (360.0f * mValue) / (float)mMaxValue;

            // Have we reached the end?
            if ((mOrder == Order.ASCENDING && mValue > mMaxValue) ||
                    (mOrder == Order.DESCENDING && mValue <= 0)) {
                // Yes, unschedule our Runnable to stop drawing.
                stop();
            }
            else {
                // Otherwise redraw using new sweep angle
                scheduleSelf(mRefreshRunnable, SystemClock.uptimeMillis() +
                        FRAME_DURATION);

                // Invoke our callback to have our drawable redrawn
                invalidateSelf();
            }
        }
    };
}
