package com.peppermint.app.ui.recording;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.app.Activity;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.peppermint.app.PeppermintApp;
import com.peppermint.app.R;
import com.peppermint.app.RecordService;
import com.peppermint.app.RecordServiceManager;
import com.peppermint.app.SenderServiceManager;
import com.peppermint.app.data.Recipient;
import com.peppermint.app.data.Recording;
import com.peppermint.app.ui.canvas.old.PeppermintRecordView;
import com.peppermint.app.ui.views.simple.CustomToast;
import com.peppermint.app.ui.views.dialogs.CustomConfirmationDialog;
import com.peppermint.app.utils.NoMicDataIOException;
import com.peppermint.app.utils.PepperMintPreferences;
import com.peppermint.app.utils.Utils;

public class RecordingFragment extends Fragment implements RecordServiceManager.Listener {

    private static final String TAG = RecordingFragment.class.getSimpleName();

    private static final String DEFAULT_FILENAME = "Peppermint";

    // intent key containing the Recipient received by the Activity
    public static final String INTENT_RECIPIENT_EXTRA = "PepperMint_RecipientExtra";
    // intent key containing a flag indicating if the sending of the recorded file was requested or not
    public static final String INTENT_RESULT_SENDING_EXTRA = "PepperMint_ResultSendingExtra";

    private static final long MAX_DURATION_MILLIS = 600000; // 10min
    private static final String SAVED_DIALOG_STATE_TAG = "Peppermint_SmsDialogState";

    private PepperMintPreferences mPreferences;

    private RecordServiceManager mRecordManager;
    private PeppermintRecordView mRecordView;
    private String mFilename = DEFAULT_FILENAME;
    //private Recipient mRecipient;

    private TextView mTxtDuration;
    private TextView mTxtTap;
    private Button mBtnRestart, mBtnPauseResume;

    private boolean mFirstRun = false;
    private boolean mSavedState = false;
    private float mLastLoudnessFactor = 1.0f;
    private boolean mPressedSend = false, mPressedRestart = false;

    private CustomConfirmationDialog mSmsConfirmationDialog;

    public RecordingFragment() {
    }

