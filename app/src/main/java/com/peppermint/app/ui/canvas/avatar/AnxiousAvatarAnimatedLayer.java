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
public class AnxiousAvatarAnimatedLayer extends BitmapSequenceAnimatedLayer {
    public AnxiousAvatarAnimatedLayer(Context context, long duration, Paint paint) {
        super(context, duration, paint);

        setBitmapSequenceResourceIds(true, new int[]{R.drawable.ic_avatar_anxious_48dp_0, 1}
                , new int[]{R.drawable.ic_avatar_anxious_48dp_1, 1}
                , new int[]{R.drawable.ic_avatar_anxious_48dp_2, 1}
                , new int[]{R.drawable.ic_avatar_anxious_48dp_3, 1}
                , new int[]{R.drawable.ic_avatar_anxious_48dp_4, 1}
                , new int[]{R.drawable.ic_avatar_anxious_48dp_5, 1}
                , new int[]{R.drawable.ic_avatar_anxious_48dp_6, 1}
                , new int[]{R.drawable.ic_avatar_anxious_48dp_7, 1}
                , new int[]{R.drawable.ic_avatar_anxious_48dp_8, 12}
                , new int[]{R.drawable.ic_avatar_anxious_48dp_20, 1}
                , new int[]{R.drawable.ic_avatar_anxious_48dp_21, 1}
                , new int[]{R.drawable.ic_avatar_anxious_48dp_22, 1}
                , new int[]{R.drawable.ic_avatar_anxious_48dp_23, 1}
                , new int[]{R.drawable.ic_avatar_anxious_48dp_24, 1}
                , new int[]{R.drawable.ic_avatar_anxious_48dp_25, 1}
                , new int[]{R.drawable.ic_avatar_anxious_48dp_26, 5});
    }
}
