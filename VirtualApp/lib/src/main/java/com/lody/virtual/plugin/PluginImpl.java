package com.lody.virtual.plugin;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Binder;
import android.os.Build;
import android.os.ConditionVariable;
import android.os.Handler;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.UserHandle;
import android.text.TextUtils;

import com.lody.virtual.BuildConfig;
import com.lody.virtual.client.IVClient;
import com.lody.virtual.client.core.InvocationStubManager;
import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.env.SpecialComponentList;
import com.lody.virtual.client.env.VirtualRuntime;
import com.lody.virtual.client.fixer.ContextFixer;
import com.lody.virtual.client.hook.providers.ProviderHook;
import com.lody.virtual.client.hook.secondary.ProxyServiceFactory;
import com.lody.virtual.client.ipc.VActivityManager;
import com.lody.virtual.client.ipc.VPackageManager;
import com.lody.virtual.client.stub.VASettings;
import com.lody.virtual.helper.compat.BuildCompat;
import com.lody.virtual.helper.utils.Reflect;
import com.lody.virtual.helper.utils.VLog;
import com.lody.virtual.os.VEnvironment;
import com.lody.virtual.os.VUserHandle;
import com.lody.virtual.plugin.core.PluginCore;
import com.lody.virtual.plugin.hook.delegate.PluginInstrumentation;
import com.lody.virtual.plugin.hook.proxies.PluginInjectors;
import com.lody.virtual.plugin.hook.proxies.am.PluginHCallbackStub;
import com.lody.virtual.plugin.utils.PluginHandle;
import com.lody.virtual.remote.InstalledAppInfo;
import com.lody.virtual.remote.PendingResultData;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import mirror.android.app.ActivityThread;
import mirror.android.app.ActivityThreadNMR1;
import mirror.android.app.ContextImpl;
import mirror.android.app.IActivityManager;
import mirror.android.app.LoadedApk;
import mirror.android.content.ContentProviderHolderOreo;
import mirror.android.content.ContextWrapper;
import mirror.android.content.res.CompatibilityInfo;
import mirror.android.providers.Settings;
import mirror.com.android.internal.content.ReferrerIntent;

public class PluginImpl extends IVClient.Stub {
    private static final String TAG = "PluginImpl";

    private static final int NEW_INTENT = 11;
    private static final int RECEIVER = 12;

    private IBinder mToken;
    private int mVUid;
    private int mVPid;
    private String mPackageName;
    private Resources mPkgResources;
    private ClassLoader mParent;
    private PluginDexClassLoader mPluginDexClassLoader;
    private PluginContext mPluginContext;
    private Application mInitialApplication;
    private ConditionVariable mTempLock;
    private AppBindData mBoundApplication;
    private H mH = new H();
    private ContentResolver mContentResolver;
    private Context mBaseContext;


    private PluginImpl() {
    }

    public static PluginImpl create() {
        return new PluginImpl();
    }

    public boolean isBound() {
        return mBoundApplication != null;
    }

    public void initPlugin(IBinder token, int vuid, int vpid) {
        mToken = token;
        mVUid = vuid;
        mVPid = vpid;
        PluginCore.get().putPlugin(vpid, this);
    }

    public void bindPluginApp(final String packageName, final String processName) {
        if (Looper.getMainLooper() == Looper.myLooper()) {
            bindPluginAppNoCheck(packageName, processName, new ConditionVariable());
        } else {
            final ConditionVariable lock = new ConditionVariable();
            VirtualRuntime.getUIHandler().post(new Runnable() {
                @Override
                public void run() {
                    bindPluginAppNoCheck(packageName, processName, lock);
                    lock.open();
                }
            });
            lock.block();
        }
    }

