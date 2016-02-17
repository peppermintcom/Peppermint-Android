package com.peppermint.app.ui.chat;

import android.app.Activity;
import android.app.ListFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.View;
import android.widget.Toast;

import com.peppermint.app.PlayerEvent;
import com.peppermint.app.PlayerServiceManager;
import com.peppermint.app.R;
import com.peppermint.app.RecordService;
import com.peppermint.app.authenticator.AuthenticationData;
import com.peppermint.app.cloud.MessagesServiceManager;
import com.peppermint.app.cloud.ReceiverEvent;
import com.peppermint.app.cloud.senders.SenderEvent;
import com.peppermint.app.cloud.senders.SenderPreferences;
import com.peppermint.app.data.Chat;
import com.peppermint.app.data.Recipient;
import com.peppermint.app.data.Recording;
import com.peppermint.app.ui.CustomActionBarActivity;
import com.peppermint.app.ui.recipients.RecipientAdapterUtils;
import com.peppermint.app.ui.recipients.add.NewRecipientActivity;
import com.peppermint.app.ui.recipients.add.NewRecipientFragment;
import com.peppermint.app.ui.views.dialogs.CustomConfirmationDialog;
import com.peppermint.app.ui.views.simple.CustomToast;
import com.peppermint.app.utils.Utils;

public class ChatRecordOverlayFragment extends ListFragment implements ChatRecordOverlay.OnRecordingFinishedCallback, MessagesServiceManager.ReceiverListener, MessagesServiceManager.SenderListener, MessagesServiceManager.ServiceListener, PlayerServiceManager.PlayerListener, PlayerServiceManager.PlayServiceListener {

    public static final int REQUEST_NEWCONTACT_AND_SEND = 223;

    // keys to save the instance state
    private static final String RECIPIENT_TAPPED_KEY = "RecipientsFragment_RecipientTapped";
    private static final String SAVED_DIALOG_STATE_KEY = "RecipientsFragment_SmsDialogState";
    private static final String RECORDING_FINAL_EVENT_KEY = "RecipientsFragment_RecordingFinalEvent";
    private static final String SMS_CONFIRMATION_STATE_KEY = "RecipientsFragment_SmsConfirmationState";

    private CustomActionBarActivity mActivity;
    private SenderPreferences mPreferences;
    private MessagesServiceManager mMessagesServiceManager;
    private PlayerServiceManager mPlayerServiceManager;

    private ChatRecordOverlay mChatRecordOverlay;

    private SMSConfirmationDialog mSmsConfirmationDialog;
    private CustomConfirmationDialog mSmsAddContactDialog;

    private Recipient mRecipient;
    private RecordService.Event mFinalEvent;

    @SuppressWarnings("deprecation")
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mActivity = (CustomActionBarActivity) activity;
        mPreferences = new SenderPreferences(activity);

