package com.peppermint.app.ui.recipients;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorSet;
import android.app.Activity;
import android.app.ListFragment;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.support.v4.content.ContextCompat;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.peppermint.app.PeppermintApp;
import com.peppermint.app.R;
import com.peppermint.app.SenderServiceManager;
import com.peppermint.app.data.Recipient;
import com.peppermint.app.data.RecipientType;
import com.peppermint.app.ui.CustomActionBarActivity;
import com.peppermint.app.ui.canvas.avatar.AnimatedAvatarView;
import com.peppermint.app.ui.canvas.progress.LoadingView;
import com.peppermint.app.ui.recording.RecordingActivity;
import com.peppermint.app.ui.recording.RecordingFragment;
import com.peppermint.app.ui.views.SearchListBarAdapter;
import com.peppermint.app.ui.views.SearchListBarView;
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

    public static final int REQUEST_RECORD = 1;

    public static final String FAST_REPLY_NAME_PARAM = "name";
    public static final String FAST_REPLY_MAIL_PARAM = "mail";

    // keys to save the instance state
    private static final String RECIPIENT_TYPE_POS_KEY = "RecipientsFragment_RecipientTypePosition";
    private static final String RECIPIENT_TYPE_SEARCH_KEY = "RecipientsFragment_RecipientTypeSearch";

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

    // new recipient view
    private EditText mTxtNewName, mTxtNewContact;
    private ImageButton mBtnNew;
    private AnimatedAvatarView mNewAvatarView;

    // the custom action bar (with recipient type filter and recipient search)
    private SearchListBarView mSearchListBarView;
    private SearchListBarAdapter<RecipientType> mRecipientTypeAdapter;

    // bottom bar
    private SenderServiceManager mSenderServiceManager;
    private SenderControlLayout mLytSenderControl;

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
                    mSearchListBarView.removeSearchTextFocus(event);
                }
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

        synchronized (mLock) {
            mCreated = true;
            mLock.notifyAll();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(RECIPIENT_TYPE_POS_KEY, mSearchListBarView.getSelectedItemPosition());
        outState.putString(RECIPIENT_TYPE_SEARCH_KEY, mSearchListBarView.getSearchText());
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
    public void onStop() {
        super.onStop();
        mHandler.removeCallbacks(mAnimationRunnable);

        mSenderServiceManager.unbind();
        mSearchListBarView.setOnSearchListener(null);

        if(mGetRecipientsTask != null && !mGetRecipientsTask.isCancelled() && mGetRecipientsTask.getStatus() != AsyncTask.Status.FINISHED) {
            mGetRecipientsTask.cancel(true);
        }
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
                // if the user has gone through the sending process without
                // discarding the recording, then clear the search filter
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

        Recipient recipient = mRecipientAdapter instanceof RecipientCursorAdapter ?
                ((RecipientCursorAdapter) mRecipientAdapter).getRecipient(position) :
                ((RecipientArrayAdapter) mRecipientAdapter).getItem(position);

        recordIntent.putExtra(RecordingFragment.INTENT_RECIPIENT_EXTRA, recipient);
        startActivityForResult(recordIntent, REQUEST_RECORD);
    }

    @Override
    public void onSearch(String filter) {
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
}