    private void bindPluginAppNoCheck(String packageName, String processName, ConditionVariable lock) {
        if (processName == null) {
            processName = packageName;
        }
        VLog.d(TAG, "bind plugin app [ pkg :" + packageName + " ] , [ process : " + processName + " ]");

        mTempLock = lock;
        mPackageName = packageName;
        InstalledAppInfo installedAppInfo = VirtualCore.get().getInstalledAppInfo(packageName, 0);
        if (installedAppInfo == null) {
            VLog.e(TAG, "plugin not install");
            return;
        }

        try {
            fixInstalledProviders();
        } catch (Throwable e) {
            VLog.e(TAG, "fix installed cp failed " + e);
        }

        AppBindData data = new AppBindData();
        data.appInfo = VPackageManager.get().getApplicationInfo(packageName, 0, getUserId());
        data.processName = processName;
        data.providers = VPackageManager.get().queryContentProviders(processName, mVUid, PackageManager.GET_META_DATA);
        mBoundApplication = data;
        VLog.i(TAG, "Binding application " + data.appInfo.packageName + " (" + data.processName + ")");

        if (!loadPlugin()) {
            mBoundApplication = null;
            return;
        }

        boolean conflict = SpecialComponentList.isConflictingInstrumentation(packageName);
        if (!conflict) {
            InvocationStubManager.getInstance().checkEnv(PluginInstrumentation.class);
        }
        makeApplication();

//        Configuration configuration = mPkgResources.getConfiguration();
//        Object compatInfo = CompatibilityInfo.ctor.newInstance(data.appInfo, configuration.screenLayout, configuration.smallestScreenWidthDp, false);
//        Object pkgInfo = mirror.android.app.Application.mLoadedApk.get(mInitialApplication);
//        ApplicationInfo applicationInfo = new ApplicationInfo(data.appInfo);
//        applicationInfo.packageName = VirtualCore.get().getHostPkg();
//
//        Object loadedApk = Reflect.on(LoadedApk.Class).create(VirtualCore.mainThread(), data.appInfo, compatInfo,
//                LoadedApk.mBaseClassLoader.get(pkgInfo), LoadedApk.mSecurityViolation.get(pkgInfo),
//                LoadedApk.mIncludeCode.get(pkgInfo), LoadedApk.mRegisterPackage.get(pkgInfo)).get();
//        data.info = loadedApk;
//        LoadedApk.mApplication.set(data.info, mInitialApplication);
//        mirror.android.app.Application.mLoadedApk.set(mInitialApplication, data.info);
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
//            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
//                DisplayAdjustments.setCompatibilityInfo.call(ContextImplKitkat.mDisplayAdjustments.get(mPluginContext), compatInfo);
//            }
//            DisplayAdjustments.setCompatibilityInfo.call(LoadedApkKitkat.mDisplayAdjustments.get(mBoundApplication.info), compatInfo);
//        } else {
//            CompatibilityInfoHolder.set.call(LoadedApkICS.mCompatibilityInfo.get(mBoundApplication.info), compatInfo);
//        }
//        LoadedApk.mClassLoader.set(data.info, mPluginDexClassLoader);
        mPluginContext.setApplication(mInitialApplication);

        Map<String, WeakReference<?>> loadedApkCache = ActivityThread.mPackages.get(VirtualCore.mainThread());
        loadedApkCache.put(packageName, new WeakReference<Object>(data.info));
        try {
            PluginInjectors.get().inject();
        } catch (Throwable throwable) {
            VLog.e(TAG, "inject failed");
        }
        /**
         * install content providers into ActivityThread cache
         *
         * Use this can support ContentProvider.
         * If not use this, {@link com.lody.virtual.plugin.hook.proxies.am.MethodProxies.GetContentProvider} and
         * {@link PluginContext#installContentResolver()} can also support ContentProvider.
         */
        if (data.providers != null) {
            installContentProviders(mInitialApplication, data.providers);
        }

        if (lock != null) {
            lock.open();
            mTempLock = null;
        }

        try {
            PluginInstrumentation.getDefault().callApplicationOnCreate(mInitialApplication);
            InvocationStubManager.getInstance().checkEnv(PluginHCallbackStub.class);
            if (conflict) {
                InvocationStubManager.getInstance().checkEnv(PluginInstrumentation.class);
            }
        } catch (Throwable e) {
            VLog.e(TAG, "application something wrong " + e);
            if (!PluginInstrumentation.getDefault().onException(mInitialApplication, e)) {
                VLog.e(TAG, "Unable to create plugin application " + mInitialApplication.getClass().getName()
                        + ": " + e.toString(), e);
            }
        }
        VActivityManager.get().appDoneExecuting(mVPid);
    }

