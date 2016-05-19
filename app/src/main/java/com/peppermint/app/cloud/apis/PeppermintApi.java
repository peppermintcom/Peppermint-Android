package com.peppermint.app.cloud.apis;

import android.content.Context;
import android.os.Build;

import com.peppermint.app.PeppermintApp;
import com.peppermint.app.authenticator.AuthenticatorUtils;
import com.peppermint.app.cloud.apis.data.AccountsResponse;
import com.peppermint.app.cloud.apis.data.JWTsResponse;
import com.peppermint.app.cloud.apis.data.MessageListResponse;
import com.peppermint.app.cloud.apis.data.MessagesResponse;
import com.peppermint.app.cloud.apis.data.RecorderResponse;
import com.peppermint.app.cloud.apis.data.UploadsResponse;
import com.peppermint.app.cloud.apis.data.parsers.AccountsResponseParser;
import com.peppermint.app.cloud.apis.data.parsers.JWTsResponseParser;
import com.peppermint.app.cloud.apis.data.parsers.MessageListResponseParser;
import com.peppermint.app.cloud.apis.data.parsers.MessagesResponseParser;
import com.peppermint.app.cloud.apis.data.parsers.RecorderResponseParser;
import com.peppermint.app.cloud.apis.data.parsers.UploadsResponseParser;
import com.peppermint.app.cloud.apis.exceptions.PeppermintApiAlreadyRegisteredException;
import com.peppermint.app.cloud.apis.exceptions.PeppermintApiInvalidAccessTokenException;
import com.peppermint.app.cloud.apis.exceptions.PeppermintApiNoAccountException;
import com.peppermint.app.cloud.apis.exceptions.PeppermintApiRecipientNoAppException;
import com.peppermint.app.cloud.apis.exceptions.PeppermintApiResponseException;
import com.peppermint.app.cloud.apis.exceptions.PeppermintApiTooManyRequestsException;
import com.peppermint.app.cloud.rest.HttpJSONResponse;
import com.peppermint.app.cloud.rest.HttpRequest;
import com.peppermint.app.cloud.rest.HttpRequestFileData;
import com.peppermint.app.cloud.rest.HttpResponse;
import com.peppermint.app.cloud.rest.HttpResponseException;
import com.peppermint.app.tracking.TrackerManager;
import com.peppermint.app.utils.Utils;

import java.io.File;
import java.io.Serializable;
import java.util.UUID;

/**
 * Created by Nuno Luz on 22-01-2016.
 * <p>
 *      Peppermint API operations wrapper.
 * </p>
 */
public class PeppermintApi extends BaseApi implements Serializable {

    // payload content type
    private static final String CONTENT_TYPE_JSON = "application/vnd.api+json";

    // supported account types
    public static final int ACCOUNT_TYPE_REGULAR = 1;
    public static final int ACCOUNT_TYPE_GOOGLE = 2;
    public static final int ACCOUNT_TYPE_FACEBOOK = 3;

    // client-app API key
    private static final String DEBUG_API_KEY = "abc123";
    private static final String API_KEY = "la8H4E6Iw5teA6nelVZWgZWqVJh7kWc6Gn1rj21hsYcTP7y7JAmDDQ";

    // server base URL
    protected static final String BASE_ENDPOINT_URL = "https://qdkkavugcd.execute-api.us-west-2.amazonaws.com/prod/v1/";

    // endpoints
    protected static final String ACCOUNTS_ENDPOINT = BASE_ENDPOINT_URL + "accounts";
    protected static final String ACCOUNTS_REL_ENDPOINT = BASE_ENDPOINT_URL + "accounts/{account_id}/relationships/receivers";

    protected static final String JWTS_ENDPOINT = BASE_ENDPOINT_URL + "jwts";
    protected static final String MESSAGES_ENDPOINT = BASE_ENDPOINT_URL + "messages";
    protected static final String PLAYS_ENDPOINT = BASE_ENDPOINT_URL + "reads";

    protected static final String RECORDER_ENDPOINT = BASE_ENDPOINT_URL + "recorder";
    protected static final String RECORDERS_ENDPOINT = BASE_ENDPOINT_URL + "recorders/{recorder_id}";
    protected static final String UPLOADS_ENDPOINT = BASE_ENDPOINT_URL + "uploads";

