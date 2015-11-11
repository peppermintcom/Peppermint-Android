package com.peppermint.app.ui.canvas.avatar;

import android.content.Context;
import android.graphics.Paint;

import com.peppermint.app.R;
import com.peppermint.app.ui.canvas.BitmapSequenceAnimatedLayer;

/**
 * Created by Nuno Luz on 06-10-2015.
 *
 * An animated layer that represents an anonymous {@link com.peppermint.app.data.Recipient}'s avatar.
 */
public class SurprisedAvatarAnimatedLayer extends BitmapSequenceAnimatedLayer {
    public SurprisedAvatarAnimatedLayer(Context context, long duration, Paint paint) {
        super(context, duration, paint);

        setBitmapSequenceResourceIds(true, new int[]{R.drawable.ic_avatar_surprised_48dp_0, 1}
                , new int[]{R.drawable.ic_avatar_surprised_48dp_1, 1}
                , new int[]{R.drawable.ic_avatar_surprised_48dp_2, 1}
                , new int[]{R.drawable.ic_avatar_surprised_48dp_3, 1}
                , new int[]{R.drawable.ic_avatar_surprised_48dp_4, 1}
                , new int[]{R.drawable.ic_avatar_surprised_48dp_5, 16}
                , new int[]{R.drawable.ic_avatar_surprised_48dp_21, 1}
                , new int[]{R.drawable.ic_avatar_surprised_48dp_22, 1}
                , new int[]{R.drawable.ic_avatar_surprised_48dp_23, 1}
                , new int[]{R.drawable.ic_avatar_surprised_48dp_24, 1}
                , new int[]{R.drawable.ic_avatar_surprised_48dp_25, 6});
    }
}
