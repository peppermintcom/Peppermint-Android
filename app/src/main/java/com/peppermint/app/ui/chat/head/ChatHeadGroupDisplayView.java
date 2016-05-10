package com.peppermint.app.ui.chat.head;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import com.facebook.rebound.SpringSystem;
import com.peppermint.app.data.Chat;
import com.peppermint.app.utils.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Nuno Luz on 05-05-2016.
 *
 * Represents an interactive Peppermint chat head that can be expanded to show multiple chats.
 */
public class ChatHeadGroupDisplayView extends DisplayView {

    private static final String TAG = ChatHeadGroupDisplayView.class.getSimpleName();

    private class ChatHeadDisplayView extends DisplayView<ChatHeadView> {
        public ChatHeadDisplayView(Context mContext, Display mDisplay, SpringSystem mSpringSystem) {
            super(mContext, mDisplay, mSpringSystem);
        }

        @Override
        protected boolean setViewPositionNoUpdateUI(int x, int y, boolean isRebounding) {

            final int maxX = mDisplay.getDisplayWidth() - mView.getMeasuredWidth();

            // update badge orientation
            if(x < (maxX / 2f)) {
                if(mView.getBadgeOrientation() != ChatHeadView.BADGE_ORIENTATION_TOP_RIGHT) {
                    mView.setBadgeOrientation(ChatHeadView.BADGE_ORIENTATION_TOP_RIGHT);
                    mView.invalidate();
                    mView.requestLayout();
                }
            } else {
                if(mView.getBadgeOrientation() != ChatHeadView.BADGE_ORIENTATION_TOP_LEFT) {
                    mView.setBadgeOrientation(ChatHeadView.BADGE_ORIENTATION_TOP_LEFT);
                    mView.invalidate();
                    mView.requestLayout();
                }
            }

            return super.setViewPositionNoUpdateUI(x, y, isRebounding);
        }
    }

    /**
     * Chat head group state change listener.
     */
    public interface OnStateChangedListener {
        void onStateChanged(int oldState, int newState);
    }

    public interface OnChatHeadSelectedListener {
        void onChatHeadSelected(Chat selectedChat);
    }

    private static final String CHAT_HEAD_POSITION_X_KEY = TAG + "_chatHeadPositionX";
    private static final String CHAT_HEAD_POSITION_Y_KEY = TAG + "_chatHeadPositionY";

    protected static final float INITIAL_POSITION_PERCENT_Y = 0.2f;     // initial chat head position Y (% of height)
    protected static final int SNAP_VELOCITY_THRESHOLD_DP = 300;

    protected static final int EXPANDED_TOP_MARGIN_DP = 10;
    protected static final int EXPANDED_SPACING_DP = 10;
    protected static final int CHAT_HEAD_SIZE_DP = ChatHeadView.DEF_AVATAR_SIZE_DP + (ChatHeadView.DEF_AVATAR_BORDER_WIDTH_DP * 2);

    public static final int STATE_DRAGGING = 1;                 // while the user drags the chat head group
    public static final int STATE_SNAPPED = 2;                  // chat head at rest after snapping
    public static final int STATE_SNAPPING = 3;                 // while the chat head group is snapping to a new position
    public static final int STATE_EXPANDED = 4;                 // chat head at rest after expanding
    public static final int STATE_EXPANDING = 5;                // while the chat head group is expanded/expanding
    public static final int STATE_HIDDEN = 6;                   // chat head group is hidden

    protected static final int MAX_CHAT_HEADS = 4;
    protected static final int MAIN_CHAT_HEAD_INDEX = MAX_CHAT_HEADS - 1;

    // to avoid multiple doLayout() to be executed right after one another,
    // when only one was enough
    private Handler mHandler = new Handler(Looper.getMainLooper());
    private boolean mDoLayoutScheduled = false;
    public void scheduleDoLayout() {
        synchronized (this) {
            if (mDoLayoutScheduled) {
                return;
            }
            mDoLayoutScheduled = true;
        }

        mHandler.post(mDoLayoutRunnable);
    }
    private final Runnable mDoLayoutRunnable = new Runnable() {
        @Override
        public void run() {
            synchronized (ChatHeadGroupDisplayView.this) {
                doLayout();
            }
        }
    };

