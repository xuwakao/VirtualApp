package com.lody.virtual.plugin.hook.proxies.am;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;

import com.lody.virtual.client.VClientImpl;
import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.interfaces.IInjector;
import com.lody.virtual.client.ipc.VActivityManager;
import com.lody.virtual.helper.utils.ComponentUtils;
import com.lody.virtual.helper.utils.Reflect;
import com.lody.virtual.helper.utils.VLog;
import com.lody.virtual.plugin.PluginImpl;
import com.lody.virtual.plugin.core.PluginCore;
import com.lody.virtual.plugin.fixer.PluginMetaBundle;
import com.lody.virtual.remote.StubActivityRecord;

import java.util.Iterator;
import java.util.List;

import mirror.android.app.ActivityManagerNative;
import mirror.android.app.ActivityThread;
import mirror.android.app.IActivityManager;

/**
 * @author Lody
 * @see Handler.Callback
 */
public class PluginHCallbackStub implements Handler.Callback, IInjector {
    private static final int CREATE_SERVICE;
    private static int EXECUTE_TRANSACTION = 159;
    private static int LAUNCH_ACTIVITY = 100;
    private static int DESTROY_ACTIVITY = 109;
    private static final int SCHEDULE_CRASH;
    private static final String TAG;
    private static final PluginHCallbackStub sCallback;
    private boolean mCalling = false;
    private Handler.Callback otherCallback;

    static {
        try {
            if (Build.VERSION.SDK_INT >= 28) {
                if (ActivityThread.H.LAUNCH_ACTIVITY != null) {
                    LAUNCH_ACTIVITY = ActivityThread.H.LAUNCH_ACTIVITY.get();
                }

                if (ActivityThread.H.EXECUTE_TRANSACTION != null) {
                    EXECUTE_TRANSACTION = ActivityThread.H.EXECUTE_TRANSACTION.get();
                }
            }
        } catch (Exception exception) {
        }

        CREATE_SERVICE = ActivityThread.H.CREATE_SERVICE.get();
        DESTROY_ACTIVITY = ActivityThread.H.DESTROY_ACTIVITY.get();
        int crash;
        if (ActivityThread.H.SCHEDULE_CRASH != null) {
            crash = ActivityThread.H.SCHEDULE_CRASH.get();
        } else {
            crash = -1;
        }

        SCHEDULE_CRASH = crash;
        TAG = PluginHCallbackStub.class.getSimpleName();
        sCallback = new PluginHCallbackStub();
    }

    private PluginHCallbackStub() {
    }

    public static PluginHCallbackStub getDefault() {
        return sCallback;
    }

    private static Handler getH() {
        return ActivityThread.mH.get(VirtualCore.mainThread());
    }

