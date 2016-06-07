package com.peppermint.app.ui.recipients;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;

import com.peppermint.app.R;
import com.peppermint.app.dal.DataObjectEvent;
import com.peppermint.app.dal.chat.Chat;
import com.peppermint.app.dal.chat.ChatManager;
import com.peppermint.app.dal.contact.ContactRaw;
import com.peppermint.app.dal.recipient.Recipient;
import com.peppermint.app.ui.base.views.CustomFontTextView;
import com.peppermint.app.ui.chat.ChatView;
import com.peppermint.app.utils.ResourceUtils;
import com.peppermint.app.utils.Utils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by Nuno Luz on 07-06-2016.
 *
 * Represents a Chat in the UI.
 */
public class ContactView extends ChatView {

    private class ExtraDataLoaderTask extends AsyncTask<Void, Void, Chat> {
        private int mAvatarWidth, mAvatarHeight;
        private BitmapDrawable mScaledBitmapDrawable;

        @Override
        protected void onPreExecute() {
            mAvatarWidth = mImgAvatar.getMeasuredWidth();
            mAvatarHeight = mImgAvatar.getMeasuredHeight();
        }

        @Override
        protected Chat doInBackground(Void... params) {
            Chat chat = null;

            synchronized (ContactView.this) {
                if (mContactRaw == null) {
                    return null;
                }

                if (!isCancelled()) {
                    chat = ChatManager.getInstance(mContext).
                            getMainChatByDroidContactId(mDatabaseHelper.getReadableDatabase(), mContactRaw.getContactId());
                }

                if (chat == null && !isCancelled()) {
                    // for now only a single recipient is supported
                    if (mContactRaw.getPhotoUri() != null) {
                        final Uri uri = Uri.parse(mContactRaw.getPhotoUri());
                        final int fixedSize = Utils.dpToPx(mContext, FIXED_AVATAR_SIZE_DP);
                        final Bitmap scaledBitmap = ResourceUtils.getScaledBitmap(mContext, uri,
                                mAvatarWidth > 0 ? mAvatarWidth : fixedSize,
                                mAvatarHeight > 0 ? mAvatarHeight : fixedSize);
                        if (scaledBitmap != null) {
                            mScaledBitmapDrawable = new BitmapDrawable(mContext.getResources(), scaledBitmap);
                        }
                    }
                }
            }

            return chat;
        }

        @Override
        protected void onPostExecute(Chat chat) {
            if(mContactRaw != null) {
                if (chat != null) {
                    setChat(chat);
                } else {
                    if (mScaledBitmapDrawable != null) {
                        mImgAvatar.setStaticDrawable(mScaledBitmapDrawable);
                        mImgAvatar.setShowStaticAvatar(true);
                    } else {
                        mImgAvatar.setShowStaticAvatar(false);
                    }
                }
            }
            mTaskSet.remove(this);
        }

        @Override
        protected void onCancelled(Chat aVoid) {
            mTaskSet.remove(this);
        }
    }

    protected CustomFontTextView mTxtVia;

    protected ContactRaw mContactRaw;

    private Set<ExtraDataLoaderTask> mTaskSet = new HashSet<>(2);

    public ContactView(Context context) {
        super(context);
    }

    public ContactView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ContactView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ContactView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void init(Context context) {
        super.init(context);
        mTxtVia = (CustomFontTextView) findViewById(R.id.txtVia);
    }

    protected void refreshContactRawData() {
        if(mImgAvatar.isShowStaticAvatar()) {
            mImgAvatar.setShowStaticAvatar(false);
        }

        if(mContactRaw == null) {
            return;
        }

        mTxtName.setText(mContactRaw.getDisplayName());
        if(mContactRaw.getPeppermint() != null) {
            mTxtContact.setText(R.string.app_name);
            mTxtVia.setVisibility(View.VISIBLE);
        } else {
            mTxtContact.setText(mContactRaw.getMainDataVia());
            if(mContactRaw.getMainDataVia() == null || mContactRaw.getMainDataVia().length() <= 0) {
                mTxtVia.setVisibility(View.GONE);
            } else {
                mTxtVia.setVisibility(View.VISIBLE);
            }
        }

        mTxtLastMessageDate.setText("");
        mTxtUnreadMessages.setVisibility(GONE);

        launchLoaderTask();
    }

    public ContactRaw getContactRaw() {
        return mContactRaw;
    }

    public void setContactRaw(ContactRaw mContactRaw) {
        setChat(null);
        synchronized (this) {
            this.mContactRaw = mContactRaw;
        }
        refreshContactRawData();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        ChatManager.getInstance(getContext()).registerDataListener(mChatDataObjectListener);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        ChatManager.getInstance(getContext()).unregisterDataListener(mChatDataObjectListener);
        for(ExtraDataLoaderTask task : mTaskSet) {
            if (task != null) {
                task.cancel(true);
            }
        }
    }

    private void launchLoaderTask() {
        if(mTaskSet.size() >= 2) {
            return;
        }
        final ExtraDataLoaderTask extraDataLoaderTask = new ExtraDataLoaderTask();
        mTaskSet.add(extraDataLoaderTask);
        extraDataLoaderTask.execute();
    }

    private Object mChatDataObjectListener = new Object() {
        public void onEventMainThread(DataObjectEvent<Chat> chatDataObjectEvent) {
            final List<Recipient> recipientList = chatDataObjectEvent.getDataObject().getRecipientList();
            if (mContactRaw != null && recipientList != null && recipientList.size() > 0 && recipientList.get(0).getDroidContactId() == mContactRaw.getContactId()) {
                launchLoaderTask();
            }
        }
    };
}
