package com.peppermint.app.ui.recipients.add;

import android.app.Activity;
import android.app.Fragment;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.peppermint.app.PeppermintApp;
import com.peppermint.app.R;
import com.peppermint.app.ui.CustomActionBarActivity;
import com.peppermint.app.ui.recipients.RecipientAdapterUtils;
import com.peppermint.app.utils.FilteredCursor;
import com.peppermint.app.utils.PepperMintPreferences;
import com.peppermint.app.utils.Popup;
import com.peppermint.app.utils.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Nuno Luz on 10-11-2015.
 */
public class NewRecipientFragment extends Fragment implements View.OnClickListener {

    public static final String KEY_VIA = "NewRecipientFragment_Via";
    public static final String KEY_NAME = "NewRecipientFragment_Name";
    public static final String KEY_PHONE = "NewRecipientFragment_Phone";
    public static final String KEY_MAIL = "NewRecipientFragment_Mail";
    public static final String KEY_RAW_ID = "NewRecipientFragment_RawId";
    public static final String KEY_PHOTO_URL = "NewRecipientFragment_PhotoUrl";
    public static final String KEY_ERROR = "NewRecipientFragment_Error";

    public static final int ERR_INVALID_NAME = 1;
    public static final int ERR_INVALID_VIA = 2;
    public static final int ERR_INVALID_EMAIL = 3;
    public static final int ERR_INVALID_PHONE = 4;
    public static final int ERR_UNABLE_TO_ADD = 5;


    public static Bundle insertRecipientContact(Context context, long rawId, String name, String phone, String email) {
        Bundle bundle = new Bundle();

        name = name == null ? "" : name.trim();
        phone = phone == null ? "" : phone.trim();
        email = email == null ? "" : email.trim();

        // validate display name
        if(name.length() <= 0) {
            bundle.putInt(KEY_ERROR, ERR_INVALID_NAME);
            return bundle;
        }

        // validate one of email or phone
        if(email.length() <= 0 && phone.length() <= 0) {
            bundle.putInt(KEY_ERROR, ERR_INVALID_VIA);
            return bundle;
        }

        // validate email
        if(email.length() > 0 && !Utils.isValidEmail(email)) {
            bundle.putInt(KEY_ERROR, ERR_INVALID_EMAIL);
            return bundle;
        }

        if(phone.length() > 0 && !Utils.isValidPhoneNumber(phone)) {
            bundle.putInt(KEY_ERROR, ERR_INVALID_PHONE);
            return bundle;
        }

        ArrayList<ContentProviderOperation> ops = new ArrayList<>();

        // create raw contact
        if(rawId <= 0) {
            ops.add(ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null).build());
        }

