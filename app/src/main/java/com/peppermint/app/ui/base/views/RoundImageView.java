package com.peppermint.app.ui.base.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.ImageView;

import com.peppermint.app.R;
import com.peppermint.app.utils.Utils;

// Inspired on https://github.com/lopspower/CircularImageView/blob/master/CircularImageView/src/com/mikhaellopez/circularimageview/CircularImageView.java

/**
 * {@link ImageView} that supports round corners.
 *
 */
public class RoundImageView extends ImageView {

    private static final String DEF_BORDER_COLOR = "#ffffff";
    private static final int DEF_BORDER_WIDTH_DP = 3;
    private static final int DEF_CORNER_RADIUS_DP = 10;

    private int mBorderWidth, mCornerRadius;
    private Paint mPaint, mBorderPaint;
    private int mWidth, mHeight;

    private Drawable mDrawable;
    private Bitmap mBitmap;
    private RectF mBitmapBounds = new RectF(), mBorderBounds = new RectF();

    private boolean mKeepAspectRatio = false;

    public RoundImageView(final Context context) {
        this(context, null);
    }

    public RoundImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RoundImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        // init paints
        mPaint = new Paint();
        mPaint.setAntiAlias(true);

        mBorderPaint = new Paint();
        mBorderPaint.setAntiAlias(true);

        if(attrs != null) {
            TypedArray a = getContext().getTheme().obtainStyledAttributes(
                    attrs,
                    R.styleable.PeppermintView,
                    0, 0);

            try {
                mKeepAspectRatio = a.getBoolean(R.styleable.PeppermintView_keepAspectRatio, mKeepAspectRatio);
                mBorderWidth = a.getDimensionPixelSize(R.styleable.PeppermintView_roundBorderWidth, Utils.dpToPx(getContext(), DEF_BORDER_WIDTH_DP));
                mCornerRadius = a.getDimensionPixelSize(R.styleable.PeppermintView_cornerRadius, Utils.dpToPx(getContext(), DEF_CORNER_RADIUS_DP));
                mBorderPaint.setColor(a.getColor(R.styleable.PeppermintView_roundBorderColor, Color.parseColor(DEF_BORDER_COLOR)));
            } finally {
                a.recycle();
            }
        } else {
            mBorderWidth = Utils.dpToPx(getContext(), DEF_BORDER_WIDTH_DP);
            mCornerRadius = Utils.dpToPx(getContext(), DEF_CORNER_RADIUS_DP);
            mBorderPaint.setColor(Color.parseColor(DEF_BORDER_COLOR));
        }
    }

    @Override
    public void onDraw(Canvas canvas) {
        if (mBitmap != null) {
            int xOffset = 0;
            int yOffset = 0;

            if(isKeepAspectRatio()) {
                xOffset = (int) (((float) mWidth - (float) mBitmap.getWidth()) / 2f);
                yOffset = (int) (((float) mHeight - (float) mBitmap.getHeight()) / 2f);
            }

            mBorderBounds.set(xOffset, yOffset, mBitmap.getWidth() + xOffset, mBitmap.getHeight() + yOffset);
            mBitmapBounds.set(xOffset + mBorderWidth, yOffset + mBorderWidth, mBitmap.getWidth() + xOffset - mBorderWidth, mBitmap.getHeight() + yOffset - mBorderWidth);
            canvas.drawRoundRect(mBorderBounds, mCornerRadius, mCornerRadius, mBorderPaint);
            canvas.drawRoundRect(mBitmapBounds, mCornerRadius - mBorderWidth, mCornerRadius - mBorderWidth, mPaint);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int tmpWidth = MeasureSpec.getSize(widthMeasureSpec);
        int tmpHeight = MeasureSpec.getSize(heightMeasureSpec);

        if(mWidth != tmpWidth || mHeight != tmpHeight || (mDrawable != null && !mDrawable.equals(getDrawable()))) {
            mWidth = tmpWidth;
            mHeight = tmpHeight;
            // update the image bitmap
            onSizeChanged();
        }

        setMeasuredDimension(tmpWidth, tmpHeight);
    }

    protected void onSizeChanged() {
        Drawable drawable = getDrawable();

        if (drawable == null) {
            // no drawable, no bitmap
            mBitmap = null;
            return;
        }

        mDrawable = drawable;

        // draw to bitmap, unless there's already a bitmap available
        Bitmap tmpBitmap;
        boolean recycleBitmap = false;
        if(drawable instanceof BitmapDrawable) {
            tmpBitmap = ((BitmapDrawable) drawable).getBitmap();
        } else {
            tmpBitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                    drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(tmpBitmap);
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);
            recycleBitmap = true;
        }

        // get the view size (image is always drawn in a square area)
        int mCanvasSize = mWidth;
        if(mHeight < mCanvasSize) {
            mCanvasSize = mHeight;
        }

        int bitmapWidth = mCanvasSize;
        int bitmapHeight = mCanvasSize;

        if(isKeepAspectRatio()) {
            float scale = tmpBitmap.getHeight() > tmpBitmap.getWidth()
                    ? (float) mCanvasSize / (float) tmpBitmap.getHeight() : (float) mCanvasSize / (float) tmpBitmap.getWidth();
            bitmapWidth = Math.round((float) tmpBitmap.getWidth() * scale);
            bitmapHeight = Math.round((float) tmpBitmap.getHeight() * scale);
        }

        // scale the image and obtain a bitmap with the exact size required
        mBitmap = Bitmap.createScaledBitmap(tmpBitmap, bitmapWidth, bitmapHeight, true);
        mPaint.setShader(new BitmapShader(mBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));

        if(recycleBitmap) {
            tmpBitmap.recycle();
        }
    }

    /**
     * See {@link #setKeepAspectRatio(boolean)} for more information.
     *
     * @return if the aspect ratio of the original image is to be kept
     */
    public boolean isKeepAspectRatio() {
        return mKeepAspectRatio;
    }

    /**
     * Defines if the aspect ratio of the original image should be kept. <br />
     * Otherwise, it stretches both the width and height of the image to fit the view.
     *
     * @param mKeepAspectRatio
     */
    public void setKeepAspectRatio(boolean mKeepAspectRatio) {
        this.mKeepAspectRatio = mKeepAspectRatio;
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

    protected int getLocalHeight() {
        return mHeight;
    }

    protected int getLocalWidth() {
        return mWidth;
    }

    protected void setLocalWidth(int mWidth) {
        this.mWidth = mWidth;
    }

    protected void setLocalHeight(int mHeight) {
        this.mHeight = mHeight;
    }

    protected Bitmap getBitmap() {
        return mBitmap;
    }
}