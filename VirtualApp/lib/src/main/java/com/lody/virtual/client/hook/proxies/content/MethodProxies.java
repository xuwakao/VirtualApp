package com.lody.virtual.client.hook.proxies.content;

import android.database.IContentObserver;
import android.net.Uri;
import android.os.IBinder;
import android.os.IInterface;

import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.hook.base.MethodProxy;

import java.lang.reflect.Method;
import java.util.WeakHashMap;

import mirror.android.content.ContentObserver;

class MethodProxies {
    private static final String TAG = "ContentService$MethodProxies";

    /**
     * Notify observers of a particular user's view of the provider.
     * userHandle the user whose view of the provider is to be notified.  May be
     * the calling user without requiring any permission, otherwise the caller needs to
     * hold the INTERACT_ACROSS_USERS_FULL permission.  Pseudousers USER_ALL
     * USER_CURRENT are properly interpreted.
     * <p>
     * void notifyChange(in Uri uri, IContentObserver observer,
     * boolean observerWantsSelfNotifications, int flags,
     * int userHandle, int targetSdkVersion)
     */
    static class NotifyChange extends MethodProxy {

        @Override
        public String getMethodName() {
            return "notifyChange";
        }


        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            Uri uri = (Uri) args[0];
            if (VirtualCore.get().isInstalledProviders(uri.getAuthority())) {
//                VLog.d(TAG, "notifyChange installed providers");
                return method.invoke(who, args);
            }
            args[0] = VirtualCore.get().getRegisterCpAuth();
            return method.invoke(who, args);
        }
    }

    /**
     * public void registerContentObserver(Uri uri, boolean notifyForDescendants,
     * IContentObserver observer, int userHandle, int targetSdkVersion)
     */
    static class RegisterContentObserver extends MethodProxy {
        private WeakHashMap<IBinder, IContentObserver> mProxyIContentObserver = new WeakHashMap<>();

        @Override
        public String getMethodName() {
            return "registerContentObserver";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            Uri uri = (Uri) args[0];
            if (VirtualCore.get().isInstalledProviders(uri.getAuthority())) {
                return method.invoke(who, args);
            }
            Object observer = args[2];
            if (IContentObserver.class.isInstance(observer) && !IContentObserverProxy.class.isInstance(observer)) {
                final IInterface old = (IInterface) args[2];
                final IBinder token = old.asBinder();
                if (token != null) {
                    token.linkToDeath(new IBinder.DeathRecipient() {
                        @Override
                        public void binderDied() {
                            token.unlinkToDeath(this, 0);
                            mProxyIContentObserver.remove(token);
                        }
                    }, 0);
                    IContentObserver proxyIContentObserver = mProxyIContentObserver.get(token);
                    Uri auth = VirtualCore.get().getRegisterCpAuth();
                    if (proxyIContentObserver == null) {
                        proxyIContentObserver = new IContentObserverProxy(old, auth, uri);
                        mProxyIContentObserver.put(token, proxyIContentObserver);
                    }
                    android.database.ContentObserver contentObserver = ContentObserver.Transport.mContentObserver.get(old);
                    if (contentObserver != null) {
                        ContentObserver.mTransport.set(contentObserver, proxyIContentObserver);
                        args[0] = auth;
                        args[2] = proxyIContentObserver;
                    }

                }
            }
            return method.invoke(who, args);
        }

        private static class IContentObserverProxy extends IContentObserver.Stub {
            IInterface mOld;
            Uri declaredUri;
            Uri uri;

            IContentObserverProxy(IInterface old, Uri auth, Uri uri) {
                this.mOld = old;
                this.declaredUri = auth;
                this.uri = uri;
            }

            @Override
            public void onChange(boolean selfUpdate, Uri uri, int userId) {
                ContentObserver.Transport.onChange.call(mOld, selfUpdate, this.uri, userId);
            }
        }

        @Override
        public boolean isEnable() {
            return isAppProcess() || isMainProcess();
        }
    }

//    /**
//     * public void registerContentObserver(Uri uri, boolean notifyForDescendants,
//     * IContentObserver observer, int userHandle, int targetSdkVersion)
//     */
//    static class UnregisterContentObserver extends MethodProxy {
//
//        @Override
//        public String getMethodName() {
//            return "unregisterContentObserver";
//        }
//
//        @Override
//        public Object call(Object who, Method method, Object... args) throws Throwable {
//            if (!!IContentObserver.class.isInstance(args[0])) {
//                return method.invoke(who, args);
//            }
//            return method.invoke(who, args);
//        }
//    }
}
