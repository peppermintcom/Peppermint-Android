package com.peppermint.app.data;

import android.content.Context;
import android.provider.ContactsContract;

import com.peppermint.app.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by NunoLuz on 26/08/2015.
 */
public class RecipientType {

    private long mId;
    private String mName;
    private int mIconResId;
    private String[] mMimeTypes;
    private Boolean mStarred;

    public static List<RecipientType> getAll(Context context) {
        List<RecipientType> list = new ArrayList<>();
        final String[] mimeTypesAll = {
                ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE,
                ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE
        };
        final String[] mimeTypesEmail = {ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE};
        final String[] mimeTypesPhone = {ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE};

        list.add(new RecipientType(0, context.getString(R.string.recipient_type_recent), R.drawable.ic_star_black_36dp, mimeTypesAll, true));
        list.add(new RecipientType(1, context.getString(R.string.recipient_type_all), R.drawable.ic_account_search_black_36dp, mimeTypesAll, null));
        list.add(new RecipientType(2, context.getString(R.string.recipient_type_email), R.drawable.ic_email_black_36dp, mimeTypesEmail, null));
        list.add(new RecipientType(3, context.getString(R.string.recipient_type_phone), R.drawable.ic_message_text_black_36dp, mimeTypesPhone, null));

        return list;
    }

    public RecipientType(long id, String name, int iconResId, String[] mimeTypes, Boolean starred) {
        this.mId = id;
        this.mName = name;
        this.mIconResId = iconResId;
        this.mMimeTypes = mimeTypes;
        this.mStarred = starred;
    }

    public String getName() {
        return mName;
    }

    public void setName(String mName) {
        this.mName = mName;
    }

    public long getId() {
        return mId;
    }

    public void setId(long mId) {
        this.mId = mId;
    }

    public int getIconResId() {
        return mIconResId;
    }

    public void setIconResId(int mIconResId) {
        this.mIconResId = mIconResId;
    }

    public String[] getMimeTypes() {
        return mMimeTypes;
    }

    public void setMimeType(String[] mimeTypes) {
        this.mMimeTypes = mimeTypes;
    }

    public Boolean isStarred() {
        return mStarred;
    }

    public void setStarred(Boolean mStarred) {
        this.mStarred = mStarred;
    }
}
