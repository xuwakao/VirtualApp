package com.lody.virtual.plugin;

import android.app.Application;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.ConditionVariable;
import android.os.IBinder;
import android.os.Looper;
import android.text.TextUtils;

import com.lody.virtual.BuildConfig;
import com.lody.virtual.client.core.InvocationStubManager;
import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.env.VirtualRuntime;
import com.lody.virtual.helper.utils.VLog;
import com.lody.virtual.os.VUserHandle;
import com.lody.virtual.plugin.hook.delegate.PluginInstrumentation;
import com.lody.virtual.remote.InstalledAppInfo;

import mirror.android.app.ActivityThread;

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
    private InstalledAppInfo mInstalledAppInfo;
    private ApplicationInfo mPkgAppInfo;
    private Application mBoundApplication;
    private ConditionVariable mTempLock;

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
        mInstalledAppInfo = VirtualCore.get().getInstalledAppInfo(packageName, 0);
        mPkgAppInfo = mInstalledAppInfo.getApplicationInfo(VUserHandle.getUserId(mVUid));

        try {
            loadPlugin();
            InvocationStubManager.getInstance().checkEnv(PluginInstrumentation.class);
            makeApplication();
            mPluginContext.setApplication(mBoundApplication);
        } catch (PackageManager.NameNotFoundException e) {
            VLog.e(TAG, "package not found " + e);
        }

        if (lock != null) {
            lock.open();
            mTempLock = null;
        }

        PluginInstrumentation.getDefault().callApplicationOnCreate(mBoundApplication);
    }

    private void makeApplication() {
        String appClass = TextUtils.isEmpty(mPkgAppInfo.className) ?
                Application.class.getName() : mPkgAppInfo.className;
        PluginInstrumentation instrumentation = PluginInstrumentation.getDefault();
        try {
            mBoundApplication = instrumentation.newApplication(
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

    private boolean loadPlugin() throws PackageManager.NameNotFoundException {
        PackageManager pm = VirtualCore.get().getContext().getPackageManager();
        if (BuildConfig.DEBUG) {
            // 如果是Debug模式的话，防止与Instant Run冲突，资源重新New一个
            Resources r = pm.getResourcesForApplication(mPkgAppInfo);
            mPkgResources = new Resources(r.getAssets(), r.getDisplayMetrics(), r.getConfiguration());
        } else {
            mPkgResources = pm.getResourcesForApplication(mPkgAppInfo);
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
        mPluginDexClassLoader = new PluginDexClassLoader(mPackageName, mPkgAppInfo.sourceDir,
                mInstalledAppInfo.getOdexFile().getParent(), mPkgAppInfo.nativeLibraryDir, mParent);
        if (mPluginDexClassLoader == null) {
            VLog.w(TAG, "get dex null");
            return false;
        }

        mPluginContext = new PluginContext(VirtualCore.get().getContext(), mPkgAppInfo.theme,
                mPluginDexClassLoader, mPkgResources, mPackageName, mVUid, mInstalledAppInfo);
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
        return mPkgAppInfo;
    }

    public Application getApp() {
        return mBoundApplication;
    }
}
