package com.peppermint.app.ui.canvas.avatar;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;

import com.peppermint.app.R;
import com.peppermint.app.ui.canvas.AnimatedView;
import com.peppermint.app.ui.canvas.BitmapLayer;
import com.peppermint.app.utils.Utils;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by Nuno Luz on 15-09-2015.
 *
 * Custom recording view for Peppermint to show the progress of the recording.
 */
public class AnimatedAvatarView extends AnimatedView {

    private static final String TAG = AnimatedAvatarView.class.getSimpleName();

    private static final int DEF_BORDER_WIDTH_DP = 3;
    private static final int DEF_CORNER_RADIUS_DP = 10;
    private static final String DEF_BORDER_COLOR = "#ffffff";

    private int mCornerRadius, mBorderWidth;
    private Paint mBorderPaint, mBitmapPaint;

    private AvatarAnimatedLayer mAvatar;
    private BitmapLayer mStaticAvatar;

    public AnimatedAvatarView(Context context) {
        super(context);
        init(null);
    }

    public AnimatedAvatarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public AnimatedAvatarView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public AnimatedAvatarView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(attrs);
    }

    protected void init(AttributeSet attrs) {
        mCornerRadius = Utils.dpToPx(getContext(), DEF_CORNER_RADIUS_DP);
        mBorderWidth = Utils.dpToPx(getContext(), DEF_BORDER_WIDTH_DP);

        mBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mBorderPaint.setStyle(Paint.Style.FILL);

        if (attrs != null) {
            TypedArray a = getContext().getTheme().obtainStyledAttributes(
                    attrs,
                    R.styleable.PeppermintView,
                    0, 0);

            try {
                mCornerRadius = a.getDimensionPixelSize(R.styleable.PeppermintView_cornerRadius, Utils.dpToPx(getContext(), DEF_CORNER_RADIUS_DP));
                mBorderWidth = a.getDimensionPixelSize(R.styleable.PeppermintView_borderWidth, Utils.dpToPx(getContext(), DEF_BORDER_WIDTH_DP));
                mBorderPaint.setColor(a.getColor(R.styleable.PeppermintView_borderColor, Color.parseColor(DEF_BORDER_COLOR)));
            } finally {
                a.recycle();
            }
        }

        mBitmapPaint = new Paint();
        mBitmapPaint.setAntiAlias(true);
        mBitmapPaint.setFilterBitmap(true);
        mBitmapPaint.setDither(true);

        mAvatar = new AvatarAnimatedLayer(getContext(), 2500, mBitmapPaint);
        mAvatar.setBorderWidth(mBorderWidth);
        mAvatar.setBorderPaint(mBorderPaint);
        mAvatar.setCornerRadius(mCornerRadius);

        mStaticAvatar = new BitmapLayer(getContext(), R.drawable.ic_anonymous_green_48dp, mBitmapPaint);
        mStaticAvatar.setBorderWidth(mBorderWidth);
        mStaticAvatar.setBorderPaint(mBorderPaint);
        mStaticAvatar.setCornerRadius(mCornerRadius);

        addLayer(mAvatar);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        // box bounds (get values obtained in parent onMeasure)
        Rect fullBounds = new Rect(0, 0, (int) getLocalWidth(), (int) getLocalHeight());
        mAvatar.setBounds(fullBounds);
        mStaticAvatar.setBounds(fullBounds);
    }

    public boolean setStaticDrawable(Uri drawableUri) {
        mStaticAvatar.setBitmapDrawable((BitmapDrawable) getBitmapFromURI(drawableUri));
        doDraw();
        return mStaticAvatar.getBitmapDrawable() != null;
    }

    public boolean setStaticDrawable(int drawableRes) {
        mStaticAvatar.setBitmapResourceId(drawableRes);
        doDraw();
        return true;
    }

    protected Drawable getBitmapFromURI(Uri uri) {
        try {
            InputStream inputStream = getContext().getContentResolver().openInputStream(uri);
            return Drawable.createFromStream(inputStream, uri.toString());
        } catch (IOException e) {
            Log.e(TAG, "Unable to get bitmap from URI!");
        }
        return null;
    }

    public boolean isShowStaticAvatar() {
        return getLayers().get(0).equals(mStaticAvatar);
    }

    public void setShowStaticAvatar(boolean mShowStaticAvatar) {
        if(mShowStaticAvatar) {
            removeLayer(mAvatar);
            addLayer(mStaticAvatar);
        } else {
            removeLayer(mStaticAvatar);
            addLayer(mAvatar);
        }
        doDraw();
    }

    public AvatarAnimatedLayer getAvatar() {
        return mAvatar;
    }
}
