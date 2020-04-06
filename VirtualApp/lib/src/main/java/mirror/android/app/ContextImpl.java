package mirror.android.app;


import android.content.Context;
import android.content.pm.PackageManager;
import android.os.UserHandle;

import mirror.MethodParams;
import mirror.RefClass;
import mirror.RefConstructor;
import mirror.RefMethod;
import mirror.RefObject;

public class ContextImpl {
    public static Class<?> TYPE = RefClass.load(ContextImpl.class, "android.app.ContextImpl");
    @MethodParams({Context.class})
    public static RefObject<String> mBasePackageName;
    public static RefObject<Object> mPackageInfo;
    public static RefObject<PackageManager> mPackageManager;

    public static RefMethod<Context> getReceiverRestrictedContext;

    public static class ApplicationContentResolver {
        public static Class<?> TYPE = RefClass.load(ApplicationContentResolver.class, "android.app.ContextImpl$ApplicationContentResolver");
        @MethodParams(Context.class)
        public static RefConstructor<Object> ctor;
        public static RefObject<UserHandle> mUser;
        public static RefObject<Object> mMainThread;
    }
}
