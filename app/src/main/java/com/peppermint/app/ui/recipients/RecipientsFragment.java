package com.peppermint.app.ui.recipients;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorSet;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
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
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.crashlytics.android.Crashlytics;
import com.peppermint.app.PeppermintApp;
import com.peppermint.app.R;
import com.peppermint.app.authenticator.AuthenticationData;
import com.peppermint.app.authenticator.AuthenticationPolicyEnforcer;
import com.peppermint.app.data.Recipient;
import com.peppermint.app.data.RecipientType;
import com.peppermint.app.data.Recording;
import com.peppermint.app.tracking.TrackerManager;
import com.peppermint.app.ui.AnimatorBuilder;
import com.peppermint.app.ui.canvas.avatar.AnimatedAvatarView;
import com.peppermint.app.ui.canvas.loading.LoadingView;
import com.peppermint.app.ui.chat.ChatActivity;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
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

    // the recipient list
    private View mRecipientListContainer;
    private View mRecipientLoadingContainer;
    private LoadingView mRecipientLoadingView;
    private boolean mRecipientListShown;
    private BaseAdapter mRecipientAdapter;
    private Button mBtnAddContact;
    private ImageView mImgListBorder;
    private PopupDialog mTipPopup;
    private final Runnable mShowLoadingRunnable = new Runnable() {
        @Override
        public void run() {
            setListShown(false);
        }
    };

    private Point mLastTouchPoint = new Point();

    // the custom action bar (with recipient type filter and recipient search)
    private SearchListBarView mSearchListBarView;
    private SearchListBarAdapter<RecipientType> mRecipientTypeAdapter;

    // search
    private final Object mLock = new Object();
    private boolean mCreated = false;
    private GetRecipients mGetRecipientsTask;
    private static final Pattern mViaPattern = Pattern.compile("<([^\\s]*)>");

    private class GetRecipients extends AsyncTask<Void, Void, Object> {
        private RecipientType _recipientType;
        private String _filter;
        private String _name, _via;

        protected GetRecipients(String filter) {
            this._filter = filter;
        }

        @Override
        protected void onPreExecute() {
            mHandler.postDelayed(mShowLoadingRunnable, 250);
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

                    PeppermintApp app = (PeppermintApp) getCustomActionBarActivity().getApplication();

                    if (_recipientType.isStarred() != null && _recipientType.isStarred()) {
                        List<Long> recentList = getPreferences().getRecentContactUris();
                        if(recentList == null) {
                            // empty list to show no contacts
                            recentList = new ArrayList<>();
                        }

                        // get recent contact list
                        Map<Long, Recipient> recipientMap = new HashMap<>();

                        if(recentList.size() > 0) {
                            Cursor cursor = RecipientAdapterUtils.getRecipientsCursor(getCustomActionBarActivity(), recentList, null, null, null, null);
                            Set<Long> allowedSet = new HashSet<>();
                            while (cursor.moveToNext()) {
                                Recipient recipient = RecipientAdapterUtils.getRecipient(cursor);
                                // this if removes deleted/invalid contacts from the list
                                if (recipient.getVia() != null && recipient.getVia().trim().length() > 0) {
                                    recipientMap.put(recipient.getContactId(), recipient);
                                    allowedSet.add(recipient.getContactId());
                                }
                            }
                            cursor.close();

                            // remove invalid contacts from recent list
                            if (recentList.size() != allowedSet.size()) {
                                Iterator<Long> it = recentList.iterator();
                                while (it.hasNext()) {
                                    if (!allowedSet.contains(it.next())) {
                                        it.remove();
                                    }
                                }
                                getPreferences().setRecentContactUris(recentList);
                            }
                        }

                        return new RecipientArrayAdapter(app, getCustomActionBarActivity(), recipientMap, recentList);
                    }

                    // get normal full, email or phone contact list
                    FilteredCursor cursor = (FilteredCursor) RecipientAdapterUtils.getRecipientsCursor(getCustomActionBarActivity(), null, _name, _recipientType.isStarred(), _recipientType.getMimeTypes(), _via);
                    if(cursor.getOriginalCursor().getCount() <= 0 && _name != null && _via != null) {
                        cursor = (FilteredCursor) RecipientAdapterUtils.getRecipientsCursor(getCustomActionBarActivity(), null, null, _recipientType.isStarred(), _recipientType.getMimeTypes(), _via);
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
                PeppermintApp app = (PeppermintApp) getCustomActionBarActivity().getApplication();

                if (data instanceof Cursor) {
                    // re-use adapter and replace cursor
                    if (mRecipientAdapter != null && mRecipientAdapter instanceof CursorAdapter) {
                        ((CursorAdapter) mRecipientAdapter).changeCursor((Cursor) data);
                    } else {
                        mRecipientAdapter = new RecipientCursorAdapter(app, getCustomActionBarActivity(), (Cursor) data);
                        // sync. trying to avoid detachFromGLContext errors
                        synchronized(mAnimationRunnable) {
                            getListView().setAdapter(mRecipientAdapter);
                        }
                    }
                } else {
                    // use new adapter
                    if (mRecipientAdapter != null && mRecipientAdapter instanceof CursorAdapter) {
                        ((CursorAdapter) mRecipientAdapter).changeCursor(null);
                    }
                    mRecipientAdapter = (BaseAdapter) data;
                    // sync. trying to avoid detachFromGLContext errors
                    synchronized(mAnimationRunnable) {
                        getListView().setAdapter(mRecipientAdapter);
                    }
                }
                mRecipientAdapter.notifyDataSetChanged();
            }

            handleAddContactButtonVisibility();
            mHandler.removeCallbacks(mShowLoadingRunnable);
            setListShown(true);
        }

        @Override
        protected void onCancelled(Object o) {
            handleAddContactButtonVisibility();
            mHandler.removeCallbacks(mShowLoadingRunnable);
            setListShown(true);
        }

        private void handleAddContactButtonVisibility() {
            if(mSearchListBarView.getSearchText() != null || mRecipientAdapter == null || mRecipientAdapter.getCount() <= 0) {
                mBtnAddContact.setVisibility(View.VISIBLE);
            } else {
                mBtnAddContact.setVisibility(View.GONE);
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
    private AnimatorSet mLoadingAnimator;
    private Animator.AnimatorListener mLoadingAnimatorListener = new Animator.AnimatorListener() {
        @Override
        public void onAnimationStart(Animator animation) {
            if(mRecipientListShown) {
                mRecipientListContainer.setVisibility(View.VISIBLE);
            } else {
                mRecipientLoadingView.startAnimations();
                mRecipientLoadingView.startDrawingThread();
                mRecipientLoadingContainer.setVisibility(View.VISIBLE);
            }
        }
        @Override
        public void onAnimationEnd(Animator animation) {
            if(mRecipientListShown) {
                mRecipientLoadingContainer.setVisibility(View.INVISIBLE);
                mRecipientLoadingView.stopAnimations();
                mRecipientLoadingView.stopDrawingThread();
            } else {
                mRecipientListContainer.setVisibility(View.INVISIBLE);
            }
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
                    FilteredCursor checkCursor = (FilteredCursor) RecipientAdapterUtils.getRecipientsCursor(getCustomActionBarActivity(), null, null, null, mimeTypes, mail);
                    boolean alreadyHasEmail = checkCursor != null && checkCursor.getOriginalCursor() != null && checkCursor.getOriginalCursor().getCount() > 0;

                    // if not, add the contact
                    if(!alreadyHasEmail) {
                        Bundle bundle = NewRecipientFragment.insertRecipientContact(getCustomActionBarActivity(), 0, name, null, mail, null, data.getEmail());
                        // will fail if there's no name or if the email is invalid
                        if(!bundle.containsKey(NewRecipientFragment.KEY_ERROR)) {
                            alreadyHasEmail = true;
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
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        PeppermintApp app = (PeppermintApp) getCustomActionBarActivity().getApplication();

        getCustomActionBarActivity().getAuthenticationPolicyEnforcer().addAuthenticationDoneCallback(mAuthenticationDoneCallback);

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
        } else {
            // default view is all contacts
            int selectedItemPosition = 0;
            if(!hasRecents()) {
                // select "all contacts" in case there are not fav/recent contacts
                selectedItemPosition = 1;
            }
            mSearchListBarView.setSelectedItemPosition(selectedItemPosition);
        }

        getCustomActionBarActivity().getCustomActionBar().setContents(mSearchListBarView, false);

        // inflate the view
        View v = inflater.inflate(R.layout.f_recipients_layout, container, false);

        // init no recipients view
        mImgListBorder = (ImageView) v.findViewById(R.id.imgListBorder);

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
        mRecipientListShown = true;
        mRecipientLoadingContainer = v.findViewById(R.id.progressContainer);
        mRecipientLoadingView = (LoadingView) v.findViewById(R.id.loading);

        // init recipient list view
        mRecipientListContainer =  v.findViewById(R.id.listContainer);

        // avoid showing "no contacts" for a split second, right after creation
        setListShownNoAnimation(false);

        return v;
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

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

        launchChatActivity(null, recipient);
    }

    @Override
    public void onStart() {
        super.onStart();

        // global touch interceptor to hide keyboard
        getCustomActionBarActivity().addTouchEventInterceptor(mTouchInterceptor);

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

        super.onStop();
    }

    @Override
    public void onDestroy() {
        mSearchListBarView.deinit();
        if(mRecipientAdapter != null && mRecipientAdapter instanceof RecipientCursorAdapter) {
            // this closes the cursor inside the adapter
            ((RecipientCursorAdapter) mRecipientAdapter).changeCursor(null);
        }
        mTipPopup.setOnDismissListener(null);
        mTipPopup.dismiss();
        super.onDestroy();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQUEST_NEWCONTACT) {
            if(resultCode == Activity.RESULT_OK) {
                mSearchListBarView.setSearchText(data.getStringExtra(NewRecipientFragment.KEY_NAME));
            } else {
                mSearchListBarView.clearSearch(0);
            }
        }
    }

    public int clearFilters() {
        if(mSearchListBarView.isShowingList()) {
            mSearchListBarView.hideList();
            return 2;
        }
        return (mSearchListBarView.clearSearch(0) ? 1 : 0);
    }

    protected void setListShown(boolean shown, boolean animate){
        if (mRecipientListShown == shown) {
            return;
        }

        if(mLoadingAnimator != null && mLoadingAnimator.isRunning()) {
            mLoadingAnimator.cancel();
            mLoadingAnimator.removeAllListeners();
            mLoadingAnimatorListener.onAnimationEnd(null);
        }

        mRecipientListShown = shown;

        if (animate && getCustomActionBarActivity() != null) {
            Animator fadeOut = mAnimatorBuilder.buildFadeOutAnimator(shown ? mRecipientLoadingContainer : mRecipientListContainer);
            Animator fadeIn = mAnimatorBuilder.buildFadeInAnimator(shown ? mRecipientListContainer : mRecipientLoadingContainer);
            mLoadingAnimator = new AnimatorSet();
            mLoadingAnimator.addListener(mLoadingAnimatorListener);
            mLoadingAnimator.playTogether(fadeOut, fadeIn);
            mLoadingAnimator.start();
        } else {
            mLoadingAnimatorListener.onAnimationStart(null);
            mLoadingAnimatorListener.onAnimationEnd(null);
        }
    }

    @Override
    public void setListShown(boolean shown){
        setListShown(shown, true);
    }

    @Override
    public void setListShownNoAnimation(boolean shown) {
        setListShown(shown, false);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
        Intent intent = new Intent(getCustomActionBarActivity(), ChatActivity.class);
        intent.putExtra(ChatFragment.PARAM_RECIPIENT, mRecipientAdapter instanceof RecipientCursorAdapter ?
                ((RecipientCursorAdapter) mRecipientAdapter).getRecipient(position) :
                ((RecipientArrayAdapter) mRecipientAdapter).getItem(position));
        startActivity(intent);
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        Recipient tappedRecipient = mRecipientAdapter instanceof RecipientCursorAdapter ?
                ((RecipientCursorAdapter) mRecipientAdapter).getRecipient(position) :
                ((RecipientArrayAdapter) mRecipientAdapter).getItem(position);
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
        }

        mGetRecipientsTask = new GetRecipients(filter);
        mGetRecipientsTask.execute();
    }

    private boolean hasRecents() {
        List<Long> recentList = getPreferences().getRecentContactUris();
        if(recentList != null && recentList.size() > 0) {
            return true;
        }

        return false;
    }
}
