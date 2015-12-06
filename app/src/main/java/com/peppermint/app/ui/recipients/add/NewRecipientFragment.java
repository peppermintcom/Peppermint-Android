package com.peppermint.app.ui.recipients.add;

import android.app.Activity;
import android.app.Fragment;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.peppermint.app.PeppermintApp;
import com.peppermint.app.R;
import com.peppermint.app.ui.CustomActionBarActivity;
import com.peppermint.app.ui.recipients.RecipientAdapterUtils;
import com.peppermint.app.ui.views.NavigationItem;
import com.peppermint.app.ui.views.NavigationListAdapter;
import com.peppermint.app.ui.views.dialogs.CustomListDialog;
import com.peppermint.app.ui.views.simple.CustomToast;
import com.peppermint.app.utils.FilteredCursor;
import com.peppermint.app.utils.Popup;
import com.peppermint.app.utils.Utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Created by Nuno Luz on 10-11-2015.
 */
public class NewRecipientFragment extends Fragment implements View.OnClickListener, AdapterView.OnItemClickListener {

    private static final String TAG = NewRecipientFragment.class.getSimpleName();

    public static final String KEY_VIA = TAG + "_Via";
    public static final String KEY_NAME = TAG + "_Name";
    public static final String KEY_PHONE = TAG + "_Phone";
    public static final String KEY_MAIL = TAG + "_Mail";
    public static final String KEY_RAW_ID = TAG + "_RawId";
    public static final String KEY_PHOTO_URL = TAG + "_PhotoUrl";
    public static final String KEY_ERROR = TAG + "_Error";

    public static final int ERR_INVALID_NAME = 1;
    public static final int ERR_INVALID_VIA = 2;
    public static final int ERR_INVALID_EMAIL = 3;
    public static final int ERR_INVALID_PHONE = 4;
    public static final int ERR_UNABLE_TO_ADD = 5;

