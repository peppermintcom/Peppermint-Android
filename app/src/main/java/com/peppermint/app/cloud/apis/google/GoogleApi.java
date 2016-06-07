package com.peppermint.app.cloud.apis.google;

import android.content.Context;
import android.os.Parcel;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.util.ExponentialBackOff;
import com.peppermint.app.cloud.apis.BaseApi;
import com.peppermint.app.cloud.apis.google.GoogleApiInvalidAccessTokenException;
import com.peppermint.app.cloud.apis.google.GoogleApiNoAuthorizationException;
import com.peppermint.app.cloud.apis.google.GoogleApiResponseException;
import com.peppermint.app.cloud.rest.HttpRequest;
import com.peppermint.app.cloud.rest.HttpResponse;
import com.peppermint.app.services.messenger.handlers.gmail.GmailAttachmentRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Date;

/**
 * Created by Nuno Luz on 22-01-2016.
 * <p>
 *      Google API operations wrapper.
 * </p>
 */
public class GoogleApi extends BaseApi implements Serializable {

    protected static final String[] SCOPES = {
            "https://www.googleapis.com/auth/gmail.compose", "https://www.googleapis.com/auth/gmail.modify",
            "https://www.googleapis.com/auth/userinfo.profile",
            "https://www.googleapis.com/auth/plus.profile.emails.read" };

    private static final String SEND_DRAFT_ENDPOINT = "https://www.googleapis.com/gmail/v1/users/me/drafts/send";
    private static final String DELETE_DRAFT_ENDPOINT = "https://www.googleapis.com/gmail/v1/users/me/drafts/{draftId}";

