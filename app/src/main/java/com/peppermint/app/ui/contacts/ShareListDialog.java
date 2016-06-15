package com.peppermint.app.ui.contacts;

import android.content.Context;
import android.content.pm.ResolveInfo;
import android.view.View;
import android.widget.AdapterView;

import com.peppermint.app.R;
import com.peppermint.app.dal.chat.Chat;
import com.peppermint.app.dal.recording.Recording;
import com.peppermint.app.services.messenger.handlers.share.ShareSender;
import com.peppermint.app.ui.base.dialogs.CustomListDialog;
import com.peppermint.app.ui.base.navigation.NavigationItem;
import com.peppermint.app.ui.base.navigation.NavigationListAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Nuno Luz on 14-06-2016.
 *
 * Dialog with a list of apps that can share Peppermint messages.
 */
public class ShareListDialog extends CustomListDialog {

    public interface OnShareListener {
        void onShare(ResolveInfo appInfo);
    }

    private Chat mChat;
    private Recording mRecording;
    private List<ResolveInfo> mAppList;
    private OnShareListener mOnShareListener;

    public ShareListDialog(Context context) {
        super(context);
        init(context);
    }

    public ShareListDialog(Context context, int themeResId) {
        super(context, themeResId);
        init(context);
    }

    public ShareListDialog(Context context, boolean cancelable, OnCancelListener cancelListener) {
        super(context, cancelable, cancelListener);
        init(context);
    }

    private void init(Context context) {
        setCancelable(true);
        setNegativeButtonListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        setTitleText(R.string.share_with_);

        final List<NavigationItem> shareOptions = new ArrayList<>();
        mAppList = ShareSender.getShareList(context);
        for(ResolveInfo resolveInfo : mAppList) {
            shareOptions.add(new NavigationItem(resolveInfo.loadLabel(context.getPackageManager()).toString(), resolveInfo.loadIcon(context.getPackageManager()), null, true));
        }

        final NavigationListAdapter shareAdapter = new NavigationListAdapter(context, shareOptions);
        setListAdapter(shareAdapter);
    }

    public Chat getChat() {
        return mChat;
    }

    public void setChat(Chat mChat) {
        this.mChat = mChat;
    }

    public Recording getRecording() {
        return mRecording;
    }

    public void setRecording(Recording mRecording) {
        this.mRecording = mRecording;
    }

    public OnShareListener getOnShareListener() {
        return mOnShareListener;
    }

    public void setOnShareListener(OnShareListener mOnShareListener) {
        this.mOnShareListener = mOnShareListener;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        super.onItemClick(parent, view, position, id);
        if(mOnShareListener != null) {
            mOnShareListener.onShare(mAppList.get(position));
        }
    }
}
