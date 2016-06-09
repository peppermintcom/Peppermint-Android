package com.peppermint.app.ui.chat;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.peppermint.app.R;
import com.peppermint.app.dal.DataObjectEvent;
import com.peppermint.app.dal.DatabaseHelper;
import com.peppermint.app.dal.chat.Chat;
import com.peppermint.app.dal.chat.ChatManager;
import com.peppermint.app.dal.message.Message;
import com.peppermint.app.dal.message.MessageManager;
import com.peppermint.app.dal.recipient.Recipient;
import com.peppermint.app.trackers.TrackerManager;
import com.peppermint.app.ui.base.views.CustomFontTextView;
import com.peppermint.app.ui.canvas.avatar.AnimatedAvatarView;
import com.peppermint.app.utils.DateContainer;
import com.peppermint.app.utils.ResourceUtils;
import com.peppermint.app.utils.SameAsyncTaskExecutor;
import com.peppermint.app.utils.Utils;

import java.text.ParseException;
import java.util.TimeZone;

/**
 * Created by Nuno Luz on 07-06-2016.
 *
 * Represents a Chat in the UI.
 */
public class ChatView extends LinearLayout {

    protected static final int FIXED_AVATAR_SIZE_DP = 50;

    private class ExtraDataLoaderTask extends SameAsyncTaskExecutor<Chat, Void, Object[]> {
        private int mAvatarWidth, mAvatarHeight;

        public ExtraDataLoaderTask(Context mContext) {
            super(mContext);
        }

        @Override
        protected void onPreExecute() {
            mAvatarWidth = mImgAvatar.getMeasuredWidth();
            mAvatarHeight = mImgAvatar.getMeasuredHeight();
        }

        @Override
        protected Object[] doInBackground(SameAsyncTask sameAsyncTask, Chat... params) {
            Integer amountUnopened = null;
            BitmapDrawable scaledBitmapDrawable = null;

            synchronized (ChatView.this) {
                if (params[0] == null) {
                    return null;
                }

                if (!sameAsyncTask.isCancelled()) {
                    amountUnopened = MessageManager.getInstance(null).
                            getUnopenedCountByChat(mDatabaseHelper.getReadableDatabase(), params[0].getId());
                }

                if (!sameAsyncTask.isCancelled() && params[0] != null) {
                    // for now only a single recipient is supported
                    final Recipient recipient = params[0].getRecipientList().get(0);
                    if (recipient != null && recipient.getPhotoUri() != null) {
                        final Uri uri = Uri.parse(recipient.getPhotoUri());
                        final int fixedSize = Utils.dpToPx(mContext, FIXED_AVATAR_SIZE_DP);
                        final Bitmap scaledBitmap = ResourceUtils.getScaledBitmap(mContext, uri,
                                mAvatarWidth > 0 ? mAvatarWidth : fixedSize,
                                mAvatarHeight > 0 ? mAvatarHeight : fixedSize);
                        if (scaledBitmap != null) {
                            scaledBitmapDrawable = new BitmapDrawable(mContext.getResources(), scaledBitmap);
                        }
                    }
                }
            }

            params[0].setAmountUnopened(amountUnopened == null ? 0 : amountUnopened);

            return new Object[]{ params[0], scaledBitmapDrawable };
        }

        @Override
        protected void onPostExecute(Object[] data) {
            final Chat chat = (Chat) data[0];
            final BitmapDrawable scaledBitmapDrawable = (BitmapDrawable) data[1];

            if(chat != null) {
                if (chat.getAmountUnopened() > 0) {
                    mTxtUnreadMessages.setText(String.valueOf(chat.getAmountUnopened()));
                    mTxtUnreadMessages.setVisibility(View.VISIBLE);
                } else {
                    mTxtUnreadMessages.setVisibility(View.GONE);
                }

                if (scaledBitmapDrawable != null) {
                    mImgAvatar.setStaticDrawable(scaledBitmapDrawable);
                    mImgAvatar.setShowStaticAvatar(true);
                } else {
                    mImgAvatar.setShowStaticAvatar(false);
                }
            }
        }
    }

