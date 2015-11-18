package com.peppermint.app.ui.recipients;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorSet;
import android.app.Activity;
import android.app.ListFragment;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Point;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.text.Html;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.peppermint.app.PeppermintApp;
import com.peppermint.app.R;
import com.peppermint.app.RecordService;
import com.peppermint.app.RecordServiceManager;
import com.peppermint.app.SenderServiceManager;
import com.peppermint.app.data.Recipient;
import com.peppermint.app.data.RecipientType;
import com.peppermint.app.data.Recording;
import com.peppermint.app.ui.CustomActionBarActivity;
import com.peppermint.app.ui.canvas.avatar.AnimatedAvatarView;
import com.peppermint.app.ui.canvas.progress.LoadingView;
import com.peppermint.app.ui.views.CustomConfirmationDialog;
import com.peppermint.app.ui.views.RecordingOverlayView;
import com.peppermint.app.ui.views.SearchListBarAdapter;
import com.peppermint.app.ui.views.SearchListBarView;
import com.peppermint.app.utils.AnimatorBuilder;
import com.peppermint.app.utils.FilteredCursor;
import com.peppermint.app.utils.NoMicDataIOException;
import com.peppermint.app.utils.PepperMintPreferences;
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

public class RecipientsFragment extends ListFragment implements AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener, View.OnTouchListener,
        SearchListBarView.OnSearchListener, RecordServiceManager.Listener {

    // recording overlay
    private static final String RECORDING_OVERLAY_TAG = "RECORDING";
    private static final int RECORDING_OVERLAY_HIDE_DELAY = 1000;
    private static final long MAX_DURATION_MILLIS = 600000; // 10min

    public static final String FAST_REPLY_NAME_PARAM = "name";
    public static final String FAST_REPLY_MAIL_PARAM = "mail";

    // keys to save the instance state
    private static final String RECIPIENT_TYPE_POS_KEY = "RecipientsFragment_RecipientTypePosition";
    private static final String RECIPIENT_TYPE_SEARCH_KEY = "RecipientsFragment_RecipientTypeSearch";
    private static final String RECORDING_FINAL_EVENT_KEY = "RecipientsFragment_RecordingFinalEvent";
    private static final String SMS_CONFIRMATION_STATE_KEY = "RecipientsFragment_SmsConfirmationState";

    // avatar animation frequency
    private static final int FIXED_AVATAR_ANIMATION_INTERVAL_MS = 15000;
    private static final int VARIABLE_AVATAR_ANIMATION_INTERVAL_MS = 15000;

    private PepperMintPreferences mPreferences;
    private CustomActionBarActivity mActivity;
    private AnimatorBuilder mAnimatorBuilder;
    private RecordingOverlayView mRecordingViewOverlay;
    private boolean mSendRecording = false;
    private boolean mDestroyed = false;

    // swipe-related
    private float x1, x2, y1, y2;
    private long t1, t2;
    private int mMinSwipeDistance;
    private static final int MIN_SWIPE_DISTANCE_DP = 60;        // min swipe distance
    private static final int MAX_SWIPE_DURATION = 300;        // max swipe duration

    private MediaPlayer mRecordSoundPlayer;
    private CustomConfirmationDialog mSmsConfirmationDialog;
    private RecordService.Event mFinalEvent;

    // the recipient list
    private View mRecipientListContainer;
    private View mRecipientLoadingContainer;
    private LoadingView mRecipientLoadingView;
    private boolean mRecipientListShown;
    private BaseAdapter mRecipientAdapter;

    // new recipient view
    private EditText mTxtNewName, mTxtNewContact;
    private ImageButton mBtnNew;
    private AnimatedAvatarView mNewAvatarView;

    private PopupWindow mHoldPopup;
    private Point mLastTouchPoint = new Point();
    private Runnable mDismissPopupRunnable = new Runnable() {
        @Override
        public void run() {
            dismissPopup();
        }
    };

    // the custom action bar (with recipient type filter and recipient search)
    private SearchListBarView mSearchListBarView;
    private SearchListBarAdapter<RecipientType> mRecipientTypeAdapter;

    // recording service
    private RecordServiceManager mRecordManager;

    // bottom bar
    private SenderServiceManager mSenderServiceManager;
    private SenderControlLayout mLytSenderControl;

    // search
    private Object mLock = new Object();
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
            setListShown(false);
            _recipientType = (RecipientType) mSearchListBarView.getSelectedItem();

            if(_filter == null) {
                return;
            }

            Matcher matcher = mViaPattern.matcher(_filter);
            if (matcher.find()) {
                _via = matcher.group(1);
                _name = _filter.replaceAll(mViaPattern.pattern(), "").trim();

                if(_via.length() <= 0) {
                    _via = null; // adjust filter to via so that only one (or no) result is shown
                }
            } else {
                _name = _filter;
            }
        }

        @Override
        protected Object doInBackground(Void... nothing) {
            try {
                if (mActivity != null && ContextCompat.checkSelfPermission(mActivity,
                        Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {

                    List<Long> recentList = mPreferences.getRecentContactUris();

                    if (recentList != null && recentList.size() > 0 && _recipientType.isStarred() != null && _recipientType.isStarred()) {

                        Map<Long, Recipient> recipientMap = new HashMap<>();

                        Cursor cursor = RecipientAdapterUtils.getRecipientsCursor(mActivity, recentList, null, null, null, null);
                        Set<Long> allowedSet = new HashSet<>();
                        while(cursor.moveToNext()) {
                            Recipient recipient = RecipientAdapterUtils.getRecipient(cursor);
                            // this if removes deleted/invalid contacts from the list
                            if(recipient.getVia() != null && recipient.getVia().trim().length() > 0) {
                                recipientMap.put(recipient.getContactId(), recipient);
                                allowedSet.add(recipient.getContactId());
                            }
                        }
                        cursor.close();

                        // remove invalid contacts from recent list
                        if(recentList.size() != allowedSet.size()) {
                            Iterator<Long> it = recentList.iterator();
                            while (it.hasNext()) {
                                if (!allowedSet.contains(it.next())) {
                                    it.remove();
                                }
                            }
                            mPreferences.setRecentContactUris(recentList);
                        }

                        if(recentList.size() > 0) {
                            return new RecipientArrayAdapter((PeppermintApp) mActivity.getApplication(), mActivity, recipientMap, recentList);
                        }
                    }

                    FilteredCursor cursor = (FilteredCursor) RecipientAdapterUtils.getRecipientsCursor(mActivity, null, _name, _recipientType.isStarred(), _recipientType.getMimeTypes(), _via);
                    if(cursor.getOriginalCursor().getCount() <= 0 && _name != null && _via != null) {
                        cursor = (FilteredCursor) RecipientAdapterUtils.getRecipientsCursor(mActivity, null, null, _recipientType.isStarred(), _recipientType.getMimeTypes(), _via);
                    }
                    cursor.filter();

                    synchronized (mLock) {
                        int i=0;    // avoid hanging the thread (just in case)
                        while(!mCreated && i < 10){
                            try {
                                i++;
                                mLock.wait(1000);
                            } catch (InterruptedException e) {
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
            if(data != null && mActivity != null && mCreated) {
                if (data instanceof Cursor) {
                    // use cursor
                    if (mRecipientAdapter != null && mRecipientAdapter instanceof CursorAdapter) {
                        ((CursorAdapter) mRecipientAdapter).changeCursor((Cursor) data);
                    } else {
                        mRecipientAdapter = new RecipientCursorAdapter((PeppermintApp) mActivity.getApplication(), mActivity, (Cursor) data);
                        getListView().setAdapter(mRecipientAdapter);
                    }
                } else {
                    // use adapter
                    if (mRecipientAdapter != null && mRecipientAdapter instanceof CursorAdapter) {
                        ((CursorAdapter) mRecipientAdapter).changeCursor(null);
                    }
                    mRecipientAdapter = (BaseAdapter) data;
                    getListView().setAdapter(mRecipientAdapter);
                }
                mRecipientAdapter.notifyDataSetChanged();
            }

            if(_name != null) {
                mTxtNewName.setText(_name);
            } else {
                mTxtNewName.setText("");
            }
            if(_via != null) {
                mTxtNewContact.setText(_via);
            } else {
                mTxtNewContact.setText("");
            }

            setListShown(true);
        }

        @Override
        protected void onCancelled(Object o) {
            setListShown(true);
        }
    }

    // smiley face (avatar) random animations
    private final Random mRandom = new Random();
    private final Handler mHandler = new Handler();
    private final Runnable mAnimationRunnable = new Runnable() {
        @Override
        public void run() {
            List<AnimatedAvatarView> possibleAnimationsList = new ArrayList<>();

            // get all anonymous avatar instances
            for(int i=0; i<getListView().getChildCount(); i++) {
                AnimatedAvatarView v = (AnimatedAvatarView) getListView().getChildAt(i).findViewById(R.id.imgPhoto);
                if(!v.isShowStaticAvatar()) {
                    possibleAnimationsList.add(v);
                }
            }

            if(mNewAvatarView.getVisibility() == View.VISIBLE) {
                possibleAnimationsList.add(mNewAvatarView);
            } else {
                mNewAvatarView.stopDrawingThread();
            }

            // randomly pick one
            int index = possibleAnimationsList.size() > 0 ? mRandom.nextInt(possibleAnimationsList.size()) : 0;

            // start the animation for the picked avatar and stop all others (avoids unnecessary drawing threads)
            for(int i=0; i<possibleAnimationsList.size(); i++) {
                AnimatedAvatarView v = possibleAnimationsList.get(i);
                if(i == index) {
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

    public RecipientsFragment() {
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        Log.d("RecipientsFragment", "onAttach");

        mActivity = (CustomActionBarActivity) activity;
        mPreferences = new PepperMintPreferences(activity);
        mSenderServiceManager = new SenderServiceManager(activity);

        mRecordManager = new RecordServiceManager(activity);
        mRecordManager.setListener(this);
        mDestroyed = false;
        mAnimatorBuilder = new AnimatorBuilder();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Log.d("RecipientsFragment", "onDetach");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        PeppermintApp app = (PeppermintApp) mActivity.getApplication();

        mSmsConfirmationDialog = new CustomConfirmationDialog(mActivity);
        mSmsConfirmationDialog.setTitle(getString(R.string.sending_via_sms));
        mSmsConfirmationDialog.setText(getString(R.string.when_you_send_via_sms));
        mSmsConfirmationDialog.setTitleTypeface(app.getFontSemibold());
        mSmsConfirmationDialog.setTextTypeface(app.getFontRegular());
        mSmsConfirmationDialog.setButtonTypeface(app.getFontRegular());
        mSmsConfirmationDialog.setNoClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mFinalEvent = null;
                mSmsConfirmationDialog.dismiss();
            }
        });
        mSmsConfirmationDialog.setYesClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage();
                mSmsConfirmationDialog.dismiss();
            }
        });

        mSenderServiceManager.start();
        mRecordManager.start(false);

        mRecordSoundPlayer = MediaPlayer.create(mActivity, R.raw.s_record);

        mMinSwipeDistance = Utils.dpToPx(mActivity, MIN_SWIPE_DISTANCE_DP);

        mRecordingViewOverlay = (RecordingOverlayView) mActivity.createOverlay(R.layout.v_recording_overlay_layout, RECORDING_OVERLAY_TAG, false, true);

        // hold popup
        mHoldPopup = new PopupWindow(mActivity);
        mHoldPopup.setContentView(inflater.inflate(R.layout.v_recipients_popup, null));
        //noinspection deprecation
        // although this is deprecated, it is required for versions  < 22/23, otherwise the popup doesn't show up
        mHoldPopup.setWindowLayoutMode(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        mHoldPopup.setBackgroundDrawable(Utils.getDrawable(mActivity, R.drawable.img_popup));
        mHoldPopup.setAnimationStyle(R.style.Peppermint_PopupAnimation);
        // do not let the popup get in the way of user interaction
        mHoldPopup.setFocusable(false);
        mHoldPopup.setTouchable(false);

        // global touch interceptor to hide keyboard
        mActivity.getTouchInterceptor().setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    mSearchListBarView.removeSearchTextFocus(event);
                }
                dismissPopup();
                mLastTouchPoint.set((int) event.getX(), (int) event.getY());
                return false;
            }
        });

        // inflate and init custom action bar view
        mSearchListBarView = (SearchListBarView) inflater.inflate(R.layout.f_recipients_actionbar, null, false);
        mRecipientTypeAdapter = new SearchListBarAdapter<>(app.getFontRegular(), mActivity, RecipientType.getAll(mActivity));
        mSearchListBarView.setListAdapter(mRecipientTypeAdapter);
        mSearchListBarView.setTypeface(app.getFontRegular());

        int selectedItemPosition = 0;
        if (savedInstanceState != null) {
            if(savedInstanceState.containsKey(RECORDING_FINAL_EVENT_KEY)) {
                mFinalEvent = (RecordService.Event) savedInstanceState.getSerializable(RECORDING_FINAL_EVENT_KEY);
            }

            Bundle dialogState = savedInstanceState.getBundle(SMS_CONFIRMATION_STATE_KEY);
            mSmsConfirmationDialog.onRestoreInstanceState(dialogState);

            selectedItemPosition = savedInstanceState.getInt(RECIPIENT_TYPE_POS_KEY, 0);
            String searchText = savedInstanceState.getString(RECIPIENT_TYPE_SEARCH_KEY, null);
            if(searchText != null) {
                mSearchListBarView.setSearchText(searchText);
            }
        } else {
            if(!hasRecentsOrFavourites()) {
                // select "all contacts" in case there are not fav/recent contacts
                selectedItemPosition = 1;
            }
        }
        mSearchListBarView.setSelectedItemPosition(selectedItemPosition);
        //mSearchListBarView.setOnSearchListener(this);

        mActivity.getCustomActionBar().setContents(mSearchListBarView, false);

        // inflate the view
        View v = inflater.inflate(R.layout.f_recipients_layout, container, false);

        // init no recipients view
        TextView txtEmpty1 = (TextView) v.findViewById(R.id.txtEmpty1);
        TextView txtEmpty2 = (TextView) v.findViewById(R.id.txtEmpty2);
        txtEmpty1.setTypeface(app.getFontSemibold());
        txtEmpty2.setTypeface(app.getFontSemibold());
        int peppermintColor = ContextCompat.getColor(mActivity, R.color.green_text);
        txtEmpty2.setText(Html.fromHtml(String.format(getString(R.string.msg_add_some_friends), String.format("#%06X", (0xFFFFFF & peppermintColor)))));

        // init new recipient view
        TextView txtNewVia = (TextView) v.findViewById(R.id.txtVia);
        mTxtNewName = (EditText) v.findViewById(R.id.txtName);
        mTxtNewContact = (EditText) v.findViewById(R.id.txtContact);
        mBtnNew = (ImageButton) v.findViewById(R.id.btnAddContact);
        mNewAvatarView = (AnimatedAvatarView) v.findViewById(R.id.imgPhoto);

        txtNewVia.setTypeface(app.getFontRegular());
        mTxtNewName.setTypeface(app.getFontSemibold());
        mTxtNewContact.setTypeface(app.getFontSemibold());
        mNewAvatarView.setShowStaticAvatar(false);
        mBtnNew.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = mTxtNewName.getText().toString().trim();
                String via = mTxtNewContact.getText().toString().trim();
                String mimeType = null;

                // validate display name
                if(name.length() <= 0) {
                    Toast.makeText(mActivity, R.string.msg_message_invalid_contactname, Toast.LENGTH_LONG).show();
                    return;
                }

                // validate email
                if(Utils.isValidEmail(via)) {
                    mimeType = ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE;
                } else if(Utils.isValidPhoneNumber(via)) {
                    mimeType = ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE;
                }

                if(mimeType == null) {
                    Toast.makeText(mActivity, R.string.msg_message_invalid_contactvia, Toast.LENGTH_LONG).show();
                    return;
                }

                ArrayList<ContentProviderOperation> ops = new ArrayList<>();

                // create raw contact
                ops.add(ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                        .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                        .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null).build());

                // add display name data
                ops.add(ContentProviderOperation
                        .newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name)
                        .build());

                // add phone/email data
                ops.add(ContentProviderOperation
                        .newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                        .withValue(ContactsContract.Data.MIMETYPE, mimeType)
                        .withValue(ContactsContract.Data.DATA1, via).build());

                try {
                    ContentProviderResult[] res = mActivity.getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
                    if(res.length < ops.size()) {
                        throw new RuntimeException("Not all operations were performed while trying to insert contact: Total Ops = " + ops.size() + "; Performed = " + res.length);
                    }
                    Toast.makeText(mActivity, R.string.msg_message_contact_added, Toast.LENGTH_LONG).show();
                    // refresh listview
                    mSearchListBarView.setSearchText(name + " <" + via + ">");
                } catch (Throwable e) {
                    Toast.makeText(mActivity, R.string.msg_message_unable_addcontact, Toast.LENGTH_LONG).show();
                    Crashlytics.logException(e);
                }
            }
        });

        // init loading recipients view
        mRecipientListShown = true;
        mRecipientLoadingContainer = v.findViewById(R.id.progressContainer);
        mRecipientLoadingView = (LoadingView) v.findViewById(R.id.loading);

        // init recipient list view
        mRecipientListContainer =  v.findViewById(R.id.listContainer);

        // bottom status bar
        mLytSenderControl = (SenderControlLayout) v.findViewById(R.id.lytStatus);
        mLytSenderControl.setSenderManager(mSenderServiceManager);
        mLytSenderControl.setTypeface(app.getFontSemibold());
        mSenderServiceManager.setListener(mLytSenderControl);

        // avoid showing "no contacts" for a split second, right after creation
        setListShownNoAnimation(false);

        if(getArguments() != null && (getArguments().containsKey(FAST_REPLY_NAME_PARAM) || getArguments().containsKey(FAST_REPLY_MAIL_PARAM))) {
            String name = getArguments().getString(FAST_REPLY_NAME_PARAM, null);
            String mail = getArguments().getString(FAST_REPLY_MAIL_PARAM, null);
            mSearchListBarView.setSearchText(name + " <" + mail + ">");
        }

        return v;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getListView().setOnItemClickListener(this);
        getListView().setLongClickable(true);
        getListView().setOnItemLongClickListener(this);
        getListView().setOnTouchListener(this);

        synchronized (mLock) {
            mCreated = true;
            mLock.notifyAll();
        }

        Log.d("RecipientsFragment", "onViewCreated");
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(RECIPIENT_TYPE_POS_KEY, mSearchListBarView.getSelectedItemPosition());
        outState.putString(RECIPIENT_TYPE_SEARCH_KEY, mSearchListBarView.getSearchText());
        if(mFinalEvent != null) {
            outState.putSerializable(RECORDING_FINAL_EVENT_KEY, mFinalEvent);
        }
        outState.putBundle(SMS_CONFIRMATION_STATE_KEY, mSmsConfirmationDialog.onSaveInstanceState());
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onStart() {
        super.onStart();

        onSearch(mSearchListBarView.getSearchText());
        mSearchListBarView.setOnSearchListener(this);
        mSenderServiceManager.bind();
        mRecordManager.bind();

        mHandler.postDelayed(mAnimationRunnable, FIXED_AVATAR_ANIMATION_INTERVAL_MS + mRandom.nextInt(VARIABLE_AVATAR_ANIMATION_INTERVAL_MS));
    }

    @Override
    public void onPause() {
        mRecordingViewOverlay.stop();
        if(hideRecordingOverlay(false)) {
            mSendRecording = false;
            mRecordManager.stopRecording(true);
        }

        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        mHandler.removeCallbacks(mAnimationRunnable);

        mSenderServiceManager.unbind();
        mRecordManager.unbind();
        mSearchListBarView.setOnSearchListener(null);

        if(mGetRecipientsTask != null && !mGetRecipientsTask.isCancelled() && mGetRecipientsTask.getStatus() != AsyncTask.Status.FINISHED) {
            mGetRecipientsTask.cancel(true);
        }
    }

    @Override
    public void onDestroy() {
        if(mRecordSoundPlayer.isPlaying()) {
            mRecordSoundPlayer.stop();
        }
        mRecordSoundPlayer.release();

        mSearchListBarView.deinit();
        if(mRecipientAdapter != null && mRecipientAdapter instanceof RecipientCursorAdapter) {
            // this closes the cursor inside the adapter
            ((RecipientCursorAdapter) mRecipientAdapter).changeCursor(null);
        }
        mDestroyed = true;
        mActivity = null;
        super.onDestroy();
    }

    public boolean showRecordingOverlay() {
        mRecordingViewOverlay.start();
        return mActivity.showOverlay(RECORDING_OVERLAY_TAG, true, null);
    }

    public boolean hideRecordingOverlay(boolean animated) {
        //mRecordingViewOverlay.blinkLeft();
        //mRecordingViewOverlay.stop();
        return mActivity.hideOverlay(RECORDING_OVERLAY_TAG, RECORDING_OVERLAY_HIDE_DELAY, animated);   // FIXME animated hide is buggy
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

        if (animate && mActivity != null) {
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
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        showPopup(mActivity, view);
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        setRecordDuration(0);

        // start recording
        if(showRecordingOverlay()) {
            if(mRecordSoundPlayer.isPlaying()) {
                mRecordSoundPlayer.stop();
            }
            mRecordSoundPlayer.seekTo(0);
            mRecordSoundPlayer.start();

            Recipient recipient = mRecipientAdapter instanceof RecipientCursorAdapter ?
                    ((RecipientCursorAdapter) mRecipientAdapter).getRecipient(position) :
                    ((RecipientArrayAdapter) mRecipientAdapter).getItem(position);

            mRecordingViewOverlay.setName(recipient.getName());
            mRecordingViewOverlay.setVia(recipient.getVia());
            String filename = getString(R.string.filename_message_from) + Utils.normalizeAndCleanString(mPreferences.getDisplayName());
            mRecordManager.startRecording(filename, recipient, MAX_DURATION_MILLIS);
        }
        return true;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch(event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                x1 = event.getX();
                y1 = event.getY();
                t1 = android.os.SystemClock.uptimeMillis();
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                x2 = event.getX();
                y2 = event.getY();
                t2 = android.os.SystemClock.uptimeMillis();
                float deltaX = x2 - x1;
                float deltaY = y2 - y1;

                if(hideRecordingOverlay(true)) {
                    mSendRecording = false;

                    if ((Math.abs(deltaX) > mMinSwipeDistance || Math.abs(deltaY) > mMinSwipeDistance) && (t2 - t1) < MAX_SWIPE_DURATION) {
                        // swipe: cancel
                        mRecordingViewOverlay.explode();
                        mRecordManager.stopRecording(true);
                    } else {
                        // release: send
                        mRecordingViewOverlay.stop();
                        if (mRecordingViewOverlay.getMillis() < 2000) {
                            Toast.makeText(mActivity, R.string.msg_record_at_least, Toast.LENGTH_SHORT).show();
                            mRecordManager.stopRecording(true);
                        } else {
                            mSendRecording = true;
                            mRecordManager.stopRecording(false);
                        }
                    }
                }
                break;
            default:
                long now = android.os.SystemClock.uptimeMillis();
                if((now - t1) > MAX_SWIPE_DURATION) {
                    t1 = now;
                    x1 = event.getX();
                    y1 = event.getY();
                }
        }

        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.d("RecipientsFragment", "onRequestPermissionsResult");
        onSearch(mSearchListBarView.getSearchText());
    }

    @Override
    public void onSearch(String filter) {
        Log.d("RecipientsFragment", "onSearch");

        if(mGetRecipientsTask != null && !mGetRecipientsTask.isCancelled() && mGetRecipientsTask.getStatus() != AsyncTask.Status.FINISHED) {
            mGetRecipientsTask.cancel(true);
        }

        mGetRecipientsTask = new GetRecipients(filter);
        mGetRecipientsTask.execute();
    }

    private boolean hasRecentsOrFavourites() {
        if (ContextCompat.checkSelfPermission(mActivity,
                Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return false;
        }

        List<Long> recentList = mPreferences.getRecentContactUris();
        if(recentList != null && recentList.size() > 0) {
            return true;
        }

        FilteredCursor cursor = (FilteredCursor) RecipientAdapterUtils.getRecipientsCursor(mActivity, null, null, true, mRecipientTypeAdapter.getItem(0).getMimeTypes(), null);
        return cursor != null && cursor.getOriginalCursor() != null && cursor.getOriginalCursor().moveToFirst();
    }

    private void dismissPopup() {
        if(mHoldPopup.isShowing() && !isDetached() && !mDestroyed) {
            mHoldPopup.dismiss();
            mHandler.removeCallbacks(mDismissPopupRunnable);
        }
    }

    // the method that displays the img_popup.
    private void showPopup(final Activity context, View parent) {
        dismissPopup();
        mHoldPopup.showAtLocation(parent, Gravity.NO_GRAVITY, mLastTouchPoint.x - Utils.dpToPx(mActivity, 80), mLastTouchPoint.y + Utils.dpToPx(mActivity, 10));
        mHandler.postDelayed(mDismissPopupRunnable, 2000);
    }

    @Override
    public void onStartRecording(RecordService.Event event) {
        onBoundRecording(event.getRecording(), event.getRecipient(), event.getLoudness());
    }

    @Override
    public void onStopRecording(RecordService.Event event) {
        onBoundRecording(event.getRecording(), event.getRecipient(), event.getLoudness());
        if(mSendRecording || mRecordManager.getCurrentRecording().getDurationMillis() >= MAX_DURATION_MILLIS) {

            if(mRecordManager.getCurrentRecording().getDurationMillis() >= MAX_DURATION_MILLIS) {
                Toast.makeText(mActivity, R.string.msg_message_exceeded_maxduration, Toast.LENGTH_LONG).show();
            }

            mSendRecording = false;

            mFinalEvent = event;
            if(!mPreferences.isShownSmsConfirmation() && event.getRecipient().getMimeType().equals(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)) {
                mSmsConfirmationDialog.show();
            } else {
                sendMessage();
            }
        }
        hideRecordingOverlay(true);
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
        mRecordingViewOverlay.setMillis(fullDuration);
    }

    @Override
    public void onLoudnessRecording(RecordService.Event event) {
        mRecordingViewOverlay.pushAmplitude(event.getLoudness());
        setRecordDuration(event.getRecording().getDurationMillis());
    }

    @Override
    public void onErrorRecording(RecordService.Event event) {
        if(event.getError() instanceof NoMicDataIOException) {
            Toast.makeText(mActivity, getString(R.string.msg_message_nomicdata_error), Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(mActivity, getString(R.string.msg_message_record_error), Toast.LENGTH_LONG).show();
        }

        mRecordingViewOverlay.explode();
        hideRecordingOverlay(true);
    }

    @Override
    public void onBoundRecording(Recording currentRecording, Recipient currentRecipient, float currentLoudness) {
        setRecordDuration(currentRecording == null ? 0 : currentRecording.getDurationMillis());
    }

    private void sendMessage() {
        // if the user has gone through the sending process without
        // discarding the recording, then clear the search filter
        mSearchListBarView.clearSearch(0);

        mPreferences.addRecentContactUri(mFinalEvent.getRecipient().getContactId());
        mSenderServiceManager.startAndSend(mFinalEvent.getRecipient(), mFinalEvent.getRecording());

        mFinalEvent = null;
    }

}
