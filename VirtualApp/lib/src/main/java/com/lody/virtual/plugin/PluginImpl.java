package com.lody.virtual.plugin;

import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Binder;
import android.os.Build;
import android.os.ConditionVariable;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Looper;
import android.text.TextUtils;

import com.lody.virtual.BuildConfig;
import com.lody.virtual.client.core.InvocationStubManager;
import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.env.SpecialComponentList;
import com.lody.virtual.client.env.VirtualRuntime;
import com.lody.virtual.client.hook.providers.ProviderHook;
import com.lody.virtual.client.ipc.VActivityManager;
import com.lody.virtual.client.ipc.VPackageManager;
import com.lody.virtual.client.stub.VASettings;
import com.lody.virtual.helper.compat.BuildCompat;
import com.lody.virtual.helper.utils.VLog;
import com.lody.virtual.os.VEnvironment;
import com.lody.virtual.plugin.hook.delegate.PluginInstrumentation;
import com.lody.virtual.plugin.hook.proxies.PluginInjectors;
import com.lody.virtual.plugin.hook.proxies.am.PluginHCallbackStub;
import com.lody.virtual.remote.InstalledAppInfo;

import java.util.List;
import java.util.Map;

import mirror.android.app.ActivityThread;
import mirror.android.app.ContextImplKitkat;
import mirror.android.app.IActivityManager;
import mirror.android.app.LoadedApkICS;
import mirror.android.app.LoadedApkKitkat;
import mirror.android.content.ContentProviderHolderOreo;
import mirror.android.content.res.CompatibilityInfo;
import mirror.android.providers.Settings;
import mirror.android.view.CompatibilityInfoHolder;
import mirror.android.view.DisplayAdjustments;

import static com.lody.virtual.os.VUserHandle.getUserId;

public class PluginImpl extends IPluginClient.Stub {
    private static final String TAG = "PluginImpl";

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

        AppBindData data = new AppBindData();
        data.appInfo = VPackageManager.get().getApplicationInfo(packageName, 0, getUserId(mVUid));
        data.processName = processName;
        data.providers = VPackageManager.get().queryContentProviders(processName, getVUid(), PackageManager.GET_META_DATA);
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
        mPluginContext.setApplication(mInitialApplication);
        data.info = mirror.android.app.Application.mLoadedApk.get(mInitialApplication);

        Configuration configuration = mPkgResources.getConfiguration();
        Object compatInfo = CompatibilityInfo.ctor.newInstance(data.appInfo, configuration.screenLayout, configuration.smallestScreenWidthDp, false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                DisplayAdjustments.setCompatibilityInfo.call(ContextImplKitkat.mDisplayAdjustments.get(mPluginContext), compatInfo);
            }
            DisplayAdjustments.setCompatibilityInfo.call(LoadedApkKitkat.mDisplayAdjustments.get(mBoundApplication.info), compatInfo);
        } else {
            CompatibilityInfoHolder.set.call(LoadedApkICS.mCompatibilityInfo.get(mBoundApplication.info), compatInfo);
        }

        if (data.providers != null) {
            installContentProviders(mInitialApplication, data.providers);
        }
        fixInstalledProviders();

        if (lock != null) {
            lock.open();
            mTempLock = null;
        }

        try {
            PluginInjectors.get().inject();
            PluginInstrumentation.getDefault().callApplicationOnCreate(mInitialApplication);
            InvocationStubManager.getInstance().checkEnv(PluginHCallbackStub.class);
            if (conflict) {
                InvocationStubManager.getInstance().checkEnv(PluginInstrumentation.class);
            }
        } catch (Throwable e) {
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
            mInitialApplication = instrumentation.newApplication(
                    mPluginDexClassLoader, appClass, mPluginContext);
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
                if (!info.authority.startsWith(VASettings.STUB_PLUGIN_AUTHORITY)) {
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
                if (!info.authority.startsWith(VASettings.STUB_PLUGIN_AUTHORITY)) {
                    provider = ProviderHook.createProxy(true, info.authority, provider);
                    ActivityThread.ProviderClientRecordJB.mProvider.set(clientRecord, provider);
                    IActivityManager.ContentProviderHolder.provider.set(holder, provider);
                }
            } else {
                String authority = ActivityThread.ProviderClientRecord.mName.get(clientRecord);
                IInterface provider = ActivityThread.ProviderClientRecord.mProvider.get(clientRecord);
                if (provider != null && !authority.startsWith(VASettings.STUB_PLUGIN_AUTHORITY)) {
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
                mPluginDexClassLoader, mPkgResources, mPackageName, mVUid, mBoundApplication.appInfo);
        return true;
    }

    public PluginDexClassLoader getPluginDexClassLoader() {
        return mPluginDexClassLoader;
    }

    public int getVUid() {
        return mVUid;
    }

    @Override
    public IBinder getAppThread() {
        return ActivityThread.getApplicationThread.call(VirtualCore.mainThread());
    }

    public PluginContext getPluginContext() {
        return mPluginContext;
    }

    @Override
    public IBinder getToken() {
        return mToken;
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

    private final class AppBindData {
        String processName;
        ApplicationInfo appInfo;
        List<ProviderInfo> providers;
        Object info;
    }
}
