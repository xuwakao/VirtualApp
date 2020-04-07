package com.lody.virtual.plugin.hook.delegate;

import android.app.Activity;
import android.app.Application;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PersistableBundle;

import com.lody.virtual.client.core.InvocationStubManager;
import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.fixer.ContextFixer;
import com.lody.virtual.client.hook.delegate.InstrumentationDelegate;
import com.lody.virtual.client.interfaces.IInjector;
import com.lody.virtual.client.ipc.VActivityManager;
import com.lody.virtual.plugin.PluginImpl;
import com.lody.virtual.plugin.core.PluginCore;
import com.lody.virtual.plugin.fixer.PluginFixer;
import com.lody.virtual.plugin.hook.proxies.am.PluginHCallbackStub;
import com.lody.virtual.plugin.hook.proxies.classloader.PluginClassLoader;
import com.lody.virtual.remote.StubActivityRecord;

import mirror.android.app.ActivityThread;

public class PluginInstrumentation extends InstrumentationDelegate implements IInjector {

    private static PluginInstrumentation gDefault;
    /**
     * Don't inject twice
     */
    private static boolean sInject = false;

    private PluginInstrumentation(Instrumentation base) {
        super(base);
    }

    public static PluginInstrumentation getDefault() {
        if (gDefault == null) {
            synchronized (PluginInstrumentation.class) {
                if (gDefault == null) {
                    gDefault = create();
                }
            }
        }
        return gDefault;
    }

    private static PluginInstrumentation create() {
        Instrumentation instrumentation = ActivityThread.mInstrumentation.get(VirtualCore.mainThread());
        if (instrumentation instanceof PluginInstrumentation) {
            return (PluginInstrumentation) instrumentation;
        }
        return new PluginInstrumentation(instrumentation);
    }

    @Override
    public void inject() throws Throwable {
        if (!sInject) {
            base = ActivityThread.mInstrumentation.get(VirtualCore.mainThread());
            ActivityThread.mInstrumentation.set(VirtualCore.mainThread(), this);
            sInject = true;
        }
    }

    @Override
    public boolean isEnvBad() {
        return !(ActivityThread.mInstrumentation.get(VirtualCore.mainThread()) instanceof PluginInstrumentation);
    }

    @Override
    public void callActivityOnResume(Activity activity) {
        VActivityManager.get().onActivityResumed(activity);
        super.callActivityOnResume(activity);
    }

    @Override
    public Activity newActivity(Class<?> clazz, Context context, IBinder token, Application application, Intent intent, ActivityInfo info, CharSequence title, Activity parent, String id, Object lastNonConfigurationInstance) throws InstantiationException, IllegalAccessException {
        StubActivityRecord r = new StubActivityRecord(intent);
        if (r.intent == null) {
            return super.newActivity(clazz, context, token, application, intent, info, title, parent, id, lastNonConfigurationInstance);
        }

        PluginImpl plugin = PluginCore.get().getPlugin(r.pluginId);
        clazz = plugin.loadClass(r.info.name, true);
        Activity activity = super.newActivity(clazz, context, token, application, intent, info, title, parent, id, lastNonConfigurationInstance);
        mirror.android.app.Activity.mApplication.set(activity, plugin.getApp());
        return activity;
    }

    @Override
    public Activity newActivity(ClassLoader cl, String className, Intent intent) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        StubActivityRecord r = new StubActivityRecord(intent);
        if (r.intent == null) {
            return super.newActivity(cl, className, intent);
        }

        className = r.info.name;
        PluginImpl plugin = PluginCore.get().getPlugin(r.pluginId);
        if (cl instanceof PluginClassLoader) {
            cl = plugin.getPluginDexClassLoader();
        }
        Activity activity = super.newActivity(cl, className, intent);
        mirror.android.app.Activity.mApplication.set(activity, plugin.getApp());
        return activity;
    }

    @Override
    public void callActivityOnCreate(Activity activity, Bundle icicle) {
        StubActivityRecord r = new StubActivityRecord(activity.getIntent());
        if (r.intent == null) {
            super.callActivityOnCreate(activity, icicle);
            return;
        }
        PluginImpl plugin = PluginCore.get().getPlugin(r.pluginId);
        PluginFixer.fixActivity(activity, plugin);
        ContextFixer.fixContext(activity);
        r.intent.setExtrasClassLoader(plugin.getPluginDexClassLoader());
        InvocationStubManager.getInstance().checkEnv(PluginHCallbackStub.class);
        super.callActivityOnCreate(activity, icicle);
    }

    @Override
    public void callActivityOnCreate(Activity activity, Bundle icicle, PersistableBundle persistentState) {
        StubActivityRecord r = new StubActivityRecord(activity.getIntent());
        if (r.intent == null) {
            super.callActivityOnCreate(activity, icicle);
            return;
        }
        PluginImpl plugin = PluginCore.get().getPlugin(r.pluginId);
        PluginFixer.fixActivity(activity, plugin);
        ContextFixer.fixContext(activity);
        r.intent.setExtrasClassLoader(plugin.getPluginDexClassLoader());
        InvocationStubManager.getInstance().checkEnv(PluginHCallbackStub.class);
        super.callActivityOnCreate(activity, icicle, persistentState);
    }
}
