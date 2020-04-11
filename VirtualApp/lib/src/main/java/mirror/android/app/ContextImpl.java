package mirror.android.app;


import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.UserHandle;

import mirror.MethodParams;
import mirror.RefClass;
import mirror.RefConstructor;
import mirror.RefInt;
import mirror.RefMethod;
import mirror.RefObject;
import mirror.RefStaticMethod;

public class ContextImpl {
    public static Class<?> TYPE = RefClass.load(ContextImpl.class, "android.app.ContextImpl");
    @MethodParams({Context.class})
    public static RefObject<String> mBasePackageName;
    public static RefObject<Object> mPackageInfo;
    public static RefObject<PackageManager> mPackageManager;
    public static RefInt mFlags;
    public static RefObject<Resources> mResources;
    public static RefObject<Context> mOuterContext;
    public static RefObject<ContentResolver> mContentResolver;

    public static RefMethod<Context> getReceiverRestrictedContext;
    @MethodParams({Context.class})
    public static RefStaticMethod<Object> getImpl;

    public static class ApplicationContentResolver {
        public static Class<?> TYPE = RefClass.load(ApplicationContentResolver.class, "android.app.ContextImpl$ApplicationContentResolver");
        @MethodParams(Context.class)
        public static RefConstructor<Object> ctor;
        public static RefObject<UserHandle> mUser;
        public static RefObject<Object> mMainThread;
    }
}
