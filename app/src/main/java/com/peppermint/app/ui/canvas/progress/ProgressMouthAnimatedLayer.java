package com.peppermint.app.ui.canvas.progress;

import android.content.Context;
import android.graphics.Paint;

import com.peppermint.app.R;
import com.peppermint.app.ui.canvas.BitmapSequenceAnimatedLayer;

/**
 * Created by Nuno Luz on 06-10-2015.
 *
 * An animated layer that represents the Peppermint recording animation mouth opening and closing.
 */
public class ProgressMouthAnimatedLayer extends BitmapSequenceAnimatedLayer {
    public ProgressMouthAnimatedLayer(Context context, long duration, Paint paint) {
        super(context, duration, paint);
        setLooping(true);

        final int normal1Duration = 1;
        final int normal2Duration = 2;
        final int longLasted = 200;
        final int openMouth = 40;
        final int closedMouth = 40;

        setBitmapSequenceResourceIds(true, new int[]{R.drawable.img_opening_mouth_1, closedMouth}
                , new int[]{R.drawable.img_opening_mouth_2, normal1Duration}
                , new int[]{R.drawable.img_opening_mouth_3, normal1Duration}
                , new int[]{R.drawable.img_opening_mouth_4, normal1Duration}
                , new int[]{R.drawable.img_opening_mouth_5, normal1Duration}
                , new int[]{R.drawable.img_opening_mouth_6, normal1Duration}
                , new int[]{R.drawable.img_opening_mouth_7, normal1Duration}
                , new int[]{R.drawable.img_opening_mouth_8, normal1Duration}
                , new int[]{R.drawable.img_opening_mouth_9, longLasted}
                , new int[]{R.drawable.img_opening_mouth_more_1, normal2Duration}
                , new int[]{R.drawable.img_opening_mouth_more_2, normal2Duration}
                , new int[]{R.drawable.img_opening_mouth_more_3, normal2Duration}
                , new int[]{R.drawable.img_opening_mouth_more_4, normal2Duration}
                , new int[]{R.drawable.img_opening_mouth_more_5, normal2Duration}
                , new int[]{R.drawable.img_opening_mouth_more_6, openMouth}
                , new int[]{R.drawable.img_opening_mouth_more_5, normal2Duration}
                , new int[]{R.drawable.img_opening_mouth_more_4, normal2Duration}
                , new int[]{R.drawable.img_opening_mouth_more_3, normal2Duration}
                , new int[]{R.drawable.img_opening_mouth_more_2, normal2Duration}
                , new int[]{R.drawable.img_opening_mouth_more_1, normal2Duration}
                , new int[]{R.drawable.img_opening_mouth_9, openMouth}
                , new int[]{R.drawable.img_opening_mouth_8, normal1Duration}
                , new int[]{R.drawable.img_opening_mouth_7, normal1Duration}
                , new int[]{R.drawable.img_opening_mouth_6, normal1Duration}
                , new int[]{R.drawable.img_opening_mouth_5, normal1Duration}
                , new int[]{R.drawable.img_opening_mouth_4, normal1Duration}
                , new int[]{R.drawable.img_opening_mouth_3, normal1Duration}
                , new int[]{R.drawable.img_opening_mouth_2, normal1Duration}
                , new int[]{R.drawable.img_opening_mouth_1, closedMouth});
    }
}
