package com.lody.virtual.client.hook.plugin;

import android.app.Activity;
import android.app.Instrumentation;
import android.os.Bundle;
import android.os.PersistableBundle;

import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.hook.delegate.AppInstrumentation;
import com.lody.virtual.client.hook.delegate.InstrumentationDelegate;
import com.lody.virtual.client.interfaces.IInjector;

import mirror.android.app.ActivityThread;

public class PluginInstrumentation extends InstrumentationDelegate implements IInjector {

    private static PluginInstrumentation gDefault;

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
        base = ActivityThread.mInstrumentation.get(VirtualCore.mainThread());
        ActivityThread.mInstrumentation.set(VirtualCore.mainThread(), this);
    }

    @Override
    public boolean isEnvBad() {
        return !(ActivityThread.mInstrumentation.get(VirtualCore.mainThread()) instanceof AppInstrumentation);
    }

    @Override
    public void callActivityOnCreate(Activity activity, Bundle icicle) {
        if(activity.getClass().getName().indexOf("MainActivity") >= 0) {
            PluginFixer.fixActivity(activity, 2131165184, 2131034112, 2131034112);
        }
        super.callActivityOnCreate(activity, icicle);
    }

    @Override
    public void callActivityOnCreate(Activity activity, Bundle icicle, PersistableBundle persistentState) {
        if(activity.getClass().getName().indexOf("MainActivity") >= 0) {
            PluginFixer.fixActivity(activity, 2131165184, 2131034112, 2131034112);
        }
        super.callActivityOnCreate(activity, icicle, persistentState);
    }
}
