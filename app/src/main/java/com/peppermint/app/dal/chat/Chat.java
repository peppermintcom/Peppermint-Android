package com.peppermint.app.dal.chat;

import com.peppermint.app.dal.DataObject;
import com.peppermint.app.dal.recipient.Recipient;
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
public class Chat extends DataObject implements Serializable {

    public static final int FIELD_TITLE = 1;
    public static final int FIELD_PEPPERMINT_CHAT_ID = 2;
    public static final int FIELD_LAST_MESSAGE_TIMESTAMP = 3;
    public static final int FIELD_AMOUNT_UNOPENED = 4;
    public static final int FIELD_RECIPIENTS = 5;

    private long mId;
    private String mTitle;
    private String mLastMessageTimestamp = DateContainer.getCurrentUTCTimestamp();

    private List<Recipient> mRecipientList = new ArrayList<>();
    private long mPeppermintChatId = 0;
    private boolean mPeppermint = false;

    private int mAmountUnopened;

    public Chat() {
        this.mRecipientList = new ArrayList<>();
    }

    public Chat(long mId, String mTitle, String mLastMessageTimestamp, Recipient... recipients) {
        this();
        this.mId = mId;
        setTitle(mTitle);
        setLastMessageTimestamp(mLastMessageTimestamp);
        Collections.addAll(this.mRecipientList, recipients);
        setRecipientList(mRecipientList);
    }

    public Chat(long mId, String mTitle, String mLastMessageTimestamp, List<Recipient> mRecipientList) {
        this.mId = mId;
        setTitle(mTitle);
        setLastMessageTimestamp(mLastMessageTimestamp);
        setRecipientList(mRecipientList);
    }

    public Chat(List<Recipient> mRecipientList, String mLastMessageTimestamp) {
        setRecipientList(mRecipientList);
        setTitle(getRecipientListDisplayNames());
        setLastMessageTimestamp(mLastMessageTimestamp);
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
        String oldValue = this.mLastMessageTimestamp;
        this.mLastMessageTimestamp = mLastMessageTimestamp;
        registerUpdate(FIELD_LAST_MESSAGE_TIMESTAMP, oldValue, mLastMessageTimestamp);
    }

    public int getAmountUnopened() {
        return mAmountUnopened;
    }

    public void setAmountUnopened(int mAmountUnopened) {
        int oldValue = this.mAmountUnopened;
        this.mAmountUnopened = mAmountUnopened;
        registerUpdate(FIELD_AMOUNT_UNOPENED, oldValue, mAmountUnopened);
    }

    public List<Recipient> getRecipientList() {
        return mRecipientList;
    }

    public void setRecipientList(List<Recipient> mRecipientList) {
        List<Recipient> oldValue = new ArrayList<>(this.mRecipientList);
        this.mRecipientList = mRecipientList;
        registerUpdate(FIELD_RECIPIENTS, oldValue, mRecipientList);
    }

    public String getTitle() {
        return mTitle;
    }

    public void setTitle(String mTitle) {
        String oldValue = this.mTitle;
        this.mTitle = mTitle;
        registerUpdate(FIELD_TITLE, oldValue, mTitle);
    }

    public long getPeppermintChatId() {
        return mPeppermintChatId;
    }

    public void setPeppermintChatId(long mPeppermintChatId) {
        long oldValue = this.mPeppermintChatId;
        this.mPeppermintChatId = mPeppermintChatId;
        registerUpdate(FIELD_PEPPERMINT_CHAT_ID, oldValue, mPeppermintChatId);
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

    public boolean isPeppermint() {
        return mPeppermint;
    }

    public void setPeppermint(boolean mPeppermint) {
        this.mPeppermint = mPeppermint;
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
                ", mPeppermint=" + mPeppermint +
                ", mPeppermintChatId=" + mPeppermintChatId +
                ", mTitle='" + mTitle + '\'' +
                ", mAmountUnopened=" + mAmountUnopened +
                ", mLastMessageTimestamp='" + mLastMessageTimestamp + '\'' +
                ", mRecipientList=" + mRecipientList +
                ", " + super.toString() +
                '}';
    }
}
