package com.peppermint.app.ui.settings;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.preference.Preference;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.crashlytics.android.Crashlytics;
import com.peppermint.app.PeppermintApp;
import com.peppermint.app.R;
import com.peppermint.app.utils.Utils;

/**
 * Created by Nuno Luz on 22-10-2015.
 */
public class CustomPreference extends Preference {

    private String mContent;
    private TextView mTxtContent;

    public CustomPreference(Context context) {
        super(context);
        setLayoutResource(R.layout.v_preference);
    }

    public CustomPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.v_preference);
    }

    public CustomPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setLayoutResource(R.layout.v_preference);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public CustomPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setLayoutResource(R.layout.v_preference);
    }

    @Override
    protected View onCreateView(ViewGroup parent) {
        ViewGroup v = (ViewGroup) super.onCreateView(parent);
        mTxtContent = (TextView) v.findViewById(R.id.content);
        return v;
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);

        mTxtContent = (TextView) view.findViewById(R.id.content);

        TextView txtTitle = (TextView) view.findViewById(android.R.id.title);
        TextView txtSummary = (TextView) view.findViewById(android.R.id.summary);
        TextView txtContent = (TextView) view.findViewById(R.id.content);

        if(mContent == null) {
            txtContent.setVisibility(View.GONE);
        } else {
            txtContent.setText(mContent);
        }

        txtTitle.setTextColor(Utils.getColor(getContext(), R.color.black));
        txtSummary.setTextColor(Utils.getColor(getContext(), R.color.dark_grey_text));
        txtContent.setTextColor(Utils.getColor(getContext(), R.color.black));

        PeppermintApp app = (PeppermintApp) ((Activity) getContext()).getApplication();
        txtTitle.setTypeface(app.getFontSemibold());
        txtSummary.setTypeface(app.getFontRegular());
        txtContent.setTypeface(app.getFontRegular());

        try {
            txtTitle.setTextColor(ContextCompat.getColorStateList(getContext(), R.color.color_black_to_darkgrey_disabled));
        } catch (Exception e) {
            Crashlytics.logException(e);
            txtTitle.setTextColor(Utils.getColor(getContext(), R.color.black));
        }

        try {
            txtContent.setTextColor(ContextCompat.getColorStateList(getContext(), R.color.color_black_to_darkgrey_disabled));
        } catch (Exception e) {
            Crashlytics.logException(e);
            txtContent.setTextColor(Utils.getColor(getContext(), R.color.black));
        }

        try {
            txtSummary.setTextColor(ContextCompat.getColorStateList(getContext(), R.color.color_darkgrey_to_grey_disabled));
        } catch (Exception e) {
            Crashlytics.logException(e);
            txtSummary.setTextColor(Utils.getColor(getContext(), R.color.dark_grey_text));
        }
    }

    public void setContent(String value) {
        this.mContent = value;
        if(mTxtContent != null) {
            if(mContent == null) {
                mTxtContent.setVisibility(View.GONE);
            } else {
                mTxtContent.setText(mContent);
                mTxtContent.setVisibility(View.VISIBLE);
            }
        }
    }
}
