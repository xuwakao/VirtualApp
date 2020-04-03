package com.lody.virtual.plugin.hook.proxies.classloader;

import android.content.Context;

import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.interfaces.IInjector;

import mirror.android.app.ContextImpl;
import mirror.android.app.LoadedApk;

public class PluginClassLoaderStub implements IInjector {

    private PluginClassLoader mPluginClassLoader;

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
        Context context = VirtualCore.get().getContext();
        Object packageInfo = ContextImpl.mPackageInfo.get(context);
        ClassLoader classLoader = LoadedApk.mClassLoader.get(packageInfo);
        return mPluginClassLoader != classLoader;
    }
}
