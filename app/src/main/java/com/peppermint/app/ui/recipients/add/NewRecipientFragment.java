package com.peppermint.app.ui.recipients.add;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
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

import com.peppermint.app.R;
import com.peppermint.app.authenticator.AuthenticationData;
import com.peppermint.app.data.Recipient;
import com.peppermint.app.data.RecipientManager;
import com.peppermint.app.tracking.TrackerManager;
import com.peppermint.app.ui.CustomActionBarActivity;
import com.peppermint.app.ui.views.NavigationItem;
import com.peppermint.app.ui.views.NavigationListAdapter;
import com.peppermint.app.ui.views.dialogs.CustomListDialog;
import com.peppermint.app.ui.views.simple.CustomFontEditText;
import com.peppermint.app.ui.views.simple.CustomToast;
import com.peppermint.app.ui.views.simple.EditTextValidatorLayout;
import com.peppermint.app.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Created by Nuno Luz on 10-11-2015.
 *
 * New recipient/contact fragment.
 */
public class NewRecipientFragment extends Fragment implements View.OnClickListener, AdapterView.OnItemClickListener {

    private static final String TAG = NewRecipientFragment.class.getSimpleName();

    private static final String SCREEN_ID = "NewContact";

    public static final String KEY_VIA = TAG + "_Via";
    public static final String KEY_NAME = TAG + "_Name";
    public static final String KEY_RAW_ID = TAG + "_RawId";
    public static final String KEY_PHOTO_URL = TAG + "_PhotoUrl";
    public static final String KEY_RECIPIENT = TAG + "_Recipient";

    private static final String FILE_SCHEME = "file:/";
    private static final String SAVED_DIALOG_STATE_KEY = TAG + "_NewAvatarDialogState";
    private static final String SAVED_AVATAR_URL_KEY = TAG + "_AvatarUrl";
    private static final String SAVED_AVATAR_INPROGRESS_URL_KEY = TAG + "_AvatarInProgressUrl";

    private static final int TAKE_PHOTO_CODE = 123;
    private static final int CHOOSE_PHOTO_CODE = 124;

    private CustomActionBarActivity mActivity;

    private ImageView mBtnAddAvatar;
    private CustomFontEditText mTxtPhone, mTxtMail;
    private CustomFontEditText mTxtFirstName, mTxtLastName;
    private EditTextValidatorLayout mNameValidatorLayout, mEmailValidatorLayout, mPhoneValidatorLayout;
    private Button mBtnSave;

    private CustomListDialog mNewAvatarDialog;
    private Uri mAvatarUrl, mAvatarInProgressUrl;

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

    private EditTextValidatorLayout.ValidityChecker mValidityChecker;

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
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        List<NavigationItem> avatarOptions = new ArrayList<>();
        avatarOptions.add(new NavigationItem(getString(R.string.take_photo), R.drawable.ic_drawer_camera, null, true));
        avatarOptions.add(new NavigationItem(getString(R.string.choose_photo), R.drawable.ic_drawer_collections, null, true));

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
        NavigationListAdapter avatarAdapter = new NavigationListAdapter(mActivity, avatarOptions);
        mNewAvatarDialog.setListAdapter(avatarAdapter);

        mBtnSave = (Button) mActivity.getCustomActionBar().findViewById(R.id.btnSave);
        mBtnSave.setOnClickListener(this);

        // inflate the view
        View v = inflater.inflate(R.layout.f_newcontact, container, false);

        mBtnAddAvatar = (ImageView) v.findViewById(R.id.imgAddAvatar);
        mBtnAddAvatar.setOnClickListener(mAvatarClickListener);

        mNameValidatorLayout = (EditTextValidatorLayout) v.findViewById(R.id.lytNameValidator);
        mTxtFirstName = (CustomFontEditText) v.findViewById(R.id.txtFirstName);
        mTxtLastName = (CustomFontEditText) v.findViewById(R.id.txtLastName);
        mTxtMail = (CustomFontEditText) v.findViewById(R.id.txtEmail);
        mTxtPhone = (CustomFontEditText) v.findViewById(R.id.txtPhoneNumber);
        mEmailValidatorLayout = (EditTextValidatorLayout) v.findViewById(R.id.lytEmailValidator);
        mPhoneValidatorLayout = (EditTextValidatorLayout) v.findViewById(R.id.lytPhoneValidator);

        mNameValidatorLayout.setValidator(mNameValidator);
        mEmailValidatorLayout.setValidator(mEmailValidator);
        mPhoneValidatorLayout.setValidator(mPhoneValidator);
        mEmailValidatorLayout.setLinkedEditTextValidatorLayout(mPhoneValidatorLayout);

        mNameValidatorLayout.setOnValidityChangeListener(mValidityChangeListener);
        mEmailValidatorLayout.setOnValidityChangeListener(mValidityChangeListener);
        mPhoneValidatorLayout.setOnValidityChangeListener(mValidityChangeListener);

        mValidityChecker = new EditTextValidatorLayout.ValidityChecker(mNameValidatorLayout, mEmailValidatorLayout, mPhoneValidatorLayout);

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

        mNameValidatorLayout.validate();
        mEmailValidatorLayout.validate();
        mPhoneValidatorLayout.validate();

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
        mActivity = null;
        super.onDestroy();
    }

    @Override
    public void onClick(View v) {

        AuthenticationData authData = mActivity.getAuthenticationPolicyEnforcer().getAuthenticationData();
        if(authData == null) {
            return;
        }

        long rawId = 0;
        if(getArguments() != null) {
            rawId = getArguments().getLong(KEY_RAW_ID, 0);
        }

        String firstName = mTxtFirstName.getText().toString();
        String lastName = mTxtLastName.getText().toString();
        String phone = mTxtPhone.getText().toString().trim();
        String email = mTxtMail.getText().toString().trim();

        Recipient recipient = null;

        try {
            if((recipient = RecipientManager.insert(mActivity, rawId, firstName, lastName, phone, email, mAvatarUrl, authData.getEmail(), false)) == null) {
                Toast.makeText(mActivity, R.string.msg_unable_addcontact, Toast.LENGTH_LONG).show();
                return;
            }
        } catch (RecipientManager.InvalidNameException e) {
            Toast.makeText(mActivity, R.string.msg_invalid_contactname, Toast.LENGTH_LONG).show();
            return;
        } catch (RecipientManager.InvalidEmailException e) {
            Toast.makeText(mActivity, R.string.msg_insert_mail, Toast.LENGTH_LONG).show();
            return;
        } catch (RecipientManager.InvalidPhoneException e) {
            Toast.makeText(mActivity, R.string.msg_insert_phone, Toast.LENGTH_LONG).show();
            return;
        }

        Toast.makeText(mActivity, R.string.msg_contact_added, Toast.LENGTH_LONG).show();
        Intent intent = new Intent();
        intent.putExtra(KEY_RECIPIENT, recipient);

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
