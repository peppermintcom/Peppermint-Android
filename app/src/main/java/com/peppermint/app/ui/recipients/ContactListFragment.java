package com.peppermint.app.ui.recipients;

import android.app.Activity;
import android.app.ListFragment;
import android.content.Context;
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.HeaderViewListAdapter;
import android.widget.ListAdapter;
import android.widget.PopupWindow;

import com.peppermint.app.R;
import com.peppermint.app.tracking.TrackerManager;
import com.peppermint.app.ui.canvas.avatar.AnimatedAvatarView;
import com.peppermint.app.utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public abstract class ContactListFragment extends ListFragment implements
        AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener,
        SearchListBarView.OnSearchListener {

    // avatar animation frequency
    private static final int FIXED_AVATAR_ANIMATION_INTERVAL_MS = 7500;
    private static final int VARIABLE_AVATAR_ANIMATION_INTERVAL_MS = 7500;

    protected ContactActivity mActivity;

    // UI
    private ViewGroup mListFooterView;
    private View.OnClickListener mAddContactClickListener;

    // hold popup
    private PopupWindow mHoldPopup;
    private final Point mLastTouchPoint = new Point();
    private final Runnable mDismissPopupRunnable = new Runnable() {
        @Override
        public void run() {
            dismissHoldPopup();
        }
    };

    // search
    private RefreshTask mRefreshTask;
    private ThreadPoolExecutor mThreadPoolExecutor;
    protected boolean mRefreshing = false;

    private class RefreshTask extends AsyncTask<Void, Void, Object> {
        private Context _context;
        private String _filter;
        private String _name, _via;
        private boolean _doNotChangeState = false;

        protected RefreshTask(Context context, String filter) {
            this._context = context;
            this._filter = filter;
        }

        @Override
        protected void onPreExecute() {
            if(_filter == null) {
                return;
            }

            if(mActivity != null) {
                mActivity.getLoadingController().setLoading(true);
            }

            onAsyncRefreshStarted(_context);

            String[] viaName = ContactActivity.getFilterData(_filter);
            _via = viaName[0]; _name = viaName[1];
        }

        @Override
        protected Object doInBackground(Void... nothing) {
            mRefreshing = true;

            try {
                return onAsyncRefresh(_context, _name, _via);
            } catch(Throwable e) {
                TrackerManager.getInstance(_context.getApplicationContext()).logException(e);
            }

            if(!_doNotChangeState) {
                mRefreshing = false;
            }

            return null;
        }

        @Override
        protected void onPostExecute(Object data) {
            onAsyncRefreshFinished(_context, data);

            // check if data is valid and activity has not been destroyed by the main thread
            if(mActivity != null) {
                mActivity.getLoadingController().setLoading(false);
            }
        }

        @Override
        protected void onCancelled(Object o) {
            onAsyncRefreshCancelled(_context, o);

            if(mActivity != null) {
                mActivity.getLoadingController().setLoading(false);
            }
        }
    }

    protected void onAsyncRefreshStarted(Context context) { /* nothing to do */ }
    protected abstract Object onAsyncRefresh(Context context, String searchName, String searchVia);
    protected abstract void onAsyncRefreshCancelled(Context context, Object data);
    protected abstract void onAsyncRefreshFinished(Context context, Object data);

    // smiley face (avatar) random animations
    private final Random mRandom = new Random();
    private final Handler mHandler = new Handler();
    private final Runnable mAnimationRunnable = new Runnable() {
        @Override
        public void run() {
            List<AnimatedAvatarView> possibleAnimationsList = new ArrayList<>();

            // get all anonymous avatar instances
            for (int i = 0; i < getListView().getChildCount() - 1; i++) {
                AnimatedAvatarView v = (AnimatedAvatarView) getListView().getChildAt(i).findViewById(R.id.imgPhoto);
                if (!v.isShowStaticAvatar()) {
                    possibleAnimationsList.add(v);
                }
            }

            // randomly pick one
            int index = possibleAnimationsList.size() > 0 ? mRandom.nextInt(possibleAnimationsList.size()) : 0;

            // start the animation for the picked avatar and stop all others (avoids unnecessary drawing threads)
            for (int i = 0; i < possibleAnimationsList.size(); i++) {
                AnimatedAvatarView v = possibleAnimationsList.get(i);
                if (i == index) {
                    v.startDrawingThread();
                    v.resetAnimations();
                    v.startAnimations();
                } else {
                    v.stopDrawingThread();
                }
            }

            mHandler.postDelayed(mAnimationRunnable, FIXED_AVATAR_ANIMATION_INTERVAL_MS + mRandom.nextInt(VARIABLE_AVATAR_ANIMATION_INTERVAL_MS));
        }
    };

    public ContactListFragment() {
        this.mThreadPoolExecutor = new ThreadPoolExecutor(1, 1,
                60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>());
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mActivity = (ContactActivity) activity;
        mAddContactClickListener = mActivity;

        refresh();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        if(mRefreshing) {
            mActivity.getLoadingController().setLoading(true);
        }

        // hold popup
        mHoldPopup = new PopupWindow(mActivity);
        mHoldPopup.setContentView(inflater.inflate(R.layout.v_recipients_popup, null));
        // although this is deprecated, it is required for versions  < 22/23, otherwise the popup doesn't show up
        //noinspection deprecation
        mHoldPopup.setWindowLayoutMode(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        mHoldPopup.setBackgroundDrawable(Utils.getDrawable(mActivity, R.drawable.img_popup));
        mHoldPopup.setAnimationStyle(R.style.Peppermint_PopupAnimation);
        // do not let the popup get in the way of user interaction
        mHoldPopup.setFocusable(false);
        mHoldPopup.setTouchable(false);

        // inflate the view
        final View v = inflater.inflate(R.layout.f_recipients_layout, container, false);

        // init no recipients view
        final View.OnClickListener addContactClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mAddContactClickListener != null) {
                    mAddContactClickListener.onClick(v);
                }
            }
        };

        mListFooterView = (ViewGroup) inflater.inflate(R.layout.v_recipients_footer_layout, null);

        final Button btnAddContact = (Button) mListFooterView.findViewById(R.id.btnAddContact);
        btnAddContact.setId(0);
        btnAddContact.setOnClickListener(addContactClickListener);

        final Button btnAddContactEmpty = (Button) v.findViewById(R.id.btnAddContact);
        btnAddContactEmpty.setOnClickListener(addContactClickListener);

        return v;
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        getListView().setOnItemClickListener(this);
        getListView().setLongClickable(true);
        getListView().setOnItemLongClickListener(this);

        getListView().addFooterView(mListFooterView);

        mActivity.getSearchListBarView().addOnSearchListener(this, ContactActivity.SEARCH_LISTENER_PRIORITY_FRAGMENT);
    }

    @Override
    public void onStart() {
        super.onStart();

        mActivity.getLoadingController().setText(R.string.loading_contacts);

        mHandler.postDelayed(mAnimationRunnable, FIXED_AVATAR_ANIMATION_INTERVAL_MS + mRandom.nextInt(VARIABLE_AVATAR_ANIMATION_INTERVAL_MS));

        // global touch interceptor to hide keyboard
        mActivity.addTouchEventInterceptor(mTouchInterceptor);
    }

    @Override
    public void onStop() {
        mActivity.removeTouchEventInterceptor(mTouchInterceptor);

        mHandler.removeCallbacks(mAnimationRunnable);

        if(mRefreshTask != null && !mRefreshTask.isCancelled() && mRefreshTask.getStatus() != AsyncTask.Status.FINISHED) {
            mRefreshTask.cancel(true);
        }

        dismissHoldPopup();

        super.onStop();
    }

    @Override
    public void onDestroy() {
        if(mThreadPoolExecutor != null) {
            mThreadPoolExecutor.shutdown();
        }
        super.onDestroy();
    }

    @Override
    public void onDetach() {
        if(mActivity != null) {
            mActivity.getSearchListBarView().removeOnSearchListener(this);
            mActivity.getLoadingController().setLoading(false);
            mActivity = null;
            mAddContactClickListener = null;
        }

        super.onDetach();
    }

    @Override
    public boolean onSearch(String searchText, boolean wasClear) {
        if(mActivity == null) {
            return false;
        }

        if(mRefreshTask != null && !mRefreshTask.isCancelled() && mRefreshTask.getStatus() != AsyncTask.Status.FINISHED) {
            mRefreshTask._doNotChangeState = true;
            mRefreshTask.cancel(true);
            mRefreshTask = null;
        }

        mRefreshing = true;

        mRefreshTask = new RefreshTask(mActivity, searchText);
        mRefreshTask.executeOnExecutor(mThreadPoolExecutor);

        return false;
    }

    protected void refresh() {
        if(mActivity != null && mActivity.getSearchListBarView() != null) {
            onSearch(mActivity.getSearchListBarView().getSearchText(), false);
        }
    }

    protected void dismissHoldPopup() {
        if(mHoldPopup.isShowing() && !isDetached() && mActivity != null) {
            mHoldPopup.dismiss();
            mHandler.removeCallbacks(mDismissPopupRunnable);
        }
    }

    // the method that displays the img_popup.
    protected void showHoldPopup(View parent) {
        if(!mHoldPopup.isShowing() && !isDetached() && mActivity != null) {
            dismissHoldPopup();
            mHoldPopup.showAtLocation(parent, Gravity.NO_GRAVITY, mLastTouchPoint.x - Utils.dpToPx(mActivity, 120), mLastTouchPoint.y + Utils.dpToPx(mActivity, 10));
            mHandler.postDelayed(mDismissPopupRunnable, 6000);
        }
    }

    /**
     * When a footer or header is added to the ListView, the adapter set through setAdapter gets
     * wrapped by a HeaderViewListAdapter. This returns the wrapped adapter.
     * @return the real, wrapped adapter
     */
    protected ListAdapter getRealAdapter() {
        return getListView().getAdapter() == null ? null : ((HeaderViewListAdapter) getListView().getAdapter()).getWrappedAdapter();
    }

    private View.OnTouchListener mTouchInterceptor = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                dismissHoldPopup();
                mLastTouchPoint.set((int) event.getX(), (int) event.getY());
            }
            return false;
        }
    };

    public void setAddContactClickListener(View.OnClickListener mAddContactClickListener) {
        this.mAddContactClickListener = mAddContactClickListener;
    }
}
