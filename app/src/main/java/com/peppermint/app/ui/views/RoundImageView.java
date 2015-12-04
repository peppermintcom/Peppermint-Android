package com.peppermint.app.ui.views;

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
 */
public class RoundImageView extends ImageView {

    private static final String DEF_BORDER_COLOR = "#ffffff";
    private static final int DEF_BORDER_WIDTH_DP = 3;
    private static final int DEF_CORNER_RADIUS_DP = 10;

    private int mBorderWidth, mCornerRadius;
    private Bitmap mImage;
    private Paint mPaint, mBorderPaint;
    private int mWidth, mHeight;

    private boolean mKeepAspectRatio = false;

    public RoundImageView(final Context context) {
        this(context, null);
    }

    public RoundImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RoundImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        // init paint
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
                mBorderWidth = a.getDimensionPixelSize(R.styleable.PeppermintView_borderWidth, Utils.dpToPx(getContext(), DEF_BORDER_WIDTH_DP));
                mCornerRadius = a.getDimensionPixelSize(R.styleable.PeppermintView_cornerRadius, Utils.dpToPx(getContext(), DEF_CORNER_RADIUS_DP));
                mBorderPaint.setColor(a.getColor(R.styleable.PeppermintView_borderColor, Color.parseColor(DEF_BORDER_COLOR)));
            } finally {
                a.recycle();
            }
        }
    }

    @Override
    public void onDraw(Canvas canvas) {
        // load the bitmap
        mImage = drawableToBitmap(getDrawable());

        // init shader
        if (mImage != null) {
            int mCanvasSize = mWidth;
            if(mHeight < mCanvasSize) {
                mCanvasSize = mHeight;
            }

            int bitmapWidth = mCanvasSize;
            int bitmapHeight = mCanvasSize;

            if(isKeepAspectRatio()) {
                float scale = mImage.getHeight() > mImage.getWidth()
                        ? (float) mCanvasSize / (float) mImage.getHeight() : (float) mCanvasSize / (float) mImage.getWidth();
                bitmapWidth = Math.round((float) mImage.getWidth() * scale);
                bitmapHeight = Math.round((float) mImage.getHeight() * scale);
            }

            Bitmap bitmap = Bitmap.createScaledBitmap(mImage, bitmapWidth, bitmapHeight, true);
            BitmapShader shader = new BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
            mPaint.setShader(shader);

            int xOffset = 0;
            int yOffset = 0;

            if(isKeepAspectRatio()) {
                xOffset = (int) (((float) mWidth - (float) bitmap.getWidth()) / 2f);
                yOffset = (int) (((float) mHeight - (float) bitmap.getHeight()) / 2f);
            }

            canvas.drawRoundRect(new RectF(xOffset, yOffset, bitmap.getWidth(), bitmap.getHeight()), mCornerRadius, mCornerRadius, mBorderPaint);
            canvas.drawRoundRect(new RectF(xOffset + mBorderWidth, yOffset + mBorderWidth, bitmap.getWidth() - mBorderWidth, bitmap.getHeight() - mBorderWidth), mCornerRadius - mBorderWidth, mCornerRadius - mBorderWidth, mPaint);

            //bitmap.recycle();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        mWidth = MeasureSpec.getSize(widthMeasureSpec);
        mHeight = MeasureSpec.getSize(heightMeasureSpec);
        setMeasuredDimension(mWidth, mHeight);
    }

    private Bitmap drawableToBitmap(Drawable drawable) {
        if (drawable == null) {
            return null;
        } else if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }

        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }

    public boolean isKeepAspectRatio() {
        return mKeepAspectRatio;
    }

    public void setKeepAspectRatio(boolean mKeepAspectRatio) {
        this.mKeepAspectRatio = mKeepAspectRatio;
    }
}