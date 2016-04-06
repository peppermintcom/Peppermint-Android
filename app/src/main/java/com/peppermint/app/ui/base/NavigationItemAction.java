package com.peppermint.app.ui.base;

import android.app.Fragment;

/**
 * Created by Nuno Luz on 04-04-2016.
 *
 * {@link NavigationItem} fragment action. Allows the execution of pre and post fragment
 * initialization routines.
 */
public interface NavigationItemAction {
    void onPreFragmentInit(Fragment newFragment, boolean isNewInstance);
    void onPostFragmentInit(Fragment currentFragment, boolean isNewInstance);
}

