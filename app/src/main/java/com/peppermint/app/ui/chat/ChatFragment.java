package com.peppermint.app.ui.chat;

import android.app.Activity;
import android.app.ListFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.RelativeLayout;

import com.peppermint.app.MessagesServiceManager;
import com.peppermint.app.R;
import com.peppermint.app.data.Chat;
import com.peppermint.app.data.Recipient;
import com.peppermint.app.data.Recording;
import com.peppermint.app.sending.ReceiverEvent;
import com.peppermint.app.sending.SenderEvent;
import com.peppermint.app.tracking.TrackerManager;
import com.peppermint.app.utils.Utils;

/**
 * Created by Nuno Luz on 10-11-2015.
 *
 * New recipient/contact fragment.
 */
public class ChatFragment extends ListFragment implements View.OnClickListener, View.OnLongClickListener {

    private static final String TAG = ChatFragment.class.getSimpleName();

    public static final String PARAM_RECIPIENT = TAG + "_paramRecipient";
    public static final String PARAM_CHAT = TAG + "_paramChat";
    public static final String PARAM_RECORDING = TAG + "_paramRecording";

    private static final String SCREEN_ID = "Chat";

    // GENERIC
    private ChatActivity mActivity;
    private MessagesServiceManager mMessagesServiceManager;

    // UI
    private RelativeLayout mRecordLayout;

    // DATA
    private Chat mChat;
    private Recipient mRecipient;

    @SuppressWarnings("deprecation")
    @Override
    public void onAttach(Activity context) {
        super.onAttach(context);
        mActivity = (ChatActivity) context;
        mMessagesServiceManager = new MessagesServiceManager(mActivity);
        mMessagesServiceManager.setServiceListener(mServiceListener);
        mMessagesServiceManager.setSenderListener(mSenderListener);
        mMessagesServiceManager.setReceiverListener(mReceiverListener);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.f_chat, null);

        mRecordLayout = (RelativeLayout) v.findViewById(R.id.lytRecord);
        mRecordLayout.setOnClickListener(this);
        mRecordLayout.setOnLongClickListener(this);

        // get arguments
        Recording argRecording = null;
        Bundle args = getArguments();
        if(args != null) {
            mRecipient = (Recipient) args.getSerializable(PARAM_RECIPIENT);
            argRecording = (Recording) args.getSerializable(PARAM_RECORDING);
            mChat = (Chat) args.getSerializable(PARAM_CHAT);

            if(mRecipient == null) {
                mActivity.getTrackerManager().logException(new RuntimeException("Recipient is null on ChatActivity intent!"));
                mActivity.finish();
                return null;
            }

            if(mChat == null) {
                mChat = new Chat(mRecipient, Utils.getCurrentTimestamp());
            }

            mActivity.setActionBarData(mRecipient.getName(), mRecipient.getVia(), mRecipient.getPhotoUri());
        }

        if(savedInstanceState != null) {
        }

        mMessagesServiceManager.startAndBind();

        if(argRecording != null) {
            // send recording
            mMessagesServiceManager.send(mChat, mRecipient, argRecording);
        }

        return v;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onResume() {
        super.onResume();
        Utils.showKeyboard(mActivity, WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        TrackerManager.getInstance(getActivity().getApplicationContext()).trackScreenView(SCREEN_ID);
    }

    @Override
    public void onPause() {
        Utils.hideKeyboard(mActivity, WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        super.onPause();
    }

    @Override
    public void onClick(View v) {

    }

    @Override
    public boolean onLongClick(View v) {
        return false;
    }

    private MessagesServiceManager.SenderListener mSenderListener = new MessagesServiceManager.SenderListener() {
        @Override
        public void onSendStarted(SenderEvent event) {
            // add to UI
        }

        @Override
        public void onSendCancelled(SenderEvent event) {

        }

        @Override
        public void onSendError(SenderEvent event) {

        }

        @Override
        public void onSendFinished(SenderEvent event) {

        }

        @Override
        public void onSendProgress(SenderEvent event) {

        }

        @Override
        public void onSendQueued(SenderEvent event) {

        }
    };

    private MessagesServiceManager.ReceiverListener mReceiverListener = new MessagesServiceManager.ReceiverListener() {
        @Override
        public void onReceivedMessage(ReceiverEvent event) {

        }
    };

    private MessagesServiceManager.ServiceListener mServiceListener = new MessagesServiceManager.ServiceListener() {
        @Override
        public void onBoundSendService() {

        }
    };
}