    protected SpringSystem mSpringSystem;

    protected HideDisplayView mHideDisplayView;
    protected List<ChatHeadDisplayView> mChatHeadDisplayViews = new ArrayList<>();
    // list of chats mapped to chat head views
    protected List<Chat> mChats = new ArrayList<>();

    private int mState, mPreviousState;
    private List<OnStateChangedListener> mOnStateChangedListeners = new ArrayList<>();
    private OnChatHeadSelectedListener mOnChatHeadSelectedListener;

    private int mChatHeadEstimatedSize;
    private int mExpandedTopMargin, mExpandedSpacing;
    private int mSnapVelocityThreshold;

    private OnInteractionListener<ChatHeadView> mOnInteractionListener = new OnInteractionListener<ChatHeadView>() {
        private boolean _snappedToRemove = false;

        @Override
        public void onAtRest(DisplayView<ChatHeadView> displayView) {
            if(mChatHeadDisplayViews.indexOf(displayView) == MAIN_CHAT_HEAD_INDEX) {
                if(mState == STATE_SNAPPING) {
                    setState(STATE_SNAPPED);
                } else if(mState == STATE_EXPANDING) {
                    setState(STATE_EXPANDED);
                }
            }
        }

        @Override
        public boolean onClick(DisplayView<ChatHeadView> displayView) {
            final ChatHeadView v = displayView.getView();
            if(mState == STATE_EXPANDING || mState == STATE_EXPANDED) {
                if(v.isSelected()) {
                    shrink(null);
                } else {
                    setSelected(displayView);
                }
            } else {
                if(!v.isSelected()) {
                    setSelected(displayView);
                }
                expand();
            }
            return true;
        }

        @Override
        public boolean onDragStarted(DisplayView<ChatHeadView> displayView, float offsetX, float offsetY, MotionEvent event) {
            if(mState == STATE_EXPANDING || mState == STATE_EXPANDED) {
                shrink((ChatHeadDisplayView) displayView);
            }

            setViewPosition((int) (event.getRawX() - (mChatHeadEstimatedSize / 2f)), (int) (event.getRawY() - (mChatHeadEstimatedSize / 2f)));
            return onDrag(displayView, offsetX, offsetY, event);
        }

        @Override
        public boolean onDrag(DisplayView<ChatHeadView> displayView, float offsetX, float offsetY, MotionEvent event) {
            setState(STATE_DRAGGING);

            // this must be invoked after the state change to enforce dimming of the screen after the OnStateChangedListener execution
            mHideDisplayView.show();

            final ChatHeadView chatHeadView = displayView.getView();
            final int chatHeadWidth = chatHeadView.getMeasuredWidth();
            final int chatHeadHeight = chatHeadView.getMeasuredHeight();

            if(mHideDisplayView.isInsideInfluence(event.getRawX(), event.getRawY())) {
                if(!_snappedToRemove) {
                    chatHeadView.setShowBadge(false);
                    final int displacementX = (int) ((mHideDisplayView.getView().getMeasuredWidth() - mChatHeadEstimatedSize) / 2f);
                    final int displacementY = (int) ((mHideDisplayView.getView().getMeasuredHeight() - mChatHeadEstimatedSize) / 2f);
                    doRebound(mHideDisplayView.getViewPositionX() + displacementX, mHideDisplayView.getViewPositionY() + displacementY, 0, 0, true);
                    //mHideDisplayView.scaleUp();
                    _snappedToRemove = true;
                }
                return true;
            }

            if(_snappedToRemove) {
                chatHeadView.setShowBadge(true);
                _snappedToRemove = false;
                //mHideDisplayView.scaleDown();
                setViewPosition((int) (event.getRawX() - chatHeadWidth), (int) (event.getRawY() - chatHeadHeight), false);
                return true;
            }

            setViewPosition((int) (getViewPositionX() - offsetX), (int) (getViewPositionY() - offsetY));
            return true;
        }

        @Override
        public boolean onDragFinished(DisplayView<ChatHeadView> displayView, float[] velocity, MotionEvent event) {
            if(mHideDisplayView.isInsideInfluence(getViewPositionX(), getViewPositionY())) {
                displayView.getView().setShowBadge(true);
                _snappedToRemove = false;
                hide();
                return true;
            }

            mHideDisplayView.hide();
            snapToClosestEdge(velocity);

            return true;
        }
    };