        // add display name data
        ContentProviderOperation.Builder op = null;
        if(rawId <= 0) {
            op = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0);
        } else {
            ops.add(ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
                    .withSelection(ContactsContract.Data.RAW_CONTACT_ID + "=? AND " + ContactsContract.Data.MIMETYPE + "=?",
                            new String[]{String.valueOf(rawId), ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE})
                    .build());

            op = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawId);
        }
        ops.add(op.withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name).build());


        // add email data
        if(email.length() > 0) {
            List<String> mimeTypes = new ArrayList<>();
            mimeTypes.add(ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE);
            FilteredCursor checkCursor = (FilteredCursor) RecipientAdapterUtils.getRecipientsCursor(context, null, null, null, mimeTypes, email);
            boolean alreadyHasEmail = checkCursor != null && checkCursor.getOriginalCursor() != null && checkCursor.getOriginalCursor().getCount() > 0;

            if(!alreadyHasEmail) {
                if (rawId <= 0) {
                    op = ContentProviderOperation
                            .newInsert(ContactsContract.Data.CONTENT_URI)
                            .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0);
                } else {
                    op = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                            .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawId);
                }
                ops.add(op.withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.Data.DATA1, email).build());
            }
        }

        if(phone.length() > 0) {
            List<String> mimeTypes = new ArrayList<>();
            mimeTypes.add(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE);
            FilteredCursor checkCursor = (FilteredCursor) RecipientAdapterUtils.getRecipientsCursor(context, null, null, null, mimeTypes, phone);
            boolean alreadyHasPhone = checkCursor != null && checkCursor.getOriginalCursor() != null && checkCursor.getOriginalCursor().getCount() > 0;

            if(!alreadyHasPhone) {
                if (rawId <= 0) {
                    op = ContentProviderOperation
                            .newInsert(ContactsContract.Data.CONTENT_URI)
                            .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0);
                } else {
                    op = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                            .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawId);
                }
                ops.add(op.withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.Data.DATA1, phone).build());
            }
        }

        try {
            ContentProviderResult[] res = context.getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
            if(res.length < ops.size()) {
                throw new RuntimeException("Not all operations were performed while trying to insert contact: Total Ops = " + ops.size() + "; Performed = " + res.length);
            }

            bundle.putString(KEY_NAME, name);
            bundle.putString(KEY_VIA, email.length() > 0 ? email : phone);
            bundle.putString(KEY_MAIL, email);
            bundle.putString(KEY_PHONE, phone);
        } catch (Throwable e) {
            bundle.putInt(KEY_ERROR, ERR_UNABLE_TO_ADD);
            Log.d(NewRecipientFragment.class.getSimpleName(), "Unable to add contact", e);
            Crashlytics.logException(e);
        }

        return bundle;
    }

    private PepperMintPreferences mPreferences;
    private CustomActionBarActivity mActivity;

    private EditText mTxtFirstName, mTxtLastName, mTxtPhone, mTxtMail;
    private Button mBtnSave;

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
        PeppermintApp app = (PeppermintApp) mActivity.getApplication();

        mPopup = new Popup(mActivity);

        mBtnSave = (Button) mActivity.getCustomActionBar().findViewById(R.id.btnSave);
        mBtnSave.setOnClickListener(this);

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

        mTxtFirstName = (EditText) v.findViewById(R.id.txtFirstName);
        mTxtLastName = (EditText) v.findViewById(R.id.txtLastName);
        mTxtMail = (EditText) v.findViewById(R.id.txtEmail);
        mTxtPhone = (EditText) v.findViewById(R.id.txtPhoneNumber);

        mTxtFirstName.setTypeface(app.getFontRegular());
        mTxtLastName.setTypeface(app.getFontRegular());
        mTxtMail.setTypeface(app.getFontRegular());
        mTxtPhone.setTypeface(app.getFontRegular());

        Bundle args = getArguments();
        if(args != null) {
            String via = args.getString(KEY_VIA, null);
            String name = args.getString(KEY_NAME, null);

            if(name != null) {
                String[] names = name.split("\\s+");

                if(names.length > 1) {
                    String lastName = names[names.length - 1];
                    mTxtLastName.setText(lastName);
                    mTxtLastName.setSelection(lastName.length());

                    String firstName = name.substring(0, name.length() - names[names.length - 1].length()).trim();
                    mTxtFirstName.setText(firstName);
                    mTxtFirstName.setSelection(firstName.length());
                } else {
                    mTxtFirstName.setText(name);
                    mTxtFirstName.setSelection(name.length());
                }
            }

            if(via != null) {
                if(Utils.isValidPhoneNumber(via)) {
                    mTxtPhone.setText(via);
                    mTxtPhone.setSelection(via.length());
                } else {
                    mTxtMail.setText(via);
                    mTxtMail.setSelection(via.length());
                }
            }
        }

        return v;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
        long rawId = 0;
        if(getArguments() != null) {
            rawId = getArguments().getLong(KEY_RAW_ID, 0);
        }

        String name = (mTxtFirstName.getText().toString().trim() + " " + mTxtLastName.getText().toString().trim()).trim();
        String phone = mTxtPhone.getText().toString().trim();
        String email = mTxtMail.getText().toString().trim();

        Bundle bundle = insertRecipientContact(mActivity, rawId, name, phone, email);

        if(bundle.containsKey(KEY_ERROR)) {
            switch(bundle.getInt(KEY_ERROR)) {
                case ERR_INVALID_EMAIL:
                    Toast.makeText(mActivity, R.string.msg_message_invalid_contactmail, Toast.LENGTH_LONG).show();
                    return;
                case ERR_INVALID_NAME:
                    Toast.makeText(mActivity, R.string.msg_message_invalid_contactname, Toast.LENGTH_LONG).show();
                    return;
                case ERR_INVALID_PHONE:
                    Toast.makeText(mActivity, R.string.msg_message_invalid_contactphone, Toast.LENGTH_LONG).show();
                    return;
                case ERR_INVALID_VIA:
                    Toast.makeText(mActivity, R.string.msg_message_invalid_contactvia, Toast.LENGTH_LONG).show();
                    return;
                case ERR_UNABLE_TO_ADD:
                    Toast.makeText(mActivity, R.string.msg_message_unable_addcontact, Toast.LENGTH_LONG).show();
                    return;
            }

            return;
        }

        Toast.makeText(mActivity, R.string.msg_message_contact_added, Toast.LENGTH_LONG).show();
        Intent intent = new Intent();
        intent.putExtras(bundle);

        mActivity.setResult(Activity.RESULT_OK, intent);
        mActivity.finish();
    }

}
