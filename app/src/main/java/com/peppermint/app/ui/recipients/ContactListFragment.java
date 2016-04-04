package com.peppermint.app.ui.recipients;

import android.Manifest;
import android.app.Activity;
import android.app.ListFragment;
import android.content.Context;
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
import android.widget.HeaderViewListAdapter;
import android.widget.ListAdapter;
import android.widget.PopupWindow;

import com.peppermint.app.R;
import com.peppermint.app.authenticator.AuthenticationData;
import com.peppermint.app.authenticator.AuthenticationPolicyEnforcer;
import com.peppermint.app.data.Chat;
import com.peppermint.app.data.ChatManager;
import com.peppermint.app.data.ChatRecipient;
import com.peppermint.app.data.ContactManager;
import com.peppermint.app.data.ContactRaw;
import com.peppermint.app.data.DatabaseHelper;
import com.peppermint.app.data.FilteredCursor;
import com.peppermint.app.data.Message;
import com.peppermint.app.data.RecipientType;
import com.peppermint.app.data.Recording;
import com.peppermint.app.events.ReceiverEvent;
import com.peppermint.app.events.SenderEvent;
import com.peppermint.app.events.SyncEvent;
import com.peppermint.app.ui.Overlay;
import com.peppermint.app.ui.OverlayManager;
import com.peppermint.app.ui.base.activities.CustomActionBarDrawerActivity;
import com.peppermint.app.ui.base.dialogs.PopupDialog;
import com.peppermint.app.ui.canvas.avatar.AnimatedAvatarView;
import com.peppermint.app.ui.chat.ChatActivity;
import com.peppermint.app.ui.chat.ChatCursorAdapter;
import com.peppermint.app.ui.chat.recorder.ChatRecordOverlayController;
import com.peppermint.app.ui.recipients.add.NewContactActivity;
import com.peppermint.app.utils.DateContainer;
import com.peppermint.app.utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ContactListFragment extends ListFragment implements ChatRecordOverlayController.Callbacks,
        AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener,
        SearchListBarView.OnSearchListener, OverlayManager.OverlayVisibilityChangeListener {

    private static final String SCREEN_ID = "Contacts";

    public static final String FAST_REPLY_NAME_PARAM = "name";
    public static final String FAST_REPLY_MAIL_PARAM = "mail";

    public static final int REQUEST_NEWCONTACT_AND_SEND = 223;
    public static final int REQUEST_NEWCONTACT = 224;

    // avatar animation frequency
    private static final int FIXED_AVATAR_ANIMATION_INTERVAL_MS = 7500;
    private static final int VARIABLE_AVATAR_ANIMATION_INTERVAL_MS = 7500;

    private CustomActionBarDrawerActivity mActivity;
    private ChatRecordOverlayController mController;
    private DatabaseHelper mDatabaseHelper;

    private boolean mHasSavedInstanceState = false;
    private boolean mOnActivityResult = false;

    // the recipient list
    private ContactCursorAdapter mRecipientAdapter;
    private Button mBtnAddContact, mBtnAddContactEmpty;
    private PopupDialog mTipPopup;
    private ViewGroup mListFooterView;

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

    // search
    private final Object mLock = new Object();
    private boolean mCreated = false;
    private GetRecipients mGetRecipientsTask;
    private static final Pattern mViaPattern = Pattern.compile("<([^\\s]*)>");

    private ChatCursorAdapter mChatAdapter;

    private ThreadPoolExecutor mGetRecipientsExecutor;

    private class GetRecipients extends AsyncTask<Void, Void, Object> {
        private RecipientType _recipientType;
        private String _filter;
        private String _name, _via;

        protected GetRecipients(String filter) {
            this._filter = filter;
        }

        @Override
        protected void onPreExecute() {
            mActivity.startFragmentLoading(ContactListFragment.this);

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

                    if (_recipientType.isStarred() != null && _recipientType.isStarred()) {
                        mChatAdapter.setPeppermintSet(ContactManager.getPeppermintContacts(mActivity, null).keySet());
                        // show chat list / recent contacts
                        // ChatManager.deleteMissingRecipientChats(mActivity, getDatabase());
                        return ChatManager.getAll(mDatabaseHelper.getReadableDatabase());
                    }

                    // get normal full, email or phone contact list
                    FilteredCursor cursor = (FilteredCursor) ContactManager.get(mActivity, null, _name, _recipientType.isStarred(), _recipientType.getMimeTypes(), _via);
                    if(cursor.getOriginalCursor().getCount() <= 0 && _name != null && _via != null) {
                        cursor.close();
                        cursor = (FilteredCursor) ContactManager.get(mActivity, null, null, _recipientType.isStarred(), _recipientType.getMimeTypes(), _via);
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
                mActivity.getTrackerManager().logException(e);
            }

            return null;
        }

        @Override
        protected void onPostExecute(Object data) {
            // check if data is valid and activity has not been destroyed by the main thread
            if(data != null && mActivity != null && mCreated) {

                if (data instanceof FilteredCursor) {
                    // re-use adapter and replace cursor
                    mRecipientAdapter.changeCursor((Cursor) data);
                    if(getRealAdapter() != mRecipientAdapter) {
                        getListView().setAdapter(mRecipientAdapter);

                    }
                    mChatAdapter.changeCursor(null);
                } else {
                    // use new adapter
                    mChatAdapter.changeCursor((Cursor) data);
                    if(getRealAdapter() != mChatAdapter) {
                         getListView().setAdapter(mChatAdapter);
                    }
                    mRecipientAdapter.changeCursor(null);
                }

                mActivity.stopFragmentLoading(true);
            }
        }

        @Override
        protected void onCancelled(Object o) {
            if(o != null) {
                ((Cursor) o).close();
            }

            if(mActivity != null) {
                mActivity.stopFragmentLoading(false);
            }
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

    private final Rect mBtnAddContactHitRect = new Rect();
    private final Rect mBtnAddContactEmptyHitRect = new Rect();
    private PointF mTipPoint;

    private final Runnable mTipRunnable = new Runnable() {
        @Override
        public void run() {
            if (mTipPopup != null && mActivity != null && !mTipPopup.isShowing() && !isRemoving() && !mActivity.isFinishing()) {
                if(mActivity.getCustomActionBar().getHeight() <= 0) {
                    mHandler.postDelayed(mTipRunnable, 100);
                } else {
                    mTipPopup.show(mActivity.getCustomActionBar());
                }
            }
        }
    };

    public ContactListFragment() {
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
            if(mActivity == null) {
                // in case fragment has been detached and destroyed
                return;
            }

            mActivity.getAuthenticationPolicyEnforcer().removeAuthenticationDoneCallback(this);

            mController.getMessagesServiceManager().startAndSync();

            if(getArguments() != null && (getArguments().containsKey(FAST_REPLY_NAME_PARAM) || getArguments().containsKey(FAST_REPLY_MAIL_PARAM))) {
                String name = getArguments().getString(FAST_REPLY_NAME_PARAM, "");
                String mail = getArguments().getString(FAST_REPLY_MAIL_PARAM, "");

                if(mail.length() <= 0) {
                    // if the email was not supplied, just search for the name
                    mSearchListBarView.setSearchText(name);
                } else {
                    // if mail is supplied, check if the contact exists
                    ContactRaw foundRecipient = ContactManager.getRawContactByViaOrContactId(mActivity, mail, 0);

                    // if not, add the contact
                    if(foundRecipient == null) {
                        try {
                            foundRecipient = ContactManager.insert(mActivity, 0, name, null, null, mail, null, data.getEmail(), false);
                        } catch (Exception e) {
                            /* nothing to do here */
                        }
                    }

                    // if it fails, add complete name+email search text to allow adding the full contact
                    // otherwise, just search by email
                    mSearchListBarView.setSearchText(foundRecipient != null || name.length() <= 0 ? mail : name + " <" + mail + ">");
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
                mBtnAddContactEmpty.getGlobalVisibleRect(mBtnAddContactEmptyHitRect);

                if (!mBtnAddContactHitRect.contains((int) event.getX(), (int) event.getY()) &&
                        !mBtnAddContactEmptyHitRect.contains((int) event.getX(), (int) event.getY())) {
                    mSearchListBarView.removeSearchTextFocus(event);
                    View view = getView();
                    if(view != null) {
                        view.requestFocus();
                    }
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
        mDatabaseHelper = DatabaseHelper.getInstance(activity);
        mActivity = (CustomActionBarDrawerActivity) activity;
        mController = new ChatRecordOverlayController(mActivity, this) {
            @Override
            public void onEventMainThread(SyncEvent event) {
                super.onEventMainThread(event);
                if(event.getType() == SyncEvent.EVENT_FINISHED) {
                    if (mSearchListBarView.getSelectedItemPosition() == 0) {
                        onSearch(mSearchListBarView.getSearchText());
                    }
                }
            }

            @Override
            public void onEventMainThread(ReceiverEvent event) {
                super.onEventMainThread(event);
                if(event.getType() == ReceiverEvent.EVENT_RECEIVED) {
                    if (mSearchListBarView.getSelectedItemPosition() == 0) {
                        onSearch(mSearchListBarView.getSearchText());
                    }
                }
            }

            @Override
            public void onEventMainThread(SenderEvent event) {
                super.onEventMainThread(event);
                if(event.getType() == SenderEvent.EVENT_FINISHED) {
                    if(mSearchListBarView.getSelectedItemPosition() == 0) {
                        onSearch(mSearchListBarView.getSearchText());
                    }
                }
            }

            @Override
            protected Message sendMessage(Chat chat, Recording recording) {
                Message message = super.sendMessage(chat, recording);

                // if the user has gone through the sending process without
                // discarding the recording, then clear the search filter
                if(!mSearchListBarView.clearSearch(0)) {
                    onSearch(mSearchListBarView.getSearchText());
                }

                launchChatActivity(message.getChatId());

                return message;
            }
        };
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        mChatAdapter = new ChatCursorAdapter(mActivity, null, null, mActivity.getTrackerManager());
        mRecipientAdapter = new ContactCursorAdapter(mActivity, null);

        mActivity.getAuthenticationPolicyEnforcer().addAuthenticationDoneCallback(mAuthenticationDoneCallback);

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

        mTipPopup = new PopupDialog(mActivity) {
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
                mController.getPreferences().setFirstRun(false);
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
                            Utils.showKeyboard(mActivity, searchEditText, WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

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
        mSearchListBarView.setListCategories(RecipientType.getAll(mActivity));

        if (savedInstanceState != null) {
            mHasSavedInstanceState = true;
        }

        mActivity.getCustomActionBar().setContents(mSearchListBarView, false);

        // inflate the view
        View v = inflater.inflate(R.layout.f_recipients_layout, container, false);

        // init no recipients view
        View.OnClickListener addContactClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mActivity, NewContactActivity.class);

                String filter = mSearchListBarView.getSearchText();
                if (filter != null) {
                    String[] viaName = getSearchData(filter);
                    intent.putExtra(NewContactActivity.KEY_VIA, viaName[0]);

                    if (viaName[0] == null && (Utils.isValidPhoneNumber(viaName[1]) || Utils.isValidEmail(viaName[1]))) {
                        intent.putExtra(NewContactActivity.KEY_VIA, viaName[1]);
                    } else {
                        intent.putExtra(NewContactActivity.KEY_NAME, viaName[1]);
                    }
                }
                startActivityForResult(intent, REQUEST_NEWCONTACT);
            }
        };

        LayoutInflater layoutInflater = (LayoutInflater) mActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mListFooterView = (ViewGroup) layoutInflater.inflate(R.layout.v_recipients_footer_layout, null);

        mBtnAddContact = (Button) mListFooterView.findViewById(R.id.btnAddContact);
        mBtnAddContact.setId(0);
        mBtnAddContact.setOnClickListener(addContactClickListener);

        mBtnAddContactEmpty = (Button) v.findViewById(R.id.btnAddContact);
        mBtnAddContactEmpty.setOnClickListener(addContactClickListener);

        return v;
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mController.init(view, mActivity.getOverlayManager(), mActivity, mActivity.getAuthenticationPolicyEnforcer(), savedInstanceState);

        mActivity.getOverlayManager().setRootScreenId(SCREEN_ID);

        getListView().setOnItemClickListener(this);
        getListView().setLongClickable(true);
        getListView().setOnItemLongClickListener(this);

        getListView().addFooterView(mListFooterView);

        synchronized (mLock) {
            mCreated = true;
            mLock.notifyAll();
        }
    }

    @Override
    public void onOverlayShown(Overlay overlay) { /* nothing to do */ }

    @Override
    public void onOverlayHidden(Overlay overlay, boolean wasCancelled) {
        if(mSearchListBarView != null) {
            mSearchListBarView.clearFocus();
            if(getView() != null) {
                getView().requestFocus();
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        mController.saveInstanceState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onStart() {
        super.onStart();

        mActivity.getOverlayManager().addOverlayVisibilityChangeListener(this);

        mController.start();

        // global touch interceptor to hide keyboard
        mActivity.addTouchEventInterceptor(mTouchInterceptor);

        if(!mOnActivityResult) {
            if (!mHasSavedInstanceState) {
                // default view is all contacts
                int selectedItemPosition = 0;
                int chatCount = ChatManager.getChatCount(mDatabaseHelper.getReadableDatabase());
                if (chatCount <= 0) {
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
            Utils.hideKeyboard(mActivity, WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        } else {
            mHasSavedInstanceState = false;
        }
        mSearchListBarView.removeSearchTextFocus(null);

        View v = getView();
        if(v != null) {
            v.requestFocus();
        }

        if(mController.getPreferences().isFirstRun()) {
            mHandler.postDelayed(mTipRunnable, 100);
        }

        mActivity.getTrackerManager().trackScreenView(SCREEN_ID);
    }

    @Override
    public void onStop() {

        // global touch interceptor to hide keyboard
        mActivity.removeTouchEventInterceptor(mTouchInterceptor);

        mHandler.removeCallbacks(mAnimationRunnable);

        mSearchListBarView.setOnSearchListener(null);

        if(mGetRecipientsTask != null && !mGetRecipientsTask.isCancelled() && mGetRecipientsTask.getStatus() != AsyncTask.Status.FINISHED) {
            mGetRecipientsTask.cancel(true);
        }

        // close adapter cursors
        mChatAdapter.changeCursor(null);
        mRecipientAdapter.changeCursor(null);

        dismissPopup();

        mActivity.getOverlayManager().removeOverlayVisibilityChangeListener(this);

        mController.stop();

        super.onStop();
    }

    @Override
    public void onDestroy() {
        mSearchListBarView.deinit();
        mTipPopup.setOnDismissListener(null);
        mTipPopup.dismiss();

        mController.deinit();
        mController.setContext(null);
        mActivity = null;

        super.onDestroy();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQUEST_NEWCONTACT) {
            mOnActivityResult = true;
            if(resultCode == Activity.RESULT_OK) {
                ContactRaw contact = (ContactRaw) data.getSerializableExtra(NewContactActivity.KEY_RECIPIENT);
                if(contact != null) {
                    mSearchListBarView.setSearchText(contact.getDisplayName());
                }
            } else {
                mSearchListBarView.clearSearch(0);
            }
        } else if(requestCode == REQUEST_NEWCONTACT_AND_SEND) {
            mController.handleNewContactResult(resultCode, data);
        }
    }

    @Override
    public void onNewContact(Intent intentToLaunchActivity) {
        startActivityForResult(intentToLaunchActivity, REQUEST_NEWCONTACT_AND_SEND);
    }

    protected void launchChatActivity(long chatId) {
        Intent chatIntent = new Intent(mActivity, ChatActivity.class);
        chatIntent.putExtra(ChatActivity.PARAM_CHAT_ID, chatId);
        startActivity(chatIntent);
    }

    public int clearFilters() {
        return (mSearchListBarView.clearSearch(0) ? 1 : 0);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
        Intent intent = new Intent(mActivity, ChatActivity.class);

        if(getRealAdapter() instanceof ContactCursorAdapter) {
            showPopup(view);
        } else {
            intent.putExtra(ChatActivity.PARAM_CHAT_ID, mChatAdapter.getChat(position).getId());
            startActivity(intent);
        }
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        if(getRealAdapter() instanceof ChatCursorAdapter) {
            return mController.triggerRecording(view, mChatAdapter.getChat(position));
        }

        // create the chat instance if non-existent
        Chat tappedChat = new Chat();
        tappedChat.setTitle(mRecipientAdapter.getRecipient(position).getDisplayName());
        tappedChat.addRecipient(new ChatRecipient(0, mRecipientAdapter.getRecipient(position), DateContainer.getCurrentUTCTimestamp()));
        return mController.triggerRecording(view, tappedChat);
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
        if(mHoldPopup.isShowing() && !isDetached() && mActivity != null) {
            mHoldPopup.dismiss();
            mHandler.removeCallbacks(mDismissPopupRunnable);
        }
    }

    // the method that displays the img_popup.
    private void showPopup(View parent) {
        if(!mHoldPopup.isShowing() && !isDetached() && mActivity != null) {
            dismissPopup();
            mHoldPopup.showAtLocation(parent, Gravity.NO_GRAVITY, mLastTouchPoint.x - Utils.dpToPx(mActivity, 120), mLastTouchPoint.y + Utils.dpToPx(mActivity, 10));
            mHandler.postDelayed(mDismissPopupRunnable, 6000);
        }
    }

    /**
     * When a footer or header is added to the ListView, the adapter set through setAdapter gets
     * wrapped by a HeaderViewListAdapter. This returns the wrapped adapter.
     * @return the real, wrapped adapter
     */
    private ListAdapter getRealAdapter() {
        return getListView().getAdapter() == null ? null : ((HeaderViewListAdapter) getListView().getAdapter()).getWrappedAdapter();
    }
}
