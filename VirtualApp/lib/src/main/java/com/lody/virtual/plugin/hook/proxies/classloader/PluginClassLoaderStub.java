package com.lody.virtual.plugin.hook.proxies.classloader;

import android.content.Context;

import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.interfaces.IInjector;
import com.lody.virtual.plugin.core.PluginCore;

import mirror.android.app.ContextImpl;
import mirror.android.app.LoadedApk;

public class PluginClassLoaderStub implements IInjector {
    @Override
    public void inject() throws Throwable {
        Context context = VirtualCore.get().getContext();
        Object packageInfo = ContextImpl.mPackageInfo.get(context);
        ClassLoader classLoader = LoadedApk.mClassLoader.get(packageInfo);
        PluginClassLoader pluginClassLoader = new PluginClassLoader(classLoader.getParent(), classLoader);
        LoadedApk.mClassLoader.set(packageInfo, pluginClassLoader);
        Thread.currentThread().setContextClassLoader(pluginClassLoader);
        PluginCore.get().setClassLoader(pluginClassLoader);
    }

    @Override
    public boolean isEnvBad() {
        Context context = VirtualCore.get().getContext();
        Object packageInfo = ContextImpl.mPackageInfo.get(context);
        ClassLoader classLoader = LoadedApk.mClassLoader.get(packageInfo);
        return PluginCore.get().getClassLoader() != classLoader;
    }
}