    // HttpResponse that parses Google endpoint JSON and returns a {@link Draft} instance
    public class DraftResponse extends HttpResponse {
        public DraftResponse() {
            super();
        }
        @Override
        public void readBody(InputStream inStream, HttpRequest request) throws Throwable {
            super.readBody(inStream, request);

            JSONObject jsonResponse = new JSONObject(String.valueOf(getBody()));
            if(!jsonResponse.isNull("error")) {
                JSONObject jsonError = jsonResponse.getJSONObject("error");
                if(!jsonError.isNull("code") && jsonError.getInt("code") == 401) {
                    throw new GoogleApiInvalidAccessTokenException(String.valueOf(getBody()));
                }
                throw new JSONException("Google API Error for " + getBody());
            }

            setBody(jsonResponse.getString("id"));
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

    // HttpResponse that parses Google endpoint JSON and returns user info data (name)
    public class UserInfoResponse extends HttpResponse {
        private String mFirstName, mLastName, mFullName;
        public UserInfoResponse() {
            super();
        }
        @Override
        public void readBody(InputStream inStream, HttpRequest request) throws Throwable {
            super.readBody(inStream, request);

            JSONObject json = new JSONObject(String.valueOf(getBody()));

            if(!json.isNull("name")) {
                JSONObject nameJson = json.getJSONObject("name");
                if (!nameJson.isNull("givenName") && !nameJson.isNull("familyName")) {
                    mFirstName = nameJson.getString("givenName");
                    mLastName = nameJson.getString("familyName");
                }
            }

            if (!json.isNull("displayName")) {
                mFullName = json.getString("displayName");
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

    public class SendDraftResponse extends HttpResponse {
        public SendDraftResponse() {
            super();
        }
        @Override
        public void readBody(InputStream inStream, HttpRequest request) throws Throwable {
            super.readBody(inStream, request);

            JSONObject jsonResponse = new JSONObject(String.valueOf(getBody()));
            if(!jsonResponse.isNull("error")) {
                JSONObject jsonError = jsonResponse.getJSONObject("error");
                if(!jsonError.isNull("code") && jsonError.getInt("code") == 401) {
                    throw new GoogleApiInvalidAccessTokenException(String.valueOf(getBody()));
                }
                throw new JSONException("Google API Error for " + getBody());
            }
        }
        public final Creator<SendDraftResponse> CREATOR = new Creator<SendDraftResponse>() {
            public SendDraftResponse createFromParcel(Parcel in) {
                return new SendDraftResponse(in);
            }
            public SendDraftResponse[] newArray(int size) {
                return new SendDraftResponse[size];
            }
        };
        protected SendDraftResponse(Parcel in) {
            super(in);
        }
    }

    private String mAccountName, mDisplayName;

    private GoogleAccountCredential mCredential;

    public GoogleApi(final Context mContext) {
        super(mContext);
    }

    /**
     * Refreshes the Google API access token.
     *
     * @return a new access token
     * @throws GoogleApiNoAuthorizationException if authorization for one of the scopes fails
     * @throws GoogleAuthException if credentials are invalid
     * @throws IOException
     */
    @Override
    public synchronized String renewAuthenticationToken() throws GoogleApiNoAuthorizationException, GoogleAuthException, IOException {
        try {
            if(this.mAuthToken == null) {
                this.mAuthToken = this.mCredential.getToken();
            }
            GoogleAuthUtil.clearToken(this.mContext, this.mAuthToken);
            this.mAuthToken = this.mCredential.getToken();
        } catch (UserRecoverableAuthException e) {
            throw new GoogleApiNoAuthorizationException(e);
        }

        return this.mAuthToken;
    }

    @Override
    protected boolean isAuthenticationTokenRenewalRequired(final HttpResponse response) {
        return response.getCode() == 401 || (response.getException() != null &&
                response.getException() instanceof GoogleJsonResponseException &&
                ((GoogleJsonResponseException) response.getException()).getDetails().getCode() == 401);
    }

    public String getAccountName() {
        return mAccountName;
    }

    /**
     * Sets the account name/email address and refreshes Google API credentials.
     * @param accountName the new account name
     */
    public synchronized void setAccountName(final String accountName) {
        this.mCredential = null;
        this.mAccountName = accountName;

        if(accountName != null) {
            this.mCredential = GoogleAccountCredential.usingOAuth2(
                    mContext, Arrays.asList(SCOPES))
                    .setBackOff(new ExponentialBackOff())
                    .setSelectedAccountName(mAccountName);
        }

        // force refresh access token on next iteration
        this.mAuthToken = null;
    }

    @Override
    protected void processGenericExceptions(final HttpRequest request, final HttpResponse response) throws Exception {
        super.processGenericExceptions(request, response);

        if(isAuthenticationTokenRenewalRequired(response)) {
            throw new GoogleApiInvalidAccessTokenException(request.toString());
        }

        if(response.getCode() / 100 != 2) {
            throw new GoogleApiResponseException(response.getCode(), request.toString());
        }
    }

    /**
     * Gets user info from the Google API (name)
     *
     * @return the response containing the user info
     * @throws GoogleApiNoAuthorizationException if authorization for one of the scopes fails
     * @throws GoogleApiInvalidAccessTokenException if credentials are invalid
     * @throws GoogleAuthException
     * @throws IOException
     */
    public synchronized UserInfoResponse getUserInfo(final String requesterId) throws Exception {
        HttpRequest request = new HttpRequest("https://www.googleapis.com/plus/v1/people/me", HttpRequest.METHOD_GET, false);

        UserInfoResponse response = executeRequest(requesterId, request, new UserInfoResponse(), true);
        processGenericExceptions(request, response);
        return response;
    }

    /**
     * Creates a Gmail draft on the Google account.
     *
     * @param subject the email subject
     * @param bodyPlain the email body
     * @param destEmails the destination email addresses
     * @param contentType the content type of the attachment
     * @param emailDate the email date
     * @param attachment the attachment
     * @return the Google API response
     * @throws GoogleApiNoAuthorizationException if authorization for one of the scopes fails
     * @throws GoogleApiInvalidAccessTokenException if credentials are invalid
     * @throws GoogleAuthException
     * @throws IOException
     */
    public synchronized DraftResponse createGmailDraft(String requesterId, String subject, String bodyPlain, String bodyHtml,
                                                       String[] destEmails, String contentType, Date emailDate, File attachment) throws Exception {
        // custom, performance optimized code to create the draft
        GmailAttachmentRequest request = new GmailAttachmentRequest(attachment, mAccountName, mDisplayName,
                subject, bodyPlain, bodyHtml, contentType, emailDate, destEmails);
        request.setHeaderParam("Content-Type", "application/json; charset=UTF-8");

        // perform request to the Gmail API endpoint
        DraftResponse response = executeRequest(requesterId, request, new DraftResponse(), true);
        processGenericExceptions(request, response);
        return response;
    }

    /**
     * Sends a previously created Gmail draft.
     * @param requesterId the requester Id
     * @param draftId the Draft instance
     * @throws GoogleApiNoAuthorizationException if authorization for one of the scopes fails
     * @throws GoogleApiInvalidAccessTokenException if credentials are invalid
     * @throws IOException
     */
    public synchronized void sendGmailDraft(String requesterId, String draftId) throws Exception {
        HttpRequest request = new HttpRequest(SEND_DRAFT_ENDPOINT, HttpRequest.METHOD_POST, false);
        request.setHeaderParam("Content-Type", "application/json; charset=UTF-8");
        request.setBody("{\"id\":\"" + draftId + "\"}");

        SendDraftResponse response = executeRequest(requesterId, request, new SendDraftResponse(), true);
        processGenericExceptions(request, response);
    }

    /**
     * Deletes a previously created Gmail draft.
     * @param requesterId the requester Id
     * @param draftId the Draft instance
     * @throws GoogleApiNoAuthorizationException if authorization for one of the scopes fails
     * @throws GoogleApiInvalidAccessTokenException if credentials are invalid
     * @throws IOException
     */
    public synchronized void deleteGmailDraft(String requesterId, String draftId) throws Exception {
        HttpRequest request = new HttpRequest(DELETE_DRAFT_ENDPOINT.replace("{draftId}", draftId), HttpRequest.METHOD_DELETE, false);
        request.removeHeaderParam("Content-Type");

        HttpResponse response = executeRequest(requesterId, request, new HttpResponse(), true);
        processGenericExceptions(request, response);
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
}
