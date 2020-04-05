package com.lody.virtual.plugin.stub;

import android.annotation.TargetApi;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import com.lody.virtual.helper.utils.VLog;

public class PluginContentResolver extends ContentObserver {
    private static final String TAG = "PluginContentResolver";
    private final Uri mAuth;

    private ResolverData mResolverData;

    public PluginContentResolver(Uri auth) {
        super(new Handler(Looper.getMainLooper()));
        mAuth = auth;
    }

    public void setResolverData(Uri uri, boolean notifyForDescendants, Object observer) {
        mResolverData = new ResolverData(uri, notifyForDescendants, observer);
    }

    /**
     * Get authority that declared in host's manifest
     *
     * @return
     */
    public Uri getRegisterAuth() {
        return mAuth;
    }

    /**
     * Get authority that declared in plugin
     *
     * @return
     */
    public Uri getResolverAuth() {
        return mResolverData.uri;
    }

    public Object getTransport() {
        return mResolverData.observer;
    }

    public ContentObserver getContentObserver() {
        return mResolverData.contentObserver;
    }

    @Override
    public void onChange(boolean selfChange) {
        getContentObserver().onChange(selfChange);
        VLog.d(TAG, "content observer receive changed : " + selfChange);

    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void onChange(boolean selfChange, Uri uri) {
        getContentObserver().onChange(selfChange, uri);
        VLog.d(TAG, "content observer receive changed : " + selfChange + ", " + uri);
    }

    @Override
    public boolean deliverSelfNotifications() {
        return getContentObserver().deliverSelfNotifications();
    }

    public static class ResolverData {
        public final Uri uri;
        public final boolean notifyForDescendants;
        public final Object observer;
        private final ContentObserver contentObserver;

        public ResolverData(Uri uri, boolean notifyForDescendants, Object observer) {
            this.uri = uri;
            this.notifyForDescendants = notifyForDescendants;
            this.observer = observer;
            contentObserver = mirror.android.content.ContentObserver.Transport.mContentObserver.get(observer);
        }

        @Override
        public String toString() {
            return "ResolverData{" +
                    "uri=" + uri +
                    ", notifyForDescendants=" + notifyForDescendants +
                    ", contentObserver=" + contentObserver +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "PluginContentResolver{" +
                "mAuth=" + mAuth +
                ", mResolverData=" + mResolverData +
                '}';
    }
}
