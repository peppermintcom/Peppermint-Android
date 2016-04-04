package com.peppermint.app.ui.base;

import android.app.Fragment;

/**
 * Created by Nuno Luz on 04-04-2016.
 *
 * Avoid having to implements all methods from
 * {@link com.peppermint.app.ui.base.NavigationItemAction} every time.
 */
public class NavigationItemSimpleAction implements NavigationItemAction {
    @Override
    public void onPreFragmentInit() {
        /* nothing to do */
    }

    @Override
    public void onPostFragmentInit(Fragment currentFragment, boolean isNewInstance) {
        /* nothing to do */
    }
}
