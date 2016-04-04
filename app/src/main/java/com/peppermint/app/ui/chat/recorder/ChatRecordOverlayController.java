package com.peppermint.app.ui.chat.recorder;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.peppermint.app.PlayerServiceManager;
import com.peppermint.app.R;
import com.peppermint.app.authenticator.AuthenticationData;
import com.peppermint.app.authenticator.AuthenticationPolicyEnforcer;
import com.peppermint.app.cloud.MessagesServiceManager;
import com.peppermint.app.cloud.senders.SenderPreferences;
import com.peppermint.app.data.Chat;
import com.peppermint.app.data.ChatRecipient;
import com.peppermint.app.data.ContactManager;
import com.peppermint.app.data.ContactRaw;
import com.peppermint.app.data.Message;
import com.peppermint.app.data.Recording;
import com.peppermint.app.events.PeppermintEventBus;
import com.peppermint.app.events.PlayerEvent;
import com.peppermint.app.events.ReceiverEvent;
import com.peppermint.app.events.RecorderEvent;
import com.peppermint.app.events.SenderEvent;
import com.peppermint.app.events.SyncEvent;
import com.peppermint.app.ui.OverlayManager;
import com.peppermint.app.ui.TouchInterceptable;
import com.peppermint.app.ui.recipients.add.NewContactActivity;
import com.peppermint.app.ui.recipients.add.NewContactFragment;
import com.peppermint.app.ui.base.dialogs.CustomConfirmationDialog;
import com.peppermint.app.ui.base.views.CustomToast;
import com.peppermint.app.utils.Utils;

/**
 * Created by Nuno Luz on 04-03-2016.
 */
