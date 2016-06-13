package com.peppermint.app.ui.chat;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import com.peppermint.app.R;
import com.peppermint.app.trackers.TrackerManager;
import com.peppermint.app.ui.base.activities.CustomActionBarActivity;
import com.peppermint.app.ui.base.views.CustomActionBarView;
import com.peppermint.app.ui.base.views.CustomFontTextView;
import com.peppermint.app.ui.base.views.RoundImageView;
import com.peppermint.app.utils.ResourceUtils;

/**
 * Created by Nuno Luz on 10-11-2015.
 *
 * Activity for chats (lists of messages).
 */
public class ChatActivity extends CustomActionBarActivity implements RecipientDataGUI {

    private static final String TAG = ChatActivity.class.getSimpleName();

    public static final String PARAM_CHAT_ID = TAG + "_paramChatId";

    // ui
    private CustomFontTextView mTxtChatName, mTxtChatVia;
    private RoundImageView mImgAvatar;

    private ChatController mChatController;

    @Override
    protected final int getContainerViewLayoutId() {
        return R.layout.f_chat;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final CustomActionBarView actionBarView = getCustomActionBar();
        if(actionBarView != null) {
            // inflate custom action bar
            final View v = getLayoutInflater().inflate(R.layout.v_chat_actionbar, null, false);

            // custom action bar views
            mTxtChatName = (CustomFontTextView) v.findViewById(R.id.txtChatName);
            mTxtChatVia = (CustomFontTextView) v.findViewById(R.id.txtChatVia);
            mImgAvatar = (RoundImageView) v.findViewById(R.id.imgChatAvatar);
            mImgAvatar.setFallbackImageDrawable(ResourceUtils.getDrawable(this, R.drawable.ic_anonymous_gray_35dp));

            actionBarView.setContents(v, false);
        }

        // get arguments
        long chatId = 0;
        final Intent paramIntent = getIntent();

        if(paramIntent != null) {
            chatId = paramIntent.getLongExtra(PARAM_CHAT_ID, 0);
        }

        if(chatId <= 0) {
            TrackerManager.getInstance(getApplicationContext()).logException(new RuntimeException("ContactRaw is null on ChatActivity intent!"));
            finish();
            return;
        }

        mChatController = new ChatController(this, this);
        mChatController.setChat(chatId);
        mChatController.init(getContainerView(), mOverlayManager, this, savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        mChatController.saveInstanceState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onStart() {
        super.onStart();
        getAuthenticationData(getIntentReplica());
        mChatController.start();
    }

    @Override
    public void onStop() {
        mChatController.stopPlayer();
        mChatController.stop();
        super.onStop();
    }

    @Override
    public void onDestroy() {
        if(mChatController != null) {
            mChatController.deinit();
            mChatController.setContext(null);
        }
        super.onDestroy();
    }

    @Override
    public void setRecipientData(String recipientName, String recipientVia, String recipientPhotoUri) {
        mTxtChatName.setText(recipientName);
        mTxtChatVia.setText(recipientVia);

        if(recipientPhotoUri != null) {
            mImgAvatar.setImageDrawable(ResourceUtils.getDrawableFromUri(this, Uri.parse(recipientPhotoUri)));
        } else {
            mImgAvatar.setImageDrawable(null);
        }
    }

}
