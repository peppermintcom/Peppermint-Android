/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.peppermint.app.services.authenticator;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.peppermint.app.PeppermintApp;

import de.greenrobot.event.EventBus;

/**
 * Created by Nuno Luz on 26-01-2016.
 * <p>
 *      Authenticator service to handle Peppermint accounts.
 *      It instantiates the authenticator and returns its IBinder.
 * </p>
 */
public class AuthenticationService extends Service {

    private static final String TAG = AuthenticationService.class.getSimpleName();

    private static final EventBus EVENT_BUS = new EventBus();

    static {
        if(PeppermintApp.DEBUG) {
            EVENT_BUS.register(new Object() {
                public void onEventBackgroundThread(SignInEvent event) {
                    Log.d(TAG, event.toString());
                }
                public void onEventBackgroundThread(SignOutEvent event) {
                    Log.d(TAG, event.toString());
                }
            });
        }
    }

    public static void registerEventListener(Object listener) {
        EVENT_BUS.register(listener);
    }

    public static void registerEventListener(Object listener, int priority) {
        EVENT_BUS.register(listener, priority);
    }

    public static void unregisterEventListener(Object listener) {
        EVENT_BUS.unregister(listener);
    }

    protected static void postSignOutEvent() {
        if(EVENT_BUS.hasSubscriberForEvent(SignOutEvent.class)) {
            EVENT_BUS.post(new SignOutEvent());
        }
    }

    protected static void postSignInEvent(AuthenticationData data) {
        if(EVENT_BUS.hasSubscriberForEvent(SignInEvent.class)) {
            EVENT_BUS.post(new SignInEvent(data));
        }
    }

    private Authenticator mAuthenticator;

    @Override
    public void onCreate() {
        mAuthenticator = new Authenticator(this);
    }

    @Override
    public void onDestroy() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mAuthenticator.getIBinder();
    }
}
