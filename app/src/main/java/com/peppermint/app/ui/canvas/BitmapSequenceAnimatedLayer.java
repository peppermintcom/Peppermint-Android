package com.peppermint.app.ui.canvas;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.view.View;

import com.peppermint.app.utils.ResourceUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Nuno Luz on 24-09-2015.
 *
 * An {@link AnimatedLayer} filled with an animated sequence of bitmap images.<br />
 * It supports borders and round corners.
 */
public class BitmapSequenceAnimatedLayer extends AnimatedLayerBase implements AnimatedLayer {

    private int[] mBitmapSequenceRes;
    private Bitmap[] mBitmapSequence;

    private Paint mPaint, mBorderPaint;
    private int mBorderWidth, mCornerRadius;
    private int mLastFrame = -1;
    private Bitmap mLastBitmap;

    public BitmapSequenceAnimatedLayer(Context context, long duration, Paint paint) {
        super(context);
        this.mPaint = new Paint(paint);
        setDuration(duration);
    }

    public BitmapSequenceAnimatedLayer(Context context, long duration, Paint paint, int... bitmapSequenceRes) {
        super(context);
        this.mPaint = new Paint(paint);
        setDuration(duration);
        setBitmapSequenceResourceIds(false, bitmapSequenceRes);
    }

    public BitmapSequenceAnimatedLayer(Context context, long duration, Paint paint, int[]... bitmapSequenceRes) {
        super(context);
        this.mPaint = new Paint(paint);
        setDuration(duration);
        setBitmapSequenceResourceIds(false, bitmapSequenceRes);
    }

    @Override
    public synchronized void onDraw(View view, Canvas canvas, double interpolatedElapsedTime) {
        final int currentFrame = (int) Math.round(interpolatedElapsedTime / getDuration() * (mBitmapSequenceRes.length - 1));
        initShader(currentFrame);

        if(canvas != null) {
            canvas.save();
            canvas.translate(mBounds.left, mBounds.top);
            if (mBorderWidth > 0 && mBorderPaint != null) {
                canvas.drawRoundRect(new RectF(
                        0, 0,
                        mBounds.width(), mBounds.height()
                ), mCornerRadius, mCornerRadius, mBorderPaint);
            }
            canvas.drawRoundRect(
                    new RectF(
                            mBorderWidth, mBorderWidth,
                            mBounds.width() - mBorderWidth,
                            mBounds.height() - mBorderWidth
                    ), mCornerRadius - mBorderWidth, mCornerRadius - mBorderWidth, mPaint);
            canvas.restore();
        }
    }

    private synchronized void initShader(int frame) {
        if(mBounds == null || mBounds.width() <= 0 || mBounds.height() <= 0) {
            return;
        }

        if(mLastFrame >= 0 && mBitmapSequenceRes[frame] == mBitmapSequenceRes[mLastFrame]) {
            return;
        }

        Bitmap bitmap = null;
        if(mBitmapSequence != null && mBitmapSequence.length > frame) {
            bitmap = mBitmapSequence[frame];
        } else if(mBitmapSequenceRes.length > frame) {
            bitmap = ResourceUtils.getScaledResizedBitmap(mContext, mBitmapSequenceRes[frame],
                    mBounds.width() - mBorderWidth, mBounds.height() - mBorderWidth, false);
            if(mLastBitmap != null && !mLastBitmap.isRecycled()) {
                mPaint.setShader(null);
                mLastBitmap.recycle();
                mLastBitmap = bitmap;
            }
        }

        if(bitmap != null) {
            mPaint.setShader(new BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));
        } else {
            mPaint.setShader(null);
        }

        mLastFrame = frame;
    }

    public synchronized void setBitmapSequenceResourceIds(boolean decodeAsYouGo, int... mBitmapSequenceRes) {
        if(!decodeAsYouGo) {
            if(mBitmapSequence != null) {
                for(Bitmap bitmap : mBitmapSequence) {
                    if(!bitmap.isRecycled()) {
                        bitmap.recycle();
                    }
                }
            }

            mBitmapSequence = new Bitmap[mBitmapSequenceRes.length];
            for (int i = 0; i < mBitmapSequenceRes.length; i++) {
                mBitmapSequence[i] = ((BitmapDrawable) ResourceUtils.getDrawable(mContext, mBitmapSequenceRes[i])).getBitmap();
            }
        }

        this.mBitmapSequenceRes = mBitmapSequenceRes;
    }

    public synchronized void setBitmapSequenceResourceIds(boolean decodeAsYouGo, int[]... mBitmapSequenceRes) {

        final List<Integer> sequenceList = new ArrayList<>();
        for(int[] bitmapData : mBitmapSequenceRes) {
            for(int i=0; i<bitmapData[1]; i++) {
                sequenceList.add(bitmapData[0]);
            }
        }

        final int [] bitmapSequenceRes = new int[sequenceList.size()];
        for(int i=0; i<bitmapSequenceRes.length; i++) {
            bitmapSequenceRes[i] = sequenceList.get(i);
        }

        setBitmapSequenceResourceIds(decodeAsYouGo, bitmapSequenceRes);
    }

    public Paint getPaint() {
        return mPaint;
    }

    public Paint getBorderPaint() {
        return mBorderPaint;
    }

    public void setBorderPaint(Paint mBorderPaint) {
        this.mBorderPaint = mBorderPaint;
    }

    public int getBorderWidth() {
        return mBorderWidth;
    }

    public void setBorderWidth(int mBorderWidth) {
        this.mBorderWidth = mBorderWidth;
    }

    public int getCornerRadius() {
        return mCornerRadius;
    }

    public void setCornerRadius(int mCornerRadius) {
        this.mCornerRadius = mCornerRadius;
    }
}
