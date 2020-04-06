package com.lody.virtual.plugin.hook.proxies.classloader;

import com.lody.virtual.helper.utils.Reflect;
import com.lody.virtual.helper.utils.VLog;

import java.net.URL;
import java.util.Enumeration;

import dalvik.system.PathClassLoader;
import mirror.dalvik.system.BaseDexClassLoader;

public class PluginClassLoader extends PathClassLoader {
    private static final String TAG = "PluginClassLoader";

    private final ClassLoader mOriginal;

    public PluginClassLoader(ClassLoader originalParent, ClassLoader original) {
        super("", "", originalParent);
        mOriginal = original;

        Object pathList = BaseDexClassLoader.pathList.get(mOriginal);
        Reflect.on(this).set("pathList", pathList);
        VLog.d(TAG, "PluginClassLoader pathList == " + Reflect.on(this).field("pathList").get());
    }

    @Override
    public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        Class<?> loadClass;
        try {
            loadClass = mOriginal.loadClass(name);
//                VLog.d(TAG, "Loading class : " + loadClass);
            return loadClass;
        } catch (ClassNotFoundException e) {
            VLog.w(TAG, "class not found in original : " + e);
        }
        return super.loadClass(name, resolve);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        return super.findClass(name);
    }

    @Override
    protected URL findResource(String name) {
        try {
            return BaseDexClassLoader.findResource.call(mOriginal, name);
        } catch (Throwable e) {
            VLog.w(TAG, "resource not found in original : " + e);
        }
        return super.findResource(name);
    }

    @Override
    protected Enumeration<URL> findResources(String name) {
        try {
            return BaseDexClassLoader.findResources.call(mOriginal, name);
        } catch (Throwable e) {
            VLog.e(TAG, "resources not found in original : " + e);
        }
        return super.findResources(name);
    }

    @Override
    public String findLibrary(String name) {
        try {
            return (String) BaseDexClassLoader.findLibrary.call(mOriginal, name);
        } catch (Throwable e) {
            VLog.e(TAG, "library not found in original : " + e);
        }
        return super.findLibrary(name);
    }

    @Override
    protected synchronized Package getPackage(String name) {
        // 金立手机的某些ROM(F103,F103L,F303,M3)代码ClassLoader.getPackage去掉了关键的保护和错误处理(2015.11~2015.12左右)，会返回null
        // 悬浮窗某些draw代码触发getPackage(...).getName()，getName出现空指针解引，导致悬浮窗进程出现了大量崩溃
        // 此处实现和AOSP一致确保不会返回null
        // SONGZHAOCHUN, 2016/02/29
        if (name != null && !name.isEmpty()) {
            Package pack = null;
            try {
                pack = (Package) BaseDexClassLoader.getPackage.call(mOriginal, name);
            } catch (Throwable e) {
                e.printStackTrace();
            }
            if (pack == null) {
                VLog.w(TAG, "NRH lcl.gp.1: n=" + name);
                pack = super.getPackage(name);
            }
            if (pack == null) {
                VLog.w(TAG, "NRH lcl.gp.2: n=" + name);
                return definePackage(name, "Unknown", "0.0", "Unknown", "Unknown", "0.0", "Unknown", null);
            }
            return pack;
        }
        return null;
    }
}
