package com.peppermint.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.peppermint.app.services.authenticator.AuthenticationData;
import com.peppermint.app.ui.authentication.AuthenticatorActivity;
import com.peppermint.app.services.authenticator.AuthenticatorUtils;
import com.peppermint.app.cloud.apis.peppermint.PeppermintApiNoAccountException;
import com.peppermint.app.ui.contacts.ContactListActivity;

/**
 * Created by Nuno Luz on 06-01-2016.
 *
 * Startup Activity.
 */
public class StartupActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(!hasAccount()) {
            // launch authentication activity
            final Intent forwardIntent = new Intent(getApplicationContext(), ContactListActivity.class);
            if(getIntent() != null) {
                if(getIntent().getExtras() != null) {
                    forwardIntent.putExtras(getIntent().getExtras());
                }
                if(getIntent().getData() != null) {
                    forwardIntent.setData(getIntent().getData());
                }
            }
            final Intent authenticationIntent = new Intent(this, AuthenticatorActivity.class);
            authenticationIntent.putExtra(AuthenticatorActivity.PARAM_FORWARD_TO, forwardIntent);
            startActivity(authenticationIntent);
        } else {
            // launch contacts activity
            final Intent contactIntent = new Intent(this, ContactListActivity.class);
            if(getIntent() != null) {
                if(getIntent().getExtras() != null) {
                    contactIntent.putExtras(getIntent().getExtras());
                }
                if(getIntent().getData() != null) {
                    contactIntent.setData(getIntent().getData());
                }
            }
            startActivity(contactIntent);
        }

        finish();
    }

    private boolean hasAccount() {
        final AuthenticatorUtils authenticatorUtils = new AuthenticatorUtils(this);
        AuthenticationData authenticationData = null;
        try {
            authenticationData = authenticatorUtils.getAccountData();
        } catch (PeppermintApiNoAccountException e) {
            /* nothing to do here */
        }
        return authenticationData != null;
    }
}
