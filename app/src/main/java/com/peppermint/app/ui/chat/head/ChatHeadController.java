package com.peppermint.app.ui.chat.head;

import android.content.Context;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import com.peppermint.app.data.Chat;

/**
 * Created by Nuno Luz on 02-03-2016.
 *
 * Controls multiple {@link ChatHeadView} in the {@link WindowManager}.<br />
 *
 */
public class ChatHeadController implements View.OnKeyListener {

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
                mChatHeadView.snapTo(mRemoveView.getSnapPositionX(), mRemoveView.getSnapPositionY());
                mRemoveView.expand();
                _snappedToRemove = true;
                return true;
            }
            if(_snappedToRemove) {
                _snappedToRemove = false;
                mRemoveView.shrink();
                mChatHeadView.setReboundXY(event.getRawX() - (mChatHeadView.getChatHeadWidth() / 2), event.getRawY() - (mChatHeadView.getChatHeadHeight() / 2), true);
                return true;
            }
            return false;
        }

        @Override
        public boolean onDragFinished(int viewIndex, View view, float[] velocity, MotionEvent event) {
            if(mRemoveView.isInsideInfluence(event.getRawX(), event.getRawY())) {
                hide();
            } else {
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
        mChatView.requestLayout();
        mBackgroundView.requestLayout();
    }

    public boolean show() {
        return mChatHeadView.show();
    }

    public boolean hide() {
        mRemoveView.hide();
        mChatView.hide();
        mBackgroundView.hide();
        return mChatHeadView.hide();
    }

    public void destroy() {
        mChatView.destroy();
        mBackgroundView.destroy();
    }

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
            case KeyEvent.KEYCODE_HOME:
                mChatHeadView.snapToSavedPosition(true);
                break;
        }
        return false;
    }

}
