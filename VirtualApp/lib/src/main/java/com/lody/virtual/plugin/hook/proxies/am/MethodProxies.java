package com.lody.virtual.plugin.hook.proxies.am;

import android.app.ActivityManager;
import android.app.IServiceConnection;
import android.app.Notification;
import android.app.Service;
import android.content.ComponentName;
import android.content.IIntentReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.ComponentInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Process;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;

import com.lody.virtual.client.badger.BadgerManager;
import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.env.Constants;
import com.lody.virtual.client.env.SpecialComponentList;
import com.lody.virtual.client.hook.base.MethodProxy;
import com.lody.virtual.client.hook.providers.ProviderHook;
import com.lody.virtual.client.hook.secondary.ServiceConnectionDelegate;
import com.lody.virtual.client.hook.utils.MethodParameterUtils;
import com.lody.virtual.client.ipc.ActivityClientRecord;
import com.lody.virtual.client.ipc.VActivityManager;
import com.lody.virtual.client.ipc.VNotificationManager;
import com.lody.virtual.client.ipc.VPackageManager;
import com.lody.virtual.client.stub.ChooserActivity;
import com.lody.virtual.client.stub.StubPendingActivity;
import com.lody.virtual.client.stub.StubPendingReceiver;
import com.lody.virtual.client.stub.StubPendingService;
import com.lody.virtual.client.stub.VASettings;
import com.lody.virtual.helper.collection.SparseArray;
import com.lody.virtual.helper.compat.ActivityManagerCompat;
import com.lody.virtual.helper.compat.BuildCompat;
import com.lody.virtual.helper.utils.ArrayUtils;
import com.lody.virtual.helper.utils.BitmapUtils;
import com.lody.virtual.helper.utils.ComponentUtils;
import com.lody.virtual.helper.utils.FileUtils;
import com.lody.virtual.helper.utils.Reflect;
import com.lody.virtual.helper.utils.VLog;
import com.lody.virtual.os.VUserHandle;
import com.lody.virtual.os.VUserInfo;
import com.lody.virtual.plugin.PluginImpl;
import com.lody.virtual.plugin.core.PluginCore;
import com.lody.virtual.plugin.fixer.PluginMetaBundle;
import com.lody.virtual.plugin.utils.PluginHandle;
import com.lody.virtual.remote.AppTaskInfo;
import com.lody.virtual.server.am.ServiceRecord;
import com.lody.virtual.server.interfaces.IAppRequestListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;

import mirror.android.app.IActivityManager;
import mirror.android.app.LoadedApk;
import mirror.android.content.ContentProviderHolderOreo;
import mirror.android.content.IIntentReceiverJB;
import mirror.android.content.pm.UserInfo;

import static com.lody.virtual.client.stub.VASettings.INTERCEPT_BACK_HOME;

/**
 * @author Lody
 */
@SuppressWarnings("unused")
class MethodProxies {

    static class AddPackageDependency extends MethodProxy {

        @Override
        public String getMethodName() {
            return "addPackageDependency";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            MethodParameterUtils.replaceFirstAppPkg(args);
            return method.invoke(who, args);
        }
    }

    static class GetPackageForToken extends MethodProxy {

        @Override
        public String getMethodName() {
            return "getPackageForToken";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            IBinder token = (IBinder) args[0];
            String pkg = VActivityManager.get().getPackageForToken(token);
            if (pkg != null) {
                return pkg;
            }
            return super.call(who, method, args);
        }
    }

    static class UnbindService extends MethodProxy {

        @Override
        public String getMethodName() {
            return "unbindService";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            IServiceConnection conn = (IServiceConnection) args[0];
            ServiceConnectionDelegate delegate = ServiceConnectionDelegate.removeDelegate(conn);
            if (delegate == null) {
                return method.invoke(who, args);
            }
            return VActivityManager.get().unbindService(delegate);
        }
    }

    static class GetContentProviderExternal extends GetContentProvider {

        @Override
        public String getMethodName() {
            return "getContentProviderExternal";
        }

        @Override
        public int getProviderNameIndex() {
            return 0;
        }
    }

    static class StartVoiceActivity extends StartActivity {
        @Override
        public String getMethodName() {
            return "startVoiceActivity";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            return super.call(who, method, args);
        }
    }


    static class UnstableProviderDied extends MethodProxy {

        @Override
        public String getMethodName() {
            return "unstableProviderDied";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            if (args[0] == null) {
                return 0;
            }
            return method.invoke(who, args);
        }
    }


    static class PeekService extends MethodProxy {

        @Override
        public String getMethodName() {
            return "peekService";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            Intent service = (Intent) args[0];
            String resolvedType = (String) args[1];
            PluginImpl plugin = PluginCore.get().findPlugin(service);
            if (plugin == null) {
                return method.invoke(who, args);
            }
            MethodParameterUtils.replaceLastAppPkg(args);
            IBinder result = VActivityManager.get().peekService(service, resolvedType, plugin.getUserId());
            return result;
        }
    }


    static class GetPackageAskScreenCompat extends MethodProxy {

        @Override
        public String getMethodName() {
            return "getPackageAskScreenCompat";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                if (args.length > 0 && args[0] instanceof String) {
                    args[0] = getHostPkg();
                }
            }
            return method.invoke(who, args);
        }

