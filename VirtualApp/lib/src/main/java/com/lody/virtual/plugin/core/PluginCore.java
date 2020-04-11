package com.lody.virtual.plugin.core;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.os.Process;

import com.lody.virtual.helper.collection.SparseArray;
import com.lody.virtual.helper.utils.VLog;
import com.lody.virtual.plugin.PluginImpl;
import com.lody.virtual.plugin.fixer.PluginMetaBundle;
import com.lody.virtual.plugin.hook.proxies.classloader.PluginClassLoader;
import com.lody.virtual.plugin.utils.PluginHandle;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class PluginCore {
    private static final String TAG = "PluginCore";

    private boolean useHostClassIfNotFound;
    private SparseArray<PluginImpl> mPlugins = new SparseArray<>();
    private PluginClassLoader mPluginClassLoader;
    private PluginCallerContext mCallerContext;

    private static class Singleton {
        static final PluginCore singleton = new PluginCore();
    }

    public static PluginCore get() {
        return Singleton.singleton;
    }

    private PluginCore() {
        mCallerContext = new PluginCallerContext();
    }

    public void putPlugin(int vpid, PluginImpl client) {
        synchronized (mPlugins) {
            mPlugins.put(vpid, client);
        }
    }

    public PluginImpl findPlugin(Intent intent) {
        synchronized (mPlugins) {
            int pluginId = PluginMetaBundle.getIntentPluginId(intent);
            if (PluginHandle.isPluginVPid(pluginId)) {
                return getPlugin(pluginId);
            }
            String packageName = intent.getPackage();
            ComponentName component = intent.getComponent();
            if (packageName == null) {
                if (component != null) {
                    packageName = component.getPackageName();
                } else {
                    VLog.w(TAG, "intent has no package info." + VLog.getStackTraceString(new Exception()));
                    return null;
                }
            }
            return findPluginByPackage(packageName);
        }
    }

    public PluginImpl findPluginByPackage(String packageName) {
        synchronized (mPlugins) {
            for (int i = 0; i < mPlugins.size(); i++) {
                PluginImpl plugin = mPlugins.valueAt(i);
                if (plugin.getApplicationInfo().packageName.equals(packageName)) {
                    return plugin;
                }
            }
        }
        return null;
    }

    /**
     * Get plugin client by plugin id
     *
     * @param vpid plugin id
     * @return
     */
    public PluginImpl getPlugin(int vpid) {
        synchronized (mPlugins) {
            return mPlugins.get(vpid);
        }
    }

    public List<ActivityManager.RunningAppProcessInfo> getProcessList() {
        synchronized (mPlugins) {
            List<ActivityManager.RunningAppProcessInfo> infoList = new ArrayList<>();
            int latestCallRunningProcessPlugin = mCallerContext.getLatestCallRunningProcessPlugin();
            if (latestCallRunningProcessPlugin >= 0) {
                PluginImpl plugin = getPlugin(latestCallRunningProcessPlugin);
                ApplicationInfo applicationInfo = plugin.getApplicationInfo();
                ActivityManager.RunningAppProcessInfo processInfo = new ActivityManager.RunningAppProcessInfo();
                processInfo.pid = Process.myPid();
                processInfo.processName = applicationInfo.processName == null ? applicationInfo.packageName : applicationInfo.packageName;
                infoList.add(processInfo);
            }
            /*for (int i = 0; i < mPlugins.size(); i++) {
                PluginImpl plugin = mPlugins.valueAt(i);
                ActivityManager.RunningAppProcessInfo processInfo = new ActivityManager.RunningAppProcessInfo();
                processInfo.pid = Process.myPid();
                ApplicationInfo applicationInfo = plugin.getApplicationInfo();
                VLog.d(TAG, "get plugin process list : [ " + applicationInfo.processName + " ]");
                processInfo.processName = applicationInfo.processName == null ? applicationInfo.packageName : applicationInfo.packageName;
                infoList.add(processInfo);
            }*/
            return infoList;
        }
    }

    public boolean isUseHostClassIfNotFound() {
        return useHostClassIfNotFound;
    }

    public void setUseHostClassIfNotFound(boolean useHostClassIfNotFound) {
        this.useHostClassIfNotFound = useHostClassIfNotFound;
    }

    /**
     * layout缓存：忽略表
     */
    private HashSet<String> mCacheLayouts = new HashSet<String>();

    /**
     * layout缓存：构造器表
     */
    private HashMap<String, Constructor<?>> mConstructors = new HashMap<String, Constructor<?>>();

    public boolean isCacheLayout(String name) {
        return mCacheLayouts.contains(name);
    }

    public boolean addCacheLayout(String name) {
        return mCacheLayouts.add(name);
    }

    public Constructor<?> getCacheConstructor(String name) {
        return mConstructors.get(name);
    }

    public void addCacheConstructor(String name, Constructor<?> constructor) {
        mConstructors.put(name, constructor);
    }

    public void setClassLoader(PluginClassLoader classLoader) {
        mPluginClassLoader = classLoader;
    }

    public PluginClassLoader getClassLoader() {
        return mPluginClassLoader;
    }


    public void setLatestCallRunningProcessPlugin(int pluginId) {
        mCallerContext.setLatestCallRunningProcessPlugin(pluginId);
    }

    public int getLatestCallRunningProcessPlugin() {
        return mCallerContext.getLatestCallRunningProcessPlugin();
    }
}
