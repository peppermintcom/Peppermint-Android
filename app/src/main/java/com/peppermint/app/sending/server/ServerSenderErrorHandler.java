package com.peppermint.app.sending.server;

import android.content.Context;
import android.os.Build;
import android.telephony.TelephonyManager;

import com.crashlytics.android.Crashlytics;
import com.peppermint.app.data.SendingRequest;
import com.peppermint.app.rest.HttpRequest;
import com.peppermint.app.rest.HttpRequestListener;
import com.peppermint.app.rest.HttpResponse;
import com.peppermint.app.sending.SenderErrorHandler;
import com.peppermint.app.sending.SenderListener;
import com.peppermint.app.sending.SenderPreferences;
import com.peppermint.app.sending.SenderTask;
import com.peppermint.app.sending.mail.gmail.GmailSender;
import com.peppermint.app.utils.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Created by Nuno Luz on 01-10-2015.
 *
 * Error handler for the {@link GmailSender}.
 */
public class ServerSenderErrorHandler extends SenderErrorHandler implements HttpRequestListener {

    private static final String TAG = ServerSenderErrorHandler.class.getSimpleName();

    private static final int MAX_RETRIES = 3;

    // retry map, which allows trying to send the email up to MAX_RETRIES times if it fails
    // this allows us to ask for new access tokens upon failure and retry
    protected Map<UUID, Integer> mRetryMap;
    protected Map<UUID, SenderTask> mRecoveringMap;
    protected ServerClientManager mManager;

    public ServerSenderErrorHandler(Context context, SenderListener senderListener, Map<String, Object> parameters, SenderPreferences preferences) {
        super(context, senderListener, parameters, preferences);
        mRetryMap = new HashMap<>();
        mRecoveringMap = new HashMap<>();
        mManager = (ServerClientManager) getParameter(ServerSender.PARAM_MANAGER);
    }

    @Override
    public void init() {
        super.init();
        mManager.addHttpRequestListener(this);
    }

    @Override
    public void deinit() {
        mManager.removeHttpRequestListener(this);
        super.deinit();
    }

    private String[] getKeys() {
        final TelephonyManager tm = (TelephonyManager) getContext().getSystemService(Context.TELEPHONY_SERVICE);

        final String tmDevice, tmSerial, androidId;
        tmDevice = "" + tm.getDeviceId();
        tmSerial = "" + tm.getSimSerialNumber();
        androidId = "" + android.provider.Settings.Secure.getString(getContext().getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);

        UUID deviceUuid = new UUID(androidId.hashCode(), ((long)tmDevice.hashCode() << 32) | tmSerial.hashCode());
        String deviceId = deviceUuid.toString();

        return new String[]{ deviceId, Build.SERIAL};
    }

    @Override
    public void tryToRecover(SenderTask failedSendingTask) {
        super.tryToRecover(failedSendingTask);

        Throwable e = failedSendingTask.getError();

        // in this case just ask for permissions
        if(e instanceof InvalidAccessTokenException) {
            String[] keys = getKeys();
            UUID uuid = mManager.authenticate(keys[0], keys[1]);
            mRecoveringMap.put(uuid, failedSendingTask);
            return;
        }

        checkRetries(failedSendingTask);
    }

    private void checkRetries(SenderTask failedSendingTask) {
        SendingRequest request = failedSendingTask.getSendingRequest();

        if(!mRetryMap.containsKey(request.getId())) {
            mRetryMap.put(request.getId(), 1);
        } else {
            mRetryMap.put(request.getId(), mRetryMap.get(request.getId()) + 1);
        }

        // if it has failed MAX_RETRIES times, do not try it anymore
        if(mRetryMap.get(request.getId()) > MAX_RETRIES) {
            mRetryMap.remove(request.getId());
            doNotRecover(failedSendingTask);
            return;
        }

        // just try again for MAX_RETRIES times tops
        doRecover(failedSendingTask);
    }

    @Override
    public void onRequestSuccess(HttpRequest request, HttpResponse response) {
        SenderTask failedSendingTask = mRecoveringMap.remove(request.getUUID());
        if(failedSendingTask == null) {
            return;
        }

        JSONObject obj = null;
        try {
            obj = new JSONObject(response.getBody().toString());
            mManager.setAccessToken(obj.getString("at"));
        } catch (JSONException e) {
            Crashlytics.logException(e);
            checkRetries(failedSendingTask);
            return;
        }

        doRecover(failedSendingTask);
    }

    @Override
    public void onRequestError(HttpRequest request, HttpResponse response) {
        SenderTask failedSendingTask = mRecoveringMap.remove(request.getUUID());
        if(failedSendingTask == null) {
            return;
        }

        if(response.getException() == null && response.getCode() == 401) {
            if(request.getEndpoint().compareTo(ServerClientManager.RECORDER_TOKEN_ENDPOINT) == 0) {
                String[] keys = getKeys();
                UUID uuid = mManager.register(keys[0], keys[1], Utils.getAndroidVersion() + " - " + Utils.getDeviceName());
                mRecoveringMap.put(uuid, failedSendingTask);
                return;
            }
        }

        if(response.getException() != null) {
            Crashlytics.logException(response.getException());
        }

        checkRetries(failedSendingTask);
    }

    @Override
    public void onRequestCancel(HttpRequest request) {
        // nothing to do here
    }
}