    protected AnimatedAvatarView mImgAvatar;
    protected CustomFontTextView mTxtName, mTxtContact;
    protected CustomFontTextView mTxtUnreadMessages, mTxtLastMessageDate;

    protected Chat mChat;
    protected DatabaseHelper mDatabaseHelper;
    protected Context mContext;

    private ExtraDataLoaderTask mExtraDataLoader;

    public ChatView(Context context) {
        super(context);
        init(context);
    }

    public ChatView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ChatView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ChatView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    protected void init(Context context) {
        mContext = context;
        mDatabaseHelper = DatabaseHelper.getInstance(context);
        mExtraDataLoader = new ExtraDataLoaderTask(context);

        setOrientation(VERTICAL);
        LayoutInflater.from(context).inflate(R.layout.i_chat_layout, this);

        mImgAvatar = (AnimatedAvatarView) findViewById(R.id.imgPhoto);
        mTxtName = (CustomFontTextView) findViewById(R.id.txtName);
        mTxtContact = (CustomFontTextView) findViewById(R.id.txtContact);
        mTxtUnreadMessages = (CustomFontTextView) findViewById(R.id.txtUnreadMessages);
        mTxtLastMessageDate = (CustomFontTextView) findViewById(R.id.txtLastMessageDate);

        final ImageView dividerImageView = new ImageView(context);
        dividerImageView.setImageResource(R.color.divider_grey);
        final LinearLayout.LayoutParams layoutParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Utils.dpToPx(context, 1));
        layoutParams.gravity = Gravity.BOTTOM;
        addView(dividerImageView, layoutParams);
    }

    protected void refreshChatData() {
        if(mImgAvatar.isShowStaticAvatar()) {
            mImgAvatar.setShowStaticAvatar(false);
        }

        mTxtUnreadMessages.setVisibility(View.GONE);

        if(mChat == null || mChat.getRecipientList().size() <= 0) {
            return;
        }

        // for now only a single recipient is supported
        final Recipient recipient = mChat.getRecipientList().get(0);

        if(recipient != null) {
            mTxtName.setText(recipient.getDisplayName());
            if(mChat.getPeppermintChatId() > 0 || mChat.isPeppermint()) {
                mTxtContact.setText(R.string.app_name);
            } else {
                mTxtContact.setText(recipient.getVia());
            }
        }

        if(mChat.getLastMessageTimestamp() != null) {
            try {
                DateContainer lastMessageDate = new DateContainer(DateContainer.TYPE_DATE, mChat.getLastMessageTimestamp());
                mTxtLastMessageDate.setText(DateContainer.getRelativeLabelToToday(getContext(), lastMessageDate, TimeZone.getDefault()));
            } catch (ParseException e) {
                TrackerManager.getInstance(getContext()).logException(e);
                mTxtLastMessageDate.setText(mChat.getLastMessageTimestamp());
            }
        } else {
            mTxtLastMessageDate.setText("");
        }

        mExtraDataLoader.execute(mChat);
    }

    public Chat getChat() {
        return mChat;
    }

    public void setChat(Chat mChat) {
        synchronized (this) {
            this.mChat = mChat;
        }
        refreshChatData();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        MessageManager.getInstance(getContext()).registerDataListener(mMessageDataObjectListener);
        ChatManager.getInstance(getContext()).registerDataListener(mChatDataObjectListener);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        MessageManager.getInstance(getContext()).unregisterDataListener(mMessageDataObjectListener);
        ChatManager.getInstance(getContext()).unregisterDataListener(mChatDataObjectListener);
        mExtraDataLoader.cancel(true);
    }

    private Object mMessageDataObjectListener = new Object() {
        public void onEventMainThread(DataObjectEvent<Message> messageDataObjectEvent) {
            if (mChat != null && messageDataObjectEvent.getDataObject().getChatId() == mChat.getId()) {
                refreshChatData();
            }
        }
    };

    private Object mChatDataObjectListener = new Object() {
        public void onEventMainThread(DataObjectEvent<Chat> chatDataObjectEvent) {
            if (mChat != null && chatDataObjectEvent.getDataObject().getId() == mChat.getId()) {
                refreshChatData();
            }
        }
    };
}
