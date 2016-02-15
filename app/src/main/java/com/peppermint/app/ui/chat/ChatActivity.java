package com.peppermint.app.ui.chat;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import com.peppermint.app.PeppermintApp;
import com.peppermint.app.R;
import com.peppermint.app.ui.CustomActionBarActivity;
import com.peppermint.app.ui.views.NavigationItem;
import com.peppermint.app.ui.views.RoundImageView;
import com.peppermint.app.ui.views.simple.CustomFontTextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Nuno Luz on 10-11-2015.
 *
 * Activity for user authentication.
 */
public class ChatActivity extends CustomActionBarActivity {

    // UI
    private CustomFontTextView mTxtChatName, mTxtChatVia;
    private RoundImageView mImgAvatar;

    @Override
    protected List<NavigationItem> getNavigationItems() {
        final List<NavigationItem> navItems = new ArrayList<>();
        navItems.add(new NavigationItem("Chat", R.drawable.ic_settings_36dp, ChatFragment.class, false, false));
        return navItems;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PeppermintApp app = (PeppermintApp) getApplication();

        // inflate custom action bar
        View v = getLayoutInflater().inflate(R.layout.v_chat_actionbar, null, false);

        mTxtChatName = (CustomFontTextView) v.findViewById(R.id.txtChatName);
        mTxtChatVia = (CustomFontTextView) v.findViewById(R.id.txtChatVia);
        mImgAvatar = (RoundImageView) v.findViewById(R.id.imgChatAvatar);

        getCustomActionBar().setContents(v, false);
    }

    protected void setActionBarData(String recipientName, String recipientVia, String recipientPhotoUri) {
        mTxtChatName.setText(recipientName);
        mTxtChatVia.setText(recipientVia);
        if(recipientPhotoUri == null) {
            mImgAvatar.setImageResource(R.drawable.ic_anonymous_gray_35dp);
        } else {
            mImgAvatar.setImageURI(Uri.parse(recipientPhotoUri));
        }
    }

}
