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
 * A {@link Layer} filled with a static bitmap image.<br />
 * It supports borders and round corners.
 */
public class BitmapLayer extends LayerBase implements Layer {

    private int mBitmapRes;
    private BitmapDrawable mDrawable;
    private Bitmap mShaderBitmap;
    private Paint mPaint, mBorderPaint;

    private int mBorderWidth, mCornerRadius;
    private Rect mPrevBounds;

    public BitmapLayer(Context context, int bitmapRes, Paint paint) {
        super(context);
        this.mBitmapRes = bitmapRes;
        this.mPaint = new Paint(paint);
        initDrawable();
    }

    private synchronized void initDrawable() {
        mDrawable = (BitmapDrawable) Utils.getDrawable(getContext(), mBitmapRes);
        mPrevBounds = null;
        initShader();
    }

    private synchronized void initShader() {
        if(getBounds() == null || getBounds().width() <= 0 || getBounds().height() <= 0) {
            return;
        }

        if(mPrevBounds != null && mPrevBounds.equals(getBounds())) {
            return;
        }

        mPrevBounds = getBounds();

        if(mDrawable != null) {
            if(mShaderBitmap != null) {
                mShaderBitmap.recycle();
            }
            mShaderBitmap = Bitmap.createScaledBitmap(mDrawable.getBitmap(), getBounds().width(), getBounds().height(), false);
            mPaint.setShader(new BitmapShader(mShaderBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));
        } else {
            mPaint.setShader(null);
        }
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        if(mShaderBitmap != null) {
            mShaderBitmap.recycle();
            mShaderBitmap = null;
        }
    }

    @Override
    protected void onMeasure(Rect bounds) {
        initShader();
    }

    @Override
    public synchronized void draw(View view, Canvas canvas) {
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
        mPrevBounds = null;
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