    // response parsers
    private final AccountsResponseParser mAccountsResponseParser = new AccountsResponseParser();
    private final RecorderResponseParser mRecorderResponseParser = new RecorderResponseParser();
    private final UploadsResponseParser mUploadsResponseParser = new UploadsResponseParser();
    private final JWTsResponseParser mJWTsResponseParser = new JWTsResponseParser();
    private final MessagesResponseParser mMessagesResponseParser = new MessagesResponseParser();
    private final MessageListResponseParser mMessageListResponseParser = new MessageListResponseParser();

    public PeppermintApi(final Context mContext) {
        super(mContext);
        this.mApiKey = PeppermintApp.DEBUG ? DEBUG_API_KEY : API_KEY;
    }

    @Override
    public String peekAuthenticationToken() throws Exception {
        final AuthenticatorUtils authenticatorUtils = new AuthenticatorUtils(mContext);
        this.mAuthToken = authenticatorUtils.peekAccessToken();
        return this.mAuthToken;
    }

    @Override
    public String renewAuthenticationToken() throws Exception {
        final AuthenticatorUtils authenticatorUtils = new AuthenticatorUtils(mContext);

        if(authenticatorUtils.getAccount() == null) {
            throw new PeppermintApiNoAccountException();
        }

        authenticatorUtils.invalidateAccessToken();
        this.mAuthToken = authenticatorUtils.getAccessToken();

        return this.mAuthToken;
    }

    @Override
    protected boolean isAuthenticationTokenRenewalRequired(final HttpResponse response) {
        return response.getCode() == 401 || response.getCode() == 403;
    }

    @Override
    protected void processGenericExceptions(final HttpRequest request, final HttpResponse response) throws Exception {
        super.processGenericExceptions(request, response);

        if(response.getCode() == 401) {
            throw new PeppermintApiInvalidAccessTokenException(response.getCode(), request.toString());
        }

        if((response.getCode() / 100) != 2) {
            throw new PeppermintApiResponseException(response.getCode(), request.toString());
        }
    }

    // MESSAGES
    public HttpResponse markAsPlayedMessage(final String requesterId, final String serverMessageId) throws Exception {
        final HttpRequest request = new HttpRequest(PLAYS_ENDPOINT, HttpRequest.METHOD_POST);
        request.setHeaderParam("Content-Type", CONTENT_TYPE_JSON);
        request.setBody("{ \"data\": { \"type\":\"reads\", \"id\":\"" + serverMessageId + "\" } }");

        final HttpResponse response = executeRequest(requesterId, request, new HttpResponse(), true);
        processGenericExceptions(request, response);
        return response;
    }

    public MessageListResponse getMessages(final String requesterId, final String serverAccountId, final String sinceTimestamp, final boolean received) throws Exception {
        final HttpRequest request = new HttpRequest(MESSAGES_ENDPOINT, HttpRequest.METHOD_GET, false);
        request.setUrlParam(received ? "recipient" : "sender", serverAccountId);
        request.setUrlParam("since", sinceTimestamp);

        final HttpJSONResponse<MessageListResponse> response = executeRequest(requesterId, request, new HttpJSONResponse<>(mMessageListResponseParser), true);
        if(response.getCode() == 404) {
            throw new PeppermintApiRecipientNoAppException(response.getCode());
        }
        processGenericExceptions(request, response);

        return response.getJsonBody();
    }

    /**
     * Sends a previously created message to the specified recipient through in-app messaging.
     * @param transcriptionUrl the transcription URL
     * @param audioUrl the audio message URL
     * @param senderEmail the sender email address
     * @param recipientEmail the recipient email address
     * @return the endpoint response
     * @throws HttpResponseException a status code different from 2XX was returned
     * @throws PeppermintApiInvalidAccessTokenException
     * @throws PeppermintApiTooManyRequestsException
     * @throws PeppermintApiRecipientNoAppException if the recipient doesn't have Peppermint installed
     */
    public MessagesResponse sendMessage(final String requesterId, final String transcriptionUrl, final String audioUrl, final String senderEmail, final String recipientEmail, final int durationInSeconds) throws Exception {
        final HttpRequest request = new HttpRequest(MESSAGES_ENDPOINT, HttpRequest.METHOD_POST);
        request.setHeaderParam("Content-Type", CONTENT_TYPE_JSON);
        request.setBody("{ \"data\": { \"attributes\": { \"audio_url\": \"" + audioUrl + "\", \"sender_email\": \"" + senderEmail + "\", \"recipient_email\": \"" + recipientEmail + "\" }, \"type\":\"messages\" } }");

        final HttpJSONResponse<MessagesResponse> response = executeRequest(requesterId, request, new HttpJSONResponse<>(mMessagesResponseParser), true);
        if(response.getCode() == 404) {
            throw new PeppermintApiRecipientNoAppException(response.getCode());
        }
        processGenericExceptions(request, response);

        return response.getJsonBody();
    }

