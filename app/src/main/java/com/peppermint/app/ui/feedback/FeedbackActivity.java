package com.peppermint.app.ui.feedback;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.peppermint.app.R;
import com.peppermint.app.data.Chat;
import com.peppermint.app.data.ChatManager;
import com.peppermint.app.data.ContactRaw;
import com.peppermint.app.data.DatabaseHelper;
import com.peppermint.app.data.GlobalManager;
import com.peppermint.app.data.Message;
import com.peppermint.app.data.Recording;
import com.peppermint.app.tracking.TrackerManager;
import com.peppermint.app.ui.base.CustomActionBarView;
import com.peppermint.app.ui.base.activities.CustomActionBarActivity;
import com.peppermint.app.ui.canvas.avatar.AnimatedAvatarView;
import com.peppermint.app.ui.chat.ChatActivity;
import com.peppermint.app.ui.chat.recorder.ChatRecordOverlayController;
import com.peppermint.app.utils.Utils;

import java.sql.SQLException;
import java.util.Random;

/**
 * Created by Nuno Luz on 26-04-2016.
 *
 * Feedback Activity.
 */
public class FeedbackActivity extends CustomActionBarActivity implements View.OnClickListener, View.OnLongClickListener {

    // avatar animation frequency
    private static final int FIXED_AVATAR_ANIMATION_INTERVAL_MS = 7500;
    private static final int VARIABLE_AVATAR_ANIMATION_INTERVAL_MS = 7500;

    private ChatRecordOverlayController mChatRecordOverlayController;
    private View mRecordView;
    private AnimatedAvatarView mAnimatedAvatarView;

    // smiley face (avatar) random animations
    private final Random mRandom = new Random();
    private final Handler mHandler = new Handler();
    private final Runnable mAnimationRunnable = new Runnable() {
        @Override
        public void run() {
            mAnimatedAvatarView.resetAnimations();
            mAnimatedAvatarView.startAnimations();
            mHandler.postDelayed(mAnimationRunnable, FIXED_AVATAR_ANIMATION_INTERVAL_MS + mRandom.nextInt(VARIABLE_AVATAR_ANIMATION_INTERVAL_MS));
        }
    };

    @Override
    protected final int getContainerViewLayoutId() {
        return R.layout.f_feedback;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final CustomActionBarView actionBarView = getCustomActionBar();
        if(actionBarView != null) {
            actionBarView.setTitle(getString(R.string.about));
        }

        final TextView txtEmailUs = (TextView) findViewById(R.id.txtEmailUs);
        txtEmailUs.setOnClickListener(this);

        mRecordView = findViewById(R.id.lytRecord);
        mRecordView.setOnLongClickListener(this);

        mAnimatedAvatarView = (AnimatedAvatarView) findViewById(R.id.imgPhoto);

        // record overlay controller
        mChatRecordOverlayController = new ChatRecordOverlayController(this) {
            @Override
            protected Message sendMessage(Chat chat, Recording recording) {
                Intent chatIntent = new Intent(FeedbackActivity.this, ChatActivity.class);
                chatIntent.putExtra(ChatActivity.PARAM_CHAT_ID, chat.getId());
                startActivity(chatIntent);

                finish();

                return super.sendMessage(chat, recording);
            }
        };
        mChatRecordOverlayController.init(getContainerView(), mOverlayManager,
                this, savedInstanceState);
    }

    @Override
    protected void onStart() {
        super.onStart();
        getAuthenticationData(getIntentReplica());
        mAnimatedAvatarView.startDrawingThread();
        mHandler.postDelayed(mAnimationRunnable, FIXED_AVATAR_ANIMATION_INTERVAL_MS + mRandom.nextInt(VARIABLE_AVATAR_ANIMATION_INTERVAL_MS));
        mChatRecordOverlayController.start();
    }

    @Override
    protected void onStop() {
        mChatRecordOverlayController.stop();
        mHandler.removeCallbacks(mAnimationRunnable);
        mAnimatedAvatarView.stopDrawingThread();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        mChatRecordOverlayController.deinit();
        mChatRecordOverlayController.setContext(null);

        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        mChatRecordOverlayController.saveInstanceState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onClick(View v) {
        Utils.triggerSupportEmail(this);
    }

    @Override
    public boolean onLongClick(View v) {

        final ContactRaw contactRaw = new ContactRaw(0, 0, false, null, null, getString(R.string.support_display_name), null, getString(R.string.support_email),
                null);

        Chat tappedChat;
        try {
            tappedChat = GlobalManager.insertOrUpdateChatAndRecipients(this, contactRaw);
        } catch (SQLException e) {
            TrackerManager.getInstance(this.getApplicationContext()).logException(e);
            Toast.makeText(this, R.string.msg_database_error, Toast.LENGTH_LONG).show();
            return true;
        }

        if(!mChatRecordOverlayController.triggerRecording(mRecordView, tappedChat)) {
            final DatabaseHelper databaseHelper = DatabaseHelper.getInstance(this);
            databaseHelper.lock();
            try {
                ChatManager.delete(databaseHelper.getWritableDatabase(), tappedChat.getId());
            } catch (SQLException e) {
                TrackerManager.getInstance(this.getApplicationContext()).logException(e);
            }
            databaseHelper.unlock();
        }

        return true;
    }
}
