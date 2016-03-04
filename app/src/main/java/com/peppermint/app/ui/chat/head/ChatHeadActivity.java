package com.peppermint.app.ui.chat.head;

import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;

import com.peppermint.app.R;
import com.peppermint.app.data.Chat;
import com.peppermint.app.data.ChatManager;
import com.peppermint.app.data.DatabaseHelper;
import com.peppermint.app.ui.CustomActionBarActivity;
import com.peppermint.app.ui.chat.ChatFragment;
import com.peppermint.app.ui.chat.RecipientDataGUI;
import com.peppermint.app.ui.views.NavigationItem;
import com.peppermint.app.ui.views.RoundImageView;
import com.peppermint.app.ui.views.simple.CustomFontTextView;
import com.peppermint.app.utils.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Nuno Luz on 10-11-2015.
 *
 * Activity for user authentication.
 */
public class ChatHeadActivity extends CustomActionBarActivity implements RecipientDataGUI, OnClickListener {

    // UI
    private RoundImageView mImgAvatar;
    private ImageButton mBtnClose;
    private CustomFontTextView mTxtName;
    private ViewGroup mLytRoot;
    private Chat mChat;

    @Override
    protected List<NavigationItem> getNavigationItems() {
        final List<NavigationItem> navItems = new ArrayList<>();
        navItems.add(new NavigationItem("Chat", R.drawable.ic_settings_36dp, ChatFragment.class, false, false, 0));
        return navItems;
    }

    @Override
    protected int getContentViewResourceId() {
        return R.layout.a_chat_head_layout;
    }

    @Override
    protected int getBackgroundResourceId() {
        return android.R.color.transparent;
    }

    @Override
    public void onClick(View v) {
        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);

        mTxtName = (CustomFontTextView) findViewById(R.id.txtName);
        mLytRoot = (ViewGroup) findViewById(R.id.lytRoot);

        if(getIntent() != null && getIntent().hasExtra(ChatFragment.PARAM_CHAT_ID)) {
            DatabaseHelper databaseHelper = new DatabaseHelper(this);
            SQLiteDatabase db = databaseHelper.getReadableDatabase();
            mChat = ChatManager.getChatById(this, db, getIntent().getLongExtra(ChatFragment.PARAM_CHAT_ID, 0));
            db.close();
        }

        mImgAvatar = (RoundImageView) findViewById(R.id.imgChatAvatar);
        if(mChat.getMainRecipientParameter().getPhotoUri() != null) {
            mImgAvatar.setImageURI(Uri.parse(mChat.getMainRecipientParameter().getPhotoUri()));
        }
        mTxtName.setText(mChat.getMainRecipientParameter().getDisplayName());

        mBtnClose = (ImageButton) findViewById(R.id.btnClose);
        mBtnClose.setOnClickListener(this);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            int statusBarHeight = Utils.getStatusBarHeight(this);
            ((ViewGroup.MarginLayoutParams) mLytRoot.getLayoutParams()).topMargin += statusBarHeight;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        startService(new Intent(ChatHeadService.ACTION_DISABLE, null, this, ChatHeadService.class));
    }

    @Override
    protected void onStop() {
        startService(new Intent(ChatHeadService.ACTION_ENABLE, null, this, ChatHeadService.class));
        super.onStop();
    }

    @Override
    public void setRecipientData(String recipientName, String recipientVia, String recipientPhotoUri) {
        mTxtName.setText(recipientName);
    }
}
