package com.lody.virtual.plugin;

import android.content.Context;

import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.plugin.stub.PluginContentProvider;
import com.lody.virtual.client.interfaces.IInjector;
import com.lody.virtual.helper.collection.SparseArray;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.HashSet;

import mirror.android.app.ContextImpl;
import mirror.android.app.LoadedApk;

public class PluginCore implements IInjector {

    private PluginClassLoader mPluginClassLoader;
    private boolean useHostClassIfNotFound;
    public SparseArray<PluginContentProvider> mPluginCp = new SparseArray<>();

    public boolean isUseHostClassIfNotFound() {
        return useHostClassIfNotFound;
    }

    public void setUseHostClassIfNotFound(boolean useHostClassIfNotFound) {
        this.useHostClassIfNotFound = useHostClassIfNotFound;
    }

    public void putPlugin(int vpid, PluginContentProvider pluginContentProvider) {
        mPluginCp.put(vpid, pluginContentProvider);
    }

    private static class Singleton {
        static final PluginCore singleton = new PluginCore();
    }

    /**
     * layout缓存：忽略表
     */
    private HashSet<String> mCacheLayouts = new HashSet<String>();

    /**
     * layout缓存：构造器表
     */
    private HashMap<String, Constructor<?>> mConstructors = new HashMap<String, Constructor<?>>();

    public static PluginCore get() {
        return Singleton.singleton;
    }

    @Override
    public void inject() throws Throwable {
        Context context = VirtualCore.get().getContext();
        Object packageInfo = ContextImpl.mPackageInfo.get(context);
        ClassLoader classLoader = LoadedApk.mClassLoader.get(packageInfo);
        mPluginClassLoader = new PluginClassLoader(classLoader.getParent(), classLoader);
        LoadedApk.mClassLoader.set(packageInfo, mPluginClassLoader);
        Thread.currentThread().setContextClassLoader(mPluginClassLoader);
    }

    @Override
    public boolean isEnvBad() {
        return false;
    }

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

    public PluginClassLoader getPluginClassLoader() {
        return mPluginClassLoader;
    }

    /**
     * Get plugin client by plugin id
     * @param vpid plugin id
     * @return
     */
    public PluginImpl getClient(int vpid) {
        PluginContentProvider contentProvider = mPluginCp.get(vpid);
        if (contentProvider != null) {
            return contentProvider.getClient();
        }
        return null;
    }
}
