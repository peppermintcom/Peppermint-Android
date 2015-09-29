package com.peppermint.app.ui.tutorial;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.peppermint.app.PeppermintApp;
import com.peppermint.app.R;
import com.peppermint.app.ui.views.PeppermintRecordView;

public class T2RecordFragment extends TutorialActivity.TutorialFragment {

    public T2RecordFragment() {
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        PeppermintApp app = (PeppermintApp) getActivity().getApplication();

        View v = inflater.inflate(R.layout.f_t2_record_layout, container, false);

        TextView txtSent = (TextView) v.findViewById(R.id.txtSent);
        txtSent.setTypeface(app.getFontSemibold());

        TextView txtRecordAndSendToEmail = (TextView) v.findViewById(R.id.txtRecordAndSendToEmail);
        txtRecordAndSendToEmail.setTypeface(app.getFontSemibold());

        PeppermintRecordView recordView = (PeppermintRecordView) v.findViewById(R.id.pmProgress);
        recordView.getMouthOpeningAnimation().setCurrentFrame(9);

        return v;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
