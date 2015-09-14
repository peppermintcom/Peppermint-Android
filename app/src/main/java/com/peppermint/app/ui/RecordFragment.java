package com.peppermint.app.ui;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.peppermint.app.R;
import com.peppermint.app.RecordService;
import com.peppermint.app.RecordServiceManager;
import com.peppermint.app.SendRecordServiceManager;
import com.peppermint.app.data.Recipient;
import com.peppermint.app.utils.PepperMintPreferences;
import com.peppermint.app.utils.Utils;

public class RecordFragment extends Fragment implements RecordServiceManager.Listener {//, SendRecordServiceManager.Listener {

    public static final String RECIPIENT_EXTRA = "PepperMint_RecipientExtra";
    public static final String RESULT_SENDING_EXTRA = "PepperMint_ResultSendingExtra";

    private static final String DEFAULT_FILENAME = "Peppermint";

    private TextView mDuration;
    private TextView mTap;
    private Button mSend;
    private ImageView mRecordState;

    private RecordServiceManager mRecordManager;
    //private SendRecordServiceManager mSendRecordManager;

    private boolean mFirstRun = false;
    private boolean mSavedState = false;
    private float mLastLoudnessFactor = 1.0f;
    private boolean mPressedSend = false;
    private String mFilename = DEFAULT_FILENAME;

    private PepperMintPreferences mPreferences;

    public RecordFragment() {
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mRecordManager = new RecordServiceManager(activity);
        mRecordManager.setListener(this);

        //mSendRecordManager = new SendRecordServiceManager(activity);
        //mSendRecordManager.setListener(this);

        mPreferences = new PepperMintPreferences(activity);
        mFilename = getString(R.string.filename_message_from) + Utils.normalizeAndCleanString(mPreferences.getDisplayName());
    }

    @Override
    public void onStartRecording(RecordService.Event event) {
        onBoundRecording();
    }

    @Override
    public void onStopRecording(RecordService.Event event) {
        onBoundRecording();
        if(mPressedSend) {
            Recipient recipient = (Recipient) getActivity().getIntent().getExtras().get(RECIPIENT_EXTRA);
            mPreferences.addRecentContactUri(recipient.getId());

            SendRecordServiceManager sendRecordServiceManager = new SendRecordServiceManager(getActivity());
            sendRecordServiceManager.startAndSend(recipient, event.getFullFilePaths().get(0));

            Intent resultIntent = new Intent();
            resultIntent.putExtra(RESULT_SENDING_EXTRA, true);
            getActivity().setResult(Activity.RESULT_OK, resultIntent);
            getActivity().finish();
            /*Intent intent = new Intent();
            intent.setAction(android.content.Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.fromFile(new File(event.getFullFilePaths().get(0))), "audio/mp3");
            startActivityForResult(intent, 10);
            */
        }
    }

    @Override
    public void onResumeRecording(RecordService.Event event) {
        onBoundRecording();
    }

    @Override
    public void onPauseRecording(RecordService.Event event) {
        onBoundRecording();
    }

    @Override
    public void onLoudnessRecording(RecordService.Event event) {
        Log.d("RecordFragment", "Loudness: " + event.getLoudness());

        long duration = event.getFullDuration() + event.getCurrentDuration();

        long mins = duration / 60;
        long secs = duration % 60;
        long hours = mins / 60;
        if(hours > 0) {
            mins = mins % 60;
        }

        mDuration.setText((hours > 0 ? hours + "h " : "") + mins + "m " + secs + "s");

        ObjectAnimator scaleAnimator = new ObjectAnimator();
        scaleAnimator.setDuration(100);
        scaleAnimator.setTarget(mRecordState);
        scaleAnimator.setInterpolator(new AccelerateDecelerateInterpolator());

        float loudnessFactor = 1.0f + (event.getLoudness() * 0.2f);

        scaleAnimator.setValues(
                PropertyValuesHolder.ofFloat("scaleX", mLastLoudnessFactor,loudnessFactor),
                PropertyValuesHolder.ofFloat("scaleY", mLastLoudnessFactor, loudnessFactor));
        scaleAnimator.start();

        mLastLoudnessFactor = loudnessFactor;
    }

    @Override
    public void onBoundRecording() {
        if(!mRecordManager.isRecording() || mRecordManager.isPaused()) {
            mRecordState.setImageResource(R.drawable.ic_paused);
            mTap.setText(R.string.tap_to_resume);

            if(mFirstRun) {
                mFirstRun = false;
                if(mRecordManager.isRecording()) {
                    mRecordManager.resumeRecording();
                } else {
                    mRecordManager.startRecording(mFilename);
                }
            }
        } else {
            mRecordState.setImageResource(R.drawable.ic_recording);
            mTap.setText(R.string.tap_to_pause);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.record_layout, container, false);

        mDuration = (TextView) v.findViewById(R.id.duration);
        mTap = (TextView) v.findViewById(R.id.tap);
        mSend = (Button) v.findViewById(R.id.send);
        mRecordState = (ImageView) v.findViewById(R.id.record_state);

        mSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPressedSend = true;
                mRecordManager.stopRecording();
            }
        });

        LinearLayout lytTap = (LinearLayout) v.findViewById(R.id.tapLayout);
        lytTap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mRecordManager.isRecording()) {
                    mRecordManager.startRecording(mFilename);
                    return;
                }

                if (mRecordManager.isPaused()) {
                    mRecordManager.resumeRecording();
                } else {
                    mRecordManager.pauseRecording();
                }
            }
        });

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
        mRecordManager.start();
        //mSendRecordManager.start();
    }

    @Override
    public void onPause() {
        super.onPause();
        mRecordManager.unbind();
        //mSendRecordManager.unbind();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mSavedState = true;
    }

    @Override
    public void onDestroy() {
        if(!mSavedState && !mPressedSend) {
            mRecordManager.stopRecording();
            mRecordManager.discard();
            mRecordManager.shouldStop();
        }
        super.onDestroy();
    }

    /*
    public void onBackPressed() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.your_recording_will_be_discarded_are_you_sure)
            .setPositiveButton(R.string.discard, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    getActivity().finish();
                }
            })
            .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                }
            });
        builder.create().show();
    }
    */
}
