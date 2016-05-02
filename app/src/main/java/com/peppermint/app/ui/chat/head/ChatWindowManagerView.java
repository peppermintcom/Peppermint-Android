package com.peppermint.app.ui.chat.head;

import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.peppermint.app.R;
import com.peppermint.app.data.Chat;
import com.peppermint.app.ui.OverlayManager;
import com.peppermint.app.ui.base.views.TouchInterceptorView;
import com.peppermint.app.ui.chat.ChatController;
import com.peppermint.app.ui.chat.RecipientDataGUI;
import com.peppermint.app.utils.Utils;

/**
 * Created by Nuno Luz on 20-03-2016.
 */
public class ChatWindowManagerView extends WindowManagerViewGroup implements RecipientDataGUI {

    // chat controller
    private TouchInterceptorView mChatView;
    private WindowManager.LayoutParams mChatViewLayoutParams;

    private ChatController mChatController;
    private OverlayManager mOverlayManager;

    private int mTopMargin = 0;

    public ChatWindowManagerView(Context mContext) {
        super(mContext, null);

        this.mChatView = (TouchInterceptorView) LayoutInflater.from(mContext).inflate(R.layout.a_chat_head_layout, null);
        this.mChatViewLayoutParams = new WindowManager.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                PixelFormat.TRANSLUCENT);
        this.mChatViewLayoutParams.gravity = Gravity.TOP | Gravity.LEFT;
        addView(mChatView, mChatViewLayoutParams);

        this.mOverlayManager = new OverlayManager(mContext, null, (FrameLayout) this.mChatView.findViewById(R.id.lytOverlay));

        this.mChatController = new ChatController(mContext, this);
        this.mChatController.init(mChatView, mOverlayManager, mChatView, null);
    }

    @Override
    public boolean show() {
        boolean shown = super.show();
        if(shown) {
            mChatController.start();
        }
        return shown;
    }

    @Override
    public boolean hide() {
        boolean hidden = super.hide();
        if(hidden) {
            mChatController.stop();
        }
        return hidden;
    }

    public void destroy() {
        mChatController.deinit();
        mChatController.setContext(null);
        mOverlayManager.destroyAllOverlays();
    }

    public void setChat(Chat chat) {
        mChatController.setAutoPlayMessageId(chat.getLastReceivedUnplayedId());
        mChatController.setChat(chat.getId());
    }

    public void requestLayout() {
        int statusBarHeight = Utils.getStatusBarHeight(mContext);
        Point point = Utils.getScreenSize(mContext);
        mChatViewLayoutParams.height = point.y - mTopMargin - statusBarHeight;
        setViewPosition(0, 0, mTopMargin + statusBarHeight);
    }

    @Override
    public void setRecipientData(String recipientName, String recipientVia, String recipientPhotoUri) {
        if(mChatView != null) {
            TextView txtName = (TextView) mChatView.findViewById(R.id.txtName);
            txtName.setText(recipientName);
        }
    }

    public int getTopMargin() {
        return mTopMargin;
    }

    public void setTopMargin(int mTopMargin) {
        this.mTopMargin = mTopMargin;
    }
}
