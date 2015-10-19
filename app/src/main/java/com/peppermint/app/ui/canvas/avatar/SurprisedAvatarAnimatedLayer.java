package com.peppermint.app.ui.canvas.avatar;

import android.content.Context;
import android.graphics.Paint;

import com.peppermint.app.ui.canvas.BitmapSequenceAnimatedLayer;

/**
 * Created by Nuno Luz on 06-10-2015.
 *
 * An animated layer that represents an anonymous {@link com.peppermint.app.data.Recipient}'s avatar.
 */
public class SurprisedAvatarAnimatedLayer extends BitmapSequenceAnimatedLayer {
    public SurprisedAvatarAnimatedLayer(Context context, long duration, Paint paint) {
        super(context, duration, paint);

        int[] frames = new int[31];
        for(int i=0; i<31; i++) {
            frames[i] = context.getResources().getIdentifier("ic_avatar_surprised_48dp_" + i, "drawable", context.getPackageName());
        }
        setBitmapSequenceResourceIds(true, frames);
    }
}
