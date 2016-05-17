package com.peppermint.app.ui.chat.head;

import android.content.Context;
import android.view.View;
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
 *
 * View representing the container on top of which the chat head chain has to be dragged to
 * in order to hide it.
 */
public class HideDisplayView extends DisplayView<View> {

    private static final int BOTTOM_MARGIN_DP = 30;
    private static final int INFLUENCE_RADIUS_DP = 50;
    private static final int VIEW_SIZE_DP = ChatHeadGroupDisplayView.CHAT_HEAD_SIZE_DP + 10;

    private static final float EXPANDED_SCALE = 1.25f;

    // measurements
    private int mInfluenceRadius;
    private int mViewSize, mBottomMargin;

    // rebound scaling
    private SpringSystem mSpringSystem;
    private Spring mScaleSpring;

    private final SpringListener mScaleSpringListener = new SimpleSpringListener() {
        @Override
        public void onSpringUpdate(Spring spring) {
            setViewScale((float) mScaleSpring.getCurrentValue(), (float) mScaleSpring.getCurrentValue());
        }
    };

    public HideDisplayView(Context mContext, Display mDisplay) {
        super(mContext, mDisplay);

        mInfluenceRadius = Utils.dpToPx(mContext, INFLUENCE_RADIUS_DP);
        mBottomMargin = Utils.dpToPx(mContext, BOTTOM_MARGIN_DP);

        mSpringSystem = SpringSystem.create();
        mScaleSpring = mSpringSystem.createSpring();
        mScaleSpring.setSpringConfig(SpringConfig.fromOrigamiTensionAndFriction(20, 2));
        mScaleSpring.addListener(mScaleSpringListener);

        // view
        mViewSize = Utils.dpToPx(mContext, VIEW_SIZE_DP);

        final ImageView view = new ImageView(mContext);
        view.setImageResource(R.drawable.ic_remove_48dp);
        view.setScaleType(ImageView.ScaleType.CENTER_CROP);
        setView(view, mViewSize, mViewSize, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
    }

    @Override
    public void onDisplaySizeObtained(int prevDisplayWidth, int prevDisplayHeight, int displayWidth, int displayHeight) {
        setViewPositionNoUpdateUI((int) ((displayWidth - mViewSize) / 2f), displayHeight - mBottomMargin - mViewSize, false);
        // dont recalculate position (pass 0 as previous measurements)
        super.onDisplaySizeObtained(0, 0, displayWidth, displayHeight);
    }

    /**
     * Checks whether the supplied coordinates are inside this views area.
     *
     * @param x the X coordinate
     * @param y the Y coordinate
     * @return true if inside the area; false otherwise
     */
    public boolean isInsideInfluence(float x, float y) {
        final int posX = getViewPositionX();
        final int posY = getViewPositionY();

        final int areaX1 = posX - mInfluenceRadius;
        final int areaY1 = posY - mInfluenceRadius;
        final int areaX2 = posX + mInfluenceRadius + mViewSize;
        final int areaY2 = posY + mInfluenceRadius + mViewSize;

        return x >= areaX1 && x <= areaX2 && y >= areaY1 && y <= areaY2;
    }

    public void scaleUp() {
        mScaleSpring.setEndValue(EXPANDED_SCALE);
    }

    public void scaleDown() {
        mScaleSpring.setEndValue(1f);
    }
}