    public ChatHeadGroupDisplayView(Context mContext, Display mDisplay) {
        super(mContext, mDisplay);
        mSpringSystem = SpringSystem.create();

        mExpandedTopMargin = Utils.dpToPx(mContext, EXPANDED_TOP_MARGIN_DP);
        mExpandedSpacing = Utils.dpToPx(mContext, EXPANDED_SPACING_DP);
        mSnapVelocityThreshold = Utils.dpToPx(mContext, SNAP_VELOCITY_THRESHOLD_DP);
        mChatHeadEstimatedSize = Utils.dpToPx(mContext, CHAT_HEAD_SIZE_DP);

        // chat head views
        for(int i=0; i < MAX_CHAT_HEADS; i++) {
            final ChatHeadDisplayView chatHeadDisplayView = new ChatHeadDisplayView(mContext, mDisplay, mSpringSystem);
            chatHeadDisplayView.setView(new ChatHeadView(mContext), mChatHeadEstimatedSize, mChatHeadEstimatedSize, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
            chatHeadDisplayView.setTouchable(true);
            chatHeadDisplayView.addOnInteractionListener(mOnInteractionListener);
            mChatHeadDisplayViews.add(chatHeadDisplayView);
        }

        mHideDisplayView = new HideDisplayView(mContext, mDisplay);
    }

    protected void setState(int newState) {
        if(newState == mState) {
            return;
        }

        mPreviousState = mState;
        mState = newState;

        Log.d(TAG, "setState() # State = " + mState +  "  (Old = " + mPreviousState + ")");

        scheduleDoLayout();

        for(OnStateChangedListener listener : mOnStateChangedListeners) {
            listener.onStateChanged(mPreviousState, mState);
        }
    }

    /**
     * Goes through all ChatHeadView in case the measured width of some is invalid.
     * This assumes that all have the same width.
     * @return the measured width
     */
    private int getChatHeadViewWidth() {
        int viewWidth = 0;
        for(int i=MAX_CHAT_HEADS - 1; i >= 0 && viewWidth <= 0; i--) {
            final ChatHeadView v = mChatHeadDisplayViews.get(i).getView();
            if(v != null) {
                viewWidth = v.getMeasuredWidth();
                if(viewWidth <= 0) {
                    // retry after requesting layout
                    v.requestLayout();
                    viewWidth = v.getMeasuredWidth();
                }
            }
        }
        return viewWidth;
    }

    protected void doLayout() {
        Log.d(TAG, "doLayout() # State = " + mState);

        mHandler.removeCallbacks(mDoLayoutRunnable);
        mDoLayoutScheduled = false;

        final int viewWidth = getChatHeadViewWidth();

        int j = 0;
        final int startsAt = MAX_CHAT_HEADS - mChats.size();

        switch(mState) {
            case STATE_EXPANDED:
            case STATE_EXPANDING:
                for(int i=0; i<MAX_CHAT_HEADS; i++) {
                    final DisplayView<ChatHeadView> displayView = mChatHeadDisplayViews.get(MAX_CHAT_HEADS - 1 - i);
                    final int maxX = mDisplay.getDisplayWidth() - viewWidth;
                    displayView.doRebound(maxX - ((viewWidth * i) + (mExpandedSpacing * (i + 1))), mExpandedTopMargin, false);

                    final ChatHeadView chatHeadView = mChatHeadDisplayViews.get(i).getView();

                    if(i < startsAt) {
                        chatHeadView.setChat(null);
                        chatHeadView.setVisibility(View.GONE);
                    } else {
                        chatHeadView.setChat(mChats.get(j));
                        chatHeadView.setVisibility(View.VISIBLE);
                        j++;
                    }

                    chatHeadView.setShowBadge(true);
                    chatHeadView.setSelectable(true);
                    chatHeadView.invalidate();

                    displayView.show();
                }
                break;
            default:
                // setup view state
                j = 0;
                for (int i = 0; i < MAX_CHAT_HEADS; i++) {
                    final ChatHeadView chatHeadView = mChatHeadDisplayViews.get(i).getView();
                    final Chat previousChat = chatHeadView.getChat();

                    if(i < startsAt) {
                        chatHeadView.setChat(null);
                        chatHeadView.setVisibility(View.GONE);
                    } else {
                        chatHeadView.setChat(mChats.get(j));
                        chatHeadView.setVisibility(View.VISIBLE);
                        j++;
                    }

                    chatHeadView.setSelectable(false);
                    chatHeadView.setShowBadge(i == MAIN_CHAT_HEAD_INDEX);
                    chatHeadView.invalidate();

                    if(i != MAIN_CHAT_HEAD_INDEX) {
                        mChatHeadDisplayViews.get(i).hide();
                    } else {
                        if(previousChat == null || !previousChat.equals(chatHeadView.getChat())) {
                            // reset state to allow #setSelected to work
                            chatHeadView.setSelected(false);
                        }
                        setSelected(mChatHeadDisplayViews.get(i));
                    }
                }
                break;
        }
    }

    protected void setSelected(final DisplayView<ChatHeadView> displayView) {
        final ChatHeadView v = displayView.getView();
        if(v.getChat() == null || v.isSelected()) {
            return;
        }

        v.setSelected(true);
        displayView.invalidate();

        for(DisplayView<ChatHeadView> displayViewFromList : mChatHeadDisplayViews) {
            final ChatHeadView otherView = displayViewFromList.getView();
            if(!v.equals(otherView) && otherView.isSelected()) {
                otherView.setSelected(false);
                displayViewFromList.invalidate();
            }
        }

        if(mOnChatHeadSelectedListener != null) {
            mOnChatHeadSelectedListener.onChatHeadSelected(v.getChat());
        }
    }

    public Chat getSelectedChat() {
        for(int i=0; i < MAX_CHAT_HEADS; i++) {
            final ChatHeadView v = mChatHeadDisplayViews.get(i).getView();
            if(v.isSelected()) {
                return v.getChat();
            }
        }
        return null;
    }

    private int getViewIndexForChat(final Chat chat) {
        for(int i=0; i < MAX_CHAT_HEADS; i++) {
            final ChatHeadView v = mChatHeadDisplayViews.get(i).getView();
            if(v.getChat() != null && v.getChat().equals(chat)) {
                return i;
            }
        }
        return -1;
    }

    public void addChat(Chat chat) {
        int foundChatHeadIndex = getViewIndexForChat(chat);
        if(foundChatHeadIndex >= 0) {
            mChats.remove(chat);
        }
        mChats.add(chat);

        // enforce the max. chat head amount
        if(mChats.size() > MAX_CHAT_HEADS) {
            mChats.remove(0);
        }

        Log.d(TAG, "addChat() # " + mChats.size() + " . " + chat.toString());

        scheduleDoLayout();
    }

    @Override
    public void onDisplaySizeObtained(int prevDisplayWidth, int prevDisplayHeight, int displayWidth, int displayHeight) {
        scheduleDoLayout();
    }

    public void resetToSavedPosition(boolean useRebound) {
        float[] startPos = loadChatHeadPosition();

        final ChatHeadView chatHeadView = getMainChatHeadDisplayView().getView();
        final int maxX = mDisplay.getDisplayWidth() - chatHeadView.getMeasuredWidth();
        final int maxY = mDisplay.getDisplayHeight() - chatHeadView.getMeasuredHeight();

        float x = startPos[0] < 0.5f ? 0 : maxX;
        float y = startPos[1] * (float) maxY;

        snapTo((int) x, (int) y, 0, 0, useRebound);
    }

    public void saveChatHeadPosition() {
        final ChatHeadView chatHeadView = getMainChatHeadDisplayView().getView();
        final int maxX = mDisplay.getDisplayWidth() - chatHeadView.getMeasuredWidth();
        final int maxY = mDisplay.getDisplayHeight() - chatHeadView.getMeasuredHeight();

        // do not allow 0 values since it will just reset the position
        // 0 only in case there's no data
        float x = getViewPositionX() / (float) maxX;
        float y = getViewPositionY() / (float) maxY;

        if(x < 0) { x = 0; }
        if(y < 0) { y = 0; }
        if(x > 1) { x = 1; }
        if(y > 1) { y = 1; }

        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(mContext).edit();
        editor.putFloat(CHAT_HEAD_POSITION_X_KEY, x);
        editor.putFloat(CHAT_HEAD_POSITION_Y_KEY, y);
        editor.apply();
    }

    public float[] loadChatHeadPosition() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        float x = sharedPreferences.getFloat(CHAT_HEAD_POSITION_X_KEY, 0);
        float y = sharedPreferences.getFloat(CHAT_HEAD_POSITION_Y_KEY, INITIAL_POSITION_PERCENT_Y);
        return new float[]{x, y};
    }

