package com.peppermint.app.sending.api;

import android.content.Context;
import android.os.Parcel;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.Draft;
import com.google.api.services.gmail.model.Message;
import com.peppermint.app.R;
import com.peppermint.app.rest.HttpRequest;
import com.peppermint.app.rest.HttpResponse;
import com.peppermint.app.sending.api.exceptions.GoogleApiInvalidAccessTokenException;
import com.peppermint.app.sending.mail.gmail.GmailAttachmentRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Created by Nuno Luz on 22-01-2016.
 */
public class GoogleApi implements Serializable {

    private static final String TAG = GoogleApi.class.getSimpleName();
    protected static final String[] SCOPES = { GmailScopes.GMAIL_COMPOSE, GmailScopes.GMAIL_MODIFY, "https://www.googleapis.com/auth/userinfo.profile" };

    public class DraftResponse extends HttpResponse {
        public DraftResponse() {
            super();
        }
        @Override
        public void readBody(InputStream inStream, HttpRequest request) throws Throwable {
            super.readBody(inStream, request);

            JSONObject jsonResponse = new JSONObject(String.valueOf(getBody()));
            if(!jsonResponse.isNull("error")) {
                throw new JSONException("Google API Error for " + getBody());
            }

            JSONObject jsonMessage = jsonResponse.getJSONObject("message");
            Message responseMessage = new Message();
            responseMessage.setId(jsonMessage.getString("id"));
            responseMessage.setThreadId(jsonMessage.getString("threadId"));
            JSONArray jsonLabels = jsonMessage.getJSONArray("labelIds");
            List<String> labels = new ArrayList<>();
            for(int i=0; i<jsonLabels.length(); i++) {
                labels.add(jsonLabels.getString(i));
            }
            responseMessage.setLabelIds(labels);
            Draft responseDraft = new Draft();
            responseDraft.setId(jsonResponse.getString("id"));
            responseDraft.setMessage(responseMessage);

            setBody(responseDraft);
        }
        public final Creator<DraftResponse> CREATOR = new Creator<DraftResponse>() {
            public DraftResponse createFromParcel(Parcel in) {
                return new DraftResponse(in);
            }
            public DraftResponse[] newArray(int size) {
                return new DraftResponse[size];
            }
        };
        protected DraftResponse(Parcel in) {
            super(in);
        }
    }

    public class UserInfoResponse extends HttpResponse {
        private String mFirstName, mLastName, mFullName;
        public UserInfoResponse() {
            super();
        }
        @Override
        public void readBody(InputStream inStream, HttpRequest request) throws Throwable {
            super.readBody(inStream, request);

            JSONObject json = new JSONObject(String.valueOf(getBody()));
            if (!json.isNull("given_name") && !json.isNull("family_name")) {
                mFirstName = json.getString("given_name");
                mLastName = json.getString("family_name");
            }

            if (!json.isNull("name")) {
                mFullName = json.getString("name");
            }
        }
        public String getFirstName() {
            return mFirstName;
        }
        public String getLastName() {
            return mLastName;
        }
        public String getFullName() {
            return mFullName;
        }
        public final Creator<UserInfoResponse> CREATOR = new Creator<UserInfoResponse>() {
            public UserInfoResponse createFromParcel(Parcel in) {
                return new UserInfoResponse(in);
            }
            public UserInfoResponse[] newArray(int size) {
                return new UserInfoResponse[size];
            }
        };
        protected UserInfoResponse(Parcel in) {
            super(in);
        }
    }

    private final HttpTransport mTransport = AndroidHttp.newCompatibleTransport();
    private final JsonFactory mJsonFactory = GsonFactory.getDefaultInstance();

    private Context mContext;
    private String mAccountName, mDisplayName;

    private GoogleAccountCredential mCredential;
    private Gmail mService;
    private String mAccessToken;

    public GoogleApi(Context context) {
        this.mContext = context;
    }

    public synchronized void refreshAccessToken() throws Throwable {
        String token = mCredential.getToken();
        GoogleAuthUtil.clearToken(mContext, token);
        this.mAccessToken = null;
    }

    public String getAccountName() {
        return mAccountName;
    }

    public synchronized void setAccountName(String accountName) {
        this.mAccountName = accountName;

        this.mCredential = GoogleAccountCredential.usingOAuth2(
                mContext, Arrays.asList(SCOPES))
                .setBackOff(new ExponentialBackOff())
                .setSelectedAccountName(mAccountName);

        this.mService = new Gmail.Builder(
                mTransport, mJsonFactory, mCredential)
                .setApplicationName(mContext.getString(R.string.app_name))
                .build();

        // force refresh access token
        this.mAccessToken = null;
    }

    public synchronized UserInfoResponse getUserInfo() throws IOException, GoogleAuthException, GoogleApiInvalidAccessTokenException {
        if(mAccessToken == null) {
            mAccessToken = mCredential.getToken();
        }

        HttpRequest request = new HttpRequest("https://www.googleapis.com/oauth2/v1/userinfo", HttpRequest.METHOD_GET, false);
        request.setHeaderParam("Authorization", "Bearer " + mAccessToken);
        request.setUrlParam("alt", "json");

        UserInfoResponse response = new UserInfoResponse();
        request.execute(response);

        return response;
    }

    public synchronized DraftResponse createGmailDraft(String subject, String body, String destEmail, String contentType, Date emailDate, File attachment) throws IOException, GoogleAuthException, GoogleApiInvalidAccessTokenException {
        if(mAccessToken == null) {
            mAccessToken = mCredential.getToken();
        }

        // custom, performance optimized code to create the draft
        GmailAttachmentRequest request = new GmailAttachmentRequest(attachment, mAccountName, mDisplayName, destEmail,
                subject, body, contentType, emailDate);
        request.setHeaderParam("Authorization", "Bearer " + mAccessToken);
        request.setHeaderParam("Content-Type", "application/json; charset=UTF-8");
        // perform request to the Gmail API endpoint
        DraftResponse response = new DraftResponse();
        request.execute(response);

        if(response.getCode() == 401) {
            throw new GoogleApiInvalidAccessTokenException(request.getBody());
        }

        return response;
    }

    public synchronized void sendGmailDraft(Draft draft) throws IOException {
        mService.users().drafts().send("me", draft).execute();
    }

    public synchronized void deleteGmailDraft(Draft draft) throws IOException {
        mService.users().drafts().delete("me", draft.getId()).execute();
    }

    public String getDisplayName() {
        return mDisplayName;
    }

    public void setDisplayName(String mDisplayName) {
        this.mDisplayName = mDisplayName;
    }

    public GoogleAccountCredential getCredential() {
        return mCredential;
    }

    public Gmail getService() {
        return mService;
    }
}
