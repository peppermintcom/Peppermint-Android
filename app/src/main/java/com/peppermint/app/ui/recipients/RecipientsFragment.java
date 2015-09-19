package com.peppermint.app.ui.recipients;

import android.app.Activity;
import android.app.ListFragment;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.peppermint.app.PeppermintApp;
import com.peppermint.app.R;
import com.peppermint.app.data.Recipient;
import com.peppermint.app.data.RecipientType;
import com.peppermint.app.ui.recording.RecordFragment;
import com.peppermint.app.ui.recording.RecordingActivity;
import com.peppermint.app.ui.views.ActionBarListAdapter;
import com.peppermint.app.ui.views.ActionBarView;
import com.peppermint.app.ui.views.PeppermintLoadingView;
import com.peppermint.app.utils.FilteredCursor;
import com.peppermint.app.utils.PepperMintPreferences;
import com.peppermint.app.utils.Utils;

import java.util.List;

public class RecipientsFragment extends ListFragment implements AdapterView.OnItemClickListener, ActionBarView.OnSearchListener {

    private static final int REQUEST_RECORD = 1;

    // Keys to save the Instance State
    private static final String RECIPIENT_TYPE_POS_KEY = "RecipientsFragment_RecipientTypePosition";
    private static final String RECIPIENT_TYPE_SEARCH_KEY = "RecipientsFragment_RecipientTypeSearch";

    private PepperMintPreferences mPreferences;

    // The Menu Button
    private ImageButton mMenuButton;

    // The Custom Action Bar (with Recipient Type Filter and Recipient Search)
    private ActionBarView mActionBarView;
    private ActionBarListAdapter<RecipientType> mRecipientTypeAdapter;

    // The Recipient List
    private View mRecipientListContainer;
    private View mRecipientLoadingContainer;
    private PeppermintLoadingView mRecipientLoadingView;
    private boolean mRecipientListShown;
    private BaseAdapter mRecipientAdapter;

