package com.peppermint.app.cloud.apis;

import android.content.Context;
import android.os.Build;

import com.peppermint.app.cloud.apis.data.AccountsResponse;
import com.peppermint.app.cloud.apis.data.JWTsResponse;
import com.peppermint.app.cloud.apis.data.MessageListResponse;
import com.peppermint.app.cloud.apis.data.MessagesResponse;
import com.peppermint.app.cloud.apis.data.RecordResponse;
import com.peppermint.app.cloud.apis.data.RecorderResponse;
import com.peppermint.app.cloud.apis.data.UploadsResponse;
import com.peppermint.app.cloud.apis.data.parsers.AccountsResponseParser;
import com.peppermint.app.cloud.apis.data.parsers.JWTsResponseParser;
import com.peppermint.app.cloud.apis.data.parsers.MessageListResponseParser;
import com.peppermint.app.cloud.apis.data.parsers.MessagesResponseParser;
import com.peppermint.app.cloud.apis.data.parsers.RecordResponseParser;
import com.peppermint.app.cloud.apis.data.parsers.RecorderResponseParser;
import com.peppermint.app.cloud.apis.data.parsers.UploadsResponseParser;
import com.peppermint.app.cloud.apis.exceptions.PeppermintApiAlreadyRegisteredException;
import com.peppermint.app.cloud.apis.exceptions.PeppermintApiInvalidAccessTokenException;
import com.peppermint.app.cloud.apis.exceptions.PeppermintApiRecipientNoAppException;
import com.peppermint.app.cloud.apis.exceptions.PeppermintApiResponseCodeException;
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
public class PeppermintApi implements Serializable {

    // payload content type
    private static final String CONTENT_TYPE_JSON = "application/vnd.api+json";

    // supported account types
    public static final int ACCOUNT_TYPE_REGULAR = 1;
    public static final int ACCOUNT_TYPE_GOOGLE = 2;
    public static final int ACCOUNT_TYPE_FACEBOOK = 3;

    // client-app API key
    private static final String API_KEY = "la8H4E6Iw5teA6nelVZWgZWqVJh7kWc6Gn1rj21hsYcTP7y7JAmDDQ";

    // server base URL
    protected static final String BASE_ENDPOINT_URL = "https://qdkkavugcd.execute-api.us-west-2.amazonaws.com/prod/v1/";

    // endpoints
    protected static final String ACCOUNTS_ENDPOINT = BASE_ENDPOINT_URL + "accounts";
    protected static final String ACCOUNTS_TOKENS_ENDPOINT = BASE_ENDPOINT_URL + "accounts/tokens";
    protected static final String ACCOUNTS_REL_ENDPOINT = BASE_ENDPOINT_URL + "accounts/{account_id}/relationships/receivers";

    protected static final String JWTS_ENDPOINT = BASE_ENDPOINT_URL + "jwts";
    protected static final String MESSAGES_ENDPOINT = BASE_ENDPOINT_URL + "messages";
    protected static final String PLAYS_ENDPOINT = BASE_ENDPOINT_URL + "reads";

    protected static final String RECORD_ENDPOINT = BASE_ENDPOINT_URL + "record";
    protected static final String RECORDER_ENDPOINT = BASE_ENDPOINT_URL + "recorder";
    protected static final String RECORDERS_ENDPOINT = BASE_ENDPOINT_URL + "recorders/{recorder_id}";
    protected static final String RECORDER_TOKEN_ENDPOINT = BASE_ENDPOINT_URL + "recorder-token";
    protected static final String UPLOADS_ENDPOINT = BASE_ENDPOINT_URL + "uploads";

    private String mAccessToken;

    // response parsers
    private final AccountsResponseParser mAccountsResponseParser = new AccountsResponseParser();
    private final RecorderResponseParser mRecorderResponseParser = new RecorderResponseParser();
    private final UploadsResponseParser mUploadsResponseParser = new UploadsResponseParser();
    private final RecordResponseParser mRecordResponseParser = new RecordResponseParser();
    private final JWTsResponseParser mJWTsResponseParser = new JWTsResponseParser();
    private final MessagesResponseParser mMessagesResponseParser = new MessagesResponseParser();
    private final MessageListResponseParser mMessageListResponseParser = new MessageListResponseParser();

    public PeppermintApi() {
    }

