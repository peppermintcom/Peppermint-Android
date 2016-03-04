package com.peppermint.app.ui.chat.head;

import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

import com.facebook.rebound.BaseSpringSystem;
import com.facebook.rebound.SimpleSpringListener;
import com.facebook.rebound.Spring;
import com.facebook.rebound.SpringConfig;
import com.facebook.rebound.SpringListener;
import com.facebook.rebound.SpringSystem;
import com.facebook.rebound.SpringSystemListener;
import com.peppermint.app.R;
import com.peppermint.app.cloud.senders.SenderPreferences;
import com.peppermint.app.data.Chat;
import com.peppermint.app.ui.chat.ChatFragment;
import com.peppermint.app.utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Nuno Luz on 02-03-2016.
 *
 * Controls multiple {@link ChatHeadView} in the {@link WindowManager}.<br />
 *
 */
public class ChatHeadController {

    // possible states of the main chat head
    private static final int STATE_HEAD_DRAGGING = 1;
    private static final int STATE_HEAD_SNAP_EDGE = 2;
    private static final int STATE_HEAD_SNAP_REMOVE = 3;
    private static final int STATE_HEAD_SNAP_DISMISS = 4;

    private static final int REMOVE_BOTTOM_SPACING_DP = 26;

    private Context mContext;
    private SenderPreferences mPreferences;
    private WindowManager mWindowManager;
    private GestureDetector mChatHeadTapDetector;

    private int mState = STATE_HEAD_SNAP_EDGE;

    // ui
    private ImageView mRemove;
    private WindowManager.LayoutParams mRemoveLayoutParams;
    private List<ChatHeadView> mChatHeadList = new ArrayList<>();
    private List<WindowManager.LayoutParams> mChatHeadLayoutParamsList = new ArrayList<>();
    private Map<Long, ChatHeadView> mChatHeadMap = new HashMap<>();

    // util
    private final int mChatHeadLength, mChatHeadHalfLength;
    private final int mScreenWidth, mScreenHeight;

    // positioning
    private float mTopPercent = 0.1f;
    private boolean mLeftSide = true;
    private float mChatHeadX, mChatHeadY;

    // spring system
    private SpringSystem mSpringSystem;
    private SpringConfig mSpringConfigDrag, mSpringConfigSnap, mSpringConfigSize;
    private CustomSpringChain mChatHeadMovementSpringX, mChatHeadMovementSpringY;
    private Spring mRemoveSizeSpring;

    public ChatHeadController(Context context) {
        this.mContext = context;
        this.mPreferences = new SenderPreferences(mContext);
        this.mWindowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);

        this.mChatHeadLength = Utils.dpToPx(mContext, 48);
        this.mChatHeadHalfLength = mChatHeadLength / 2;
        Point point = Utils.getScreenSize(mContext);
        this.mScreenWidth = point.x;
        this.mScreenHeight = point.y - Utils.getStatusBarHeight(mContext) - mChatHeadLength;

        // remove button view
        mRemove = new ImageView(mContext);
        mRemove.setImageResource(R.drawable.ic_remove_48dp);

        mRemoveLayoutParams = new WindowManager.LayoutParams(
                mChatHeadLength,
                mChatHeadLength,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                PixelFormat.TRANSLUCENT);
        mRemoveLayoutParams.gravity = Gravity.TOP | Gravity.LEFT;

        mSpringConfigDrag = SpringConfig.fromOrigamiTensionAndFriction(0, 0.5);
        mSpringConfigSnap = SpringConfig.fromOrigamiTensionAndFriction(30, 5);
        mSpringConfigSize = SpringConfig.fromOrigamiTensionAndFriction(20, 2);

        // spring system
        mSpringSystem = SpringSystem.create();
        mSpringSystem.addListener(mSpringSystemListener);

        mChatHeadMovementSpringX = CustomSpringChain.create(mSpringSystem);
        mChatHeadMovementSpringY = CustomSpringChain.create(mSpringSystem);

        mRemoveSizeSpring = mSpringSystem.createSpring();
        mRemoveSizeSpring.setSpringConfig(mSpringConfigSize);
        mRemoveSizeSpring.addListener(mRemoveSizeSpringListener);

        mChatHeadTapDetector = new GestureDetector(mContext, new GestureDetector.OnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) { return false; }

            @Override
            public void onShowPress(MotionEvent e) { /* nothing to do here */ }

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                mState = STATE_HEAD_SNAP_DISMISS;

                mChatHeadMovementSpringX.getControlSpring().setSpringConfig(mSpringConfigSnap);
                mChatHeadMovementSpringY.getControlSpring().setSpringConfig(mSpringConfigSnap);
                mChatHeadMovementSpringX.getControlSpring().setEndValue(mScreenWidth - (mChatHeadLength * 2) + mChatHeadHalfLength);
                mChatHeadMovementSpringY.getControlSpring().setEndValue(Utils.dpToPx(mContext, 10));

