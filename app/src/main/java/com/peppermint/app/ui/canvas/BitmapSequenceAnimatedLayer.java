package com.peppermint.app.ui.canvas;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
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
 * Given the specified FPS value, draws a sequence of bitmaps on a Canvas.
 */
public class BitmapSequenceAnimatedLayer extends AnimatedLayerBase implements AnimatedLayer {

    private int[] mBitmapSequenceRes;
    private BitmapDrawable[] mBitmapSequence;

    private Paint mPaint, mBorderPaint;
    private int mBorderWidth, mCornerRadius;

    public BitmapSequenceAnimatedLayer(Context context, long duration, Paint paint) {
        super(context);
        this.mPaint = new Paint(paint);
        setDuration(duration);
    }

    public BitmapSequenceAnimatedLayer(Context context, long duration, Paint paint, int... bitmapSequenceRes) {
        super(context);
        this.mPaint = new Paint(paint);
        setDuration(duration);
        setBitmapSequenceResourceIds(bitmapSequenceRes);
    }

    public BitmapSequenceAnimatedLayer(Context context, long duration, Paint paint, int[]... bitmapSequenceRes) {
        super(context);
        this.mPaint = new Paint(paint);
        setDuration(duration);
        setBitmapSequenceResourceIds(bitmapSequenceRes);
    }

    @Override
    protected void onMeasure(Rect bounds) {
        int currentFrame = (int) Math.round(getInterpolatedElapsedTime() / getDuration() * (mBitmapSequenceRes.length - 1));
        initShader(currentFrame);
    }

    @Override
    public void onDraw(View view, Canvas canvas, double interpolatedElapsedTime) {
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

    private void initShader(int frame) {
        if(getBounds() == null || getBounds().width() <= 0 || getBounds().height() <= 0) {
            return;
        }

        if(mBitmapSequence != null && mBitmapSequence.length > frame) {
            BitmapShader shader = new BitmapShader(Bitmap.createScaledBitmap(mBitmapSequence[frame].getBitmap(), getBounds().width(), getBounds().height(), false), Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
            mPaint.setShader(shader);
        } else {
            mPaint.setShader(null);
        }
    }

    public int[] getBitmapSequenceResourceIds() {
        return mBitmapSequenceRes;
    }

    public void setBitmapSequenceResourceIds(int... mBitmapSequenceRes) {
        mBitmapSequence = new BitmapDrawable[mBitmapSequenceRes.length];
        for(int i=0; i<mBitmapSequenceRes.length; i++) {
            mBitmapSequence[i] = (BitmapDrawable) Utils.getDrawable(getContext(), mBitmapSequenceRes[i]);
        }

        this.mBitmapSequenceRes = mBitmapSequenceRes;
    }

    public void setBitmapSequenceResourceIds(int[]... mBitmapSequenceRes) {

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

        setBitmapSequenceResourceIds(bitmapSequenceRes);
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
