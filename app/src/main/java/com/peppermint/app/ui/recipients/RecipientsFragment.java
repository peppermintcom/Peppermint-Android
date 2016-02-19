package com.peppermint.app.ui.recipients;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorSet;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.crashlytics.android.Crashlytics;
import com.peppermint.app.PeppermintApp;
import com.peppermint.app.R;
import com.peppermint.app.authenticator.AuthenticationData;
import com.peppermint.app.authenticator.AuthenticationPolicyEnforcer;
import com.peppermint.app.cloud.ReceiverEvent;
import com.peppermint.app.data.ChatManager;
import com.peppermint.app.data.DatabaseHelper;
import com.peppermint.app.data.Recipient;
import com.peppermint.app.data.RecipientManager;
import com.peppermint.app.data.RecipientType;
import com.peppermint.app.data.Recording;
import com.peppermint.app.tracking.TrackerManager;
import com.peppermint.app.ui.AnimatorBuilder;
import com.peppermint.app.ui.AnimatorChain;
import com.peppermint.app.ui.canvas.avatar.AnimatedAvatarView;
import com.peppermint.app.ui.canvas.loading.LoadingView;
import com.peppermint.app.ui.chat.ChatActivity;
import com.peppermint.app.ui.chat.ChatCursorAdapter;
import com.peppermint.app.ui.chat.ChatFragment;
import com.peppermint.app.ui.chat.ChatRecordOverlayFragment;
import com.peppermint.app.ui.recipients.add.NewRecipientActivity;
import com.peppermint.app.ui.recipients.add.NewRecipientFragment;
import com.peppermint.app.ui.views.SearchListBarAdapter;
import com.peppermint.app.ui.views.SearchListBarView;
import com.peppermint.app.ui.views.dialogs.PopupDialog;
import com.peppermint.app.ui.views.simple.CustomVisibilityListView;
import com.peppermint.app.utils.FilteredCursor;
import com.peppermint.app.utils.Utils;

