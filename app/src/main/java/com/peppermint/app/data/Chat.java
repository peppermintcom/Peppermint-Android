package com.peppermint.app.data;

import com.peppermint.app.utils.DateContainer;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Nuno Luz on 01-10-2015.
 *
 * Represents a chat/conversation.
 */
public class Chat implements Serializable {

    private long mId;
    private String mTitle;
    private String mLastMessageTimestamp = DateContainer.getCurrentUTCTimestamp();

    private List<ChatRecipient> mRecipientList = new ArrayList<>();
    private int mAmountUnopened;
    private long mLastReceivedUnplayedId;

    public Chat() {
        this.mRecipientList = new ArrayList<>();
    }

    public Chat(List<ChatRecipient> mRecipientList, String mLastMessageTimestamp) {
        this.mRecipientList = mRecipientList;
        this.mLastMessageTimestamp = mLastMessageTimestamp;
    }

    public long getId() {
        return mId;
    }

    public void setId(long mId) {
        this.mId = mId;
    }

    public String getLastMessageTimestamp() {
        return mLastMessageTimestamp;
    }

    public void setLastMessageTimestamp(String mLastMessageTimestamp) {
        this.mLastMessageTimestamp = mLastMessageTimestamp;
    }

    public int getAmountUnopened() {
        return mAmountUnopened;
    }

    public void setAmountUnopened(int mAmountUnopened) {
        this.mAmountUnopened = mAmountUnopened;
    }

    public void addRecipient(ChatRecipient recipient) {
        mRecipientList.add(recipient);
    }

    public boolean removeRecipient(ChatRecipient recipient) {
        return mRecipientList.remove(recipient);
    }

    public List<ChatRecipient> getRecipientList() {
        return mRecipientList;
    }

    public void setRecipientList(List<ChatRecipient> mRecipientList) {
        this.mRecipientList = mRecipientList;
    }

    public String getTitle() {
        return mTitle;
    }

    public void setTitle(String mTitle) {
        this.mTitle = mTitle;
    }

    public long getLastReceivedUnplayedId() {
        return mLastReceivedUnplayedId;
    }

    public void setLastReceivedUnplayedId(long mLastReceivedUnplayedId) {
        this.mLastReceivedUnplayedId = mLastReceivedUnplayedId;
    }

    @Override
    public String toString() {
        return "Chat{" +
                "mId=" + mId +
                ", mTitle='" + mTitle + '\'' +
                ", mLastMessageTimestamp='" + mLastMessageTimestamp + '\'' +
                ", mRecipientList=" + mRecipientList +
                ", mAmountUnopened=" + mAmountUnopened +
                ", mLastReceivedUnplayedId=" + mLastReceivedUnplayedId +
                '}';
    }
}
