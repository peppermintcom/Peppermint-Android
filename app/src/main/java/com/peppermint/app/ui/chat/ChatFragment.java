package com.peppermint.app.ui.chat;

import android.app.Activity;
import android.app.ListFragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.peppermint.app.R;
import com.peppermint.app.tracking.TrackerManager;
import com.peppermint.app.ui.CustomActionBarActivity;
import com.peppermint.app.ui.chat.recorder.ChatRecordOverlayController;

/**
 * Created by Nuno Luz on 10-11-2015.
 *
 * New recipient/contact fragment.
 */
public class ChatFragment extends ListFragment implements ChatRecordOverlayController.Callbacks {

    public static final int REQUEST_NEWCONTACT_AND_SEND = 223;

    private static final String TAG = ChatFragment.class.getSimpleName();

    public static final String PARAM_AUTO_PLAY_MESSAGE_ID = TAG + "_paramAutoPlayMessageId";
    public static final String PARAM_CHAT_ID = TAG + "_paramChatId";

    private CustomActionBarActivity mActivity;
    private ChatController mController;

    @SuppressWarnings("deprecation")
    @Override
    public void onAttach(Activity context) {
        super.onAttach(context);
        mActivity = (CustomActionBarActivity) context;
        mController = new ChatController(context, (RecipientDataGUI) context, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.f_chat, null);

        // get arguments
        Bundle args = getArguments();
        long chatId = 0;
        long autoPlayMessageId = 0;
        if(args != null) {
            chatId = args.getLong(PARAM_CHAT_ID, 0);
            autoPlayMessageId = args.getLong(PARAM_AUTO_PLAY_MESSAGE_ID, 0);
        }

        if(chatId <= 0) {
            TrackerManager.getInstance(mActivity.getApplicationContext()).logException(new RuntimeException("ContactRaw is null on ChatActivity intent!"));
            mActivity.finish();
            return null;
        }

        mController.setChat(chatId);
        mController.setAutoPlayMessageId(autoPlayMessageId);
        mController.init(v, mActivity.getOverlayManager(), mActivity, mActivity.getAuthenticationPolicyEnforcer(), savedInstanceState);

        return v;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        mController.saveInstanceState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onStart() {
        super.onStart();
        mController.start();
    }

    @Override
    public void onStop() {
        mController.stop();
        super.onStop();
    }

    @Override
    public void onDestroy() {
        mController.deinit();
        mController.setContext(null);
        mActivity = null;
        super.onDestroy();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQUEST_NEWCONTACT_AND_SEND) {
            mController.handleNewContactResult(resultCode, data);
        }
    }

    @Override
    public void onNewContact(Intent intentToLaunchActivity) {
        startActivityForResult(intentToLaunchActivity, REQUEST_NEWCONTACT_AND_SEND);
    }
}
