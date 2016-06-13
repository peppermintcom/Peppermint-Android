package com.peppermint.app.ui.contacts.listall;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
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
import com.peppermint.app.ui.contacts.listrecents.ChatView;
import com.peppermint.app.utils.ResourceUtils;
import com.peppermint.app.utils.SameAsyncTaskExecutor;
import com.peppermint.app.utils.Utils;

import java.util.List;

/**
 * Created by Nuno Luz on 07-06-2016.
 *
 * Represents a Contact in the UI.
 */
public class ContactView extends ChatView {

    /**
     * Delayed load of the avatar and extra chat data from the database.
     * Speeds up {@link #setChat(Chat)} (scrolling in lists).
     */
    private class ExtraDataLoaderExecutor extends SameAsyncTaskExecutor<ContactRaw, Void, Object[]> {
        private int mAvatarWidth, mAvatarHeight;

        public ExtraDataLoaderExecutor(Context mContext) {
            super(mContext);
        }

        @Override
        protected void onPreExecute() {
            mAvatarWidth = mImgAvatar.getMeasuredWidth();
            mAvatarHeight = mImgAvatar.getMeasuredHeight();
        }

        @Override
        protected Object[] doInBackground(SameAsyncTask asyncTask, ContactRaw... params) {
            Chat chat = null;
            BitmapDrawable scaledBitmapDrawable = null;

            synchronized (ContactView.this) {
                if (params[0] == null) {
                    return null;
                }

                if (!asyncTask.isCancelled()) {
                    chat = ChatManager.getInstance(mContext).
                            getMainChatByDroidContactId(mDatabaseHelper.getReadableDatabase(), params[0].getContactId());
                }

                if (chat == null && !asyncTask.isCancelled()) {
                    // for now only a single recipient is supported
                    if (params[0].getPhotoUri() != null) {
                        final Uri uri = Uri.parse(params[0].getPhotoUri());
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

            return new Object[] { params[0], chat, scaledBitmapDrawable };
        }

        @Override
        protected void onPostExecute(Object[] data) {
            final ContactRaw contactRaw = (ContactRaw) data[0];
            final Chat chat = (Chat) data[1];
            final BitmapDrawable scaledBitmapDrawable = (BitmapDrawable) data[2];

            if(contactRaw != null) {
                if (chat != null) {
                    setChat(chat);
                } else {
                    if (scaledBitmapDrawable != null) {
                        mImgAvatar.setStaticDrawable(scaledBitmapDrawable);
                        mImgAvatar.setShowStaticAvatar(true);
                    } else {
                        mImgAvatar.setShowStaticAvatar(false);
                    }
                }
            }
        }
    }

    protected CustomFontTextView mTxtVia;

    protected ContactRaw mContactRaw;

    private ExtraDataLoaderExecutor mExtraDataLoaderExecutor;

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
        mExtraDataLoaderExecutor = new ExtraDataLoaderExecutor(context);
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

        mExtraDataLoaderExecutor.execute(mContactRaw);
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
        mExtraDataLoaderExecutor.cancel(true);
    }

    private Object mChatDataObjectListener = new Object() {
        @SuppressWarnings("unused")
        public void onEventBackgroundThread(DataObjectEvent<Chat> chatDataObjectEvent) {
            final List<Recipient> recipientList = chatDataObjectEvent.getDataObject().getRecipientList();
            if (mContactRaw != null && recipientList != null && recipientList.size() > 0 && recipientList.get(0).getDroidContactId() == mContactRaw.getContactId()) {
                mExtraDataLoaderExecutor.execute(mContactRaw);
            }
        }
    };
}
