package com.peppermint.app.ui.canvas.progress;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.view.View;

import com.peppermint.app.R;
import com.peppermint.app.ui.canvas.AnimatedLayerBase;
import com.peppermint.app.ui.canvas.BitmapLayer;

/**
 * Created by Nuno Luz on 24-09-2015.
 *
 * Given the specified FPS value, draws a sequence of bitmaps on a Canvas.
 */
public class ProgressEyeAnimatedLayer extends AnimatedLayerBase {

    private BitmapLayer mEyeLayer;
    private float mBlinkFrames;
    private AnimatedLayerBase mBlinkAnimation;
    private Paint mEyeBlinkMaskPaint;
    private Canvas mBlinkCanvas;
    private Bitmap mBlinkBitmap;

    public ProgressEyeAnimatedLayer(Context context, long duration, long blinkDuration, Paint bitmapPaint) {
        super(context);
        setDuration(duration);
        setLooping(true);

        this.mEyeLayer = new BitmapLayer(context, R.drawable.img_logo_eye, bitmapPaint);

        mEyeBlinkMaskPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mEyeBlinkMaskPaint.setColor(Color.WHITE);
        mEyeBlinkMaskPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));

        this.mBlinkFrames = (float) blinkDuration / 40f;
        this.mBlinkAnimation = new AnimatedLayerBase(getContext(), null, blinkDuration) {

            @Override
            protected void onDraw(View view, Canvas canvas, double interpolatedElapsedTime) {
                canvas.save();
                // blink
                double currentFrame = Math.round(interpolatedElapsedTime / this.getDuration() * mBlinkFrames);
                float halfFrames = mBlinkFrames / 2f;

                if(currentFrame < halfFrames) {
                    // scale down Y
                    //canvas.scale(1.0f, 1.0f - (float)(currentFrame/halfFrames), getBounds().centerX(), getBounds().centerY());
                    double factor = currentFrame / halfFrames * (float) (getBounds().height() + 10);
                    canvas.translate(0, (float) (factor - getBounds().height() - 10));
                } else {
                    // scale up Y
                    //canvas.scale(1.0f, (float)((currentFrame-halfFrames)/halfFrames), getBounds().centerX(), getBounds().centerY());
                    double factor = (currentFrame - halfFrames) / halfFrames * (float) (getBounds().height() + 10);
                    canvas.translate(0, (float) -factor);
                }
                canvas.drawCircle(getBounds().centerX(), getBounds().centerY(), getBounds().width() / 2, mEyeBlinkMaskPaint);
                canvas.restore();
            }
        };
    }

    @Override
    public synchronized void onDraw(View view, Canvas canvas, double interpolatedElapsedTime) {
        double angle = 45f + (interpolatedElapsedTime / getDuration() * 360f);
        Rect bounds = getBounds();

        if(mBlinkBitmap != null) {
            mBlinkBitmap.eraseColor(Color.TRANSPARENT);

            mBlinkCanvas.save();
            mBlinkCanvas.rotate((float) angle, mBlinkAnimation.getBounds().centerX(), mBlinkAnimation.getBounds().centerY());
            //mBlinkCanvas.drawBitmap(getBitmap(), null, mBlinkAnimation.getBounds(), getPaint());
            mEyeLayer.draw(view, mBlinkCanvas);
            mBlinkCanvas.restore();

            mBlinkAnimation.draw(view, mBlinkCanvas);

            canvas.drawBitmap(mBlinkBitmap, null, bounds, null);
        }
    }

    public void blink() {
        if(mBlinkAnimation.isRunning()) {
            return;
        }

        //mBlinkAnimation.reset();
        mBlinkAnimation.start();
    }

    @Override
    protected synchronized void onMeasure(Rect bounds) {
        if(bounds.height() > 0 && bounds.width() > 0) {
            mBlinkBitmap = Bitmap.createBitmap(bounds.width(), bounds.height(), Bitmap.Config.ARGB_8888);
            mBlinkCanvas = new Canvas(mBlinkBitmap);
        }
        Rect eyeBounds = new Rect(1, 1, bounds.width()-2, bounds.height()-2);
        mBlinkAnimation.setBounds(eyeBounds);
        mEyeLayer.setBounds(eyeBounds);
    }

    public AnimatedLayerBase getBlinkAnimation() {
        return mBlinkAnimation;
    }
}