    // JWTS

    /**
     * Authenticates (or registers if not found) both account and recorder device.
     *
     * @param accountEmail the account email
     * @param accountPassword the account password
     * @param accountType the account type
     * @param recorderId the recorder/device id
     * @param recorderKey the recorder/device key/password
     * @param fullName the full name of the user
     * @param trackerManager the tracker manager
     * @return the JWT response
     * @throws PeppermintApiTooManyRequestsException
     * @throws PeppermintApiResponseException a status code different from 2XX was returned
     * @throws PeppermintApiInvalidAccessTokenException
     */
    public JWTsResponse authOrRegister(final String requesterId, final String accountEmail, final String accountPassword, final int accountType,
                                       final String recorderId, final String recorderKey, final String fullName, final TrackerManager trackerManager) throws Exception {
        JWTsResponse jwtsResponse;

        try {
            jwtsResponse = authBoth(requesterId, accountEmail, accountPassword, recorderId, recorderKey, accountType);
            addReceiverRecorder(requesterId, jwtsResponse.getAccount().getAccountId(), jwtsResponse.getRecorder().getRecorderId());
        } catch(PeppermintApiInvalidAccessTokenException e) {
            // register/auth recorder
            try {
                registerRecorder(requesterId, recorderId, recorderKey, Utils.getAndroidVersion() + " - " + Utils.getDeviceName());
            } catch (PeppermintApiAlreadyRegisteredException ex) {
                trackerManager.log("Recorder already registered! Moving on...");
            }

            if(accountType == ACCOUNT_TYPE_REGULAR) {
                // register/auth account
                try {
                    registerAccount(requesterId, accountEmail, accountPassword, fullName, accountType);
                } catch (PeppermintApiAlreadyRegisteredException ex) {
                    trackerManager.log("Account already registered! Moving on...");
                }
            }

            // try again to authenticate; it should work now
            jwtsResponse = authBoth(requesterId, accountEmail, accountPassword, recorderId, recorderKey, accountType);
            // add relationship between account and recorder
            addReceiverRecorder(requesterId, jwtsResponse.getAccount().getAccountId(), jwtsResponse.getRecorder().getRecorderId());
        }

        return jwtsResponse;
    }

    /**
     * Authenticates both account and recorder device.
     *
     * @param accountEmail the account email
     * @param accountPassword the account password
     * @param recorderId the recorder/device id
     * @param recorderKey the recorder/device key/password
     * @param accountType the account type
     * @return the JWT response
     * @throws PeppermintApiResponseException a status code different from 2XX was returned
     * @throws PeppermintApiInvalidAccessTokenException
     * @throws PeppermintApiTooManyRequestsException
     */
    public JWTsResponse authBoth(final String requesterId, final String accountEmail, final String accountPassword,
                                 final String recorderId, final String recorderKey, final int accountType) throws Exception {
        String accountTypeStr = "account";
        switch (accountType) {
            case ACCOUNT_TYPE_GOOGLE:
                accountTypeStr = "google";
                break;

            case ACCOUNT_TYPE_FACEBOOK:
                accountTypeStr = "facebook";
                break;
        }

        final HttpRequest request = new HttpRequest(JWTS_ENDPOINT, HttpRequest.METHOD_POST);
        request.setHeaderParam("Authorization", "Peppermint " + accountTypeStr + "=" + Utils.getBasicAuthenticationToken(accountEmail, accountPassword) + ", recorder=" + Utils.getBasicAuthenticationToken(recorderId, recorderKey));

        final HttpJSONResponse<JWTsResponse> response = executeRequest(requesterId, request, new HttpJSONResponse<>(mJWTsResponseParser), false);
        if(response.getCode() == 404) {
            throw new PeppermintApiInvalidAccessTokenException(response.getCode(), request.toString());
        }
        if(response.getCode() == 429) {
            throw new PeppermintApiTooManyRequestsException(response.getCode(), request.toString());
        }
        processGenericExceptions(request, response);

        final JWTsResponse responseData = response.getJsonBody();
        if(responseData != null) {
            this.mAuthToken = responseData.getAccessToken();
        }
        return responseData;
    }

