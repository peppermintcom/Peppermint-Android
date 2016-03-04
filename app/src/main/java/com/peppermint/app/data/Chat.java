package com.peppermint.app.data;

import com.peppermint.app.utils.DateContainer;

import java.io.Serializable;

/**
 * Created by Nuno Luz on 01-10-2015.
 *
 * Represents a chat/conversation.
 */
public class Chat implements Serializable {

    private long mId;

    private long mMainRecipientId;
    private String mLastMessageTimestamp = DateContainer.getCurrentUTCTimestamp();

    private Recipient mMainRecipientParameter;
    private int mAmountUnopened;

    public Chat() {
    }

    public Chat(long mainRecipientId) {
        this.mMainRecipientId = mainRecipientId;
    }

    public Chat(long mainRecipientId, String lastMessageTimestamp) {
        this(mainRecipientId);
        this.mLastMessageTimestamp = lastMessageTimestamp;
    }

    public long getId() {
        return mId;
    }

    public void setId(long mId) {
        this.mId = mId;
    }

    public long getMainRecipientId() {
        return mMainRecipientId;
    }

    public void setMainRecipientId(long mMainRecipientId) {
        this.mMainRecipientId = mMainRecipientId;
    }

    public String getLastMessageTimestamp() {
        return mLastMessageTimestamp;
    }

    public void setLastMessageTimestamp(String mLastMessageTimestamp) {
        this.mLastMessageTimestamp = mLastMessageTimestamp;
    }

    public Recipient getMainRecipientParameter() {
        return mMainRecipientParameter;
    }

    public void setMainRecipientParameter(Recipient mMainRecipientParameter) {
        this.mMainRecipientParameter = mMainRecipientParameter;
    }

    public int getAmountUnopened() {
        return mAmountUnopened;
    }

    public void setAmountUnopened(int mAmountUnopened) {
        this.mAmountUnopened = mAmountUnopened;
    }

    @Override
    public String toString() {
        return "Chat{" +
                "mId=" + mId +
                ", mAmountUnopened=" + mAmountUnopened +
                ", mMainRecipientId=" + mMainRecipientId +
                ", mLastMessageTimestamp=" + mLastMessageTimestamp +
                '}';
    }
}
