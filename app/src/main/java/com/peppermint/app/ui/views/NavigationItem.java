package com.peppermint.app.ui.views;

import android.app.Fragment;

/**
 * Created by Nuno Luz (nluz@mobaton.com) on 21-08-2015.
 *
 * Represents the data of an item in the navigation drawer menu.
 */
public class NavigationItem {

    protected String mTitle;
    protected int mIconResId;
    protected Class<? extends Fragment> mFragmentClass;
    protected Runnable mRunnable;
    protected String mTag;
    protected boolean mShowSeparator = false;
    protected boolean mVisible = true;

    public NavigationItem(String title, int iconResId, Class<? extends Fragment> fragmentClass) {
        this.mIconResId = iconResId;
        this.mTitle = title;
        this.mFragmentClass = fragmentClass;
        if(fragmentClass != null) {
            this.mTag = fragmentClass.getName();
        }
    }

    public NavigationItem(String title, int iconResId, Class<? extends Fragment> fragmentClass, boolean showSeparator, boolean isVisible) {
        this(title, iconResId, fragmentClass);
        this.mShowSeparator = showSeparator;
        this.mVisible = isVisible;
    }

    public NavigationItem(String title, int iconResId, Runnable runnable, boolean showSeparator) {
        this(title, iconResId, null);
        this.mShowSeparator = showSeparator;
        this.mRunnable = runnable;
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

    public Runnable getRunnable() {
        return mRunnable;
    }

    public void setRunnable(Runnable mRunnable) {
        this.mRunnable = mRunnable;
    }

    public boolean isVisible() {
        return mVisible;
    }

    public void setVisible(boolean mVisible) {
        this.mVisible = mVisible;
    }
}
