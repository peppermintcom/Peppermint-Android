package com.peppermint.app.ui.settings;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import com.peppermint.app.PeppermintApp;

/**
 * Created by Nuno Luz on 22-10-2015.
 */
public class GmailAccountPreference extends Preference {

    public GmailAccountPreference(Context context) {
        super(context);
    }

    public GmailAccountPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public GmailAccountPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public GmailAccountPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);

        TextView txtTitle = (TextView) view.findViewById(android.R.id.title);
        TextView txtSummary = (TextView) view.findViewById(android.R.id.summary);

        PeppermintApp app = (PeppermintApp) ((Activity) getContext()).getApplication();
        txtTitle.setTypeface(app.getFontSemibold());
        txtSummary.setTypeface(app.getFontRegular());
    }
}
