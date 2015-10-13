package com.peppermint.app.ui.canvas.progress;

import android.content.Context;
import android.graphics.Paint;

import com.peppermint.app.R;
import com.peppermint.app.ui.canvas.BitmapSequenceAnimatedLayer;

/**
 * Created by Nuno Luz on 06-10-2015.
 *
 * An animated layer that represents the Peppermint logo mouth opening slightly.
 */
public class SplashMouthAnimatedLayer extends BitmapSequenceAnimatedLayer {
    public SplashMouthAnimatedLayer(Context context, long duration, Paint paint) {
        super(context, duration, paint);
        setLooping(false);

        final int normal1Duration = 1;

        setBitmapSequenceResourceIds(new int[]{R.drawable.img_opening_mouth_1, normal1Duration}
                , new int[]{R.drawable.img_opening_mouth_2, normal1Duration}
                , new int[]{R.drawable.img_opening_mouth_3, normal1Duration}
                , new int[]{R.drawable.img_opening_mouth_4, normal1Duration}
                , new int[]{R.drawable.img_opening_mouth_5, normal1Duration}
                , new int[]{R.drawable.img_opening_mouth_6, normal1Duration}
                , new int[]{R.drawable.img_opening_mouth_7, normal1Duration}
                , new int[]{R.drawable.img_opening_mouth_8, normal1Duration}
                , new int[]{R.drawable.img_opening_mouth_9, normal1Duration});
    }
}
