package com.peppermint.app.services.messenger.handlers.share;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.widget.Toast;

import com.peppermint.app.R;
import com.peppermint.app.dal.message.Message;
import com.peppermint.app.dal.recipient.Recipient;
import com.peppermint.app.services.messenger.handlers.Sender;
import com.peppermint.app.services.messenger.handlers.SenderUploadListener;
import com.peppermint.app.services.messenger.handlers.SenderUploadTask;

/**
 * Created by Nuno Luz on 14-06-2016.
 *
 * SenderUploadTask for the {@link ShareSender}.
 */
public class ShareSenderTask extends SenderUploadTask {

    public ShareSenderTask(ShareSenderTask uploadTask) {
        super(uploadTask);
    }

    public ShareSenderTask(Sender sender, Message message, SenderUploadListener senderUploadListener) {
        super(sender, message, senderUploadListener);
    }

    @Override
    public void execute() throws Throwable {
        setupPeppermintAuthentication();

        if(!isCancelled()) {
            getTranscription();
        }

        if(!isCancelled()) {
            uploadPeppermintMessage();
        }

        final Message message = getMessage();
        final String recipientName = message.getChatParameter().getRecipientListDisplayNames();
        final String transcription = message.getRecordingParameter().getTranscription();
        String text;

        final boolean onlyTranscription = recipientName.compareToIgnoreCase(Recipient.NAME_SHARE_TRANSCRIPTION) == 0;
        final boolean onlyAudio = recipientName.compareToIgnoreCase(Recipient.NAME_SHARE_AUDIO) == 0;

        if(onlyTranscription) {
            if(transcription == null) {
                Toast.makeText(getContext(), R.string.msg_no_transcription_sharing_audio, Toast.LENGTH_LONG).show();
                text = message.getServerShortUrl();
            } else {
                text = transcription;
            }
        } else if(onlyAudio) {
            text = message.getServerShortUrl();
        } else {
            text = message.getServerShortUrl() + (transcription == null ? "" : " \n\n" + transcription);
        }

        if(!isCancelled()) {
            setNonCancellable();

            String componentNameStr = message.getChatParameter().getRecipientList().get(0).getPhotoUri();
            componentNameStr = componentNameStr.substring(2, componentNameStr.length() - 2);

            ComponentName componentName = null;
            try {
                componentName = ComponentName.unflattenFromString(componentNameStr);
                if(componentName == null) {
                    throw new PackageManager.NameNotFoundException(componentNameStr);
                }
            } catch (PackageManager.NameNotFoundException e) {
                getTrackerManager().logException(e);
            }

            final Intent sendIntent = new Intent();
            sendIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT, text);
            sendIntent.setType("text/plain");
            if(componentName != null) {
                sendIntent.setComponent(componentName);
            }
            getContext().startActivity(sendIntent);
        }

        if(!isCancelled()) {
            sendPeppermintTranscription();
        }
    }

}
