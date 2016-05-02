package com.peppermint.app.ui.recipients;

import android.app.Activity;
import android.app.ListFragment;
import android.content.Context;
import android.content.Intent;
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
import android.widget.PopupWindow;

import com.peppermint.app.R;
import com.peppermint.app.data.Chat;
import com.peppermint.app.data.ContactRaw;
import com.peppermint.app.data.Message;
import com.peppermint.app.data.Recording;
import com.peppermint.app.events.MessageEvent;
import com.peppermint.app.events.PeppermintEventBus;
import com.peppermint.app.events.ReceiverEvent;
import com.peppermint.app.events.SenderEvent;
import com.peppermint.app.events.SyncEvent;
import com.peppermint.app.tracking.TrackerManager;
import com.peppermint.app.ui.canvas.avatar.AnimatedAvatarView;
import com.peppermint.app.ui.chat.ChatActivity;
import com.peppermint.app.ui.chat.recorder.ChatRecordOverlayController;
import com.peppermint.app.ui.recipients.add.NewContactActivity;
import com.peppermint.app.utils.ResourceUtils;
import com.peppermint.app.utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class ContactListFragment extends ListFragment implements
        AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener,
        SearchListBarView.OnSearchListener, View.OnClickListener {

    protected static final int REQUEST_NEWCONTACT = 224;

    private static final Pattern VIA_PATTERN = Pattern.compile("<([^\\s]*)>");

    // avatar animation frequency
    private static final int FIXED_AVATAR_ANIMATION_INTERVAL_MS = 7500;
    private static final int VARIABLE_AVATAR_ANIMATION_INTERVAL_MS = 7500;

    protected ContactActivity mActivity;
    protected ChatRecordOverlayController mChatRecordOverlayController;

    // UI
    private SearchListBarView mSearchListBarView;
    private ViewGroup mListFooterView;

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
            if(mActivity != null && mActivity.getLoadingController() != null) {
                mActivity.getLoadingController().setLoading(true);
            }

            onAsyncRefreshStarted(_context);

            if(_filter == null) {
                return;
            }

            String[] viaName = getFilterData(_filter);
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
            if(mActivity != null && mActivity.getLoadingController() != null) {
                mActivity.getLoadingController().setLoading(false);
            }
        }

        @Override
        protected void onCancelled(Object o) {
            onAsyncRefreshCancelled(_context, o);

            if(mActivity != null && mActivity.getLoadingController() != null) {
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

    private Object mMessageEventListener = new Object() {
        public void onEventMainThread(MessageEvent event) {
            if(event.getType() == MessageEvent.EVENT_MARK_PLAYED) {
                refresh();
            }
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
        mSearchListBarView = mActivity.getSearchListBarView();

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
        mHoldPopup.setBackgroundDrawable(ResourceUtils.getDrawable(mActivity, R.drawable.img_popup));
        mHoldPopup.setAnimationStyle(R.style.Peppermint_PopupAnimation);
        // do not let the popup get in the way of user interaction
        mHoldPopup.setFocusable(false);
        mHoldPopup.setTouchable(false);

        // inflate the view
        final View v = inflater.inflate(R.layout.f_recipients_layout, container, false);

        // init no recipients view
        mListFooterView = (ViewGroup) inflater.inflate(R.layout.v_recipients_footer_layout, null);

        final Button btnAddContact = (Button) mListFooterView.findViewById(R.id.btnAddContact);
        btnAddContact.setId(0);
        btnAddContact.setOnClickListener(this);

        final Button btnAddContactEmpty = (Button) v.findViewById(R.id.btnAddContact);
        btnAddContactEmpty.setOnClickListener(this);

        return v;
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        getListView().setOnItemClickListener(this);
        getListView().setLongClickable(true);
        getListView().setOnItemLongClickListener(this);

        getListView().addFooterView(mListFooterView);

        mSearchListBarView = mActivity.getSearchListBarView();
        mSearchListBarView.addOnSearchListener(this, ContactActivity.SEARCH_LISTENER_PRIORITY_FRAGMENT);

        // chat record overlay controller
        mChatRecordOverlayController = new ChatRecordOverlayController(mActivity) {
            @Override
            public void onEventMainThread(SyncEvent event) {
                super.onEventMainThread(event);
                if(event.getType() == SyncEvent.EVENT_FINISHED) {
                    if(mSearchListBarView.getSearchText() == null) {
                        refresh();
                    }
                }
            }

            @Override
            public void onEventMainThread(ReceiverEvent event) {
                super.onEventMainThread(event);
                if(event.getType() == ReceiverEvent.EVENT_RECEIVED) {
                    if(mSearchListBarView.getSearchText() == null) {
                        refresh();
                    }
                }
            }

            @Override
            public void onEventMainThread(SenderEvent event) {
                super.onEventMainThread(event);
                if(event.getType() == SenderEvent.EVENT_FINISHED) {
                    if(mSearchListBarView.getSearchText() == null) {
                        refresh();
                    }
                }
            }

            @Override
            protected Message sendMessage(Chat chat, Recording recording) {
                Message message = super.sendMessage(chat, recording);

                // launch chat activity
                long chatId = message.getChatParameter().getPeppermintChatId() > 0 ? message.getChatParameter().getPeppermintChatId() : message.getChatParameter().getId();
                Intent chatIntent = new Intent(mActivity, ChatActivity.class);
                chatIntent.putExtra(ChatActivity.PARAM_CHAT_ID, chatId);
                startActivity(chatIntent);

                // if the user has gone through the sending process without
                // discarding the recording, then clear the search filter
                mSearchListBarView.clearSearch(true);

                return message;
            }
        };

        mChatRecordOverlayController.init(getListView(), mActivity.getOverlayManager(),
                mActivity, savedInstanceState);

        PeppermintEventBus.registerMessages(mMessageEventListener);
    }

    @Override
    public void onStart() {
        super.onStart();

        mChatRecordOverlayController.start();

        mActivity.getLoadingController().setText(R.string.loading_contacts);

        mHandler.postDelayed(mAnimationRunnable, FIXED_AVATAR_ANIMATION_INTERVAL_MS + mRandom.nextInt(VARIABLE_AVATAR_ANIMATION_INTERVAL_MS));

        // global touch interceptor to hide keyboard
        mActivity.addTouchEventInterceptor(mTouchInterceptor);
    }

    @Override
    public void onStop() {
        mActivity.removeTouchEventInterceptor(mTouchInterceptor);

        mHandler.removeCallbacks(mAnimationRunnable);

        dismissHoldPopup();

        mChatRecordOverlayController.stop();

        super.onStop();
    }

    @Override
    public void onDestroy() {
        PeppermintEventBus.unregisterMessages(mMessageEventListener);

        if(mRefreshTask != null) {
            mRefreshTask.cancel(true);
            mRefreshTask = null;
        }

        if(mThreadPoolExecutor != null) {
            mThreadPoolExecutor.shutdown();
        }

        mChatRecordOverlayController.deinit();
        mChatRecordOverlayController.setContext(null);

        super.onDestroy();
    }

    @Override
    public void onDetach() {
        if(mActivity != null) {
            mSearchListBarView.removeOnSearchListener(this);
            mActivity.getLoadingController().setLoading(false);
            mActivity = null;
        }

        super.onDetach();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == REQUEST_NEWCONTACT) {
            if(resultCode == Activity.RESULT_OK) {
                ContactRaw contact = (ContactRaw) data.getSerializableExtra(NewContactActivity.KEY_RECIPIENT);
                if(contact != null) {
                    mSearchListBarView.setSearchText(contact.getDisplayName());
                }
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        mChatRecordOverlayController.saveInstanceState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onSearch(String searchText, boolean wasClear) {
        if(mActivity == null) {
            return false;
        }

        if(mRefreshTask != null) {
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
        if(mActivity != null) {
            onSearch(mSearchListBarView != null ? mSearchListBarView.getSearchText() : null, false);
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

    @Override
    public void onClick(View v) {
        Intent intent = new Intent(mActivity, NewContactActivity.class);

        String filter = mSearchListBarView.getSearchText();
        if (filter != null) {
            String[] viaName = getFilterData(filter);
            intent.putExtra(NewContactActivity.KEY_VIA, viaName[0]);

            if (viaName[0] == null && (Utils.isValidPhoneNumber(viaName[1]) || Utils.isValidEmail(viaName[1]))) {
                intent.putExtra(NewContactActivity.KEY_VIA, viaName[1]);
            } else {
                intent.putExtra(NewContactActivity.KEY_NAME, viaName[1]);
            }
        }

        startActivityForResult(intent, REQUEST_NEWCONTACT);
    }

    private static String[] getFilterData(String filter) {
        String[] viaName = new String[2];
        Matcher matcher = VIA_PATTERN.matcher(filter);
        if (matcher.find()) {
            viaName[0] = matcher.group(1);
            viaName[1] = filter.replaceAll(VIA_PATTERN.pattern(), "").trim();

            if(viaName[0].length() <= 0) {
                viaName[0] = null; // adjust filter to via so that only one (or no) result is shown
            }
        } else {
            viaName[1] = filter;
        }
        return viaName;
    }
}
