package com.lody.virtual.plugin.hook.proxies;

import android.os.Build;

import com.lody.virtual.client.core.InvocationStubManager;
import com.lody.virtual.client.hook.proxies.alarm.AlarmManagerStub;
import com.lody.virtual.client.hook.proxies.appops.AppOpsManagerStub;
import com.lody.virtual.client.hook.proxies.job.JobServiceStub;
import com.lody.virtual.client.hook.proxies.location.LocationManagerStub;
import com.lody.virtual.client.hook.proxies.notification.NotificationManagerStub;
import com.lody.virtual.client.hook.proxies.view.AutoFillManagerStub;
import com.lody.virtual.client.interfaces.IInjector;

import java.util.HashMap;
import java.util.Map;

import static android.os.Build.VERSION_CODES.JELLY_BEAN_MR2;
import static android.os.Build.VERSION_CODES.KITKAT;
import static android.os.Build.VERSION_CODES.LOLLIPOP;

public class PluginInjectors implements IInjector {
    private Map<Class<?>, IInjector> mInjectors = new HashMap<>(13);

    private static class Singleton {
        static final PluginInjectors singleton = new PluginInjectors();
    }

    public static PluginInjectors get() {
        return Singleton.singleton;
    }

    public PluginInjectors() {
//        addInjector(new PluginClassLoaderStub());
        addInjector(new NotificationManagerStub());
        addInjector(new LocationManagerStub());

        if (Build.VERSION.SDK_INT >= JELLY_BEAN_MR2) {
//            addInjector(new VibratorStub());
//            addInjector(new WifiManagerStub());
//            addInjector(new BluetoothStub());
//            addInjector(new ContextHubServiceStub());
        }
        if (Build.VERSION.SDK_INT >= KITKAT) {
            addInjector(new AlarmManagerStub());
            addInjector(new AppOpsManagerStub());
//            addInjector(new MediaRouterServiceStub());
        }
        if (Build.VERSION.SDK_INT >= LOLLIPOP) {
            addInjector(new JobServiceStub());
        }
        if (Build.VERSION.SDK_INT >= 26) {
            addInjector(new AutoFillManagerStub());//must autofill
        }
    }

    @Override
    public void inject() throws Throwable {
        for (Class clz : mInjectors.keySet()) {
            InvocationStubManager.getInstance().addInjector(mInjectors.get(clz));
            InvocationStubManager.getInstance().checkEnv(clz);
        }
    }

    @Override
    public boolean isEnvBad() {
        return false;
    }

    private void addInjector(IInjector IInjector) {
        mInjectors.put(IInjector.getClass(), IInjector);
    }
}
