package com.peppermint.app.ui.chat.head;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import com.peppermint.app.data.Chat;
import com.peppermint.app.utils.Utils;

/**
 * Created by Nuno Luz on 02-03-2016.
 *
 * Controls multiple {@link ChatHeadView} in the {@link WindowManager}.<br />
 *
 */
public class ChatHeadController implements View.OnKeyListener {

    private final BroadcastReceiver mHomeAppsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)) {
                if(mChatHeadView.isVisible() && mChatHeadView.getState() == mChatHeadView.STATE_EXPAND) {
                    mChatHeadView.snapToSavedPosition(true);
                }
            }
        }
    };
    private final IntentFilter mHomeAppsIntentFilter = new IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);

    private Context mContext;

    // ui
    private BackgroundView mBackgroundView;
    private RemoveView mRemoveView;
    private ChatHeadChainView mChatHeadView;
    private ChatWindowManagerView mChatView;

    private ChatHeadChainView.OnStateChangedListener mOnStateChangeListener = new ChatHeadChainView.OnStateChangedListener() {
        @Override
        public void onSnapped(int viewIndex) {
        }

        @Override
        public void onShrinkStarted() {
            Log.d("TAG", "onShrinkStarted()");
            mChatView.hide();
            mBackgroundView.hide();
        }

        @Override
        public void onExpandStarted() {
            mBackgroundView.show();
        }

        @Override
        public void onExpandFinished() {
            Log.d("TAG", "onExpandFinished()");
            Chat chat = mChatHeadView.getSelectedChat();
            onChatSelected(chat);
        }

        @Override
        public void onChatSelected(Chat chat) {
            mChatView.setChat(chat);
            mChatView.show();
            mChatView.invalidate();
        }
    };

    private WindowManagerViewGroup.OnInteractionListener mOnInteractionListener = new WindowManagerViewGroup.OnInteractionListener() {
        private boolean _snappedToRemove = false;

        @Override
        public boolean onClick(int viewIndex, View view) {
            return false;
        }

        @Override
        public boolean onDragStarted(int viewIndex, View view, float offsetX, float offsetY, MotionEvent event) {
            mRemoveView.show();
            mBackgroundView.show();
            return onDrag(viewIndex, view, offsetX, offsetY, event);
        }

        @Override
        public boolean onDrag(int viewIndex, View view, float offsetX, float offsetY, MotionEvent event) {
            if(mRemoveView.isInsideInfluence(event.getRawX(), event.getRawY())) {
                if(!_snappedToRemove) {
                    mChatHeadView.setShowBadge(false);
                }
                mChatHeadView.snapTo(mRemoveView.getSnapPositionX(), mRemoveView.getSnapPositionY());
                mRemoveView.expand();
                _snappedToRemove = true;
                return true;
            }
            if(_snappedToRemove) {
                mChatHeadView.setShowBadge(true);
                _snappedToRemove = false;
                mRemoveView.shrink();
                mChatHeadView.setReboundXY(event.getRawX() - (mChatHeadView.getWidth() / 2), event.getRawY() - (mChatHeadView.getHeight() / 2), true);
                return true;
            }
            return false;
        }

        @Override
        public boolean onDragFinished(int viewIndex, View view, float[] velocity, MotionEvent event) {
            Log.d("TAG", "onDragFinished()");
            if(mRemoveView.isInsideInfluence(event.getRawX(), event.getRawY())) {
                hide();
            } else {
                Log.d("TAG", "Hide Remove and Background");
                mRemoveView.hide();
                mBackgroundView.hide();
            }
            return false;
        }
    };

    public ChatHeadController(Context context) {
        this.mContext = context;

        mBackgroundView = new BackgroundView(mContext);
        mBackgroundView.addKeyEventInterceptor(this);

        mChatHeadView = new ChatHeadChainView(mContext);
        mChatHeadView.addOnStateChangedListener(mOnStateChangeListener);
        mChatHeadView.addOnInteractionListener(mOnInteractionListener);

        mRemoveView = new RemoveView(mContext);

        mChatView = new ChatWindowManagerView(mContext);
        mContext.registerReceiver(mHomeAppsReceiver, mHomeAppsIntentFilter);
    }

    public void pushChat(Chat chat) {
        mChatHeadView.addChat(chat);
    }

    public void invalidate() {
        mChatHeadView.invalidate();
    }

    public void requestLayout() {
        mChatHeadView.requestLayout();
        mRemoveView.requestLayout();
        // -2 dp to be sure (there's still a really small gap without it)
        mChatView.setTopMargin(mChatHeadView.getSelectModeHeight() + Utils.dpToPx(mContext, ChatHeadChainView.EXPANDED_TOP_MARGIN_DP - 2));
        mChatView.requestLayout();
        mBackgroundView.requestLayout();
    }

    public boolean show() {
        // add invisible view to window
        // but still keep it invisible
        mBackgroundView.addViewsToWindow();
        return mChatHeadView.show();
    }

    public boolean hide() {
        mRemoveView.hide();
        mChatView.hide();
        mBackgroundView.hide();
        return mChatHeadView.hide();
    }

    public void destroy() {
        mContext.unregisterReceiver(mHomeAppsReceiver);
        mChatView.destroy();
        mBackgroundView.destroy();
    }

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        Log.d("TAG", "onKey = " + keyCode +   "   " + event);
        if(event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_BACK:
                case KeyEvent.KEYCODE_HOME:
                case KeyEvent.KEYCODE_APP_SWITCH:
                    mChatHeadView.snapToSavedPosition(true);
                    break;
            }
        }
        return false;
    }

}
