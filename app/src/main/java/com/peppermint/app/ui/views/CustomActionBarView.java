package com.peppermint.app.ui.views;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.peppermint.app.R;
import com.peppermint.app.utils.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Nuno Luz on 22-09-2015.
 */
public class CustomActionBarView extends RelativeLayout {

    private boolean mMenuAsUpInit = false;
    private ImageButton mMenuButton;
    private RelativeLayout mContainer;

    private List<View> mOriginalContents;

    public CustomActionBarView(Context context) {
        super(context);
        init(null);
    }

    public CustomActionBarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public CustomActionBarView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public CustomActionBarView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(attrs);
    }

    private void init(AttributeSet attrs) {
        if(attrs != null) {

        }
    }

    public void initViews() {
        List<View> childViews = new ArrayList<>();
        int childCount = getChildCount();
        for(int i=0; i<childCount; i++) {
            childViews.add(getChildAt(i));
        }
        removeAllViews();

        LayoutInflater layoutInflater = LayoutInflater.from(getContext());
        layoutInflater.inflate(R.layout.v_custom_actionbar, this);

        mContainer = (RelativeLayout) findViewById(R.id.customActionBarContainer);
        mOriginalContents = new ArrayList<>();
        childCount = mContainer.getChildCount();
        for(int i=0; i<childCount; i++) {
            mOriginalContents.add(mContainer.getChildAt(i));
        }

        if(childViews.size() > 0) {
            mContainer.removeAllViews();
            for (int i = 0; i < childCount; i++) {
                mContainer.addView(childViews.get(i));
            }
        }

        mMenuButton = (ImageButton) findViewById(R.id.btnMenu);
        setDisplayMenuAsUpEnabled(mMenuAsUpInit);

        // bo: adjust status bar height
        int statusBarHeight = Utils.getStatusBarHeight(getContext());
        findViewById(R.id.customActionBarTopSpace).getLayoutParams().height = statusBarHeight;
        View lytActionBar = findViewById(R.id.customActionBar);
        lytActionBar.getLayoutParams().height = lytActionBar.getLayoutParams().height + statusBarHeight;
        // eo: adjust status bar height
    }

    public void setTitle(String title) {
        TextView txtTitle = (TextView) findViewById(R.id.txtTitle);
        if(txtTitle != null) {
            txtTitle.setText(title);
        }
    }

    public void setContents(View v, boolean center) {
        LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        if(center) {
            params.setMargins(0, 0, Utils.dpToPx(getContext(), 42), 0);
        }
        v.setLayoutParams(params);
        mContainer.removeAllViews();
        mContainer.addView(v);
        invalidate();
    }

    public void restoreContents() {
        mContainer.removeAllViews();
        for(int i=0; i<mOriginalContents.size(); i++) {
            mContainer.addView(mOriginalContents.get(i));
        }
        invalidate();
    }

    public ImageButton getMenuButton() {
        return mMenuButton;
    }

    public void setDisplayMenuAsUpEnabled(boolean val) {
        if(val) {
            mMenuButton.setImageResource(R.drawable.ic_back_36dp);
        } else {
            mMenuButton.setImageResource(R.drawable.ic_menu_36dp);
        }
    }
}