public class ChatRecordOverlayController implements ChatRecordOverlay.OnRecordingFinishedCallback,
        MessagesServiceManager.ServiceListener,
        PlayerServiceManager.PlayServiceListener {

    public interface Callbacks {
        void onNewContact(Intent intentToLaunchActivity);
    }

    // keys to save the instance state
    private static final String CHAT_TAPPED_KEY = "RecipientsFragment_ChatTapped";
    private static final String SAVED_DIALOG_STATE_KEY = "RecipientsFragment_SmsDialogState";
    private static final String RECORDING_FINAL_EVENT_KEY = "RecipientsFragment_RecordingFinalEvent";
    private static final String SMS_CONFIRMATION_STATE_KEY = "RecipientsFragment_SmsConfirmationState";

    // contextual/external instances
    private Context mContext;
    private SenderPreferences mPreferences;
    private OverlayManager mOverlayManager;
    private TouchInterceptable mTouchInterceptable;
    private AuthenticationPolicyEnforcer mAuthenticationPolicyEnforcer;
    private Callbacks mCallbacks;

    private MessagesServiceManager mMessagesServiceManager;
    private PlayerServiceManager mPlayerServiceManager;

    // overlay
    private ChatRecordOverlay mChatRecordOverlay;

    // dialogs
    private SMSConfirmationDialog mSmsConfirmationDialog;
    private CustomConfirmationDialog mSmsAddContactDialog;

    private Chat mChat;
    private RecorderEvent mFinalEvent;

    public ChatRecordOverlayController(Context context, Callbacks callbacks) {
        mContext = context;
        mCallbacks = callbacks;

        mPreferences = new SenderPreferences(mContext);

        // send through SMS confirmation dialog
        mSmsConfirmationDialog = new SMSConfirmationDialog(mContext);
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
                sendMessage(mFinalEvent.getChat(), mFinalEvent.getRecording());
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
                ContactRaw emailRecipient = mSmsConfirmationDialog.getEmailRecipient();
                if (emailRecipient != null) {
                    mFinalEvent.getChat().getRecipientList().get(0).setFromRawContact(emailRecipient);
                    sendMessage(mFinalEvent.getChat(), mFinalEvent.getRecording());
                    mFinalEvent = null;
                    mSmsConfirmationDialog.dismiss();
                } else {
                    launchNewRecipientActivity(mFinalEvent.getChat().getRecipientList().get(0));
                }
            }
        });

        // dialog for unsupported SMS
        mSmsAddContactDialog = new CustomConfirmationDialog(mContext);
        mSmsAddContactDialog.setTitleText(R.string.sending_via_sms);
        mSmsAddContactDialog.setMessageText(R.string.msg_sms_disabled_add_contact);
        mSmsAddContactDialog.setPositiveButtonListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchNewRecipientActivity(mChat.getRecipientList().get(0));
                mSmsAddContactDialog.dismiss();
            }
        });
        mSmsAddContactDialog.setNegativeButtonListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSmsAddContactDialog.dismiss();
            }
        });

        // services
        mMessagesServiceManager = new MessagesServiceManager(mContext);
        mMessagesServiceManager.addServiceListener(this);
        PeppermintEventBus.registerMessages(this);

        mPlayerServiceManager = new PlayerServiceManager(mContext);
        mPlayerServiceManager.addServiceListener(this);
    }

    public void init(View rootView, OverlayManager overlayManager, TouchInterceptable touchInterceptable, AuthenticationPolicyEnforcer authenticationPolicyEnforcer, Bundle savedInstanceState) {
        mOverlayManager = overlayManager;
        mTouchInterceptable = touchInterceptable;
        mAuthenticationPolicyEnforcer = authenticationPolicyEnforcer;

        // start services
        mMessagesServiceManager.start();
        mPlayerServiceManager.start();

        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(RECORDING_FINAL_EVENT_KEY)) {
                mFinalEvent = (RecorderEvent) savedInstanceState.getSerializable(RECORDING_FINAL_EVENT_KEY);
            }

            Bundle smsDialogState = savedInstanceState.getBundle(SMS_CONFIRMATION_STATE_KEY);
            if (smsDialogState != null) {
                mSmsConfirmationDialog.onRestoreInstanceState(smsDialogState);
            }

            mChat = (Chat) savedInstanceState.getSerializable(CHAT_TAPPED_KEY);

            Bundle dialogState = savedInstanceState.getBundle(SAVED_DIALOG_STATE_KEY);
            if (dialogState != null) {
                mSmsAddContactDialog.onRestoreInstanceState(dialogState);
            }
        }

        // create overlay
        mChatRecordOverlay = (ChatRecordOverlay) mOverlayManager.createOverlay(new ChatRecordOverlay(mTouchInterceptable));
        mChatRecordOverlay.setOnRecordingFinishedCallback(this);
    }

    public void saveInstanceState(Bundle outState) {
        // save tapped recipient in case add contact dialog is showing
        outState.putSerializable(CHAT_TAPPED_KEY, mChat);
        // save add contact dialog state as well
        Bundle dialogState = mSmsAddContactDialog.onSaveInstanceState();
        if (dialogState != null) {
            outState.putBundle(SAVED_DIALOG_STATE_KEY, dialogState);
        }

        if(mFinalEvent != null) {
            outState.putSerializable(RECORDING_FINAL_EVENT_KEY, mFinalEvent);
        }
        outState.putBundle(SMS_CONFIRMATION_STATE_KEY, mSmsConfirmationDialog.onSaveInstanceState());
    }

    public void start() {
        mMessagesServiceManager.bind();
        mPlayerServiceManager.bind();
        mChatRecordOverlay.bindService();
    }

    public void stop() {
        mChatRecordOverlay.hide(false, true);
        mChatRecordOverlay.unbindService();
        mPlayerServiceManager.unbind();
        mMessagesServiceManager.unbind();
    }

    public void deinit() {
        PeppermintEventBus.unregisterMessages(this);
        mMessagesServiceManager.removeServiceListener(this);
        mPlayerServiceManager.removeServiceListener(this);
    }

    private void launchNewRecipientActivity(ChatRecipient editRecipient) {
        Intent intent = new Intent(mContext.getApplicationContext(), NewContactActivity.class);
        if (editRecipient != null) {
            intent.putExtra(NewContactFragment.KEY_VIA, editRecipient.getVia());
            intent.putExtra(NewContactFragment.KEY_NAME, editRecipient.getDisplayName());
            intent.putExtra(NewContactFragment.KEY_RAW_ID, editRecipient.getRawContactId());
            intent.putExtra(NewContactFragment.KEY_PHOTO_URL, editRecipient.getPhotoUri() == null ? null : Uri.parse(editRecipient.getPhotoUri()));
        }
        mCallbacks.onNewContact(intent);
    }

    public void handleNewContactResult(int resultCode, Intent data) {
        if(resultCode == Activity.RESULT_OK && mFinalEvent != null) {
            ContactRaw emailRecipient = ContactManager.getRawContactWithEmailByRawId(mContext, mFinalEvent.getChat().getRecipientList().get(0).getRawContactId());
            if(emailRecipient == null) {
                CustomToast.makeText(mContext, R.string.msg_no_email_address, Toast.LENGTH_LONG).show();
            } else {
                mSmsConfirmationDialog.dismiss();
                mFinalEvent.getChat().getRecipientList().get(0).setFromRawContact(emailRecipient);
                sendMessage(mFinalEvent.getChat(), mFinalEvent.getRecording());
                mFinalEvent = null;
            }
        }
    }

    public boolean triggerRecording(View boundsView, Chat chat) {
        AuthenticationData authData = mAuthenticationPolicyEnforcer.getAuthenticationData();
        if(authData == null) {
            return false;
        }

        String userRef = getPreferences().getFullName();
        if(userRef == null) {
            userRef = authData.getEmail();
        }

        this.mChat = chat;

        if (mChat.getRecipientList().get(0).isPhone() && !Utils.isSimAvailable(mContext)) {
            if(!mSmsAddContactDialog.isShowing()) {
                mSmsAddContactDialog.show();
            }
            return true;
        }

        // start recording
        mChatRecordOverlay.setViewBounds(boundsView);
        mChatRecordOverlay.setChat(mChat);
        mChatRecordOverlay.setSenderName(userRef);
        mChatRecordOverlay.show(false);

        return true;
    }

    @Override
    public void onRecordingFinished(RecorderEvent event) {
        mFinalEvent = event;
        if(!mPreferences.isShownSmsConfirmation() && event.getChat().getRecipientList().get(0).isPhone()) {
            ContactRaw emailRecipient = ContactManager.getRawContactWithEmailByRawId(mContext, mFinalEvent.getChat().getRecipientList().get(0).getRawContactId());
            mSmsConfirmationDialog.setEmailRecipient(emailRecipient);
            mSmsConfirmationDialog.show();
        } else {
            sendMessage(mFinalEvent.getChat(), mFinalEvent.getRecording());
            mFinalEvent = null;
        }
    }

    protected Message sendMessage(Chat chat, Recording recording) {
        mFinalEvent = null;
        return mMessagesServiceManager.send(chat, recording);
    }

    public ChatRecordOverlay getChatRecordOverlay() {
        return mChatRecordOverlay;
    }

    public SenderPreferences getPreferences() {
        return mPreferences;
    }

    public Context getContext() {
        return mContext;
    }

    public void setContext(Context mContext) {
        this.mContext = mContext;
    }

    public MessagesServiceManager getMessagesServiceManager() {
        return mMessagesServiceManager;
    }

    public PlayerServiceManager getPlayerServiceManager() {
        return mPlayerServiceManager;
    }

    public TouchInterceptable getTouchInterceptable() {
        return mTouchInterceptable;
    }

    public void setTouchInterceptable(TouchInterceptable mTouchInterceptable) {
        this.mTouchInterceptable = mTouchInterceptable;
    }

    public void onEventMainThread(SyncEvent event) {
    }

    public void onEventMainThread(ReceiverEvent event) {
    }

    public void onEventMainThread(SenderEvent event) {
    }

    @Override
    public void onBoundSendService() {
    }

    public void onEventMainThread(PlayerEvent event) {
    }

    @Override
    public void onBoundPlayService() {
    }

}
