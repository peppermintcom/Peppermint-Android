package com.peppermint.app.ui.tutorial;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.peppermint.app.PeppermintApp;
import com.peppermint.app.R;
import com.peppermint.app.utils.Utils;

public class T1PickRecipientFragment extends TutorialActivity.TutorialFragment {

    public T1PickRecipientFragment() {
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        PeppermintApp app = (PeppermintApp) getActivity().getApplication();

        View v = inflater.inflate(R.layout.f_t1_pickrecipient_layout, container, false);

        TextView txtSearch = (TextView) v.findViewById(R.id.txtSearch);
        txtSearch.setTypeface(app.getFontBold());

        TextView txtSendAudioMessages = (TextView) v.findViewById(R.id.txtSendAudioMessages);
        txtSendAudioMessages.setTypeface(app.getFontSemibold());

        LinearLayout lytContacts = (LinearLayout) v.findViewById(R.id.lytContacts);

        String[] dummyNames = getResources().getStringArray(R.array.dummy_name_array);
        String[] dummyContacts = getResources().getStringArray(R.array.dummy_contact_array);
        String[] dummyAvatars = getResources().getStringArray(R.array.dummy_avatar_array);

        for(int i=0; i<dummyNames.length; i++) {
            int avatarResId = getResources().getIdentifier(dummyAvatars[i], "drawable", getActivity().getPackageName());
            LinearLayout contactView = getContactView(app, inflater, dummyNames[i], dummyContacts[i], avatarResId);
            lytContacts.addView(contactView);
        }

        return v;
    }

    private LinearLayout getContactView(PeppermintApp app, LayoutInflater inflater, String name, String contact, int avatarResId) {
        LinearLayout v = (LinearLayout) inflater.inflate(R.layout.f_t1_recipient_item_layout, null);

        TextView txtName = (TextView) v.findViewById(R.id.txtName);
        txtName.setTypeface(app.getFontBold());
        txtName.setText(name);

        TextView txtVia = (TextView) v.findViewById(R.id.txtVia);
        txtVia.setTypeface(app.getFontSemibold());

        TextView txtContact = (TextView) v.findViewById(R.id.txtContact);
        txtContact.setTypeface(app.getFontSemibold());
        txtContact.setText(contact);

        ImageView imgAvatar = (ImageView) v.findViewById(R.id.imgAvatar);
        imgAvatar.setImageResource(avatarResId);

        int margin = Utils.dpToPx(getActivity(), 4);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, margin);
        v.setLayoutParams(params);

        return v;
    }

}
