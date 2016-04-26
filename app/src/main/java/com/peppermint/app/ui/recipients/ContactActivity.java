package com.peppermint.app.ui.recipients;

import android.app.Activity;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.peppermint.app.R;
import com.peppermint.app.authenticator.AuthenticationData;
import com.peppermint.app.data.Chat;
import com.peppermint.app.data.ChatManager;
import com.peppermint.app.data.ContactManager;
import com.peppermint.app.data.ContactRaw;
import com.peppermint.app.data.DatabaseHelper;
import com.peppermint.app.data.Message;
import com.peppermint.app.data.Recording;
import com.peppermint.app.events.MessageEvent;
import com.peppermint.app.events.PeppermintEventBus;
import com.peppermint.app.events.ReceiverEvent;
import com.peppermint.app.events.SenderEvent;
import com.peppermint.app.events.SyncEvent;
import com.peppermint.app.ui.Overlay;
import com.peppermint.app.ui.OverlayManager;
import com.peppermint.app.ui.about.AboutActivity;
import com.peppermint.app.ui.base.CustomActionBarView;
import com.peppermint.app.ui.base.NavigationItem;
import com.peppermint.app.ui.base.NavigationItemSimpleAction;
import com.peppermint.app.ui.base.activities.CustomActionBarDrawerActivity;
import com.peppermint.app.ui.base.dialogs.PopupDialog;
import com.peppermint.app.ui.chat.ChatActivity;
import com.peppermint.app.ui.chat.recorder.ChatRecordOverlayController;
import com.peppermint.app.ui.feedback.FeedbackActivity;
import com.peppermint.app.ui.recipients.add.NewContactActivity;
import com.peppermint.app.ui.settings.SettingsActivity;
import com.peppermint.app.utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ContactActivity extends CustomActionBarDrawerActivity implements SearchListBarView.OnSearchListener,
        ChatRecordOverlayController.Callbacks, OverlayManager.OverlayVisibilityChangeListener, View.OnClickListener {

    protected static final int SEARCH_LISTENER_PRIORITY_FRAGMENT = 1;
    protected static final int SEARCH_LISTENER_PRIORITY_ACTIVITY = 2;

    protected static final int REQUEST_NEWCONTACT = 224;
    protected static final int REQUEST_NEWCONTACT_AND_SEND = 223;

    private static final Pattern VIA_PATTERN = Pattern.compile("<([^\\s]*)>");

    // intent params
    public static final String FAST_REPLY_NAME_PARAM = "name";
    public static final String FAST_REPLY_MAIL_PARAM = "mail";

    private ChatRecordOverlayController mChatRecordOverlayController;
    private SearchListBarView mSearchListBarView;

    private boolean mShouldSync = true;
    private boolean mHasSavedInstanceState = false;
    private boolean mIsDestroyed = false;

    // search tip popup
    private Handler mHandler = new Handler();
    private PopupDialog mSearchTipPopup;
    private PointF mSearchTipPoint = new PointF();
    private final Runnable mSearchTipRunnable = new Runnable() {
        @Override
        public void run() {
            if (mSearchTipPopup != null && !mSearchTipPopup.isShowing() && !mIsDestroyed) {
                final CustomActionBarView customActionBarView = getCustomActionBar();
                if(customActionBarView.getHeight() <= 0) {
                    mHandler.postDelayed(mSearchTipRunnable, 100);
                } else {
                    mSearchTipPopup.show(customActionBarView);
                }
            }
        }
    };

    @Override
    public boolean onSearch(String searchText, boolean wasClear) {
        boolean switchedFragment;

        if(wasClear || (mTappedItemPosition == 0 && searchText == null)) {
            // recent contacts / chats
            switchedFragment = selectItemFromDrawer(0);
        } else {
            // all contacts
            switchedFragment = selectItemFromDrawer(1);
        }

        return switchedFragment;
    }

    private View.OnTouchListener mTouchInterceptor = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                // do not do it if the add contact button was pressed
                // since this might be causing the tap event to not work
                // the onClick event already has instructions to do this
                if(v == null) {
                    v = Utils.getClickableViewAtLocation(event.getRawX(), event.getRawY(), getContainerView());
                }

                if (!(v instanceof Button)) {
                    mSearchListBarView.removeSearchTextFocus(event);
                    getContainerView().requestFocus();
                }

                mSearchTipPopup.dismiss();
            }
            return false;
        }
    };

    private Object mMessageEventListener = new Object() {
        public void onEventMainThread(MessageEvent event) {
            if(event.getType() == MessageEvent.EVENT_MARK_PLAYED) {
                refreshContactList();
            }
        }
    };

    @Override
    protected List<NavigationItem> getNavigationItems() {
        final List<NavigationItem> navItems = new ArrayList<>();
        navItems.add(new NavigationItem(getString(R.string.drawer_menu_recent_contacts), R.drawable.ic_drawer_recent, RecentContactsListFragment.class, false, R.string.loading_contacts, null));
        navItems.add(new NavigationItem(getString(R.string.drawer_menu_all_contacts), R.drawable.ic_drawer_contacts, AllContactsListFragment.class, false, R.string.loading_contacts, null));
        navItems.add(new NavigationItem(getString(R.string.drawer_menu_settings), R.drawable.ic_drawer_settings, new NavigationItemSimpleAction() {
            @Override
            public void onPreFragmentInit(Fragment newFragment, boolean isNewInstance) {
                Intent intent = new Intent(ContactActivity.this, SettingsActivity.class);
                startActivity(intent);
            }
        }, true));
        navItems.add(new NavigationItem(getString(R.string.drawer_menu_help_feedback), R.drawable.ic_drawer_feedback, new NavigationItemSimpleAction() {
            @Override
            public void onPreFragmentInit(Fragment newFragment, boolean isNewInstance) {
                Intent intent = new Intent(ContactActivity.this, FeedbackActivity.class);
                startActivity(intent);
            }
        }, true));
        navItems.add(new NavigationItem(getString(R.string.drawer_menu_about), R.drawable.ic_drawer_help, new NavigationItemSimpleAction() {
            @Override
            public void onPreFragmentInit(Fragment newFragment, boolean isNewInstance) {
                Intent intent = new Intent(ContactActivity.this, AboutActivity.class);
                startActivity(intent);
            }
        }, true));
        return navItems;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mHasSavedInstanceState = savedInstanceState != null;

        // show recent contacts if recents are empty
        if(!mHasSavedInstanceState) {
            if(ChatManager.getChatCount(DatabaseHelper.getInstance(this).getReadableDatabase(), true) <= 0) {
                this.mTappedItemPosition = 1;
            } else {
                this.mTappedItemPosition = 0;
            }
            getDrawerListView().setItemChecked(this.mTappedItemPosition, true);
        }

        // inflate and init custom action bar view
        final CustomActionBarView customActionBarView = getCustomActionBar();

        mSearchListBarView = (SearchListBarView) LayoutInflater.from(this).inflate(R.layout.f_recipients_actionbar, null, false);

        customActionBarView.setContents(mSearchListBarView, false);

        // chat record overlay controller
        mChatRecordOverlayController = new ChatRecordOverlayController(this, this) {
            @Override
            public void onEventMainThread(SyncEvent event) {
                super.onEventMainThread(event);
                if(event.getType() == SyncEvent.EVENT_FINISHED) {
                    if(mSearchListBarView.getSearchText() == null) {
                        refreshContactList();
                    }
                }
            }

            @Override
            public void onEventMainThread(ReceiverEvent event) {
                super.onEventMainThread(event);
                if(event.getType() == ReceiverEvent.EVENT_RECEIVED) {
                    if(mSearchListBarView.getSearchText() == null) {
                        refreshContactList();
                    }
                }
            }

            @Override
            public void onEventMainThread(SenderEvent event) {
                super.onEventMainThread(event);
                if(event.getType() == SenderEvent.EVENT_FINISHED) {
                    if(mSearchListBarView.getSearchText() == null) {
                        refreshContactList();
                    }
                }
            }

            @Override
            protected Message sendMessage(Chat chat, Recording recording) {
                Message message = super.sendMessage(chat, recording);

                // if the user has gone through the sending process without
                // discarding the recording, then clear the search filter
                mSearchListBarView.clearSearch(true);

                launchChatActivity(message.getChatParameter().getPeppermintChatId() > 0 ? message.getChatParameter().getPeppermintChatId() : message.getChatParameter().getId());

                return message;
            }
        };

        mChatRecordOverlayController.init(getContainerView(), mOverlayManager,
                this, savedInstanceState);

        // search tip popup
        mSearchTipPopup = new PopupDialog(this) {
            @Override
            public boolean onTouchEvent(MotionEvent event) {
                mSearchTipPoint.set(event.getRawX(), event.getRawY());
                return super.onTouchEvent(event);
            }
        };
        mSearchTipPopup.setLayoutResource(R.layout.v_popup_tip1);
        mSearchTipPopup.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                // set first run to false to avoid showing it again
                mChatRecordOverlayController.getPreferences().setFirstRun(false);

                if (mSearchTipPopup != null) {
                    EditText searchEditText = mSearchListBarView.getSearchEditText();
                    if (searchEditText != null) {
                        Rect mSearchRect = new Rect();
                        searchEditText.getGlobalVisibleRect(mSearchRect);
                        searchEditText.setFocusableInTouchMode(true);
                        searchEditText.setFocusable(true);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            searchEditText.setShowSoftInputOnFocus(true);
                        }
                        if (mSearchRect.contains((int) mSearchTipPoint.x, (int) mSearchTipPoint.y) || (mSearchTipPoint.y < 0 && mSearchRect.contains((int) mSearchTipPoint.x, mSearchRect.centerY()))) {
                            searchEditText.requestFocus();
                            Utils.showKeyboard(ContactActivity.this, searchEditText, WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

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

        final AuthenticationData authenticationData = getAuthenticationData(getIntentReplica());

        // synchronize messages (only when the activity is created for the first time)
        if(savedInstanceState == null) {
            mChatRecordOverlayController.getMessagesServiceManager().startAndSync();
        }

        Intent paramIntent = getIntent();

        if(paramIntent != null && (paramIntent.hasExtra(FAST_REPLY_NAME_PARAM) || paramIntent.hasExtra(FAST_REPLY_MAIL_PARAM))) {
            String name = paramIntent.getStringExtra(FAST_REPLY_NAME_PARAM);
            String mail = paramIntent.getStringExtra(FAST_REPLY_MAIL_PARAM);

            if(name == null) { name = ""; }
            if(mail == null) { mail = ""; }

            if(mail.length() <= 0) {
                // if the email was not supplied, just search for the name
                mSearchListBarView.setSearchText(name);
            } else {
                // if mail is supplied, check if the contact exists
                ContactRaw foundRecipient = ContactManager.getRawContactByVia(ContactActivity.this, mail);

                // if not, add the contact
                if(foundRecipient == null) {
                    try {
                        foundRecipient = ContactManager.insert(ContactActivity.this, 0, 0, name, null, null, mail, null, authenticationData.getEmail(), false);
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

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mSearchListBarView.addOnSearchListener(this, SEARCH_LISTENER_PRIORITY_ACTIVITY);
        PeppermintEventBus.registerMessages(mMessageEventListener);
    }

    @Override
    protected void onStart() {
        super.onStart();

        mChatRecordOverlayController.start();

        mOverlayManager.addOverlayVisibilityChangeListener(this);

        // global touch interceptor to hide keyboard
        addTouchEventInterceptor(mTouchInterceptor);

        getAuthenticationData(getIntentReplica());
    }

    @Override
    protected void onResume() {
        super.onResume();
        mPermissionsManager.requestPermissions(this);

        // avoid cursor focus and keyboard when opening
        // if it is on onStart(), it doesn't work for screen rotations
        if(!mHasSavedInstanceState) {
            Utils.hideKeyboard(this, WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        } else {
            mHasSavedInstanceState = false;
        }

        // remove focus on search view
        mSearchListBarView.removeSearchTextFocus(null);
        View v = getContainerView();
        if(v != null) {
            v.requestFocus();
        }

        if(mPreferences.isFirstRun()) {
            mHandler.postDelayed(mSearchTipRunnable, 100);
        }
    }

    @Override
    protected void onStop() {
        removeTouchEventInterceptor(mTouchInterceptor);

        mOverlayManager.removeOverlayVisibilityChangeListener(this);

        mChatRecordOverlayController.stop();

        super.onStop();
    }

    @Override
    protected void onDestroy() {
        PeppermintEventBus.unregisterMessages(mMessageEventListener);

        mSearchListBarView.removeOnSearchListener(this);

        mSearchTipPopup.setOnDismissListener(null);
        mSearchTipPopup.dismiss();

        mChatRecordOverlayController.deinit();
        mChatRecordOverlayController.setContext(null);

        mIsDestroyed = true;

        super.onDestroy();
    }

    @Override
    public void onNewContact(Intent intentToLaunchActivity) {
        startActivityForResult(intentToLaunchActivity, REQUEST_NEWCONTACT_AND_SEND);
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
        } else if(requestCode == REQUEST_NEWCONTACT_AND_SEND) {
            mChatRecordOverlayController.handleNewContactResult(resultCode, data);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(mPermissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults)) {
            refreshContactList();
        } else {
            Toast.makeText(this, R.string.msg_must_supply_mandatory_permissions, Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        mChatRecordOverlayController.saveInstanceState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onBackPressed() {
        if(isDrawerOpen()) {
            super.onBackPressed();
            return;
        }

        int stepsLeft = (mSearchListBarView.clearSearch(false) ? 1 : 0);
        if (stepsLeft <= 0) {
            super.onBackPressed();
            return;
        }

        Toast.makeText(ContactActivity.this, R.string.msg_press_back_again_exit, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onOverlayShown(Overlay overlay) { /* nothing to do */ }

    @Override
    public void onOverlayHidden(Overlay overlay, boolean wasCancelled) {
        if(mSearchListBarView != null) {
            mSearchListBarView.clearFocus();
            ViewGroup v = getContainerView();
            if(v != null) {
                v.requestFocus();
            }
        }
    }

    private void launchChatActivity(long chatId) {
        Intent chatIntent = new Intent(this, ChatActivity.class);
        chatIntent.putExtra(ChatActivity.PARAM_CHAT_ID, chatId);
        startActivity(chatIntent);
    }

    protected static String[] getFilterData(String filter) {
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

    public ChatRecordOverlayController getChatRecordOverlayController() {
        return mChatRecordOverlayController;
    }

    @Override
    public void onClick(View v) {
        Intent intent = new Intent(ContactActivity.this, NewContactActivity.class);

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

        startActivityForResult(intent, ContactActivity.REQUEST_NEWCONTACT);
    }

    private void refreshContactList() {
        ContactListFragment fragment = (ContactListFragment) getCurrentFragment();
        boolean refresh = fragment != null;

        if(fragment == null) {
            refresh = !onSearch(mSearchListBarView.getSearchText(), false);
        }

        if(refresh) {
            fragment.refresh();
        }
    }

    public SearchListBarView getSearchListBarView() {
        return mSearchListBarView;
    }
}
