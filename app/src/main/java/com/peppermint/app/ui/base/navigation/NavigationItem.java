package com.peppermint.app.ui.base.navigation;

import android.app.Fragment;

/**
 * Created by Nuno Luz (nluz@mobaton.com) on 21-08-2015.
 *
 * Represents the data of an item in the navigation drawer menu.
 */
public class NavigationItem {

    protected String mTitle;                                // label to present in the drawer menu
    protected int mIconResId;                               // icon to present in the drawer menu
    protected boolean mShowSeparator = false;               // show a separator in the drawer menu?

    protected NavigationItemAction mAction;                 // to execute when tapped
    protected Class<? extends Fragment> mFragmentClass;     // fragment to show when tapped

    protected String mTag;                                  // tag of the fragment

    public NavigationItem(String title, int iconResId, Class<? extends Fragment> fragmentClass) {
        this.mIconResId = iconResId;
        this.mTitle = title;
        this.mFragmentClass = fragmentClass;
        if(fragmentClass != null) {
            this.mTag = fragmentClass.getName();
        }
    }

    public NavigationItem(String title, int iconResId, Class<? extends Fragment> fragmentClass, boolean showSeparator, NavigationItemAction action) {
        this(title, iconResId, fragmentClass);
        this.mShowSeparator = showSeparator;
        this.mAction = action;
    }

    public NavigationItem(String title, int iconResId, NavigationItemAction action, boolean showSeparator) {
        this(title, iconResId, null);
        this.mShowSeparator = showSeparator;
        this.mAction = action;
    }

    public String getTitle() {
        return mTitle;
    }

    public void setTitle(String mTitle) {
        this.mTitle = mTitle;
    }

    public int getIconResId() {
        return mIconResId;
    }

    public void setIconResId(int mIconResId) {
        this.mIconResId = mIconResId;
    }

    public Class<? extends Fragment> getFragmentClass() {
        return mFragmentClass;
    }

    public void setFragmentClass(Class<? extends Fragment> mFragmentClass) {
        this.mFragmentClass = mFragmentClass;
    }

    public String getTag() {
        return mTag;
    }

    public void setTag(String mTag) {
        this.mTag = mTag;
    }

    public boolean isShowSeparator() {
        return mShowSeparator;
    }

    public void setShowSeparator(boolean mShowSeparator) {
        this.mShowSeparator = mShowSeparator;
    }

    public NavigationItemAction getAction() {
        return mAction;
    }

    public void setAction(NavigationItemAction mAction) {
        this.mAction = mAction;
    }

}
