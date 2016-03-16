package com.peppermint.app.events;

import com.peppermint.app.authenticator.AuthenticationData;
import com.peppermint.app.cloud.senders.SenderTask;
import com.peppermint.app.data.Chat;
import com.peppermint.app.data.Message;
import com.peppermint.app.data.Recording;

import java.util.List;

import de.greenrobot.event.EventBus;

/**
 * Created by Nuno Luz on 16-03-2016.
 */
public class PeppermintEventBus {

    private static final EventBus EVENT_BUS_MESSAGES = new EventBus();
    private static final EventBus EVENT_BUS_AUDIO = new EventBus();

    public static EventBus getDefault() {
        return EventBus.getDefault();
    }

    // DEFAULT
    public static void register(Object listener) {
        getDefault().register(listener);
    }

    public static void register(Object listener, int priority) {
        getDefault().register(listener, priority);
    }

    public static void unregister(Object listener) {
        getDefault().unregister(listener);
    }

    // MESSAGES
    public static void registerMessages(Object listener) {
        EVENT_BUS_MESSAGES.register(listener);
    }

    public static void registerMessages(Object listener, int priority) {
        EVENT_BUS_MESSAGES.register(listener, priority);
    }

    public static void unregisterMessages(Object listener) {
        EVENT_BUS_MESSAGES.unregister(listener);
    }

    // PLAYER & RECORDER
    public static void registerAudio(Object listener) {
        EVENT_BUS_AUDIO.register(listener);
    }

    public static void registerAudio(Object listener, int priority) {
        EVENT_BUS_AUDIO.register(listener, priority);
    }

    public static void unregisterAudio(Object listener) {
        EVENT_BUS_AUDIO.unregister(listener);
    }

    // DEFAULT
    public static void postSignOutEvent() {
        getDefault().post(new SignOutEvent());
    }

    public static void postSignInEvent(AuthenticationData data) {
        getDefault().post(new SignInEvent(data));
    }

    // MESSAGES
    public static void postReceiverEvent(String receiverEmail, Message message) {
        EVENT_BUS_MESSAGES.post(new ReceiverEvent(ReceiverEvent.EVENT_RECEIVED, receiverEmail, message));
    }

    public static void postSenderEvent(int type, SenderTask senderTask, Throwable error) {
        EVENT_BUS_MESSAGES.post(new SenderEvent(senderTask, type, error));
    }

    public static void postSyncEvent(int type, List<Message> receivedMessages, List<Message> sentMessages, Throwable error) {
        EVENT_BUS_MESSAGES.post(new SyncEvent(type, receivedMessages, sentMessages));
    }

    // AUDIO
    public static void postPlayerEvent(int type, Message message, int percent, long currentMs, int errorCode) {
        EVENT_BUS_AUDIO.post(new PlayerEvent(type, message, percent, currentMs, errorCode));
    }

    public static void postRecorderEvent(int type, Recording recording, Chat chat, float loudness, Throwable error) {
        if(EVENT_BUS_AUDIO.hasSubscriberForEvent(RecorderEvent.class)) {
            EVENT_BUS_AUDIO.post(new RecorderEvent(type, recording, chat, loudness, error));
        }
    }
}
