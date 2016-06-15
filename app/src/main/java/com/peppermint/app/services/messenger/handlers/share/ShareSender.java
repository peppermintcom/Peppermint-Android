package com.peppermint.app.services.messenger.handlers.share;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import com.peppermint.app.dal.message.Message;
import com.peppermint.app.services.messenger.handlers.Sender;
import com.peppermint.app.services.messenger.handlers.SenderObject;
import com.peppermint.app.services.messenger.handlers.SenderUploadListener;
import com.peppermint.app.services.messenger.handlers.SenderUploadTask;

import java.util.List;
import java.util.UUID;

/**
 * Created by Nuno Luz on 14-06-2016.
 *
 * Sender that triggers sharing options to send audio messages.
 */
public class ShareSender extends Sender {

    public ShareSender(SenderObject objToExtend, SenderUploadListener senderUploadListener) {
        super(objToExtend, senderUploadListener);
    }

    @Override
    public SenderUploadTask newTask(Message message, UUID enforcedId) {
        SenderUploadTask task = new ShareSenderTask(this, message, getSenderUploadListener());
        if(enforcedId != null) {
            task.getIdentity().setId(enforcedId);
        }
        return task;
    }

    public static List<ResolveInfo> getShareList(final Context context) {
        final Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.setType("text/plain");

        final PackageManager packageManager = context.getPackageManager();
        return packageManager.queryIntentActivities(
                sendIntent, PackageManager.MATCH_ALL);
    }
}
