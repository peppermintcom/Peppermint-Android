package com.peppermint.app.ui.canvas.avatar;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.util.AttributeSet;

import com.peppermint.app.R;
import com.peppermint.app.dal.contact.ContactRaw;
import com.peppermint.app.ui.canvas.AnimatedView;
import com.peppermint.app.ui.canvas.BitmapLayer;
import com.peppermint.app.ui.canvas.BitmapSequenceAnimatedLayer;
import com.peppermint.app.utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by Nuno Luz on 15-09-2015.
 *
 * Avatar view for {@link ContactRaw}s.<br />
 * It can either show a static picture as the avatar, or an animated sequence of pictures.<br />
 * The animation can be triggered through {@link #startAnimations()} and {@link #startDrawingThread()}.
 */
public class AnimatedAvatarView extends AnimatedView {

    private static final int DEF_BORDER_WIDTH_DP = 3;
    private static final int DEF_CORNER_RADIUS_DP = 10;
    private static final String DEF_BORDER_COLOR = "#ffffff";

    private Random mRandom = new Random();

    private List<BitmapSequenceAnimatedLayer> mAvatars = new ArrayList<>();
    private BitmapSequenceAnimatedLayer mCurrentAvatar;
    private BitmapLayer mStaticAvatar;
    private Rect mFullBounds = new Rect();

    private int mHeight = -1, mWidth = -1;

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
        int cornerRadius = Utils.dpToPx(getContext(), DEF_CORNER_RADIUS_DP);
        int borderWidth = Utils.dpToPx(getContext(), DEF_BORDER_WIDTH_DP);

        Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setStyle(Paint.Style.FILL);

        if (attrs != null) {
            TypedArray a = getContext().getTheme().obtainStyledAttributes(
                    attrs,
                    R.styleable.PeppermintView,
                    0, 0);

            try {
                cornerRadius = a.getDimensionPixelSize(R.styleable.PeppermintView_cornerRadius, Utils.dpToPx(getContext(), DEF_CORNER_RADIUS_DP));
                borderWidth = a.getDimensionPixelSize(R.styleable.PeppermintView_roundBorderWidth, Utils.dpToPx(getContext(), DEF_BORDER_WIDTH_DP));
                borderPaint.setColor(a.getColor(R.styleable.PeppermintView_roundBorderColor, Color.parseColor(DEF_BORDER_COLOR)));
            } finally {
                a.recycle();
            }
        }

        Paint bitmapPaint = new Paint();
        bitmapPaint.setAntiAlias(true);
        bitmapPaint.setFilterBitmap(true);
        bitmapPaint.setDither(true);

        WinkAvatarAnimatedLayer winkAvatar = new WinkAvatarAnimatedLayer(getContext(), 2500, bitmapPaint);
        winkAvatar.setBorderWidth(borderWidth);
        winkAvatar.setBorderPaint(borderPaint);
        winkAvatar.setCornerRadius(cornerRadius);
        mAvatars.add(winkAvatar);

        TongueOutAvatarAnimatedLayer tongueAvatar = new TongueOutAvatarAnimatedLayer(getContext(), 2500, bitmapPaint);
        tongueAvatar.setBorderWidth(borderWidth);
        tongueAvatar.setBorderPaint(borderPaint);
        tongueAvatar.setCornerRadius(cornerRadius);
        mAvatars.add(tongueAvatar);

        SurprisedAvatarAnimatedLayer surprisedAvatar = new SurprisedAvatarAnimatedLayer(getContext(), 2000, bitmapPaint);
        surprisedAvatar.setBorderWidth(borderWidth);
        surprisedAvatar.setBorderPaint(borderPaint);
        surprisedAvatar.setCornerRadius(cornerRadius);
        mAvatars.add(surprisedAvatar);

        SillyAvatarAnimatedLayer sillyAvatar = new SillyAvatarAnimatedLayer(getContext(), 2500, bitmapPaint);
        sillyAvatar.setBorderWidth(borderWidth);
        sillyAvatar.setBorderPaint(borderPaint);
        sillyAvatar.setCornerRadius(cornerRadius);
        mAvatars.add(sillyAvatar);

        AngryAvatarAnimatedLayer angryAvatar = new AngryAvatarAnimatedLayer(getContext(), 2500, bitmapPaint);
        angryAvatar.setBorderWidth(borderWidth);
        angryAvatar.setBorderPaint(borderPaint);
        angryAvatar.setCornerRadius(cornerRadius);
        mAvatars.add(angryAvatar);

        CryingAvatarAnimatedLayer cryingAvatar = new CryingAvatarAnimatedLayer(getContext(), 2500, bitmapPaint);
        cryingAvatar.setBorderWidth(borderWidth);
        cryingAvatar.setBorderPaint(borderPaint);
        cryingAvatar.setCornerRadius(cornerRadius);
        mAvatars.add(cryingAvatar);

        EvilAvatarAnimatedLayer evilAvatar = new EvilAvatarAnimatedLayer(getContext(), 2500, bitmapPaint);
        evilAvatar.setBorderWidth(borderWidth);
        evilAvatar.setBorderPaint(borderPaint);
        evilAvatar.setCornerRadius(cornerRadius);
        mAvatars.add(evilAvatar);

        SadAvatarAnimatedLayer sadAvatar = new SadAvatarAnimatedLayer(getContext(), 2500, bitmapPaint);
        sadAvatar.setBorderWidth(borderWidth);
        sadAvatar.setBorderPaint(borderPaint);
        sadAvatar.setCornerRadius(cornerRadius);
        mAvatars.add(sadAvatar);

        AnxiousAvatarAnimatedLayer anxiousAvatar = new AnxiousAvatarAnimatedLayer(getContext(), 2500, bitmapPaint);
        anxiousAvatar.setBorderWidth(borderWidth);
        anxiousAvatar.setBorderPaint(borderPaint);
        anxiousAvatar.setCornerRadius(cornerRadius);
        mAvatars.add(anxiousAvatar);

        InLoveAvatarAnimatedLayer inloveAvatar = new InLoveAvatarAnimatedLayer(getContext(), 2500, bitmapPaint);
        inloveAvatar.setBorderWidth(borderWidth);
        inloveAvatar.setBorderPaint(borderPaint);
        inloveAvatar.setCornerRadius(cornerRadius);
        mAvatars.add(inloveAvatar);

        mStaticAvatar = new BitmapLayer(getContext(), R.drawable.ic_anonymous_green_48dp, bitmapPaint);
        mStaticAvatar.setBorderWidth(borderWidth);
        mStaticAvatar.setBorderPaint(borderPaint);
        mStaticAvatar.setCornerRadius(cornerRadius);

        mCurrentAvatar = getRandomAnimatedAvatar();
        addLayer(mCurrentAvatar);
    }

    @Override
    public synchronized void resetAnimations() {
        for(BitmapSequenceAnimatedLayer avatarLayer : mAvatars) {
            avatarLayer.reset();
        }

        mCurrentAvatar = getRandomAnimatedAvatar();
        addLayer(mCurrentAvatar);

        super.resetAnimations();
    }

    @Override
    protected synchronized void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        if(getMeasuredHeight() == 0 || getMeasuredWidth() == 0 || (getMeasuredHeight() == mHeight && getMeasuredWidth() == mWidth)) {
            return;
        }

        mHeight = getMeasuredHeight();
        mWidth = getMeasuredWidth();

        // box bounds (get values obtained in parent onMeasure)
        mFullBounds.set(0, 0, (int) getLocalWidth(), (int) getLocalHeight());
        for(BitmapSequenceAnimatedLayer avatarLayer : mAvatars) {
            avatarLayer.setBounds(mFullBounds);
        }
        mStaticAvatar.setBounds(mFullBounds);
    }

    public synchronized void setStaticDrawable(BitmapDrawable bitmapDrawable) {
        mStaticAvatar.setBitmapDrawable(bitmapDrawable);
    }

    public boolean isShowStaticAvatar() {
        return getLayers().get(0).equals(mStaticAvatar);
    }

    public synchronized void setShowStaticAvatar(boolean mShowStaticAvatar) {
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

    protected BitmapSequenceAnimatedLayer getRandomAnimatedAvatar() {
        return mAvatars.get(mRandom.nextInt(mAvatars.size()));
    }
}
