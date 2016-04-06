package com.peppermint.app.ui.about;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.peppermint.app.BuildConfig;
import com.peppermint.app.R;
import com.peppermint.app.ui.base.CustomActionBarView;
import com.peppermint.app.ui.base.activities.CustomActionBarActivity;

/**
 * Created by Nuno Luz on 06-01-2016.
 *
 * About Activity.
 */
public class AboutActivity extends CustomActionBarActivity implements View.OnClickListener {

    private static final String PRIVACY_URL = "http://peppermint.com/privacy";

    @Override
    protected final int getContainerViewLayoutId() {
        return R.layout.f_about;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final CustomActionBarView actionBarView = getCustomActionBar();
        if(actionBarView != null) {
            actionBarView.setTitle(getString(R.string.about));
        }

        // tap privacy listener
        final TextView txtPrivacy = (TextView) findViewById(R.id.txtPrivacy);
        txtPrivacy.setOnClickListener(this);

        // set the version text
        final TextView txtVersion = (TextView) findViewById(R.id.txtVersion);
        txtVersion.setText(getString(R.string.version) + " " + BuildConfig.VERSION_NAME);
    }

    @Override
    public void onClick(View v) {
        // launch browser with privacy url
        final Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(PRIVACY_URL));
        startActivity(intent);
    }
}
