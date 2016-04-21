package com.peppermint.app.ui.recipients.add;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.peppermint.app.R;
import com.peppermint.app.authenticator.AuthenticationData;
import com.peppermint.app.data.ContactManager;
import com.peppermint.app.data.ContactRaw;
import com.peppermint.app.data.DatabaseHelper;
import com.peppermint.app.data.GlobalManager;
import com.peppermint.app.ui.base.CustomActionBarView;
import com.peppermint.app.ui.base.NavigationItem;
import com.peppermint.app.ui.base.NavigationListAdapter;
import com.peppermint.app.ui.base.activities.CustomActionBarActivity;
import com.peppermint.app.ui.base.dialogs.CustomListDialog;
import com.peppermint.app.ui.base.views.CustomFontEditText;
import com.peppermint.app.ui.base.views.CustomToast;
import com.peppermint.app.ui.base.views.EditTextValidatorLayout;
import com.peppermint.app.utils.DateContainer;
import com.peppermint.app.utils.ResourceUtils;
import com.peppermint.app.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Created by Nuno Luz on 10-11-2015.
 *
 * Activity for user authentication.
 */
public class NewContactActivity extends CustomActionBarActivity implements AdapterView.OnItemClickListener, View.OnClickListener {

    private static final String TAG = NewContactActivity.class.getSimpleName();
    private static final String FILE_SCHEME = "file:/";

    // intent params
    public static final String KEY_VIA = TAG + "_Via";
    public static final String KEY_NAME = TAG + "_Name";
    public static final String KEY_PHOTO_URL = TAG + "_PhotoUrl";

    public static final String KEY_RAW_ID = TAG + "_RawId";

    // result params
    public static final String KEY_RECIPIENT = TAG + "_Recipient";

    // instance state params
    private static final String SAVED_DIALOG_STATE_KEY = TAG + "_NewAvatarDialogState";
    private static final String SAVED_AVATAR_URL_KEY = TAG + "_AvatarUrl";
    private static final String SAVED_AVATAR_INPROGRESS_URL_KEY = TAG + "_AvatarInProgressUrl";

    // activity request codes
    private static final int TAKE_PHOTO_CODE = 123;
    private static final int CHOOSE_PHOTO_CODE = 124;

    // UI
    private ImageView mBtnAddAvatar;
    private CustomFontEditText mTxtPhone, mTxtMail;
    private CustomFontEditText mTxtFirstName, mTxtLastName;
    private Button mBtnSave;

    private CustomListDialog mNewAvatarDialog;

    // validators
    private EditTextValidatorLayout mNameValidatorLayout, mEmailValidatorLayout, mPhoneValidatorLayout;
    private EditTextValidatorLayout.ValidityChecker mValidityChecker;

    private Uri mAvatarUrl, mAvatarInProgressUrl;

    @Override
    protected final int getContainerViewLayoutId() {
        return R.layout.f_newcontact;
    }

