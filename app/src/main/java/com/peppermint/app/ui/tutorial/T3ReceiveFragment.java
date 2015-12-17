package com.peppermint.app.ui.tutorial;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.peppermint.app.PeppermintApp;
import com.peppermint.app.R;
import com.peppermint.app.tracking.TrackerManager;

public class T3ReceiveFragment extends TutorialActivity.TutorialFragment {

    private static final String SCREEN_ID = "Tutorial-S3-ReceiveMessage";

    public T3ReceiveFragment() {
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        PeppermintApp app = (PeppermintApp) getActivity().getApplication();

        View v = inflater.inflate(R.layout.f_t3_receive_layout, container, false);

        TextView txtYourFriendDoesntNeedPeppermint = (TextView) v.findViewById(R.id.txtYourFriendDoesntNeedPeppermint);
        txtYourFriendDoesntNeedPeppermint.setTypeface(app.getFontSemibold());

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        TrackerManager.getInstance(getActivity().getApplicationContext()).trackScreenView(SCREEN_ID);
    }
}
