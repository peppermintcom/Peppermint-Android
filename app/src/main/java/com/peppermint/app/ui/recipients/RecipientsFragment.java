package com.peppermint.app.ui.recipients;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorSet;
import android.app.Activity;
import android.app.ListFragment;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.TextView;

import com.crashlytics.android.Crashlytics;
import com.peppermint.app.PeppermintApp;
import com.peppermint.app.R;
import com.peppermint.app.SenderServiceManager;
import com.peppermint.app.data.Recipient;
import com.peppermint.app.data.RecipientType;
import com.peppermint.app.ui.CustomActionBarActivity;
import com.peppermint.app.ui.canvas.avatar.AnimatedAvatarView;
import com.peppermint.app.ui.canvas.progress.LoadingView;
import com.peppermint.app.ui.recipients.add.NewRecipientActivity;
import com.peppermint.app.ui.recipients.add.NewRecipientFragment;
import com.peppermint.app.ui.recording.RecordingActivity;
import com.peppermint.app.ui.recording.RecordingFragment;
import com.peppermint.app.ui.views.SearchListBarAdapter;
import com.peppermint.app.ui.views.SearchListBarView;
import com.peppermint.app.ui.views.dialogs.CustomConfirmationDialog;
import com.peppermint.app.utils.AnimatorBuilder;
import com.peppermint.app.utils.FilteredCursor;
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

public class RecipientsFragment extends ListFragment implements AdapterView.OnItemClickListener, SearchListBarView.OnSearchListener {

    public static final int REQUEST_RECORD = 111;
    public static final int REQUEST_NEWCONTACT = 222;

    public static final String FAST_REPLY_NAME_PARAM = "name";
    public static final String FAST_REPLY_MAIL_PARAM = "mail";

    // keys to save the instance state
    private static final String RECIPIENT_TAPPED_KEY = "RecipientsFragment_RecipientTapped";
    private static final String SAVED_DIALOG_STATE_KEY = "RecipientsFragment_SmsDialogState";

    private static final int FIXED_AVATAR_ANIMATION_INTERVAL_MS = 15000;
    private static final int VARIABLE_AVATAR_ANIMATION_INTERVAL_MS = 15000;

    private PepperMintPreferences mPreferences;
    private CustomActionBarActivity mActivity;
    private AnimatorBuilder mAnimatorBuilder;

    // the recipient list
    private View mRecipientListContainer;
    private View mRecipientLoadingContainer;
    private LoadingView mRecipientLoadingView;
    private boolean mRecipientListShown;
    private BaseAdapter mRecipientAdapter;
    private Button mBtnAddContact;
    private boolean mNoRecentsAtStartAndDidntPick = false;

    // the custom action bar (with recipient type filter and recipient search)
    private SearchListBarView mSearchListBarView;
    private SearchListBarAdapter<RecipientType> mRecipientTypeAdapter;

