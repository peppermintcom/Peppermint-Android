package com.peppermint.app.ui.animations;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;

/**
 * Created by Nuno Luz on 24-09-2015.
 *
 * Given the specified FPS value, draws a sequence of bitmaps on a Canvas.
 */
public class CanvasBoxProgressAnimation extends BaseCanvasAnimation {

    private Context mContext;
    private float mCornerRadius;
    private float mCornerLength;
    private float mTotalLength, mFullSideLength;
    private float mThickness;
    private Paint mProgressPaint, mEmptyProgressPaint, mBackgroundPaint, mMaskPaint;

    public CanvasBoxProgressAnimation(Context context, int framesPerSecond, boolean looping, Paint progressPaint, Paint emptyProgressPaint, Paint backgroundPaint) {
        super(context);
        this.mContext = context;
        setLooping(looping);
        setFramesPerSecond(framesPerSecond);

        mMaskPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mMaskPaint.setStyle(Paint.Style.FILL);
        mMaskPaint.setColor(Color.WHITE);
        mMaskPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));
    }

    public void setup(Rect bounds, int totalLength, float cornerRadius, float cornerLength, float thickness) {
        this.mCornerRadius = cornerRadius;
        this.mCornerLength = cornerLength;
        this.mThickness = thickness;
        setBounds(bounds);
        setTotalLength(totalLength);
        setDuration(getFrameInterval() * totalLength);
    }

    @Override
    public void apply(Canvas canvas, double interpolatedElapsedTime) {
        double progress = interpolatedElapsedTime / getFrameInterval();
        Rect bounds = getBounds();

        Path fullPath = getPath(bounds.centerX(), bounds.centerY(), mCornerRadius, mCornerLength, mFullSideLength, mTotalLength);
        Path progressPath = getPath(bounds.centerX(), bounds.centerY(), mCornerRadius, mCornerLength, mFullSideLength, (float) progress);
        Path maskPath = getPath(bounds.centerX(), bounds.centerY(), mCornerRadius - mThickness, mCornerLength, mFullSideLength - (mThickness * 2f), mTotalLength);

        // progress paths
        if(getLoop() % 2 != 0) {
            canvas.drawPath(fullPath, mProgressPaint);
            canvas.drawPath(progressPath, mEmptyProgressPaint);
        } else {
            canvas.drawPath(fullPath, mEmptyProgressPaint);
            canvas.drawPath(progressPath, mProgressPaint);
        }

        // fill/background
        if (mBackgroundPaint != null) {
            canvas.drawPath(maskPath, mBackgroundPaint);
        } else {
            canvas.drawPath(maskPath, mMaskPaint);
        }
    }

    private static float getAngle(float progress, float cornerLength) {
        return 90f * progress / cornerLength;
    }

    private static Path getPath(float centerX, float centerY, float cornerRadius, float cornerLength, float fullSideLength, float progress) {

        float halfFullSideLength = fullSideLength / 2f;
        float sideLength = fullSideLength - (cornerRadius * 2f);
        float sideHalfLength = sideLength / 2f;

        float xx, yy;
        boolean done = false;

        Path p = new Path();
        p.moveTo(centerX, centerY);
        yy = centerY - halfFullSideLength;
        p.lineTo(centerX, yy);

        // top right half of line
        {
            float tmpProgress = progress < sideHalfLength ? progress : sideHalfLength;
            progress -= tmpProgress;
            xx = centerX + tmpProgress;
            p.lineTo(xx, yy);
        }

        // top right corner
        if(progress > 0) {
            float tmpAngle = progress < cornerLength ? getAngle(progress, cornerLength) : 90f;
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
                float tmpProgress = progress < sideLength ? progress : sideLength;
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
                float tmpAngle = progress < cornerLength ? getAngle(progress, cornerLength) : 90f;
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
                float tmpProgress = progress < sideLength ? progress : sideLength;
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
                float tmpAngle = progress < cornerLength ? getAngle(progress, cornerLength) : 90f;
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
                float tmpProgress = progress < sideLength ? progress : sideLength;
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
                float tmpAngle = progress < cornerLength ? getAngle(progress, cornerLength) : 90f;
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

    public float getCornerRadius() {
        return mCornerRadius;
    }

    public void setCornerRadius(float mCornerRadius) {
        this.mCornerRadius = mCornerRadius;
    }

    public float getCornerLength() {
        return mCornerLength;
    }

    public void setCornerLength(float mCornerLength) {
        this.mCornerLength = mCornerLength;
    }

    public float getTotalLength() {
        return mTotalLength;
    }

    public void setTotalLength(float mTotalLength) {
        this.mTotalLength = mTotalLength;
        this.mFullSideLength = mTotalLength / 4f;
    }

    public float getThickness() {
        return mThickness;
    }

    public void setThickness(float mThickness) {
        this.mThickness = mThickness;
    }

    public Paint getProgressPaint() {
        return mProgressPaint;
    }

    public void setProgressPaint(Paint mProgressPaint) {
        this.mProgressPaint = mProgressPaint;
    }

    public Paint getEmptyProgressPaint() {
        return mEmptyProgressPaint;
    }

    public void setEmptyProgressPaint(Paint mEmptyProgressPaint) {
        this.mEmptyProgressPaint = mEmptyProgressPaint;
    }

    public Paint getBackgroundPaint() {
        return mBackgroundPaint;
    }

    public void setBackgroundPaint(Paint mBackgroundPaint) {
        this.mBackgroundPaint = mBackgroundPaint;
    }
}
