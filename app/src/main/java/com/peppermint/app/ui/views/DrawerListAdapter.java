package com.peppermint.app.ui.views;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.peppermint.app.R;

import java.util.List;

/**
 * Created by Nuno Luz (nluz@mobaton.com) on 21-08-2015.
 *
 * List adapter for the drawer menu.
 */
public class DrawerListAdapter extends BaseAdapter {

    protected Context mContext;
    protected List<NavigationItem> mList;
    protected Typeface mFont;

    public DrawerListAdapter(Context context, List<NavigationItem> list, Typeface font) {
        this.mContext = context;
        this.mList = list;
        this.mFont = font;
    }

    @Override
    public int getCount() {
        return mList.size();
    }

    @Override
    public Object getItem(int position) {
        return mList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view;

        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.i_drawer_menu_item, null);
        }
        else {
            view = convertView;
        }

        TextView titleView = (TextView) view.findViewById(R.id.txtName);
        ImageView iconView = (ImageView) view.findViewById(R.id.imgIcon);

        titleView.setTypeface(mFont);

        titleView.setText(mList.get(position).getTitle());
        iconView.setImageResource(mList.get(position).getIconResId());

        return view;
    }
}