    // bottom bar
    private SenderServiceManager mSenderServiceManager;
    private SenderControlLayout mLytSenderControl;
    private CustomConfirmationDialog mSmsAddContactDialog;
    private Recipient mTappedRecipient;

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
            setListShown(false);
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
                if (mActivity != null && ContextCompat.checkSelfPermission(mActivity,
                        Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {

                    PeppermintApp app = (PeppermintApp) mActivity.getApplication();

                    if (_recipientType.isStarred() != null && _recipientType.isStarred()) {
                        List<Long> recentList = mPreferences.getRecentContactUris();
                        if(recentList == null) {
                            // empty list to show no contacts
                            recentList = new ArrayList<>();
                        }

                        // get recent contact list
                        Map<Long, Recipient> recipientMap = new HashMap<>();

                        if(recentList.size() > 0) {
                            Cursor cursor = RecipientAdapterUtils.getRecipientsCursor(mActivity, recentList, null, null, null, null);
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
                                mPreferences.setRecentContactUris(recentList);
                            }
                        }

                        return new RecipientArrayAdapter(app, mActivity, recipientMap, recentList);
                    }

                    // get normal full, email or phone contact list
                    FilteredCursor cursor = (FilteredCursor) RecipientAdapterUtils.getRecipientsCursor(mActivity, null, _name, _recipientType.isStarred(), _recipientType.getMimeTypes(), _via);
                    if(cursor.getOriginalCursor().getCount() <= 0 && _name != null && _via != null) {
                        cursor = (FilteredCursor) RecipientAdapterUtils.getRecipientsCursor(mActivity, null, null, _recipientType.isStarred(), _recipientType.getMimeTypes(), _via);
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
            // check if data is valid and activity has not been destroyed by the main thread
            if(data != null && mActivity != null && mCreated) {
                PeppermintApp app = (PeppermintApp) mActivity.getApplication();

                if (data instanceof Cursor) {
                    // re-use adapter and replace cursor
                    if (mRecipientAdapter != null && mRecipientAdapter instanceof CursorAdapter) {
                        ((CursorAdapter) mRecipientAdapter).changeCursor((Cursor) data);
                    } else {
                        mRecipientAdapter = new RecipientCursorAdapter(app, mActivity, (Cursor) data);
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
            setListShown(true);
        }

        @Override
        protected void onCancelled(Object o) {
            handleAddContactButtonVisibility();
            setListShown(true);
        }

        private void handleAddContactButtonVisibility() {
            if(mSearchListBarView.getSearchText() != null) {
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

    final Rect mBtnAddContactHitRect = new Rect();

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

    @SuppressWarnings("deprecation")
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        mActivity = (CustomActionBarActivity) activity;
        mPreferences = new PepperMintPreferences(activity);
        mSenderServiceManager = new SenderServiceManager(activity);
        mAnimatorBuilder = new AnimatorBuilder();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        PeppermintApp app = (PeppermintApp) mActivity.getApplication();

        mSenderServiceManager.start();

        // global touch interceptor to hide keyboard
        mActivity.getTouchInterceptor().setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    // do not do it if the add contact button was pressed
                    // since this might be causing the tap event to not work
                    // the onClick event already has instructions to do this
                    mBtnAddContact.getHitRect(mBtnAddContactHitRect);
                    if(!mBtnAddContactHitRect.contains((int) event.getX(), (int) event.getY())) {
                        mSearchListBarView.removeSearchTextFocus(event);
                        getView().requestFocus();
                    }
                }
                return false;
            }
        });

        // dialog for unsupported SMS
        mSmsAddContactDialog = new CustomConfirmationDialog(mActivity);
        mSmsAddContactDialog.setTitleText(R.string.sending_via_sms);
        mSmsAddContactDialog.setMessageText(R.string.msg_message_sms_disabled_add_contact);
        mSmsAddContactDialog.setPositiveButtonListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mActivity, NewRecipientActivity.class);
                if(mTappedRecipient != null) {
                    intent.putExtra(NewRecipientFragment.KEY_VIA, mTappedRecipient.getVia());
                    intent.putExtra(NewRecipientFragment.KEY_NAME, mTappedRecipient.getName());
                    intent.putExtra(NewRecipientFragment.KEY_RAW_ID, mTappedRecipient.getRawId());
                    intent.putExtra(NewRecipientFragment.KEY_PHOTO_URL, mTappedRecipient.getPhotoUri());
                }
                startActivityForResult(intent, REQUEST_NEWCONTACT);
                mSmsAddContactDialog.dismiss();
            }
        });
        mSmsAddContactDialog.setNegativeButtonListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSmsAddContactDialog.dismiss();
            }
        });

        // inflate and init custom action bar view
        mSearchListBarView = (SearchListBarView) inflater.inflate(R.layout.f_recipients_actionbar, null, false);
        mRecipientTypeAdapter = new SearchListBarAdapter<>(app.getFontRegular(), mActivity, RecipientType.getAll(mActivity));
        mSearchListBarView.setListAdapter(mRecipientTypeAdapter);
        mSearchListBarView.setTypeface(app.getFontRegular());

        if (savedInstanceState != null) {
            mTappedRecipient = (Recipient) savedInstanceState.getSerializable(RECIPIENT_TAPPED_KEY);

            Bundle dialogState = savedInstanceState.getBundle(SAVED_DIALOG_STATE_KEY);
            if (dialogState != null) {
                mSmsAddContactDialog.onRestoreInstanceState(dialogState);
            }
        } else {
            // default view is all contacts
            int selectedItemPosition = 0;
            if(!hasRecents()) {
                // select "all contacts" in case there are not fav/recent contacts
                selectedItemPosition = 1;
                mNoRecentsAtStartAndDidntPick = true;
            }
            mSearchListBarView.setSelectedItemPosition(selectedItemPosition);
        }

        mActivity.getCustomActionBar().setContents(mSearchListBarView, false);

        // inflate the view
        View v = inflater.inflate(R.layout.f_recipients_layout, container, false);

        /*// bo: adjust status bar height (only do it for API 21 onwards since overlay is not working for older versions)
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            int statusBarHeight = Utils.getStatusBarHeight(mActivity);
            View listContainer = v.findViewById(R.id.listContainer);
            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) listContainer.getLayoutParams();
            layoutParams.topMargin -= statusBarHeight;
        }
        // eo: adjust status bar height*/

        // init no recipients view
        mBtnAddContact = (Button) v.findViewById(R.id.btnAddContact);
        mBtnAddContact.setTypeface(app.getFontSemibold());
        mBtnAddContact.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mActivity, NewRecipientActivity.class);

                String filter = mSearchListBarView.getSearchText();
                if(filter != null) {
                    String[] viaName = getSearchData(filter);
                    intent.putExtra(NewRecipientFragment.KEY_VIA, viaName[0]);
                    intent.putExtra(NewRecipientFragment.KEY_NAME, viaName[1]);
                }
                startActivityForResult(intent, REQUEST_NEWCONTACT);

                mSearchListBarView.removeSearchTextFocus(null);
                getView().requestFocus();
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

        // bottom status bar
        mLytSenderControl = (SenderControlLayout) v.findViewById(R.id.lytStatus);
        mLytSenderControl.setSenderManager(mSenderServiceManager);
        mLytSenderControl.setTypeface(app.getFontSemibold());
        mSenderServiceManager.setListener(mLytSenderControl);

        // avoid showing "no contacts" for a split second, right after creation
        setListShownNoAnimation(false);

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
                FilteredCursor checkCursor = (FilteredCursor) RecipientAdapterUtils.getRecipientsCursor(mActivity, null, null, null, mimeTypes, mail);
                boolean alreadyHasEmail = checkCursor != null && checkCursor.getOriginalCursor() != null && checkCursor.getOriginalCursor().getCount() > 0;

                // if not, add the contact
                if(!alreadyHasEmail) {
                    Bundle bundle = NewRecipientFragment.insertRecipientContact(mActivity, 0, name, null, mail, null);
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

        return v;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getListView().setOnItemClickListener(this);

        synchronized (mLock) {
            mCreated = true;
            mLock.notifyAll();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        // save tapped recipient in case add contact dialog is showing
        outState.putSerializable(RECIPIENT_TAPPED_KEY, mTappedRecipient);
        // save add contact dialog state as well
        Bundle dialogState = mSmsAddContactDialog.onSaveInstanceState();
        if (dialogState != null) {
            outState.putBundle(SAVED_DIALOG_STATE_KEY, dialogState);
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onStart() {
        super.onStart();

        onSearch(mSearchListBarView.getSearchText());
        mSearchListBarView.setOnSearchListener(this);
        mSenderServiceManager.bind();

        mHandler.postDelayed(mAnimationRunnable, FIXED_AVATAR_ANIMATION_INTERVAL_MS + mRandom.nextInt(VARIABLE_AVATAR_ANIMATION_INTERVAL_MS));
    }

    @Override
    public void onResume() {
        super.onResume();
        // avoid cursor focus and keyboard when opening
        // if it is on onStart(), it doesn't work for screen rotations
        mSearchListBarView.removeSearchTextFocus(null);
        getView().requestFocus();
    }

    @Override
    public void onStop() {
        mHandler.removeCallbacks(mAnimationRunnable);

        mSenderServiceManager.unbind();
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
        mActivity = null;
        super.onDestroy();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQUEST_RECORD) {
            if(resultCode == Activity.RESULT_OK) {
                if(mNoRecentsAtStartAndDidntPick) {
                    // still go back to recent contacts after sending a message
                    mSearchListBarView.setSelectedItemPositionBeforeSearch(0);
                }

                // if the user has gone through the sending process without
                // discarding the recording, then clear the search filter
                mSearchListBarView.clearSearch(0);
            }
        } else if(requestCode == REQUEST_NEWCONTACT) {
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
        Intent recordIntent = new Intent(mActivity, RecordingActivity.class);

        mTappedRecipient = mRecipientAdapter instanceof RecipientCursorAdapter ?
                ((RecipientCursorAdapter) mRecipientAdapter).getRecipient(position) :
                ((RecipientArrayAdapter) mRecipientAdapter).getItem(position);

        if (mTappedRecipient.getMimeType().compareTo(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE) == 0 &&
                !Utils.isSimAvailable(mActivity)) {
            if(!mSmsAddContactDialog.isShowing()) {
                mSmsAddContactDialog.show();
            }
            return;
        }

        recordIntent.putExtra(RecordingFragment.INTENT_RECIPIENT_EXTRA, mTappedRecipient);
        startActivityForResult(recordIntent, REQUEST_RECORD);
    }

    @Override
    public void onSearch(String filter) {
        if(mSearchListBarView.getSelectedItemPositionBeforeSearch() != 1 && mNoRecentsAtStartAndDidntPick) {
            mNoRecentsAtStartAndDidntPick = false;
        }

        if(mGetRecipientsTask != null && !mGetRecipientsTask.isCancelled() && mGetRecipientsTask.getStatus() != AsyncTask.Status.FINISHED) {
            mGetRecipientsTask.cancel(true);
        }

        mGetRecipientsTask = new GetRecipients(filter);
        mGetRecipientsTask.execute();
    }

    private boolean hasRecents() {
        List<Long> recentList = mPreferences.getRecentContactUris();
        if(recentList != null && recentList.size() > 0) {
            return true;
        }

        return false;
    }

}
