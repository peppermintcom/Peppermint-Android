package com.peppermint.app.utils;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.peppermint.app.trackers.TrackerManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by Nuno Luz on 08-06-2016.
 *
 * Controls the instantiation and execution of the very same AsyncTask.<br />
 * Very useful for refresh tasks that might be called multiple times in a very short period of time.<br />
 * This limits the amount of AsyncTasks and makes sure no redundant executions are performed.
 */
public abstract class SameAsyncTaskExecutor<Params, Progress, Result> {

    private static final String TAG = SameAsyncTaskExecutor.class.getSimpleName();

    protected class SameAsyncTask extends AsyncTask<Params, Progress, Result> {
        private boolean mRunning = false;
        private boolean mNextPending = false;
        private Params[] mOverrideParams;

        @Override
        protected void onPreExecute() {
            SameAsyncTaskExecutor.this.onPreExecute();
        }

        @Override
        protected void onProgressUpdate(Progress... values) {
            SameAsyncTaskExecutor.this.onProgressUpdate(values);
        }

        @Override
        protected void onPostExecute(Result result) {
            mAsyncTaskList.remove(this);
            SameAsyncTaskExecutor.this.onPostExecute(result);
        }

        @Override
        protected Result doInBackground(Params... params) {
            mStartLock.lock();
            mRunning = true;
            mStartLock.unlock();
            if(mOverrideParams != null) {
                Log.d(TAG, "Got Override Params!");
            }
            return SameAsyncTaskExecutor.this.doInBackground(this, mOverrideParams == null ? params : mOverrideParams);
        }

        @Override
        protected void onCancelled() {
            mAsyncTaskList.remove(this);
            SameAsyncTaskExecutor.this.onCancelled();
        }

        @Override
        protected void onCancelled(Result result) {
            mAsyncTaskList.remove(this);
            SameAsyncTaskExecutor.this.onCancelled(result);
        }

        public boolean isNextPending() {
            return mNextPending;
        }
    }

    private static final int DEFAULT_MAX_TASKS = 2;

    public static final int MODE_DISCARD_EXCESS = 1;
    public static final int MODE_CANCEL_CURRENT = 2;

    private int mMode = MODE_DISCARD_EXCESS;
    private int mMaxTasks = DEFAULT_MAX_TASKS;

    private List<SameAsyncTask> mAsyncTaskList = new ArrayList<>();
    private Lock mStartLock = new ReentrantLock();

    protected ExecutorService mExecutor;
    protected Context mContext;

    public SameAsyncTaskExecutor(Context mContext) {
        this.mContext = mContext;
    }

    @SafeVarargs
    public final synchronized SameAsyncTask execute(Params... params) {
        boolean goAhead = false;
        switch (mMode) {
            case MODE_CANCEL_CURRENT:
                if(!hasPendingTask()) {
                    cancel(true);
                    goAhead = true;
                }
                break;
            case MODE_DISCARD_EXCESS:
                final int size = mAsyncTaskList.size();
                if(!hasPendingTask() && size < mMaxTasks) {
                    goAhead = true;
                } else {
                    mStartLock.lock();
                    try {
                        if (!mAsyncTaskList.get(size - 1).mRunning) {
                            mAsyncTaskList.get(size - 1).mOverrideParams = params;
                        }
                    } catch(Throwable e) {
                        TrackerManager.getInstance(mContext).logException(e);
                    }
                    mStartLock.unlock();
                }
                break;
        }

        if(goAhead) {
            markNextIsPending();
            final SameAsyncTask asyncTask = new SameAsyncTask();
            mAsyncTaskList.add(asyncTask);
            if(mExecutor != null) {
                asyncTask.executeOnExecutor(mExecutor, params);
            } else {
                asyncTask.execute(params);
            }
            return asyncTask;
        }

        Log.d(TAG, "NO NEED to Execute New SameAsyncTask!");

        return null;
    }

    protected void onPreExecute() {
        /* nothing to do here */
    }

    protected void onProgressUpdate(Progress... values) {
        /* nothing to do here */
    }

    protected void onPostExecute(Result result) {
        /* nothing to do here */
    }

    protected void onCancelled() {
        /* nothing to do here */
    }

    protected void onCancelled(Result result) {
        /* nothing to do here */
    }

    protected abstract Result doInBackground(SameAsyncTask sameAsyncTask, Params... params);

    private boolean hasPendingTask() {
        for(SameAsyncTask sameAsyncTask : mAsyncTaskList) {
            if(sameAsyncTask.getStatus() == AsyncTask.Status.PENDING && !sameAsyncTask.mRunning) {
                return true;
            }
        }
        return false;
    }

    private void markNextIsPending() {
        for(SameAsyncTask sameAsyncTask : mAsyncTaskList) {
            sameAsyncTask.mNextPending = true;
        }
    }

    public synchronized void cancel(boolean mayInterruptIfRunning) {
        for(AsyncTask asyncTask : mAsyncTaskList) {
            asyncTask.cancel(mayInterruptIfRunning);
        }
    }

    public void setExecutor(ExecutorService mExecutor) {
        this.mExecutor = mExecutor;
    }
}
