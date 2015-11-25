package com.peppermint.app.ui.authentication;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.ListFragment;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;

import com.peppermint.app.R;
import com.peppermint.app.SenderServiceManager;
import com.peppermint.app.utils.PepperMintPreferences;

/**
 * Created by Nuno Luz on 10-11-2015.
 */
public class AuthFragment extends ListFragment implements View.OnClickListener, AdapterView.OnItemClickListener {

    public static boolean startAuthentication(Activity callerActivity, int requestCode) {
        PepperMintPreferences prefs = new PepperMintPreferences(callerActivity);
        if(prefs.getGmailPreferences().getPreferredAccountName() != null) {
            return false;
        }

        Account[] accounts = AccountManager.get(callerActivity).getAccountsByType("com.google");
        if(accounts.length == 1) {
            prefs.getGmailPreferences().setPreferredAccountName(accounts[0].name);
            return false;
        }

        Intent intent = new Intent(callerActivity, AuthActivity.class);
        callerActivity.startActivityForResult(intent, requestCode);
        return true;
    }

    private Button mBtnAddAccount;
    private AuthArrayAdapter mAdapter;
    private Account[] mAccounts;
    private PepperMintPreferences mPreferences;
    private Activity mActivity;

    @SuppressWarnings("deprecation")
    @Override
    public void onAttach(Activity context) {
        super.onAttach(context);
        mActivity = context;
        mPreferences = new PepperMintPreferences(context);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // inflate the view
        View v = inflater.inflate(R.layout.f_authentication, container, false);

        mBtnAddAccount = (Button) v.findViewById(R.id.btnAddAccount);
        mBtnAddAccount.setOnClickListener(this);

        return v;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getListView().setOnItemClickListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        mAccounts = AccountManager.get(mActivity).getAccountsByType("com.google");
        mAdapter = new AuthArrayAdapter(mActivity, mAccounts);
        getListView().setAdapter(mAdapter);
    }

    @Override
    public void onClick(View v) {
        if(v.equals(mBtnAddAccount)) {
            startActivity(new Intent(Settings.ACTION_ADD_ACCOUNT));
            return;
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        mPreferences.getGmailPreferences().setPreferredAccountName(mAccounts[position].name);

        SenderServiceManager senderManager = new SenderServiceManager(mActivity.getApplicationContext());
        senderManager.startAndAuthorize();

        mActivity.setResult(Activity.RESULT_OK);
        mActivity.finish();
    }
}