                Chat chat = mChatHeadList.get(mChatHeadList.size() - 1).getChat();
                Intent intent = new Intent(mContext, ChatHeadActivity.class);
                intent.putExtra(ChatFragment.PARAM_CHAT_ID, chat.getId());
                if(chat.getAmountUnopened() > 0) {
                    // play last message
                    intent.putExtra(ChatFragment.PARAM_AUTO_PLAY_MESSAGE_ID, -1l);
                }
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                mContext.startActivity(intent);

                return true;
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                return false;
            }

            @Override
            public void onLongPress(MotionEvent e) { /* nothing to do here */ }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                return false;
            }
        });
    }

    public ChatHeadController(Context context, float topPercent, boolean leftSide) {
        this(context);
        this.mTopPercent = topPercent;
        this.mLeftSide = leftSide;
    }

    public void addChat(Chat chat) {
        // chat head view
        final ChatHeadView chatHead = new ChatHeadView(mContext);
        chatHead.setChat(chat);

        final WindowManager.LayoutParams chatHeadLayoutParams = new WindowManager.LayoutParams(
                mChatHeadLength,
                mChatHeadLength + 100,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        chatHeadLayoutParams.gravity = Gravity.TOP | Gravity.LEFT;

        if(mChatHeadList.size() <= 0) {
            chatHead.setOnTouchListener(mChatHeadTouchListener);
        }

        mChatHeadList.add(0, chatHead);
        mChatHeadLayoutParamsList.add(0, chatHeadLayoutParams);
        mChatHeadMap.put(chat.getId(), chatHead);

        mChatHeadMovementSpringX.addSpring(new SimpleSpringListener() {
            @Override
            public void onSpringUpdate(Spring spring) {
                float x = (float) spring.getCurrentValue();
                if (spring.equals(mChatHeadMovementSpringX.getControlSpring())) {
                    mChatHeadX = x;
                }
                chatHeadLayoutParams.x = (int) (x - mChatHeadHalfLength);
                if (chatHead.getWindowToken() != null) {
                    mWindowManager.updateViewLayout(chatHead, chatHeadLayoutParams);
                }
            }
        });
        mChatHeadMovementSpringY.addSpring(new SimpleSpringListener() {
            @Override
            public void onSpringUpdate(Spring spring) {
                float y = (float) spring.getCurrentValue();
                if (spring.equals(mChatHeadMovementSpringY.getControlSpring())) {
                    mChatHeadY = y;
                }
                chatHeadLayoutParams.y = (int) (y - mChatHeadHalfLength);
                if (chatHead.getWindowToken() != null) {
                    mWindowManager.updateViewLayout(chatHead, chatHeadLayoutParams);
                }
            }

            @Override
            public void onSpringAtRest(Spring spring) {
                if(mState == STATE_HEAD_SNAP_DISMISS) {
                    disable();
                }
            }
        });

        mChatHeadMovementSpringX.setControlSpringIndex(0);
        mChatHeadMovementSpringY.setControlSpringIndex(0);
    }

    public void removeChat(Chat chat) {

    }

    public Chat popChat() {
        return null;
    }

    public void requestLayout() {
        Log.d("TAG", "REQUEST LAYOUT");

        if(mChatHeadList.size() <= 0) {
            return;
        }

        float[] startPos = mPreferences.getChatHeadPosition();

        if(startPos[0] > 0) {
            mLeftSide = startPos[0] < 0.5f ? true : false;
        }

        float x = mLeftSide ? mChatHeadHalfLength : mScreenWidth - mChatHeadHalfLength;
        float y = startPos[1] > 0 ? startPos[1] * mScreenHeight : mTopPercent * (float) mScreenHeight;

        // setup position of chat head
        for(Spring spring : mChatHeadMovementSpringX.getAllSprings()) {
            spring.setCurrentValue(x).setAtRest();
        }
        for(Spring spring : mChatHeadMovementSpringY.getAllSprings()) {
            spring.setCurrentValue(y).setAtRest();
        }

        // setup position and size of remove button
        mRemoveSizeSpring.setCurrentValue(1);
    }

    public void enable() {
        Log.d("TAG", "Enable!");

        mState = STATE_HEAD_SNAP_EDGE;

        int size = mChatHeadList.size();
        for(int i=0; i<size; i++) {
            if(i >= (size-1)) {
                mChatHeadList.get(size-1).setNameVisible(true);
            } else {
                mChatHeadList.get(i).setNameVisible(false);
            }

            if (mChatHeadList.get(i).getWindowToken() != null) {
                mWindowManager.removeViewImmediate(mChatHeadList.get(i));
            }
            mWindowManager.addView(mChatHeadList.get(i), mChatHeadLayoutParamsList.get(i));
        }
    }

    public void disable() {
        Log.d("TAG", "Disable!");

        if(mState != STATE_HEAD_SNAP_DISMISS && mState != STATE_HEAD_SNAP_REMOVE) {
            mPreferences.setChatHeadPosition(mChatHeadX / (float) mScreenWidth, mChatHeadY / (float) mScreenHeight);
        }

        for(ChatHeadView chatHead : mChatHeadList) {
            if(chatHead.getWindowToken() != null) {
                mWindowManager.removeView(chatHead);
            }
        }
    }

    protected void showRemove() {
        if(mRemove.getWindowToken() != null) {
            mWindowManager.removeViewImmediate(mRemove);
        }
        mWindowManager.addView(mRemove, mRemoveLayoutParams);
    }

    protected void hideRemove() {
        mWindowManager.removeView(mRemove);
    }

    private boolean isInsideRemoveInfluence(float x, float y) {
        int dpRadius = mChatHeadLength;

        int areaX1 = mRemoveLayoutParams.x - dpRadius;
        int areaY1 = mRemoveLayoutParams.y - dpRadius;
        int areaX2 = mRemoveLayoutParams.x + dpRadius + mChatHeadLength;
        int areaY2 = mRemoveLayoutParams.y + dpRadius + mChatHeadLength;

        if(x >= areaX1 && x <= areaX2 && y >= areaY1 && y <= areaY2) {
            mRemoveSizeSpring.setEndValue(1.25);
            return true;
        }

        mRemoveSizeSpring.setEndValue(1);
        return false;
    }

    private void checkConstraints() {
        switch(mState) {
            case STATE_HEAD_SNAP_EDGE:
                Log.d("TAG", "SNAP_EDGE");
                boolean stickToRightEdge = mChatHeadX >= mScreenWidth;
                boolean stickToLeftEdge = mChatHeadX <= 0;
                boolean stickToTopEdge = mChatHeadY <= 0;
                boolean stickToBottomEdge = mChatHeadY >= mScreenHeight;

                // calc. current velocity
                float normalVel = (float) Math.sqrt((mChatHeadMovementSpringX.getControlSpring().getVelocity() * mChatHeadMovementSpringX.getControlSpring().getVelocity()) +
                        (mChatHeadMovementSpringY.getControlSpring().getVelocity() * mChatHeadMovementSpringY.getControlSpring().getVelocity()));

                if ((stickToLeftEdge || stickToRightEdge || stickToBottomEdge || stickToTopEdge || normalVel < 300)) {
                    // either an edge has been reached or the spring velocity is too slow
                    // in this case, activate the spring that snaps the chat head to the closest edge
                    mChatHeadMovementSpringX.getControlSpring().setSpringConfig(mSpringConfigSnap);
                    mChatHeadMovementSpringY.getControlSpring().setSpringConfig(mSpringConfigSnap);
                    mChatHeadMovementSpringX.getControlSpring().setEndValue(mChatHeadX < (mScreenWidth / 2f) ? mChatHeadHalfLength : mScreenWidth - mChatHeadHalfLength);
                    mChatHeadMovementSpringY.getControlSpring().setEndValue(stickToTopEdge || stickToBottomEdge ? (stickToTopEdge ? mChatHeadHalfLength : mScreenHeight - mChatHeadHalfLength) : mChatHeadY);
                    mState = 0;
                }

                break;

            case STATE_HEAD_SNAP_REMOVE:
                break;

            case STATE_HEAD_DRAGGING:
                break;
        }
    }

    // velocity calc touch listener
    private View.OnTouchListener mChatHeadTouchListener = new View.OnTouchListener() {
        private float lastX, lastY;

        // custom velocity tracking code
        // Android's VelocityTracker implementation has severe issues,
        // returning inverse velocity vectors in some cases
        private List<Long> lastTimes = new ArrayList<>();
        private List<float[]> lastEvents = new ArrayList<>();

        private void pushMotionEvent(MotionEvent event) {
            // keep track of the 5 last motion events
            if(lastEvents.size() >= 5) {
                lastEvents.remove(0);
                lastTimes.remove(0);
            }
            lastEvents.add(new float[]{event.getRawX(), event.getRawY()});
            lastTimes.add(event.getEventTime());
        }

        private float[] getVelocity() {
            // use the first and last of the stored motion events to calculate the vel. vector
            int lastIndex = lastEvents.size() - 1;
            float durationSec = ((float) (lastTimes.get(lastIndex) - lastTimes.get(0)) / 1000f);
            float velX = (lastEvents.get(lastIndex)[0] - lastEvents.get(0)[0]) / durationSec;
            float velY = (lastEvents.get(lastIndex)[1] - lastEvents.get(0)[1]) / durationSec;
            return new float[]{velX, velY};
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            boolean onClick = mChatHeadTapDetector.onTouchEvent(event);

            float touchX = event.getRawX();
            float touchY = event.getRawY();
            boolean ret = false;

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    pushMotionEvent(event);

                    lastX = touchX;
                    lastY = touchY;
                    mState = STATE_HEAD_DRAGGING;

                    showRemove();

                    mPreferences.setChatHeadPosition(mChatHeadX / (float) mScreenWidth, mChatHeadY / (float) mScreenHeight);

                    ret = true;
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    hideRemove();

                    if(!onClick) {
                        if (isInsideRemoveInfluence(touchX, touchY)) {
                            disable();
                        } else {
                            mState = STATE_HEAD_SNAP_EDGE;

                            mChatHeadMovementSpringX.getControlSpring().setSpringConfig(mSpringConfigDrag);
                            mChatHeadMovementSpringY.getControlSpring().setSpringConfig(mSpringConfigDrag);

                            float[] vel = getVelocity();
                            mChatHeadMovementSpringX.getControlSpring().setVelocity(vel[0]);
                            mChatHeadMovementSpringY.getControlSpring().setVelocity(vel[1]);
                        }
                    }

                    ret = true;
                    break;
                case MotionEvent.ACTION_MOVE:
                    pushMotionEvent(event);

                    if(isInsideRemoveInfluence(touchX, touchY)) {
                        mState = STATE_HEAD_SNAP_REMOVE;
                        mChatHeadMovementSpringX.getControlSpring().setSpringConfig(mSpringConfigSnap);
                        mChatHeadMovementSpringY.getControlSpring().setSpringConfig(mSpringConfigSnap);
                        mChatHeadMovementSpringX.getControlSpring().setEndValue(mRemoveLayoutParams.x + mRemoveLayoutParams.width / 2);
                        mChatHeadMovementSpringY.getControlSpring().setEndValue(mRemoveLayoutParams.y + mRemoveLayoutParams.height / 2);
                    } else {
                        if(mState == STATE_HEAD_SNAP_REMOVE) {
                            mChatHeadMovementSpringX.getControlSpring().setCurrentValue(touchX).setAtRest();
                            mChatHeadMovementSpringY.getControlSpring().setCurrentValue(touchY).setAtRest();
                        } else {
                            float offsetX = lastX - touchX;
                            float offsetY = lastY - touchY;

                            mChatHeadMovementSpringX.getControlSpring().setCurrentValue(mChatHeadMovementSpringX.getControlSpring().getCurrentValue() - offsetX).setAtRest();
                            mChatHeadMovementSpringY.getControlSpring().setCurrentValue(mChatHeadMovementSpringY.getControlSpring().getCurrentValue() - offsetY).setAtRest();
                        }

                        mChatHeadMovementSpringX.getControlSpring().setSpringConfig(mSpringConfigDrag);
                        mChatHeadMovementSpringY.getControlSpring().setSpringConfig(mSpringConfigDrag);

                        mState = STATE_HEAD_DRAGGING;
                    }

                    ret = true;
                    break;
            }

            lastX = touchX;
            lastY = touchY;

            return ret;
        }
    };

    private SpringSystemListener mSpringSystemListener = new SpringSystemListener() {
        @Override
        public void onBeforeIntegrate(BaseSpringSystem springSystem) { /* nothing to do */ }

        @Override
        public void onAfterIntegrate(BaseSpringSystem springSystem) {
            checkConstraints();
        }
    };

    private SpringListener mRemoveSizeSpringListener = new SimpleSpringListener() {
        @Override
        public void onSpringUpdate(Spring spring) {
            if(mRemoveLayoutParams == null) {
                return;
            }

            Log.d("TAG", "Remove Spring Update");

            final float newSize = (float) (mChatHeadLength * mRemoveSizeSpring.getCurrentValue());
            final float halfDiff = (newSize - mChatHeadLength) / 2f;

            mRemoveLayoutParams.width = mRemoveLayoutParams.height = (int) newSize;

            mRemoveLayoutParams.x = (int) ((mScreenWidth / 2) - mChatHeadHalfLength - halfDiff);
            mRemoveLayoutParams.y = (int) (mScreenHeight - (Utils.dpToPx(mContext, REMOVE_BOTTOM_SPACING_DP) + mChatHeadLength) - halfDiff);

            if(mRemove.getWindowToken() != null) {
                mWindowManager.updateViewLayout(mRemove, mRemoveLayoutParams);
            }
        }
    };

}
