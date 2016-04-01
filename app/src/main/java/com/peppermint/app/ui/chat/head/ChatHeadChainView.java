package com.peppermint.app.ui.chat.head;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.preference.PreferenceManager;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import com.facebook.rebound.BaseSpringSystem;
import com.facebook.rebound.SimpleSpringListener;
import com.facebook.rebound.Spring;
import com.facebook.rebound.SpringConfig;
import com.facebook.rebound.SpringSystem;
import com.facebook.rebound.SpringSystemListener;
import com.peppermint.app.data.Chat;
import com.peppermint.app.utils.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Nuno Luz on 19-03-2016.
 *
 * Represents a chain of chat heads that can be moved and controlled by the user.
 *
 */
public class ChatHeadChainView extends WindowManagerViewGroup {

    /**
     * Chat head chain state change listener.
     */
    public interface OnStateChangedListener {
        void onSnapped(int viewIndex);
        void onShrinkStarted();
        void onExpandStarted();
        void onExpandFinished();
        void onChatSelected(Chat chat);
    }

    // listens for updates from the X-axis chat head spring
    private class ReboundSpringXListener extends SimpleSpringListener {
        protected int _i;
        public ReboundSpringXListener(int index) {
            this._i = index;
        }
        @Override
        public void onSpringUpdate(Spring spring) {
            setViewPositionX(_i, (int) spring.getCurrentValue());
        }
    }

    // listens for updates from the Y-axis chat head spring
    private class ReboundSpringYListener extends SimpleSpringListener {
        protected int _i;
        public ReboundSpringYListener(int index) {
            this._i = index;
        }
        @Override
        public void onSpringUpdate(Spring spring) {
            setViewPositionY(_i, (int) spring.getCurrentValue());
        }
        @Override
        public void onSpringAtRest(Spring spring) {
            if(mState == STATE_SNAP) {
                // invoke state listener after snap finishes
                for (OnStateChangedListener listener : mOnStateChangedListeners) {
                    listener.onSnapped(_i);
                }
                if(_i == 0) {
                    saveChatHeadPosition();
                }
            } else if(mState == STATE_EXPAND && _i == 0) {
                // invoke state listener after expand finishes
                for(OnStateChangedListener listener : mOnStateChangedListeners) {
                    listener.onExpandFinished();
                }
            }
        }
    }

    private static final String TAG = ChatHeadChainView.class.getSimpleName();

    private static final String CHAT_HEAD_POSITION_X_KEY = TAG + "_chatHeadPositionX";
    private static final String CHAT_HEAD_POSITION_Y_KEY = TAG + "_chatHeadPositionY";

    protected static final float INITIAL_POSITION_PERCENT_Y = 0.2f;
    protected static final int EXPANDED_TOP_MARGIN_DP = 10;
    protected static final int EXPANDED_CHATHEAD_SPACING_DP = 10;

    protected static final int MAX_CHAT_HEADS = 4;

    // possible states of the main chat head
    public static final int STATE_DRAGGING = 1;             // while the user drags the chat head chain
    public static final int STATE_SNAP_TRIGGER = 2;         // a state that monitors the vel. and pos. of the chat head chain and triggers the snap
    public static final int STATE_SNAP = 3;                 // snaps the chat head to a particular position
    public static final int STATE_EXPAND = 4;               // while the chat head chain is expanded/expanding

    private int mState, mPreviousState;
    private List<OnStateChangedListener> mOnStateChangedListeners = new ArrayList<>();

    private List<Chat> mChats = new ArrayList<>();          // list of chats mapped to chat head views

    // measurements
    private int mScreenWidth, mScreenHeight;
    private final int mExpandedTopMargin, mExpandedChatHeadSpacing;

    // rebound
    private SpringSystem mSpringSystem;
    private SpringConfig mSpringConfigDrag, mSpringConfigSnap;
    private CustomSpringChain mPositionSpringX, mPositionSpringY;

