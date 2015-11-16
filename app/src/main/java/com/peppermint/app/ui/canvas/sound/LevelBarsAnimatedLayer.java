package com.peppermint.app.ui.canvas.sound;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.View;

import com.peppermint.app.ui.canvas.AnimatedLayerBase;
import com.peppermint.app.utils.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Nuno Luz on 24-09-2015.
 *
 * Given the specified FPS value, draws a sequence of bitmaps on a Canvas.
 */
public class LevelBarsAnimatedLayer extends AnimatedLayerBase {

    private static final int DEFAULT_BAR_AMOUNT = 6;
    private static final int DEFAULT_BAR_SPACING_DP = 1;

    private int mBarAmount = DEFAULT_BAR_AMOUNT;
    private List<Float> mBarValues = new ArrayList<>();
    private float mBarSpacing;
    private float mCircleRadius;
    private Paint mPaintBottom, mPaintTop, mPaintBottom65, mPaintBottom35;
    private float mSize;
    private boolean mReversed = false;
    private double mLastTime = 0;

    public LevelBarsAnimatedLayer(Context context) {
        super(context, null, 1000);
        setLooping(true);

        mPaintBottom = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaintBottom.setColor(Color.WHITE);

        mPaintBottom65 = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaintBottom65.setColor(Color.parseColor("#A6FFFFFF"));

        mPaintBottom35 = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaintBottom35.setColor(Color.parseColor("#59FFFFFF"));

        mPaintTop = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaintTop.setColor(Color.parseColor("#22857b"));

        mBarSpacing = Utils.dpToPx(context, DEFAULT_BAR_SPACING_DP);
    }

    private void drawBar(Canvas canvas, float barValue) {
        canvas.save();
        float barHeight = Math.max(1f, barValue*2f);
        for (int j = 0; j < barHeight; j++) {
            canvas.drawCircle(mCircleRadius, -mCircleRadius, mCircleRadius, j >= 6 ? mPaintTop : mPaintBottom);
            canvas.translate(0, -((mCircleRadius*2f) + mBarSpacing));
        }
        canvas.restore();
    }

    @Override
    public synchronized void onDraw(View view, Canvas canvas, double interpolatedElapsedTime) {

        if(mLastTime > interpolatedElapsedTime) {
            mReversed = !mReversed;
        }
        mLastTime = interpolatedElapsedTime;

        int effectPos = (int) Math.floor(interpolatedElapsedTime / 1000f * (((float) mBarAmount * 2f) - 1));
        if(effectPos >= ((mBarAmount*2)-1)) {
            effectPos--;
        }
        if(mReversed) {
            effectPos = ((mBarAmount*2) - 2) - effectPos;
        }

        canvas.save();
        canvas.translate(getBounds().centerX() - (mSize / 2f), getBounds().centerY() + (mSize / 2f));

        canvas.save();
        canvas.translate(((mCircleRadius * 2f) + mBarSpacing) * (effectPos + (mReversed ? 1 : 0)), (-((mCircleRadius * 2f) + mBarSpacing)));
        canvas.drawCircle(mCircleRadius, -mCircleRadius, mCircleRadius, mPaintBottom);

        int spaceBehind = mReversed ? ((mBarAmount*2)-2) - effectPos : effectPos;

        // trail
        if(spaceBehind >= 1) {
            canvas.translate((mReversed ? 1 : -1) * ((mCircleRadius * 2f) + mBarSpacing), 0);
            canvas.drawCircle(mCircleRadius, -mCircleRadius, mCircleRadius, mPaintBottom65);
        }
        if(spaceBehind >= 2) {
            canvas.translate((mReversed ? 1 : -1) * ((mCircleRadius * 2f) + mBarSpacing), 0);
            canvas.drawCircle(mCircleRadius, -mCircleRadius, mCircleRadius, mPaintBottom35);
        }

        canvas.restore();

        for (int i = 0; i < mBarAmount; i++) {
            try {
                drawBar(canvas, mBarValues.size() > i ? mBarValues.get(i) : 0f);
            } catch (ArrayIndexOutOfBoundsException e) {
                drawBar(canvas, 0f);
            }
            canvas.translate((mCircleRadius*2f) + mBarSpacing, 0);
        }
        for (int i = mBarAmount - 1; i >= 0; i--) {
            try {
                drawBar(canvas, mBarValues.size() > i ? mBarValues.get(i) : 0f);
            } catch (ArrayIndexOutOfBoundsException e) {
                drawBar(canvas, 0f);
            }
            canvas.translate((mCircleRadius*2f) + mBarSpacing, 0);
        }

        canvas.restore();
    }

    @Override
    protected synchronized void onMeasure(Rect bounds) {
        super.onMeasure(bounds);
        if(bounds.height() > 0 && bounds.width() > 0) {
            mSize = bounds.height() > bounds.width() ? bounds.width() : bounds.height();
            float min = mSize - (mBarSpacing * (((float) mBarAmount * 2f) - 1f));
            mCircleRadius = (min / ((float) mBarAmount * 2f)) / 2f;
        }
    }

    public float getBarAmount() {
        return mBarAmount;
    }

    public synchronized void setBarAmount(int mBarAmount) {
        this.mBarAmount = mBarAmount;
        this.mBarValues = new ArrayList<>();
    }

    public void pushAmplitude(float... values) {
        for(int i=0; i<values.length; i++) {
            pushAmplitude(values[i]);
        }
    }

    public synchronized void pushAmplitude(float value) {
        if(value < 0) {
            value = 0;
        } else if(value > 1) {
            value = 1;
        }

        if(mBarValues.size() >= mBarAmount) {
            mBarValues.remove(0);
        }

        mBarValues.add(value * (float) mBarAmount);
    }

    public List<Float> getBarValues() {
        return mBarValues;
    }
}