    public RecipientsFragment() {
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mPreferences = new PepperMintPreferences(activity);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        PeppermintApp app = (PeppermintApp) getActivity().getApplication();

        View v = inflater.inflate(R.layout.f_recipients_layout, container, false);

        // bo: adjust status bar height
        int statusBarHeight = Utils.getStatusBarHeight(getActivity());
        v.findViewById(R.id.customActionBarTopSpace).getLayoutParams().height = statusBarHeight;
        View lytActionBar = v.findViewById(R.id.customActionBar);
        lytActionBar.getLayoutParams().height = lytActionBar.getLayoutParams().height + statusBarHeight;
        // eo: adjust status bar height

        // global touch interceptor to hide keyboard
        v.findViewById(R.id.touchInterceptor).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    mActionBarView.removeSearchTextFocus(event);
                }
                return false;
            }
        });

        // init custom action bar view
        mRecipientTypeAdapter = new ActionBarListAdapter(app.getFontSemibold(), getActivity(), RecipientType.getAll(getActivity()));

        mActionBarView = (ActionBarView) v.findViewById(R.id.lytRecipientSearchAndList);
        mActionBarView.setTypeface(app.getFontRegular());
        mActionBarView.setListAdapter(mRecipientTypeAdapter);

        int selectedItemPosition = 0;
        if(savedInstanceState != null) {
            selectedItemPosition = savedInstanceState.getInt(RECIPIENT_TYPE_POS_KEY, 0);
            mActionBarView.setSearchText(savedInstanceState.getString(RECIPIENT_TYPE_SEARCH_KEY, null));
        }
        mActionBarView.setSelectedItemPosition(selectedItemPosition);

        // init menu view
        mMenuButton = (ImageButton) v.findViewById(R.id.menu);
        mMenuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showMenu(v);
            }
        });

        // init no recipients view
        TextView txtEmpty1 = (TextView) v.findViewById(R.id.txtEmpty1);
        TextView txtEmpty2 = (TextView) v.findViewById(R.id.txtEmpty2);
        txtEmpty1.setTypeface(app.getFontSemibold());
        txtEmpty2.setTypeface(app.getFontSemibold());
        int peppermintColor = getResources().getColor(R.color.green_text);
        txtEmpty2.setText(Html.fromHtml(String.format(getString(R.string.msg_add_some_friends), String.format("#%06X", (0xFFFFFF & peppermintColor)))));

        // init loading recipients view
        mRecipientListShown = true;
        mRecipientLoadingContainer = v.findViewById(R.id.progressContainer);
        mRecipientLoadingView = (PeppermintLoadingView) v.findViewById(R.id.loading);

        // init recipient list view
        mRecipientListContainer =  v.findViewById(R.id.listContainer);

        return v;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getListView().setOnItemClickListener(this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(RECIPIENT_TYPE_POS_KEY, mActionBarView.getSelectedItemPosition());
        outState.putString(RECIPIENT_TYPE_SEARCH_KEY, mActionBarView.getSearchText());
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onStart() {
        super.onStart();
        onSearch(mActionBarView.getSearchText());
        mActionBarView.setOnSearchListener(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        mActionBarView.setOnSearchListener(null);
        // just stop the loading view in case it is animated
        mRecipientLoadingView.stop();
    }

    @Override
    public void onDestroy() {
        mActionBarView.deinit();
        if(mRecipientAdapter != null && mRecipientAdapter instanceof RecipientCursorAdapter) {
            // this closes the cursor inside the adapter
            ((RecipientCursorAdapter) mRecipientAdapter).changeCursor(null);
        }
        super.onDestroy();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQUEST_RECORD) {
            if(resultCode == Activity.RESULT_OK) {
                // if the user has gone through the sending process without
                // discarding the recording, then clear the search filter
                mActionBarView.clearSearch(0);
            }
        }
    }

    protected void setListShown(boolean shown, boolean animate){
        if (mRecipientListShown == shown) {
            return;
        }
        mRecipientListShown = shown;
        if (shown) {
            if (animate && getActivity() != null) {
                mRecipientLoadingContainer.startAnimation(AnimationUtils.loadAnimation(
                        getActivity(), android.R.anim.fade_out));
                mRecipientListContainer.startAnimation(AnimationUtils.loadAnimation(
                        getActivity(), android.R.anim.fade_in));
            }
            mRecipientLoadingContainer.setVisibility(View.GONE);
            mRecipientListContainer.setVisibility(View.VISIBLE);
            mRecipientLoadingView.start();
        } else {
            if (animate && getActivity() != null) {
                mRecipientLoadingContainer.startAnimation(AnimationUtils.loadAnimation(
                        getActivity(), android.R.anim.fade_in));
                mRecipientListContainer.startAnimation(AnimationUtils.loadAnimation(
                        getActivity(), android.R.anim.fade_out));
            }
            mRecipientLoadingContainer.setVisibility(View.VISIBLE);
            mRecipientListContainer.setVisibility(View.INVISIBLE);
            mRecipientLoadingView.stop();
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

    private void showMenu(View v) {
        PopupMenu popup = new PopupMenu(getActivity(), v);
        popup.inflate(R.menu.menu_main);
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {

            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    default:
                        return false;
                }
            }

        });
        popup.show();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Intent recordIntent = new Intent(getActivity(), RecordingActivity.class);

        Recipient recipient = mRecipientAdapter instanceof RecipientCursorAdapter ?
                ((RecipientCursorAdapter) mRecipientAdapter).getRecipient(position) :
                ((RecipientArrayAdapter) mRecipientAdapter).getItem(position);

        recordIntent.putExtra(RecordFragment.INTENT_RECIPIENT_EXTRA, recipient);
        startActivityForResult(recordIntent, REQUEST_RECORD);
    }

    public int clearFilters() {
        if(mActionBarView.isShowingList()) {
            mActionBarView.hideList();
            return 2;
        }
        return (mActionBarView.clearSearch(0) ? 1 : 0);
    }

    @Override
    public void onSearch(String filter) {
        RecipientType recipientType = (RecipientType) mActionBarView.getSelectedItem();

        List<Long> recentList = mPreferences.getRecentContactUris();

        if(recentList != null && recentList.size() > 0 && recipientType.isStarred() != null && recipientType.isStarred()) {
            Long[] recentArray = recentList.toArray(new Long[recentList.size()]);
            if (mRecipientAdapter != null && mRecipientAdapter instanceof RecipientCursorAdapter) {
                ((RecipientCursorAdapter) mRecipientAdapter).changeCursor(null);
            }
            mRecipientAdapter = RecipientArrayAdapter.get((PeppermintApp) getActivity().getApplication(), getActivity(), recentArray);
            getListView().setAdapter(mRecipientAdapter);
            mRecipientAdapter.notifyDataSetChanged();
            return;
        }

        FilteredCursor cursor = (FilteredCursor) RecipientAdapterUtils.getRecipientsCursor(getActivity(), null, filter, recipientType.isStarred(), recipientType.getMimeTypes());
        setListShown(false);
        cursor.filterAsync(new FilteredCursor.FilterCallback() {
            @Override
            public void done(FilteredCursor cursor) {
                if (mRecipientAdapter != null && mRecipientAdapter instanceof RecipientCursorAdapter) {
                    ((RecipientCursorAdapter) mRecipientAdapter).changeCursor(cursor);
                } else {
                    mRecipientAdapter = new RecipientCursorAdapter((PeppermintApp) getActivity().getApplication(), getActivity(), cursor);
                    getListView().setAdapter(mRecipientAdapter);
                }
                mRecipientAdapter.notifyDataSetChanged();
                setListShown(true);
            }
        });
    }
}
