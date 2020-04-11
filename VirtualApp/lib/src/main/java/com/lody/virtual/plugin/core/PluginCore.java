package com.lody.virtual.plugin.core;

import android.content.ComponentName;
import android.content.Intent;

import com.lody.virtual.helper.collection.SparseArray;
import com.lody.virtual.helper.utils.VLog;
import com.lody.virtual.plugin.PluginImpl;

import java.util.HashMap;
import java.util.Map;

public class PluginCore {
    private static final String TAG = "PluginCore";

    private SparseArray<PluginImpl> mPlugins = new SparseArray<>();
    private Map<String, PluginImpl> mAuthPlugins = new HashMap<>();

    public PluginImpl getPluginByCpAuth(String auth) {
        synchronized (mAuthPlugins) {
            return mAuthPlugins.get(auth);
        }
    }

    public void putPluginByCpAuth(String auth, PluginImpl plugin) {
        synchronized (mAuthPlugins) {
            mAuthPlugins.put(auth, plugin);
        }
    }

    private static class Singleton {
        static final PluginCore singleton = new PluginCore();
    }

    public static PluginCore get() {
        return Singleton.singleton;
    }

    private PluginCore() {
    }

    public void putPlugin(int vpid, PluginImpl client) {
        synchronized (mPlugins) {
            mPlugins.put(vpid, client);
        }
    }

    public SparseArray<PluginImpl> getPlugins() {
        return mPlugins;
    }

    public PluginImpl findPlugin(Intent intent) {
        synchronized (mPlugins) {
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
                if (plugin.isBound() && plugin.getApplicationInfo().packageName.equals(packageName)) {
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
}
