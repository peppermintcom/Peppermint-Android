package com.peppermint.app.ui.canvas;

import android.content.Context;
import android.graphics.Rect;

/**
 * Created by Nuno Luz on 30-09-2015.
 *
 * Abstract static layer implementation.
 */
public abstract class LayerBase implements Layer {

    private Rect mBounds;
    private Context mContext;

    public LayerBase(Context context) {
        this.mContext = context;
    }

    @Override
    public Rect getBounds() {
        return mBounds;
    }

    /**
     * Set the bounds of this layer. Triggers {@link #onMeasure(Rect)}.
     * @param bounds the new bounds
     */
    @Override
    public void setBounds(Rect bounds) {
        this.mBounds = bounds;
        onMeasure(bounds);
    }

    protected void onMeasure(Rect bounds) {
        // nothing to do here
    }

    public Context getContext() {
        return mContext;
    }

}
