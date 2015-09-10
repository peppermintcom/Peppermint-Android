package com.peppermint.app.senders;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;

import com.peppermint.app.R;

import java.util.UUID;

/**
 * Created by Nuno Luz on 09-09-2015.
 *
 * Invisible activity that allows services to start other activities through "startActivityForResult",
 * and obtain the result on "onActivityResult". To accomplish this, it uses the LocalBroadcastManager.
 */
public class GetResultActivity extends Activity {

    private UUID mUuid;
    private String mBroadcastType;
    private int mRequestCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_getresult_layout);

        // obtain the intent with the data to start the new activity for result
        mUuid = (UUID) getIntent().getSerializableExtra(Sender.INTENT_ID);
        mBroadcastType = getIntent().getStringExtra(Sender.INTENT_BROADCAST_TYPE);
        mRequestCode = getIntent().getIntExtra(Sender.INTENT_REQUESTCODE, -1);

        Intent i = getIntent().getParcelableExtra(Sender.INTENT_DATA);
        startActivityForResult(i, mRequestCode);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // obtain the result from the started activity
        if(requestCode == mRequestCode || requestCode == -1) {
            Intent intent = new Intent(mBroadcastType);
            intent.putExtra(Sender.INTENT_ID, mUuid);
            intent.putExtra(Sender.INTENT_RESULTCODE, resultCode);
            intent.putExtra(Sender.INTENT_REQUESTCODE, requestCode);
            intent.putExtra(Sender.INTENT_DATA, data);
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
            finish();
        }
    }
}
