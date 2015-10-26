package com.peppermint.app.ui.recording;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
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

    private PepperMintPreferences mPreferences;

    private RecordServiceManager mRecordManager;
    private PeppermintRecordView mRecordView;
    private String mFilename = DEFAULT_FILENAME;
    private Recipient mRecipient;

    private TextView mTxtDuration;
    private TextView mTxtTap;
    private Button mBtnRestart, mBtnPauseResume;

    private boolean mFirstRun = false;
    private boolean mSavedState = false;
    private float mLastLoudnessFactor = 1.0f;
    private boolean mPressedSend = false, mPressedRestart = false;

    public RecordingFragment() {
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
                mPressedRestart = true;
                mRecordManager.stopRecording(true);
            }
        });

        mBtnPauseResume.setTypeface(app.getFontSemibold());
        mBtnPauseResume.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (!mRecordManager.isRecording() && !mRecordManager.isPaused()) {
                    mBtnPauseResume.setEnabled(false);
                    mRecordManager.startRecording(mFilename, mRecipient);
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
                    Toast.makeText(RecordingFragment.this.getActivity(), R.string.msg_record_at_least, Toast.LENGTH_SHORT).show();
                } else {
                    mPressedSend = true;
                    mRecordManager.stopRecording(false);
                }
            }
        });

        mRecordManager.start(false);

        Bundle args = getArguments();
        if(args == null || (mRecipient = (Recipient) args.get(INTENT_RECIPIENT_EXTRA)) == null) {
            Toast.makeText(getActivity(), R.string.msg_message_norecipient_error, Toast.LENGTH_LONG).show();
            Crashlytics.log(Log.ERROR, TAG, "Recipient received by fragment is null or non-existent! Unexpected access to RecordingActivity/Fragment.");
            getActivity().finish();
            return v;
        }

        if(savedInstanceState != null) {
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
        mSavedState = true;
    }

    @Override
    public void onDestroy() {
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
            mPreferences.addRecentContactUri(event.getRecipient().getContactId());

            SenderServiceManager sendRecordServiceManager = new SenderServiceManager(getActivity());
            sendRecordServiceManager.startAndSend(event.getRecipient(), event.getRecording());

            Intent resultIntent = new Intent();
            resultIntent.putExtra(INTENT_RESULT_SENDING_EXTRA, true);
            getActivity().setResult(Activity.RESULT_OK, resultIntent);
            getActivity().finish();
        } else if(mPressedRestart) {
            mRecordManager.startRecording(mFilename, mRecipient);
        }
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
                    mRecordManager.startRecording(mFilename, mRecipient);
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
