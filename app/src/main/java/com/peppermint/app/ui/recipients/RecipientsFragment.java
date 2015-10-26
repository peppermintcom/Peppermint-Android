package com.peppermint.app.ui.recipients;

import android.Manifest;
import android.animation.Animator;
import android.app.Activity;
import android.app.ListFragment;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
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
import com.peppermint.app.ui.canvas.avatar.AnimatedAvatarView;
import com.peppermint.app.ui.canvas.progress.LoadingView;
import com.peppermint.app.ui.recording.RecordingActivity;
import com.peppermint.app.ui.recording.RecordingFragment;
import com.peppermint.app.ui.views.SearchListBarAdapter;
import com.peppermint.app.ui.views.SearchListBarView;
import com.peppermint.app.utils.AnimatorBuilder;
import com.peppermint.app.utils.FilteredCursor;
import com.peppermint.app.utils.PepperMintPreferences;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RecipientsFragment extends ListFragment implements AdapterView.OnItemClickListener, SearchListBarView.OnSearchListener {

    public static final int REQUEST_RECORD = 1;

    // keys to save the instance state
    private static final String RECIPIENT_TYPE_POS_KEY = "RecipientsFragment_RecipientTypePosition";
    private static final String RECIPIENT_TYPE_SEARCH_KEY = "RecipientsFragment_RecipientTypeSearch";

    private static final int FIXED_AVATAR_ANIMATION_INTERVAL_MS = 15000;
    private static final int VARIABLE_AVATAR_ANIMATION_INTERVAL_MS = 15000;

    private PepperMintPreferences mPreferences;
    private CustomActionBarActivity mActivity;

    // the recipient list
    private View mRecipientListContainer;
    private View mRecipientLoadingContainer;
    private LoadingView mRecipientLoadingView;
    private boolean mRecipientListShown;
    private BaseAdapter mRecipientAdapter;

    // the custom action bar (with recipient type filter and recipient search)
    private SearchListBarView mSearchListBarView;
    private SearchListBarAdapter<RecipientType> mRecipientTypeAdapter;

    // bottom bar
    private AnimatorBuilder mAnimatorBuilder;
    private SenderServiceManager mSendRecordManager;
    private View lytStatus;
    private TextView txtStatus, txtTapToCancel;
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
                txtTapToCancel.setVisibility(View.VISIBLE);
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
                txtTapToCancel.setVisibility(View.GONE);
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

    private final Random mRandom = new Random();
    private final Handler mHandler = new Handler();
    private final Runnable mAnimationRunnable = new Runnable() {
        @Override
        public void run() {
            List<AnimatedAvatarView> possibleAnimationsList = new ArrayList<>();

            for(int i=0; i<getListView().getChildCount(); i++) {
                AnimatedAvatarView v = (AnimatedAvatarView) getListView().getChildAt(i).findViewById(R.id.imgPhoto);
                if(!v.isShowStaticAvatar()) {
                    possibleAnimationsList.add(v);
                }
            }

            int index = possibleAnimationsList.size() > 0 ? mRandom.nextInt(possibleAnimationsList.size()) : 0;

            for(int i=0; i<possibleAnimationsList.size(); i++) {
                AnimatedAvatarView v = possibleAnimationsList.get(i);
                if(i == index) {
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
        mRecipientTypeAdapter = new SearchListBarAdapter<>(app.getFontRegular(), mActivity, RecipientType.getAll(mActivity));
        mSearchListBarView.setListAdapter(mRecipientTypeAdapter);
        mSearchListBarView.setTypeface(app.getFontRegular());

        int selectedItemPosition = 0;
        if (savedInstanceState != null) {
            selectedItemPosition = savedInstanceState.getInt(RECIPIENT_TYPE_POS_KEY, 0);
            mSearchListBarView.setSearchText(savedInstanceState.getString(RECIPIENT_TYPE_SEARCH_KEY, null));
        } else {
            if(!hasRecentsOrFavourites()) {
                // select "all contacts" in case there are not fav/recent contacts
                selectedItemPosition = 1;
            }
        }
        mSearchListBarView.setSelectedItemPosition(selectedItemPosition);
        //mSearchListBarView.setOnSearchListener(this);

        mActivity.getCustomActionBar().setContents(mSearchListBarView, false);

        // inflate the view
        View v = inflater.inflate(R.layout.f_recipients_layout, container, false);

        // init no recipients view
        TextView txtEmpty1 = (TextView) v.findViewById(R.id.txtEmpty1);
        TextView txtEmpty2 = (TextView) v.findViewById(R.id.txtEmpty2);
        txtEmpty1.setTypeface(app.getFontSemibold());
        txtEmpty2.setTypeface(app.getFontSemibold());
        int peppermintColor = ContextCompat.getColor(getActivity(), R.color.green_text);
        txtEmpty2.setText(Html.fromHtml(String.format(getString(R.string.msg_add_some_friends), String.format("#%06X", (0xFFFFFF & peppermintColor)))));

        // init loading recipients view
        mRecipientListShown = true;
        mRecipientLoadingContainer = v.findViewById(R.id.progressContainer);
        mRecipientLoadingView = (LoadingView) v.findViewById(R.id.loading);

        // init recipient list view
        mRecipientListContainer =  v.findViewById(R.id.listContainer);

        // bottom status bar
        lytStatus = v.findViewById(R.id.lytStatus);
        txtStatus = (TextView) v.findViewById(R.id.txtStatus);
        txtTapToCancel = (TextView) v.findViewById(R.id.txtTapToCancel);
        imgStatus = (ImageView) v.findViewById(R.id.imgStatus);

        txtStatus.setTypeface(app.getFontSemibold());
        txtTapToCancel.setTypeface(app.getFontSemibold());

        lytStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSendRecordManager.cancel();
                txtTapToCancel.setVisibility(View.GONE);
                txtStatus.setText(R.string.cancelling);
            }
        });

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

        mHandler.postDelayed(mAnimationRunnable, FIXED_AVATAR_ANIMATION_INTERVAL_MS + mRandom.nextInt(VARIABLE_AVATAR_ANIMATION_INTERVAL_MS));
    }

    @Override
    public void onStop() {
        super.onStop();
        mHandler.removeCallbacks(mAnimationRunnable);

        mSendRecordManager.unbind();
        mSearchListBarView.setOnSearchListener(null);
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

            mRecipientLoadingView.stopAnimations();
            mRecipientLoadingView.stopDrawingThread();
        } else {
            if (animate && getActivity() != null) {
                mRecipientLoadingContainer.startAnimation(AnimationUtils.loadAnimation(
                        getActivity(), android.R.anim.fade_in));
                mRecipientListContainer.startAnimation(AnimationUtils.loadAnimation(
                        getActivity(), android.R.anim.fade_out));
            }
            mRecipientLoadingContainer.setVisibility(View.VISIBLE);
            mRecipientListContainer.setVisibility(View.INVISIBLE);

            mRecipientLoadingView.startAnimations();
            mRecipientLoadingView.startDrawingThread();
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

        if (ContextCompat.checkSelfPermission(mActivity,
                Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {

            RecipientType recipientType = (RecipientType) mSearchListBarView.getSelectedItem();
            List<Long> recentList = mPreferences.getRecentContactUris();

            if (recentList != null && recentList.size() > 0 && recipientType.isStarred() != null && recipientType.isStarred()) {
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
                    if(getActivity() == null) {
                        return;
                    }
                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && getActivity().isDestroyed()) {
                        return;
                    }

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

    private boolean hasRecentsOrFavourites() {
        if (ContextCompat.checkSelfPermission(mActivity,
                Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return false;
        }

        List<Long> recentList = mPreferences.getRecentContactUris();
        if(recentList != null && recentList.size() > 0) {
            return true;
        }

        FilteredCursor cursor = (FilteredCursor) RecipientAdapterUtils.getRecipientsCursor(getActivity(), null, null, true, mRecipientTypeAdapter.getItem(0).getMimeTypes());
        return cursor != null && cursor.getOriginalCursor() != null && cursor.getOriginalCursor().moveToFirst();
    }
}
