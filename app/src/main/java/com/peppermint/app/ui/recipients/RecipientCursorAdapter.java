package com.peppermint.app.ui.recipients;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

import com.peppermint.app.PeppermintApp;
import com.peppermint.app.R;
import com.peppermint.app.data.PeppermintFilteredCursor;
import com.peppermint.app.data.Recipient;
import com.peppermint.app.data.RecipientManager;
import com.peppermint.app.ui.canvas.avatar.AnimatedAvatarView;

/**
 * Created by Nuno Luz on 27/08/2015.
 *
 * CursorAdapter to show recipients in a ListView.
 */
public class RecipientCursorAdapter extends CursorAdapter {

    private PeppermintApp mApp;

    public RecipientCursorAdapter(PeppermintApp app, Context context, Cursor cursor) {
        super(context, cursor, 0);
        this.mApp = app;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.i_recipient_layout, parent, false);
    }

    @Override
    public void bindView(View v, Context context, Cursor cursor) {
        Recipient recipient = RecipientManager.getRecipientFromCursor(cursor);

        AnimatedAvatarView imgPhoto = (AnimatedAvatarView) v.findViewById(R.id.imgPhoto);
        TextView txtName = (TextView) v.findViewById(R.id.txtName);
        TextView txtVia = (TextView) v.findViewById(R.id.txtVia);
        TextView txtContact = (TextView) v.findViewById(R.id.txtContact);

        txtVia.setTypeface(mApp.getFontRegular());
        txtName.setTypeface(mApp.getFontSemibold());
        txtContact.setTypeface(mApp.getFontSemibold());

        if(recipient != null && recipient.getPhotoUri() != null) {
            imgPhoto.setStaticDrawable(Uri.parse(recipient.getPhotoUri()));
            imgPhoto.setShowStaticAvatar(true);
        } else {
            imgPhoto.setShowStaticAvatar(false);
        }

        if(recipient != null) {
            txtName.setText(recipient.getDisplayName());
            if(isPeppermintContact(cursor, recipient.getRawId())) {
                txtContact.setText(R.string.app_name);
            } else {
                txtContact.setText(recipient.getEmail() != null ? recipient.getEmail().getVia() : recipient.getPhone().getVia());
            }
        }
    }

    public Recipient getRecipient(int position) {
        Cursor cursor = (Cursor) getItem(position);
        return RecipientManager.getRecipientFromCursor(cursor);
    }

    public boolean isPeppermintContact(Cursor cursor, long rawId) {
        return cursor instanceof PeppermintFilteredCursor && ((PeppermintFilteredCursor) cursor).getPeppermintContact(rawId) != null;
    }
}
