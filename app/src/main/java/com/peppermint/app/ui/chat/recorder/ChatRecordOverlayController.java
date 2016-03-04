package com.peppermint.app.ui.chat.recorder;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.peppermint.app.PlayerEvent;
import com.peppermint.app.PlayerServiceManager;
import com.peppermint.app.R;
import com.peppermint.app.RecordService;
import com.peppermint.app.authenticator.AuthenticationData;
import com.peppermint.app.authenticator.AuthenticationPolicyEnforcer;
import com.peppermint.app.cloud.MessagesServiceManager;
import com.peppermint.app.cloud.ReceiverEvent;
import com.peppermint.app.cloud.senders.SenderEvent;
import com.peppermint.app.cloud.senders.SenderPreferences;
import com.peppermint.app.data.Message;
import com.peppermint.app.data.Recipient;
import com.peppermint.app.data.RecipientManager;
import com.peppermint.app.data.Recording;
import com.peppermint.app.ui.OverlayManager;
import com.peppermint.app.ui.TouchInterceptable;
import com.peppermint.app.ui.recipients.add.NewRecipientActivity;
import com.peppermint.app.ui.recipients.add.NewRecipientFragment;
import com.peppermint.app.ui.views.dialogs.CustomConfirmationDialog;
import com.peppermint.app.ui.views.simple.CustomToast;
import com.peppermint.app.utils.Utils;

/**
 * Created by Nuno Luz on 04-03-2016.
 */
public class ChatRecordOverlayController implements ChatRecordOverlay.OnRecordingFinishedCallback,
        MessagesServiceManager.ReceiverListener, MessagesServiceManager.SenderListener, MessagesServiceManager.ServiceListener,
        PlayerServiceManager.PlayerListener, PlayerServiceManager.PlayServiceListener {

    public interface Callbacks {
        void onNewContact(Intent intentToLaunchActivity);
    }

    // keys to save the instance state
    private static final String RECIPIENT_TAPPED_KEY = "RecipientsFragment_RecipientTapped";
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

    private Recipient mRecipient;
    private RecordService.Event mFinalEvent;

    private View mView;

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
                    launchNewRecipientActivity(mFinalEvent.getRecipient());
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
                launchNewRecipientActivity(mRecipient);
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
        mMessagesServiceManager.addSenderListener(this);
        mMessagesServiceManager.addReceiverListener(this);

        mPlayerServiceManager = new PlayerServiceManager(mContext);
        mPlayerServiceManager.addServiceListener(this);
        mPlayerServiceManager.addPlayerListener(this);
    }

    public void init(View rootView, OverlayManager overlayManager, TouchInterceptable touchInterceptable, AuthenticationPolicyEnforcer authenticationPolicyEnforcer, Bundle savedInstanceState) {
        mView = rootView;
        mOverlayManager = overlayManager;
        mTouchInterceptable = touchInterceptable;
        mAuthenticationPolicyEnforcer = authenticationPolicyEnforcer;

        // start services
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

        // create overlay
        mChatRecordOverlay = (ChatRecordOverlay) mOverlayManager.createOverlay(new ChatRecordOverlay(mTouchInterceptable));
        mChatRecordOverlay.setOnRecordingFinishedCallback(this);
    }

    public void saveInstanceState(Bundle outState) {
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
        mMessagesServiceManager.removeReceiverListener(this);
        mMessagesServiceManager.removeSenderListener(this);
        mMessagesServiceManager.removeServiceListener(this);
        mPlayerServiceManager.removePlayerListener(this);
        mPlayerServiceManager.removeServiceListener(this);
    }

    private void launchNewRecipientActivity(Recipient editRecipient) {
        Intent intent = new Intent(mContext.getApplicationContext(), NewRecipientActivity.class);
        if (editRecipient != null) {
            intent.putExtra(NewRecipientFragment.KEY_VIA, editRecipient.getEmail() != null ? editRecipient.getEmail().getVia() : editRecipient.getPhone().getVia());
            intent.putExtra(NewRecipientFragment.KEY_NAME, editRecipient.getDisplayName());
            intent.putExtra(NewRecipientFragment.KEY_RAW_ID, editRecipient.getRawId());
            intent.putExtra(NewRecipientFragment.KEY_PHOTO_URL, editRecipient.getPhotoUri() == null ? null : Uri.parse(editRecipient.getPhotoUri()));
        }
        mCallbacks.onNewContact(intent);
    }

    public void handleNewContactResult(int resultCode, Intent data) {
        if(resultCode == Activity.RESULT_OK && mFinalEvent != null) {
            Recipient emailRecipient = RecipientManager.getRecipientWithMainEmailContactByRawId(mContext, mFinalEvent.getRecipient().getRawId());
            if(emailRecipient == null) {
                CustomToast.makeText(mContext, R.string.msg_no_email_address, Toast.LENGTH_LONG).show();
            } else {
                mSmsConfirmationDialog.dismiss();
                sendMessage(emailRecipient, mFinalEvent.getRecording());
                mFinalEvent = null;
            }
        }
    }

    public boolean triggerRecording(View boundsView, Recipient recipient) {
        AuthenticationData authData = mAuthenticationPolicyEnforcer.getAuthenticationData();
        if(authData == null) {
            return false;
        }

        String userRef = getPreferences().getFullName();
        if(userRef == null) {
            userRef = authData.getEmail();
        }

        this.mRecipient = recipient;

        if (mRecipient.getPhone() != null && !Utils.isSimAvailable(mContext)) {
            if(!mSmsAddContactDialog.isShowing()) {
                mSmsAddContactDialog.show();
            }
            return true;
        }

        // start recording
        mChatRecordOverlay.setViewBounds(boundsView);
        mChatRecordOverlay.setRecipient(mRecipient);
        mChatRecordOverlay.setSenderName(userRef);
        mChatRecordOverlay.show(false);

        return true;
    }

    @Override
    public void onRecordingFinished(RecordService.Event event) {
        mFinalEvent = event;
        if(!mPreferences.isShownSmsConfirmation() && event.getRecipient().getPhone() != null) {
            Recipient emailRecipient = RecipientManager.getRecipientWithMainEmailContactByRawId(mContext, mFinalEvent.getRecipient().getRawId());
            mSmsConfirmationDialog.setEmailRecipient(emailRecipient);
            mSmsConfirmationDialog.show();
        } else {
            sendMessage(mFinalEvent.getRecipient(), mFinalEvent.getRecording());
            mFinalEvent = null;
        }
    }

    protected Message sendMessage(Recipient recipient, Recording recording) {
        mFinalEvent = null;
        return mMessagesServiceManager.send(recipient, recording);
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