    /**
     * Inserts a new contact using the global contacts content provider.<br />
     * If rawId is supplied, will update the contact with the supplied information.<br />
     * One of email or phone is mandatory.
     *
     * @param context the context
     * @param rawId the already existent rawId (0 for a new contact)
     * @param name the contact name (mandatory)
     * @param phone the phone number
     * @param email the email address
     * @return a {@link Bundle} with results (can be passed on to an {@link Intent}
     */
    public static Bundle insertRecipientContact(Context context, long rawId, String name, String phone, String email, String photoUrl) {
        Bundle bundle = new Bundle();

        name = name == null ? "" : Utils.capitalizeFully(name.trim());
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

                // not including phone type will crash on HTC devices
                ops.add(op.withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_OTHER)
                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.Data.DATA1, phone).build());
            }
        }

        if(photoUrl != null && photoUrl.length() > 0) {

            int dp200 = Utils.dpToPx(context, 200);
            int dp100 = Utils.dpToPx(context, 100);
            // scale image to 200dp to save memory
            Bitmap realImage = photoUrl.startsWith(FILE_SCHEME) ? Utils.getScaledBitmap(photoUrl.substring(6), dp200, dp200)
                    : Utils.getScaledBitmap(context, Uri.parse(photoUrl), dp200, dp200);

            if(realImage != null) {
                // rotate image according to the photo params
                Bitmap rotatedImage = Utils.getRotatedBitmapFromFileAttributes(realImage, photoUrl.startsWith(FILE_SCHEME) ? photoUrl.substring(6) : photoUrl);
                // cut a square thumbnail
                rotatedImage = ThumbnailUtils.extractThumbnail(rotatedImage, dp100, dp100, ThumbnailUtils.OPTIONS_RECYCLE_INPUT);

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                rotatedImage.compress(Bitmap.CompressFormat.PNG, 100, baos);

                if (rawId <= 0) {
                    op = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                            .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0);
                } else {
                    ops.add(ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
                            .withSelection(ContactsContract.Data.RAW_CONTACT_ID + "=? AND " + ContactsContract.Data.MIMETYPE + "=?",
                                    new String[]{String.valueOf(rawId), ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE})
                            .build());

                    op = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                            .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawId);
                }

                ops.add(op.withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.Photo.PHOTO, baos.toByteArray()).build());
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

    private static final String FILE_SCHEME = "file:/";
    private static final String SAVED_DIALOG_STATE_KEY = TAG + "_NewAvatarDialogState";
    private static final String SAVED_AVATAR_URL_KEY = TAG + "_AvatarUrl";
    private static final String SAVED_AVATAR_INPROGRESS_URL_KEY = TAG + "_AvatarInProgressUrl";
    private static final int TAKE_PHOTO_CODE = 123;
    private static final int CHOOSE_PHOTO_CODE = 124;

    private CustomActionBarActivity mActivity;

    private ImageView mBtnAddAvatar;
    private EditText mTxtFirstName, mTxtLastName, mTxtPhone, mTxtMail;
    private Button mBtnSave;

    private Popup mPopup;
    private CustomListDialog mNewAvatarDialog;
    private NavigationListAdapter mAvatarAdapter;
    private List<NavigationItem> mAvatarOptions;
    private String mAvatarUrl, mAvatarInProgressUrl;

    private TextWatcher mTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) { /* nothing to do here */ }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) { /* nothing to do here */ }

        @Override
        public void afterTextChanged(Editable s) {
            if(isValid()) {
                mBtnSave.setVisibility(View.VISIBLE);
            } else {
                mBtnSave.setVisibility(View.INVISIBLE);
            }
        }
    };

    private View.OnClickListener mAvatarClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if(mNewAvatarDialog != null && !mNewAvatarDialog.isShowing()) {
                mNewAvatarDialog.show();
            }
        }
    };

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if(mNewAvatarDialog != null && mNewAvatarDialog.isShowing()) {
            mNewAvatarDialog.dismiss();
        }

        switch (position) {
            case 0:
                // take photo
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                // ensure that there's a camera activity to handle the intent
                if (takePictureIntent.resolveActivity(mActivity.getPackageManager()) != null) {
                    File photoFile = null;
                    try {
                        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
                        String imageFileName = "PeppermintAvatar_" + timeStamp + "_";
                        File storageDir = mActivity.getApplicationContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
                        photoFile = File.createTempFile(imageFileName, ".jpg", storageDir);
                        mAvatarInProgressUrl = photoFile.getAbsolutePath();
                    } catch (IOException ex) {
                        Crashlytics.logException(ex);
                        Log.e(TAG, "Unable to create image file!", ex);
                        CustomToast.makeText(mActivity, R.string.msg_message_external_storage_error, Toast.LENGTH_LONG).show();
                    }

                    if (photoFile != null) {
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
                        startActivityForResult(takePictureIntent, TAKE_PHOTO_CODE);
                    }
                } else {
                    CustomToast.makeText(mActivity, R.string.msg_message_no_camera_app, Toast.LENGTH_LONG).show();
                }
                break;
            default:
                // choose photo
                Intent getPictureIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(getPictureIntent, CHOOSE_PHOTO_CODE);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case TAKE_PHOTO_CODE:
                if(resultCode == Activity.RESULT_OK) {
                    setAvatarUrl(FILE_SCHEME + mAvatarInProgressUrl);
                } else {
                    File file = new File(mAvatarInProgressUrl);
                    if(file.exists()) {
                        file.delete();
                    }
                }
                break;
            case CHOOSE_PHOTO_CODE:
                if(resultCode == Activity.RESULT_OK) {
                    Uri selectedImage = data.getData();
                    String[] filePathColumn = {MediaStore.Images.Media.DATA};
                    Cursor cursor = mActivity.getContentResolver().query(selectedImage, filePathColumn, null, null, null);
                    if (cursor.moveToFirst()) {
                        int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                        setAvatarUrl(FILE_SCHEME + cursor.getString(columnIndex));
                    }
                    cursor.close();
                }
                break;
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onAttach(Activity context) {
        super.onAttach(context);
        mActivity = (CustomActionBarActivity) context;
    }

    private boolean isValid() {
        String firstName = mTxtFirstName.getText().toString().trim();
        String lastName = mTxtLastName.getText().toString().trim();
        String email = mTxtMail.getText().toString().trim();
        String phone = mTxtPhone.getText().toString().trim();

        if(firstName.length() <= 0 && lastName.length() <= 0) {
            return false;
        }

        boolean invalidEmail = email.length() <= 0 || !Utils.isValidEmail(email);
        boolean invalidPhone = phone.length() <= 0 || !Utils.isValidPhoneNumber(phone);

        return !(invalidEmail && invalidPhone);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        PeppermintApp app = (PeppermintApp) mActivity.getApplication();

        mPopup = new Popup(mActivity);

        mAvatarOptions = new ArrayList<>();
        mAvatarOptions.add(new NavigationItem(getString(R.string.take_photo), R.drawable.ic_drawer_camera, null, true));
        mAvatarOptions.add(new NavigationItem(getString(R.string.choose_photo), R.drawable.ic_drawer_collections, null, true));

        mNewAvatarDialog = new CustomListDialog(mActivity);
        mNewAvatarDialog.setCancelable(true);
        mNewAvatarDialog.setTitleText(R.string.change_photo);
        mNewAvatarDialog.setListOnItemClickListener(this);
        mAvatarAdapter = new NavigationListAdapter(mActivity, mAvatarOptions, app.getFontSemibold());
        mNewAvatarDialog.setListAdapter(mAvatarAdapter);

        mBtnSave = (Button) mActivity.getCustomActionBar().findViewById(R.id.btnSave);
        mBtnSave.setOnClickListener(this);

        // global touch interceptor to hide keyboard
        mActivity.getTouchInterceptor().setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                mPopup.dismiss();
                return false;
            }
        });

        // inflate the view
        View v = inflater.inflate(R.layout.f_newcontact, container, false);

        mBtnAddAvatar = (ImageView) v.findViewById(R.id.imgAddAvatar);
        mBtnAddAvatar.setOnClickListener(mAvatarClickListener);

        mTxtFirstName = (EditText) v.findViewById(R.id.txtFirstName);
        mTxtLastName = (EditText) v.findViewById(R.id.txtLastName);
        mTxtMail = (EditText) v.findViewById(R.id.txtEmail);
        mTxtPhone = (EditText) v.findViewById(R.id.txtPhoneNumber);

        mTxtFirstName.setTypeface(app.getFontRegular());
        mTxtLastName.setTypeface(app.getFontRegular());
        mTxtMail.setTypeface(app.getFontRegular());
        mTxtPhone.setTypeface(app.getFontRegular());

        mTxtFirstName.addTextChangedListener(mTextWatcher);
        mTxtLastName.addTextChangedListener(mTextWatcher);
        mTxtMail.addTextChangedListener(mTextWatcher);
        mTxtPhone.addTextChangedListener(mTextWatcher);

        Bundle args = getArguments();
        if(args != null) {
            String via = args.getString(KEY_VIA, null);
            String name = args.getString(KEY_NAME, null);
            mAvatarUrl = args.getString(KEY_PHOTO_URL, null);

            if(name != null) {
                name = Utils.capitalizeFully(name);
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

        if(savedInstanceState != null) {
            Bundle dialogState = savedInstanceState.getBundle(SAVED_DIALOG_STATE_KEY);
            if (dialogState != null) {
                mNewAvatarDialog.onRestoreInstanceState(dialogState);
            }
            mAvatarUrl = savedInstanceState.getString(SAVED_AVATAR_URL_KEY, null);
            mAvatarInProgressUrl = savedInstanceState.getString(SAVED_AVATAR_INPROGRESS_URL_KEY, null);
        }

        setAvatarUrl(mAvatarUrl);

        return v;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        Bundle dialogState = mNewAvatarDialog.onSaveInstanceState();
        if (dialogState != null) {
            outState.putBundle(SAVED_DIALOG_STATE_KEY, dialogState);
        }
        outState.putString(SAVED_AVATAR_URL_KEY, mAvatarUrl);
        outState.putString(SAVED_AVATAR_INPROGRESS_URL_KEY, mAvatarInProgressUrl);
        super.onSaveInstanceState(outState);
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
        if(mNewAvatarDialog != null && mNewAvatarDialog.isShowing()) {
            mNewAvatarDialog.dismiss();
        }
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

        Bundle bundle = insertRecipientContact(mActivity, rawId, name, phone, email, mAvatarUrl);

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

    public void setAvatarUrl(String url) {
        String prevUrl = this.mAvatarUrl;
        this.mAvatarUrl = url;

        if(mAvatarUrl != null) {
            int dp70 = Utils.dpToPx(mActivity, 70);
            int dp150 = Utils.dpToPx(mActivity, 150);
            Bitmap realImage = mAvatarUrl.startsWith(FILE_SCHEME) ? Utils.getScaledBitmap(mAvatarUrl.substring(6), dp150, dp150)
                    : Utils.getScaledBitmap(mActivity, Uri.parse(mAvatarUrl), dp150, dp150);
            if(realImage != null) {
                Bitmap rotatedImage = Utils.getRotatedBitmapFromFileAttributes(realImage, mAvatarUrl.startsWith(FILE_SCHEME) ? mAvatarUrl.substring(6) : mAvatarUrl);
                rotatedImage = ThumbnailUtils.extractThumbnail(rotatedImage, dp70, dp70, ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
                mBtnAddAvatar.setImageBitmap(rotatedImage);
            } else {
                this.mAvatarUrl = prevUrl;
            }
        }
    }

}
