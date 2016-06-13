package com.peppermint.app.ui.canvas.loading;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.View;

import com.peppermint.app.ui.canvas.AnimatedLayerBase;

/**
 * Created by Nuno Luz on 24-09-2015.
 *
 * Given the specified FPS value, draws a sequence of bitmaps on a Canvas.
 */
public class LoadingBoxAnimatedLayer extends AnimatedLayerBase {

    public static final int PROGRESS_BOX = 1;
    public static final int PROGRESS_WAVE = 2;

    private float mCornerRadius;
    private float mCornerLength;
    private float mTotalLength, mFullSideLength;
    private float mProgressWidth;
    private Paint mProgressPaint, mEmptyProgressPaint, mBackgroundPaint, mBackgroundPressedPaint, mMaskPaint;
    private int mProgressType = PROGRESS_BOX;
    private boolean mFirstPartOnly = false;

    public LoadingBoxAnimatedLayer(Context context, long duration, boolean looping, float cornerRadius, float progressWidth, Paint progressPaint, Paint emptyProgressPaint, Paint backgroundPaint, Paint backgroundPressedPaint) {
        super(context);
        setLooping(looping);
        setDuration(duration);

        setCornerRadius(cornerRadius);
        this.mProgressWidth = progressWidth;
        this.mProgressPaint = progressPaint;
        this.mEmptyProgressPaint = emptyProgressPaint;
        this.mBackgroundPaint = backgroundPaint;
        this.mBackgroundPressedPaint = backgroundPressedPaint;

        mMaskPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mMaskPaint.setStyle(Paint.Style.FILL);
        mMaskPaint.setColor(Color.WHITE);
        mMaskPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));
    }

    @Override
    protected synchronized void onMeasure(Rect bounds) {
        mFullSideLength = bounds.width() > bounds.height() ? bounds.height() : bounds.width();
        mTotalLength = ((mFullSideLength - (mCornerRadius * 2f)) * 4f) + (mCornerLength * 4f);
    }

    @Override
    public synchronized void onDraw(View view, Canvas canvas, double interpolatedElapsedTime) {
        final double halfDuration = getDuration() / 2f;

        double progress;
        final boolean goTwoHalves = mProgressType == PROGRESS_BOX && !mFirstPartOnly;
        if(!goTwoHalves) {
            progress = interpolatedElapsedTime / getDuration() * mTotalLength;
        } else {
            progress = (interpolatedElapsedTime < halfDuration ? (interpolatedElapsedTime / halfDuration) : (interpolatedElapsedTime - halfDuration) / halfDuration) * mTotalLength;
        }

        final Path fullPath = getRoundRectPath(mBounds.centerX(), mBounds.centerY(), mCornerRadius, mCornerLength, mFullSideLength, mTotalLength);
        final Path progressPath = mProgressType == PROGRESS_BOX ?
                getRoundRectPath(mBounds.centerX(), mBounds.centerY(), mCornerRadius, mCornerLength, mFullSideLength, (float) progress) :
                getSinWavePath(mBounds.centerX(), mBounds.centerY(), mCornerRadius, mCornerLength, mFullSideLength, mProgressWidth, (float) (interpolatedElapsedTime / getDuration() * 20f * Math.PI));

        // progress paths
        if(goTwoHalves && interpolatedElapsedTime >= halfDuration) {
            if(mProgressPaint != null) {
                canvas.drawPath(fullPath, mProgressPaint);
            }
            if(mEmptyProgressPaint != null) {
                canvas.drawPath(progressPath, mEmptyProgressPaint);
            }
        } else {
            if(mEmptyProgressPaint != null) {
                canvas.drawPath(fullPath, mEmptyProgressPaint);
            }
            if(mProgressPaint != null) {
                canvas.drawPath(progressPath, mProgressPaint);
            }
        }

        if(mProgressType == PROGRESS_BOX) {
            final Path maskPath = getRoundRectPath(mBounds.centerX(), mBounds.centerY(), mCornerRadius - mProgressWidth, mCornerLength, mFullSideLength - (mProgressWidth * 2f), mTotalLength);

            if (mBackgroundPressedPaint != null && view.isPressed()) {
                canvas.drawPath(maskPath, mBackgroundPressedPaint);
            } else {
                // fill/background
                if (mBackgroundPaint != null) {
                    canvas.drawPath(maskPath, mBackgroundPaint);
                } else {
                    canvas.drawPath(maskPath, mMaskPaint);
                }
            }
        }
    }

    private static float getAngle(float progress, float cornerLength) {
        return 90f * progress / cornerLength;
    }

    private static Path getRoundRectPath(float centerX, float centerY, float cornerRadius, float cornerLength, float fullSideLength, float progress) {

        final float halfFullSideLength = fullSideLength / 2f;
        final float sideLength = fullSideLength - (cornerRadius * 2f);
        final float sideHalfLength = sideLength / 2f;

        float xx, yy;
        boolean done = false;

        final Path p = new Path();
        p.moveTo(centerX, centerY);
        yy = centerY - halfFullSideLength;
        p.lineTo(centerX, yy);

        // top right half of line
        {
            final float tmpProgress = progress < sideHalfLength ? progress : sideHalfLength;
            progress -= tmpProgress;
            xx = centerX + tmpProgress;
            p.lineTo(xx, yy);
        }

        // top right corner
        if(progress > 0) {
            final float tmpAngle = progress < cornerLength ? getAngle(progress, cornerLength) : 90f;
            progress -= cornerLength;
            p.arcTo(new RectF(centerX + sideHalfLength - cornerRadius,      // left
                    yy,                                                     // top
                    centerX + sideHalfLength + cornerRadius,                // right
                    yy + (cornerRadius * 2f)), -90, tmpAngle);              // bottom
        } else {
            p.lineTo(xx, yy + cornerRadius);
            done = true;
        }

        // right line
        if(!done) {
            if (progress > 0) {
                final float tmpProgress = progress < sideLength ? progress : sideLength;
                progress -= tmpProgress;
                xx = centerX + halfFullSideLength;
                yy += tmpProgress + cornerRadius;
                p.lineTo(xx, yy);
            } else {
                p.lineTo(centerX + sideHalfLength, yy + cornerRadius);
                done = true;
            }
        }

        // bottom right corner
        if(!done) {
            if (progress > 0) {
                final float tmpAngle = progress < cornerLength ? getAngle(progress, cornerLength) : 90f;
                progress -= cornerLength;
                p.arcTo(new RectF(centerX + sideHalfLength - cornerRadius,  // left
                        centerY - halfFullSideLength + sideLength,          // top
                        centerX + halfFullSideLength,                       // right
                        centerY + halfFullSideLength), 0, tmpAngle);        // bottom
            } else {
                p.lineTo(xx - cornerRadius, yy);
                done = true;
            }
        }

        // bottom line
        if(!done) {
            if (progress > 0) {
                final float tmpProgress = progress < sideLength ? progress : sideLength;
                progress -= tmpProgress;
                xx = centerX - sideHalfLength + (sideLength - tmpProgress);
                yy = centerY + halfFullSideLength;
                p.lineTo(xx, yy);
            } else {
                p.lineTo(centerX + sideHalfLength, centerY + sideHalfLength);
                done = true;
            }
        }

        // bottom left corner
        if(!done) {
            if (progress > 0) {
                final float tmpAngle = progress < cornerLength ? getAngle(progress, cornerLength) : 90f;
                progress -= cornerLength;
                p.arcTo(new RectF(centerX - halfFullSideLength,             // left
                        centerY + sideHalfLength - cornerRadius,            // top
                        centerX - sideHalfLength + cornerRadius,            // right
                        centerY + halfFullSideLength), 90, tmpAngle);       // bottom
            } else {
                p.lineTo(xx, yy - cornerRadius);
                done = true;
            }
        }

        // left line
        if(!done) {
            if (progress > 0) {
                final float tmpProgress = progress < sideLength ? progress : sideLength;
                progress -= tmpProgress;
                xx = centerX - halfFullSideLength;
                yy = centerY + sideHalfLength - tmpProgress;
                p.lineTo(xx, yy);
            } else {
                p.lineTo(centerX - sideHalfLength, centerY + sideHalfLength);
                done = true;
            }
        }

        // top left corner
        if(!done) {
            if (progress > 0) {
                final float tmpAngle = progress < cornerLength ? getAngle(progress, cornerLength) : 90f;
                progress -= cornerLength;
                p.arcTo(new RectF(centerX - halfFullSideLength,                     //left
                        centerY - halfFullSideLength,                               // top
                        centerX - sideHalfLength + cornerRadius,                    // right
                        centerY - sideHalfLength + cornerRadius), 180, tmpAngle);   // bottom
            } else {
                p.lineTo(xx + cornerRadius, yy);
                done = true;
            }
        }

        // top left half of line
        if(!done) {
            if (progress > 0) {
                xx = centerX - sideHalfLength + progress;
                yy = centerY - halfFullSideLength;
                p.lineTo(xx, yy);
                p.lineTo(xx, yy + cornerRadius);
            } else {
                p.lineTo(centerX - sideHalfLength, centerY - sideHalfLength);
            }
        }

        p.close();

        return p;
    }

    private static Path getSinWavePath(float centerX, float centerY, float cornerRadius, float cornerLength, float fullSideLength, float amplitude, float progress) {

        final float halfFullSideLength = fullSideLength / 2f;
        final float sideLength = fullSideLength - (cornerRadius * 2f);
        final float sideHalfLength = sideLength / 2f;

        float xx, yy;

        final Path p = new Path();
        p.moveTo(centerX + halfFullSideLength, centerY);
        yy = centerY + sideHalfLength;
        p.lineTo(centerX + halfFullSideLength, yy);

        // bottom right corner
        p.arcTo(new RectF(centerX + sideHalfLength - cornerRadius,  // left
                centerY - halfFullSideLength + sideLength,          // top
                centerX + halfFullSideLength,                       // right
                centerY + halfFullSideLength), 0, 90f);        // bottom

        // bottom line
        xx = centerX - sideHalfLength;
        yy = centerY + halfFullSideLength;
        p.lineTo(xx, yy);

        // bottom left corner
        p.arcTo(new RectF(centerX - halfFullSideLength,             // left
                centerY + sideHalfLength - cornerRadius,            // top
                centerX - sideHalfLength + cornerRadius,            // right
                centerY + halfFullSideLength), 90, 90f);       // bottom

        // left line
        xx = centerX - halfFullSideLength;
        yy = centerY;
        p.lineTo(xx, yy);

        // sin
        final float divider = fullSideLength / 10f;
        final float unitX = fullSideLength / divider;
        final float unitAngle = (float) ((2f*Math.PI) / divider);
        float angle = unitAngle;
        for(float i=0; i<divider; i++) {
            yy = (float) (centerY - (Math.sin(progress + angle) * amplitude));
            p.lineTo(xx + (i * unitX), yy);
            angle += unitAngle;
        }

        p.lineTo(centerX + halfFullSideLength, yy);
        p.close();

        return p;
    }


    public float getCornerRadius() {
        return mCornerRadius;
    }

    public synchronized void setCornerRadius(float mCornerRadius) {
        this.mCornerRadius = mCornerRadius;
        this.mCornerLength = (float) (mCornerRadius * Math.PI / 2f);    // 1/4 of perimeter
    }

    public float getCornerLength() {
        return mCornerLength;
    }

    public float getProgressWidth() {
        return mProgressWidth;
    }

    public synchronized void setProgressWidth(float mProgressWidth) {
        this.mProgressWidth = mProgressWidth;
    }

    public Paint getProgressPaint() {
        return mProgressPaint;
    }

    public synchronized void setProgressPaint(Paint mProgressPaint) {
        this.mProgressPaint = mProgressPaint;
    }

    public Paint getEmptyProgressPaint() {
        return mEmptyProgressPaint;
    }

    public synchronized void setEmptyProgressPaint(Paint mEmptyProgressPaint) {
        this.mEmptyProgressPaint = mEmptyProgressPaint;
    }

    public Paint getBackgroundPaint() {
        return mBackgroundPaint;
    }

    public synchronized void setBackgroundPaint(Paint mBackgroundPaint) {
        this.mBackgroundPaint = mBackgroundPaint;
    }

    public int getProgressType() {
        return mProgressType;
    }

    public synchronized void setProgressType(int mProgressType) {
        this.mProgressType = mProgressType;
    }

    public Paint getBackgroundPressedPaint() {
        return mBackgroundPressedPaint;
    }

    public synchronized void setBackgroundPressedPaint(Paint mBackgroundPressedPaint) {
        this.mBackgroundPressedPaint = mBackgroundPressedPaint;
    }

    public boolean isFirstPartOnly() {
        return mFirstPartOnly;
    }

    public synchronized void setFirstPartOnly(boolean mFirstPartOnly) {
        this.mFirstPartOnly = mFirstPartOnly;
    }
}
