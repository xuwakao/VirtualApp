package com.lody.virtual.plugin.core;

import com.lody.virtual.helper.collection.SparseArray;
import com.lody.virtual.plugin.PluginImpl;
import com.lody.virtual.plugin.hook.proxies.classloader.PluginClassLoader;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.HashSet;

public class PluginCore {

    private boolean useHostClassIfNotFound;
    private SparseArray<PluginImpl> mPlugins = new SparseArray<>();
    private PluginClassLoader mPluginClassLoader;

    private static class Singleton {
        static final PluginCore singleton = new PluginCore();
    }


    public static PluginCore get() {
        return Singleton.singleton;
    }

    public void putPlugin(int vpid, PluginImpl client) {
        synchronized (mPlugins) {
            mPlugins.put(vpid, client);
        }
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

    public static Class<?> loadClass(int vpid, String name, boolean resolve) {
        return get().getPlugin(vpid).loadClass(name, resolve);
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
}