    // ACCOUNT
    /**
     * Creates a new account.
     *
     * @param email the account email
     * @param password the account password
     * @param fullName the owners full name
     * @param accountType the account type
     * @return the account response
     * @throws PeppermintApiInvalidAccessTokenException
     * @throws PeppermintApiResponseException a status code different from 2XX was returned
     * @throws PeppermintApiAlreadyRegisteredException if the account is already registered
     */
    public AccountsResponse registerAccount(final String requesterId, final String email, final String password, final String fullName, final int accountType) throws Exception {
        final HttpRequest request = new HttpRequest(ACCOUNTS_ENDPOINT, HttpRequest.METHOD_POST);
        request.setBody("{ \"api_key\": \"" + mApiKey + "\", \"u\": { \"email\": \"" + email +
                "\", \"password\": \"" + password + "\", \"full_name\": \"" + fullName + "\" } }");

        final HttpJSONResponse<AccountsResponse> response = executeRequest(requesterId, request, new HttpJSONResponse<>(mAccountsResponseParser), false);
        if(response.getCode() == 409) {
            throw new PeppermintApiAlreadyRegisteredException(response.getCode(), request.toString());
        }
        processGenericExceptions(request, response);

        final AccountsResponse responseData = response.getJsonBody();
        if(responseData != null) {
            this.mAuthToken = responseData.getAccessToken();
        }
        return responseData;
    }

    /**
     * Establishes a relationship between an account and a recorder/device, stating that
     * the specified account is configured on the specified recorder/device.
     *
     * @param accountId the account server id
     * @param recorderId the recorder server id
     * @return the HTTP response
     * @throws PeppermintApiResponseException a status code different from 2XX was returned
     * @throws PeppermintApiInvalidAccessTokenException
     */
    public HttpResponse addReceiverRecorder(final String requesterId, final String accountId, final String recorderId) throws Exception {
        final HttpRequest request = new HttpRequest(ACCOUNTS_REL_ENDPOINT.replace("{account_id}", accountId), HttpRequest.METHOD_POST);
        request.setHeaderParam("Content-Type", CONTENT_TYPE_JSON);
        request.setBody("{ \"data\": [ { \"id\":\"" + recorderId + "\", \"type\":\"recorders\" } ] }");

        final HttpResponse response = executeRequest(requesterId, request, new HttpResponse(), true);
        if(response.getCode() == 403) {
            throw new PeppermintApiInvalidAccessTokenException(response.getCode(), request.toString());
        }
        processGenericExceptions(request, response);

        return response;
    }

    /**
     * Deletes the relationship between an account and a recorder/device.
     *
     * @param accountId the account server id
     * @param recorderId the recorder server id
     * @return the HTTP response
     * @throws PeppermintApiResponseException a status code different from 2XX was returned
     * @throws PeppermintApiInvalidAccessTokenException
     */
    public HttpResponse removeReceiverRecorder(final String requesterId, final String accountId, final String recorderId) throws Exception {
        final HttpRequest request = new HttpRequest(ACCOUNTS_REL_ENDPOINT.replace("{account_id}", accountId) + "/" + recorderId, HttpRequest.METHOD_DELETE);
        // timeout after 10 secs.
        request.setConnectTimeout(10000);
        request.setReadTimeout(10000);

        final HttpResponse response = executeRequest(requesterId, request, new HttpResponse(), true);
        if(response.getCode() == 404) {
            throw new PeppermintApiInvalidAccessTokenException(response.getCode(), request.toString());
        }
        processGenericExceptions(request, response);

        return response;
    }

