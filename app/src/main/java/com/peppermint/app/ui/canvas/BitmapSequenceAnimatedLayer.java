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

import com.peppermint.app.utils.Utils;

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
        int currentFrame = (int) Math.round(interpolatedElapsedTime / getDuration() * (mBitmapSequenceRes.length - 1));
        initShader(currentFrame);

        if(canvas != null) {
            canvas.save();
            canvas.translate(getBounds().left, getBounds().top);
            if (mBorderWidth > 0 && mBorderPaint != null) {
                canvas.drawRoundRect(new RectF(0, 0, getBounds().width(), getBounds().height()), mCornerRadius, mCornerRadius, mBorderPaint);
            }
            canvas.drawRoundRect(new RectF(mBorderWidth, mBorderWidth, getBounds().width() - mBorderWidth, getBounds().height() - mBorderWidth), mCornerRadius - mBorderWidth, mCornerRadius - mBorderWidth, mPaint);
            canvas.restore();
        }
    }

    private synchronized void initShader(int frame) {
        if(getBounds() == null || getBounds().width() <= 0 || getBounds().height() <= 0) {
            return;
        }

        if(mLastFrame >= 0 && mBitmapSequenceRes[frame] == mBitmapSequenceRes[mLastFrame]) {
            return;
        }

        Bitmap bitmap = null;
        if(mBitmapSequence != null && mBitmapSequence.length > frame) {
            bitmap = mBitmapSequence[frame];
        } else if(mBitmapSequenceRes.length > frame) {
            bitmap = Utils.getScaledResizedBitmap(getContext(), mBitmapSequenceRes[frame], getBounds().width() - mBorderWidth, getBounds().height() - mBorderWidth, false);
            if(mLastBitmap != null && !mLastBitmap.isRecycled()) {
                mPaint.setShader(null);
                mLastBitmap.recycle();
                mLastBitmap = bitmap;
            }
        }

        if(bitmap != null) {
            BitmapShader shader = new BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
            mPaint.setShader(shader);
        } else {
            mPaint.setShader(null);
        }

        mLastFrame = frame;
    }

    public int[] getBitmapSequenceResourceIds() {
        return mBitmapSequenceRes;
    }

    public synchronized void setBitmapSequenceResourceIds(boolean decodeAsYouGo, int... mBitmapSequenceRes) {
        if(!decodeAsYouGo) {
            if(mBitmapSequence != null) {
                for(int i=0; i<mBitmapSequence.length; i++) {
                    if(!mBitmapSequence[i].isRecycled()) {
                        mBitmapSequence[i].recycle();
                    }
                }
            }

            mBitmapSequence = new Bitmap[mBitmapSequenceRes.length];
            for (int i = 0; i < mBitmapSequenceRes.length; i++) {
                mBitmapSequence[i] = ((BitmapDrawable) Utils.getDrawable(getContext(), mBitmapSequenceRes[i])).getBitmap();
            }
        }

        this.mBitmapSequenceRes = mBitmapSequenceRes;
    }

    public synchronized void setBitmapSequenceResourceIds(boolean decodeAsYouGo, int[]... mBitmapSequenceRes) {

        List<Integer> sequenceList = new ArrayList<>();
        for(int[] bitmapData : mBitmapSequenceRes) {
            for(int i=0; i<bitmapData[1]; i++) {
                sequenceList.add(bitmapData[0]);
            }
        }

        int [] bitmapSequenceRes = new int[sequenceList.size()];
        for(int i=0; i<bitmapSequenceRes.length; i++) {
            bitmapSequenceRes[i] = sequenceList.get(i);
        }

        setBitmapSequenceResourceIds(decodeAsYouGo, bitmapSequenceRes);
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        if(mBitmapSequence != null) {
            for(int i=0; i<mBitmapSequence.length; i++) {
                if(!mBitmapSequence[i].isRecycled()) {
                    mBitmapSequence[i].recycle();
                }
            }
        }
        mBitmapSequence = null;
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
