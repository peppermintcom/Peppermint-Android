package com.peppermint.app.data;

import com.peppermint.app.utils.DateContainer;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Created by Nuno Luz on 01-10-2015.
 *
 * Represents an audio message from/to a particular recipient.
 */
public class Message implements Serializable {

    public static final String PARAM_INSERTED = "paramInserted";

    private static final String PARAM_CHAT = "paramChat";
    private static final String PARAM_RECORDING = "paramRecording";

    private UUID mUUID = UUID.randomUUID();
    private long mId;
    private long mChatId, mRecordingId, mAuthorId;

    private String mEmailSubject;
    private String mEmailBody;

    private String mRegistrationTimestamp = DateContainer.getCurrentUTCTimestamp();
    private boolean mSent = false;
    private boolean mReceived = false;
    private boolean mPlayed = false;

    private String mServerId;
    private String mServerCanonicalUrl, mServerShortUrl, mTranscription;

    private List<Long> mConfirmedSentRecipientIds = new ArrayList<>();
    private List<Long> mRecipientIds = new ArrayList<>();

    // extra parameters about the message that can be stored by/feed to senders
    private Map<String, Object> mParameters = new HashMap<>();

    public Message() {
    }

    public Message(Message message) {
        setId(message.mId);
        mChatId = message.mChatId;
        mRecordingId = message.mRecordingId;
        mAuthorId = message.mAuthorId;
        mEmailBody = message.mEmailBody;
        mEmailSubject = message.mEmailSubject;
        mRegistrationTimestamp = message.mRegistrationTimestamp;
        mSent = message.mSent;
        mReceived = message.mReceived;
        mPlayed = message.mPlayed;
        mServerId = message.mServerId;
        mServerCanonicalUrl = message.mServerCanonicalUrl;
        mServerShortUrl = message.mServerShortUrl;
        mTranscription = message.mTranscription;
        mParameters.putAll(message.mParameters);
        mConfirmedSentRecipientIds.addAll(message.mConfirmedSentRecipientIds);
    }

    public Message(long mRecordingId, long mChatId) {
        this.mChatId = mChatId;
        this.mRecordingId = mRecordingId;
    }

    public Message(long mRecordingId, long mChatId, String mEmailSubject, String mEmailBody) {
        this.mChatId = mChatId;
        this.mRecordingId = mRecordingId;
        this.mEmailSubject = mEmailSubject;
        this.mEmailBody = mEmailBody;
    }

    public Message(long mId, long mChatId, long mRecordingId, long mAuthorId, String mEmailSubject, String mEmailBody, String mRegistrationTimestamp, boolean mReceived, boolean mSent, boolean mPlayed, String mServerId, String mServerCanonicalUrl, String mServerShortUrl, String mTranscription) {
        setId(mId);
        this.mChatId = mChatId;
        this.mRecordingId = mRecordingId;
        this.mAuthorId = mAuthorId;
        this.mEmailSubject = mEmailSubject;
        this.mEmailBody = mEmailBody;
        this.mRegistrationTimestamp = mRegistrationTimestamp;
        this.mReceived = mReceived;
        this.mSent = mSent;
        this.mPlayed = mPlayed;
        this.mServerId = mServerId;
        this.mServerCanonicalUrl = mServerCanonicalUrl;
        this.mServerShortUrl = mServerShortUrl;
        this.mTranscription = mTranscription;
    }

    public boolean isPlayed() {
        return mPlayed;
    }

    public long getChatId() {
        return mChatId;
    }

    public void setChatId(long mChatId) {
        this.mChatId = mChatId;
    }

    public long getAuthorId() {
        return mAuthorId;
    }

    public void setAuthorId(long mAuthorId) {
        this.mAuthorId = mAuthorId;
    }

    public long getRecordingId() {
        return mRecordingId;
    }

    public void setRecordingId(long mRecordingId) {
        this.mRecordingId = mRecordingId;
    }

    public void setPlayed(boolean mPlayed) {
        this.mPlayed = mPlayed;
    }

    public boolean isReceived() {
        return mReceived;
    }

    public void setReceived(boolean mReceived) {
        this.mReceived = mReceived;
    }

    public String getEmailSubject() {
        return mEmailSubject;
    }

    public void setEmailSubject(String mEmailSubject) {
        this.mEmailSubject = mEmailSubject;
    }

    public String getEmailBody() {
        return mEmailBody;
    }

    public void setEmailBody(String mEmailBody) {
        this.mEmailBody = mEmailBody;
    }