    private OnInteractionListener mOnInteractionListener = new OnInteractionListener() {
        @Override
        public boolean onClick(int viewIndex, View view) {
            if(mState == STATE_EXPAND) {
                ChatHeadView v = (ChatHeadView) view;
                if(v.isSelected()) {
                    snapToSavedPosition(true);
                } else {
                    setSelected(v);
                }
            } else {
                setState(STATE_EXPAND);
            }
            return true;
        }

        @Override
        public boolean onDragStarted(int viewIndex, View view, float offsetX, float offsetY, MotionEvent event) {
            ChatHeadView v = (ChatHeadView) mViews.get(MAX_CHAT_HEADS - 1);
            setReboundXY(MAX_CHAT_HEADS - 1, event.getRawX() - (v.getWidth() / 2), event.getRawY() - (v.getHeight() / 2), true);
            return onDrag(viewIndex, view, offsetX, offsetY, event);
        }

        @Override
        public boolean onDrag(int viewIndex, View view, float offsetX, float offsetY, MotionEvent event) {
            setState(STATE_DRAGGING);
            setReboundXY(getReboundX() - offsetX, getReboundY() - offsetY, true);
            return true;
        }

        @Override
        public boolean onDragFinished(int viewIndex, View view, float[] velocity, MotionEvent event) {
            setState(STATE_SNAP_TRIGGER);
            mPositionSpringX.getControlSpring().setVelocity(velocity[0]);
            mPositionSpringY.getControlSpring().setVelocity(velocity[1]);
            return true;
        }
    };

    private SpringSystemListener mSpringSystemListener = new SpringSystemListener() {
        @Override
        public void onBeforeIntegrate(BaseSpringSystem springSystem) { /* nothing to do */ }

        @Override
        public void onAfterIntegrate(BaseSpringSystem springSystem) {
            switch(mState) {
                case STATE_SNAP_TRIGGER:
                    // keep checking the vel. and position of the chat head chain
                    // if its vel. is below a certain threshold, or it has touched a screen edge
                    // snap it

                    int controlIndex = MAX_CHAT_HEADS - 1;
                    int x = getViewPositionX(controlIndex);
                    int y = getViewPositionY(controlIndex);

                    boolean stickToRightEdge = x >= mScreenWidth;
                    boolean stickToLeftEdge = x <= 0;
                    boolean stickToTopEdge = y <= 0;
                    boolean stickToBottomEdge = y >= mScreenHeight;

                    // calc. current velocity
                    float normalVel = (float) Math.sqrt((mPositionSpringX.getControlSpring().getVelocity() * mPositionSpringX.getControlSpring().getVelocity()) +
                            (mPositionSpringY.getControlSpring().getVelocity() * mPositionSpringY.getControlSpring().getVelocity()));

                    if ((stickToLeftEdge || stickToRightEdge || stickToBottomEdge || stickToTopEdge || normalVel < 300)) {
                        // either an edge has been reached or the spring velocity is too slow
                        // in this case, activate the spring that snaps the chat head chain to the closest edge
                        setState(STATE_SNAP);
                        setReboundXY(x < (mScreenWidth / 2f) ? 0 : mScreenWidth,
                                stickToTopEdge || stickToBottomEdge ? (stickToTopEdge ? 0 : mScreenHeight) : y,
                                false);
                    }

                    break;
            }

            // do not use Spring.setCurrentValue! it will re-activate the Spring and enter into an infinite loop!
            int size = mViews.size();
            for(int i=0; i<size; i++) {
                Spring springX = mPositionSpringX.getAllSprings().get(i);
                Spring springY = mPositionSpringY.getAllSprings().get(i);

                if(Math.abs(springX.getCurrentValue() - springX.getEndValue()) < 2 &&
                        Math.abs(springY.getCurrentValue() - springY.getEndValue()) < 2) {
                    int x = (int) springX.getEndValue();
                    int y = (int) springY.getEndValue();
                    springX.setAtRest();
                    springY.setAtRest();
                    setViewPosition(i, x, y);
                }
            }
        }
    };

