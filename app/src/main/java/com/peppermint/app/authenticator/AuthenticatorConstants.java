package com.peppermint.app.authenticator;

/**
 * Created by Nuno Luz on 27-01-2016.
 */
public class AuthenticatorConstants {

    public static final String ACCOUNT_NAME = "Peppermint";
    public static final String ACCOUNT_TYPE = "com.peppermint";

    // access token types
    public static final String RECORDER_TOKEN_TYPE = "com.peppermint.jwt.recorder";
    public static final String ACCOUNT_TOKEN_TYPE = "com.peppermint.jwt.account";
    public static final String FULL_TOKEN_TYPE = "com.peppermint.jwts";

    // account data
    public static final String ACCOUNT_PARAM_EMAIL = "email";
    public static final String ACCOUNT_PARAM_DEVICE_SERVER_ID = "deviceServerId";
    public static final String ACCOUNT_PARAM_DEVICE_ID = "deviceId";
    public static final String ACCOUNT_PARAM_DEVICE_KEY = "deviceKey";
    public static final String ACCOUNT_PARAM_TYPE = "accountType";
    public static final String ACCOUNT_PARAM_GCM_REG = "gcmRegistration";
}