    // RECORDER
    /**
     * Creates a new recorder/device.
     *
     * @param recorderId the recorder/device id
     * @param recorderKey the recorder/device key/password
     * @param description a description of the recorder/device
     * @return the recorder response
     * @throws PeppermintApiInvalidAccessTokenException
     * @throws PeppermintApiResponseException a status code different from 2XX was returned
     * @throws PeppermintApiAlreadyRegisteredException if the recorder/device is already registered
     */
    public RecorderResponse registerRecorder(final String requesterId, final String recorderId, final String recorderKey, final String description) throws Exception {
        final HttpRequest request = new HttpRequest(RECORDER_ENDPOINT, HttpRequest.METHOD_POST);
        request.setBody("{ \"api_key\": \"" + mApiKey + "\", \"recorder\": { \"description\": \"" + description +
                "\", \"recorder_client_id\": \"" + recorderId + "\", \"recorder_key\": \"" + recorderKey + "\" } }");

        final HttpJSONResponse<RecorderResponse> response = executeRequest(requesterId, request, new HttpJSONResponse<>(mRecorderResponseParser), false);
        if(response.getCode() == 409) {
            throw new PeppermintApiAlreadyRegisteredException(response.getCode(), request.toString());
        }
        processGenericExceptions(request, response);

        final RecorderResponse responseData = response.getJsonBody();
        if(responseData != null) {
            this.mAuthToken = responseData.getAccessToken();
        }
        return responseData;
    }

    /**
     * Updates the recorder/device GCM registration token.
     *
     * @param recorderId the recorder/device id
     * @param gcmRegistrationToken the GCM registration token
     * @return the HTTP response
     * @throws PeppermintApiInvalidAccessTokenException
     * @throws PeppermintApiResponseException a status code different from 2XX was returned
     */
    public HttpResponse updateRecorder(final String requesterId, final String recorderId, final String gcmRegistrationToken) throws Exception {
        final HttpRequest request = new HttpRequest(RECORDERS_ENDPOINT.replace("{recorder_id}", recorderId), HttpRequest.METHOD_PUT);
        request.setHeaderParam("Content-Type", CONTENT_TYPE_JSON);
        request.setBody("{\"data\": { \"type\": \"recorders\", \"id\": \"" + recorderId + "\", \"attributes\": { \"gcm_registration_token\": \"" + gcmRegistrationToken + "\" } } }");

        final HttpResponse response = executeRequest(requesterId, request, new HttpResponse(), true);
        processGenericExceptions(request, response);

        return response;
    }

    // UPLOADS

    /**
     * Gets a signed Amazon URL to upload the audio message.
     *
     * @param senderName the sender's name
     * @param senderEmail the sender's email address
     * @param contentType the content type of the file to upload to the signed URL
     * @return the uploads endpoint response
     * @throws PeppermintApiInvalidAccessTokenException
     * @throws PeppermintApiResponseException a status code different from 2XX was returned
     */
    public UploadsResponse getSignedUrl(final String requesterId, final String senderName, final String senderEmail, final String contentType) throws Exception {
        final HttpRequest request = new HttpRequest(UPLOADS_ENDPOINT, HttpRequest.METHOD_POST);
        request.setBody("{\"content_type\":\"" + contentType + "\", \"sender_name\":\"" + senderName + "\", \"sender_email\":\"" + senderEmail + "\"}");

        final HttpJSONResponse<UploadsResponse> response = executeRequest(requesterId, request, new HttpJSONResponse<>(mUploadsResponseParser), true);
        processGenericExceptions(request, response);

        return response.getJsonBody();
    }

    /**
     * Uploads the audio message file to the specified signed Amazon URL.
     *
     * @param signedUrl the signed URL
     * @param audioFile the audio file
     * @param contentType the audio file content type
     * @return the HTTP response
     */
    public HttpResponse uploadMessage(final String requesterId, final String signedUrl, final File audioFile, final String contentType) throws Exception {
        final HttpRequestFileData request = new HttpRequestFileData(signedUrl, HttpRequest.METHOD_PUT, false, audioFile);
        request.setHeaderParam("Content-Type", contentType);
        request.removeHeaderParam("Accept");
        request.removeUrlParam("_ts");
        request.setUploadResultReceiver(null);  // for now, no progress

        final HttpResponse response = executeRequest(requesterId, request, new HttpResponse(), false);
        super.processGenericExceptions(request, response);
        if((response.getCode() / 100) != 2) {
            throw new HttpResponseException(response.getCode(), request.toString());
        }

        return response;
    }

    /**
     * Gets the keys that uniquely identify the Android device
     *
     * @return the unique identifier of the device at position 0; its serial at position 1
     * @param context the app context
     */
    public static String[] getKeys(final Context context) {
        String androidId = String.valueOf(android.provider.Settings.Secure.getString(context.getContentResolver(), android.provider.Settings.Secure.ANDROID_ID));
        if(androidId == null) {
            androidId = UUID.randomUUID().toString();
        }
        return new String[]{ androidId, String.valueOf(Build.SERIAL) };
    }

}
