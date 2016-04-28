package com.peppermint.app.ui.recipients;

import android.Manifest;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
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
import com.peppermint.app.cloud.MessagesServiceManager;
import com.peppermint.app.data.ChatManager;
import com.peppermint.app.data.ContactManager;
import com.peppermint.app.data.ContactRaw;
import com.peppermint.app.data.DatabaseHelper;
import com.peppermint.app.ui.Overlay;
import com.peppermint.app.ui.OverlayManager;
import com.peppermint.app.ui.PermissionsPolicyEnforcer;
import com.peppermint.app.ui.about.AboutActivity;
import com.peppermint.app.ui.base.CustomActionBarView;
import com.peppermint.app.ui.base.NavigationItem;
import com.peppermint.app.ui.base.NavigationItemSimpleAction;
import com.peppermint.app.ui.base.activities.CustomActionBarDrawerActivity;
import com.peppermint.app.ui.base.dialogs.PopupDialog;
import com.peppermint.app.ui.feedback.FeedbackActivity;
import com.peppermint.app.ui.settings.SettingsActivity;
import com.peppermint.app.utils.Utils;

import java.util.ArrayList;
import java.util.List;

public class ContactActivity extends CustomActionBarDrawerActivity implements SearchListBarView.OnSearchListener,
        OverlayManager.OverlayVisibilityChangeListener {

    protected static final int SEARCH_LISTENER_PRIORITY_FRAGMENT = 1;
    protected static final int SEARCH_LISTENER_PRIORITY_ACTIVITY = 2;

    // intent params
    public static final String FAST_REPLY_NAME_PARAM = "name";
    public static final String FAST_REPLY_MAIL_PARAM = "mail";

    private SearchListBarView mSearchListBarView;

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

                if (!(v instanceof Button) && !(v instanceof EditText)) {
                    mSearchListBarView.removeSearchTextFocus(event);
                    getContainerView().requestFocus();
                }

                mSearchTipPopup.dismiss();
            }
            return false;
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
    protected void onSetupPermissions(PermissionsPolicyEnforcer permissionsPolicyEnforcer) {
        super.onSetupPermissions(permissionsPolicyEnforcer);
        permissionsPolicyEnforcer.addPermission(Manifest.permission.READ_CONTACTS, false);
        permissionsPolicyEnforcer.addPermission("android.permission.READ_PROFILE", false);
        permissionsPolicyEnforcer.addPermission(Manifest.permission.WRITE_CONTACTS, false);
        permissionsPolicyEnforcer.addPermission(Manifest.permission.RECORD_AUDIO, false);
        permissionsPolicyEnforcer.addPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, false);
        permissionsPolicyEnforcer.addPermission(Manifest.permission.INTERNET, false);
        permissionsPolicyEnforcer.addPermission(Manifest.permission.ACCESS_NETWORK_STATE, false);
        permissionsPolicyEnforcer.addPermission(Manifest.permission.GET_ACCOUNTS, false);
        permissionsPolicyEnforcer.addPermission("android.permission.USE_CREDENTIALS", false);
        permissionsPolicyEnforcer.addPermission(Manifest.permission.SEND_SMS, true, PackageManager.FEATURE_TELEPHONY);
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
                mPreferences.setFirstRun(false);

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
            final MessagesServiceManager messagesServiceManager = new MessagesServiceManager(this);
            messagesServiceManager.startAndSync();
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
    }

    @Override
    protected void onStart() {
        super.onStart();

        mOverlayManager.addOverlayVisibilityChangeListener(this);

        // global touch interceptor to hide keyboard
        addTouchEventInterceptor(mTouchInterceptor);

        getAuthenticationData(getIntentReplica());
    }

    @Override
    protected void onResume() {
        super.onResume();

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

        super.onStop();
    }

    @Override
    protected void onDestroy() {
        mSearchListBarView.removeOnSearchListener(this);

        mSearchTipPopup.setOnDismissListener(null);
        mSearchTipPopup.dismiss();

        mIsDestroyed = true;

        super.onDestroy();
    }

    @Override
    protected void onPermissionsAccepted() {
        super.onPermissionsAccepted();
        refreshContactList();
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
