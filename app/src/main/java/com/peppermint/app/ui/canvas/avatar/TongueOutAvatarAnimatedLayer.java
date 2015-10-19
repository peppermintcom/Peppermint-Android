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
public class TongueOutAvatarAnimatedLayer extends BitmapSequenceAnimatedLayer {
    public TongueOutAvatarAnimatedLayer(Context context, long duration, Paint paint) {
        super(context, duration, paint);

        setBitmapSequenceResourceIds(true, R.drawable.ic_avatar_wink_48dp_44,
                R.drawable.ic_avatar_wink_48dp_0,
                R.drawable.ic_avatar_wink_48dp_1,
                R.drawable.ic_avatar_wink_48dp_2,
                R.drawable.ic_avatar_wink_48dp_3,
                R.drawable.ic_avatar_wink_48dp_4,
                R.drawable.ic_avatar_wink_48dp_5,
                R.drawable.ic_avatar_wink_48dp_6,
                R.drawable.ic_avatar_wink_48dp_7,
                R.drawable.ic_avatar_wink_48dp_8,
                R.drawable.ic_avatar_wink_48dp_9,
                R.drawable.ic_avatar_wink_48dp_10,
                R.drawable.ic_avatar_wink_48dp_11,
                R.drawable.ic_avatar_wink_48dp_12,
                R.drawable.ic_avatar_wink_48dp_13,
                R.drawable.ic_avatar_wink_48dp_14,
                R.drawable.ic_avatar_wink_48dp_15,
                R.drawable.ic_avatar_wink_48dp_16,
                R.drawable.ic_avatar_wink_48dp_17,
                R.drawable.ic_avatar_wink_48dp_18,
                R.drawable.ic_avatar_wink_48dp_19,
                R.drawable.ic_avatar_wink_48dp_20,
                R.drawable.ic_avatar_wink_48dp_21,
                R.drawable.ic_avatar_wink_48dp_22,
                R.drawable.ic_avatar_wink_48dp_23,
                R.drawable.ic_avatar_wink_48dp_24,
                R.drawable.ic_avatar_wink_48dp_25,
                R.drawable.ic_avatar_wink_48dp_26,
                R.drawable.ic_avatar_wink_48dp_27,
                R.drawable.ic_avatar_wink_48dp_28,
                R.drawable.ic_avatar_wink_48dp_29,
                R.drawable.ic_avatar_wink_48dp_30,
                R.drawable.ic_avatar_wink_48dp_31,
                R.drawable.ic_avatar_wink_48dp_32,
                R.drawable.ic_avatar_wink_48dp_33,
                R.drawable.ic_avatar_wink_48dp_34,
                R.drawable.ic_avatar_wink_48dp_35,
                R.drawable.ic_avatar_wink_48dp_36,
                R.drawable.ic_avatar_wink_48dp_37,
                R.drawable.ic_avatar_wink_48dp_38,
                R.drawable.ic_avatar_wink_48dp_39,
                R.drawable.ic_avatar_wink_48dp_40,
                R.drawable.ic_avatar_wink_48dp_41,
                R.drawable.ic_avatar_wink_48dp_42,
                R.drawable.ic_avatar_wink_48dp_43,
                R.drawable.ic_avatar_wink_48dp_44);
    }
}
