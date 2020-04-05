package com.lody.virtual.client.hook.proxies.content;

import android.net.Uri;

import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.hook.base.MethodProxy;
import com.lody.virtual.client.stub.StubContentResolver;

import java.lang.reflect.Method;

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
            Object observer = args[1];
            Object userHandle = args[3];
            Object targetSdkVersion = args[4];
//            VLog.d(TAG, "notifyChange [ " + uri + ", " + observer + ", " + userHandle + ", " + targetSdkVersion + " ]");
            StubContentResolver contentObserver = VirtualCore.get().getContentObserver(uri);
            if (contentObserver != null) {
//                VLog.d(TAG, "notify change has cache : " + contentObserver);
                args[0] = contentObserver.getRegisterAuth();
                args[1] = contentObserver.getTransport();
            }
            return method.invoke(who, args);
        }
    }

    /**
     * public void registerContentObserver(Uri uri, boolean notifyForDescendants,
     * IContentObserver observer, int userHandle, int targetSdkVersion)
     */
    static class RegisterContentObserver extends MethodProxy {

        @Override
        public String getMethodName() {
            return "registerContentObserver";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            Uri uri = (Uri) args[0];
            if (VirtualCore.get().isInstalledProviders(uri.getAuthority())) {
//                VLog.d(TAG, "registerContentObserver installed providers");
                return method.invoke(who, args);
            }
            boolean notifyForDescendants = (boolean) args[1];
            Object observer = args[2];
            Object userHandle = args[3];
            Object targetSdkVersion = args[4];
            Uri changed = VirtualCore.get().registerContentObserver(uri, notifyForDescendants, observer);
//            VLog.d(TAG, "registerContentObserver [ " + changed + ", " + uri + ", " + observer + ", " + userHandle + ", " + targetSdkVersion + " ]");
            if (changed != null) {
                args[0] = changed;
            }
            return method.invoke(who, args);
        }
    }

    /**
     * public void registerContentObserver(Uri uri, boolean notifyForDescendants,
     * IContentObserver observer, int userHandle, int targetSdkVersion)
     */
    static class UnregisterContentObserver extends MethodProxy {

        @Override
        public String getMethodName() {
            return "unregisterContentObserver";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            Object observer = args[0];
            boolean remove = VirtualCore.get().unregisterContentObserver(observer);
            if (remove) {
//                VLog.d(TAG, "unregisterContentObserver remove");
            }
            return method.invoke(who, args);
        }
    }
}
