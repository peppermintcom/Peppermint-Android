package com.peppermint.app.ui.canvas.avatar;

import android.content.Context;
import android.graphics.Paint;

import com.peppermint.app.R;
import com.peppermint.app.data.ContactRaw;
import com.peppermint.app.ui.canvas.BitmapSequenceAnimatedLayer;

/**
 * Created by Nuno Luz on 06-10-2015.
 *
 * An animated layer that represents an anonymous {@link ContactRaw}'s avatar.
 */
public class WinkAvatarAnimatedLayer extends BitmapSequenceAnimatedLayer {
    public WinkAvatarAnimatedLayer(Context context, long duration, Paint paint) {
        super(context, duration, paint);

        setBitmapSequenceResourceIds(true, new int[]{R.drawable.ic_avatar_wink_48dp_0, 1}
                , new int[]{R.drawable.ic_avatar_wink_48dp_1, 1}
                , new int[]{R.drawable.ic_avatar_wink_48dp_2, 1}
                , new int[]{R.drawable.ic_avatar_wink_48dp_3, 1}
                , new int[]{R.drawable.ic_avatar_wink_48dp_4, 1}
                , new int[]{R.drawable.ic_avatar_wink_48dp_5, 1}
                , new int[]{R.drawable.ic_avatar_wink_48dp_6, 1}
                , new int[]{R.drawable.ic_avatar_wink_48dp_7, 1}
                , new int[]{R.drawable.ic_avatar_wink_48dp_8, 1}
                , new int[]{R.drawable.ic_avatar_wink_48dp_9, 1}
                , new int[]{R.drawable.ic_avatar_wink_48dp_10, 1}
                , new int[]{R.drawable.ic_avatar_wink_48dp_11, 1}
                , new int[]{R.drawable.ic_avatar_wink_48dp_12, 1}
                , new int[]{R.drawable.ic_avatar_wink_48dp_13, 1}
                , new int[]{R.drawable.ic_avatar_wink_48dp_14, 1}
                , new int[]{R.drawable.ic_avatar_wink_48dp_15, 1}
                , new int[]{R.drawable.ic_avatar_wink_48dp_16, 1}
                , new int[]{R.drawable.ic_avatar_wink_48dp_17, 1}
                , new int[]{R.drawable.ic_avatar_wink_48dp_18, 1}
                , new int[]{R.drawable.ic_avatar_wink_48dp_19, 1}
                , new int[]{R.drawable.ic_avatar_wink_48dp_20, 1}
                , new int[]{R.drawable.ic_avatar_wink_48dp_21, 1}
                , new int[]{R.drawable.ic_avatar_wink_48dp_22, 1}
                , new int[]{R.drawable.ic_avatar_wink_48dp_23, 1}
                , new int[]{R.drawable.ic_avatar_wink_48dp_24, 1}
                , new int[]{R.drawable.ic_avatar_wink_48dp_25, 1}
                , new int[]{R.drawable.ic_avatar_wink_48dp_26, 1}
                , new int[]{R.drawable.ic_avatar_wink_48dp_27, 1}
                , new int[]{R.drawable.ic_avatar_wink_48dp_28, 1}
                , new int[]{R.drawable.ic_avatar_wink_48dp_29, 1}
                , new int[]{R.drawable.ic_avatar_wink_48dp_30, 1}
                , new int[]{R.drawable.ic_avatar_wink_48dp_31, 1}
                , new int[]{R.drawable.ic_avatar_wink_48dp_32, 1}
                , new int[]{R.drawable.ic_avatar_wink_48dp_33, 1}
                , new int[]{R.drawable.ic_avatar_wink_48dp_34, 1}
                , new int[]{R.drawable.ic_avatar_wink_48dp_35, 1}
                , new int[]{R.drawable.ic_avatar_wink_48dp_36, 1}
                , new int[]{R.drawable.ic_avatar_wink_48dp_37, 1}
                , new int[]{R.drawable.ic_avatar_wink_48dp_38, 7});
    }
}