    public void clearChatHeadSavedPosition() {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(mContext).edit();
        editor.remove(CHAT_HEAD_POSITION_X_KEY);
        editor.remove(CHAT_HEAD_POSITION_Y_KEY);
        editor.apply();
    }

    public void expand() {
        // expand
        saveChatHeadPosition();
        setState(STATE_EXPANDING);
    }

    public void shrink(ChatHeadDisplayView draggingChatHeadDisplayView) {
        if(draggingChatHeadDisplayView == null) {
            // push selected chat to the top
            addChat(getSelectedChat());
            doLayout();
            resetToSavedPosition(true);
        } else {
            final int displayViewIndex = mChatHeadDisplayViews.indexOf(draggingChatHeadDisplayView);
            if(displayViewIndex >= 0 && displayViewIndex != MAIN_CHAT_HEAD_INDEX) {
                // push selected chat to the top
                addChat(draggingChatHeadDisplayView.getView().getChat());
                // make sure the dragged element is on top
                mChatHeadDisplayViews.remove(displayViewIndex);
                mChatHeadDisplayViews.add(draggingChatHeadDisplayView);

                if(!draggingChatHeadDisplayView.getView().isSelected()) {
                    setSelected(draggingChatHeadDisplayView);
                }
            }
            snapTo(getViewPositionX(), getViewPositionY(), 0, 0, false);
        }
    }

