package com.peppermint.app.sending.mail.gmail;

import android.content.Context;

import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.peppermint.app.rest.HttpAsyncTask;
import com.peppermint.app.rest.HttpRequest;
import com.peppermint.app.utils.PepperMintPreferences;
import com.peppermint.app.utils.Utils;

import org.json.JSONObject;

import java.util.Map;

/**
 * Created by Nuno Luz on 22-01-2016.
 */
public class GmailDisplayNameTask extends HttpAsyncTask<Void, String> {

    public GmailDisplayNameTask(Context context) {
        super(context);
    }

    public GmailDisplayNameTask(Context context, Map<String, Object> parameters) {
        super(context, parameters);
    }

    public GmailDisplayNameTask(HttpAsyncTask task) {
        super(task);
    }

    @Override
    protected String send(Void... params) throws Throwable {
        PepperMintPreferences globalPrefs = new PepperMintPreferences(getContext());
        GoogleAccountCredential credential = (GoogleAccountCredential) getParameter(GmailSender.PARAM_GMAIL_CREDENTIAL);

        // try to get the name from Google API
        HttpRequest request = new HttpRequest("https://www.googleapis.com/oauth2/v1/userinfo", HttpRequest.METHOD_GET, false);
        request.setHeaderParam("Authorization", "Bearer " + credential.getToken());
        request.setUrlParam("alt", "json");

        executeHttpRequest(request);
        String content = waitForHttpResponse();

        if (content != null) {
            JSONObject json = new JSONObject(content);
            if (!json.isNull("given_name") && !json.isNull("family_name")) {
                String firstName = json.getString("given_name");
                String lastName = json.getString("family_name");
                if (Utils.isValidName(firstName) && Utils.isValidName(lastName)) {
                    globalPrefs.setFirstName(firstName);
                    globalPrefs.setLastName(lastName);
                }
            } else if (!json.isNull("name")) {
                String fullName = json.getString("name");
                if (Utils.isValidName(fullName)) {
                    globalPrefs.setFullName(fullName);
                }
            }
        }

        return globalPrefs.getFullName();
    }
}