    public long getId() {
        return mId;
    }

    public void setId(long mId) {
        this.mId = mId;
        this.mUUID = new UUID(mId, mId);
    }

    public String getRegistrationTimestamp() {
        return mRegistrationTimestamp;
    }

    public void setRegistrationTimestamp(String mRegistrationTimestamp) {
        this.mRegistrationTimestamp = mRegistrationTimestamp;
    }

    public boolean isSent() {
        return mSent;
    }

    public void setSent(boolean mSent) {
        this.mSent = mSent;
    }

    public String getTranscription() {
        return mTranscription;
    }

    public void setTranscription(String mTranscription) {
        this.mTranscription = mTranscription;
    }

    public String getServerId() {
        return mServerId;
    }

    public void setServerId(String mServerId) {
        this.mServerId = mServerId;
    }

    public String getServerShortUrl() {
        return mServerShortUrl;
    }

    public void setServerShortUrl(String mServerShortUrl) {
        this.mServerShortUrl = mServerShortUrl;
    }

    public String getServerCanonicalUrl() {
        return mServerCanonicalUrl;
    }

    public void setServerCanonicalUrl(String mServerCanonicalUrl) {
        this.mServerCanonicalUrl = mServerCanonicalUrl;
    }

    public void addConfirmedSentRecipientId(long chatRecipientId) {
        mConfirmedSentRecipientIds.add(chatRecipientId);
    }

    public boolean removeConfirmedSentRecipientId(long chatRecipientId) {
        return mConfirmedSentRecipientIds.remove(chatRecipientId);
    }

    public List<Long> getConfirmedSentRecipientIds() {
        return mConfirmedSentRecipientIds;
    }

    public void addRecipientId(long chatRecipientId) {
        mRecipientIds.add(chatRecipientId);
    }

    public boolean removeRecipientId(long chatRecipientId) {
        return mRecipientIds.remove(chatRecipientId);
    }

    public List<Long> getRecipientIds() {
        return mRecipientIds;
    }

    public void setConfirmedSentRecipientIds(List<Long> mConfirmedSentRecipientIds) {
        this.mConfirmedSentRecipientIds = mConfirmedSentRecipientIds;
    }

    public void setRecipientIds(List<Long> mRecipientIds) {
        this.mRecipientIds = mRecipientIds;
    }

    public UUID getUUID() {
        return mUUID;
    }

    // PARAMETERS
    public Map<String, Object> getParameters() {
        return mParameters;
    }

    public Message setParameters(Map<String, Object> mParameters) {
        this.mParameters = mParameters;
        return this;
    }

    public Object getParameter(String key) {
        if(!mParameters.containsKey(key)) {
            return null;
        }
        return mParameters.get(key);
    }

    public Message setParameter(String key, Object value) {
        mParameters.put(key, value);
        return this;
    }

    public Object removeParameter(String key) {
        return mParameters.remove(key);
    }

    public Chat getChatParameter() {
        return (Chat) mParameters.get(PARAM_CHAT);
    }

    public void setChatParameter(Chat chat) {
        mParameters.put(PARAM_CHAT, chat);
    }

    public Recording getRecordingParameter() {
        return (Recording) mParameters.get(PARAM_RECORDING);
    }

    public void setRecordingParameter(Recording recording) {
        mParameters.put(PARAM_RECORDING, recording);
    }

    @Override
    public boolean equals(Object o) {
        if(o instanceof Message) {
            return ((Message) o).getUUID().equals(mUUID);
        }
        return super.equals(o);
    }

    @Override
    public String toString() {
        return "Message{" +
                "mUUID=" + mUUID +
                ", mId=" + mId +
                ", mChatId=" + mChatId +
                ", mRecordingId=" + mRecordingId +
                ", mAuthorId=" + mAuthorId +
                ", mEmailSubject='" + mEmailSubject + '\'' +
                ", mEmailBody='" + mEmailBody + '\'' +
                ", mRegistrationTimestamp='" + mRegistrationTimestamp + '\'' +
                ", mSent=" + mSent +
                ", mReceived=" + mReceived +
                ", mPlayed=" + mPlayed +
                ", mServerId='" + mServerId + '\'' +
                ", mServerCanonicalUrl='" + mServerCanonicalUrl + '\'' +
                ", mServerShortUrl='" + mServerShortUrl + '\'' +
                ", mTranscription='" + mTranscription + '\'' +
                ", mParameters=" + mParameters +
                '}';
    }
}