        // send through SMS confirmation dialog
        mSmsConfirmationDialog = new SMSConfirmationDialog(mActivity);
        mSmsConfirmationDialog.setTitleText(R.string.sending_via_sms);
        mSmsConfirmationDialog.setMessageText(R.string.when_you_send_via_sms);
        mSmsConfirmationDialog.setCheckText(R.string.do_not_show_this_again);
        mSmsConfirmationDialog.setNegativeButtonListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mFinalEvent = null;
                mSmsConfirmationDialog.dismiss();
            }
        });
        mSmsConfirmationDialog.setPositiveButtonListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage(mFinalEvent.getRecipient(), mFinalEvent.getRecording());
                mFinalEvent = null;
                mSmsConfirmationDialog.dismiss();
            }
        });
        mSmsConfirmationDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                mSmsConfirmationDialog.setEmailRecipient(null);
                if (mSmsConfirmationDialog.isChecked()) {
                    mPreferences.setShownSmsConfirmation(true);
                }
            }
        });
        mSmsConfirmationDialog.setEmailClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Recipient emailRecipient = mSmsConfirmationDialog.getEmailRecipient();
                if (emailRecipient != null) {
                    sendMessage(emailRecipient, mFinalEvent.getRecording());
                    mFinalEvent = null;
                    mSmsConfirmationDialog.dismiss();
                } else {
                    launchNewRecipientActivity(mFinalEvent.getRecipient(), REQUEST_NEWCONTACT_AND_SEND);
                }
            }
        });

        // dialog for unsupported SMS
        mSmsAddContactDialog = new CustomConfirmationDialog(mActivity);
        mSmsAddContactDialog.setTitleText(R.string.sending_via_sms);
        mSmsAddContactDialog.setMessageText(R.string.msg_sms_disabled_add_contact);
        mSmsAddContactDialog.setPositiveButtonListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchNewRecipientActivity(mRecipient, REQUEST_NEWCONTACT_AND_SEND);
                mSmsAddContactDialog.dismiss();
            }
        });
        mSmsAddContactDialog.setNegativeButtonListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSmsAddContactDialog.dismiss();
            }
        });

        mMessagesServiceManager = new MessagesServiceManager(mActivity);
        mMessagesServiceManager.addServiceListener(this);
        mMessagesServiceManager.addSenderListener(this);
        mMessagesServiceManager.addReceiverListener(this);

        mPlayerServiceManager = new PlayerServiceManager(mActivity);
        mPlayerServiceManager.addServiceListener(this);
        mPlayerServiceManager.addPlayerListener(this);
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mMessagesServiceManager.start();
        mPlayerServiceManager.start();

        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(RECORDING_FINAL_EVENT_KEY)) {
                mFinalEvent = (RecordService.Event) savedInstanceState.getSerializable(RECORDING_FINAL_EVENT_KEY);
            }

            Bundle smsDialogState = savedInstanceState.getBundle(SMS_CONFIRMATION_STATE_KEY);
            if (smsDialogState != null) {
                mSmsConfirmationDialog.onRestoreInstanceState(smsDialogState);
            }

            mRecipient = (Recipient) savedInstanceState.getSerializable(RECIPIENT_TAPPED_KEY);

            Bundle dialogState = savedInstanceState.getBundle(SAVED_DIALOG_STATE_KEY);
            if (dialogState != null) {
                mSmsAddContactDialog.onRestoreInstanceState(dialogState);
            }
        }

        mChatRecordOverlay = (ChatRecordOverlay) mActivity.getOverlayManager().createOverlay(new ChatRecordOverlay(mActivity, null, view));
        mChatRecordOverlay.setOnRecordingFinishedCallback(this);
    }

    private void launchNewRecipientActivity(Recipient editRecipient, int requestCode) {
        Intent intent = new Intent(mActivity, NewRecipientActivity.class);
        if (editRecipient != null) {
            intent.putExtra(NewRecipientFragment.KEY_VIA, editRecipient.getVia());
            intent.putExtra(NewRecipientFragment.KEY_NAME, editRecipient.getName());
            intent.putExtra(NewRecipientFragment.KEY_RAW_ID, editRecipient.getRawId());
            intent.putExtra(NewRecipientFragment.KEY_PHOTO_URL, editRecipient.getPhotoUri() == null ? null : Uri.parse(editRecipient.getPhotoUri()));
        }
        startActivityForResult(intent, requestCode);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        // save tapped recipient in case add contact dialog is showing
        outState.putSerializable(RECIPIENT_TAPPED_KEY, mRecipient);
        // save add contact dialog state as well
        Bundle dialogState = mSmsAddContactDialog.onSaveInstanceState();
        if (dialogState != null) {
            outState.putBundle(SAVED_DIALOG_STATE_KEY, dialogState);
        }

        if(mFinalEvent != null) {
            outState.putSerializable(RECORDING_FINAL_EVENT_KEY, mFinalEvent);
        }
        outState.putBundle(SMS_CONFIRMATION_STATE_KEY, mSmsConfirmationDialog.onSaveInstanceState());

        super.onSaveInstanceState(outState);
    }

    @Override
    public void onStart() {
        super.onStart();
        mMessagesServiceManager.bind();
        mPlayerServiceManager.bind();
        mChatRecordOverlay.bindService();
    }

    @Override
    public void onPause() {
        mChatRecordOverlay.hide(false, true);
        super.onPause();
    }

    @Override
    public void onStop() {
        mChatRecordOverlay.unbindService();
        mPlayerServiceManager.unbind();
        mMessagesServiceManager.unbind();
        super.onStop();
    }

    @Override
    public void onDestroy() {
        mMessagesServiceManager.removeReceiverListener(this);
        mMessagesServiceManager.removeSenderListener(this);
        mMessagesServiceManager.removeServiceListener(this);
        mPlayerServiceManager.removePlayerListener(this);
        mPlayerServiceManager.removeServiceListener(this);
        mActivity = null;
        super.onDestroy();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQUEST_NEWCONTACT_AND_SEND) {
            if(resultCode == Activity.RESULT_OK && mFinalEvent != null) {
                Recipient emailRecipient = RecipientAdapterUtils.getMainEmailRecipient(mActivity, mFinalEvent.getRecipient());
                if(emailRecipient == null) {
                    CustomToast.makeText(mActivity, R.string.msg_no_email_address, Toast.LENGTH_LONG).show();
                } else {
                    mSmsConfirmationDialog.dismiss();
                    sendMessage(emailRecipient, mFinalEvent.getRecording());
                    mFinalEvent = null;
                }
            }
        }
    }

    public boolean triggerRecording(View boundsView, Recipient recipient) {
        AuthenticationData authData = getCustomActionBarActivity().getAuthenticationPolicyEnforcer().getAuthenticationData();
        if(authData == null) {
            return false;
        }
        String userRef = getPreferences().getFullName();
        if(userRef == null) {
            userRef = authData.getEmail();
        }

        this.mRecipient = recipient;

        if (mRecipient.getMimeType().compareTo(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE) == 0 &&
                !Utils.isSimAvailable(mActivity)) {
            if(!mSmsAddContactDialog.isShowing()) {
                mSmsAddContactDialog.show();
            }
            return true;
        }

        // start recording
        mChatRecordOverlay.setBoundsView(boundsView);
        mChatRecordOverlay.setRecipient(mRecipient);
        mChatRecordOverlay.setSenderName(userRef);
        mChatRecordOverlay.show(false);

        return true;
    }

    @Override
    public void onRecordingFinished(RecordService.Event event) {
        mFinalEvent = event;
        if(!mPreferences.isShownSmsConfirmation() && event.getRecipient().getMimeType().equals(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)) {
            Recipient emailRecipient = RecipientAdapterUtils.getMainEmailRecipient(mActivity, mFinalEvent.getRecipient());
            mSmsConfirmationDialog.setEmailRecipient(emailRecipient);
            mSmsConfirmationDialog.show();
        } else {
            sendMessage(mFinalEvent.getRecipient(), mFinalEvent.getRecording());
            mFinalEvent = null;
        }
    }

    protected void sendMessage(Recipient recipient, Recording recording) {
        mFinalEvent = null;
        mMessagesServiceManager.send(null, recipient, recording);
    }

    protected void launchChatActivity(Chat chat, Recipient recipient) {
        Intent chatIntent = new Intent(mActivity, ChatActivity.class);
        chatIntent.putExtra(ChatFragment.PARAM_RECIPIENT, recipient);
        chatIntent.putExtra(ChatFragment.PARAM_CHAT, chat);
        startActivity(chatIntent);
    }

    public ChatRecordOverlay getChatRecordOverlay() {
        return mChatRecordOverlay;
    }

    public SenderPreferences getPreferences() {
        return mPreferences;
    }

    public CustomActionBarActivity getCustomActionBarActivity() {
        return mActivity;
    }

    public MessagesServiceManager getMessagesServiceManager() {
        return mMessagesServiceManager;
    }

    public PlayerServiceManager getPlayerServiceManager() {
        return mPlayerServiceManager;
    }

    @Override
    public void onReceivedMessage(ReceiverEvent event) {
    }

    @Override
    public void onSendStarted(SenderEvent event) {

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

    @Override
    public void onBoundSendService() {

    }

    @Override
    public void onPlayerEvent(PlayerEvent event) {

    }

    @Override
    public void onBoundPlayService() {

    }
}
