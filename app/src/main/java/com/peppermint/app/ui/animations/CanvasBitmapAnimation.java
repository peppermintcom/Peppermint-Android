package com.peppermint.app.ui.animations;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;

import com.peppermint.app.utils.Utils;

/**
 * Created by Nuno Luz on 24-09-2015.
 *
 * Given the specified FPS value, draws a sequence of bitmaps on a Canvas.
 */
public class CanvasBitmapAnimation extends BaseCanvasAnimation {

    private Context mContext;
    private int[] mBitmapSequenceRes;
    private BitmapDrawable[] mDrawables;
    private Paint mPaint;

    public CanvasBitmapAnimation(Context context, int framesPerSecond, Paint paint, int... bitmapSequenceRes) {
        super(context);
        this.mBitmapSequenceRes = bitmapSequenceRes;
        this.mContext = context;
        this.mPaint = paint;
        setFramesPerSecond(framesPerSecond);
        init();
    }

    private void init() {
        mDrawables = new BitmapDrawable[mBitmapSequenceRes.length];
        for(int i=0; i<mBitmapSequenceRes.length; i++) {
            mDrawables[i] = (BitmapDrawable) Utils.getDrawable(mContext, mBitmapSequenceRes[i]);
        }

        setDuration((long) (getFrameInterval() * (float) mDrawables.length));
    }

    @Override
    public void apply(Canvas canvas, double interpolatedElapsedTime) {
        int currentFrame = (int) (interpolatedElapsedTime / getFrameInterval());

        if (isReversed()) {
            canvas.drawBitmap(mDrawables[mDrawables.length - currentFrame - 1].getBitmap(), null, getBounds(), mPaint);
        } else {
            canvas.drawBitmap(mDrawables[currentFrame].getBitmap(), null, getBounds(), mPaint);
        }
    }
}
