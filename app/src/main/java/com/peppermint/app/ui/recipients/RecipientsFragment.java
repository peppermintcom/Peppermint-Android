package com.peppermint.app.ui.recipients;

import android.animation.Animator;
import android.app.Activity;
import android.app.ListFragment;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.peppermint.app.PeppermintApp;
import com.peppermint.app.R;
import com.peppermint.app.SenderServiceManager;
import com.peppermint.app.data.Recipient;
import com.peppermint.app.data.RecipientType;
import com.peppermint.app.sending.SendingEvent;
import com.peppermint.app.ui.CustomActionBarActivity;
import com.peppermint.app.ui.recording.RecordingActivity;
import com.peppermint.app.ui.recording.RecordingFragment;
import com.peppermint.app.ui.views.PeppermintLoadingView;
import com.peppermint.app.ui.views.SearchListBarAdapter;
import com.peppermint.app.ui.views.SearchListBarView;
import com.peppermint.app.utils.AnimatorBuilder;
import com.peppermint.app.utils.FilteredCursor;
import com.peppermint.app.utils.PepperMintPreferences;
import com.peppermint.app.utils.Utils;

import java.util.List;

public class RecipientsFragment extends ListFragment implements AdapterView.OnItemClickListener, SearchListBarView.OnSearchListener {

    public static final int REQUEST_RECORD = 1;

    // Keys to save the Instance State
    private static final String RECIPIENT_TYPE_POS_KEY = "RecipientsFragment_RecipientTypePosition";
    private static final String RECIPIENT_TYPE_SEARCH_KEY = "RecipientsFragment_RecipientTypeSearch";

    private PepperMintPreferences mPreferences;
    private CustomActionBarActivity mActivity;

    // The Recipient List
    private View mRecipientListContainer;
    private View mRecipientLoadingContainer;
    private PeppermintLoadingView mRecipientLoadingView;
    private boolean mRecipientListShown;
    private BaseAdapter mRecipientAdapter;

    // The Custom Action Bar (with Recipient Type Filter and Recipient Search)
    private SearchListBarView mSearchListBarView;
    private SearchListBarAdapter<RecipientType> mRecipientTypeAdapter;

    // Bottom bar
    private AnimatorBuilder mAnimatorBuilder;
    private SenderServiceManager mSendRecordManager;
    private View lytStatus;
    private TextView txtStatus;
    private ImageView imgStatus;
    private SenderServiceManager.Listener mSendRecordListener = new SenderServiceManager.Listener() {

        private void hide(int delay) {
            if(lytStatus.getVisibility() == View.VISIBLE) {
                Animator anim = mAnimatorBuilder.buildSlideOutBottomAnimator(delay, lytStatus, 0);
                anim.addListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        lytStatus.setVisibility(View.GONE);
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {

                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {

                    }
                });
                anim.start();
            }
        }

        private void show() {
            if(lytStatus.getVisibility() == View.GONE) {
                Animator anim = mAnimatorBuilder.buildFadeSlideInBottomAnimator(lytStatus);
                anim.start();
            }
            lytStatus.setVisibility(View.VISIBLE);
        }

