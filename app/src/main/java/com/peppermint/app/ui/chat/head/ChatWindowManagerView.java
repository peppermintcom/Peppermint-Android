package com.peppermint.app.ui.chat.head;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.support.v4.content.LocalBroadcastManager;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.peppermint.app.R;
import com.peppermint.app.authenticator.AuthenticationPolicyEnforcer;
import com.peppermint.app.cloud.senders.GetResultActivity;
import com.peppermint.app.data.Chat;
import com.peppermint.app.ui.OverlayManager;
import com.peppermint.app.ui.chat.ChatController;
import com.peppermint.app.ui.chat.RecipientDataGUI;
import com.peppermint.app.ui.chat.recorder.ChatRecordOverlayController;
import com.peppermint.app.ui.views.simple.TouchInterceptorView;
import com.peppermint.app.utils.Utils;

/**
 * Created by Nuno Luz on 20-03-2016.
 */
public class ChatWindowManagerView extends WindowManagerViewGroup implements ChatRecordOverlayController.Callbacks, RecipientDataGUI {

    private static final int REQUEST_NEWCONTACT_AND_SEND = 154;

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

        this.mChatController = new ChatController(mContext, this, this);
        this.mChatController.init(mChatView, mOverlayManager, mChatView, new AuthenticationPolicyEnforcer(mContext, null), null);
    }

    @Override
    public boolean show() {
        boolean shown = super.show();
        if(shown) {
            mChatController.start();
            // register the activity result broadcast listener
            LocalBroadcastManager.getInstance(mContext).registerReceiver(mReceiver, mFilter);
        }
        return shown;
    }

    @Override
    public boolean hide() {
        boolean hidden = super.hide();
        if(hidden) {
            LocalBroadcastManager.getInstance(mContext).unregisterReceiver(mReceiver);
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
    public void onNewContact(Intent intentToLaunchActivity) {
        Intent i = new Intent(mContext, GetResultActivity.class);
        i.putExtra(GetResultActivity.INTENT_ID, mChatController.getChat().getId());
        i.putExtra(GetResultActivity.INTENT_REQUESTCODE, REQUEST_NEWCONTACT_AND_SEND);
        i.putExtra(GetResultActivity.INTENT_DATA, intentToLaunchActivity);
        i.putExtra(GetResultActivity.INTENT_BROADCAST_TYPE, BROADCAST_TYPE);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(i);
    }

    /**
     * This broadcast receiver gets results from {@link GetResultActivity}<br />
     * It allows any SenderErrorHandler to recover from an error by triggering activities.<br />
     * This is useful for using APIs that request permissions through another Activity, such as the Google API
     **/
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent == null) {
                return;
            }

            int requestCode = intent.getIntExtra(GetResultActivity.INTENT_REQUESTCODE, -1);
            int resultCode = intent.getIntExtra(GetResultActivity.INTENT_RESULTCODE, -1);
            long chatId = intent.getLongExtra(GetResultActivity.INTENT_ID, 0l);
            Intent data = intent.getParcelableExtra(GetResultActivity.INTENT_DATA);

            if(requestCode == REQUEST_NEWCONTACT_AND_SEND &&
                    chatId == mChatController.getChat().getId() &&
                    data != null) {
                mChatController.handleNewContactResult(resultCode, data);
            }
        }
    };
    private static final String BROADCAST_TYPE = ChatWindowManagerView.class.getSimpleName();
    private final IntentFilter mFilter = new IntentFilter(BROADCAST_TYPE);

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