    public void snapTo(int x, int y, float velX, float velY) {
        snapTo(x, y, velX, velY, true);
    }

    public void snapTo(int x, int y, float velX, float velY, boolean useRebound) {
        // setup position of chat head
        if(useRebound && mSpringSystem != null) {
            setState(STATE_SNAPPING);
            doRebound(x, y, velX, velY, true);
        } else {
            setState(STATE_SNAPPED);
            setViewPosition(x, y);
        }
    }

    public void snapToClosestEdge(float[] velocity) {
        setState(STATE_SNAPPING);
        final ChatHeadView chatHeadView = getMainChatHeadDisplayView().getView();

        final int x = getViewPositionX();
        final int y = getViewPositionY();
        final int maxX = mDisplay.getDisplayWidth() - chatHeadView.getMeasuredWidth();
        final int maxY = mDisplay.getDisplayHeight() - chatHeadView.getMeasuredHeight();

        int newX = x < (maxX / 2f) ? 0 : maxX;
        int newY = y;

        if(velocity != null && Math.abs(velocity[0]) > mSnapVelocityThreshold) {
            newX = velocity[0] > 0 ? maxX : 0;
            // rule of three from velocity
            newY = y + (int) (velocity[1] * (newX - x) / velocity[0]);
            if (newY < 0) {
                newY = 0;
            } else if (newY > maxY) {
                newY = maxY;
            }
        }

        if(velocity == null) {
            doRebound(newX, newY, true);
        } else {
            doRebound(newX, newY, velocity[0], velocity[1], true);
        }
    }

    @Override
    public void init() {
        super.init();

        mHideDisplayView.init();

        for(DisplayView<ChatHeadView> chatHeadWindowManagerView : mChatHeadDisplayViews) {
            chatHeadWindowManagerView.init();
        }

        resetToSavedPosition(false);
    }

    @Override
    public void deinit() {
        super.deinit();

        mHideDisplayView.deinit();

        for(DisplayView<ChatHeadView> chatHeadWindowManagerView : mChatHeadDisplayViews) {
            chatHeadWindowManagerView.deinit();
        }
    }

