package com.peppermint.app.ui;

import android.content.Context;
import android.media.Image;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.peppermint.app.R;
import com.peppermint.app.data.RecipientType;

import org.w3c.dom.Text;

import java.util.List;

/**
 * Created by NunoLuz on 26/08/2015.
 */
public class RecipientTypeAdapter extends ArrayAdapter<RecipientType> {

    private LayoutInflater mLayoutInflater;

    public RecipientTypeAdapter(Context context, List<RecipientType> objects) {
        super(context, 0, objects);
        this.mLayoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {


        View v = convertView;
        if(v == null) {
            v = mLayoutInflater.inflate(R.layout.recipient_type_spinner_layout, null);
        }

        RecipientType rType = getItem(position);
        ImageView iconView = (ImageView) v.findViewById(R.id.icon);
        iconView.setImageResource(rType.getIconResId());

        return v;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        if(v == null) {
            v = mLayoutInflater.inflate(R.layout.recipient_type_item_layout, null);
        }

        ImageView iconView = (ImageView) v.findViewById(R.id.icon);
        TextView nameView = (TextView) v.findViewById(R.id.name);

        RecipientType rType = getItem(position);

        iconView.setImageResource(rType.getIconResId());
        nameView.setText(rType.getName());

        return v;
    }
}
