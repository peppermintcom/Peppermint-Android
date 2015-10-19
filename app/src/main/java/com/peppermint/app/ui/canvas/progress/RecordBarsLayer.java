package com.peppermint.app.ui.canvas.progress;

import android.content.Context;
import android.graphics.Paint;
import android.util.Log;

import com.peppermint.app.ui.canvas.BitmapSequenceAnimatedLayer;

/**
 * Created by Nuno Luz on 06-10-2015.
 *
 * An animated layer that represents the Peppermint recording animation mouth opening and closing.
 */
public class RecordBarsLayer extends BitmapSequenceAnimatedLayer {
    public RecordBarsLayer(Context context, long duration, Paint paint) {
        super(context, duration, paint);
        setLooping(true);

        int[] frames = new int[215];
        for(int i=0; i<=214; i++) {
            frames[i] = context.getResources().getIdentifier("img_bars_" + String.format("%05d", i), "drawable", context.getPackageName());
        }
        setBitmapSequenceResourceIds(true, frames);
    }
}
