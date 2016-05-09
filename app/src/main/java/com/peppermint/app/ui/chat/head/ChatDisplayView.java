package com.peppermint.app.ui.chat.head;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.peppermint.app.R;
import com.peppermint.app.data.Chat;
import com.peppermint.app.ui.KeyInterceptable;
import com.peppermint.app.ui.OverlayManager;
import com.peppermint.app.ui.base.views.TouchInterceptorView;
import com.peppermint.app.ui.chat.ChatController;
import com.peppermint.app.ui.chat.RecipientDataGUI;

/**
 * Created by Nuno Luz on 20-03-2016.
 *
 * The chat/message list UI overlay component.
 */
public class ChatDisplayView extends DisplayView<TouchInterceptorView> implements RecipientDataGUI, KeyInterceptable {

    // chat controller
    private ChatController mChatController;
    private OverlayManager mOverlayManager;

    private int mTopMargin = 0;

    public ChatDisplayView(Context mContext, Display mDisplay) {
        super(mContext, mDisplay);

        setView((TouchInterceptorView) LayoutInflater.from(mContext).inflate(R.layout.a_chat_head_layout, null),
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, 0);
        // allow focus for back button
        mViewLayoutParams.flags = mViewOriginalLayoutParams.flags = WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED |
                WindowManager.LayoutParams.FLAG_LAYOUT_ATTACHED_IN_DECOR;
        mView.setVisibility(View.INVISIBLE);

        this.mOverlayManager = new OverlayManager(mContext, null, (FrameLayout) mView.findViewById(R.id.lytOverlay));

        this.mChatController = new ChatController(mContext, this);
        this.mChatController.init(mView, mOverlayManager, mView, null);
    }

    @Override
    public void onDisplaySizeObtained(int prevDisplayWidth, int prevDisplayHeight, int displayWidth, int displayHeight) {
        mViewLayoutParams.height = mViewOriginalLayoutParams.height = displayHeight - mTopMargin;
        mViewLayoutParams.y = mViewOriginalLayoutParams.y = mTopMargin;
        // dont recalculate position (pass 0 as previous measurements)
        super.onDisplaySizeObtained(0, 0, displayWidth, displayHeight);
    }

    public void init() {
        super.init();
        super.show();
        mChatController.start();
    }

    public void deinit() {
        super.hide();
        mChatController.stop();
        mView.removeAllKeyEventInterceptors();
        mChatController.deinit();
        mChatController.setContext(null);
        mOverlayManager.destroyAllOverlays();
        super.deinit();
    }

    @Override
    public boolean show() {
        if(mView.getVisibility() != View.VISIBLE) {
            mView.setVisibility(View.VISIBLE);
            setViewPosition(0, mTopMargin, false);
            return true;
        }
        return false;
    }

    @Override
    public boolean hide() {
        if(mView.getVisibility() != View.INVISIBLE) {
            mView.setVisibility(View.INVISIBLE);
            return true;
        }
        return false;
    }

    public void setChat(Chat chat) {
        mChatController.setAutoPlayMessageId(chat.getLastReceivedUnplayedId());
        mChatController.setChat(chat);
    }

    @Override
    public void setRecipientData(String recipientName, String recipientVia, String recipientPhotoUri) {
        if(mView != null) {
            TextView txtName = (TextView) mView.findViewById(R.id.txtName);
            txtName.setText(recipientName);
        }
    }

    public int getTopMargin() {
        return mTopMargin;
    }

    public void setTopMargin(int mTopMargin) {
        this.mTopMargin = mTopMargin;
    }

    @Override
    public void addKeyEventInterceptor(View.OnKeyListener listener) {
        mView.addKeyEventInterceptor(listener);
    }

    @Override
    public boolean removeKeyEventInterceptor(View.OnKeyListener listener) {
        return mView.removeKeyEventInterceptor(listener);
    }

    @Override
    public void removeAllKeyEventInterceptors() {
        mView.removeAllKeyEventInterceptors();
    }
}
