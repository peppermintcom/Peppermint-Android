package com.peppermint.app.ui.canvas;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.view.View;

import com.peppermint.app.utils.ResourceUtils;

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
        mDrawable = (BitmapDrawable) ResourceUtils.getDrawable(mContext, mBitmapRes);
        mPrevBounds = null;
        initShader();
    }

    private synchronized void initShader() {
        if(mDrawable == null) {
            // cleanup if drawable is set to null
            if(mShaderBitmap != null && !mShaderBitmap.isRecycled()) {
                mPaint.setShader(null);
                mShaderBitmap.recycle();
            }
            return;
        }

        // do nothing if bounds are invalid or didn't change
        if(mBounds == null || mBounds.width() <= 0 || mBounds.height() <= 0) {
            return;
        }

        if(mPrevBounds != null && mPrevBounds.equals(mBounds)) {
            return;
        }

        mPrevBounds = mBounds;

        if(mShaderBitmap == null || mShaderBitmap.isRecycled() ||
                mShaderBitmap.getHeight() != mBounds.height() || mShaderBitmap.getWidth() != mBounds.width()) {
            if(mShaderBitmap != null && !mShaderBitmap.isRecycled()) {
                mPaint.setShader(null);
                mShaderBitmap.recycle();
            }

            mShaderBitmap = Bitmap.createBitmap(mBounds.width(), mBounds.height(), Bitmap.Config.ARGB_8888);
            mPaint.setShader(new BitmapShader(mShaderBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));
        }

        // draw to bitmap
        final Canvas canvas = new Canvas(mShaderBitmap);
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        mDrawable.setBounds(0, 0, mBounds.width(), mBounds.height());
        mDrawable.draw(canvas);
        canvas.setBitmap(null);
    }

    @Override
    protected void onMeasure(Rect bounds) {
        initShader();
    }

    @Override
    public synchronized void draw(View view, Canvas canvas) {
        if(canvas != null) {
            canvas.save();
            canvas.translate(mBounds.left, mBounds.top);
            if (mBorderWidth > 0 && mBorderPaint != null) {
                canvas.drawRoundRect(new RectF(0, 0, mBounds.width(), mBounds.height()), mCornerRadius, mCornerRadius, mBorderPaint);
            }
            canvas.drawRoundRect(new RectF(mBorderWidth, mBorderWidth, mBounds.width() - mBorderWidth, mBounds.height() - mBorderWidth), mCornerRadius - mBorderWidth, mCornerRadius - mBorderWidth, mPaint);
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