    // MESSAGES
    public void markAsPlayedMessage(String serverMessageId) throws PeppermintApiResponseCodeException, PeppermintApiInvalidAccessTokenException, PeppermintApiTooManyRequestsException, PeppermintApiRecipientNoAppException {
        HttpRequest request = new HttpRequest(PLAYS_ENDPOINT, HttpRequest.METHOD_POST);
        request.setHeaderParam("Authorization", "Bearer " + getAccessToken());
        request.setHeaderParam("X-Api-Key", API_KEY);
        request.setHeaderParam("Content-Type", CONTENT_TYPE_JSON);
        request.setBody("{ \"data\": { \"type\":\"reads\", \"id\":\"" + serverMessageId + "\" } }");
        HttpResponse response = new HttpResponse();
        request.execute(response);

        if(response.getException() != null) {
            throw new HttpResponseException(response.getException());
        }
        if(response.getCode() == 401 || response.getCode() == 403) {
            throw new PeppermintApiInvalidAccessTokenException(request.toString());
        }
        if((response.getCode() / 100) != 2) {
            throw new PeppermintApiResponseCodeException(response.getCode(), request.toString());
        }
    }

    public MessageListResponse getMessages(String serverAccountId, String sinceTimestamp, boolean received) throws PeppermintApiResponseCodeException, PeppermintApiInvalidAccessTokenException, PeppermintApiTooManyRequestsException, PeppermintApiRecipientNoAppException {
        HttpRequest request = new HttpRequest(MESSAGES_ENDPOINT, HttpRequest.METHOD_GET, false);
        request.setHeaderParam("Authorization", "Bearer " + getAccessToken());
        request.setHeaderParam("X-Api-Key", API_KEY);
        request.setUrlParam(received ? "recipient" : "sender", serverAccountId);
        request.setUrlParam("since", sinceTimestamp);
        HttpJSONResponse<MessageListResponse> response = new HttpJSONResponse<>(mMessageListResponseParser);
        request.execute(response);

        if(response.getException() != null) {
            throw new HttpResponseException(response.getException());
        }
        if(response.getCode() == 401 || response.getCode() == 403) {
            throw new PeppermintApiInvalidAccessTokenException(request.toString());
        }
        if(response.getCode() == 404) {
            throw new PeppermintApiRecipientNoAppException();
        }
        if((response.getCode() / 100) != 2) {
            throw new PeppermintApiResponseCodeException(response.getCode(), request.toString());
        }

        return response.getJsonBody();
    }