    private Recipient getRecipient() {
        return getArguments() != null ? (Recipient) getArguments().get(INTENT_RECIPIENT_EXTRA) : null;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        mRecordManager = new RecordServiceManager(activity);
        mRecordManager.setListener(this);

        mPreferences = new PepperMintPreferences(activity);
        mFilename = getString(R.string.filename_message_from) + Utils.normalizeAndCleanString(mPreferences.getDisplayName());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        PeppermintApp app = (PeppermintApp) getActivity().getApplication();

        mSmsConfirmationDialog = new CustomConfirmationDialog(getActivity());
        mSmsConfirmationDialog.setTitleText(R.string.sending_via_sms);
        mSmsConfirmationDialog.setMessageText(R.string.when_you_send_via_sms);
        mSmsConfirmationDialog.setCheckText(R.string.do_not_show_this_again);
        mSmsConfirmationDialog.setPositiveButtonListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSmsConfirmationDialog.dismiss();
                sendMessage();
            }
        });
        mSmsConfirmationDialog.setNegativeButtonListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSmsConfirmationDialog.dismiss();
            }
        });
        mSmsConfirmationDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                if(mSmsConfirmationDialog.isChecked()) {
                    mPreferences.setShownSmsConfirmation(true);
                }
                getActivity().finish();
            }
        });

        View v = inflater.inflate(R.layout.f_recording_layout, container, false);

        mTxtDuration = (TextView) v.findViewById(R.id.duration);
        mTxtTap = (TextView) v.findViewById(R.id.tap);
        mBtnRestart = (Button) v.findViewById(R.id.btnRestart);
        mBtnPauseResume = (Button) v.findViewById(R.id.btnPauseResume);
        mRecordView = (PeppermintRecordView) v.findViewById(R.id.record_state);

        mBtnRestart.setTypeface(app.getFontSemibold());
        mBtnRestart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mRecordManager.isRecording()) {
                    mPressedRestart = true;
                    mRecordManager.stopRecording(true);
                } else {
                    mRecordManager.startRecording(mFilename, getRecipient(), MAX_DURATION_MILLIS);
                }
            }
        });

        mBtnPauseResume.setTypeface(app.getFontSemibold());
        mBtnPauseResume.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (!mRecordManager.isRecording() && !mRecordManager.isPaused()) {
                    mBtnPauseResume.setEnabled(false);
                    mRecordManager.startRecording(mFilename, getRecipient(), MAX_DURATION_MILLIS);
                    return;
                }

                try {
                    mBtnPauseResume.setEnabled(false);
                    if (mRecordManager.isPaused()) {
                        mRecordManager.resumeRecording();
                    } else {
                        mRecordManager.pauseRecording();
                    }
                } catch (RuntimeException e) {
                    mBtnPauseResume.setEnabled(true);
                    Crashlytics.logException(e);
                    Log.e(TAG, e.getMessage(), e);
                }
            }
        });

        mTxtDuration.setTypeface(app.getFontSemibold());
        mTxtTap.setTypeface(app.getFontSemibold());

        mRecordView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mRecordView.getSeconds() < 2) {
                    CustomToast.makeText(RecordingFragment.this.getActivity(), R.string.msg_record_at_least, Toast.LENGTH_SHORT).show();
                } else {
                    if(mRecordManager.isRecording()) {
                        mPressedSend = true;
                        mRecordManager.stopRecording(false);
                    } else {
                        confirmAndSendMessage();
                    }
                }
            }
        });

        mRecordManager.start(false);

        Bundle args = getArguments();
        if(args == null || getRecipient() == null) {
            Toast.makeText(getActivity(), R.string.msg_message_norecipient_error, Toast.LENGTH_LONG).show();
            Crashlytics.log(Log.ERROR, TAG, "Recipient received by fragment is null or non-existent! Unexpected access to RecordingActivity/Fragment.");
            getActivity().finish();
            return v;
        }

        if(savedInstanceState != null) {
            Bundle dialogState = savedInstanceState.getBundle(SAVED_DIALOG_STATE_TAG);
            if (dialogState != null) {
                mSmsConfirmationDialog.onRestoreInstanceState(dialogState);
            }
            return v;
        }

        mFirstRun = true;

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        mSavedState = false;
        mPressedSend = false;
        mRecordManager.setListener(this);
        mRecordManager.bind();
    }

    @Override
    public void onPause() {
        super.onPause();
        mRecordManager.unbind();
        mRecordManager.setListener(null);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        Bundle dialogState = mSmsConfirmationDialog.onSaveInstanceState();
        if (dialogState != null) {
            outState.putBundle(SAVED_DIALOG_STATE_TAG, dialogState);
        }

        mSavedState = true;
    }

    @Override
    public void onDestroy() {
        // FIXME this destroys the recording when pressing the notification after going out of the activity
        // move the RecordingManager to the activity instead of the fragment, and handle in the ondestroy of the activity?
        if(!mSavedState && !mPressedSend) {
            mRecordManager.stopRecording(true);
            mRecordManager.shouldStop();
        }
        super.onDestroy();
    }

    @Override
    public void onStartRecording(RecordService.Event event) {
        onBoundRecording(event.getRecording(), event.getRecipient(), event.getLoudness());
    }

    @Override
    public void onStopRecording(RecordService.Event event) {
        onBoundRecording(event.getRecording(), event.getRecipient(), event.getLoudness());
        mLastLoudnessFactor = 1f;

        if(mPressedSend) {
            confirmAndSendMessage();
            mPressedSend = false;
        } else if(mPressedRestart) {
            mRecordManager.startRecording(mFilename, getRecipient(), MAX_DURATION_MILLIS);
            mPressedRestart = false;
        } else if(event.getRecording().getDurationMillis() >= MAX_DURATION_MILLIS) {
            mBtnPauseResume.setEnabled(false);
            Toast.makeText(getActivity(), R.string.msg_message_exceeded_maxduration, Toast.LENGTH_LONG).show();
        }
    }

    private void confirmAndSendMessage() {
        if(!mPreferences.isShownSmsConfirmation() && getRecipient().getMimeType().equals(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)) {
            mSmsConfirmationDialog.show();
            // FIXME record is not deleted from sdcard if person says no
            return;
        }
        sendMessage();
    }

    private void sendMessage() {
        mPreferences.addRecentContactUri(getRecipient().getContactId());

        SenderServiceManager sendRecordServiceManager = new SenderServiceManager(getActivity());
        sendRecordServiceManager.startAndSend(getRecipient(), mRecordManager.getCurrentRecording());

        Intent resultIntent = new Intent();
        resultIntent.putExtra(INTENT_RESULT_SENDING_EXTRA, true);
        getActivity().setResult(Activity.RESULT_OK, resultIntent);
        getActivity().finish();
    }

    @Override
    public void onResumeRecording(RecordService.Event event) {
        onBoundRecording(event.getRecording(), event.getRecipient(), event.getLoudness());
    }

    @Override
    public void onPauseRecording(RecordService.Event event) {
        onBoundRecording(event.getRecording(), event.getRecipient(), event.getLoudness());
    }

    private void setRecordDuration(float fullDuration) {
        float duration = fullDuration / 1000f;
        mRecordView.setSeconds(duration);

        long mins = (long) duration / 60;
        long secs = (long) duration % 60;
        long hours = mins / 60;
        if(hours > 0) {
            mins = mins % 60;
        }

        mTxtDuration.setText((hours > 0 ? hours + ":" : "") + mins + ":" + (secs < 10 ? "0" : "") + secs);
    }

    @Override
    public void onLoudnessRecording(RecordService.Event event) {
        setRecordDuration(event.getRecording().getDurationMillis());

        ObjectAnimator scaleAnimator = new ObjectAnimator();
        scaleAnimator.setDuration(100);
        scaleAnimator.setTarget(mRecordView);
        scaleAnimator.setInterpolator(new AccelerateDecelerateInterpolator());

        float loudnessFactor = 1.0f + (event.getLoudness() * 0.2f);

        scaleAnimator.setValues(
                PropertyValuesHolder.ofFloat("scaleX", mLastLoudnessFactor, loudnessFactor),
                PropertyValuesHolder.ofFloat("scaleY", mLastLoudnessFactor, loudnessFactor));
        scaleAnimator.start();

        mLastLoudnessFactor = loudnessFactor;
    }

    @Override
    public void onErrorRecording(RecordService.Event event) {
        if(event.getError() instanceof NoMicDataIOException) {
            Toast.makeText(getActivity(), getString(R.string.msg_message_nomicdata_error), Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(getActivity(), getString(R.string.msg_message_record_error), Toast.LENGTH_LONG).show();
        }
        getActivity().finish();
    }

    @Override
    public void onBoundRecording(Recording currentRecording, Recipient currentRecipient, float currentLoudness) {
        setRecordDuration(currentRecording == null ? 0 : currentRecording.getDurationMillis());

        if(!mRecordManager.isRecording() || mRecordManager.isPaused()) {
            mBtnPauseResume.setText(R.string.resume);
            if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                mBtnPauseResume.setCompoundDrawablesWithIntrinsicBounds(null, Utils.getDrawable(getActivity(), R.drawable.ic_resume_states), null, null);
            } else {
                mBtnPauseResume.setCompoundDrawablesWithIntrinsicBounds(Utils.getDrawable(getActivity(), R.drawable.ic_resume_states), null, null, null);
            }

            if(mFirstRun) {
                mFirstRun = false;
                if(mRecordManager.isPaused()) {
                    mRecordManager.resumeRecording();
                } else if(!mRecordManager.isRecording()){
                    mRecordManager.startRecording(mFilename, getRecipient(), MAX_DURATION_MILLIS);
                }
            }
        } else {
            mBtnPauseResume.setText(R.string.pause);
            if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                mBtnPauseResume.setCompoundDrawablesWithIntrinsicBounds(null, Utils.getDrawable(getActivity(), R.drawable.ic_pause_states), null, null);
            } else {
                mBtnPauseResume.setCompoundDrawablesWithIntrinsicBounds(Utils.getDrawable(getActivity(), R.drawable.ic_pause_states), null, null, null);
            }
        }

        mBtnPauseResume.setEnabled(true);
    }
}
