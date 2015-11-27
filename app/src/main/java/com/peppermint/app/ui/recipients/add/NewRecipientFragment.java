package com.peppermint.app.ui.recipients.add;

import android.app.Activity;
import android.app.Fragment;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.graphics.Rect;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.peppermint.app.R;
import com.peppermint.app.ui.CustomActionBarActivity;
import com.peppermint.app.utils.PepperMintPreferences;
import com.peppermint.app.utils.Popup;
import com.peppermint.app.utils.Utils;

import java.util.ArrayList;

/**
 * Created by Nuno Luz on 10-11-2015.
 */
public class NewRecipientFragment extends Fragment {

    private PepperMintPreferences mPreferences;
    private CustomActionBarActivity mActivity;

    private Popup mPopup;

    @SuppressWarnings("deprecation")
    @Override
    public void onAttach(Activity context) {
        super.onAttach(context);
        mActivity = (CustomActionBarActivity) context;
        mPreferences = new PepperMintPreferences(context);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mPopup = new Popup(mActivity);

        // global touch interceptor to hide keyboard
        mActivity.getTouchInterceptor().setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                /*if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    Rect outRect = new Rect();
                    mTxtName.getGlobalVisibleRect(outRect);
                    if (!outRect.contains((int) event.getRawX(), (int) event.getRawY())) {
                        Utils.hideKeyboard(mActivity);
                    }
                }*/
                mPopup.dismiss();
                return false;
            }
        });

        // inflate the view
        View v = inflater.inflate(R.layout.f_newcontact, container, false);

        return v;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }


    /*String name = mTxtNewName.getText().toString().trim();
    String via = mTxtNewContact.getText().toString().trim();
    String mimeType = null;

    // validate display name
    if(name.length() <= 0) {
        Toast.makeText(mActivity, R.string.msg_message_invalid_contactname, Toast.LENGTH_LONG).show();
        return;
    }

    // validate email
    if(Utils.isValidEmail(via)) {
        mimeType = ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE;
    } else if(Utils.isValidPhoneNumber(via)) {
        mimeType = ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE;
    }

    if(mimeType == null) {
        Toast.makeText(mActivity, R.string.msg_message_invalid_contactvia, Toast.LENGTH_LONG).show();
        return;
    }

    ArrayList<ContentProviderOperation> ops = new ArrayList<>();

    // create raw contact
    ops.add(ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
            .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
    .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null).build());

    // add display name data
    ops.add(ContentProviderOperation
            .newInsert(ContactsContract.Data.CONTENT_URI)
            .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
    .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name)
    .build());

    // add phone/email data
    ops.add(ContentProviderOperation
            .newInsert(ContactsContract.Data.CONTENT_URI)
            .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
    .withValue(ContactsContract.Data.MIMETYPE, mimeType)
    .withValue(ContactsContract.Data.DATA1, via).build());

    try {
        ContentProviderResult[] res = mActivity.getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
        if(res.length < ops.size()) {
            throw new RuntimeException("Not all operations were performed while trying to insert contact: Total Ops = " + ops.size() + "; Performed = " + res.length);
        }
        Toast.makeText(mActivity, R.string.msg_message_contact_added, Toast.LENGTH_LONG).show();
        // refresh listview
        mSearchListBarView.setSearchText(name + " <" + via + ">");
    } catch (Throwable e) {
        Toast.makeText(mActivity, R.string.msg_message_unable_addcontact, Toast.LENGTH_LONG).show();
        Crashlytics.logException(e);
    }*/
}