    /**
     * Sends a previously created message to the specified recipient through in-app messaging.
     * @param transcriptionUrl the transcription URL
     * @param audioUrl the audio message URL
     * @param senderEmail the sender email address
     * @param recipientEmail the recipient email address
     * @return the endpoint response
     * @throws PeppermintApiResponseCodeException a status code different from 2XX was returned
     * @throws PeppermintApiInvalidAccessTokenException
     * @throws PeppermintApiTooManyRequestsException
     * @throws PeppermintApiRecipientNoAppException if the recipient doesn't have Peppermint installed
     */
    public MessagesResponse sendMessage(String transcriptionUrl, String audioUrl, String senderEmail, String recipientEmail, int durationInSeconds) throws PeppermintApiResponseCodeException, PeppermintApiInvalidAccessTokenException, PeppermintApiTooManyRequestsException, PeppermintApiRecipientNoAppException {
        HttpRequest request = new HttpRequest(MESSAGES_ENDPOINT, HttpRequest.METHOD_POST);
        request.setHeaderParam("Authorization", "Bearer " + getAccessToken());
        request.setHeaderParam("X-Api-Key", API_KEY);
        request.setHeaderParam("Content-Type", CONTENT_TYPE_JSON);
        request.setBody("{ \"data\": { \"attributes\": { \"audio_url\": \"" + audioUrl + "\", \"sender_email\": \"" + senderEmail + "\", \"recipient_email\": \"" + recipientEmail + "\" }, \"type\":\"messages\" } }");
        HttpJSONResponse<MessagesResponse> response = new HttpJSONResponse<>(mMessagesResponseParser);
        request.execute(response);

        if(response.getException() != null) {
            throw new HttpResponseException(response.getException());
        }
        if(response.getCode() == 401 || response.getCode() == 403) {
            throw new PeppermintApiInvalidAccessTokenException(request.toString());
        }
        if(response.getCode() == 404) {
            throw new PeppermintApiRecipientNoAppException();
        }
        if((response.getCode() / 100) != 2) {
            throw new PeppermintApiResponseCodeException(response.getCode(), request.toString());
        }

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
     * @throws PeppermintApiResponseCodeException  a status code different from 2XX was returned
     * @throws PeppermintApiInvalidAccessTokenException
     */
    public JWTsResponse authOrRegister(String accountEmail, String accountPassword, int accountType, String recorderId, String recorderKey, String fullName, TrackerManager trackerManager) throws PeppermintApiTooManyRequestsException, PeppermintApiResponseCodeException, PeppermintApiInvalidAccessTokenException {
        JWTsResponse jwtsResponse = null;

        try {
            jwtsResponse = authBoth(accountEmail, accountPassword, recorderId, recorderKey, accountType);
            addReceiverRecorder(jwtsResponse.getAccount().getAccountId(), jwtsResponse.getRecorder().getRecorderId());
        } catch(PeppermintApiInvalidAccessTokenException e) {
            // register/auth recorder
            try {
                registerRecorder(recorderId, recorderKey, Utils.getAndroidVersion() + " - " + Utils.getDeviceName());
            } catch (PeppermintApiAlreadyRegisteredException ex) {
                trackerManager.log("Recorder already registered! Moving on...");
            }

            if(accountType == ACCOUNT_TYPE_REGULAR) {
                // register/auth account
                try {
                    registerAccount(accountEmail, accountPassword, fullName, accountType);
                } catch (PeppermintApiAlreadyRegisteredException ex) {
                    trackerManager.log("Account already registered! Moving on...");
                }
            }

            // try again to authenticate; it should work now
            jwtsResponse = authBoth(accountEmail, accountPassword, recorderId, recorderKey, accountType);
            // add relationship between account and recorder
            addReceiverRecorder(jwtsResponse.getAccount().getAccountId(), jwtsResponse.getRecorder().getRecorderId());
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
     * @throws PeppermintApiResponseCodeException a status code different from 2XX was returned
     * @throws PeppermintApiInvalidAccessTokenException
     * @throws PeppermintApiTooManyRequestsException
     */
    public JWTsResponse authBoth(String accountEmail, String accountPassword, String recorderId, String recorderKey, int accountType) throws PeppermintApiResponseCodeException, PeppermintApiInvalidAccessTokenException, PeppermintApiTooManyRequestsException {
        String accountTypeStr = "account";
        switch (accountType) {
            case ACCOUNT_TYPE_GOOGLE:
                accountTypeStr = "google";
                break;
            case ACCOUNT_TYPE_FACEBOOK:
                accountTypeStr = "facebook";
                break;
        }

        HttpRequest request = new HttpRequest(JWTS_ENDPOINT, HttpRequest.METHOD_POST);
        request.setHeaderParam("Authorization", "Peppermint " + accountTypeStr + "=" + Utils.getBasicAuthenticationToken(accountEmail, accountPassword) + ", recorder=" + Utils.getBasicAuthenticationToken(recorderId, recorderKey));
        request.setHeaderParam("X-Api-Key", API_KEY);
        HttpJSONResponse<JWTsResponse> response = new HttpJSONResponse<>(mJWTsResponseParser);
        request.execute(response);

        if(response.getException() != null) {
            throw new HttpResponseException(response.getException());
        }
        if(response.getCode() == 401 || response.getCode() == 404) {
            throw new PeppermintApiInvalidAccessTokenException(request.toString());
        }
        if(response.getCode() == 429) {
            throw new PeppermintApiTooManyRequestsException(request.toString());
        }
        if((response.getCode() / 100) != 2) {
            throw new PeppermintApiResponseCodeException(response.getCode(), request.toString());
        }

        JWTsResponse responseData = response.getJsonBody();
        if(responseData != null) {
            setAccessToken(responseData.getAccessToken());
        }
        return responseData;
    }

    // ACCOUNT

    /**
     * Authenticates the account.
     *
     * @param email the account email
     * @param password the account password
     * @return the account response
     * @throws PeppermintApiResponseCodeException a status code different from 2XX was returned
     * @throws PeppermintApiInvalidAccessTokenException
     */
    public AccountsResponse authAccount(String email, String password) throws PeppermintApiResponseCodeException, PeppermintApiInvalidAccessTokenException {
        HttpRequest request = new HttpRequest(ACCOUNTS_TOKENS_ENDPOINT, HttpRequest.METHOD_POST);
        request.setHeaderParam("Authorization", "Basic " + Utils.getBasicAuthenticationToken(email, password));
        request.setBody("{ \"api_key\": \"" + API_KEY + "\" }");
        HttpJSONResponse<AccountsResponse> response = new HttpJSONResponse<>(mAccountsResponseParser);
        request.execute(response);

        if(response.getException() != null) {
            throw new HttpResponseException(response.getException());
        }
        if(response.getCode() == 401) {
            throw new PeppermintApiInvalidAccessTokenException(request.toString());
        }
        if((response.getCode() / 100) != 2) {
            throw new PeppermintApiResponseCodeException(response.getCode(), request.toString());
        }

        AccountsResponse responseData = response.getJsonBody();
        setAccessToken(responseData.getAccessToken());
        return responseData;
    }

    /**
     * Creates a new account.
     *
     * @param email the account email
     * @param password the account password
     * @param fullName the owners full name
     * @param accountType the account type
     * @return the account response
     * @throws PeppermintApiInvalidAccessTokenException
     * @throws PeppermintApiResponseCodeException a status code different from 2XX was returned
     * @throws PeppermintApiAlreadyRegisteredException if the account is already registered
     */
    public AccountsResponse registerAccount(String email, String password, String fullName, int accountType) throws PeppermintApiInvalidAccessTokenException, PeppermintApiResponseCodeException, PeppermintApiAlreadyRegisteredException {
        HttpRequest request = new HttpRequest(ACCOUNTS_ENDPOINT, HttpRequest.METHOD_POST);
        request.setBody("{ \"api_key\": \"" + API_KEY + "\", \"u\": { \"email\": \"" + email +
                "\", \"password\": \"" + password + "\", \"full_name\": \"" + fullName + "\" } }");
        HttpJSONResponse<AccountsResponse> response = new HttpJSONResponse<>(mAccountsResponseParser);
        request.execute(response);

        if(response.getException() != null) {
            throw new HttpResponseException(response.getException());
        }
        if(response.getCode() == 401) {
            throw new PeppermintApiInvalidAccessTokenException(request.toString());
        }
        if(response.getCode() == 409) {
            throw new PeppermintApiAlreadyRegisteredException(request.toString());
        }
        if((response.getCode() / 100) != 2) {
            throw new PeppermintApiResponseCodeException(response.getCode(), request.toString());
        }

        AccountsResponse responseData = response.getJsonBody();
        setAccessToken(responseData.getAccessToken());
        return responseData;
    }

    /**
     * Establishes a relationship between an account and a recorder/device, stating that
     * the specified account is configured on the specified recorder/device.
     *
     * @param accountId the account server id
     * @param recorderId the recorder server id
     * @return the HTTP response
     * @throws PeppermintApiResponseCodeException a status code different from 2XX was returned
     * @throws PeppermintApiInvalidAccessTokenException
     */
    public HttpResponse addReceiverRecorder(String accountId, String recorderId) throws PeppermintApiResponseCodeException, PeppermintApiInvalidAccessTokenException {
        HttpRequest request = new HttpRequest(ACCOUNTS_REL_ENDPOINT.replace("{account_id}", accountId), HttpRequest.METHOD_POST);
        request.setHeaderParam("Authorization", "Bearer " + getAccessToken());
        request.setHeaderParam("X-Api-Key", API_KEY);
        request.setHeaderParam("Content-Type", CONTENT_TYPE_JSON);
        request.setBody("{ \"data\": [ { \"id\":\"" + recorderId + "\", \"type\":\"recorders\" } ] }");
        HttpResponse response = new HttpResponse();
        request.execute(response);

        if(response.getException() != null) {
            throw new HttpResponseException(response.getException());
        }
        if(response.getCode() == 401 || response.getCode() == 404) {
            throw new PeppermintApiInvalidAccessTokenException(request.toString());
        }
        if((response.getCode() / 100) != 2) {
            throw new PeppermintApiResponseCodeException(response.getCode(), request.toString());
        }

        return response;
    }

    /**
     * Deletes the relationship between an account and a recorder/device.
     *
     * @param accountId the account server id
     * @param recorderId the recorder server id
     * @return the HTTP response
     * @throws PeppermintApiResponseCodeException a status code different from 2XX was returned
     * @throws PeppermintApiInvalidAccessTokenException
     */
    public HttpResponse removeReceiverRecorder(String accountId, String recorderId) throws PeppermintApiResponseCodeException, PeppermintApiInvalidAccessTokenException {
        HttpRequest request = new HttpRequest(ACCOUNTS_REL_ENDPOINT.replace("{account_id}", accountId) + "/" + recorderId, HttpRequest.METHOD_DELETE);
        request.setHeaderParam("Authorization", "Bearer " + getAccessToken());
        request.setHeaderParam("X-Api-Key", API_KEY);
        HttpResponse response = new HttpResponse();
        request.execute(response);

        if(response.getException() != null) {
            throw new HttpResponseException(response.getException());
        }
        if(response.getCode() == 401 || response.getCode() == 404) {
            throw new PeppermintApiInvalidAccessTokenException(request.toString());
        }
        if((response.getCode() / 100) != 2) {
            throw new PeppermintApiResponseCodeException(response.getCode(), request.toString());
        }

        return response;
    }

    // RECORDER

    /**
     * Authenticates the recorder/device.
     *
     * @param recorderId the recorder/device id
     * @param recorderKey the recorder/device key/password
     * @return the recorder response
     * @throws PeppermintApiResponseCodeException a status code different from 2XX was returned
     * @throws PeppermintApiInvalidAccessTokenException
     */
    public RecorderResponse authRecorder(String recorderId, String recorderKey) throws PeppermintApiResponseCodeException, PeppermintApiInvalidAccessTokenException {
        HttpRequest request = new HttpRequest(RECORDER_TOKEN_ENDPOINT, HttpRequest.METHOD_POST);
        request.setHeaderParam("Authorization", "Basic " + Utils.getBasicAuthenticationToken(recorderId, recorderKey));
        HttpJSONResponse<RecorderResponse> response = new HttpJSONResponse<>(mRecorderResponseParser);
        request.execute(response);

        if(response.getException() != null) {
            throw new HttpResponseException(response.getException());
        }
        if(response.getCode() == 401) {
            throw new PeppermintApiInvalidAccessTokenException(request.toString());
        }
        if((response.getCode() / 100) != 2) {
            throw new PeppermintApiResponseCodeException(response.getCode(), request.toString());
        }

        RecorderResponse responseData = response.getJsonBody();
        setAccessToken(responseData.getAccessToken());
        return responseData;
    }

    /**
     * Creates a new recorder/device.
     *
     * @param recorderId the recorder/device id
     * @param recorderKey the recorder/device key/password
     * @param description a description of the recorder/device
     * @return the recorder response
     * @throws PeppermintApiInvalidAccessTokenException
     * @throws PeppermintApiResponseCodeException a status code different from 2XX was returned
     * @throws PeppermintApiAlreadyRegisteredException if the recorder/device is already registered
     */
    public RecorderResponse registerRecorder(String recorderId, String recorderKey, String description) throws PeppermintApiInvalidAccessTokenException, PeppermintApiResponseCodeException, PeppermintApiAlreadyRegisteredException {
        HttpRequest request = new HttpRequest(RECORDER_ENDPOINT, HttpRequest.METHOD_POST);
        request.setBody("{ \"api_key\": \"" + API_KEY + "\", \"recorder\": { \"description\": \"" + description +
                "\", \"recorder_client_id\": \"" + recorderId + "\", \"recorder_key\": \"" + recorderKey + "\" } }");
        HttpJSONResponse<RecorderResponse> response = new HttpJSONResponse<>(mRecorderResponseParser);
        request.execute(response);

        if(response.getException() != null) {
            throw new HttpResponseException(response.getException());
        }
        if(response.getCode() == 401) {
            throw new PeppermintApiInvalidAccessTokenException(request.toString());
        }
        if(response.getCode() == 409) {
            throw new PeppermintApiAlreadyRegisteredException(request.toString());
        }
        if((response.getCode() / 100) != 2) {
            throw new PeppermintApiResponseCodeException(response.getCode(), request.toString());
        }

        RecorderResponse responseData = response.getJsonBody();
        setAccessToken(responseData.getAccessToken());
        return responseData;
    }

    /**
     * Updates the recorder/device GCM registration token.
     *
     * @param recorderId the recorder/device id
     * @param gcmRegistrationToken the GCM registration token
     * @return the HTTP response
     * @throws PeppermintApiInvalidAccessTokenException
     * @throws PeppermintApiResponseCodeException a status code different from 2XX was returned
     */
    public HttpResponse updateRecorder(String recorderId, String gcmRegistrationToken) throws PeppermintApiInvalidAccessTokenException, PeppermintApiResponseCodeException {
        HttpRequest request = new HttpRequest(RECORDERS_ENDPOINT.replace("{recorder_id}", recorderId), HttpRequest.METHOD_PUT);
        request.setHeaderParam("X-Api-Key", API_KEY);
        request.setHeaderParam("Content-Type", CONTENT_TYPE_JSON);
        request.setHeaderParam("Authorization", "Bearer " + getAccessToken());
        request.setBody("{\"data\": { \"type\": \"recorders\", \"id\": \"" + recorderId + "\", \"attributes\": { \"gcm_registration_token\": \"" + gcmRegistrationToken + "\" } } }");
        HttpResponse response = new HttpResponse();
        request.execute(response);

        if(response.getException() != null) {
            throw new HttpResponseException(response.getException());
        }
        if(response.getCode() == 401) {
            throw new PeppermintApiInvalidAccessTokenException(request.toString());
        }
        if((response.getCode() / 100) != 2) {
            throw new PeppermintApiResponseCodeException(response.getCode(), request.toString());
        }

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
     * @throws PeppermintApiResponseCodeException a status code different from 2XX was returned
     */
    public UploadsResponse getSignedUrl(String senderName, String senderEmail, String contentType) throws PeppermintApiInvalidAccessTokenException, PeppermintApiResponseCodeException {
        HttpRequest request = new HttpRequest(UPLOADS_ENDPOINT, HttpRequest.METHOD_POST);
        request.setHeaderParam("Authorization", "Bearer " + getAccessToken());
        request.setBody("{\"content_type\":\"" + contentType + "\", \"sender_name\":\"" + senderName + "\", \"sender_email\":\"" + senderEmail + "\"}");
        HttpJSONResponse<UploadsResponse> response = new HttpJSONResponse<>(mUploadsResponseParser);
        request.execute(response);

        if(response.getException() != null) {
            throw new HttpResponseException(response.getException());
        }
        if(response.getCode() == 401) {
            throw new PeppermintApiInvalidAccessTokenException(request.toString());
        }
        if((response.getCode() / 100) != 2) {
            throw new PeppermintApiResponseCodeException(response.getCode(), request.toString());
        }

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
    public HttpResponse uploadMessage(String signedUrl, File audioFile, String contentType) {
        HttpRequestFileData request = new HttpRequestFileData(signedUrl, HttpRequest.METHOD_PUT, false, audioFile);
        request.setHeaderParam("Content-Type", contentType);
        request.removeHeaderParam("Accept");
        request.removeUrlParam("_ts");
        request.setUploadResultReceiver(null);  // for now, no progress
        HttpResponse response = new HttpResponse();
        request.execute(response);
        return response;
    }

    /**
     * Confirms that an audio message has been uploaded to the specified signed Amazon URL.
     *
     * @param signedUrl the signed URL
     * @return the record endpoint response
     */
    public RecordResponse confirmUploadMessage(String signedUrl) {
        HttpRequest request = new HttpRequest(RECORD_ENDPOINT, HttpRequest.METHOD_POST);
        request.setHeaderParam("Authorization", "Bearer " + getAccessToken());
        request.setBody("{ \"signed_url\": \"" + signedUrl + "\" }");
        HttpJSONResponse<RecordResponse> response = new HttpJSONResponse<>(mRecordResponseParser);
        request.execute(response);
        return response.getJsonBody();
    }

    public String getAccessToken() {
        return mAccessToken;
    }

    public void setAccessToken(String at) {
        this.mAccessToken = at;
    }

    /**
     * Gets the keys that uniquely identify the Android device
     *
     * @return the unique identifier of the device at position 0; its serial at position 1
     * @param context the app context
     */
    public static String[] getKeys(Context context) {
        String androidId = String.valueOf(android.provider.Settings.Secure.getString(context.getContentResolver(), android.provider.Settings.Secure.ANDROID_ID));
        if(androidId == null) {
            androidId = UUID.randomUUID().toString();
        }
        return new String[]{ androidId, String.valueOf(Build.SERIAL) };
    }

}
