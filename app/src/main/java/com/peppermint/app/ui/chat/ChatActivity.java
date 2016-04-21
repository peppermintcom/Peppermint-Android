package com.peppermint.app.ui.chat;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import com.peppermint.app.R;
import com.peppermint.app.tracking.TrackerManager;
import com.peppermint.app.ui.base.CustomActionBarView;
import com.peppermint.app.ui.base.activities.CustomActionBarActivity;
import com.peppermint.app.ui.base.views.CustomFontTextView;
import com.peppermint.app.ui.base.views.RoundImageView;
import com.peppermint.app.ui.chat.recorder.ChatRecordOverlayController;
import com.peppermint.app.utils.ResourceUtils;

/**
 * Created by Nuno Luz on 10-11-2015.
 *
 * Activity for user authentication.
 */
public class ChatActivity extends CustomActionBarActivity implements RecipientDataGUI {

    private static final String TAG = ChatActivity.class.getSimpleName();

    private static final int REQUEST_NEWCONTACT_AND_SEND = 223;

    public static final String PARAM_AUTO_PLAY_MESSAGE_ID = TAG + "_paramAutoPlayMessageId";
    public static final String PARAM_CHAT_ID = TAG + "_paramChatId";

    // UI
    private CustomFontTextView mTxtChatName, mTxtChatVia;
    private RoundImageView mImgAvatar;

    private ChatController mChatController;
    private ChatRecordOverlayController.Callbacks mChatControllerCallbacks = new ChatRecordOverlayController.Callbacks() {
        @Override
        public void onNewContact(Intent intentToLaunchActivity) {
            startActivityForResult(intentToLaunchActivity, REQUEST_NEWCONTACT_AND_SEND);
        }
    };

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
            View v = getLayoutInflater().inflate(R.layout.v_chat_actionbar, null, false);

            // custom action bar views
            mTxtChatName = (CustomFontTextView) v.findViewById(R.id.txtChatName);
            mTxtChatVia = (CustomFontTextView) v.findViewById(R.id.txtChatVia);
            mImgAvatar = (RoundImageView) v.findViewById(R.id.imgChatAvatar);
            mImgAvatar.setFallbackImageDrawable(ResourceUtils.getDrawable(this, R.drawable.ic_anonymous_gray_35dp));

            actionBarView.setContents(v, false);
        }

        // get arguments
        long chatId = 0;
        long autoPlayMessageId = 0;
        Intent paramIntent = getIntent();

        if(paramIntent != null) {
            chatId = paramIntent.getLongExtra(PARAM_CHAT_ID, 0);
            // only auto-play the first time (not when there's an orientation change)
            autoPlayMessageId = savedInstanceState == null ? paramIntent.getLongExtra(PARAM_AUTO_PLAY_MESSAGE_ID, 0) : 0;
        }

        if(chatId <= 0) {
            TrackerManager.getInstance(getApplicationContext()).logException(new RuntimeException("ContactRaw is null on ChatActivity intent!"));
            finish();
            return;
        }

        mChatController = new ChatController(this, this, mChatControllerCallbacks);
        mChatController.setChat(chatId);
        mChatController.setAutoPlayMessageId(autoPlayMessageId);
        mChatController.init(getContainerView(), mOverlayManager, this, mAuthenticationPolicyEnforcer, savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        mChatController.saveInstanceState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onStart() {
        super.onStart();
        mChatController.start();
    }

    @Override
    public void onStop() {
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
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQUEST_NEWCONTACT_AND_SEND) {
            mChatController.handleNewContactResult(resultCode, data);
        }
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
