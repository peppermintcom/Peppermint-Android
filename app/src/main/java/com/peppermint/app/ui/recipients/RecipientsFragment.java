package com.peppermint.app.ui.recipients;

import android.Manifest;
import android.animation.Animator;
import android.app.Activity;
import android.app.ListFragment;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.text.Html;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.peppermint.app.PeppermintApp;
import com.peppermint.app.R;
import com.peppermint.app.RecordService;
import com.peppermint.app.RecordServiceManager;
import com.peppermint.app.SenderServiceManager;
import com.peppermint.app.data.Recipient;
import com.peppermint.app.data.RecipientType;
import com.peppermint.app.data.Recording;
import com.peppermint.app.sending.SendingEvent;
import com.peppermint.app.sending.nativemail.IntentMailSendingTask;
import com.peppermint.app.ui.CustomActionBarActivity;
import com.peppermint.app.ui.canvas.avatar.AnimatedAvatarView;
import com.peppermint.app.ui.canvas.progress.LoadingView;
import com.peppermint.app.ui.views.RecordingOverlayView;
import com.peppermint.app.ui.views.SearchListBarAdapter;
import com.peppermint.app.ui.views.SearchListBarView;
import com.peppermint.app.utils.AnimatorBuilder;
import com.peppermint.app.utils.FilteredCursor;
import com.peppermint.app.utils.NoMicDataIOException;
import com.peppermint.app.utils.PepperMintPreferences;
import com.peppermint.app.utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RecipientsFragment extends ListFragment implements AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener, View.OnTouchListener,
        SearchListBarView.OnSearchListener, RecordServiceManager.Listener {

    // recording overlay
    private static final String RECORDING_OVERLAY_TAG = "RECORDING";
    private static final int RECORDING_OVERLAY_HIDE_DELAY = 1000;

    // keys to save the instance state
    private static final String RECIPIENT_TYPE_POS_KEY = "RecipientsFragment_RecipientTypePosition";
    private static final String RECIPIENT_TYPE_SEARCH_KEY = "RecipientsFragment_RecipientTypeSearch";

    // avatar animation frequency
    private static final int FIXED_AVATAR_ANIMATION_INTERVAL_MS = 15000;
    private static final int VARIABLE_AVATAR_ANIMATION_INTERVAL_MS = 15000;

    private PepperMintPreferences mPreferences;
    private CustomActionBarActivity mActivity;
    private RecordingOverlayView mRecordingViewOverlay;
    private boolean mSendRecording = false;

    // swipe-related
    private float x1, x2, y1, y2;
    private long t1, t2;
    private int mMinSwipeDistance;
    private static final int MIN_SWIPE_DISTANCE_DP = 60;        // min swipe distance
    private static final int MAX_SWIPE_DURATION = 300;        // max swipe duration

    private MediaPlayer mRecordSoundPlayer;

    // the recipient list
    private View mRecipientListContainer;
    private View mRecipientLoadingContainer;
    private LoadingView mRecipientLoadingView;
    private boolean mRecipientListShown;
    private BaseAdapter mRecipientAdapter;

    private PopupWindow mHoldPopup;
    private Point mLastTouchPoint = new Point();
    private Runnable mDismissPopupRunnable = new Runnable() {
        @Override
        public void run() {
            dismissPopup();
        }
    };

    // the custom action bar (with recipient type filter and recipient search)
    private SearchListBarView mSearchListBarView;
    private SearchListBarAdapter<RecipientType> mRecipientTypeAdapter;

    // recording service
    private RecordServiceManager mRecordManager;

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
            onBoundSendService(null);
        }

        private void onBoundSendService(SendingEvent event) {
            if(mSendRecordManager.isSending()) {
                // do not show message for IntentMailSender
                if(event != null && event.getSendingTask() instanceof IntentMailSendingTask) {
                    return;
                }

                txtStatus.setText(getString(R.string.uploading));
                txtTapToCancel.setVisibility(View.VISIBLE);
                imgStatus.setVisibility(View.GONE);
                show();
            } else {
                hide(500);
            }
        }

        @Override
        public void onSendStarted(SendingEvent event) {
            onBoundSendService(event);
        }

        @Override
        public void onSendCancelled(SendingEvent event) {
            onBoundSendService(event);
        }

        @Override
        public void onSendError(SendingEvent event) {
            onBoundSendService(event);
        }

        @Override
        public void onSendFinished(SendingEvent event) {
            if(!mSendRecordManager.isSending()) {
                // do not show message for IntentMailSender
                if(event != null && event.getSendingTask() instanceof IntentMailSendingTask) {
                    hide(0);
                    return;
                }
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
            onBoundSendService(event);
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

        mRecordManager = new RecordServiceManager(activity);
        mRecordManager.setListener(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        PeppermintApp app = (PeppermintApp) getActivity().getApplication();

        mSendRecordManager.start();

        mRecordSoundPlayer = MediaPlayer.create(mActivity, R.raw.s_record);

        mMinSwipeDistance = Utils.dpToPx(mActivity, MIN_SWIPE_DISTANCE_DP);

        mRecordingViewOverlay = (RecordingOverlayView) mActivity.createOverlay(R.layout.v_recording_overlay_layout, RECORDING_OVERLAY_TAG, false, true);

        // hold popup
        mHoldPopup = new PopupWindow(mActivity);
        mHoldPopup.setContentView(inflater.inflate(R.layout.v_recipients_popup, null));
        //noinspection deprecation
        // although this is deprecated, it is required for versions  < 22/23, otherwise the popup doesn't show up
        mHoldPopup.setWindowLayoutMode(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        mHoldPopup.setBackgroundDrawable(Utils.getDrawable(mActivity, R.drawable.img_popup));
        mHoldPopup.setAnimationStyle(R.style.Peppermint_PopupAnimation);
        // do not let the popup get in the way of user interaction
        mHoldPopup.setFocusable(false);
        mHoldPopup.setTouchable(false);

        // global touch interceptor to hide keyboard
        mActivity.getTouchInterceptor().setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    mSearchListBarView.removeSearchTextFocus(event);
                }
                dismissPopup();
                mLastTouchPoint.set((int) event.getX(), (int) event.getY());
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

        mRecordManager.start(false);

        return v;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getListView().setOnItemClickListener(this);
        getListView().setLongClickable(true);
        getListView().setOnItemLongClickListener(this);
        getListView().setOnTouchListener(this);
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
        mRecordManager.bind();

        mHandler.postDelayed(mAnimationRunnable, FIXED_AVATAR_ANIMATION_INTERVAL_MS + mRandom.nextInt(VARIABLE_AVATAR_ANIMATION_INTERVAL_MS));
    }

    @Override
    public void onPause() {
        if(hideRecordingOverlay(false)) {
            mSendRecording = false;
            mRecordManager.stopRecording(true);
        }

        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        mHandler.removeCallbacks(mAnimationRunnable);

        mRecordManager.unbind();
        mSendRecordManager.unbind();
        mSearchListBarView.setOnSearchListener(null);
    }

    @Override
    public void onDestroy() {
        if(mRecordSoundPlayer.isPlaying()) {
            mRecordSoundPlayer.stop();
        }
        mRecordSoundPlayer.release();

        mSearchListBarView.deinit();
        if(mRecipientAdapter != null && mRecipientAdapter instanceof RecipientCursorAdapter) {
            // this closes the cursor inside the adapter
            ((RecipientCursorAdapter) mRecipientAdapter).changeCursor(null);
        }
        super.onDestroy();
    }

    public boolean showRecordingOverlay() {
        mRecordingViewOverlay.start();
        return mActivity.showOverlay(RECORDING_OVERLAY_TAG, true, null);
    }

    public boolean hideRecordingOverlay(boolean animated) {
        //mRecordingViewOverlay.blinkLeft();
        mRecordingViewOverlay.stop();
        return mActivity.hideOverlay(RECORDING_OVERLAY_TAG, RECORDING_OVERLAY_HIDE_DELAY, animated);   // FIXME animated hide is buggy
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
        showPopup(mActivity, view);
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        setRecordDuration(0);

        // start recording
        if(showRecordingOverlay()) {
            if(mRecordSoundPlayer.isPlaying()) {
                mRecordSoundPlayer.stop();
            }
            mRecordSoundPlayer.seekTo(0);
            mRecordSoundPlayer.start();

            Recipient recipient = mRecipientAdapter instanceof RecipientCursorAdapter ?
                    ((RecipientCursorAdapter) mRecipientAdapter).getRecipient(position) :
                    ((RecipientArrayAdapter) mRecipientAdapter).getItem(position);

            mRecordingViewOverlay.setName(recipient.getName());
            mRecordingViewOverlay.setVia(recipient.getVia());
            String filename = getString(R.string.filename_message_from) + Utils.normalizeAndCleanString(mPreferences.getDisplayName());
            mRecordManager.startRecording(filename, recipient);
        }
        return true;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch(event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                x1 = event.getX();
                y1 = event.getY();
                t1 = android.os.SystemClock.uptimeMillis();
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                x2 = event.getX();
                y2 = event.getY();
                t2 = android.os.SystemClock.uptimeMillis();
                float deltaX = x2 - x1;
                float deltaY = y2 - y1;

                if(hideRecordingOverlay(true)) {
                    mSendRecording = false;

                    if ((Math.abs(deltaX) > mMinSwipeDistance || Math.abs(deltaY) > mMinSwipeDistance) && (t2 - t1) < MAX_SWIPE_DURATION) {
                        // swipe: cancel
                        mRecordManager.stopRecording(true);
                    } else {
                        // release: send
                        if (mRecordingViewOverlay.getMillis() < 2000) {
                            Toast.makeText(mActivity, R.string.msg_record_at_least, Toast.LENGTH_SHORT).show();
                            mRecordManager.stopRecording(true);
                        } else {
                            mSendRecording = true;
                            mRecordManager.stopRecording(false);
                        }
                    }
                }
                break;
            default:
                long now = android.os.SystemClock.uptimeMillis();
                if((now - t1) > MAX_SWIPE_DURATION) {
                    t1 = now;
                    x1 = event.getX();
                    y1 = event.getY();
                }
        }

        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        onSearch(mSearchListBarView.getSearchText());
    }

    @Override
    public void onSearch(String filter) {
        // check permission
        if(ContextCompat.checkSelfPermission(mActivity,
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

    private void dismissPopup() {
        if(mHoldPopup.isShowing() && !isDetached()) {
            mHoldPopup.dismiss();
            mHandler.removeCallbacks(mDismissPopupRunnable);
        }
    }

    // the method that displays the img_popup.
    private void showPopup(final Activity context, View parent) {
        dismissPopup();
        mHoldPopup.showAtLocation(parent, Gravity.NO_GRAVITY, mLastTouchPoint.x - Utils.dpToPx(mActivity, 80), mLastTouchPoint.y + Utils.dpToPx(mActivity, 10));
        mHandler.postDelayed(mDismissPopupRunnable, 2000);
    }

    @Override
    public void onStartRecording(RecordService.Event event) {
        onBoundRecording(event.getRecording(), event.getRecipient(), event.getLoudness());
    }

    @Override
    public void onStopRecording(RecordService.Event event) {
        onBoundRecording(event.getRecording(), event.getRecipient(), event.getLoudness());
        if(mSendRecording) {
            mPreferences.addRecentContactUri(event.getRecipient().getContactId());

            // if the user has gone through the sending process without
            // discarding the recording, then clear the search filter
            mSearchListBarView.clearSearch(0);

            mSendRecordManager.startAndSend(event.getRecipient(), event.getRecording());
            mSendRecording = false;
        }
    }

    @Override
    public void onResumeRecording(RecordService.Event event) {
        onBoundRecording(event.getRecording(), event.getRecipient(), event.getLoudness());
    }

    @Override
    public void onPauseRecording(RecordService.Event event) {
        onBoundRecording(event.getRecording(), event.getRecipient(), event.getLoudness());
    }

    private void setRecordDuration(float fullDuration) {
        mRecordingViewOverlay.setMillis(fullDuration);
    }

    @Override
    public void onLoudnessRecording(RecordService.Event event) {
        mRecordingViewOverlay.pushAmplitude(event.getLoudness());
        setRecordDuration(event.getRecording().getDurationMillis());
    }

    @Override
    public void onErrorRecording(RecordService.Event event) {
        if(event.getError() instanceof NoMicDataIOException) {
            Toast.makeText(getActivity(), getString(R.string.msg_message_nomicdata_error), Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(getActivity(), getString(R.string.msg_message_record_error), Toast.LENGTH_LONG).show();
        }
        hideRecordingOverlay(true);
    }

    @Override
    public void onBoundRecording(Recording currentRecording, Recipient currentRecipient, float currentLoudness) {
        setRecordDuration(currentRecording == null ? 0 : currentRecording.getDurationMillis());
    }

}
