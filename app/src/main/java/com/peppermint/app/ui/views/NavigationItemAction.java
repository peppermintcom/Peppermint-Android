package com.peppermint.app.ui.views;

import android.app.Fragment;

/**
 * Created by Nuno Luz on 04-04-2016.
 */
public interface NavigationItemAction {
    void onPreFragmentInit();
    void onPostFragmentInit(Fragment currentFragment, boolean isNewInstance);
}

