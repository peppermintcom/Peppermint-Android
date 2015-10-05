package com.peppermint.app.ui.tutorial;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.peppermint.app.PeppermintApp;
import com.peppermint.app.R;
import com.peppermint.app.ui.views.PeppermintRecordView;
import com.peppermint.app.utils.AnimatorBuilder;

import org.w3c.dom.Text;

public class SplashActivity extends AppCompatActivity {

    private PeppermintRecordView mImgLogo;
    private TextView mTxtName;
    private AnimatorBuilder mAnimatorBuilder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.a_splash_layout);

        PeppermintApp app = (PeppermintApp) getApplication();
        mAnimatorBuilder = new AnimatorBuilder();

        mImgLogo = (PeppermintRecordView) findViewById(R.id.imgLogo);
        mTxtName = (TextView) findViewById(R.id.txtName);
        mTxtName.setTypeface(app.getFontSemibold());

        //mAnimatorBuilder.b
    }
}
