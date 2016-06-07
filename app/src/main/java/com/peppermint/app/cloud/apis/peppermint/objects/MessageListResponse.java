package com.peppermint.app.cloud.apis.peppermint.objects;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Nuno Luz on 28-01-2016.
 *
 * Data wrapper for HTTP /messages endpoint responses.
 */
public class MessageListResponse implements Serializable {

    private String mNextUrl;
    private List<MessagesResponse> mMessages;

    public MessageListResponse() {
        mMessages = new ArrayList<>();
    }

    public MessageListResponse(String mNextUrl, List<MessagesResponse> mMessages) {
        this.mNextUrl = mNextUrl;
        this.mMessages = mMessages;
    }

    public String getNextUrl() {
        return mNextUrl;
    }

    public void setNextUrl(String mNextUrl) {
        this.mNextUrl = mNextUrl;
    }

    public List<MessagesResponse> getMessages() {
        return mMessages;
    }

    public void setMessages(List<MessagesResponse> mMessages) {
        this.mMessages = mMessages;
    }

    public void addMessage(MessagesResponse message) {
        mMessages.add(message);
    }

    public boolean removeMessage(MessagesResponse message) {
        return mMessages.remove(message);
    }

    @Override
    public String toString() {
        return "MessageListResponse{" +
                "mNextUrl='" + mNextUrl + '\'' +
                ", mMessages=" + mMessages +
                '}';
    }
}