    private void makeApplication() {
        String appClass = TextUtils.isEmpty(mBoundApplication.appInfo.className) ?
                Application.class.getName() : mBoundApplication.appInfo.className;
        PluginInstrumentation instrumentation = PluginInstrumentation.getDefault();
        try {
            Configuration configuration = mPkgResources.getConfiguration();
            Object compatInfo = CompatibilityInfo.ctor.newInstance(mBoundApplication.appInfo, configuration.screenLayout, configuration.smallestScreenWidthDp, false);
//            Object mainApk = ContextImpl.getImpl.call(mPluginContext);
            Object loadedApk = Reflect.on(LoadedApk.Class).create(VirtualCore.mainThread(), mBoundApplication.appInfo, compatInfo,
                    mPluginDexClassLoader, true,
                    true, true).get();
            LoadedApk.mClassLoader.set(loadedApk, mPluginDexClassLoader);
            mBoundApplication.info = loadedApk;
            mBaseContext = generateBaseContextImpl(loadedApk);

            mInitialApplication = instrumentation.newApplication(
                    mPluginDexClassLoader, appClass, mBaseContext);
            LoadedApk.mResources.set(loadedApk, mPkgResources);
            LoadedApk.mApplication.set(loadedApk, mInitialApplication);
            ContextImpl.mOuterContext.set(mBaseContext, mInitialApplication);
            installContentResolver(mBaseContext);
            ContextWrapper.mBase.set(mPluginContext, mBaseContext);
            ContextFixer.fixContext(mInitialApplication);
            ContextFixer.fixContext(mBaseContext);
            Object boundApp = ActivityThread.mBoundApplication.get(VirtualCore.mainThread());
            ActivityThread.AppBindData.providers.get(boundApp).addAll(mBoundApplication.providers);
            ContextWrapper.mBase.set(mInitialApplication, mPluginContext);
        } catch (ClassNotFoundException e) {
            VLog.e(TAG, "makeApplication error " + e);
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            VLog.e(TAG, "makeApplication error " + e);
            e.printStackTrace();
        } catch (InstantiationException e) {
            VLog.e(TAG, "makeApplication error " + e);
            e.printStackTrace();
        }
    }


    private Context generateBaseContextImpl(Object loadedApk) {
        int flags = ContextImpl.mFlags.get(VirtualCore.get().getContext());
        Context context = Reflect.on(ContextImpl.TYPE)
                .create(VirtualCore.get().getContext(), VirtualCore.mainThread(),
                        loadedApk, null, null, null, flags, mPluginDexClassLoader).get();
        ContextImpl.mResources.set(context, mPkgResources);
        return context;
    }

    private void installContentResolver(Context context) {
        UserHandle userHandle = mirror.android.os.UserHandle.ctor.newInstance(PluginHandle.getHandleForPlugin(mVPid));
        Class<?> clz = ContextImpl.ApplicationContentResolver.TYPE;
        mContentResolver = Reflect.on(clz).create(context, VirtualCore.mainThread(), userHandle).get();
        ContextImpl.mContentResolver.set(context, mContentResolver);
        mirror.android.content.ContentResolver.mPackageName.set(mContentResolver, mBaseContext.getPackageName());
    }

