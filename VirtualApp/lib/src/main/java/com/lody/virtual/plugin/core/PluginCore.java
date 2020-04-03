package com.lody.virtual.plugin.core;

import com.lody.virtual.helper.collection.SparseArray;
import com.lody.virtual.plugin.PluginImpl;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.HashSet;

public class PluginCore {

    private boolean useHostClassIfNotFound;
    public SparseArray<PluginImpl> mPluginCp = new SparseArray<>();

    private static class Singleton {
        static final PluginCore singleton = new PluginCore();
    }


    public static PluginCore get() {
        return Singleton.singleton;
    }

    public void putPlugin(int vpid, PluginImpl client) {
        synchronized (mPluginCp) {
            mPluginCp.put(vpid, client);
        }
    }

    /**
     * Get plugin client by plugin id
     *
     * @param vpid plugin id
     * @return
     */
    public PluginImpl getClient(int vpid) {
        synchronized (mPluginCp) {
            return mPluginCp.get(vpid);
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
}
