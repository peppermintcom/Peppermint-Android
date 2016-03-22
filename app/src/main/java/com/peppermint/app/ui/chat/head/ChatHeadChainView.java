package com.peppermint.app.ui.chat.head;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import com.facebook.rebound.BaseSpringSystem;
import com.facebook.rebound.SimpleSpringListener;
import com.facebook.rebound.Spring;
import com.facebook.rebound.SpringConfig;
import com.facebook.rebound.SpringSystem;
import com.facebook.rebound.SpringSystemListener;
import com.peppermint.app.R;
import com.peppermint.app.data.Chat;
import com.peppermint.app.utils.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Nuno Luz on 19-03-2016.
 */
public class ChatHeadChainView extends WindowManagerViewGroup {

    public interface OnStateChangedListener {
        void onSnapped(int viewIndex);
        void onShrinkStarted();
        void onExpandStarted();
        void onExpandFinished();
        void onChatSelected(Chat chat);
    }

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
                for (OnStateChangedListener listener : mOnStateChangedListeners) {
                    listener.onSnapped(_i);
                }
                if(_i == 0) {
                    saveChatHeadPosition();
                }
            } else if(mState == STATE_EXPAND && _i == 0) {
                int viewSize = mViews.size();
                for(int i=0; i<viewSize; i++) {
                    ChatHeadView v = (ChatHeadView) mViews.get(i);
                    v.setNameOnTop(true);
                    v.setNameVisible(true);
                    v.invalidate();
                }
                for(OnStateChangedListener listener : mOnStateChangedListeners) {
                    listener.onExpandFinished();
                }
            }
        }
    }

    private static final String TAG = ChatHeadChainView.class.getSimpleName();

    private static final String CHAT_HEAD_POSITION_X_KEY = TAG + "_chatHeadPositionX";
    private static final String CHAT_HEAD_POSITION_Y_KEY = TAG + "_chatHeadPositionY";

    protected static final int CHATHEAD_SIZE_DP = 48;
    protected static final int CHATHEAD_TEXT_HEIGHT_DP = 30;
    protected static final float INITIAL_POSITION_PERCENT_Y = 0.2f;
    protected static final int EXPANDED_TOP_MARGIN_DP = 10;
    protected static final int EXPANDED_CHATHEAD_SPACING_DP = 10;

    protected static final int MAX_CHAT_HEADS = 4;

    // possible states of the main chat head
    public static final int STATE_DRAGGING = 1;
    public static final int STATE_SNAP_TRIGGER = 2;        // a state that monitors the vel. and pos. of the chat head and triggers the snap
    public static final int STATE_SNAP = 3;                // snaps the chat head
    public static final int STATE_EXPAND = 4;

    private int mState, mPreviousState;
    private List<OnStateChangedListener> mOnStateChangedListeners = new ArrayList<>();

    private List<Chat> mChats = new ArrayList<>();

    // measurements
    private final int mChatHeadWidth, mChatHeadHeight;
    private int mScreenWidth, mScreenHeight;
    private final int mExpandedTopMargin, mExpandedChatHeadSpacing;

    // rebound
    private SpringSystem mSpringSystem;
    private SpringConfig mSpringConfigDrag, mSpringConfigSnap;
    private CustomSpringChain mPositionSpringX, mPositionSpringY;

    private OnInteractionListener mOnInteractionListener = new OnInteractionListener() {
        @Override
        public boolean onClick(int viewIndex, View view) {
            Log.d(TAG, "onClick " + viewIndex);
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
            setReboundXY(MAX_CHAT_HEADS - 1, event.getRawX() - (mChatHeadWidth / 2), event.getRawY() - (mChatHeadHeight / 2), true);
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
                        // in this case, activate the spring that snaps the chat head to the closest edge
                        setState(STATE_SNAP);
                        setReboundXY(x < (mScreenWidth / 2f) ? 0 : mScreenWidth,
                                stickToTopEdge || stickToBottomEdge ? (stickToTopEdge ? 0 : mScreenHeight) : y,
                                false);
                    }

                    break;
            }

            for(Spring spring : mPositionSpringX.getAllSprings()) {
                if(Math.abs(spring.getCurrentValue() - spring.getEndValue()) < 2) {
                    spring.setAtRest();
                }
            }

            for(Spring spring : mPositionSpringY.getAllSprings()) {
                if(Math.abs(spring.getCurrentValue() - spring.getEndValue()) < 2) {
                    spring.setAtRest();
                }
            }
        }
    };

    protected void setSelected(ChatHeadView v) {
        for(View view : mViews) {
            ChatHeadView otherView = (ChatHeadView) view;
            if(v.equals(otherView)) {
                otherView.setSelected(true);
            } else {
                otherView.setSelected(false);
            }
            otherView.invalidate();
        }

        for(OnStateChangedListener listener : mOnStateChangedListeners) {
            listener.onChatSelected(v.getChat());
        }
    }

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

        Log.d("TAG", "Switching State OLD=" + mPreviousState + " NEW=" + mState);

        // deinit old state
        switch(mPreviousState) {
            case STATE_EXPAND:
                // shrink
                enableReboundChain();

                int viewSize = mViews.size();
                for(int i=0; i<viewSize; i++) {
                    ChatHeadView v = (ChatHeadView) mViews.get(i);
                    v.setNameOnTop(false);
                    if(i == MAX_CHAT_HEADS - 1) {
                        v.setNameVisible(true);
                    } else {
                        v.setNameVisible(false);
                    }
                    v.invalidate();
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
                    setSpringConfig(viewSize - 1 - i, mSpringConfigSnap);
                    setReboundXY(viewSize - 1 - i, mScreenWidth - ((mChatHeadWidth * i) + (mExpandedChatHeadSpacing * (i + 1))), mExpandedTopMargin, false);
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

        mPositionSpringX = CustomSpringChain.create(mSpringSystem);
        mPositionSpringY = CustomSpringChain.create(mSpringSystem);

        this.mChatHeadWidth = Utils.dpToPx(mContext, CHATHEAD_SIZE_DP);
        this.mChatHeadHeight = mChatHeadWidth + Utils.dpToPx(mContext, CHATHEAD_TEXT_HEIGHT_DP) + Utils.dpToPx(mContext, ChatHeadView.DEF_SEL_LENGTH_DP + 3);
        this.mExpandedTopMargin = Utils.dpToPx(mContext, EXPANDED_TOP_MARGIN_DP);
        this.mExpandedChatHeadSpacing = Utils.dpToPx(mContext, EXPANDED_CHATHEAD_SPACING_DP);

        // chat heads
        for(int i=0; i < MAX_CHAT_HEADS; i++) {
            ChatHeadView v = new ChatHeadView(mContext);
            v.setButtonImageResource(R.drawable.ic_play_15dp);
            v.setVisibility(View.GONE);
            if(i == (MAX_CHAT_HEADS - 1)) {
                v.setNameVisible(true);
                v.setSelected(true);
            }

            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(
                    mChatHeadWidth,
                    mChatHeadHeight,
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

        boolean goLeft = true;
        if(startPos[0] > 0) {
            goLeft = startPos[0] < 0.5f ? true : false;
        }

        float x = goLeft ? 0 : mScreenWidth;
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
        this.mScreenWidth = point.x - mChatHeadWidth;
        this.mScreenHeight = point.y - mChatHeadHeight;
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

    public void invalidateChats() {
        Chat selectedChat = getSelectedChat();
        boolean gotSelected = false;
        int j=0;
        int startsAt = MAX_CHAT_HEADS - mChats.size();
        for(int i=0; i<MAX_CHAT_HEADS; i++) {
            ChatHeadView v = (ChatHeadView) mViews.get(i);
            if(i < startsAt) {
                v.setVisibility(View.GONE);
            } else {
                v.setChat(mChats.get(j));
                v.setVisibility(View.VISIBLE);

                if(selectedChat != null && selectedChat.getId() == mChats.get(j).getId()) {
                    v.setSelected(true);
                    gotSelected = true;
                } else {
                    v.setSelected(false);
                }

                j++;
            }
        }

        if(!gotSelected) {
            ((ChatHeadView) mViews.get(MAX_CHAT_HEADS - 1)).setSelected(true);
        }
    }

    public float getReboundX() {
        return (float) mPositionSpringX.getControlSpring().getCurrentValue();
    }

    public float getReboundY() {
        return (float) mPositionSpringY.getControlSpring().getCurrentValue();
    }

    public void setReboundX(float x, boolean atRest) {
        setReboundXY(x, getReboundY(), atRest);
    }

    public void setReboundY(float y, boolean atRest) {
        setReboundXY(getReboundX(), y, atRest);
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

    public int getChatHeadWidth() {
        return mChatHeadWidth;
    }

    public int getChatHeadHeight() {
        return mChatHeadHeight;
    }
}
