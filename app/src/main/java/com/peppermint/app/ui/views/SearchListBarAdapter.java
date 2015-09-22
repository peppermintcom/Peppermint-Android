package com.peppermint.app.ui.views;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.peppermint.app.R;

import java.util.List;

/**
 * Created by Nuno Luz on 26/08/2015.
 *
 * Adapter for the Custom Action Bar List Items.
 */
public class SearchListBarAdapter<T extends SearchListBarView.ListItem> extends ArrayAdapter<T> {

    private LayoutInflater mLayoutInflater;
    private Typeface mTypeface;
    private List<T> mObjects;

    public SearchListBarAdapter(Context context, List<T> objects) {
        super(context, 0, objects);
        this.mObjects = objects;
        this.mLayoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public SearchListBarAdapter(Typeface font, Context context, List<T> objects) {
        super(context, 0, objects);
        this.mObjects = objects;
        this.mTypeface = font;
        this.mLayoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        if(v == null) {
            v = mLayoutInflater.inflate(R.layout.i_search_and_list_box_layout, null);
        }

        ImageView iconView = (ImageView) v.findViewById(R.id.imgIcon);
        TextView nameView = (TextView) v.findViewById(R.id.txtName);
        if(mTypeface != null) {
            nameView.setTypeface(mTypeface);
        }

        SearchListBarView.ListItem item = getItem(position);

        iconView.setImageResource(item.getDrawableResource());
        nameView.setText(item.getText());

        return v;
    }

    @Override
    public T getItem(int position) {
        return mObjects.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public int getCount() {
        return mObjects.size();
    }
}