    @Override
    protected String getTrackerLabel() {
        return "NewContact";
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // init action bar
        final CustomActionBarView actionBarView = getCustomActionBar();
        if(actionBarView != null) {
            // inflate custom action bar
            View v = getLayoutInflater().inflate(R.layout.v_newcontact_actionbar, null, false);
            actionBarView.setContents(v, false);
            actionBarView.setTitle(getString(R.string.new_contact));
            // cancel new contact icon
            actionBarView.getMenuButton().setImageResource(R.drawable.ic_cancel_14dp);

            mBtnSave = (Button) actionBarView.findViewById(R.id.btnSave);
            mBtnSave.setOnClickListener(this);
        }

        // init content view
        final List<NavigationItem> avatarOptions = new ArrayList<>();
        avatarOptions.add(new NavigationItem(getString(R.string.take_photo), R.drawable.ic_drawer_camera, null, true));
        avatarOptions.add(new NavigationItem(getString(R.string.choose_photo), R.drawable.ic_drawer_collections, null, true));

        // new avatar options dialog
        mNewAvatarDialog = new CustomListDialog(this);
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
        NavigationListAdapter avatarAdapter = new NavigationListAdapter(this, avatarOptions);
        mNewAvatarDialog.setListAdapter(avatarAdapter);

        // add/replace avatar button
        mBtnAddAvatar = (ImageView) findViewById(R.id.imgAddAvatar);
        mBtnAddAvatar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mNewAvatarDialog != null && !mNewAvatarDialog.isShowing()) {
                    mNewAvatarDialog.show();
                }
            }
        });

        // inputs
        mNameValidatorLayout = (EditTextValidatorLayout) findViewById(R.id.lytNameValidator);
        mTxtFirstName = (CustomFontEditText) findViewById(R.id.txtFirstName);
        mTxtLastName = (CustomFontEditText) findViewById(R.id.txtLastName);
        mTxtMail = (CustomFontEditText) findViewById(R.id.txtEmail);
        mTxtPhone = (CustomFontEditText) findViewById(R.id.txtPhoneNumber);
        mEmailValidatorLayout = (EditTextValidatorLayout) findViewById(R.id.lytEmailValidator);
        mPhoneValidatorLayout = (EditTextValidatorLayout) findViewById(R.id.lytPhoneValidator);

        // input validators
        mNameValidatorLayout.setValidator(mNameValidator);
        mEmailValidatorLayout.setValidator(mEmailValidator);
        mPhoneValidatorLayout.setValidator(mPhoneValidator);
        mEmailValidatorLayout.setLinkedEditTextValidatorLayout(mPhoneValidatorLayout);

        mNameValidatorLayout.setOnValidityChangeListener(mValidityChangeListener);
        mEmailValidatorLayout.setOnValidityChangeListener(mValidityChangeListener);
        mPhoneValidatorLayout.setOnValidityChangeListener(mValidityChangeListener);

        mValidityChecker = new EditTextValidatorLayout.ValidityChecker(mNameValidatorLayout, mEmailValidatorLayout, mPhoneValidatorLayout);

        // get intent data
        Intent paramIntent = getIntent();
        if(paramIntent != null) {
            String via = paramIntent.getStringExtra(KEY_VIA);
            String name = paramIntent.getStringExtra(KEY_NAME);
            mAvatarUrl = paramIntent.getParcelableExtra(KEY_PHOTO_URL);

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

        mNameValidatorLayout.validate();
        mEmailValidatorLayout.validate();
        mPhoneValidatorLayout.validate();

        Utils.showKeyboard(this, WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    }

    @Override
    public void onPause() {
        Utils.hideKeyboard(this, WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
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

    @Override
    public void onClick(View v) {

        AuthenticationData authData = mAuthenticationPolicyEnforcer.getAuthenticationData();
        if(authData == null) {
            return;
        }

        long rawId = 0;
        Intent paramIntent = getIntent();
        if(paramIntent != null) {
            rawId = paramIntent.getLongExtra(KEY_RAW_ID, 0);
        }

        String firstName = mTxtFirstName.getText().toString();
        String lastName = mTxtLastName.getText().toString();
        String phone = mTxtPhone.getText().toString().trim();
        String email = mTxtMail.getText().toString().trim();

        ContactRaw recipient;

        try {
            if((recipient = ContactManager.insert(this, 0, rawId, firstName, lastName, phone, email, mAvatarUrl, authData.getEmail(), false)) == null) {
                Toast.makeText(this, R.string.msg_unable_addcontact, Toast.LENGTH_LONG).show();
                return;
            }
        } catch (ContactManager.InvalidNameException e) {
            Toast.makeText(this, R.string.msg_invalid_contactname, Toast.LENGTH_LONG).show();
            return;
        } catch (ContactManager.InvalidEmailException e) {
            Toast.makeText(this, R.string.msg_insert_mail, Toast.LENGTH_LONG).show();
            return;
        } catch (ContactManager.InvalidPhoneException e) {
            Toast.makeText(this, R.string.msg_insert_phone, Toast.LENGTH_LONG).show();
            return;
        }

        // add new contact to recent contact list (i.e. create a chat record for it)
        try {
            GlobalManager.insertOrUpdateTimestampChatAndRecipient(this,
                    DatabaseHelper.getInstance(this).getWritableDatabase(),
                    DateContainer.getCurrentUTCTimestamp(), recipient);
        } catch (SQLException e) {
            mTrackerManager.log("Unable to set new contact as recent contact!", e);
        }

        Toast.makeText(this, R.string.msg_contact_added, Toast.LENGTH_LONG).show();
        Intent intent = new Intent();
        intent.putExtra(KEY_RECIPIENT, recipient);

        setResult(Activity.RESULT_OK, intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        setResult(Activity.RESULT_CANCELED);
        super.onBackPressed();
    }

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
                if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
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
                        mTrackerManager.logException(ex);
                        Log.e(TAG, "Unable to create image file!", ex);
                        CustomToast.makeText(this, R.string.msg_external_storage_error, Toast.LENGTH_LONG).show();
                    }

                    if (photoFile != null) {
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
                        startActivityForResult(takePictureIntent, TAKE_PHOTO_CODE);
                    }
                } else {
                    CustomToast.makeText(this, R.string.msg_no_camera_app, Toast.LENGTH_LONG).show();
                }
                break;
            default:
                // choose photo
                /*Intent getPictureIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(getPictureIntent, CHOOSE_PHOTO_CODE);*/

                startActivityForResult(
                        Intent.createChooser(
                                new Intent(Intent.ACTION_GET_CONTENT)
                                        .setType("image/*"), getString(R.string.choose_photo)),
                        CHOOSE_PHOTO_CODE);
        }
    }

    public void setAvatarUrl(Uri uri) {
        Uri prevUrl = this.mAvatarUrl;
        this.mAvatarUrl = uri;

        if(mAvatarUrl != null) {
            final int dp70 = Utils.dpToPx(this, 70);
            final int dp150 = Utils.dpToPx(this, 150);
            Bitmap realImage = ResourceUtils.getScaledBitmap(this, mAvatarUrl, dp150, dp150);
            if(realImage != null) {
                // cut a square thumbnail
                Bitmap rotatedImage = ThumbnailUtils.extractThumbnail(realImage, dp70, dp70, ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
                if(FILE_SCHEME.startsWith(mAvatarUrl.getScheme())) {
                    // rotate image according to the photo params
                    rotatedImage = ResourceUtils.getRotatedBitmapFromFileAttributes(rotatedImage, mAvatarUrl.toString().substring(6));
                }
                mBtnAddAvatar.setImageBitmap(rotatedImage);
            } else {
                this.mAvatarUrl = prevUrl;
            }
        }
    }

    private EditTextValidatorLayout.Validator mNameValidator = new EditTextValidatorLayout.Validator() {
        @Override
        public String getValidatorMessage(Set<Integer> indicesWithError, CharSequence[] text) {
            String firstName = text[0].toString().trim();
            String lastName = text[1].toString().trim();
            String fullName = (firstName + " " + lastName).trim();

            if(!Utils.isValidName(fullName)) {
                mBtnSave.setEnabled(false);
                indicesWithError.add(0);
                indicesWithError.add(1);
                return getString(R.string.msg_insert_name);
            }

            return null;
        }
    };

    private EditTextValidatorLayout.Validator mEmailValidator = new EditTextValidatorLayout.Validator() {
        @Override
        public String getValidatorMessage(Set<Integer> indicesWithError, CharSequence[] text) {
            String email = mTxtMail.getText().toString().trim();
            String phone = mTxtPhone.getText().toString().trim();

            if(!Utils.isValidEmail(email) && !(TextUtils.isEmpty(email) && Utils.isValidPhoneNumber(phone))) {
                mBtnSave.setEnabled(false);
                indicesWithError.add(0);
                return getString(R.string.msg_insert_mail);
            }

            return null;
        }
    };

    private EditTextValidatorLayout.Validator mPhoneValidator = new EditTextValidatorLayout.Validator() {
        @Override
        public String getValidatorMessage(Set<Integer> indicesWithError, CharSequence[] text) {
            String email = mTxtMail.getText().toString().trim();
            String phone = mTxtPhone.getText().toString().trim();

            if(!Utils.isValidPhoneNumber(phone) && !(TextUtils.isEmpty(phone) && Utils.isValidEmail(email))) {
                mBtnSave.setEnabled(false);
                indicesWithError.add(0);
                return getString(R.string.msg_insert_phone);
            }

            return null;
        }
    };

    private EditTextValidatorLayout.OnValidityChangeListener mValidityChangeListener = new EditTextValidatorLayout.OnValidityChangeListener() {
        @Override
        public void onValidityChange(boolean isValid) {
            if(mValidityChecker.areValid()) {
                mBtnSave.setEnabled(true);
            } else {
                mBtnSave.setEnabled(false);
            }
        }
    };
}
