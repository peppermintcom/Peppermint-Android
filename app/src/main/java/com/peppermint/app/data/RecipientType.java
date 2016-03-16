package com.peppermint.app.data;

import android.content.Context;
import android.provider.ContactsContract;

import com.peppermint.app.R;
import com.peppermint.app.ui.recipients.SearchListBarView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by Nuno Luz on 26/08/2015.
 *
 * Represents the allowed types of Recipients and the restrictions that define them.
 */
public class RecipientType implements SearchListBarView.ListCategory {

    /**
     * Get a list with all possible RecipientType instances.
     *
     * @param context the execution context
     * @return the list with all recipient types
     */
    public static List<RecipientType> getAll(Context context) {
        List<RecipientType> list = new ArrayList<>();
        final String[] mimeTypesAll = {
                ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE,
                ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE
        };
        final String[] mimeTypesEmail = {ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE};
        final String[] mimeTypesPhone = {ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE};

        list.add(new RecipientType(0, context.getString(R.string.recipient_type_recent), R.drawable.ic_recipienttype_recentcontacts, mimeTypesAll, true, false));
        list.add(new RecipientType(1, context.getString(R.string.recipient_type_all), R.drawable.ic_recipienttype_allcontacts, mimeTypesAll, null, true));
        list.add(new RecipientType(2, context.getString(R.string.recipient_type_email), R.drawable.ic_recipienttype_emailcontacts, mimeTypesEmail, null, true));
        list.add(new RecipientType(3, context.getString(R.string.recipient_type_phone), R.drawable.ic_recipienttype_phonecontacts, mimeTypesPhone, null, true));

        return list;
    }

    private long mId;
    private String mName;
    private int mIconResId;
    private List<String> mMimeTypes = new ArrayList<>();
    private Boolean mStarred;
    private boolean mSearchable = true;

    public RecipientType(long id, String name, int iconResId, String[] mimeTypes, Boolean starred, boolean isSearchable) {
        this.mId = id;
        this.mName = name;
        this.mIconResId = iconResId;
        setMimeType(mimeTypes);
        this.mStarred = starred;
        this.mSearchable = isSearchable;
    }

    public void setSearchable(boolean searchable) {
        this.mSearchable = searchable;
    }

    @Override
    public boolean isSearchable() {
        return mSearchable;
    }

    public String getText() {
        return mName;
    }

    public void setText(String mName) {
        this.mName = mName;
    }

    public long getId() {
        return mId;
    }

    public void setId(long mId) {
        this.mId = mId;
    }

    public List<String> getMimeTypes() {
        return mMimeTypes;
    }

    public void setMimeType(String[] mimeTypes) {
        mMimeTypes.clear();
        Collections.addAll(mMimeTypes, mimeTypes);
    }

    public Boolean isStarred() {
        return mStarred;
    }

    public void setStarred(Boolean mStarred) {
        this.mStarred = mStarred;
    }
}
