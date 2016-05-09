package com.peppermint.app.data;

import com.peppermint.app.utils.DateContainer;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
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

    private List<Recipient> mRecipientList = new ArrayList<>();
    private long mPeppermintChatId = 0;

    private int mAmountUnopened;
    private long mLastReceivedUnplayedId;

    public Chat() {
        this.mRecipientList = new ArrayList<>();
    }

    public Chat(long mId, String mTitle, String mLastMessageTimestamp, Recipient... recipients) {
        this();
        this.mId = mId;
        this.mTitle = mTitle;
        this.mLastMessageTimestamp = mLastMessageTimestamp;
        Collections.addAll(this.mRecipientList, recipients);
    }

    public Chat(long mId, String mTitle, String mLastMessageTimestamp, List<Recipient> mRecipientList) {
        this.mId = mId;
        this.mTitle = mTitle;
        this.mLastMessageTimestamp = mLastMessageTimestamp;
        this.mRecipientList = mRecipientList;
    }

    public Chat(List<Recipient> mRecipientList, String mLastMessageTimestamp) {
        this.mRecipientList = mRecipientList;
        this.mLastMessageTimestamp = mLastMessageTimestamp;
        this.mTitle = getRecipientListDisplayNames();
    }

    /**
     * Get the Ids of all the {@link Recipient} through {@link Recipient#getId()}
     * @return a list of ids
     */
    public List<Long> getRecipientListIds() {
        List<Long> recipientIds = new ArrayList<>();
        for(Recipient chatRecipient : mRecipientList) {
            recipientIds.add(chatRecipient.getId());
        }
        return recipientIds;
    }

    /**
     * Get a single string with the display names of all the {@link Recipient},
     * separated by commas, through {@link Recipient#getDisplayName()}
     * @return the display name string
     */
    public String getRecipientListDisplayNames() {
        if(mRecipientList.size() <= 0) {
            return null;
        }

        StringBuilder builder = new StringBuilder();
        int i=0;
        for(Recipient chatRecipient : mRecipientList) {
            if(i > 0) {
                builder.append(", ");
            }
            builder.append(chatRecipient.getDisplayName() == null ? chatRecipient.getVia() : chatRecipient.getDisplayName());
            i++;
        }

        return builder.toString();
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

    public void addRecipient(Recipient recipient) {
        mRecipientList.add(recipient);
    }

    public boolean removeRecipient(Recipient recipient) {
        return mRecipientList.remove(recipient);
    }

    public List<Recipient> getRecipientList() {
        return mRecipientList;
    }

    public void setRecipientList(List<Recipient> mRecipientList) {
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

    public long getPeppermintChatId() {
        return mPeppermintChatId;
    }

    public void setPeppermintChatId(long mPeppermintChatId) {
        this.mPeppermintChatId = mPeppermintChatId;
    }

    @Override
    public boolean equals(Object o) {
        if(o == null) {
            return false;
        }

        // to allow comparison operations performed by native Java lists
        if(o instanceof Chat) {
            if(((Chat) o).mId > 0 && mId > 0) {
                return ((Chat) o).mId == mId;
            }
        }
        return super.equals(o);
    }

    @Override
    public String toString() {
        return "Chat{" +
                "mId=" + mId +
                ", mPeppermintChatId=" + mPeppermintChatId +
                ", mTitle='" + mTitle + '\'' +
                ", mAmountUnopened=" + mAmountUnopened +
                ", mLastReceivedUnplayedId=" + mLastReceivedUnplayedId +
                ", mLastMessageTimestamp='" + mLastMessageTimestamp + '\'' +
                ", mRecipientList=" + mRecipientList +
                '}';
    }
}