    private static Handler.Callback getHCallback() {
        try {
            Handler handler = getH();
            return mirror.android.os.Handler.mCallback.get(handler);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public boolean handleMessage(Message msg) {
        if (!mCalling) {
            mCalling = true;
            try {
                if (LAUNCH_ACTIVITY == msg.what) {
                    if (!handleLaunchActivity(msg)) {
                        return true;
                    }
                } else if (DESTROY_ACTIVITY == msg.what) {
                    if (!handleDestroyActivity(msg)) {
                        return true;
                    }
                } else if (EXECUTE_TRANSACTION == msg.what) {
                    if (!handleExecuteTransaction(msg)) {
                        return true;
                    }
                } else if (CREATE_SERVICE == msg.what) {
                    /*if (!handleCreateService(msg)) {
                        return true;
                    }*/
                    Object data = msg.obj;
                    ServiceInfo serviceInfo = ActivityThread.CreateServiceData.info.get(data);
                    PluginImpl plugin = PluginCore.get().findPluginByPackage(serviceInfo.packageName);
                    if (plugin != null && !plugin.isBound()) {
                        VLog.e(TAG, "handleCreateService plugin not launched : " + serviceInfo);
                        plugin.bindPluginApp(serviceInfo.packageName, serviceInfo.processName);
                    }
                } else if (SCHEDULE_CRASH == msg.what) {
                    // to avoid the exception send from System.
                    return true;
                }
                if (otherCallback != null) {
                    boolean desired = otherCallback.handleMessage(msg);
                    mCalling = false;
                    return desired;
                } else {
                    mCalling = false;
                }
            } finally {
                mCalling = false;
            }
        }
        return false;
    }

    private boolean handleCreateService(Message msg) {
        Object data = msg.obj;
        ServiceInfo serviceInfo = ActivityThread.CreateServiceData.info.get(data);
        int pluginId = PluginMetaBundle.getPluginIdFromMeta(serviceInfo);

        PluginImpl plugin = PluginCore.get().getPlugin(pluginId);
        if (plugin == null) {
            VLog.e(TAG, "handleCreateService plugin not launched : " + serviceInfo);
            return true;
        }

        if (!plugin.isBound()) {
            plugin.bindPluginApp(serviceInfo.packageName, serviceInfo.processName);
            getH().sendMessageAtFrontOfQueue(Message.obtain(msg));
            return false;
        }
        return true;
    }

    private boolean handleDestroyActivity(Message msg) {
        IBinder token = (IBinder) msg.obj;
        VActivityManager.get().onActivityDestroy(token);
        return true;
    }

    private boolean handleExecuteTransaction(Message msg) {
        Object r = msg.obj;//ClientTransaction instance
        List<?> callbacks = Reflect.on(r).call("getCallbacks").get();
        if (callbacks != null) {
            if (callbacks.size() > 0) {
                Iterator<?> iterator = callbacks.iterator();
                while (iterator.hasNext()) {
                    try {
                        Object next = iterator.next();
                        if (next.getClass().getName().contains("LaunchActivityItem")) {
                            return handleLaunchActivity2(msg);
                        }
                    } catch (Exception e) {
                    }
                }
            }
        }
        return true;
    }

    private boolean handleLaunchActivity2(Message msg) {
        Object r = msg.obj;//ClientTransaction instance
        List<?> callbacks = Reflect.on(r).call("getCallbacks").get();
        if (callbacks != null) {
            if (callbacks.size() > 0) {
                Iterator<?> iterator = callbacks.iterator();
                while (iterator.hasNext()) {
                    try {
                        Object next = iterator.next();
                        if (next.getClass().getName().contains("LaunchActivityItem")) {
                            Intent stubIntent = Reflect.on(next).field("mIntent").get();
                            StubActivityRecord saveInstance = new StubActivityRecord(stubIntent);
                            if (saveInstance.intent == null) {
                                return true;
                            }

                            Intent intent = saveInstance.intent;
                            ComponentName caller = saveInstance.caller;
                            IBinder token = Reflect.on(r).call("getActivityToken").get();
                            ActivityInfo info = saveInstance.info;
                            int pluginId = PluginMetaBundle.getPluginIdFromMeta(info);
                            PluginImpl plugin = PluginCore.get().getPlugin(pluginId);
                            if (plugin == null) {
                                VLog.e(TAG, "handleLaunchActivity2 plugin not launched : " + saveInstance);
                                return true;
                            }

                            if (!plugin.isBound()) {
                                plugin.bindPluginApp(info.packageName, info.processName);
                                getH().sendMessageAtFrontOfQueue(Message.obtain(msg));
                                return false;
                            }

                            int taskId = IActivityManager.getTaskForActivity.call(
                                    ActivityManagerNative.getDefault.call(),
                                    token,
                                    false
                            );

                            VActivityManager.get().onActivityCreate(ComponentUtils.toComponentName(info),
                                    caller, token, info, intent, ComponentUtils.getTaskAffinity(info),
                                    taskId, info.launchMode, info.flags, pluginId);
                            ClassLoader appClassLoader = VClientImpl.get().getClassLoader(info.applicationInfo);
                            intent.setExtrasClassLoader(appClassLoader);
                            Reflect.on(next).set("mIntent", intent);
                            Reflect.on(next).set("mInfo", info);
                            return true;
                        }
                    } catch (Exception e) {
                    }
                }
            }
        }
        return false;
    }

    private boolean handleLaunchActivity(Message msg) {
        Object r = msg.obj;
        Intent stubIntent = ActivityThread.ActivityClientRecord.intent.get(r);
        StubActivityRecord saveInstance = new StubActivityRecord(stubIntent);
        if (saveInstance.intent == null) {
            return true;
        }
        Intent intent = saveInstance.intent;
        ComponentName caller = saveInstance.caller;
        IBinder token = ActivityThread.ActivityClientRecord.token.get(r);
        ActivityInfo info = saveInstance.info;
        int pluginId = PluginMetaBundle.getPluginIdFromMeta(info);
        PluginImpl plugin = PluginCore.get().getPlugin(pluginId);
        if (plugin == null) {
            VLog.e(TAG, "handleLaunchActivity plugin not launched : " + saveInstance);
            return true;
        }

        if (!plugin.isBound()) {
            plugin.bindPluginApp(info.packageName, info.processName);
            getH().sendMessageAtFrontOfQueue(Message.obtain(msg));
            return false;
        }

        int taskId = IActivityManager.getTaskForActivity.call(
                ActivityManagerNative.getDefault.call(),
                token,
                false
        );

        VActivityManager.get().onActivityCreate(ComponentUtils.toComponentName(info), caller, token, info, intent, ComponentUtils.getTaskAffinity(info), taskId, info.launchMode, info.flags, pluginId);
        ClassLoader appClassLoader = VClientImpl.get().getClassLoader(info.applicationInfo);
        intent.setExtrasClassLoader(appClassLoader);
        ActivityThread.ActivityClientRecord.intent.set(r, intent);
        ActivityThread.ActivityClientRecord.activityInfo.set(r, info);
        return true;
    }

    @Override
    public void inject() throws Throwable {
        otherCallback = getHCallback();
        mirror.android.os.Handler.mCallback.set(getH(), this);
    }

    @Override
    public boolean isEnvBad() {
        Handler.Callback callback = getHCallback();
        boolean envBad = callback != this;
        if (callback != null && envBad) {
            VLog.d(TAG, "PluginHCallback has bad, other callback = " + callback);
        }
        return envBad;
    }

}