    private void installContentProviders(Context app, List<ProviderInfo> providers) {
        long origId = Binder.clearCallingIdentity();
        Object mainThread = VirtualCore.mainThread();
        try {
            for (ProviderInfo cpi : providers) {
                try {
                    ActivityThread.installProvider(mainThread, app, cpi, null);
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    private void fixInstalledProviders() {
        clearSettingProvider();
        Map clientMap = ActivityThread.mProviderMap.get(VirtualCore.mainThread());
        for (Object clientRecord : clientMap.values()) {
            if (BuildCompat.isOreo()) {
                IInterface provider = ActivityThread.ProviderClientRecordJB.mProvider.get(clientRecord);
                Object holder = ActivityThread.ProviderClientRecordJB.mHolder.get(clientRecord);
                if (holder == null) {
                    continue;
                }
                ProviderInfo info = ContentProviderHolderOreo.info.get(holder);
                if (!info.authority.startsWith(VASettings.STUB_PLUGIN_AUTHORITY) &&
                        !info.authority.startsWith(VASettings.STUB_DECLARED_CP_AUTHORITY) &&
                        !info.authority.startsWith(VASettings.STUB_CP_AUTHORITY)) {
                    provider = ProviderHook.createProxy(true, info.authority, provider);
                    ActivityThread.ProviderClientRecordJB.mProvider.set(clientRecord, provider);
                    ContentProviderHolderOreo.provider.set(holder, provider);
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                IInterface provider = ActivityThread.ProviderClientRecordJB.mProvider.get(clientRecord);
                Object holder = ActivityThread.ProviderClientRecordJB.mHolder.get(clientRecord);
                if (holder == null) {
                    continue;
                }
                ProviderInfo info = IActivityManager.ContentProviderHolder.info.get(holder);
                if (!info.authority.startsWith(VASettings.STUB_PLUGIN_AUTHORITY) &&
                        !info.authority.startsWith(VASettings.STUB_DECLARED_CP_AUTHORITY) &&
                        !info.authority.startsWith(VASettings.STUB_CP_AUTHORITY)) {
                    provider = ProviderHook.createProxy(true, info.authority, provider);
                    ActivityThread.ProviderClientRecordJB.mProvider.set(clientRecord, provider);
                    IActivityManager.ContentProviderHolder.provider.set(holder, provider);
                }
            } else {
                String authority = ActivityThread.ProviderClientRecord.mName.get(clientRecord);
                IInterface provider = ActivityThread.ProviderClientRecord.mProvider.get(clientRecord);
                if (provider != null && !authority.startsWith(VASettings.STUB_PLUGIN_AUTHORITY) &&
                        !authority.startsWith(VASettings.STUB_DECLARED_CP_AUTHORITY) &&
                        !authority.startsWith(VASettings.STUB_CP_AUTHORITY)) {
                    provider = ProviderHook.createProxy(true, authority, provider);
                    ActivityThread.ProviderClientRecord.mProvider.set(clientRecord, provider);
                }
            }
        }

    }

    private void clearSettingProvider() {
        Object cache;
        cache = Settings.System.sNameValueCache.get();
        if (cache != null) {
            clearContentProvider(cache);
        }
        cache = Settings.Secure.sNameValueCache.get();
        if (cache != null) {
            clearContentProvider(cache);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && Settings.Global.TYPE != null) {
            cache = Settings.Global.sNameValueCache.get();
            if (cache != null) {
                clearContentProvider(cache);
            }
        }
    }

    private void clearContentProvider(Object cache) {
        if (BuildCompat.isOreo()) {
            Object holder = Settings.NameValueCacheOreo.mProviderHolder.get(cache);
            if (holder != null) {
                Settings.ContentProviderHolder.mContentProvider.set(holder, null);
            }
        } else {
            Settings.NameValueCache.mContentProvider.set(cache, null);
        }
    }

    private boolean loadPlugin() {
        PackageManager pm = VirtualCore.get().getContext().getPackageManager();
        try {
            if (BuildConfig.DEBUG) {
                // 如果是Debug模式的话，防止与Instant Run冲突，资源重新New一个
                Resources r = pm.getResourcesForApplication(mBoundApplication.appInfo);
                mPkgResources = new Resources(r.getAssets(), r.getDisplayMetrics(), r.getConfiguration());
            } else {
                mPkgResources = pm.getResourcesForApplication(mBoundApplication.appInfo);
            }
        } catch (PackageManager.NameNotFoundException e) {
            VLog.e(TAG, e);
        }
        if (mPkgResources == null) {
            VLog.d(TAG, "get resources null");
            return false;
        }

        if (BuildConfig.DEBUG) {
            // 因为Instant Run会替换parent为IncrementalClassLoader，所以在DEBUG环境里
            // 需要替换为BootClassLoader才行
            // Added by yangchao-xy & Jiongxuan Zhang
            mParent = ClassLoader.getSystemClassLoader();
        } else {
            // 线上环境保持不变
            mParent = getClass().getClassLoader().getParent(); // TODO: 这里直接用父类加载器
        }
        mPluginDexClassLoader = new PluginDexClassLoader(mPackageName, mBoundApplication.appInfo.sourceDir,
                VEnvironment.getOdexFile(mPackageName).getParent(), mBoundApplication.appInfo.nativeLibraryDir, mParent);
        if (mPluginDexClassLoader == null) {
            VLog.w(TAG, "get dex null");
            return false;
        }

        mPluginContext = new PluginContext(VirtualCore.get().getContext(), mBoundApplication.appInfo.theme,
                mPluginDexClassLoader, mPkgResources, mVPid, mVUid, mBoundApplication.appInfo);
        return true;
    }

    public PluginDexClassLoader getPluginDexClassLoader() {
        return mPluginDexClassLoader;
    }

    public int getVUid() {
        return mVUid;
    }

    public int getUserId() {
        return VUserHandle.getUserId(mVUid);
    }

    public PluginContext getPluginContext() {
        return mPluginContext;
    }

    public Class<?> loadClass(String name, boolean resolve) {
        try {
            return mPluginDexClassLoader.loadClass(name, resolve);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public ApplicationInfo getApplicationInfo() {
        return mBoundApplication.appInfo;
    }

    public Application getApp() {
        return mInitialApplication;
    }


    @Override
    public void scheduleNewIntent(String creator, IBinder token, Intent intent) {
        NewIntentData data = new NewIntentData();
        data.creator = creator;
        data.token = token;
        data.intent = intent;
        sendMessage(NEW_INTENT, data);
    }

    @Override
    public void finishActivity(IBinder token) {
        VActivityManager.get().finishActivity(token);
    }

    @Override
    public void scheduleReceiver(String processName, ComponentName component, Intent intent, PendingResultData resultData) {
        ReceiverData receiverData = new ReceiverData();
        receiverData.resultData = resultData;
        receiverData.intent = intent;
        receiverData.component = component;
        receiverData.processName = processName;
        sendMessage(RECEIVER, receiverData);
    }

    private void sendMessage(int what, Object obj) {
        Message msg = Message.obtain();
        msg.what = what;
        msg.obj = obj;
        mH.sendMessage(msg);
    }

    private void handleReceiver(ReceiverData data) {
        BroadcastReceiver.PendingResult result = data.resultData.build();
        try {
            if (!isBound()) {
                bindPluginApp(data.component.getPackageName(), data.processName);
            }
            Context context = VirtualCore.get().getContext();
            Context receiverContext = ContextImpl.getReceiverRestrictedContext.call(context);
            String className = data.component.getClassName();
            BroadcastReceiver receiver = (BroadcastReceiver) mPluginDexClassLoader.loadClass(className).newInstance();
            mirror.android.content.BroadcastReceiver.setPendingResult.call(receiver, result);
            data.intent.setExtrasClassLoader(mPluginDexClassLoader);
            if (data.intent.getComponent() == null) {
                data.intent.setComponent(data.component);
            }
            receiver.onReceive(receiverContext, data.intent);
            if (mirror.android.content.BroadcastReceiver.getPendingResult.call(receiver) != null) {
                result.finish();
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(
                    "Unable to start receiver " + data.component
                            + ": " + e.toString(), e);
        }
        VActivityManager.get().broadcastFinish(data.resultData);
    }

    @Override
    public IBinder createProxyService(ComponentName component, IBinder binder) {
        return ProxyServiceFactory.getProxyService(mInitialApplication, component, binder);
    }

    @Override
    public IBinder acquireProviderClient(ProviderInfo info) {
        VLog.d(TAG, "mTempLock block " + Thread.currentThread().getId());
        /*if (mTempLock != null) {
            mTempLock.block();
        }*/

        VLog.d(TAG, "mTempLock already block " + mVPid);

        if (!isBound()) {
            bindPluginApp(info.packageName, info.processName);
        }
        IInterface provider = null;
        String[] authorities = info.authority.split(";");
        String authority = authorities.length == 0 ? info.authority : authorities[0];
        ContentResolver resolver = mPluginContext.getContentResolver();
        ContentProviderClient client = null;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                client = resolver.acquireUnstableContentProviderClient(authority);
            } else {
                client = resolver.acquireContentProviderClient(authority);
            }
        } catch (Throwable e) {
            VLog.e(TAG, "acquireUnstableContentProviderClient failed, " + e);
        }
        if (client != null) {
            provider = mirror.android.content.ContentProviderClient.mContentProvider.get(client);
            client.release();
        }
        return provider != null ? provider.asBinder() : null;
    }

    @Override
    public IBinder getAppThread() {
        return ActivityThread.getApplicationThread.call(VirtualCore.mainThread());
    }

    @Override
    public IBinder getToken() {
        return mToken;
    }

    @Override
    public String getDebugInfo() throws RemoteException {
        return null;
    }

    private final class NewIntentData {
        String creator;
        IBinder token;
        Intent intent;
    }

    private final class AppBindData {
        String processName;
        ApplicationInfo appInfo;
        List<ProviderInfo> providers;
        Object info;
    }

    private final class ReceiverData {
        PendingResultData resultData;
        Intent intent;
        ComponentName component;
        String processName;
    }

    private class H extends Handler {

        private H() {
            super(Looper.getMainLooper());
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case NEW_INTENT: {
                    handleNewIntent((NewIntentData) msg.obj);
                }
                break;
                case RECEIVER: {
                    handleReceiver((ReceiverData) msg.obj);
                }
            }
        }
    }

    private void handleNewIntent(NewIntentData data) {
        Intent intent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            intent = ReferrerIntent.ctor.newInstance(data.intent, data.creator);
        } else {
            intent = data.intent;
        }
        if (ActivityThread.performNewIntents != null) {
            ActivityThread.performNewIntents.call(
                    VirtualCore.mainThread(),
                    data.token,
                    Collections.singletonList(intent)
            );
        } else {
            ActivityThreadNMR1.performNewIntents.call(
                    VirtualCore.mainThread(),
                    data.token,
                    Collections.singletonList(intent),
                    true);
        }
    }
}
