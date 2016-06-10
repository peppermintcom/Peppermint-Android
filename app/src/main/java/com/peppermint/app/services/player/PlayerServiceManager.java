package com.peppermint.app.services.player;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import com.peppermint.app.dal.message.Message;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Nuno Luz on 28/08/2015.
 *
 * Manages the Android Service that plays {@link Message}s.
 *
 */
public class PlayerServiceManager {

    public interface PlayServiceListener {
        /**
         * Invoked when a binding to the service is performed.
         */
        void onBoundPlayService();
    }

    private Context mContext;
    private PlayerService.PlayerServiceBinder mService;
    private List<PlayServiceListener> mServiceListenerList = new ArrayList<>();
    protected boolean mIsBound = false;
    protected boolean mIsBinding = false;

    /**
     * Event listener associated with the service bind/unbind.
     */
    protected ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder binder) {
            mService = (PlayerService.PlayerServiceBinder) binder;

            mIsBound = true;
            mIsBinding = false;

            for(PlayServiceListener listener : mServiceListenerList) {
                listener.onBoundPlayService();
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            // this is called when the connection with the service has been unexpectedly disconnected - process crashed.
            mService = null;
            mIsBound = false;
            mIsBinding = false;
        }
    };

    public PlayerServiceManager(Context context) {
        this.mContext = context;
    }

    public void start() {
        Intent intent = new Intent(mContext, PlayerService.class);
        mContext.startService(intent);
    }

    /**
     * Binds this manager to the service.
     */
    public void bind() {
        mIsBinding = true;
        mContext.bindService(new Intent(mContext, PlayerService.class), mConnection, Context.BIND_AUTO_CREATE);
    }

    /**
     * Unbinds this manager from the service.
     */
    public void unbind() {
        if (mIsBound || mIsBinding) {
             // detach our existing connection.
            mContext.unbindService(mConnection);
            mIsBound = false;
            mIsBinding = false;
        }
    }

    public boolean isBound() {
        return mIsBound;
    }

    public void play(Message message, int startPercent) {
        mService.play(message, startPercent);
    }

    public boolean pause(Message message, boolean resetProgress) {
        return mService.pause(message, resetProgress);
    }

    public boolean pause() {
        return mService.pause(null, false);
    }

    public boolean stop() {
        return mService.stop(null);
    }

    public boolean stop(Message message) {
        return mService.stop(message);
    }

    public boolean setPosition(Message message, int percent) {
        return mService.setPosition(message, percent);
    }

    public boolean isPlaying(Message message) {
        return mService.isPlaying(message);
    }

    public boolean isPlaying() {
        return mService.isPlaying();
    }

    public boolean isLoading(Message message) {
        return mService.isLoading(message);
    }

    public boolean isLoading() {
        return mService.isLoading();
    }

    public void addServiceListener(PlayServiceListener listener) {
        mServiceListenerList.add(listener);
    }

    public boolean removeServiceListener(PlayServiceListener listener) {
        return mServiceListenerList.remove(listener);
    }
}