import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RecipientsFragment extends ChatRecordOverlayFragment implements AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener,
        SearchListBarView.OnSearchListener {

    private static final String SCREEN_ID = "Contacts";

    public static final String FAST_REPLY_NAME_PARAM = "name";
    public static final String FAST_REPLY_MAIL_PARAM = "mail";

    public static final int REQUEST_NEWCONTACT = 224;

    // avatar animation frequency
    private static final int FIXED_AVATAR_ANIMATION_INTERVAL_MS = 7500;
    private static final int VARIABLE_AVATAR_ANIMATION_INTERVAL_MS = 7500;

    private AnimatorBuilder mAnimatorBuilder;
    private boolean mHasSavedInstanceState = false;
    private boolean mOnActivityResult = false;

    // the recipient list
    private View mRecipientListContainer;
    private View mRecipientLoadingContainer;
    private LoadingView mRecipientLoadingView;
    private RecipientCursorAdapter mRecipientAdapter;
    private Button mBtnAddContact;
    private ViewGroup mLytAddContactContainer;
    private ImageView mImgListBorder;
    private PopupDialog mTipPopup;
    private final Runnable mShowLoadingRunnable = new Runnable() {
        @Override
        public void run() {
            doLoading();
        }
    };

    private Point mLastTouchPoint = new Point();
    private PopupWindow mHoldPopup;
    private Runnable mDismissPopupRunnable = new Runnable() {
        @Override
        public void run() {
            dismissPopup();
        }
    };

    // the custom action bar (with recipient type filter and recipient search)
    private SearchListBarView mSearchListBarView;
    private SearchListBarAdapter<RecipientType> mRecipientTypeAdapter;

    // search
    private final Object mLock = new Object();
    private boolean mCreated = false;
    private GetRecipients mGetRecipientsTask;
    private static final Pattern mViaPattern = Pattern.compile("<([^\\s]*)>");

    private DatabaseHelper mDatabaseHelper;
    private SQLiteDatabase mDatabase;
    private ChatCursorAdapter mChatAdapter;

    private ThreadPoolExecutor mGetRecipientsExecutor;

    private SQLiteDatabase getDatabase() {
        if(mDatabase == null || !mDatabase.isOpen()) {
            mDatabase = mDatabaseHelper.getReadableDatabase();
        }
        return mDatabase;
    }

    private class GetRecipients extends AsyncTask<Void, Void, Object> {
        private RecipientType _recipientType;
        private String _filter;
        private String _name, _via;

        protected GetRecipients(String filter) {
            this._filter = filter;
        }

        @Override
        protected void onPreExecute() {
            mHandler.postDelayed(mShowLoadingRunnable, 100);

            _recipientType = (RecipientType) mSearchListBarView.getSelectedItem();

            if(_filter == null) {
                return;
            }

            String[] viaName = getSearchData(_filter);
            _via = viaName[0];
            _name = viaName[1];
        }

        @Override
        protected Object doInBackground(Void... nothing) {
            try {
                if (getCustomActionBarActivity() != null && ContextCompat.checkSelfPermission(getCustomActionBarActivity(),
                        Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {

                    if (_recipientType.isStarred() != null && _recipientType.isStarred()) {
                        // show chat list / recent contacts
                        ChatManager.deleteMissingRecipientChats(getCustomActionBarActivity(), getDatabase());
                        return ChatManager.getAllCursor(getDatabase());
                    }

                    // get normal full, email or phone contact list
                    FilteredCursor cursor = (FilteredCursor) RecipientManager.get(getCustomActionBarActivity(), null, _name, _recipientType.isStarred(), _recipientType.getMimeTypes(), _via);
                    if(cursor.getOriginalCursor().getCount() <= 0 && _name != null && _via != null) {
                        cursor = (FilteredCursor) RecipientManager.get(getCustomActionBarActivity(), null, null, _recipientType.isStarred(), _recipientType.getMimeTypes(), _via);
                    }
                    cursor.filter();

                    // for some reason this is invoked before onCreateView is complete on some devices
                    // thus, sleep and wait for a bit while onCreateView finishes to avoid crashes
                    synchronized (mLock) {
                        int i=0;    // avoid hanging the thread (just in case)
                        while(!mCreated && i < 10){
                            try {
                                i++;
                                mLock.wait(1000);
                            } catch (Exception e) {
                                // nothing to do here
                            }
                        }
                    }

                    return cursor;
                }
            } catch(Throwable e) {
                if(!(e instanceof InterruptedIOException)) {
                    Crashlytics.logException(e);
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(Object data) {
            // check if data is valid and activity has not been destroyed by the main thread
            if(data != null && getCustomActionBarActivity() != null && mCreated) {

                if (data instanceof FilteredCursor) {
                    // re-use adapter and replace cursor
                    mRecipientAdapter.changeCursor((Cursor) data);
                    mChatAdapter.changeCursor(null);

                    if(getListView().getAdapter() != mRecipientAdapter) {
                        // sync. trying to avoid detachFromGLContext errors
                        synchronized (mAnimationRunnable) {
                            getListView().setAdapter(mRecipientAdapter);
                        }
                    }

                    mRecipientAdapter.notifyDataSetChanged();
                } else {
                    // use new adapter
                    mChatAdapter.changeCursor((Cursor) data);
                    mRecipientAdapter.changeCursor(null);

                    if(getListView().getAdapter() != mChatAdapter) {
                        // sync. trying to avoid detachFromGLContext errors
                        synchronized (mAnimationRunnable) {
                            getListView().setAdapter(mChatAdapter);
                        }
                    }

                    mChatAdapter.notifyDataSetChanged();
                }
            }

            handleAddContactButtonVisibility();
            mHandler.removeCallbacks(mShowLoadingRunnable);
            if(mLoadingAnimatorChain != null) {
                mLoadingAnimatorChain.allowNext(false);
            }
        }

        @Override
        protected void onCancelled(Object o) {
            handleAddContactButtonVisibility();
            mHandler.removeCallbacks(mShowLoadingRunnable);
        }

        private void handleAddContactButtonVisibility() {
            if(mSearchListBarView.getSearchText() != null || mRecipientAdapter == null || mRecipientAdapter.getCount() <= 0) {
                mLytAddContactContainer.setVisibility(View.VISIBLE);
            } else {
                mLytAddContactContainer.setVisibility(View.GONE);
            }
        }
    }

    // smiley face (avatar) random animations
    private final Random mRandom = new Random();
    private final Handler mHandler = new Handler();
    private final Runnable mAnimationRunnable = new Runnable() {
        @Override
        public void run() {
            // sync. trying to avoid detachFromGLContext errors
            synchronized (this) {

                List<AnimatedAvatarView> possibleAnimationsList = new ArrayList<>();

                // get all anonymous avatar instances
                for (int i = 0; i < getListView().getChildCount(); i++) {
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
            }

            mHandler.postDelayed(mAnimationRunnable, FIXED_AVATAR_ANIMATION_INTERVAL_MS + mRandom.nextInt(VARIABLE_AVATAR_ANIMATION_INTERVAL_MS));
        }
    };

    // loading animation
    private AnimatorChain mLoadingAnimatorChain;
    private Animator.AnimatorListener mLoadingAnimatorListener = new Animator.AnimatorListener() {
        @Override
        public void onAnimationStart(Animator animation) {
            mRecipientLoadingView.startAnimations();
            mRecipientLoadingView.startDrawingThread();
            mRecipientLoadingContainer.setVisibility(View.VISIBLE);
        }
        @Override
        public void onAnimationEnd(Animator animation) {
            mRecipientLoadingContainer.setVisibility(View.INVISIBLE);
            mRecipientLoadingView.stopAnimations();
            mRecipientLoadingView.stopDrawingThread();
            mLoadingAnimatorChain = null;
        }
        @Override
        public void onAnimationCancel(Animator animation) { }
        @Override
        public void onAnimationRepeat(Animator animation) { }
    };

    private final Rect mBtnAddContactHitRect = new Rect();
    private PointF mTipPoint;

    private final Runnable mTipRunnable = new Runnable() {
        @Override
        public void run() {
            if (mTipPopup != null && getCustomActionBarActivity() != null && !mTipPopup.isShowing() && !isRemoving() && !getCustomActionBarActivity().isFinishing()) {
                if(getCustomActionBarActivity().getCustomActionBar().getHeight() <= 0) {
                    mHandler.postDelayed(mTipRunnable, 100);
                } else {
                    mTipPopup.show(getCustomActionBarActivity().getCustomActionBar());
                }
            }
        }
    };

    public RecipientsFragment() {
        this.mGetRecipientsExecutor = new ThreadPoolExecutor(1, 1,
                60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>());
    }

    private String[] getSearchData(String filter) {
        String[] viaName = new String[2];
        Matcher matcher = mViaPattern.matcher(filter);
        if (matcher.find()) {
            viaName[0] = matcher.group(1);
            viaName[1] = filter.replaceAll(mViaPattern.pattern(), "").trim();

            if(viaName[0].length() <= 0) {
                viaName[0] = null; // adjust filter to via so that only one (or no) result is shown
            }
        } else {
            viaName[1] = filter;
        }
        return viaName;
    }

    private AuthenticationPolicyEnforcer.AuthenticationDoneCallback mAuthenticationDoneCallback = new AuthenticationPolicyEnforcer.AuthenticationDoneCallback() {
        @Override
        public void done(AuthenticationData data) {
            if(getCustomActionBarActivity() == null) {
                // in case fragment has been detached and destroyed
                return;
            }

            getCustomActionBarActivity().getAuthenticationPolicyEnforcer().removeAuthenticationDoneCallback(this);

            if(getArguments() != null && (getArguments().containsKey(FAST_REPLY_NAME_PARAM) || getArguments().containsKey(FAST_REPLY_MAIL_PARAM))) {
                String name = getArguments().getString(FAST_REPLY_NAME_PARAM, "");
                String mail = getArguments().getString(FAST_REPLY_MAIL_PARAM, "");

                if(mail.length() <= 0) {
                    // if the email was not supplied, just search for the name
                    mSearchListBarView.setSearchText(name);
                } else {
                    // if mail is supplied, check if the contact exists
                    List<String> mimeTypes = new ArrayList<>();
                    mimeTypes.add(ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE);
                    mimeTypes.add(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE);
                    FilteredCursor checkCursor = (FilteredCursor) RecipientManager.get(getCustomActionBarActivity(), null, null, null, mimeTypes, mail);
                    boolean alreadyHasEmail = checkCursor != null && checkCursor.getOriginalCursor() != null && checkCursor.getOriginalCursor().getCount() > 0;

                    // if not, add the contact
                    if(!alreadyHasEmail) {
                        try {
                            if(RecipientManager.insert(getCustomActionBarActivity(), 0, name, null, null, mail, null, data.getEmail(), false) != null) {
                                alreadyHasEmail = true;
                            }
                        } catch (Exception e) {
                            /* nothing to do here */
                        }
                    }

                    // if it fails, add complete name+email search text to allow adding the full contact
                    // otherwise, just search by email
                    mSearchListBarView.setSearchText(alreadyHasEmail || name.length() <= 0 ? mail : name + " <" + mail + ">");
                }
            }
        }
    };

    private View.OnTouchListener mTouchInterceptor = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                // do not do it if the add contact button was pressed
                // since this might be causing the tap event to not work
                // the onClick event already has instructions to do this
                mBtnAddContact.getGlobalVisibleRect(mBtnAddContactHitRect);
                if (!mBtnAddContactHitRect.contains((int) event.getX(), (int) event.getY())) {
                    mSearchListBarView.removeSearchTextFocus(event);
                    getView().requestFocus();
                }

                dismissPopup();
                mLastTouchPoint.set((int) event.getX(), (int) event.getY());
                mTipPopup.dismiss();
            }
            return false;
        }
    };

    @SuppressWarnings("deprecation")
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mAnimatorBuilder = new AnimatorBuilder();
        mDatabaseHelper = new DatabaseHelper(activity);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        PeppermintApp app = (PeppermintApp) getCustomActionBarActivity().getApplication();

        mChatAdapter = new ChatCursorAdapter(getCustomActionBarActivity(), null, null, getCustomActionBarActivity().getTrackerManager());
        mRecipientAdapter = new RecipientCursorAdapter(app, getCustomActionBarActivity(), null);

        getCustomActionBarActivity().getAuthenticationPolicyEnforcer().addAuthenticationDoneCallback(mAuthenticationDoneCallback);

        // hold popup
        mHoldPopup = new PopupWindow(getCustomActionBarActivity());
        mHoldPopup.setContentView(inflater.inflate(R.layout.v_recipients_popup, null));
        //noinspection deprecation
        // although this is deprecated, it is required for versions  < 22/23, otherwise the popup doesn't show up
        mHoldPopup.setWindowLayoutMode(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        mHoldPopup.setBackgroundDrawable(Utils.getDrawable(getCustomActionBarActivity(), R.drawable.img_popup));
        mHoldPopup.setAnimationStyle(R.style.Peppermint_PopupAnimation);
        // do not let the popup get in the way of user interaction
        mHoldPopup.setFocusable(false);
        mHoldPopup.setTouchable(false);

        mTipPopup = new PopupDialog(getCustomActionBarActivity()) {
            @Override
            public boolean onTouchEvent(MotionEvent event) {
                mTipPoint = new PointF();
                mTipPoint.set(event.getRawX(), event.getRawY());
                return super.onTouchEvent(event);
            }
        };
        mTipPopup.setLayoutResource(R.layout.v_popup_tip1);
        mTipPopup.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                getPreferences().setFirstRun(false);
                if (mTipPoint != null) {
                    EditText searchEditText = mSearchListBarView.getSearchEditText();
                    if (searchEditText != null) {
                        Rect mSearchRect = new Rect();
                        searchEditText.getGlobalVisibleRect(mSearchRect);
                        searchEditText.setFocusableInTouchMode(true);
                        searchEditText.setFocusable(true);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            searchEditText.setShowSoftInputOnFocus(true);
                        }
                        if (mSearchRect.contains((int) mTipPoint.x, (int) mTipPoint.y) || (mTipPoint.y < 0 && mSearchRect.contains((int) mTipPoint.x, mSearchRect.centerY()))) {
                            searchEditText.requestFocus();
                            Utils.showKeyboard(getCustomActionBarActivity(), searchEditText, WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

                            // hack for some devices with Android < 5
                            searchEditText.dispatchTouchEvent(MotionEvent.obtain(
                                    SystemClock.uptimeMillis(),
                                    SystemClock.uptimeMillis() + 100,
                                    MotionEvent.ACTION_DOWN,
                                    mSearchRect.centerX(),
                                    mSearchRect.centerY(),
                                    0
                            ));

                            searchEditText.dispatchTouchEvent(MotionEvent.obtain(
                                    SystemClock.uptimeMillis() + 200,
                                    SystemClock.uptimeMillis() + 300,
                                    MotionEvent.ACTION_UP,
                                    mSearchRect.centerX(),
                                    mSearchRect.centerY(),
                                    0
                            ));
                        }
                    }
                }
            }
        });

        // inflate and init custom action bar view
        mSearchListBarView = (SearchListBarView) inflater.inflate(R.layout.f_recipients_actionbar, null, false);
        mRecipientTypeAdapter = new SearchListBarAdapter<>(app.getFontRegular(), getCustomActionBarActivity(), RecipientType.getAll(getCustomActionBarActivity()));
        mSearchListBarView.setListAdapter(mRecipientTypeAdapter);
        mSearchListBarView.setTypeface(app.getFontRegular());

        if (savedInstanceState != null) {
            mHasSavedInstanceState = true;
        }

        getCustomActionBarActivity().getCustomActionBar().setContents(mSearchListBarView, false);

        // inflate the view
        View v = inflater.inflate(R.layout.f_recipients_layout, container, false);

        // init no recipients view
        mImgListBorder = (ImageView) v.findViewById(R.id.imgListBorder);

        mLytAddContactContainer = (ViewGroup) v.findViewById(R.id.lytAddContactContainer);

        mBtnAddContact = (Button) v.findViewById(R.id.btnAddContact);
        mBtnAddContact.setTypeface(app.getFontSemibold());
        mBtnAddContact.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getCustomActionBarActivity(), NewRecipientActivity.class);

                String filter = mSearchListBarView.getSearchText();
                if (filter != null) {
                    String[] viaName = getSearchData(filter);
                    intent.putExtra(NewRecipientFragment.KEY_VIA, viaName[0]);

                    if (viaName[0] == null && (Utils.isValidPhoneNumber(viaName[1]) || Utils.isValidEmail(viaName[1]))) {
                        intent.putExtra(NewRecipientFragment.KEY_VIA, viaName[1]);
                    } else {
                        intent.putExtra(NewRecipientFragment.KEY_NAME, viaName[1]);
                    }
                }
                startActivityForResult(intent, REQUEST_NEWCONTACT);
            }
        });

        TextView txtEmpty1 = (TextView) v.findViewById(R.id.txtEmpty1);
        txtEmpty1.setTypeface(app.getFontSemibold());

        // init loading recipients view
        mRecipientLoadingContainer = v.findViewById(R.id.progressContainer);
        mRecipientLoadingView = (LoadingView) v.findViewById(R.id.loading);

        // init recipient list view
        mRecipientListContainer =  v.findViewById(R.id.listContainer);

        return v;
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // avoid showing "no contacts" for a split second, right after creation
        /*setListShownNoAnimation(false);*/
        doLoading();

        getCustomActionBarActivity().getOverlayManager().setRootScreenId(SCREEN_ID);
        getChatRecordOverlay().setViewToRemoveFocus(mSearchListBarView);

        getListView().setOnItemClickListener(this);
        getListView().setLongClickable(true);
        getListView().setOnItemLongClickListener(this);

        // apply visibility of list view to its border too
        // not using drawables to avoid overdraws
        ((CustomVisibilityListView) getListView()).setCanScrollListener(new CustomVisibilityListView.CanScrollListener() {
            @Override
            public synchronized void canScrollChanged(boolean canScroll, int visibility) {
                if (canScroll) {
                    mImgListBorder.setVisibility(visibility);
                } else {
                    mImgListBorder.setVisibility(View.GONE);
                }
            }
        });

        synchronized (mLock) {
            mCreated = true;
            mLock.notifyAll();
        }
    }

    @Override
    protected void sendMessage(Recipient recipient, Recording recording) {
        super.sendMessage(recipient, recording);

        // go back to recent contacts after sending a message
        mSearchListBarView.setSelectedItemPositionBeforeSearch(0);

        // if the user has gone through the sending process without
        // discarding the recording, then clear the search filter
        if(!mSearchListBarView.clearSearch(0)) {
            onSearch(mSearchListBarView.getSearchText());
        }

        launchChatActivity(recipient);
    }

    @Override
    public void onStart() {
        super.onStart();

        mChatAdapter.setDatabase(getDatabase());

        // global touch interceptor to hide keyboard
        getCustomActionBarActivity().addTouchEventInterceptor(mTouchInterceptor);

        if(!mOnActivityResult) {
            if (!mHasSavedInstanceState) {
                // default view is all contacts
                int selectedItemPosition = 0;
                if (!getPreferences().hasRecentContactUris()) {
                    // select "all contacts" in case there are no fav/recent contacts
                    selectedItemPosition = 1;
                }
                mSearchListBarView.setSelectedItemPosition(selectedItemPosition);
            }
        } else {
            mOnActivityResult = false;
        }

        onSearch(mSearchListBarView.getSearchText());
        mSearchListBarView.setOnSearchListener(this);

        mHandler.postDelayed(mAnimationRunnable, FIXED_AVATAR_ANIMATION_INTERVAL_MS + mRandom.nextInt(VARIABLE_AVATAR_ANIMATION_INTERVAL_MS));
    }

    @Override
    public void onResume() {
        super.onResume();

        // avoid cursor focus and keyboard when opening
        // if it is on onStart(), it doesn't work for screen rotations
        if(!mHasSavedInstanceState) {
            Utils.hideKeyboard(getCustomActionBarActivity(), WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        } else {
            mHasSavedInstanceState = false;
        }
        mSearchListBarView.removeSearchTextFocus(null);
        getView().requestFocus();

        if(getPreferences().isFirstRun()) {
            mHandler.postDelayed(mTipRunnable, 100);
        }

        TrackerManager.getInstance(getCustomActionBarActivity().getApplicationContext()).trackScreenView(SCREEN_ID);
    }

    @Override
    public void onStop() {
        // global touch interceptor to hide keyboard
        getCustomActionBarActivity().removeTouchEventInterceptor(mTouchInterceptor);

        mHandler.removeCallbacks(mAnimationRunnable);

        mSearchListBarView.setOnSearchListener(null);

        if(mGetRecipientsTask != null && !mGetRecipientsTask.isCancelled() && mGetRecipientsTask.getStatus() != AsyncTask.Status.FINISHED) {
            mGetRecipientsTask.cancel(true);
        }

        // close adapter cursors
        mChatAdapter.changeCursor(null);
        mRecipientAdapter.changeCursor(null);

        if(mDatabase != null) {
            mDatabase.close();
            mDatabase = null;
        }

        dismissPopup();

        super.onStop();
    }

    @Override
    public void onDestroy() {
        mSearchListBarView.deinit();
        mTipPopup.setOnDismissListener(null);
        mTipPopup.dismiss();
        super.onDestroy();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQUEST_NEWCONTACT) {
            mOnActivityResult = true;
            if(resultCode == Activity.RESULT_OK) {
                mSearchListBarView.setSearchText(data.getStringExtra(NewRecipientFragment.KEY_NAME));
            } else {
                mSearchListBarView.clearSearch(0);
            }
        }
    }

    @Override
    public void onReceivedMessage(ReceiverEvent event) {
        super.onReceivedMessage(event);
        if(mSearchListBarView.getSelectedItemPosition() == 0) {
            onSearch(mSearchListBarView.getSearchText());
        }
    }

    public int clearFilters() {
        if(mSearchListBarView.isShowingList()) {
            mSearchListBarView.hideList();
            return 2;
        }
        return (mSearchListBarView.clearSearch(0) ? 1 : 0);
    }

    protected void doLoading() {
        if(mLoadingAnimatorChain != null) {
            return;
        }

        Animator fadeOut = mAnimatorBuilder.buildFadeOutAnimator(400, mRecipientListContainer);
        Animator fadeIn = mAnimatorBuilder.buildFadeInAnimator(400, mRecipientLoadingContainer);
        AnimatorSet startLoadingSet = new AnimatorSet();
        startLoadingSet.playTogether(fadeOut, fadeIn);

        fadeOut = mAnimatorBuilder.buildFadeOutAnimator(600, mRecipientLoadingContainer);
        fadeIn = mAnimatorBuilder.buildFadeInAnimator(600, mRecipientListContainer);
        AnimatorSet stopLoadingSet = new AnimatorSet();
        stopLoadingSet.playTogether(fadeOut, fadeIn);

        mLoadingAnimatorChain = new AnimatorChain(startLoadingSet, stopLoadingSet);
        mLoadingAnimatorChain.setAnimatorListener(mLoadingAnimatorListener);
        mLoadingAnimatorChain.start();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
        Intent intent = new Intent(getCustomActionBarActivity(), ChatActivity.class);

        if(getListView().getAdapter() instanceof RecipientCursorAdapter) {
            showPopup(view);
        } else {
            intent.putExtra(ChatFragment.PARAM_RECIPIENT_ID, mChatAdapter.getChat(position).getMainRecipientId());
            startActivity(intent);
        }
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        Recipient tappedRecipient = getListView().getAdapter() instanceof RecipientCursorAdapter ?
                mRecipientAdapter.getRecipient(position) :
                mChatAdapter.getChat(position).getMainRecipientParameter();
        return triggerRecording(view, tappedRecipient);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        onSearch(mSearchListBarView.getSearchText());
    }

    @Override
    public void onSearch(String filter) {
        if(mGetRecipientsTask != null && !mGetRecipientsTask.isCancelled() && mGetRecipientsTask.getStatus() != AsyncTask.Status.FINISHED) {
            mGetRecipientsTask.cancel(true);
            mGetRecipientsTask = null;
        }

        mGetRecipientsTask = new GetRecipients(filter);
        mGetRecipientsTask.executeOnExecutor(mGetRecipientsExecutor);
    }

    private void dismissPopup() {
        if(mHoldPopup.isShowing() && !isDetached() && getCustomActionBarActivity() != null) {
            mHoldPopup.dismiss();
            mHandler.removeCallbacks(mDismissPopupRunnable);
        }
    }

    // the method that displays the img_popup.
    private void showPopup(View parent) {
        if(!mHoldPopup.isShowing() && !isDetached() && getCustomActionBarActivity() != null) {
            dismissPopup();
            mHoldPopup.showAtLocation(parent, Gravity.NO_GRAVITY, mLastTouchPoint.x - Utils.dpToPx(getCustomActionBarActivity(), 120), mLastTouchPoint.y + Utils.dpToPx(getCustomActionBarActivity(), 10));
            mHandler.postDelayed(mDismissPopupRunnable, 6000);
        }
    }
}
