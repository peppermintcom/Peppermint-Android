package com.peppermint.app.senders;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.crashlytics.android.Crashlytics;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadPoolExecutor;

import de.greenrobot.event.EventBus;

/**
 * Created by Nuno Luz on 08-09-2015.
 *
 * Base class for Senders. A Sender represents a method of sending an audio/video recording.
 * For instance, there's a sender for emails and a sender for SMS/text messages.
 */
public abstract class Sender {
    private static final String TAG = Sender.class.getSimpleName();

    public static final String INTENT_ID = "SenderBase_Id";
    public static final String INTENT_BROADCAST_TYPE = "SenderBase_BroadcastType";
    public static final String INTENT_REQUESTCODE = "SenderBase_RequestCode";
    public static final String INTENT_RESULTCODE = "SenderBase_ResultCode";
    public static final String INTENT_DATA = "SenderBase_Data";

    protected UUID mUuid = UUID.randomUUID();

    protected Context mContext;
    protected SharedPreferences mSettings;
    protected EventBus mEventBus;

    protected Map<UUID, SenderTask> mTaskMap;
    protected Map<UUID, SenderTask> mRecoveringTaskMap;
    protected Map<String, Object> mParams;

    private ThreadPoolExecutor mExecutor;

    protected Sender mFailureChainSender;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent != null && intent.hasExtra(INTENT_ID) && mRecoveringTaskMap.containsKey(intent.getSerializableExtra(INTENT_ID))) {
                onActivityResult(mRecoveringTaskMap.get(intent.getSerializableExtra(INTENT_ID)), intent.getIntExtra(INTENT_REQUESTCODE, -1), intent.getIntExtra(INTENT_RESULTCODE, -1), (Intent) intent.getParcelableExtra(INTENT_DATA));
            }
        }
    };
    private final String mBroadcastType = this.getClass().getSimpleName() + "-" + mUuid.toString();
    private final IntentFilter mFilter = new IntentFilter(mBroadcastType);

    protected Sender(Context context, EventBus eventBus, ThreadPoolExecutor executor) {
        this.mExecutor = executor;
        this.mContext = context;
        this.mSettings = PreferenceManager.getDefaultSharedPreferences(mContext);
        this.mTaskMap = new HashMap<>();
        this.mRecoveringTaskMap = new HashMap<>();
        this.mEventBus = eventBus;
        this.mParams = new HashMap<>();
    }

    public void init(Map<String, Object> parameters) {
        if(parameters != null) {
            this.mParams.putAll(parameters);
        }
        LocalBroadcastManager.getInstance(mContext).registerReceiver(mReceiver, mFilter);
    }

    public void deinit() {
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(mReceiver);
    }

    public SenderTask sendAsync(String toName, String toAddress, String subject, String body, String fullFilePath, String contentType) {
        SenderTask task = new SenderTask(fullFilePath, toName, toAddress, subject, body, contentType);
        task.executeOnExecutor(mExecutor);
        return task;
    }

    protected SenderTask resendAsync(SenderTask oldTask) {
        SenderTask task = new SenderTask(oldTask);
        task.executeOnExecutor(mExecutor);
        return task;
    }

    public void cancelAsync(SenderTask task) {
        task.cancel(true);
    }

    protected abstract void send(String toName, String toAddress, String subject, String body, String fullFilePath, String contentType) throws Throwable;

    protected void onActivityResult(SenderTask recoveredTask, int requestCode, int resultCode, Intent data) {
        recoveredTask.doNotRecover();   // nothing to recover; must be overriden to include additional behaviour
    }

    protected boolean recoverAndTryAgain(SenderTask task, Throwable e) {
        return false;   // nothing to recover; just send the error
    }

    protected void startActivityForResult(UUID uuid, int requestCode, Intent dataIntent) {
        Intent i = new Intent(mContext, GetResultActivity.class);
        i.putExtra(INTENT_ID, uuid);
        i.putExtra(INTENT_REQUESTCODE, requestCode);
        i.putExtra(INTENT_DATA, dataIntent);
        i.putExtra(INTENT_BROADCAST_TYPE, mBroadcastType);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(i);
    }

    protected File validateFile(String filePath) throws FileNotFoundException {
        File file = new File(filePath);
        if(!file.exists() || !file.canRead()) {
            throw new FileNotFoundException("Either the file does not exist at " + file.getAbsolutePath() + " or it can not be read!");
        }
        return file;
    }

    public UUID getId() {
        return mUuid;
    }

    public Sender getFailureChainSender() {
        return mFailureChainSender;
    }

    public void setFailureChainSender(Sender mFailureChainSender) {
        this.mFailureChainSender = mFailureChainSender;
    }

    public Map<String, Object> getParameters() {
        return mParams;
    }

    public class SenderEvent {

        public static final int EVENT_STARTED = 1;
        public static final int EVENT_CANCELLED = 2;
        public static final int EVENT_FINISHED = 3;
        public static final int EVENT_ERROR = 4;

        private SenderTask mTask;
        private int mType;
        private Throwable mError;

        public SenderEvent(SenderTask task, int type) {
            this.mTask = task;
            this.mType = type;
        }

        public SenderEvent(SenderTask task, Throwable error) {
            this.mTask = task;
            this.mType = EVENT_ERROR;
            this.mError = error;
        }

        public SenderTask getTask() {
            return mTask;
        }

        public void setTask(SenderTask mTask) {
            this.mTask = mTask;
        }

        public int getType() {
            return mType;
        }

        public void setType(int mType) {
            this.mType = mType;
        }

        public Throwable getError() {
            return mError;
        }

        public void setError(Throwable mError) {
            this.mError = mError;
        }
    }

    public class SenderTask extends AsyncTask<Void, Void, Void> {

        private UUID mUuid = UUID.randomUUID();

        private String mFilePath;
        private String mToName;
        private String mToAddress;
        private String mSubject, mBody;
        private String mContentType;
        private Throwable mError;

        public SenderTask(String mFilePath, String toName, String toAddress, String subject, String body, String contentType) {
            this.mFilePath = mFilePath;
            this.mToName = toName;
            this.mToAddress = toAddress;
            this.mSubject = subject;
            this.mBody = body;
            this.mContentType = contentType;
        }

        public SenderTask(SenderTask task) {
            this.mUuid = task.mUuid;
            this.mFilePath = task.mFilePath;
            this.mToName = task.mToName;
            this.mToAddress = task.mToAddress;
            this.mSubject = task.mSubject;
            this.mBody = task.mBody;
            this.mContentType = task.mContentType;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mEventBus.post(new SenderEvent(this, SenderEvent.EVENT_STARTED));
            mTaskMap.put(this.mUuid, this);
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                send(mToName, mToAddress, mSubject, mBody, mFilePath, mContentType);
            } catch (Throwable e) {
                mError = e;
                Log.w(TAG, e);
            }
            return null;
        }

        @Override
        protected void onCancelled(Void aVoid) {
            mEventBus.post(new SenderEvent(this, SenderEvent.EVENT_CANCELLED));
            mTaskMap.remove(this.mUuid);
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            mTaskMap.remove(this.mUuid);
            mRecoveringTaskMap.remove(this.mUuid);

            if(mError == null) {
                mEventBus.post(new SenderEvent(this, SenderEvent.EVENT_FINISHED));
            } else {
                if(!recoverAndTryAgain(this, mError)) {
                    Crashlytics.logException(mError);
                    if(mFailureChainSender == null) {
                        mEventBus.post(new SenderEvent(this, mError));
                    } else {
                        mFailureChainSender.resendAsync(this);
                    }
                } else {
                    mRecoveringTaskMap.put(this.mUuid, this);
                }
            }
        }

        public void doNotRecover() {
            if(mError != null) {
                Crashlytics.logException(mError);
                if(mFailureChainSender == null) {
                    mEventBus.post(new SenderEvent(this, mError));
                } else {
                    mFailureChainSender.resendAsync(this);
                }
            } else {
                mEventBus.post(new SenderEvent(this, SenderEvent.EVENT_FINISHED));
            }
            mRecoveringTaskMap.remove(this.mUuid);
        }

        public void doRecover() {
            resendAsync(this);
        }

        public UUID getUUID() {
            return this.mUuid;
        }

        public String getToName() { return this.mToName; }
        public String getToAddress() { return this.mToAddress; }
    }
}