    protected void setSelected(ChatHeadView v) {
        if(v.isSelected()) {
            return;
        }

        for(View view : mViews) {
            ChatHeadView otherView = (ChatHeadView) view;
            if(v.equals(otherView)) {
                if(!otherView.isSelected()) {
                    otherView.setSelected(true);
                    otherView.requestLayout();
                    otherView.invalidate();
                }
            } else {
                if(otherView.isSelected()) {
                    otherView.setSelected(false);
                    otherView.requestLayout();
                    otherView.invalidate();
                }
            }
        }

        for(OnStateChangedListener listener : mOnStateChangedListeners) {
            listener.onChatSelected(v.getChat());
        }
    }

    /**
     * Gets the selected chat.
     *
     * @return the selected chat
     */
    public Chat getSelectedChat() {
        for(View view : mViews) {
            ChatHeadView v = (ChatHeadView) view;
            if(v.isSelected()) {
                return v.getChat();
            }
        }
        return null;
    }

    protected void setState(int newState) {
        if(newState == mState) {
            return;
        }

        mPreviousState = mState;
        mState = newState;

        // de-init old state
        switch(mPreviousState) {
            case STATE_EXPAND:
                // shrink
                enableReboundChain();

                ChatHeadView selectedView = null;
                int viewSize = mViews.size();
                for(int i=0; i<viewSize; i++) {
                    ChatHeadView v = (ChatHeadView) mViews.get(i);
                    v.setSelectMode(false);
                    if(v.isSelected()) {
                        selectedView = v;
                    }
                }

                if(selectedView != null) {
                    addChat(selectedView.getChat());
                }

                for(OnStateChangedListener listener : mOnStateChangedListeners) {
                    listener.onShrinkStarted();
                }
                break;
        }

        // init new state
        switch (mState) {
            case STATE_DRAGGING:
                setSpringConfig(mSpringConfigDrag);
                break;
            case STATE_SNAP:
                setSpringConfig(mSpringConfigSnap);
                break;
            case STATE_EXPAND:
                saveChatHeadPosition();
                disableReboundChain();

                int viewSize = mViews.size();
                for(int i=0; i<viewSize; i++) {
                    int viewWidth = mViews.get(viewSize - 1 - i).getWidth();
                    setSpringConfig(viewSize - 1 - i, mSpringConfigSnap);
                    setReboundXY(viewSize - 1 - i, mScreenWidth - ((viewWidth * i) + (mExpandedChatHeadSpacing * (i + 1))), mExpandedTopMargin, false);

                    ChatHeadView v = (ChatHeadView) mViews.get(i);
                    v.setSelectMode(true);
                    v.requestLayout();
                    v.invalidate();
                }

                for(OnStateChangedListener listener : mOnStateChangedListeners) {
                    listener.onExpandStarted();
                }
                break;
        }
    }

    public ChatHeadChainView(Context mContext) {
        super(mContext);

        addOnInteractionListener(mOnInteractionListener);

        mSpringSystem = SpringSystem.create();
        mSpringSystem.addListener(mSpringSystemListener);

        mSpringConfigDrag = SpringConfig.fromOrigamiTensionAndFriction(0, 0.5);
        mSpringConfigSnap = SpringConfig.fromOrigamiTensionAndFriction(30, 5);

        // springs that control the position of the chat head chain
        mPositionSpringX = CustomSpringChain.create(mSpringSystem);
        mPositionSpringY = CustomSpringChain.create(mSpringSystem);

        this.mExpandedTopMargin = Utils.dpToPx(mContext, EXPANDED_TOP_MARGIN_DP);
        this.mExpandedChatHeadSpacing = Utils.dpToPx(mContext, EXPANDED_CHATHEAD_SPACING_DP);

        // chat head views
        for(int i=0; i < MAX_CHAT_HEADS; i++) {
            ChatHeadView v = new ChatHeadView(mContext);
            v.setVisibility(View.GONE);

            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_PHONE,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED |
                            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    PixelFormat.TRANSLUCENT);
            layoutParams.gravity = Gravity.TOP | Gravity.LEFT;

            addView(v, layoutParams);

            mPositionSpringX.addSpring(new ReboundSpringXListener(i));
            mPositionSpringY.addSpring(new ReboundSpringYListener(i));
        }

