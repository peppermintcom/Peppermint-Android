package com.peppermint.app.ui.chat.head;

import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.ImageView;

import com.facebook.rebound.SimpleSpringListener;
import com.facebook.rebound.Spring;
import com.facebook.rebound.SpringConfig;
import com.facebook.rebound.SpringListener;
import com.facebook.rebound.SpringSystem;
import com.peppermint.app.R;
import com.peppermint.app.utils.Utils;

/**
 * Created by Nuno Luz on 19-03-2016.
 */
public class RemoveView extends WindowManagerViewGroup {

    private static final String TAG = RemoveView.class.getSimpleName();

    private static final int BOTTOM_MARGIN_DP = 36;
    private static final int INFLUENCE_RADIUS_DP = 50;

    private static final float EXPANDED_SCALE = 1.25f;

    // measurements
    private int mScreenWidth, mScreenHeight;
    private int mInfluenceRadius;
    private int mViewSize;

    // rebound
    private SpringSystem mSpringSystem;
    private SpringConfig mSpringConfigScale;
    private Spring mScaleSpring;

    private SpringListener mScaleSpringListener = new SimpleSpringListener() {
        @Override
        public void onSpringUpdate(Spring spring) {
            setViewScale(0, (float) mScaleSpring.getCurrentValue(), (float) mScaleSpring.getCurrentValue());
        }
    };

    public RemoveView(Context mContext) {
        super(mContext);

        mSpringSystem = SpringSystem.create();

        mSpringConfigScale = SpringConfig.fromOrigamiTensionAndFriction(20, 2);

        mScaleSpring = mSpringSystem.createSpring();
        mScaleSpring.setSpringConfig(mSpringConfigScale);
        mScaleSpring.addListener(mScaleSpringListener);

        mInfluenceRadius = Utils.dpToPx(mContext, INFLUENCE_RADIUS_DP);

        // view
        mViewSize = Utils.dpToPx(mContext, ChatHeadChainView.CHATHEAD_SIZE_DP);

        ImageView view = new ImageView(mContext);
        view.setImageResource(R.drawable.ic_remove_48dp);

        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(
                mViewSize,
                mViewSize,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED |
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        layoutParams.gravity = Gravity.TOP | Gravity.LEFT;

        addView(view, layoutParams);
    }

    public void requestLayout() {
        Point point = Utils.getScreenSize(mContext);
        this.mScreenWidth = point.x - mViewSize;
        this.mScreenHeight = point.y - mViewSize;
        setViewPosition(0, (int) (mScreenWidth / 2f),
                mScreenHeight - Utils.dpToPx(mContext, BOTTOM_MARGIN_DP));
    }

    public boolean isInsideInfluence(float x, float y) {
        int posX = getViewPositionX(0);
        int posY = getViewPositionY(0);

        int areaX1 = posX - mInfluenceRadius;
        int areaY1 = posY - mInfluenceRadius;
        int areaX2 = posX + mInfluenceRadius + mViewSize;
        int areaY2 = posY + mInfluenceRadius + mViewSize;

        if(x >= areaX1 && x <= areaX2 && y >= areaY1 && y <= areaY2) {
            return true;
        }

        return false;
    }

    public int getSnapPositionX() {
        return mOriginalLayoutParams.get(0).x;
    }

    public int getSnapPositionY() {
        return mOriginalLayoutParams.get(0).y;
    }

    public void expand() {
        mScaleSpring.setEndValue(EXPANDED_SCALE);
    }

    public void shrink() {
        mScaleSpring.setEndValue(1f);
    }
}
