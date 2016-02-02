package com.peppermint.app.ui.chat;

import android.app.Activity;
import android.app.Fragment;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.peppermint.app.PeppermintApp;
import com.peppermint.app.R;
import com.peppermint.app.sending.SenderPreferences;
import com.peppermint.app.tracking.TrackerManager;
import com.peppermint.app.ui.CustomActionBarActivity;
import com.peppermint.app.ui.recipients.RecipientAdapterUtils;
import com.peppermint.app.ui.views.NavigationItem;
import com.peppermint.app.ui.views.NavigationListAdapter;
import com.peppermint.app.ui.views.dialogs.CustomListDialog;
import com.peppermint.app.ui.views.simple.CustomToast;
import com.peppermint.app.ui.views.simple.CustomValidatedEditText;
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
 *
 * New recipient/contact fragment.
 */
public class ChatFragment extends Fragment implements View.OnClickListener, AdapterView.OnItemClickListener {

    private static final String TAG = ChatFragment.class.getSimpleName();

    private static final String SCREEN_ID = "NewContact";
    private static final String PEPPERMINT_GROUP_TITLE = "Peppermint";

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
     * Create recipient/contact data at the specified google account.
     * @param context the contact
     * @param rawId the already existent rawId (0 for a new contact)
     * @param fullName the new contact full name
     * @param phone the new contact phone
     * @param email the new contact email
     * @param photoUri the new contact photo URI
     * @param googleAccountName the google account
     * @return a {@link Bundle} with results (can be passed on to an {@link Intent}
     */
    public static Bundle insertRecipientContact(Context context, long rawId, String fullName, String phone, String email, Uri photoUri, String googleAccountName) {
        String[] names = Utils.getFirstAndLastNames(fullName);
        return insertRecipientContact(context, rawId, names[0], names[1], phone, email, photoUri, googleAccountName);
    }

    /**
     * Inserts a new contact using the global contacts content provider.<br />
     * If rawId is supplied, will update the contact with the supplied information.<br />
     * One of email or phone is mandatory.
     *
     * @param context the context
     * @param rawId the already existent rawId (0 for a new contact)
     * @param firstName the contact given name (mandatory one of firstName or lastName)
     * @param lastName the contact family name
     * @param phone the phone number
     * @param email the email address
     * @param photoUri the contact photo URI
     * @param googleAccountName the google account name to insert the contact
     * @return a {@link Bundle} with results (can be passed on to an {@link Intent}
     */
    public static Bundle insertRecipientContact(Context context, long rawId, String firstName, String lastName, String phone, String email, Uri photoUri, String googleAccountName) {
        Bundle bundle = new Bundle();

        firstName = firstName == null ? "" : Utils.capitalizeFully(firstName.trim());
        lastName = lastName == null ? "" : Utils.capitalizeFully(lastName.trim());
        phone = phone == null ? "" : phone.trim();
        email = email == null ? "" : email.trim();

        // validate display name
        if(firstName.length() <= 0 && lastName.length() <= 0) {
            bundle.putInt(KEY_ERROR, ERR_INVALID_NAME);
            return bundle;
        }
        if(!Utils.isValidName(firstName) || !Utils.isValidNameMaybeEmpty(lastName)) {
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

        // validate phone number
        if(phone.length() > 0 && !Utils.isValidPhoneNumber(phone)) {
            bundle.putInt(KEY_ERROR, ERR_INVALID_PHONE);
            return bundle;
        }

        ArrayList<ContentProviderOperation> ops = new ArrayList<>();

        // create raw contact
        if(rawId <= 0) {
            ops.add(ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, "com.google")
                    .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, googleAccountName).build());

            // check if Peppermint group exists; create it if not
            // we could add to the default group "My Contacts" but it might not exist
            // also, the default operation doesn't necessarily add the contact to a group
            // causing the GROUP_VISIBLE flag to be 0
            Long groupId = null;
            Cursor groupCursor = context.getContentResolver().query(
                    ContactsContract.Groups.CONTENT_URI,
                    new String[]{ContactsContract.Groups._ID},
                    ContactsContract.Groups.GROUP_VISIBLE + "=1 AND " + ContactsContract.Groups.ACCOUNT_NAME + "=" + DatabaseUtils.sqlEscapeString(googleAccountName) +
                    " AND " + ContactsContract.Groups.TITLE + "=" + DatabaseUtils.sqlEscapeString(PEPPERMINT_GROUP_TITLE), null,
                    null
            );
            if(groupCursor.moveToNext()) {
                groupId = groupCursor.getLong(groupCursor.getColumnIndex(ContactsContract.Groups._ID));
            } else {
                ContentValues groupValues = new ContentValues();
                groupValues.put(ContactsContract.Groups.TITLE, PEPPERMINT_GROUP_TITLE);
                groupValues.put(ContactsContract.Groups.GROUP_VISIBLE, 1);
                groupValues.put(ContactsContract.Groups.ACCOUNT_NAME, googleAccountName);
                groupValues.put(ContactsContract.Groups.ACCOUNT_TYPE, "com.google");
                Uri groupUri = context.getContentResolver().insert(ContactsContract.Groups.CONTENT_URI, groupValues);
                groupId = ContentUris.parseId(groupUri);
            }
            groupCursor.close();

            ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    .withValue(ContactsContract.Data.DATA1, groupId)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE).build());
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
                .withValue(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, firstName)
                .withValue(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME, lastName).build());


        // add email data
        if(email.length() > 0) {
            boolean alreadyHasEmail = false;

            if(rawId > 0) {
                List<Long> rawIds = new ArrayList<>();
                List<String> mimeTypes = new ArrayList<>();
                rawIds.add(rawId);
                mimeTypes.add(ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE);
                Cursor checkCursor = RecipientAdapterUtils.getRecipientsCursor(context, rawIds, mimeTypes, email);
                alreadyHasEmail = checkCursor != null && checkCursor.getCount() > 0;
            }

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
            boolean alreadyHasPhone = false;

            if(rawId > 0) {
                List<Long> rawIds = new ArrayList<>();
                rawIds.add(rawId);
                List<String> mimeTypes = new ArrayList<>();
                mimeTypes.add(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE);
                Cursor checkCursor = RecipientAdapterUtils.getRecipientsCursor(context, rawIds, mimeTypes, phone);
                alreadyHasPhone = checkCursor != null && checkCursor.getCount() > 0;
            }

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

        if(photoUri != null) {

            int dp200 = Utils.dpToPx(context, 200);
            int dp100 = Utils.dpToPx(context, 100);
            // scale image to 200dp to save memory
            Bitmap realImage = /*photoUrl.startsWith(FILE_SCHEME) ? Utils.getScaledBitmap(photoUrl.substring(6), dp200, dp200)
                    : */Utils.getScaledBitmap(context, photoUri, dp200, dp200);

            if(realImage != null) {
                // cut a square thumbnail
                Bitmap rotatedImage = ThumbnailUtils.extractThumbnail(realImage, dp100, dp100, ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
                if(FILE_SCHEME.startsWith(photoUri.getScheme())) {
                    // rotate image according to the photo params
                    rotatedImage = Utils.getRotatedBitmapFromFileAttributes(rotatedImage, photoUri.toString().substring(6));
                }

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

            bundle.putString(KEY_NAME, firstName + " " + lastName);
            bundle.putString(KEY_VIA, email.length() > 0 ? email : phone);
            bundle.putString(KEY_MAIL, email);
            bundle.putString(KEY_PHONE, phone);
        } catch (Throwable e) {
            bundle.putInt(KEY_ERROR, ERR_UNABLE_TO_ADD);
            Log.d(TAG, "Unable to add contact", e);
            TrackerManager.getInstance(context.getApplicationContext()).logException(e);
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
    private SenderPreferences mPreferences;

    private ImageView mBtnAddAvatar;
    private CustomValidatedEditText mTxtFirstName, mTxtLastName, mTxtPhone, mTxtMail;
    private Button mBtnSave;

    private CustomListDialog mNewAvatarDialog;
    private NavigationListAdapter mAvatarAdapter;
    private List<NavigationItem> mAvatarOptions;
    private Uri mAvatarUrl, mAvatarInProgressUrl;

    private CustomValidatedEditText.Validator mFirstNameValidator = new CustomValidatedEditText.Validator() {
        @Override
        public String getValidatorMessage(CharSequence text) {
            String name = text.toString().trim();
            if(!Utils.isValidName(name)) {
                mBtnSave.setEnabled(false);
                return getString(R.string.msg_insert_first_name);
            }

            return null;
        }
    };

    private CustomValidatedEditText.Validator mLastNameValidator = new CustomValidatedEditText.Validator() {
        @Override
        public String getValidatorMessage(CharSequence text) {
            String name = text.toString().trim();
            if(!Utils.isValidNameMaybeEmpty(name)) {
                mBtnSave.setEnabled(false);
                return getString(R.string.msg_insert_last_name);
            }

            return null;
        }
    };

    private CustomValidatedEditText.Validator mEmailValidator = new CustomValidatedEditText.Validator() {
        @Override
        public String getValidatorMessage(CharSequence text) {
            String email = mTxtMail.getText().toString().trim();
            String phone = mTxtPhone.getText().toString().trim();

            if(!Utils.isValidEmail(email) && !(TextUtils.isEmpty(email) && Utils.isValidPhoneNumber(phone))) {
                mBtnSave.setEnabled(false);
                return getString(R.string.msg_insert_mail);
            }

            return null;
        }
    };

    private CustomValidatedEditText.Validator mPhoneValidator = new CustomValidatedEditText.Validator() {
        @Override
        public String getValidatorMessage(CharSequence text) {
            String email = mTxtMail.getText().toString().trim();
            String phone = mTxtPhone.getText().toString().trim();

            if(!Utils.isValidPhoneNumber(phone) && !(TextUtils.isEmpty(phone) && Utils.isValidEmail(email))) {
                mBtnSave.setEnabled(false);
                return getString(R.string.msg_insert_phone);
            }

            return null;
        }
    };

    private CustomValidatedEditText.OnValidityChangeListener mValidityChangeListener = new CustomValidatedEditText.OnValidityChangeListener() {
        @Override
        public void onValidityChange(boolean isValid) {
            if(mValidityChecker.areValid()) {
                mBtnSave.setEnabled(true);
            } else {
                mBtnSave.setEnabled(false);
            }
        }
    };

    private CustomValidatedEditText.ValidityChecker mValidityChecker;

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
                        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.getDefault()).format(new Date());
                        String imageFileName = "PeppermintAvatar_" + timeStamp + "_";
                        File storageDir = new File(Environment.getExternalStoragePublicDirectory(
                                Environment.DIRECTORY_PICTURES), "Peppermint");
                        if(!storageDir.isDirectory() && storageDir.canWrite()) {
                            storageDir.delete();
                        }
                        if(!storageDir.exists()) {
                            storageDir.mkdirs();
                        }
                        /*File storageDir = mActivity.getApplicationContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);*/
                        photoFile = new File(storageDir, imageFileName + ".jpg");
                        photoFile.createNewFile();
                        mAvatarInProgressUrl = Uri.parse(FILE_SCHEME + photoFile.getAbsolutePath());
                    } catch (IOException ex) {
                        TrackerManager.getInstance(mActivity.getApplicationContext()).logException(ex);
                        Log.e(TAG, "Unable to create image file!", ex);
                        CustomToast.makeText(mActivity, R.string.msg_external_storage_error, Toast.LENGTH_LONG).show();
                    }

                    if (photoFile != null) {
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
                        startActivityForResult(takePictureIntent, TAKE_PHOTO_CODE);
                    }
                } else {
                    CustomToast.makeText(mActivity, R.string.msg_no_camera_app, Toast.LENGTH_LONG).show();
                }
                break;
            default:
                // choose photo
                /*Intent getPictureIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(getPictureIntent, CHOOSE_PHOTO_CODE)*/;

                startActivityForResult(
                        Intent.createChooser(
                                new Intent(Intent.ACTION_GET_CONTENT)
                                        .setType("image/*"), getString(R.string.choose_photo)),
                        CHOOSE_PHOTO_CODE);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case TAKE_PHOTO_CODE:
                if(mAvatarInProgressUrl != null) {
                    if (resultCode == Activity.RESULT_OK) {
                        setAvatarUrl(mAvatarInProgressUrl);
                    } else if(FILE_SCHEME.startsWith(mAvatarInProgressUrl.getScheme())) {
                        File file = new File(mAvatarInProgressUrl.toString().substring(6));
                        if (file.exists()) {
                            file.delete();
                        }
                    }
                }
                break;
            case CHOOSE_PHOTO_CODE:
                if(resultCode == Activity.RESULT_OK) {
                    if(data == null) {
                        return;
                    }

                    Uri selectedImage = data.getData();
                    if(selectedImage == null) {
                        return;
                    }

                    /*if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        final int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION & data.getFlags();
                        mActivity.getContentResolver().takePersistableUriPermission(selectedImage, takeFlags);
                    }*/

                    setAvatarUrl(selectedImage);


                    /*String[] filePathColumn = {MediaStore.Images.Media.DATA};
                    Cursor cursor;

                    if(Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
                        // will return "image:x*"
                        String wholeID = DocumentsContract.getDocumentId(selectedImage);
                        String id = wholeID.split(":")[1];
                        String sel = MediaStore.Images.Media._ID + "=?";

                        cursor = mActivity.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                filePathColumn, sel, new String[]{ id }, null);
                    } else {
                        cursor = mActivity.getContentResolver().query(selectedImage, filePathColumn, null, null, null);
                    }

                    if (cursor.moveToFirst()) {
                        int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                        setAvatarUrl(FILE_SCHEME + cursor.getString(columnIndex));
                    }
                    cursor.close();*/
                }
                break;
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onAttach(Activity context) {
        super.onAttach(context);
        mActivity = (CustomActionBarActivity) context;
        mPreferences = new SenderPreferences(context);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        PeppermintApp app = (PeppermintApp) mActivity.getApplication();

        mAvatarOptions = new ArrayList<>();
        mAvatarOptions.add(new NavigationItem(getString(R.string.take_photo), R.drawable.ic_drawer_camera, null, true));
        mAvatarOptions.add(new NavigationItem(getString(R.string.choose_photo), R.drawable.ic_drawer_collections, null, true));

        mNewAvatarDialog = new CustomListDialog(mActivity);
        mNewAvatarDialog.setCancelable(true);
        mNewAvatarDialog.setNegativeButtonText(R.string.cancel);
        mNewAvatarDialog.setNegativeButtonListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mNewAvatarDialog.dismiss();
            }
        });
        mNewAvatarDialog.setTitleText(R.string.select_photo);
        mNewAvatarDialog.setListOnItemClickListener(this);
        mAvatarAdapter = new NavigationListAdapter(mActivity, mAvatarOptions, app.getFontSemibold());
        mNewAvatarDialog.setListAdapter(mAvatarAdapter);

        mBtnSave = (Button) mActivity.getCustomActionBar().findViewById(R.id.btnSave);
        mBtnSave.setOnClickListener(this);

        // inflate the view
        View v = inflater.inflate(R.layout.f_newcontact, container, false);

        mBtnAddAvatar = (ImageView) v.findViewById(R.id.imgAddAvatar);
        mBtnAddAvatar.setOnClickListener(mAvatarClickListener);

        mTxtFirstName = (CustomValidatedEditText) v.findViewById(R.id.txtFirstName);
        mTxtLastName = (CustomValidatedEditText) v.findViewById(R.id.txtLastName);
        mTxtMail = (CustomValidatedEditText) v.findViewById(R.id.txtEmail);
        mTxtPhone = (CustomValidatedEditText) v.findViewById(R.id.txtPhoneNumber);

        mTxtFirstName.setBackgroundResource(R.drawable.background_border_bottom);
        mTxtMail.setBackgroundResource(R.drawable.background_border_bottom);
        mTxtPhone.setBackgroundResource(R.drawable.background_border_bottom);

        mTxtFirstName.setValidator(mFirstNameValidator);
        mTxtLastName.setValidator(mLastNameValidator);
        mTxtMail.setValidator(mEmailValidator);
        mTxtPhone.setValidator(mPhoneValidator);
        mTxtMail.setLinkedEditText(mTxtPhone);

        mTxtFirstName.setOnValidityChangeListener(mValidityChangeListener);
        mTxtLastName.setOnValidityChangeListener(mValidityChangeListener);
        mTxtMail.setOnValidityChangeListener(mValidityChangeListener);
        mTxtPhone.setOnValidityChangeListener(mValidityChangeListener);

        mValidityChecker = new CustomValidatedEditText.ValidityChecker(mTxtFirstName, mTxtLastName, mTxtMail, mTxtPhone);

        Bundle args = getArguments();
        if(args != null) {
            String via = args.getString(KEY_VIA, null);
            String name = args.getString(KEY_NAME, null);
            mAvatarUrl = args.getParcelable(KEY_PHOTO_URL);

            if(name != null) {
                String[] names = Utils.getFirstAndLastNames(name);

                mTxtFirstName.setText(names[0]);
                mTxtFirstName.setSelection(names[0].length());

                mTxtLastName.setText(names[1]);
                mTxtLastName.setSelection(names[1].length());
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
            mAvatarUrl = savedInstanceState.getParcelable(SAVED_AVATAR_URL_KEY);
            mAvatarInProgressUrl = savedInstanceState.getParcelable(SAVED_AVATAR_INPROGRESS_URL_KEY);
        }

        setAvatarUrl(mAvatarUrl);

        return v;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        Bundle dialogState = mNewAvatarDialog.onSaveInstanceState();
        if (dialogState != null) {
            outState.putBundle(SAVED_DIALOG_STATE_KEY, dialogState);
        }
        outState.putParcelable(SAVED_AVATAR_URL_KEY, mAvatarUrl);
        outState.putParcelable(SAVED_AVATAR_INPROGRESS_URL_KEY, mAvatarInProgressUrl);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onResume() {
        super.onResume();

        mTxtFirstName.validate();
        mTxtLastName.validate();
        mTxtMail.validate();
        mTxtPhone.validate();

        Utils.showKeyboard(mActivity, WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        TrackerManager.getInstance(getActivity().getApplicationContext()).trackScreenView(SCREEN_ID);
    }

    @Override
    public void onPause() {
        Utils.hideKeyboard(mActivity, WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        super.onPause();
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

        String firstName = mTxtFirstName.getText().toString();
        String lastName = mTxtLastName.getText().toString();
        String phone = mTxtPhone.getText().toString().trim();
        String email = mTxtMail.getText().toString().trim();

        Bundle bundle = insertRecipientContact(mActivity, rawId, firstName, lastName, phone, email, mAvatarUrl, mPreferences.getGmailSenderPreferences().getPreferredAccountName());

        if(bundle.containsKey(KEY_ERROR)) {
            switch(bundle.getInt(KEY_ERROR)) {
                case ERR_INVALID_EMAIL:
                    Toast.makeText(mActivity, R.string.msg_insert_mail, Toast.LENGTH_LONG).show();
                    return;
                case ERR_INVALID_NAME:
                    Toast.makeText(mActivity, R.string.msg_invalid_contactname, Toast.LENGTH_LONG).show();
                    return;
                case ERR_INVALID_PHONE:
                    Toast.makeText(mActivity, R.string.msg_insert_phone, Toast.LENGTH_LONG).show();
                    return;
                case ERR_INVALID_VIA:
                    Toast.makeText(mActivity, R.string.msg_invalid_contactvia, Toast.LENGTH_LONG).show();
                    return;
                case ERR_UNABLE_TO_ADD:
                    Toast.makeText(mActivity, R.string.msg_unable_addcontact, Toast.LENGTH_LONG).show();
                    return;
            }

            return;
        }

        Toast.makeText(mActivity, R.string.msg_contact_added, Toast.LENGTH_LONG).show();
        Intent intent = new Intent();
        intent.putExtras(bundle);

        mActivity.setResult(Activity.RESULT_OK, intent);
        mActivity.finish();
    }

    public void setAvatarUrl(Uri uri) {
        Uri prevUrl = this.mAvatarUrl;
        this.mAvatarUrl = uri;

        if(mAvatarUrl != null) {
            final int dp70 = Utils.dpToPx(mActivity, 70);
            final int dp150 = Utils.dpToPx(mActivity, 150);
            Bitmap realImage =/* mAvatarUrl.startsWith(FILE_SCHEME) ? Utils.getScaledBitmap(mAvatarUrl.substring(6), dp150, dp150)
                    : */Utils.getScaledBitmap(mActivity, mAvatarUrl, dp150, dp150);
            if(realImage != null) {
                // cut a square thumbnail
                Bitmap rotatedImage = ThumbnailUtils.extractThumbnail(realImage, dp70, dp70, ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
                if(FILE_SCHEME.startsWith(mAvatarUrl.getScheme())) {
                    // rotate image according to the photo params
                    rotatedImage = Utils.getRotatedBitmapFromFileAttributes(rotatedImage, mAvatarUrl.toString().substring(6));
                }
                mBtnAddAvatar.setImageBitmap(rotatedImage);
            } else {
                this.mAvatarUrl = prevUrl;
            }
        }
    }
    
}