        mPositionSpringX.setControlSpringIndex(MAX_CHAT_HEADS - 1);
        mPositionSpringY.setControlSpringIndex(MAX_CHAT_HEADS - 1);
    }

    protected void snapToSavedPosition(boolean animate) {
        setState(STATE_SNAP);

        float[] startPos = loadChatHeadPosition();

        float x = startPos[0] < 0.5f ? 0 : mScreenWidth;
        float y = startPos[1] * (float) mScreenHeight;

        // setup position of chat head
        setReboundXY(x, y, !animate);
    }

    public void snapTo(int x, int y) {
        setState(STATE_SNAP);
        setReboundXY(x, y, false);
    }

    public void requestLayout() {
        Point point = Utils.getScreenSize(mContext);
        this.mScreenWidth = point.x - Utils.dpToPx(mContext, 54);   //FIXME
        this.mScreenHeight = point.y - Utils.dpToPx(mContext, 54) - Utils.getNavigationBarHeight(mContext);
        snapToSavedPosition(false);
    }

    public void enableReboundChain() {
        mPositionSpringX.enable();
        mPositionSpringY.enable();
    }

    public void disableReboundChain() {
        mPositionSpringX.disable();
        mPositionSpringY.disable();
    }

    private int getIndexByChatId(long id) {
        int size = mViews.size();
        for(int i=0; i<size; i++) {
            ChatHeadView v = (ChatHeadView) mViews.get(i);
            if(v.getChat() != null && v.getChat().getId() == id) {
                return i;
            }
        }
        return -1;
    }

    public void addChat(Chat chat) {
        int foundChatHeadIndex = getIndexByChatId(chat.getId());
        if(foundChatHeadIndex >= 0) {
            mChats.remove(chat);
        }
        mChats.add(chat);

        // enforce the max. chat head amount
        if(mChats.size() > MAX_CHAT_HEADS) {
            mChats.remove(0);
        }

        invalidateChats();
    }

    /**
     * Re-maps all chats to their {@link ChatHeadView}s.
     */
    public void invalidateChats() {
        Chat selectedChat = getSelectedChat();
        boolean gotSelected = false;
        int j=0;
        int startsAt = MAX_CHAT_HEADS - mChats.size();
        for(int i=0; i<MAX_CHAT_HEADS; i++) {
            ChatHeadView v = (ChatHeadView) mViews.get(i);
            if(i < startsAt) {
                v.setChat(null);
                v.setVisibility(View.GONE);
            } else {
                v.setVisibility(View.VISIBLE);

                if(selectedChat != null && selectedChat.getId() == mChats.get(j).getId()) {
                    v.setSelected(true);
                    gotSelected = true;
                } else {
                    v.setSelected(false);
                }

                v.setChat(mChats.get(j));

                j++;
            }
        }

        if(!gotSelected) {
            mViews.get(MAX_CHAT_HEADS - 1).setSelected(true);
        }

        // re-layout and re-draw all chat head views
        for(View v : mViews) {
            v.requestLayout();
            v.invalidate();
        }
    }

    public float getReboundX() {
        return (float) mPositionSpringX.getControlSpring().getCurrentValue();
    }

    public float getReboundY() {
        return (float) mPositionSpringY.getControlSpring().getCurrentValue();
    }

    public void setReboundXY(float x, float y, boolean atRest) {
        setReboundXY(-1, x, y, atRest);
    }

    public void setReboundXY(int i, float x, float y, boolean atRest) {
        if(atRest) {
            if(i < 0) {
                for (Spring spring : mPositionSpringX.getAllSprings()) {
                    spring.setCurrentValue(x).setAtRest();
                }
                for (Spring spring : mPositionSpringY.getAllSprings()) {
                    spring.setCurrentValue(y).setAtRest();
                }
            } else {
                mPositionSpringX.getAllSprings().get(i).setCurrentValue(x).setAtRest();
                mPositionSpringY.getAllSprings().get(i).setCurrentValue(y).setAtRest();
            }
        } else {
            if(i < 0) {
                mPositionSpringX.getControlSpring().setEndValue(x);
                mPositionSpringY.getControlSpring().setEndValue(y);
            } else {
                mPositionSpringX.getAllSprings().get(i).setEndValue(x);
                mPositionSpringY.getAllSprings().get(i).setEndValue(y);
            }
        }
    }

    public void setSpringConfig(SpringConfig config) {
        setSpringConfig(-1, config);
    }

    public void setSpringConfig(int i, SpringConfig config) {
        if(i < 0) {
            mPositionSpringX.getControlSpring().setSpringConfig(config);
            mPositionSpringY.getControlSpring().setSpringConfig(config);
        } else {
            mPositionSpringX.getAllSprings().get(i).setSpringConfig(config);
            mPositionSpringY.getAllSprings().get(i).setSpringConfig(config);
        }
    }

    public void saveChatHeadPosition() {
        // do not allow 0 values since it will just reset the position
        // 0 only in case there's no data
        float x = getViewPositionX(MAX_CHAT_HEADS - 1) / (float) mScreenWidth;
        float y = getViewPositionY(MAX_CHAT_HEADS - 1) / (float) mScreenHeight;

        if(x < 0) { x = 0; }
        if(y < 0) { y = 0; }
        if(x > 1) { x = 1; }
        if(y > 1) { y = 1; }

        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(mContext).edit();
        editor.putFloat(CHAT_HEAD_POSITION_X_KEY, x);
        editor.putFloat(CHAT_HEAD_POSITION_Y_KEY, y);
        editor.commit();
    }

    public float[] loadChatHeadPosition() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        float x = sharedPreferences.getFloat(CHAT_HEAD_POSITION_X_KEY, 0);
        float y = sharedPreferences.getFloat(CHAT_HEAD_POSITION_Y_KEY, INITIAL_POSITION_PERCENT_Y);
        return new float[]{x, y};
    }

    public void addOnStateChangedListener(OnStateChangedListener onStateChangedListener) {
        mOnStateChangedListeners.add(onStateChangedListener);
    }

    public boolean removeOnStateChangedListener(OnStateChangedListener onStateChangedListener) {
        return mOnStateChangedListeners.remove(onStateChangedListener);
    }

    public int getState() {
        return mState;
    }

    @Override
    public boolean show() {
        int size = mViews.size();
        for(int i=0; i<size; i++) {
            if(i >= (MAX_CHAT_HEADS - 1)) {
                mViews.get(i).setSelected(true);
            } else {
                mViews.get(i).setSelected(false);
            }
        }
        return super.show();
    }

    public void setShowBadge(boolean val) {
        for(View view : mViews) {
            ChatHeadView v = (ChatHeadView) view;
            v.setShowBadge(val);
            v.invalidate();
        }
    }

    public int getWidth() {
        if(mViews.size() <= 0) {
            return 0;
        }
        return mViews.get(0).getMeasuredWidth();
    }

    public int getHeight() {
        if(mViews.size() <= 0) {
            return 0;
        }
        return mViews.get(0).getMeasuredHeight();
    }

    public int getSelectModeHeight() {
        if(mViews.size() <= 0) {
            return 0;
        }
        return ((ChatHeadView) mViews.get(0)).getSelectModeHeight();
    }

    @Override
    public void setViewPosition(int i, int x, int y) {
        for(View view : mViews) {
            ChatHeadView v = (ChatHeadView) view;
            if(x < (mScreenWidth / 2)) {
                if(v.getBadgeOrientation() != ChatHeadView.BADGE_ORIENTATION_TOP_RIGHT) {
                    v.setBadgeOrientation(ChatHeadView.BADGE_ORIENTATION_TOP_RIGHT);
                    v.invalidate();
                }
            } else {
                if(v.getBadgeOrientation() != ChatHeadView.BADGE_ORIENTATION_TOP_LEFT) {
                    v.setBadgeOrientation(ChatHeadView.BADGE_ORIENTATION_TOP_LEFT);
                    v.invalidate();
                }
            }
        }

        super.setViewPosition(i, x, y);
    }
}
