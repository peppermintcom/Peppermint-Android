package com.peppermint.app.ui.about;

import android.app.Fragment;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.peppermint.app.BuildConfig;
import com.peppermint.app.R;

/**
 * Created by Nuno Luz on 06-01-2016.
 *
 * About fragment.
 */
public class AboutFragment extends Fragment implements View.OnClickListener {

    private static final String PRIVACY_URL = "http://peppermint.com/privacy";

    private TextView mTxtPrivacy;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // inflate the view
        View v = inflater.inflate(R.layout.f_about, container, false);

        // tap privacy listener
        mTxtPrivacy = (TextView) v.findViewById(R.id.txtPrivacy);
        mTxtPrivacy.setOnClickListener(this);

        // set the version text
        TextView txtVersion = (TextView) v.findViewById(R.id.txtVersion);
        txtVersion.setText(getString(R.string.version) + " " + BuildConfig.VERSION_NAME);

        return v;
    }

    @Override
    public void onClick(View v) {
        // launch browser with privacy url
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(PRIVACY_URL));
        startActivity(intent);
    }

}
