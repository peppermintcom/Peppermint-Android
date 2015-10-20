package com.peppermint.app.ui.views;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.os.Build;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.peppermint.app.R;
import com.peppermint.app.ui.canvas.progress.RecordProgressBarsView;

/**
 * Created by Nuno Luz on 16-10-2015.
 *
 * The recording overlay that shows up while recording.
 *
 */
public class RecordingOverlayView extends FrameLayout {

    private TextView mTxtRecordingFor, mTxtVia, mTxtDuration, mTxtSwipe;
    private RecordProgressBarsView mRecordView;
    private float mMillis;

    public RecordingOverlayView(Context context) {
        super(context);
        init(null);
    }

    public RecordingOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public RecordingOverlayView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public RecordingOverlayView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(attrs);
    }

    private void init(AttributeSet attrs) {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        removeAllViews();
        View contentView = inflater.inflate(R.layout.v_recording_layout, null);
        addView(contentView, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        String textFont = "fonts/OpenSans-Semibold.ttf";

        if(attrs != null) {
            TypedArray a = getContext().getTheme().obtainStyledAttributes(
                    attrs,
                    R.styleable.PeppermintView,
                    0, 0);
            try {
                String aTextFont = a.getString(R.styleable.PeppermintView_textFont);
                if(aTextFont != null) {
                    textFont = aTextFont;
                }
            } finally {
                a.recycle();
            }
        }

        Typeface typeface = Typeface.createFromAsset(getContext().getAssets(), textFont);

        mRecordView = (RecordProgressBarsView) findViewById(R.id.cvRecord);

        mTxtRecordingFor = (TextView) findViewById(R.id.txtRecordingFor);
        mTxtVia = (TextView) findViewById(R.id.txtVia);
        mTxtSwipe = (TextView) findViewById(R.id.txtSwipe);
        mTxtDuration = (TextView) findViewById(R.id.txtDuration);

        mTxtRecordingFor.setTypeface(typeface);
        mTxtVia.setTypeface(typeface);
        mTxtSwipe.setTypeface(typeface);
        mTxtDuration.setTypeface(typeface);
    }

    public void start() {
        mRecordView.startAnimations();
        mRecordView.startDrawingThread();
    }

    public void stop() {
        mRecordView.stopDrawingThread();
        mRecordView.stopAnimations();
    }

    public void blinkLeft() {
        mRecordView.blinkLeftEye();
    }

    public void setMillis(float millis) {
        this.mMillis = millis;
        float duration = millis / 1000f;
        //mRecordView.setSeconds(duration);

        long mins = (long) (duration / 60);
        long secs = (long) (duration % 60);
        long hours = mins / 60;
        if(hours > 0) {
            mins = mins % 60;
        }

        mTxtDuration.setText((hours > 0 ? hours + ":" : "") + mins + ":" + (secs < 10 ? "0" : "") + secs);
    }

    /*public float getSeconds() {
        return mRecordView.getSeconds();
    }*/

    public float getMillis() {
        return mMillis;
    }

    public void setName(String name) {
        mTxtRecordingFor.setText(String.format(getContext().getString(R.string.recording_for), name));
    }

    public void setVia(String via) {
        mTxtVia.setText(via);
    }

}
