package com.peppermint.app.sending;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;

import com.crashlytics.android.Crashlytics;
import com.peppermint.app.R;

import java.util.UUID;

/**
 * Created by Nuno Luz on 09-09-2015.
 *
 * Invisible "middleware" activity that allows services to:
 * <ol>
 *     <li>Start any activity through "startActivityForResult"</li>
 *     <li>Obtain the result through "onActivityResult"</li>
 * </ol>
 *
 * This activity sends all data back to the service through the {@link LocalBroadcastManager}.
 */
public class GetResultActivity extends Activity {

    public static final String INTENT_ID = "GetResultActivity_Id";
    public static final String INTENT_BROADCAST_TYPE = "GetResultActivity_BroadcastType";
    public static final String INTENT_REQUESTCODE = "GetResultActivity_RequestCode";
    public static final String INTENT_RESULTCODE = "GetResultActivity_ResultCode";
    public static final String INTENT_DATA = "GetResultActivity_Data";

    private UUID mUuid;
    private String mBroadcastType;
    private int mRequestCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.a_getresult_layout);

        // obtain the intent with the data to start the new activity for result
        mUuid = (UUID) getIntent().getSerializableExtra(INTENT_ID);
        mBroadcastType = getIntent().getStringExtra(INTENT_BROADCAST_TYPE);
        mRequestCode = getIntent().getIntExtra(INTENT_REQUESTCODE, -1);

        Intent i = getIntent().getParcelableExtra(INTENT_DATA);

        if(i == null) {
            onActivityResult(mRequestCode, -2, null);
            Crashlytics.log("Intent data is null on GetResultActivity! ReqCode=" + mRequestCode + " BroadcastType=" + mBroadcastType + " UUID=" + mUuid);
            return;
        }

        startActivityForResult(i, mRequestCode);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // obtain the result from the started activity
        if(requestCode == mRequestCode || requestCode == -1) {
            Intent intent = new Intent(mBroadcastType);
            intent.putExtra(INTENT_ID, mUuid);
            intent.putExtra(INTENT_RESULTCODE, resultCode);
            intent.putExtra(INTENT_REQUESTCODE, requestCode);
            intent.putExtra(INTENT_DATA, data);
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
            finish();
        }
    }
}
