package com.lody.virtual.client.hook.plugin;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.IBinder;

import com.lody.virtual.BuildConfig;
import com.lody.virtual.client.core.InvocationStubManager;
import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.ipc.VPackageManager;
import com.lody.virtual.helper.utils.Reflect;
import com.lody.virtual.helper.utils.VLog;
import com.lody.virtual.os.VUserHandle;
import com.lody.virtual.plugin.IPluginClient;
import com.lody.virtual.remote.InstalledAppInfo;

import java.lang.reflect.InvocationTargetException;

import mirror.android.app.ActivityThread;

public class PluginImpl extends IPluginClient.Stub {
    private static final String TAG = "PluginImpl";

    private IBinder mToken;
    private int mVUid;
    private int mVPid;
    private String mPackageName;
    private Context mContext;
    private Resources mPkgResources;
    private ClassLoader mParent;
    private PluginDexClassLoader mPluginDexClassLoader;
    private PluginContext mPluginContext;
    private InstalledAppInfo mInstalledAppInfo;
    private ApplicationInfo mApplicationInfo;

    public static PluginImpl create() {
        return new PluginImpl();
    }

    public void initPlugin(IBinder token, int vuid, int vpid, String pkgName) {
        mToken = token;
        mVUid = vuid;
        mVPid = vpid;
        mPackageName = pkgName;
        mInstalledAppInfo = VirtualCore.get().getInstalledAppInfo(pkgName, 0);
        mApplicationInfo = mInstalledAppInfo.getApplicationInfo(VUserHandle.getUserId(mVUid));

        VLog.d(TAG, "install info : " + mInstalledAppInfo);
        VLog.d(TAG, "application info : " + mApplicationInfo);

        mContext = VirtualCore.get().getContext();

        try {
            loadPlugin();
            callEntry();
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        InvocationStubManager.getInstance().checkEnv(PluginInstrumentation.class);
    }

    private void callEntry() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        //TODO test code
        Reflect.on("com.owttwo.testcase.Entry", mPluginDexClassLoader)
                .exactMethod("create", new Class[]{Context.class, ClassLoader.class, IBinder.class})
                .invoke(null, mPluginContext, getClass().getClassLoader(), null);
    }

    private boolean loadPlugin() throws PackageManager.NameNotFoundException {
        PackageManager pm = mContext.getPackageManager();
        if (BuildConfig.DEBUG) {
            // 如果是Debug模式的话，防止与Instant Run冲突，资源重新New一个
            Resources r = pm.getResourcesForApplication(mApplicationInfo);
            mPkgResources = new Resources(r.getAssets(), r.getDisplayMetrics(), r.getConfiguration());
        } else {
            mPkgResources = pm.getResourcesForApplication(mApplicationInfo);
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
        mPluginDexClassLoader = new PluginDexClassLoader(mPackageName, mApplicationInfo.sourceDir,
                mInstalledAppInfo.getOdexFile().getParent(), mApplicationInfo.nativeLibraryDir, mParent);
        if (mPluginDexClassLoader == null) {
            VLog.w(TAG, "get dex null");
            return false;
        }

        mPluginContext = new PluginContext(mContext, mApplicationInfo.theme, mPluginDexClassLoader,
                mPkgResources, mPackageName, mVUid, mInstalledAppInfo);
        return true;
    }

    public PluginDexClassLoader getPluginDexClassLoader() {
        return mPluginDexClassLoader;
    }

    public String getCurrentPackage() {
        return mPackageName != null ?
                mPackageName : VPackageManager.get().getNameForUid(getVUid());
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
        return mApplicationInfo;
    }
}
