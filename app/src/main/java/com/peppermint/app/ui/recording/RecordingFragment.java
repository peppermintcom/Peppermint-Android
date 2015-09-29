package com.peppermint.app.ui.recording;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import com.peppermint.app.SendRecordServiceManager;
import com.peppermint.app.data.Recipient;
import com.peppermint.app.ui.views.PeppermintRecordView;
import com.peppermint.app.utils.PepperMintPreferences;
import com.peppermint.app.utils.Utils;

public class RecordingFragment extends Fragment implements RecordServiceManager.Listener {

    private static final String TAG = RecordingFragment.class.getSimpleName();

    private static final String DEFAULT_FILENAME = "Peppermint";

    // Intent key containing the Recipient received by the Activity
    public static final String INTENT_RECIPIENT_EXTRA = "PepperMint_RecipientExtra";
    // Intent key containing a flag indicating if the sending of the recorded file was requested or not
    public static final String INTENT_RESULT_SENDING_EXTRA = "PepperMint_ResultSendingExtra";

    private PepperMintPreferences mPreferences;
    private RecordServiceManager mRecordManager;

    private PeppermintRecordView mRecordView;
    //private AnimatedGIFView mRecordView;

    private TextView mTxtDuration;
    private TextView mTxtTap;
    private Button mBtnRestart, mBtnPauseResume;

    private boolean mFirstRun = false;
    private boolean mSavedState = false;
    private float mLastLoudnessFactor = 1.0f;
    private boolean mPressedSend = false, mPressedRestart = false;
    private String mFilename = DEFAULT_FILENAME;

    public RecordingFragment() {
    }

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
        //mRecordView = (AnimatedGIFView) v.findViewById(R.id.record_state);

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
                    Recipient recipient = (Recipient) getActivity().getIntent().getExtras().get(INTENT_RECIPIENT_EXTRA);
                    mRecordManager.startRecording(mFilename, recipient);
                    return;
                }

                try {
                    if (mRecordManager.isPaused()) {
                        mRecordManager.resumeRecording();
                    } else {
                        mRecordManager.pauseRecording();
                    }
                } catch (RuntimeException e) {
                    Log.e(TAG, e.getMessage(), e);
                }
            }
        });

        mTxtDuration.setTypeface(app.getFontSemibold());
        mTxtTap.setTypeface(app.getFontSemibold());

        mRecordView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(RecordingFragment.this.getActivity(), R.string.msg_record_at_least, Toast.LENGTH_SHORT).show();
            }
        });

        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mRecordView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mPressedSend = true;
                        mRecordManager.stopRecording(false);
                    }
                });
            }
        }, 2000);

        mRecordManager.start(false);

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
        //mRecordView.start();
        onBoundRecording();
    }

    @Override
    public void onStopRecording(RecordService.Event event) {
        //mRecordView.stop();
        onBoundRecording();
        mLastLoudnessFactor = 1f;

        if(mPressedSend) {
            mPreferences.addRecentContactUri(event.getRecipient().getId());

            SendRecordServiceManager sendRecordServiceManager = new SendRecordServiceManager(getActivity());
            sendRecordServiceManager.startAndSend(event.getRecipient(), event.getFilePath());

            Intent resultIntent = new Intent();
            resultIntent.putExtra(INTENT_RESULT_SENDING_EXTRA, true);
            getActivity().setResult(Activity.RESULT_OK, resultIntent);
            getActivity().finish();
        } else if(mPressedRestart) {
            Recipient recipient = (Recipient) getActivity().getIntent().getExtras().get(INTENT_RECIPIENT_EXTRA);
            mRecordManager.startRecording(mFilename, recipient);
        }
    }

    @Override
    public void onResumeRecording(RecordService.Event event) {
        onBoundRecording();
        //mRecordView.start();
    }

    @Override
    public void onPauseRecording(RecordService.Event event) {
        onBoundRecording();
        //mRecordView.stop();
    }

    @Override
    public void onLoudnessRecording(RecordService.Event event) {
        float duration = ((float)(event.getFullDuration())) / 1000f;
        mRecordView.setSeconds(duration);

        long mins = (long) duration / 60;
        long secs = (long) duration % 60;
        long hours = mins / 60;
        if(hours > 0) {
            mins = mins % 60;
        }

        mTxtDuration.setText((hours > 0 ? hours + ":" : "") + mins + ":" + (secs < 10 ? "0" : "") + secs);

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
        Crashlytics.logException(event.getError());
        Toast.makeText(getActivity(), getString(R.string.msg_message_record_error), Toast.LENGTH_LONG).show();
        getActivity().finish();
    }

    @Override
    public void onBoundRecording() {
        if(!mRecordManager.isRecording() || mRecordManager.isPaused()) {
            mBtnPauseResume.setText(R.string.resume);
            if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                mBtnPauseResume.setCompoundDrawablesWithIntrinsicBounds(null, Utils.getDrawable(getActivity(), R.drawable.ic_resume_states), null, null);
            } else {
                mBtnPauseResume.setCompoundDrawablesWithIntrinsicBounds(Utils.getDrawable(getActivity(), R.drawable.ic_resume_states), null, null, null);
            }

            if(mFirstRun) {
                mFirstRun = false;
                if(mRecordManager.isRecording()) {
                    mRecordManager.resumeRecording();
                } else {
                    Recipient recipient = (Recipient) getActivity().getIntent().getExtras().get(INTENT_RECIPIENT_EXTRA);
                    mRecordManager.startRecording(mFilename, recipient);
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
    }
}
