package com.peppermint.app.ui.canvas;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

/**
 * Created by Nuno Luz on 12-10-2015.
 *
 * A {@link Layer} that contains text.
 */
public class TextLayer extends LayerBase implements Layer {

    private Paint mPaint;
    private float mTextSize, mTextSpacing;
    private String mText;

    public TextLayer(Context context, float textSize, float textSpacing, Paint paint) {
        super(context);
        this.mTextSize = textSize;
        this.mTextSpacing = textSpacing;
        this.mPaint = paint;
    }

    @Override
    public synchronized void draw(View view, Canvas canvas) {
        if(canvas != null) {
            String[] split = mText.split("\n");
            float offsetY = - (((mTextSize/2f)+(mTextSpacing/2f)) * (float) (split.length - 1));
            for(int i=0; i<split.length; i++) {
                canvas.drawText(split[i], getBounds().centerX(), offsetY + getBounds().centerY() + ((mTextSize + mTextSpacing) * (float) i), mPaint);
            }
        }
    }

    public Paint getPaint() {
        return mPaint;
    }

    public float getTextSize() {
        return mTextSize;
    }

    public synchronized void setTextSize(float mTextSize) {
        this.mTextSize = mTextSize;
    }

    /**
     * Get the vertical spacing between text lines.
     * @return the spacing in pixels
     */
    public float getTextSpacing() {
        return mTextSpacing;
    }

    /**
     * Set the vertical spacing between text lines.
     * @param mTextSpacing the new spacing in pixels
     */
    public synchronized void setTextSpacing(float mTextSpacing) {
        this.mTextSpacing = mTextSpacing;
    }

    public String getText() {
        return mText;
    }

    public synchronized void setText(String mText) {
        this.mText = mText;
    }
}