        private void showAndHide() {
            if(lytStatus.getVisibility() != View.GONE) {
                hide(5000);
            } else {
                Animator anim = mAnimatorBuilder.buildFadeSlideInBottomAnimator(lytStatus);
                anim.addListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        hide(5000);
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {

                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {

                    }
                });
                anim.start();
                lytStatus.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public void onBoundSendService() {
            if(mSendRecordManager.isSending()) {
                txtStatus.setText(getString(R.string.sending));
                imgStatus.setVisibility(View.GONE);
                show();
            } else {
                hide(500);
            }
        }

        @Override
        public void onSendStarted(SendingEvent event) {
            onBoundSendService();
        }

        @Override
        public void onSendCancelled(SendingEvent event) {
            onBoundSendService();
        }

        @Override
        public void onSendError(SendingEvent event) {
            onBoundSendService();
        }

        @Override
        public void onSendFinished(SendingEvent event) {
            if(!mSendRecordManager.isSending()) {
                showAndHide();
                txtStatus.setText(getString(R.string.sent));
                imgStatus.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public void onSendProgress(SendingEvent event) {
        }

        @Override
        public void onSendQueued(SendingEvent event) {
            onBoundSendService();
        }
    };

    public RecipientsFragment() {
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        mActivity = (CustomActionBarActivity) activity;
        mPreferences = new PepperMintPreferences(activity);
        mSendRecordManager = new SenderServiceManager(activity);
        mSendRecordManager.setListener(mSendRecordListener);
        mAnimatorBuilder = new AnimatorBuilder();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        PeppermintApp app = (PeppermintApp) getActivity().getApplication();

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
        mRecipientTypeAdapter = new SearchListBarAdapter<>(app.getFontSemibold(), mActivity, RecipientType.getAll(mActivity));
        mSearchListBarView.setListAdapter(mRecipientTypeAdapter);
        mSearchListBarView.setTypeface(app.getFontRegular());

        int selectedItemPosition = 0;
        if (savedInstanceState != null) {
            selectedItemPosition = savedInstanceState.getInt(RECIPIENT_TYPE_POS_KEY, 0);
            mSearchListBarView.setSearchText(savedInstanceState.getString(RECIPIENT_TYPE_SEARCH_KEY, null));
        }
        mSearchListBarView.setSelectedItemPosition(selectedItemPosition);
        mSearchListBarView.setOnSearchListener(this);

        mActivity.getCustomActionBar().setContents(mSearchListBarView, false);

        // inflate the view
        View v = inflater.inflate(R.layout.f_recipients_layout, container, false);

        // init no recipients view
        TextView txtEmpty1 = (TextView) v.findViewById(R.id.txtEmpty1);
        TextView txtEmpty2 = (TextView) v.findViewById(R.id.txtEmpty2);
        txtEmpty1.setTypeface(app.getFontSemibold());
        txtEmpty2.setTypeface(app.getFontSemibold());
        int peppermintColor = Utils.getColor(getActivity(), R.color.green_text);
        txtEmpty2.setText(Html.fromHtml(String.format(getString(R.string.msg_add_some_friends), String.format("#%06X", (0xFFFFFF & peppermintColor)))));

        // init loading recipients view
        mRecipientListShown = true;
        mRecipientLoadingContainer = v.findViewById(R.id.progressContainer);
        mRecipientLoadingView = (PeppermintLoadingView) v.findViewById(R.id.loading);

        // init recipient list view
        mRecipientListContainer =  v.findViewById(R.id.listContainer);

        // bottom status bar
        lytStatus = v.findViewById(R.id.lytStatus);
        txtStatus = (TextView) v.findViewById(R.id.txtStatus);
        imgStatus = (ImageView) v.findViewById(R.id.imgStatus);

        txtStatus.setTypeface(app.getFontSemibold());

        return v;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getListView().setOnItemClickListener(this);
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
        mSendRecordManager.bind();
        //mRecipientLoadingView.start();
    }

    @Override
    public void onStop() {
        super.onStop();
        mSendRecordManager.unbind();
        mSearchListBarView.setOnSearchListener(null);
        // just stop the loading view in case it is animated
        //mRecipientLoadingView.stop();
    }

    @Override
    public void onDestroy() {
        mSearchListBarView.deinit();
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
        mRecipientListShown = shown;
        if (shown) {
            if (animate && getActivity() != null) {
                mRecipientLoadingContainer.startAnimation(AnimationUtils.loadAnimation(
                        getActivity(), android.R.anim.fade_out));
                mRecipientListContainer.startAnimation(AnimationUtils.loadAnimation(
                        getActivity(), android.R.anim.fade_in));
            }
            mRecipientLoadingContainer.setVisibility(View.INVISIBLE);
            mRecipientListContainer.setVisibility(View.VISIBLE);
            //mRecipientLoadingView.start();
        } else {
            if (animate && getActivity() != null) {
                mRecipientLoadingContainer.startAnimation(AnimationUtils.loadAnimation(
                        getActivity(), android.R.anim.fade_in));
                mRecipientListContainer.startAnimation(AnimationUtils.loadAnimation(
                        getActivity(), android.R.anim.fade_out));
            }
            mRecipientLoadingContainer.setVisibility(View.VISIBLE);
            mRecipientListContainer.setVisibility(View.INVISIBLE);
            //mRecipientLoadingView.stop();
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
        Intent recordIntent = new Intent(getActivity(), RecordingActivity.class);

        Recipient recipient = mRecipientAdapter instanceof RecipientCursorAdapter ?
                ((RecipientCursorAdapter) mRecipientAdapter).getRecipient(position) :
                ((RecipientArrayAdapter) mRecipientAdapter).getItem(position);

        recordIntent.putExtra(RecordingFragment.INTENT_RECIPIENT_EXTRA, recipient);
        startActivityForResult(recordIntent, REQUEST_RECORD);
    }

    @Override
    public void onSearch(String filter) {
        RecipientType recipientType = (RecipientType) mSearchListBarView.getSelectedItem();
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
