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
public class RoundImageView extends ImageView {

    private static final String DEF_BORDER_COLOR = "#ffffff";
    private static final int DEF_BORDER_WIDTH_DP = 3;
    private static final int DEF_CORNER_RADIUS_DP = 10;

    private int mBorderWidth, mCornerRadius;
    private Bitmap mImage;
    private Paint mPaint, mBorderPaint;
    private int mCanvasSize;

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
            mCanvasSize = canvas.getWidth();
            if(canvas.getHeight() < mCanvasSize) {
                mCanvasSize = canvas.getHeight();
            }

            BitmapShader shader = new BitmapShader(Bitmap.createScaledBitmap(mImage, mCanvasSize, mCanvasSize, false), Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
            mPaint.setShader(shader);

            canvas.drawRoundRect(new RectF(0, 0, mCanvasSize, mCanvasSize), mCornerRadius, mCornerRadius, mBorderPaint);
            canvas.drawRoundRect(new RectF(mBorderWidth, mBorderWidth, mCanvasSize-mBorderWidth, mCanvasSize-mBorderWidth), mCornerRadius-mBorderWidth, mCornerRadius-mBorderWidth, mPaint);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = measureWidth(widthMeasureSpec);
        int height = measureHeight(heightMeasureSpec);
        setMeasuredDimension(width, height);
    }

    private int measureWidth(int measureSpec) {
        int result;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);

        if (specMode == MeasureSpec.EXACTLY) {
            // the parent has determined an exact size for the child.
            result = specSize;
        } else if (specMode == MeasureSpec.AT_MOST) {
            // the child can be as large as it wants up to the specified size.
            result = specSize;
        } else {
            // the parent has not imposed any constraint on the child.
            result = mCanvasSize;
        }

        return result;
    }

    private int measureHeight(int measureSpecHeight) {
        int result;
        int specMode = MeasureSpec.getMode(measureSpecHeight);
        int specSize = MeasureSpec.getSize(measureSpecHeight);

        if (specMode == MeasureSpec.EXACTLY) {
            // we were told how big to be
            result = specSize;
        } else if (specMode == MeasureSpec.AT_MOST) {
            // the child can be as large as it wants up to the specified size.
            result = specSize;
        } else {
            // measure the text (beware: ascent is a negative number)
            result = mCanvasSize;
        }

        return (result + 2);
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
}