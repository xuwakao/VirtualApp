package com.lody.virtual.client.hook.proxies.content;

import android.net.Uri;

import com.lody.virtual.client.hook.base.MethodProxy;
import com.lody.virtual.client.stub.VASettings;
import com.lody.virtual.helper.utils.VLog;
import com.lody.virtual.plugin.core.PluginCore;
import com.lody.virtual.plugin.stub.PluginContentResolver;

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
            Object uri = args[0];
            Object observer = args[1];
            Object userHandle = args[3];
            Object targetSdkVersion = args[4];
            VLog.d(TAG, "notifyChange [ " + uri + ", " + observer + ", " + userHandle + ", " + targetSdkVersion + " ]");
            PluginContentResolver contentObserver = PluginCore.get().getContentObserver((Uri) uri);
            if (contentObserver != null) {
                VLog.d(TAG, "notify change has cache : " + contentObserver);
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
            VLog.d(TAG, "RegisterContentObserver " + uri);
            if (uri.getAuthority().indexOf(VASettings.STUB_DECLARED_CP_AUTHORITY) >= 0) {
                return method.invoke(who, args);
            }
            boolean notifyForDescendants = (boolean) args[1];
            Object observer = args[2];
            Object userHandle = args[3];
            Object targetSdkVersion = args[4];
            Uri changed = PluginCore.get().registerContentObserver(uri, notifyForDescendants, observer);
            if (changed != null) {
                VLog.d(TAG, "registerContentObserver [ " + changed + ", " + uri + ", " + observer + ", " + userHandle + ", " + targetSdkVersion + " ]");
                args[0] = changed;
            }
            return method.invoke(who, args);
        }
    }
}
