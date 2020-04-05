package com.lody.virtual.plugin.core;

import android.net.Uri;

import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.helper.collection.SparseArray;
import com.lody.virtual.helper.utils.VLog;
import com.lody.virtual.plugin.PluginImpl;
import com.lody.virtual.plugin.hook.proxies.classloader.PluginClassLoader;
import com.lody.virtual.plugin.stub.PluginContentResolver;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import static com.lody.virtual.client.stub.VASettings.STUB_DECLARED_CP_COUNT;
import static com.lody.virtual.client.stub.VASettings.getDeclaredCpAuthority;

public class PluginCore {
    private static final String TAG = "PluginCore";

    private boolean useHostClassIfNotFound;
    private SparseArray<PluginImpl> mPlugins = new SparseArray<>();
    private PluginClassLoader mPluginClassLoader;
    private Map<Uri, PluginContentResolver> mPluginContentObservers = new HashMap<>();

    private static class Singleton {
        static final PluginCore singleton = new PluginCore();
    }

    public static PluginCore get() {
        return Singleton.singleton;
    }

    private PluginCore() {
        registerStubContentObserver();
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
        PluginImpl plugin = get().getPlugin(vpid);
        if (plugin == null) {
            VLog.e(TAG, "plugin is NULL with id " + vpid + ", " + name + " \n " + VLog.getStackTraceString(new IllegalStateException()));
            return null;
        }
        return plugin.loadClass(name, resolve);
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

    private static int sFreeDeclaredCpAuthority = 0;

    public static String getFreeDeclaredCpAuthority() {
        if (sFreeDeclaredCpAuthority >= STUB_DECLARED_CP_COUNT) {
            return null;
        }
        int free = sFreeDeclaredCpAuthority++;
        return getDeclaredCpAuthority(free);
    }

    private void registerStubContentObserver() {
        String resolverAuthority = getFreeDeclaredCpAuthority();
        while (resolverAuthority != null) {
            Uri auth = Uri.parse("content://" + resolverAuthority);
            VLog.d(TAG, "register stub content observer " + auth);
            PluginContentResolver pluginContentResolver = new PluginContentResolver(auth);
            VirtualCore.get().getContext().getContentResolver().registerContentObserver(auth, true, pluginContentResolver);
            mPluginContentObservers.put(auth, pluginContentResolver);
            resolverAuthority = getFreeDeclaredCpAuthority();
        }
        sFreeDeclaredCpAuthority = 0;
    }

    public Uri registerContentObserver(Uri uri, boolean notifyForDescendants, Object observer) {
        Uri free = Uri.parse("content://" + getFreeDeclaredCpAuthority());
        PluginContentResolver pluginContentResolver = mPluginContentObservers.get(free);
        if (pluginContentResolver == null) {
            VLog.e(TAG, "stub observer not found [ " + free + ", " + uri + " ]");
            return null;
        }
        VLog.d(TAG, "registerContentObserver [ " + free + ", " + uri + " ]");
        pluginContentResolver.setResolverData(uri, notifyForDescendants, observer);
        return free;
    }

    public PluginContentResolver getContentObserver(Uri uri) {
        for (Uri key : mPluginContentObservers.keySet()) {
            PluginContentResolver resolver = mPluginContentObservers.get(key);
            if (uri.equals(resolver.getResolverAuth())) {
                return resolver;
            }
        }
        return null;
    }
}
