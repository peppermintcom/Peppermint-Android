package com.peppermint.app.ui.authentication;

import android.accounts.Account;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.peppermint.app.R;

/**
 * Created by Nuno Luz on 27/08/2015.
 *
 * ArrayAdapter to show accounts in a ListView.
 */
public class AuthArrayAdapter extends ArrayAdapter<Account> {

    private Account[] mAccounts;

    public AuthArrayAdapter(Context context, Account[] accounts) {
        super(context, 0);
        this.mAccounts = accounts;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;

        if(v == null) {
            v = LayoutInflater.from(getContext()).inflate(R.layout.i_account_layout, parent, false);
        }

        TextView txtName = (TextView) v.findViewById(R.id.txtName);
        txtName.setText(mAccounts[position].name);

        return v;
    }

    @Override
    public long getItemId(int position) {
        return mAccounts[position].hashCode();
    }

    @Override
    public Account getItem(int position) {
        return mAccounts[position];
    }

    @Override
    public int getCount() {
        return mAccounts.length;
    }
}
