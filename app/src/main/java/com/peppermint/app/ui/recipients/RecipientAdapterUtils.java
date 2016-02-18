package com.peppermint.app.ui.recipients;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.peppermint.app.PeppermintApp;
import com.peppermint.app.R;
import com.peppermint.app.data.Recipient;
import com.peppermint.app.ui.canvas.avatar.AnimatedAvatarView;

/**
 * Created by Nuno Luz on 18-09-2015.
 *
 * Utility static methods for handling {@link Recipient}s.
 */
public class RecipientAdapterUtils {

    /**
     * Builds a view with Recipient data for Adapters.
     * @param app the PeppermintApp instance
     * @param context the context
     * @param recipient the recipient data
     * @param convertView the re-usable view (if it exists)
     * @param parent the parent view
     * @return the built recipient view
     */
    public static View getView(PeppermintApp app, Context context, Recipient recipient, View convertView, ViewGroup parent) {
        View v = convertView;

        if(v == null) {
            v = LayoutInflater.from(context).inflate(R.layout.i_recipient_layout, parent, false);
        }

        AnimatedAvatarView imgPhoto = (AnimatedAvatarView) v.findViewById(R.id.imgPhoto);
        TextView txtName = (TextView) v.findViewById(R.id.txtName);
        TextView txtVia = (TextView) v.findViewById(R.id.txtVia);
        TextView txtContact = (TextView) v.findViewById(R.id.txtContact);

        txtVia.setTypeface(app.getFontRegular());
        txtName.setTypeface(app.getFontSemibold());
        txtContact.setTypeface(app.getFontSemibold());

        if(recipient != null && recipient.getPhotoUri() != null) {
            imgPhoto.setStaticDrawable(Uri.parse(recipient.getPhotoUri()));
            imgPhoto.setShowStaticAvatar(true);
        } else {
            imgPhoto.setShowStaticAvatar(false);
        }

        if(recipient != null) {
            txtName.setText(recipient.getDisplayName());
            txtContact.setText(recipient.getEmail() != null ? recipient.getEmail().getVia() : recipient.getPhone().getVia());
        }

        return v;
    }
}