        @Override
        public boolean isEnable() {
            return isAppProcess();
        }
    }


    static class GetIntentSender extends MethodProxy {

        @Override
        public String getMethodName() {
            return "getIntentSender";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            String creator = (String) args[1];
            String[] resolvedTypes = (String[]) args[6];
            int type = (int) args[0];
            int flags = (int) args[7];
            if (args[5] instanceof Intent[]) {
                Intent[] intents = (Intent[]) args[5];
                for (int i = 0; i < intents.length; i++) {
                    Intent intent = intents[i];
                    if (resolvedTypes != null && i < resolvedTypes.length) {
                        intent.setDataAndType(intent.getData(), resolvedTypes[i]);
                    }
                    Intent targetIntent = redirectIntentSender(type, creator, intent);
                    if (targetIntent != null) {
                        intents[i] = targetIntent;
                    }
                }
            }
            args[7] = flags;
            args[1] = getHostPkg();
            // Force userId to 0
            if (args[args.length - 1] instanceof Integer) {
                args[args.length - 1] = 0;
            }
            IInterface sender = (IInterface) method.invoke(who, args);
            if (sender != null && creator != null) {
                VActivityManager.get().addPendingIntent(sender.asBinder(), creator);
            }
            return sender;
        }

        private Intent redirectIntentSender(int type, String creator, Intent intent) {
            Intent newIntent = intent.cloneFilter();
            switch (type) {
                case ActivityManagerCompat.INTENT_SENDER_ACTIVITY: {
                    ComponentInfo info = VirtualCore.get().resolveActivityInfo(intent, 0);
                    if (info != null) {
                        newIntent.setClass(getHostContext(), StubPendingActivity.class);
                        newIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    }
                }
                break;
                case ActivityManagerCompat.INTENT_SENDER_SERVICE: {
                    ComponentInfo info = VirtualCore.get().resolveServiceInfo(intent, 0);
                    if (info != null) {
                        newIntent.setClass(getHostContext(), StubPendingService.class);
                    }
                }
                break;
                case ActivityManagerCompat.INTENT_SENDER_BROADCAST: {
                    newIntent.setClass(getHostContext(), StubPendingReceiver.class);
                }
                break;
                default:
                    return null;
            }
            newIntent.putExtra("_VA_|_user_id_", 0/*VUserHandle.myUserId()*/);
            newIntent.putExtra("_VA_|_intent_", intent);
            newIntent.putExtra("_VA_|_creator_", creator);
            newIntent.putExtra("_VA_|_from_inner_", true);
            return newIntent;
        }
    }


    static class StartActivity extends MethodProxy {

        private static final String SCHEME_FILE = "file";
        private static final String SCHEME_PACKAGE = "package";
        private static final String SCHEME_CONTENT = "content";

        @Override
        public String getMethodName() {
            return "startActivity";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {

            Log.d("Q_M", "---->StartActivity 类");

            int intentIndex = ArrayUtils.indexOfObject(args, Intent.class, 1);
            if (intentIndex < 0) {
                return ActivityManagerCompat.START_INTENT_NOT_RESOLVED;
            }
            Intent intent = (Intent) args[intentIndex];
            PluginImpl plugin = PluginCore.get().findPlugin(intent);
            if (plugin == null) {
                return method.invoke(who, args);
            }
            int resultToIndex = ArrayUtils.indexOfObject(args, IBinder.class, 2);
            String resolvedType = (String) args[intentIndex + 1];
            intent.setDataAndType(intent.getData(), resolvedType);
            IBinder resultTo = resultToIndex >= 0 ? (IBinder) args[resultToIndex] : null;

            if (ComponentUtils.isStubComponent(intent)) {
                return method.invoke(who, args);
            }

            if (Intent.ACTION_INSTALL_PACKAGE.equals(intent.getAction())
                    || (Intent.ACTION_VIEW.equals(intent.getAction())
                    && "application/vnd.android.package-archive".equals(intent.getType()))) {
                if (handleInstallRequest(intent)) {
                    return 0;
                }
            } else if ((Intent.ACTION_UNINSTALL_PACKAGE.equals(intent.getAction())
                    || Intent.ACTION_DELETE.equals(intent.getAction()))
                    && "package".equals(intent.getScheme())) {

                if (handleUninstallRequest(intent)) {
                    return 0;
                }
            }

            String resultWho = null;
            int requestCode = 0;
            Bundle options = ArrayUtils.getFirst(args, Bundle.class);
            if (resultTo != null) {
                resultWho = (String) args[resultToIndex + 1];
                requestCode = (int) args[resultToIndex + 2];
            }
            // chooser
            if (ChooserActivity.check(intent)) {
                intent.setComponent(new ComponentName(getHostContext(), ChooserActivity.class));
                intent.putExtra(Constants.EXTRA_USER_HANDLE, plugin.getUserId());
                intent.putExtra(ChooserActivity.EXTRA_DATA, options);
                intent.putExtra(ChooserActivity.EXTRA_WHO, resultWho);
                intent.putExtra(ChooserActivity.EXTRA_REQUEST_CODE, requestCode);
                return method.invoke(who, args);
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                args[intentIndex - 1] = getHostPkg();
            }
            if (intent.getScheme() != null && intent.getScheme().equals(SCHEME_PACKAGE) && intent.getData() != null) {
                if (intent.getAction() != null && intent.getAction().startsWith("android.settings.")) {
                    intent.setData(Uri.parse("package:" + getHostPkg()));
                }
            }

            ActivityInfo activityInfo = VirtualCore.get().resolveActivityInfo(intent, plugin.getUserId());
            if (activityInfo == null) {
                VLog.e("VActivityManager", "Unable to resolve activityInfo : " + intent);

                Log.d("Q_M", "---->StartActivity who=" + who);
                Log.d("Q_M", "---->StartActivity intent=" + intent);
                Log.d("Q_M", "---->StartActivity resultTo=" + resultTo);

                if (intent.getPackage() != null && isAppPkg(intent.getPackage())) {
                    return ActivityManagerCompat.START_INTENT_NOT_RESOLVED;
                }

                if (INTERCEPT_BACK_HOME && Intent.ACTION_MAIN.equals(intent.getAction())
                        && intent.getCategories().contains("android.intent.category.HOME")
                        && resultTo != null) {
                    VActivityManager.get().finishActivity(resultTo);
                    return 0;
                }

                return method.invoke(who, args);
            }
            int res = VActivityManager.get().startActivity(intent, activityInfo, resultTo, options, resultWho, requestCode, plugin.getUserId());
            if (res != 0 && resultTo != null && requestCode > 0) {
                VActivityManager.get().sendActivityResult(resultTo, resultWho, requestCode);
            }
            if (resultTo != null) {
                ActivityClientRecord r = VActivityManager.get().getActivityRecord(resultTo);
                if (r != null && r.activity != null) {
                    try {
                        TypedValue out = new TypedValue();
                        Resources.Theme theme = r.activity.getResources().newTheme();
                        theme.applyStyle(activityInfo.getThemeResource(), true);
                        if (theme.resolveAttribute(android.R.attr.windowAnimationStyle, out, true)) {

                            TypedArray array = theme.obtainStyledAttributes(out.data,
                                    new int[]{
                                            android.R.attr.activityOpenEnterAnimation,
                                            android.R.attr.activityOpenExitAnimation
                                    });

                            r.activity.overridePendingTransition(array.getResourceId(0, 0), array.getResourceId(1, 0));
                            array.recycle();
                        }
                    } catch (Throwable e) {
                        // Ignore
                    }
                }
            }
            return res;
        }


        private boolean handleInstallRequest(Intent intent) {
            IAppRequestListener listener = VirtualCore.get().getAppRequestListener();
            if (listener != null) {
                Uri packageUri = intent.getData();
                if (SCHEME_FILE.equals(packageUri.getScheme())) {
                    File sourceFile = new File(packageUri.getPath());
                    try {
                        listener.onRequestInstall(sourceFile.getPath());
                        return true;
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                } else if (SCHEME_CONTENT.equals(packageUri.getScheme())) {
                    InputStream inputStream = null;
                    OutputStream outputStream = null;
                    File sharedFileCopy = new File(getHostContext().getCacheDir(), packageUri.getLastPathSegment());
                    try {
                        inputStream = getHostContext().getContentResolver().openInputStream(packageUri);
                        outputStream = new FileOutputStream(sharedFileCopy);
                        byte[] buffer = new byte[1024];
                        int count;
                        while ((count = inputStream.read(buffer)) > 0) {
                            outputStream.write(buffer, 0, count);
                        }
                        outputStream.flush();

                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        FileUtils.closeQuietly(inputStream);
                        FileUtils.closeQuietly(outputStream);
                    }
                    try {
                        listener.onRequestInstall(sharedFileCopy.getPath());
                        return true;
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }

            }
            return false;
        }

        private boolean handleUninstallRequest(Intent intent) {
            IAppRequestListener listener = VirtualCore.get().getAppRequestListener();
            if (listener != null) {
                Uri packageUri = intent.getData();
                if (SCHEME_PACKAGE.equals(packageUri.getScheme())) {
                    String pkg = packageUri.getSchemeSpecificPart();
                    try {
                        listener.onRequestUninstall(pkg);
                        return true;
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }

            }
            return false;
        }

    }

    static class StartActivities extends MethodProxy {

        @Override
        public String getMethodName() {
            return "startActivities";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            Intent[] intents = ArrayUtils.getFirst(args, Intent[].class);
            Intent intent = intents[0];
            PluginImpl plugin = PluginCore.get().findPlugin(intent);
            if (plugin == null) {
                return method.invoke(who, args);
            }
            String[] resolvedTypes = ArrayUtils.getFirst(args, String[].class);
            IBinder token = null;
            int tokenIndex = ArrayUtils.indexOfObject(args, IBinder.class, 2);
            if (tokenIndex != -1) {
                token = (IBinder) args[tokenIndex];
            }
            Bundle options = ArrayUtils.getFirst(args, Bundle.class);
            return VActivityManager.get().startActivities(intents, resolvedTypes, token, options, plugin.getUserId());
        }
    }


    static class FinishActivity extends MethodProxy {
        @Override
        public String getMethodName() {
            return "finishActivity";
        }

        @Override
        public Object afterCall(Object who, Method method, Object[] args, Object result) throws Throwable {
            IBinder token = (IBinder) args[0];
            ActivityClientRecord r = VActivityManager.get().getActivityRecord(token);
            boolean taskRemoved = VActivityManager.get().onActivityDestroy(token);
            if (!taskRemoved && r != null && r.activity != null && r.info.getThemeResource() != 0) {
                try {
                    TypedValue out = new TypedValue();
                    Resources.Theme theme = r.activity.getResources().newTheme();
                    theme.applyStyle(r.info.getThemeResource(), true);
                    if (theme.resolveAttribute(android.R.attr.windowAnimationStyle, out, true)) {

                        TypedArray array = theme.obtainStyledAttributes(out.data,
                                new int[]{
                                        android.R.attr.activityCloseEnterAnimation,
                                        android.R.attr.activityCloseExitAnimation
                                });
                        r.activity.overridePendingTransition(array.getResourceId(0, 0), array.getResourceId(1, 0));
                        array.recycle();
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
            return super.afterCall(who, method, args, result);
        }
    }


    static class GetCallingPackage extends MethodProxy {

        @Override
        public String getMethodName() {
            return "getCallingPackage";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            IBinder token = (IBinder) args[0];
            String callingPackage = VActivityManager.get().getCallingPackage(token);
            if (callingPackage.equals("android")) {
                return method.invoke(who, args);
            }
            return callingPackage;
        }
    }


    static class GetPackageForIntentSender extends MethodProxy {
        @Override
        public String getMethodName() {
            return "getPackageForIntentSender";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            IInterface sender = (IInterface) args[0];
            if (sender != null) {
                String packageName = VActivityManager.get().getPackageForIntentSender(sender.asBinder());
                if (packageName != null) {
                    return packageName;
                }
            }
            return super.call(who, method, args);
        }
    }


    @SuppressWarnings("unchecked")
    static class PublishContentProviders extends MethodProxy {

        @Override
        public String getMethodName() {
            return "publishContentProviders";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            return method.invoke(who, args);
        }
    }


    static class GetServices extends MethodProxy {
        @Override
        public String getMethodName() {
            return "getServices";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            int maxNum = (int) args[0];
            int flags = (int) args[1];
            int pluginId = PluginCore.get().getLatestCallRunningProcessPlugin();
            if (pluginId > 0) {
                PluginImpl plugin = PluginCore.get().getPlugin(pluginId);
                return VActivityManager.get().getServices(maxNum, flags, plugin.getUserId()).getList();
            } else {
                return method.invoke(who, args);
            }
        }

        @Override
        public boolean isEnable() {
            return isAppProcess();
        }
    }

    static class GrantUriPermissionFromOwner extends MethodProxy {

        @Override
        public String getMethodName() {
            return "grantUriPermissionFromOwner";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            MethodParameterUtils.replaceFirstAppPkg(args);
            return method.invoke(who, args);
        }
    }

    static class SetServiceForeground extends MethodProxy {

        @Override
        public String getMethodName() {
            return "setServiceForeground";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            ComponentName component = (ComponentName) args[0];
            IBinder token = (IBinder) args[1];
            ServiceRecord r = (ServiceRecord) token;
            int pluginId = PluginMetaBundle.getPluginIdFromMeta(r.serviceInfo);
            if (!PluginHandle.isPluginVPid(pluginId)) {
                return method.invoke(who, args);
            }
            int id = (int) args[2];
            Notification notification = (Notification) args[3];
            boolean removeNotification = false;
            if (args[4] instanceof Boolean) {
                removeNotification = (boolean) args[4];
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && args[4] instanceof Integer) {
                int flags = (int) args[4];
                removeNotification = (flags & Service.STOP_FOREGROUND_REMOVE) != 0;
            } else {
                VLog.e(getClass().getSimpleName(), "Unknown flag : " + args[4]);
            }


            VNotificationManager.get().dealNotification(id, notification, VirtualCore.get().getHostPkg());

            /**
             * `BaseStatusBar#updateNotification` aosp will use use
             * `new StatusBarIcon(...notification.getSmallIcon()...)`
             *  while in samsung SystemUI.apk ,the corresponding code comes as
             * `new StatusBarIcon(...pkgName,notification.icon...)`
             * the icon comes from `getSmallIcon.getResource`
             * which will throw an exception on :x process thus crash the application
             */
            if (notification != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                    (Build.BRAND.equalsIgnoreCase("samsung") || Build.MANUFACTURER.equalsIgnoreCase("samsung"))) {
                notification.icon = getHostContext().getApplicationInfo().icon;
                Icon icon = Icon.createWithResource(getHostPkg(), notification.icon);
                Reflect.on(notification).call("setSmallIcon", icon);
            }

            PluginImpl plugin = PluginCore.get().getPlugin(pluginId);
            VActivityManager.get().setServiceForeground(component, token, id, notification, removeNotification, plugin.getUserId());
            return 0;
        }
    }


    static class UpdateDeviceOwner extends MethodProxy {

        @Override
        public String getMethodName() {
            return "updateDeviceOwner";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            MethodParameterUtils.replaceFirstAppPkg(args);
            return method.invoke(who, args);
        }
    }


    static class GetIntentForIntentSender extends MethodProxy {

        @Override
        public String getMethodName() {
            return "getIntentForIntentSender";
        }

        @Override
        public Object afterCall(Object who, Method method, Object[] args, Object result) throws Throwable {
            Intent intent = (Intent) super.afterCall(who, method, args, result);
            if (intent != null && intent.hasExtra("_VA_|_intent_")) {
                return intent.getParcelableExtra("_VA_|_intent_");
            }
            return intent;
        }
    }


    static class UnbindFinished extends MethodProxy {

        @Override
        public String getMethodName() {
            return "unbindFinished";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            IBinder token = (IBinder) args[0];
            if (!VActivityManager.get().isVAServiceToken(token)) {
                return method.invoke(who, args);
            }
            Intent service = (Intent) args[1];
            boolean doRebind = (boolean) args[2];
            VActivityManager.get().unbindFinished(token, service, doRebind);
            return 0;
        }
    }

    static class StartActivityIntentSender extends MethodProxy {
        @Override
        public String getMethodName() {
            return "startActivityIntentSender";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            return super.call(who, method, args);
        }
    }


    static class BindService extends MethodProxy {

        @Override
        public String getMethodName() {
            return "bindService";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            IInterface caller = (IInterface) args[0];
            IBinder token = (IBinder) args[1];
            Intent service = (Intent) args[2];
            String resolvedType = (String) args[3];
            IServiceConnection conn = (IServiceConnection) args[4];
            int flags = (int) args[5];
            PluginImpl plugin = PluginCore.get().findPlugin(service);
            if (plugin == null) {
                return method.invoke(who, args);
            }
            ServiceInfo serviceInfo = VirtualCore.get().resolveServiceInfo(service, plugin.getUserId());
            if (serviceInfo != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    service.setComponent(new ComponentName(serviceInfo.packageName, serviceInfo.name));
                }
                conn = ServiceConnectionDelegate.getDelegate(conn);
                return VActivityManager.get().bindService(caller.asBinder(), token, service, resolvedType,
                        conn, flags, plugin.getUserId());
            }
            return method.invoke(who, args);
        }
    }


    static class StartService extends MethodProxy {

        @Override
        public String getMethodName() {
            return "startService";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            IInterface appThread = (IInterface) args[0];
            Intent service = (Intent) args[1];
            PluginImpl plugin = PluginCore.get().findPlugin(service);
            if (plugin == null) {
                return method.invoke(who, args);
            }
            String resolvedType = (String) args[2];
            if (service.getComponent() != null
                    && getHostPkg().equals(service.getComponent().getPackageName())) {
                // for server process
                return method.invoke(who, args);
            }
            int userId = plugin.getUserId();
            if (service.getBooleanExtra("_VA_|_from_inner_", false)) {
                userId = service.getIntExtra("_VA_|_user_id_", userId);
                service = service.getParcelableExtra("_VA_|_intent_");
            }
            service.setDataAndType(service.getData(), resolvedType);
            ServiceInfo serviceInfo = VirtualCore.get().resolveServiceInfo(service, userId);
            if (serviceInfo != null) {
                return VActivityManager.get().startService(appThread, service, resolvedType, userId);
            }
            return method.invoke(who, args);
        }
    }

    static class StartActivityAndWait extends StartActivity {
        @Override
        public String getMethodName() {
            return "startActivityAndWait";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            return super.call(who, method, args);
        }
    }


    static class PublishService extends MethodProxy {

        @Override
        public String getMethodName() {
            return "publishService";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            IBinder token = (IBinder) args[0];
            if (!VActivityManager.get().isVAServiceToken(token)) {
                return method.invoke(who, args);
            }
            Intent intent = (Intent) args[1];
            IBinder service = (IBinder) args[2];
            VActivityManager.get().publishService(token, intent, service);
            return 0;
        }
    }


    @SuppressWarnings("unchecked")
    static class GetRunningAppProcesses extends MethodProxy {
        private static final String TAG = "GetRunningAppProcesses";

        @Override
        public String getMethodName() {
            return "getRunningAppProcesses";
        }

        @Override
        public synchronized Object call(Object who, Method method, Object... args) throws Throwable {
            List<ActivityManager.RunningAppProcessInfo> infoList = (List<ActivityManager.RunningAppProcessInfo>) method
                    .invoke(who, args);
            if (infoList != null) {
                if (isMainProcess()) {
                    ArrayList<ActivityManager.RunningAppProcessInfo> results = new ArrayList<>();
                    SparseArray<PluginImpl> plugins = PluginCore.get().getPlugins();
                    for (int i = 0; i < plugins.size(); i++) {
                        PluginImpl plugin = plugins.valueAt(i);
                        ApplicationInfo applicationInfo = plugin.getApplicationInfo();
                        ActivityManager.RunningAppProcessInfo processInfo = new ActivityManager.RunningAppProcessInfo();
                        processInfo.pid = Process.myPid();
                        processInfo.processName = applicationInfo.processName == null ? applicationInfo.packageName : applicationInfo.packageName;
                        results.add(processInfo);
                    }
                    results.addAll(infoList);
                    return results;
                } else {
                    for (ActivityManager.RunningAppProcessInfo info : infoList) {
                        if (VActivityManager.get().isAppPid(info.pid)) {
                            List<String> pkgList = VActivityManager.get().getProcessPkgList(info.pid);
                            String processName = VActivityManager.get().getAppProcessName(info.pid);
                            if (processName != null) {
                                info.processName = processName;
                            }
                            info.pkgList = pkgList.toArray(new String[pkgList.size()]);
                            info.uid = VUserHandle.getAppId(VActivityManager.get().getUidByPid(info.pid));
                        }
                    }
                    return infoList;
                }
            }
            return method.invoke(who, args);
        }
    }


    static class SetPackageAskScreenCompat extends MethodProxy {

        @Override
        public String getMethodName() {
            return "setPackageAskScreenCompat";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                if (args.length > 0 && args[0] instanceof String) {
                    args[0] = getHostPkg();
                }
            }
            return method.invoke(who, args);
        }
    }


    static class GetCallingActivity extends MethodProxy {

        @Override
        public String getMethodName() {
            return "getCallingActivity";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            IBinder token = (IBinder) args[0];
            ComponentName callingActivity = VActivityManager.get().getCallingActivity(token);
            if (callingActivity != null)
                return callingActivity;
            return method.invoke(who, args);
        }
    }


    static class GetCurrentUser extends MethodProxy {

        @Override
        public String getMethodName() {
            return "getCurrentUser";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            try {
                return UserInfo.ctor.newInstance(0, "user", VUserInfo.FLAG_PRIMARY);
            } catch (Throwable e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    static class StartActivityAsUser extends StartActivity {

        @Override
        public String getMethodName() {
            return "startActivityAsUser";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            return super.call(who, method, args);
        }
    }


    static class CheckPermission extends MethodProxy {

        @Override
        public String getMethodName() {
            return "checkPermission";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            String permission = (String) args[0];
            if (SpecialComponentList.isWhitePermission(permission)) {
                return PackageManager.PERMISSION_GRANTED;
            }
            if (permission.startsWith("com.google")) {
                return PackageManager.PERMISSION_GRANTED;
            }
            args[args.length - 1] = getRealUid();
            return method.invoke(who, args);
        }

        @Override
        public boolean isEnable() {
            return isAppProcess();
        }

    }


    static class StartActivityAsCaller extends StartActivity {

        @Override
        public String getMethodName() {
            return "startActivityAsCaller";
        }
    }


    static class HandleIncomingUser extends MethodProxy {

        @Override
        public String getMethodName() {
            return "handleIncomingUser";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            int lastIndex = args.length - 1;
            if (args[lastIndex] instanceof String) {
                args[lastIndex] = getHostPkg();
            }
            return method.invoke(who, args);
        }

        @Override
        public boolean isEnable() {
            return isAppProcess();
        }

    }


    @SuppressWarnings("unchecked")
    static class GetTasks extends MethodProxy {

        @Override
        public String getMethodName() {
            return "getTasks";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            List<ActivityManager.RunningTaskInfo> runningTaskInfos = (List<ActivityManager.RunningTaskInfo>) method
                    .invoke(who, args);
            for (ActivityManager.RunningTaskInfo info : runningTaskInfos) {
                AppTaskInfo taskInfo = VActivityManager.get().getTaskInfo(info.id);
                if (taskInfo != null) {
                    info.topActivity = taskInfo.topActivity;
                    info.baseActivity = taskInfo.baseActivity;
                }
            }
            return runningTaskInfos;
        }
    }


    static class GetPersistedUriPermissions extends MethodProxy {

        @Override
        public String getMethodName() {
            return "getPersistedUriPermissions";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            MethodParameterUtils.replaceFirstAppPkg(args);
            return method.invoke(who, args);
        }
    }


    static class RegisterReceiver extends MethodProxy {
        private static final int IDX_IIntentReceiver = Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1
                ? 2
                : 1;

        private static final int IDX_RequiredPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1
                ? 4
                : 3;
        private static final int IDX_IntentFilter = Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1
                ? 3
                : 2;

        private WeakHashMap<IBinder, IIntentReceiver> mProxyIIntentReceivers = new WeakHashMap<>();

        @Override
        public String getMethodName() {
            return "registerReceiver";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            MethodParameterUtils.replaceFirstAppPkg(args);
            args[IDX_RequiredPermission] = null;
            IntentFilter filter = (IntentFilter) args[IDX_IntentFilter];
            SpecialComponentList.protectIntentFilter(filter);
            if (args.length > IDX_IIntentReceiver && IIntentReceiver.class.isInstance(args[IDX_IIntentReceiver])) {
                final IInterface old = (IInterface) args[IDX_IIntentReceiver];
                if (!IIntentReceiverProxy.class.isInstance(old)) {
                    final IBinder token = old.asBinder();
                    if (token != null) {
                        token.linkToDeath(new IBinder.DeathRecipient() {
                            @Override
                            public void binderDied() {
                                token.unlinkToDeath(this, 0);
                                mProxyIIntentReceivers.remove(token);
                            }
                        }, 0);
                        IIntentReceiver proxyIIntentReceiver = mProxyIIntentReceivers.get(token);
                        if (proxyIIntentReceiver == null) {
                            proxyIIntentReceiver = new IIntentReceiverProxy(old);
                            mProxyIIntentReceivers.put(token, proxyIIntentReceiver);
                        }
                        WeakReference mDispatcher = LoadedApk.ReceiverDispatcher.InnerReceiver.mDispatcher.get(old);
                        if (mDispatcher != null) {
                            LoadedApk.ReceiverDispatcher.mIIntentReceiver.set(mDispatcher.get(), proxyIIntentReceiver);
                            args[IDX_IIntentReceiver] = proxyIIntentReceiver;
                        }
                    }
                }
            }
            return method.invoke(who, args);
        }

        private static class IIntentReceiverProxy extends IIntentReceiver.Stub {

            IInterface mOld;

            IIntentReceiverProxy(IInterface old) {
                this.mOld = old;
            }

            public void performReceive(Intent intent, int resultCode, String data, Bundle extras, boolean ordered,
                                       boolean sticky, int sendingUser) throws RemoteException {
                if (!accept(intent)) {
                    return;
                }
                if (intent.hasExtra("_VA_|_intent_")) {
                    intent = intent.getParcelableExtra("_VA_|_intent_");
                }
                SpecialComponentList.unprotectIntent(intent);
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN) {
                    IIntentReceiverJB.performReceive.call(mOld, intent, resultCode, data, extras, ordered, sticky, sendingUser);
                } else {
                    mirror.android.content.IIntentReceiver.performReceive.call(mOld, intent, resultCode, data, extras, ordered, sticky);
                }
            }

            private boolean accept(Intent intent) {
                return intent.hasExtra("_VA_|_user_id_");
                /*int uid = intent.getIntExtra("_VA_|_uid_", -1);
                if (uid != -1) {
                    return VClientImpl.get().getVUid() == uid;
                }
                int userId = intent.getIntExtra("_VA_|_user_id_", -1);
                return userId == -1 || userId == VUserHandle.myPluginUserId();*/
            }

            @SuppressWarnings("unused")
            public void performReceive(Intent intent, int resultCode, String data, Bundle extras, boolean ordered,
                                       boolean sticky) throws RemoteException {
                this.performReceive(intent, resultCode, data, extras, ordered, sticky, 0);
            }

        }
    }


    static class StopService extends MethodProxy {

        @Override
        public String getMethodName() {
            return "stopService";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            IInterface caller = (IInterface) args[0];
            Intent intent = (Intent) args[1];
            PluginImpl plugin = PluginCore.get().findPlugin(intent);
            if (plugin == null) {
                return method.invoke(who, args);
            }
            String resolvedType = (String) args[2];
            intent.setDataAndType(intent.getData(), resolvedType);
            ComponentName componentName = intent.getComponent();
            PackageManager pm = VirtualCore.getPM();
            if (componentName == null) {
                ResolveInfo resolveInfo = pm.resolveService(intent, 0);
                if (resolveInfo != null && resolveInfo.serviceInfo != null) {
                    componentName = new ComponentName(resolveInfo.serviceInfo.packageName, resolveInfo.serviceInfo.name);
                }
            }
            if (componentName != null && !getHostPkg().equals(componentName.getPackageName())) {
                return VActivityManager.get().stopService(caller, intent, resolvedType, plugin.getUserId());
            }
            return method.invoke(who, args);
        }
    }


    static class GetContentProvider extends MethodProxy {
        private static final String TAG = "GetContentProvider";

        @Override
        public String getMethodName() {
            return "getContentProvider";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            int nameIdx = getProviderNameIndex();
            String name = (String) args[nameIdx];
            PluginImpl plugin = PluginCore.get().getPluginByCpAuth(name);
            if (plugin == null) {
                return method.invoke(who, args);
            }
            ProviderInfo info = VPackageManager.get().resolveContentProvider(name, 0, plugin.getUserId());
            if (info != null && info.enabled && isAppPkg(info.packageName)) {
                int targetVPid = VActivityManager.get().initProcess(info.packageName, info.processName, plugin.getUserId());
                String stubAuthority = VASettings.getPluginStubAuthority(targetVPid);
                if (targetVPid == -1) {
                    return null;
                }
                args[nameIdx] = stubAuthority;
                Object holder = method.invoke(who, args);
                if (holder == null) {
                    return null;
                }
                if (BuildCompat.isOreo()) {
                    IInterface provider = ContentProviderHolderOreo.provider.get(holder);
                    if (provider != null) {
                        PluginMetaBundle.putPluginIdToMeta(info, targetVPid);
                        provider = VActivityManager.get().acquireProviderClient(plugin.getUserId(), info);
                    }
                    ContentProviderHolderOreo.provider.set(holder, provider);
                    ContentProviderHolderOreo.info.set(holder, info);
                } else {
                    IInterface provider = IActivityManager.ContentProviderHolder.provider.get(holder);
                    if (provider != null) {
                        PluginMetaBundle.putPluginIdToMeta(info, targetVPid);
                        provider = VActivityManager.get().acquireProviderClient(plugin.getUserId(), info);
                    }
                    IActivityManager.ContentProviderHolder.provider.set(holder, provider);
                    IActivityManager.ContentProviderHolder.info.set(holder, info);
                }
                return holder;
            }
            Object holder = method.invoke(who, args);
            if (holder != null) {
                if (BuildCompat.isOreo()) {
                    IInterface provider = ContentProviderHolderOreo.provider.get(holder);
                    info = ContentProviderHolderOreo.info.get(holder);
                    if (provider != null) {
                        provider = ProviderHook.createProxy(true, info.authority, provider);
                    }
                    ContentProviderHolderOreo.provider.set(holder, provider);
                } else {
                    IInterface provider = IActivityManager.ContentProviderHolder.provider.get(holder);
                    info = IActivityManager.ContentProviderHolder.info.get(holder);
                    if (provider != null) {
                        provider = ProviderHook.createProxy(true, info.authority, provider);
                    }
                    IActivityManager.ContentProviderHolder.provider.set(holder, provider);
                }
                return holder;
            }
            return null;
        }


        public int getProviderNameIndex() {
            return 1;
        }
    }

//    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
//    static class SetTaskDescription extends MethodProxy {
//        @Override
//        public String getMethodName() {
//            return "setTaskDescription";
//        }
//
//        @Override
//        public Object call(Object who, Method method, Object... args) throws Throwable {
//            ActivityManager.TaskDescription td = (ActivityManager.TaskDescription) args[1];
//            String label = td.getLabel();
//            Bitmap icon = td.getIcon();
//
//            // If the activity label/icon isn't specified, the application's label/icon is shown instead
//            // Android usually does that for us, but in this case we want info about the contained app, not VIrtualApp itself
//            if (label == null || icon == null) {
//                Application app = VClientImpl.get().getCurrentApplication();
//                if (app != null) {
//                    try {
//                        if (label == null) {
//                            label = app.getApplicationInfo().loadLabel(app.getPackageManager()).toString();
//                        }
//                        if (icon == null) {
//                            Drawable drawable = app.getApplicationInfo().loadIcon(app.getPackageManager());
//                            if (drawable != null) {
//                                icon = DrawableUtils.drawableToBitMap(drawable);
//                            }
//                        }
//                        td = new ActivityManager.TaskDescription(label, icon, td.getPrimaryColor());
//                    } catch (Throwable e) {
//                        e.printStackTrace();
//                    }
//                }
//            }
//
//            TaskDescriptionDelegate descriptionDelegate = VirtualCore.get().getTaskDescriptionDelegate();
//            if (descriptionDelegate != null) {
//                td = descriptionDelegate.getTaskDescription(td);
//            }
//
//            args[1] = td;
//            return method.invoke(who, args);
//        }
//
//        @Override
//        public boolean isEnable() {
//            return isAppProcess();
//        }
//    }

    static class StopServiceToken extends MethodProxy {

        @Override
        public String getMethodName() {
            return "stopServiceToken";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            ComponentName componentName = (ComponentName) args[0];
            IBinder token = (IBinder) args[1];
            if (!VActivityManager.get().isVAServiceToken(token)) {
                return method.invoke(who, args);
            }
            int startId = (int) args[2];
            if (componentName != null) {
                return VActivityManager.get().stopServiceToken(componentName, token, startId);
            }
            return method.invoke(who, args);
        }
    }

    static class StartActivityWithConfig extends StartActivity {
        @Override
        public String getMethodName() {
            return "startActivityWithConfig";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            return super.call(who, method, args);
        }
    }

    static class StartNextMatchingActivity extends StartActivity {
        @Override
        public String getMethodName() {
            return "startNextMatchingActivity";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            return false;
        }
    }


    static class BroadcastIntent extends MethodProxy {

        @Override
        public String getMethodName() {
            return "broadcastIntent";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            Intent intent = (Intent) args[1];
            PluginImpl plugin = PluginCore.get().findPlugin(intent);
            if (plugin == null) {
                return method.invoke(who, args);
            }
            String type = (String) args[2];
            intent.setDataAndType(intent.getData(), type);
            if (VirtualCore.get().getComponentDelegate() != null) {
                VirtualCore.get().getComponentDelegate().onSendBroadcast(intent);
            }
            Intent newIntent = handleIntent(intent, plugin.getUserId());
            if (newIntent != null) {
                args[1] = newIntent;
            } else {
                return 0;
            }

            if (args[7] instanceof String || args[7] instanceof String[]) {
                // clear the permission
                args[7] = null;
            }
            return method.invoke(who, args);
        }


        private Intent handleIntent(final Intent intent, int userId) {
            final String action = intent.getAction();
            if ("android.intent.action.CREATE_SHORTCUT".equals(action)
                    || "com.android.launcher.action.INSTALL_SHORTCUT".equals(action)) {

                return VASettings.ENABLE_INNER_SHORTCUT ? handleInstallShortcutIntent(intent, userId) : null;

            } else if ("com.android.launcher.action.UNINSTALL_SHORTCUT".equals(action)) {

                handleUninstallShortcutIntent(intent);

            } else if (BadgerManager.handleBadger(intent)) {
                return null;
            } else {
                return ComponentUtils.redirectBroadcastIntent(intent, userId);
            }
            return intent;
        }

        private Intent handleInstallShortcutIntent(Intent intent, int userId) {
            Intent shortcut = intent.getParcelableExtra(Intent.EXTRA_SHORTCUT_INTENT);
            if (shortcut != null) {
                ComponentName component = shortcut.resolveActivity(VirtualCore.getPM());
                if (component != null) {
                    String pkg = component.getPackageName();
                    Intent newShortcutIntent = new Intent();
                    newShortcutIntent.setClassName(getHostPkg(), Constants.SHORTCUT_PROXY_ACTIVITY_NAME);
                    newShortcutIntent.addCategory(Intent.CATEGORY_DEFAULT);
                    newShortcutIntent.putExtra("_VA_|_intent_", shortcut);
                    newShortcutIntent.putExtra("_VA_|_uri_", shortcut.toUri(0));
                    newShortcutIntent.putExtra("_VA_|_user_id_", userId);
                    intent.removeExtra(Intent.EXTRA_SHORTCUT_INTENT);
                    intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, newShortcutIntent);

                    Intent.ShortcutIconResource icon = intent.getParcelableExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE);
                    if (icon != null && !TextUtils.equals(icon.packageName, getHostPkg())) {
                        try {
                            Resources resources = VirtualCore.get().getResources(pkg);
                            int resId = resources.getIdentifier(icon.resourceName, "drawable", pkg);
                            if (resId > 0) {
                                //noinspection deprecation
                                Drawable iconDrawable = resources.getDrawable(resId);
                                Bitmap newIcon = BitmapUtils.drawableToBitmap(iconDrawable);
                                if (newIcon != null) {
                                    intent.removeExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE);
                                    intent.putExtra(Intent.EXTRA_SHORTCUT_ICON, newIcon);
                                }
                            }
                        } catch (Throwable e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            return intent;
        }

        private void handleUninstallShortcutIntent(Intent intent) {
            Intent shortcut = intent.getParcelableExtra(Intent.EXTRA_SHORTCUT_INTENT);
            if (shortcut != null) {
                ComponentName componentName = shortcut.resolveActivity(getPM());
                if (componentName != null) {
                    Intent newShortcutIntent = new Intent();
                    newShortcutIntent.putExtra("_VA_|_uri_", shortcut.toUri(0));
                    newShortcutIntent.setClassName(getHostPkg(), Constants.SHORTCUT_PROXY_ACTIVITY_NAME);
                    newShortcutIntent.removeExtra(Intent.EXTRA_SHORTCUT_INTENT);
                    intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, newShortcutIntent);
                }
            }
        }
    }


    static class GetActivityClassForToken extends MethodProxy {

        @Override
        public String getMethodName() {
            return "getActivityClassForToken";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            IBinder token = (IBinder) args[0];
            ComponentName name = VActivityManager.get().getActivityForToken(token);
            if (name == null) {
                name = (ComponentName) method.invoke(who, args);
            }
            return name;
        }
    }


    static class CheckGrantUriPermission extends MethodProxy {

        @Override
        public String getMethodName() {
            return "checkGrantUriPermission";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            MethodParameterUtils.replaceFirstAppPkg(args);
            return method.invoke(who, args);
        }
    }


    static class ServiceDoneExecuting extends MethodProxy {

        @Override
        public String getMethodName() {
            return "serviceDoneExecuting";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            IBinder token = (IBinder) args[0];
            if (!VActivityManager.get().isVAServiceToken(token)) {
                return method.invoke(who, args);
            }
            int type = (int) args[1];
            int startId = (int) args[2];
            int res = (int) args[3];
            VActivityManager.get().serviceDoneExecuting(token, type, startId, res);
            return 0;
        }
    }
}
