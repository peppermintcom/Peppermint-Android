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
import com.peppermint.app.ui.canvas.BitmapSequenceAnimatedLayer;
import com.peppermint.app.utils.Utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by Nuno Luz on 15-09-2015.
 *
 * Avatar view for {@link com.peppermint.app.data.Recipient}s.<br />
 * It can either show a static picture as the avatar, or an animated sequence of pictures.<br />
 * The animation can be triggered through {@link #startAnimations()} and {@link #startDrawingThread()}.
 */
public class AnimatedAvatarView extends AnimatedView {

    private static final String TAG = AnimatedAvatarView.class.getSimpleName();

    private static final int DEF_BORDER_WIDTH_DP = 3;
    private static final int DEF_CORNER_RADIUS_DP = 10;
    private static final String DEF_BORDER_COLOR = "#ffffff";

    private Random mRandom = new Random();

    private int mCornerRadius, mBorderWidth;
    private Paint mBorderPaint, mBitmapPaint;

    private List<BitmapSequenceAnimatedLayer> mAvatars = new ArrayList<>();
    private BitmapSequenceAnimatedLayer mCurrentAvatar;
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

        WinkAvatarAnimatedLayer winkAvatar = new WinkAvatarAnimatedLayer(getContext(), 2500, mBitmapPaint);
        winkAvatar.setBorderWidth(mBorderWidth);
        winkAvatar.setBorderPaint(mBorderPaint);
        winkAvatar.setCornerRadius(mCornerRadius);
        mAvatars.add(winkAvatar);

        TongueOutAvatarAnimatedLayer tongueAvatar = new TongueOutAvatarAnimatedLayer(getContext(), 2500, mBitmapPaint);
        tongueAvatar.setBorderWidth(mBorderWidth);
        tongueAvatar.setBorderPaint(mBorderPaint);
        tongueAvatar.setCornerRadius(mCornerRadius);
        mAvatars.add(tongueAvatar);

        SurprisedAvatarAnimatedLayer surprisedAvatar = new SurprisedAvatarAnimatedLayer(getContext(), 2000, mBitmapPaint);
        surprisedAvatar.setBorderWidth(mBorderWidth);
        surprisedAvatar.setBorderPaint(mBorderPaint);
        surprisedAvatar.setCornerRadius(mCornerRadius);
        mAvatars.add(surprisedAvatar);

        SillyAvatarAnimatedLayer sillyAvatar = new SillyAvatarAnimatedLayer(getContext(), 2500, mBitmapPaint);
        sillyAvatar.setBorderWidth(mBorderWidth);
        sillyAvatar.setBorderPaint(mBorderPaint);
        sillyAvatar.setCornerRadius(mCornerRadius);
        mAvatars.add(sillyAvatar);

        AngryAvatarAnimatedLayer angryAvatar = new AngryAvatarAnimatedLayer(getContext(), 2500, mBitmapPaint);
        angryAvatar.setBorderWidth(mBorderWidth);
        angryAvatar.setBorderPaint(mBorderPaint);
        angryAvatar.setCornerRadius(mCornerRadius);
        mAvatars.add(angryAvatar);

        CryingAvatarAnimatedLayer cryingAvatar = new CryingAvatarAnimatedLayer(getContext(), 2500, mBitmapPaint);
        cryingAvatar.setBorderWidth(mBorderWidth);
        cryingAvatar.setBorderPaint(mBorderPaint);
        cryingAvatar.setCornerRadius(mCornerRadius);
        mAvatars.add(cryingAvatar);

        EvilAvatarAnimatedLayer evilAvatar = new EvilAvatarAnimatedLayer(getContext(), 2500, mBitmapPaint);
        evilAvatar.setBorderWidth(mBorderWidth);
        evilAvatar.setBorderPaint(mBorderPaint);
        evilAvatar.setCornerRadius(mCornerRadius);
        mAvatars.add(evilAvatar);

        SadAvatarAnimatedLayer sadAvatar = new SadAvatarAnimatedLayer(getContext(), 2500, mBitmapPaint);
        sadAvatar.setBorderWidth(mBorderWidth);
        sadAvatar.setBorderPaint(mBorderPaint);
        sadAvatar.setCornerRadius(mCornerRadius);
        mAvatars.add(sadAvatar);

        AnxiousAvatarAnimatedLayer anxiousAvatar = new AnxiousAvatarAnimatedLayer(getContext(), 2500, mBitmapPaint);
        anxiousAvatar.setBorderWidth(mBorderWidth);
        anxiousAvatar.setBorderPaint(mBorderPaint);
        anxiousAvatar.setCornerRadius(mCornerRadius);
        mAvatars.add(anxiousAvatar);

        mStaticAvatar = new BitmapLayer(getContext(), R.drawable.ic_anonymous_green_48dp, mBitmapPaint);
        mStaticAvatar.setBorderWidth(mBorderWidth);
        mStaticAvatar.setBorderPaint(mBorderPaint);
        mStaticAvatar.setCornerRadius(mCornerRadius);

        mCurrentAvatar = getRandomAnimatedAvatar();
        addLayer(mCurrentAvatar);
    }

    @Override
    public void resetAnimations() {
        for(BitmapSequenceAnimatedLayer avatarLayer : mAvatars) {
            avatarLayer.reset();
        }

        mCurrentAvatar = getRandomAnimatedAvatar();
        addLayer(mCurrentAvatar);

        super.resetAnimations();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        // box bounds (get values obtained in parent onMeasure)
        Rect fullBounds = new Rect(0, 0, (int) getLocalWidth(), (int) getLocalHeight());
        for(BitmapSequenceAnimatedLayer avatarLayer : mAvatars) {
            avatarLayer.setBounds(fullBounds);
        }
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
            removeLayers();
            addLayer(mStaticAvatar);
        } else {
            mCurrentAvatar = getRandomAnimatedAvatar();
            removeLayers();
            addLayer(mCurrentAvatar);
        }
        doDraw();
    }

    public BitmapSequenceAnimatedLayer getCurrentAvatar() {
        return mCurrentAvatar;
    }

    public List<BitmapSequenceAnimatedLayer> getAvatars() {
        return mAvatars;
    }

    protected BitmapSequenceAnimatedLayer getRandomAnimatedAvatar() {
        return mAvatars.get(mRandom.nextInt(mAvatars.size()));
    }
}
