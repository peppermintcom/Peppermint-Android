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

/**
 * Created by Nuno Luz on 24-09-2015.
 *
 * Given the specified FPS value, draws a sequence of bitmaps on a Canvas.
 */
public class BitmapLayer extends LayerBase implements Layer {

    private int mBitmapRes;
    private BitmapDrawable mDrawable;
    private Paint mPaint, mBorderPaint;

    private int mBorderWidth, mCornerRadius;

    public BitmapLayer(Context context, int bitmapRes, Paint paint) {
        super(context);
        this.mBitmapRes = bitmapRes;
        this.mPaint = new Paint(paint);
        initDrawable();
    }

    private void initDrawable() {
        mDrawable = (BitmapDrawable) Utils.getDrawable(getContext(), mBitmapRes);
        initShader();
    }

    private void initShader() {
        if(getBounds() == null || getBounds().width() <= 0 || getBounds().height() <= 0) {
            return;
        }

        if(mDrawable != null) {
            BitmapShader shader = new BitmapShader(Bitmap.createScaledBitmap(mDrawable.getBitmap(), getBounds().width(), getBounds().height(), false), Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
            mPaint.setShader(shader);
        } else {
            mPaint.setShader(null);
        }
    }

    @Override
    protected void onMeasure(Rect bounds) {
        initShader();
    }

    @Override
    public void draw(View view, Canvas canvas) {
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

    public Paint getPaint() {
        return mPaint;
    }

    public Bitmap getBitmap() {
        return mDrawable.getBitmap();
    }

    public int getBitmapResourceId() {
        return mBitmapRes;
    }

    public void setBitmapResourceId(int mBitmapRes) {
        this.mBitmapRes = mBitmapRes;
        initDrawable();
    }

    public BitmapDrawable getBitmapDrawable() {
        return mDrawable;
    }

    public void setBitmapDrawable(BitmapDrawable mDrawable) {
        this.mDrawable = mDrawable;
        initShader();
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