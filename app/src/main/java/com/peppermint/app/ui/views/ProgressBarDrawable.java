package com.peppermint.app.ui.views;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

/**
 * Created by Nuno Luz on 09-02-2016.
 */
public class ProgressBarDrawable extends Drawable {

    private static final String PROGRESS_BACKGROUND_COLOR = "#ffffff";
    private static final String PROGRESS_COLOR = "#cccccc";

    private RectF mRect;
    private Paint mPaintProgress, mPaintBackground;
    private int mSweepAngle = 0;
    private int mStartAngle = 270;

    public ProgressBarDrawable() {
        mPaintProgress = new Paint();
        mPaintProgress.setAntiAlias(true);
        mPaintProgress.setStyle(Paint.Style.STROKE);
        mPaintProgress.setStrokeWidth(10);
        mPaintProgress.setStrokeCap(Paint.Cap.ROUND);
        mPaintProgress.setColor(Color.parseColor(PROGRESS_COLOR));

        mPaintBackground = new Paint();
        mPaintBackground.setAntiAlias(true);
        mPaintBackground.setStyle(Paint.Style.STROKE);
        mPaintBackground.setStrokeWidth(10);
        mPaintBackground.setStrokeCap(Paint.Cap.ROUND);
        mPaintBackground.setColor(Color.parseColor(PROGRESS_BACKGROUND_COLOR));

        mRect = new RectF(0, 0, 100, 100);
    }

    public void update() {
        /*mSweepAngle += mStep;*/
        invalidateSelf();
    }

    @Override
    public void draw(Canvas canvas) {
        mRect.set(0, 0, canvas.getWidth(), canvas.getHeight());

        // draw background line
        canvas.drawArc(mRect, 0, 360, false, mPaintBackground);
        // draw progress line
        canvas.drawArc(mRect, mStartAngle, mSweepAngle, false, mPaintProgress);
    }

    @Override
    public void setAlpha(int alpha) {
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
    }

    @Override
    public int getOpacity() {
        return 1;
    }
}
