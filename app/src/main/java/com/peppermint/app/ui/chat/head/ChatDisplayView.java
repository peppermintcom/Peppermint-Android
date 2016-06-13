package com.peppermint.app.ui.chat.head;

import android.content.Context;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.peppermint.app.R;
import com.peppermint.app.dal.chat.Chat;
import com.peppermint.app.ui.base.KeyInterceptable;
import com.peppermint.app.ui.base.OverlayManager;
import com.peppermint.app.ui.base.views.TouchInterceptorView;
import com.peppermint.app.ui.chat.ChatController;
import com.peppermint.app.ui.chat.RecipientDataGUI;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Nuno Luz on 20-03-2016.
 *
 * The chat/message list UI overlay component.
 */
public class ChatDisplayView extends DisplayView<TouchInterceptorView> implements RecipientDataGUI, KeyInterceptable, View.OnClickListener {

    // chat controller
    private ChatController mChatController;
    private OverlayManager mOverlayManager;

    private List<View.OnKeyListener> mKeyListeners = new ArrayList<>();
    private int mTopMargin = 0;

    public ChatDisplayView(Context mContext, Display mDisplay) {
        super(mContext, mDisplay);
    }

    @Override
    public void onDisplaySizeObtained(int prevDisplayWidth, int prevDisplayHeight, int displayWidth, int displayHeight) {
        mViewLayoutParams.height = mViewOriginalLayoutParams.height = displayHeight;/*- mTopMargin;*/
        /*mViewLayoutParams.y = mViewOriginalLayoutParams.y = mTopMargin;*/
        // dont recalculate position (pass 0 as previous measurements)
        super.onDisplaySizeObtained(0, 0, displayWidth, displayHeight);
    }

    public void init() {
        final TouchInterceptorView v = (TouchInterceptorView) LayoutInflater.from(mContext).inflate(R.layout.a_chat_head_layout, null);
        v.setOnClickListener(this);
        ((ViewGroup.MarginLayoutParams) v.findViewById(R.id.lytBox).getLayoutParams()).topMargin = mTopMargin;
        setView(v, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, 0);

        // allow focus for back button
        mViewLayoutParams.flags = mViewOriginalLayoutParams.flags = WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED |
                WindowManager.LayoutParams.FLAG_LAYOUT_ATTACHED_IN_DECOR;
        mView.setVisibility(View.GONE);
        mView.setKeyEventInterceptors(mKeyListeners);

        this.mOverlayManager = new OverlayManager(mContext, null, (FrameLayout) mView.findViewById(R.id.lytOverlay));

        this.mChatController = new ChatController(mContext, this);
        this.mChatController.setAllowSkipNotification(false);
        this.mChatController.init(mView, mOverlayManager, mView, null);

        super.init();
        super.show();
    }

    public void deinit() {
        super.hide();
        stop();
        if(mChatController != null) {
            mChatController.deinit();
        }
        if(mOverlayManager != null) {
            mOverlayManager.destroyAllOverlays();
        }
        super.deinit();
    }

    @Override
    public boolean show() {
        if(mView.getVisibility() != View.VISIBLE) {
            mView.setVisibility(View.VISIBLE);
            /*setViewPosition(0, mTopMargin, false);*/
            return true;
        }
        return false;
    }

    @Override
    public boolean hide() {
        if(mView != null && mView.getVisibility() != View.GONE) {
            mChatController.stopPlayer();
            mView.setVisibility(View.GONE);
            return true;
        }
        return false;
    }

    public void start() {
        if(!mChatController.isStarted()) {
            mChatController.start();
        }
    }

    public void stop() {
        if(mChatController != null && mChatController.isStarted()) {
            mChatController.stop();
        }
    }

    public void setChat(Chat chat) {
        mChatController.setChat(chat);
    }

    @Override
    public void setRecipientData(String recipientName, String recipientVia, String recipientPhotoUri) {
        if(mView != null) {
            TextView txtName = (TextView) mView.findViewById(R.id.txtName);
            txtName.setText(recipientName);
        }
    }

    public void setTopMargin(int mTopMargin) {
        this.mTopMargin = mTopMargin;
        if(mView != null) {
            ((ViewGroup.MarginLayoutParams) mView.findViewById(R.id.lytBox).getLayoutParams()).topMargin = mTopMargin;
        }
    }

    @Override
    public void addKeyEventInterceptor(View.OnKeyListener listener) {
        mKeyListeners.add(listener);
    }

    @Override
    public boolean removeKeyEventInterceptor(View.OnKeyListener listener) {
        return mKeyListeners.remove(listener);
    }

    @Override
    public void removeAllKeyEventInterceptors() {
        mView.removeAllKeyEventInterceptors();
    }

    @Override
    public void onLocaleChanged() {
        if(mChatController == null || mOverlayManager == null) {
            return;
        }

        boolean wasStarted;
        if((wasStarted = mChatController.isStarted())) {
            mChatController.stop();
        }

        final Chat chat = mChatController.getChat();

        mChatController.deinit();
        mOverlayManager.destroyAllOverlays();

        // do not reattach the whole view to avoid it being drawn on top of other views
        // just keep the root view and refresh the children
        final TouchInterceptorView v = (TouchInterceptorView) LayoutInflater.from(mContext).inflate(R.layout.a_chat_head_layout, null);
        mView.removeAllViews();
        while(v.getChildCount() > 0) {
            View childView = v.getChildAt(0);
            final ViewGroup.LayoutParams layoutParams = childView.getLayoutParams();
            v.removeViewAt(0);
            mView.addView(childView, layoutParams);
        }

        this.mOverlayManager = new OverlayManager(mContext, null, (FrameLayout) mView.findViewById(R.id.lytOverlay));

        this.mChatController = new ChatController(mContext, this);
        this.mChatController.init(mView, mOverlayManager, mView, null);

        if(wasStarted) {
            mChatController.start();
        }

        setChat(chat);
    }

    @Override
    public void onClick(View v) {
        final KeyEvent dummyEvent = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK);
        for(View.OnKeyListener keyListener : mKeyListeners) {
            keyListener.onKey(mView, KeyEvent.KEYCODE_BACK, dummyEvent);
        }
    }
}