    @Override
    public void invalidate() {
        for(DisplayView<ChatHeadView> chatHeadWindowManagerView : mChatHeadDisplayViews) {
            chatHeadWindowManagerView.invalidate();
        }
    }

    @Override
    public boolean show() {
        if(mState == STATE_HIDDEN) {
            resetToSavedPosition(false);
        }

        for(int i=0; i < MAX_CHAT_HEADS; i++) {
            if(i != MAIN_CHAT_HEAD_INDEX) {
                mChatHeadDisplayViews.get(i).hide();
            }
        }
        return getMainChatHeadDisplayView().show();
    }

    @Override
    public boolean hide() {
        mHideDisplayView.hide();

        for(int i=0; i < MAX_CHAT_HEADS; i++) {
            if(i != MAIN_CHAT_HEAD_INDEX) {
                mChatHeadDisplayViews.get(i).hide();
            }
        }

        final boolean result = getMainChatHeadDisplayView().hide();
        setState(STATE_HIDDEN);

        clearChatHeadSavedPosition();

        return result;
    }

    @Override
    public void doRebound(int x, int y, float velX, float velY, boolean startAtRest) {
        mChatHeadDisplayViews.get(MAIN_CHAT_HEAD_INDEX).doRebound(x, y, velX, velY, startAtRest);
    }

    @Override
    public void doRebound(int x, int y, boolean startAtRest) {
        mChatHeadDisplayViews.get(MAIN_CHAT_HEAD_INDEX).doRebound(x, y, startAtRest);
    }

    @Override
    protected void setViewPosition(int x, int y, boolean isRebounding) {
        mChatHeadDisplayViews.get(MAIN_CHAT_HEAD_INDEX).setViewPosition(x, y, isRebounding);
    }

    @Override
    public int getViewPositionX() {
        return mChatHeadDisplayViews.get(MAIN_CHAT_HEAD_INDEX).getViewPositionX();
    }

    @Override
    public int getViewPositionY() {
        return mChatHeadDisplayViews.get(MAIN_CHAT_HEAD_INDEX).getViewPositionY();
    }

    @Override
    public void setViewScale(float w, float h) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setViewAlpha(float alpha) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isVisible() {
        return mChatHeadDisplayViews.get(MAIN_CHAT_HEAD_INDEX).isVisible();
    }

    public int getPreviousState() {
        return mPreviousState;
    }

    public int getState() {
        return mState;
    }

    @Override
    public boolean removeOnInteractionListener(OnInteractionListener onInteractionListener) {
        boolean removed = true;
        for(DisplayView<ChatHeadView> chatHeadWindowManagerView : mChatHeadDisplayViews) {
            removed = removed && chatHeadWindowManagerView.removeOnInteractionListener(onInteractionListener);
        }
        return removed;
    }

    @Override
    public void addOnInteractionListener(OnInteractionListener onInteractionListener) {
        for(DisplayView<ChatHeadView> chatHeadWindowManagerView : mChatHeadDisplayViews) {
            chatHeadWindowManagerView.addOnInteractionListener(onInteractionListener);
        }
    }

    public void addOnStateChangedListener(OnStateChangedListener onStateChangedListener) {
        mOnStateChangedListeners.add(onStateChangedListener);
    }

    public boolean removeOnStateChangedListener(OnStateChangedListener onStateChangedListener) {
        return mOnStateChangedListeners.remove(onStateChangedListener);
    }

    public OnChatHeadSelectedListener getOnChatSelectedListener() {
        return mOnChatHeadSelectedListener;
    }

    public void setOnChatSelectedListener(OnChatHeadSelectedListener mOnChatHeadSelectedListener) {
        this.mOnChatHeadSelectedListener = mOnChatHeadSelectedListener;
    }

    public int getExpandedHeight() {
        return getMainChatHeadDisplayView().getView().getSelectModeHeight() + mExpandedTopMargin;
    }

    protected ChatHeadDisplayView getMainChatHeadDisplayView() {
        return mChatHeadDisplayViews.get(MAIN_CHAT_HEAD_INDEX);
    }

    public List<Chat> getChats() {
        return mChats;
    }
}
