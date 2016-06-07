package com.peppermint.app.dal.message;

import com.peppermint.app.dal.DataObject;
import com.peppermint.app.dal.chat.Chat;
import com.peppermint.app.dal.recording.Recording;
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
public class Message extends DataObject implements Serializable {

    public static final String PARAM_INSERTED = "paramInserted";

    private static final String PARAM_CHAT = "paramChat";
    private static final String PARAM_RECORDING = "paramRecording";

    public static final int FIELD_CHAT_ID = 1;
    public static final int FIELD_RECORDING_ID = 2;
    public static final int FIELD_AUTHOR_ID = 3;

    public static final int FIELD_EMAIL_SUBJECT = 4;
    public static final int FIELD_EMAIL_BODY = 5;

    public static final int FIELD_REGISTRATION_TIMESTAMP = 6;
    public static final int FIELD_SENT = 7;
    public static final int FIELD_RECEIVED = 8;
    public static final int FIELD_PLAYED = 9;

    public static final int FIELD_SERVER_ID = 10;
    public static final int FIELD_SERVER_CANONICAL_URL = 11;
    public static final int FIELD_SERVER_SHORT_URL = 12;

    public static final int FIELD_RECIPIENT_IDS = 13;
    public static final int FIELD_CONFIRMED_RECIPIENT_IDS = 14;

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
    private String mServerCanonicalUrl, mServerShortUrl;

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

    public Message(long mId, long mChatId, long mRecordingId, long mAuthorId, String mEmailSubject, String mEmailBody, String mRegistrationTimestamp, boolean mReceived, boolean mSent, boolean mPlayed, String mServerId, String mServerCanonicalUrl, String mServerShortUrl) {
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
    }

    public long getChatId() {
        return mChatId;
    }

    public void setChatId(long mChatId) {
        long oldValue = this.mChatId;
        this.mChatId = mChatId;
        registerUpdate(FIELD_CHAT_ID, oldValue, mChatId);
    }

    public long getAuthorId() {
        return mAuthorId;
    }

    public void setAuthorId(long mAuthorId) {
        long oldValue = this.mAuthorId;
        this.mAuthorId = mAuthorId;
        registerUpdate(FIELD_AUTHOR_ID, oldValue, mAuthorId);
    }

    public long getRecordingId() {
        return mRecordingId;
    }

    public void setRecordingId(long mRecordingId) {
        long oldValue = this.mRecordingId;
        this.mRecordingId = mRecordingId;
        registerUpdate(FIELD_RECORDING_ID, oldValue, mRecordingId);
    }

    public boolean isPlayed() {
        return mPlayed;
    }

    public void setPlayed(boolean mPlayed) {
        boolean oldValue = this.mPlayed;
        this.mPlayed = mPlayed;
        registerUpdate(FIELD_PLAYED, oldValue, mPlayed);
    }

    public boolean isReceived() {
        return mReceived;
    }

    public void setReceived(boolean mReceived) {
        boolean oldValue = this.mReceived;
        this.mReceived = mReceived;
        registerUpdate(FIELD_RECEIVED, oldValue, mReceived);
    }

    public String getEmailSubject() {
        return mEmailSubject;
    }

    public void setEmailSubject(String mEmailSubject) {
        String oldValue = this.mEmailSubject;
        this.mEmailSubject = mEmailSubject;
        registerUpdate(FIELD_EMAIL_SUBJECT, oldValue, mEmailSubject);
    }

    public String getEmailBody() {
        return mEmailBody;
    }

    public void setEmailBody(String mEmailBody) {
        String oldValue = this.mEmailBody;
        this.mEmailBody = mEmailBody;
        registerUpdate(FIELD_EMAIL_BODY, oldValue, mEmailBody);
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
        String oldValue = this.mRegistrationTimestamp;
        this.mRegistrationTimestamp = mRegistrationTimestamp;
        registerUpdate(FIELD_REGISTRATION_TIMESTAMP, oldValue, mRegistrationTimestamp);
    }

    public boolean isSent() {
        return mSent;
    }

    public void setSent(boolean mSent) {
        boolean oldValue = this.mSent;
        this.mSent = mSent;
        registerUpdate(FIELD_SENT, oldValue, mSent);
    }

    public String getServerId() {
        return mServerId;
    }

    public void setServerId(String mServerId) {
        String oldValue = this.mServerId;
        this.mServerId = mServerId;
        registerUpdate(FIELD_SERVER_ID, oldValue, mServerId);
    }

    public String getServerShortUrl() {
        return mServerShortUrl;
    }

    public void setServerShortUrl(String mServerShortUrl) {
        String oldValue = this.mServerShortUrl;
        this.mServerShortUrl = mServerShortUrl;
        registerUpdate(FIELD_SERVER_SHORT_URL, oldValue, mServerShortUrl);
    }

    public String getServerCanonicalUrl() {
        return mServerCanonicalUrl;
    }

    public void setServerCanonicalUrl(String mServerCanonicalUrl) {
        String oldValue = this.mServerCanonicalUrl;
        this.mServerCanonicalUrl = mServerCanonicalUrl;
        registerUpdate(FIELD_SERVER_CANONICAL_URL, oldValue, mServerCanonicalUrl);
    }

    public void addConfirmedSentRecipientId(long chatRecipientId) {
        List<Long> oldValue = new ArrayList<>(mConfirmedSentRecipientIds);
        mConfirmedSentRecipientIds.add(chatRecipientId);
        registerUpdate(FIELD_CONFIRMED_RECIPIENT_IDS, oldValue, mConfirmedSentRecipientIds);
    }

    public boolean removeConfirmedSentRecipientId(long chatRecipientId) {
        List<Long> oldValue = new ArrayList<>(mConfirmedSentRecipientIds);
        boolean ret = mConfirmedSentRecipientIds.remove(chatRecipientId);
        registerUpdate(FIELD_CONFIRMED_RECIPIENT_IDS, oldValue, mConfirmedSentRecipientIds);
        return ret;
    }

    public List<Long> getConfirmedSentRecipientIds() {
        return mConfirmedSentRecipientIds;
    }

    public void addRecipientId(long chatRecipientId) {
        List<Long> oldValue = new ArrayList<>(mRecipientIds);
        mRecipientIds.add(chatRecipientId);
        registerUpdate(FIELD_RECIPIENT_IDS, oldValue, mRecipientIds);
    }

    public boolean removeRecipientId(long chatRecipientId) {
        List<Long> oldValue = new ArrayList<>(mRecipientIds);
        boolean ret = mRecipientIds.remove(chatRecipientId);
        registerUpdate(FIELD_RECIPIENT_IDS, oldValue, mRecipientIds);
        return ret;
    }

    public List<Long> getRecipientIds() {
        return mRecipientIds;
    }

    public void setConfirmedSentRecipientIds(List<Long> mConfirmedSentRecipientIds) {
        List<Long> oldValue = this.mConfirmedSentRecipientIds;
        this.mConfirmedSentRecipientIds = mConfirmedSentRecipientIds;
        registerUpdate(FIELD_CONFIRMED_RECIPIENT_IDS, oldValue, mConfirmedSentRecipientIds);
    }

    public void setRecipientIds(List<Long> mRecipientIds) {
        List<Long> oldValue = this.mRecipientIds;
        this.mRecipientIds = mRecipientIds;
        registerUpdate(FIELD_RECIPIENT_IDS, oldValue, mRecipientIds);
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
                ", mParameters=" + mParameters +
                ", " + super.toString() +
                '}';
    }
}
