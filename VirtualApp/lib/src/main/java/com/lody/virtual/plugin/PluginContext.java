/*
 * Copyright (C) 2005-2017 Qihoo 360 Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed To in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.lody.virtual.plugin;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.util.AttributeSet;
import android.view.ContextThemeWrapper;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewStub;

import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.hook.utils.FilePermissionUtils;
import com.lody.virtual.helper.utils.Reflect;
import com.lody.virtual.os.VEnvironment;
import com.lody.virtual.os.VUserHandle;
import com.lody.virtual.plugin.core.PluginCore;
import com.lody.virtual.plugin.fixer.PluginMetaBundle;
import com.lody.virtual.plugin.utils.PluginHandle;
import com.lody.virtual.remote.InstalledAppInfo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.lang.reflect.Constructor;

import mirror.android.app.ContextImpl;

/**
 * @author RePlugin Team
 */
public class PluginContext extends ContextThemeWrapper {
    private static final String TAG = "PluginContext";

    private final ClassLoader mNewClassLoader;

    private final Resources mNewResources;

    private final int mPluginId;

    private final Object mSync = new Object();
    private final InstalledAppInfo mInstalledAppInfo;
    private final ApplicationInfo mApplicationInfo;
    private final int mUserId;

    private File mFilesDir;

    private File mCacheDir;

    private File mDatabasesDir;

    private LayoutInflater mInflater;

    LayoutInflater.Factory mFactory = new LayoutInflater.Factory() {

        @Override
        public View onCreateView(String name, Context context, AttributeSet attrs) {
            return handleCreateView(name, context, attrs);
        }
    };
    private Application mApplication;
    private ContentResolver mNewContentResolver;
    private Context mBaseContext;

    public PluginContext(Context base, int themeResId, ClassLoader cl, Resources r,
                         int pluginId, int userId, ApplicationInfo applicationInfo) {
        super(base, themeResId);

        mNewClassLoader = cl;
        mNewResources = r;
        mPluginId = pluginId;
        mUserId = VUserHandle.getUserId(userId);
        mApplicationInfo = applicationInfo;
        mInstalledAppInfo = VirtualCore.get().getInstalledAppInfo(applicationInfo.packageName, 0);
    }

    public void setApplication(Application application) {
        mApplication = application;
//        installContentResolver();
    }

    private void installContentResolver() {
        UserHandle userHandle = mirror.android.os.UserHandle.ctor.newInstance(PluginHandle.getHandleForPlugin(mPluginId));
        Class<?> clz = ContextImpl.ApplicationContentResolver.TYPE;
        mNewContentResolver = Reflect.on(clz).create(this, VirtualCore.mainThread(), userHandle).get();
    }

//    @Override
//    public ClassLoader getClassLoader() {
//        if (mNewClassLoader != null) {
//            return mNewClassLoader;
//        }
//        return super.getClassLoader();
//    }
//
//    @Override
//    public Resources getResources() {
//        if (mNewResources != null) {
//            return mNewResources;
//        }
//        return super.getResources();
//    }
//
//    @Override
//    public AssetManager getAssets() {
//        if (mNewResources != null) {
//            return mNewResources.getAssets();
//        }
//        return super.getAssets();
//    }

    @Override
    public Object getSystemService(String name) {
        if (LAYOUT_INFLATER_SERVICE.equals(name)) {
            if (mInflater == null) {
                LayoutInflater inflater = (LayoutInflater) super.getSystemService(name);
                // 新建一个，设置其工厂
                mInflater = inflater.cloneInContext(this);
                mInflater.setFactory(mFactory);
                // 再新建一个，后续可再次设置工厂
                mInflater = mInflater.cloneInContext(this);
            }
            return mInflater;
        } else if (ACTIVITY_SERVICE.equals(name)) {
            PluginCore.get().setLatestCallRunningProcessPlugin(mPluginId);
        }
        return super.getSystemService(name);
    }

    @Override
    public SharedPreferences getSharedPreferences(String name, int mode) {
        name = "plugin_" + name;
        return super.getSharedPreferences(name, mode);
    }

    @Override
    public FileInputStream openFileInput(String name) throws FileNotFoundException {
        File f = makeFilename(getFilesDir(), name);
        return new FileInputStream(f);
    }

    @Override
    public FileOutputStream openFileOutput(String name, int mode) throws FileNotFoundException {
        final boolean append = (mode & MODE_APPEND) != 0;
        File f = makeFilename(getFilesDir(), name);
        try {
            FileOutputStream fos = new FileOutputStream(f, append);
            setFilePermissionsFromMode(f.getPath(), mode, 0);
            return fos;
        } catch (FileNotFoundException e) {
            //
        }

        File parent = f.getParentFile();
        parent.mkdir();
        FilePermissionUtils.setPermissions(parent.getPath(), FilePermissionUtils.S_IRWXU | FilePermissionUtils.S_IRWXG, -1, -1);
        FileOutputStream fos = new FileOutputStream(f, append);
        setFilePermissionsFromMode(f.getPath(), mode, 0);
        return fos;
    }

    @Override
    public boolean deleteFile(String name) {
        File f = makeFilename(getFilesDir(), name);
        return f.delete();
    }

    @Override
    public File getFilesDir() {
        synchronized (mSync) {
            if (mFilesDir == null) {
                mFilesDir = new File(getDataDirFile(), "files");
            }
            if (!mFilesDir.exists()) {
                if (!mFilesDir.mkdirs()) {
                    if (mFilesDir.exists()) {
                        // spurious failure; probably racing with another process for this app
                        return mFilesDir;
                    }
                    return null;
                }
                FilePermissionUtils.setPermissions(mFilesDir.getPath(),
                        FilePermissionUtils.S_IRWXU | FilePermissionUtils.S_IRWXG
                                | FilePermissionUtils.S_IXOTH, -1, -1);
            }
            return mFilesDir;
        }
    }

    @Override
    public File getCacheDir() {
        synchronized (mSync) {
            if (mCacheDir == null) {
                mCacheDir = new File(getDataDirFile(), "cache");
            }
            if (!mCacheDir.exists()) {
                if (!mCacheDir.mkdirs()) {
                    if (mCacheDir.exists()) {
                        // spurious failure; probably racing with another process for this app
                        return mCacheDir;
                    }
                    return null;
                }
                FilePermissionUtils.setPermissions(mCacheDir.getPath(),
                        FilePermissionUtils.S_IRWXU | FilePermissionUtils.S_IRWXG
                                | FilePermissionUtils.S_IXOTH, -1, -1);
            }
        }
        return mCacheDir;
    }

/*
    为了适配 Android 8.1 及后续版本，该方法不再重写，因此，需要各插件之间约定，防止出现重名数据库。
    by cundong
    @Override
    public File getDatabasePath(String name) {
        return validateFilePath(name, false);
    }
*/

    @Override
    public File getFileStreamPath(String name) {
        return makeFilename(getFilesDir(), name);
    }

    @Override
    public File getDir(String name, int mode) {
        name = "plugin_app_" + name;
        File file = makeFilename(getDataDirFile(), name);
        if (!file.exists()) {
            file.mkdir();
            setFilePermissionsFromMode(file.getPath(), mode,
                    FilePermissionUtils.S_IRWXU | FilePermissionUtils.S_IRWXG | FilePermissionUtils.S_IXOTH);
        }
        return file;
    }

    private File getDatabasesDir() {
        synchronized (mSync) {
            if (mDatabasesDir == null) {
                mDatabasesDir = new File(getDataDirFile(), "databases");
            }
            if (mDatabasesDir.getPath().equals("databases")) {
                mDatabasesDir = new File("/data/system");
            }
            return mDatabasesDir;
        }
    }

    private File validateFilePath(String name, boolean createDirectory) {
        File dir;
        File f;

        if (name.charAt(0) == File.separatorChar) {
            String dirPath = name.substring(0, name.lastIndexOf(File.separatorChar));
            dir = new File(dirPath);
            name = name.substring(name.lastIndexOf(File.separatorChar));
            f = new File(dir, name);
        } else {
            dir = getDatabasesDir();
            f = makeFilename(dir, name);
        }

        if (createDirectory && !dir.isDirectory() && dir.mkdir()) {
            FilePermissionUtils.setPermissions(dir.getPath(),
                    FilePermissionUtils.S_IRWXU | FilePermissionUtils.S_IRWXG
                            | FilePermissionUtils.S_IXOTH, -1, -1);
        }

        return f;
    }

    private final File makeFilename(File base, String name) {
        if (name.indexOf(File.separatorChar) < 0) {
            return new File(base, name);
        }
        throw new IllegalArgumentException("File " + name + " contains a path separator");
    }

    /**
     * 设置文件的访问权限
     *
     * @param name             需要被设置访问权限的文件
     * @param mode             文件操作模式
     * @param extraPermissions 文件访问权限
     *                         <p>
     *                         注意： <p>
     *                         此部分经由360安全部门审核后，在所有者|同组用户|其他用户三部分的权限设置中，认为在其他用户的权限设置存在一定的安全风险 <p>
     *                         目前暂且忽略传入的文件操作模式参数，并移除了允许其他用户的读写权限的操作 <p>
     *                         对于文件操作模式以及其他用户访问权限的设置，开发者可自行评估 <p>
     * @return
     */
    private final void setFilePermissionsFromMode(String name, int mode, int extraPermissions) {
        int perms = FilePermissionUtils.S_IRUSR | FilePermissionUtils.S_IWUSR | FilePermissionUtils.S_IRGRP | FilePermissionUtils.S_IWGRP | extraPermissions;
//        if ((mode & MODE_WORLD_READABLE) != 0) {
//            perms |= FilePermissionUtils.S_IROTH;
//        }
//        if ((mode & MODE_WORLD_WRITEABLE) != 0) {
//            perms |= FilePermissionUtils.S_IWOTH;
//        }
        FilePermissionUtils.setPermissions(name, perms, -1, -1);
    }

    /**
     * @return
     */
    private final File getDataDirFile() {
        // 原本用 getDir(Constant.LOCAL_PLUGIN_DATA_SUB_DIR)
        // 由于有些模块的数据写死在files目录下，这里不得已改为getFilesDir + Constant.LOCAL_PLUGIN_DATA_SUB_DIR
        // File dir = getApplicationContext().getDir(Constant.LOCAL_PLUGIN_DATA_SUB_DIR, 0);

        // files
        // huchangqing getApplicationContext()会返回null
        /*File dir0 = getBaseContext().getFilesDir();

        // v3 data
        File dir = new File(dir0, Constants.LOCAL_PLUGIN_DATA_SUB_DIR);
        if (!dir.exists()) {
            if (!dir.mkdir()) {
                return null;
            }
            setFilePermissionsFromMode(dir.getPath(), 0, FilePermissionUtils.S_IRWXU | FilePermissionUtils.S_IRWXG | FilePermissionUtils.S_IXOTH);
        }

        // 插件名
        File file = makeFilename(dir, mPlugin);
        if (!file.exists()) {
            if (!file.mkdir()) {
                return null;
            }
            setFilePermissionsFromMode(file.getPath(), 0, FilePermissionUtils.S_IRWXU | FilePermissionUtils.S_IRWXG | FilePermissionUtils.S_IXOTH);
        }*/

        return VEnvironment.getDataUserPackageDirectory(mUserId, mApplicationInfo.packageName);
    }

    private final View handleCreateView(String name, Context context, AttributeSet attrs) {
        // 忽略表命中，返回
        if (PluginCore.get().isCacheLayout(name)) {
            return null;
        }

        // 构造器缓存
        Constructor<?> construct = PluginCore.get().getCacheConstructor(name);

        // 缓存失败
        if (construct == null) {
            // 找类
            Class<?> c = null;
            boolean found = false;
            do {
                try {
                    c = mNewClassLoader.loadClass(name);
                    if (c == null) {
                        // 没找到，不管
                        break;
                    }
                    if (c == ViewStub.class) {
                        // 系统特殊类，不管
                        break;
                    }
                    if (c.getClassLoader() != mNewClassLoader) {
                        // 不是插件类，不管
                        break;
                    }
                    // 找到
                    found = true;
                } catch (ClassNotFoundException e) {
                    // 失败，不管
                    break;
                }
            } while (false);
            if (!found) {
                PluginCore.get().addCacheLayout(name);
                return null;
            }
            // 找构造器
            try {
                construct = c.getConstructor(Context.class, AttributeSet.class);
                PluginCore.get().addCacheConstructor(name, construct);
            } catch (Exception e) {
                InflateException ie = new InflateException(attrs.getPositionDescription() + ": Error inflating mobilesafe class " + name, e);
                throw ie;
            }
        }

        // 构造
        try {
            View v = (View) construct.newInstance(context, attrs);
            return v;
        } catch (Exception e) {
            InflateException ie = new InflateException(attrs.getPositionDescription() + ": Error inflating mobilesafe class " + name, e);
            throw ie;
        }
    }
//
//    @Override
//    public String getPackageName() {
//        return mApplicationInfo.packageName;
//    }
//
//    @Override
//    public Context getBaseContext() {
////        Context baseContext = super.getBaseContext();
//        return mBaseContext == null ? super.getBaseContext() : mBaseContext;
//    }
//
//    /**
//     * {@link #mApplication} is NULL when Plugin {@link Application#getApplicationContext()} in {@link Application#onCreate()}
//     *
//     * @return
//     */
//    @Override
//    public Context getApplicationContext() {
//        return mApplication == null ? this : mApplication;
//    }
//
//    @Override
//    public String getPackageCodePath() {
//        // 获取插件Apk的路径
//        return mInstalledAppInfo.apkPath;
//    }
//
//    @Override
//    public ApplicationInfo getApplicationInfo() {
//        return mApplicationInfo;
//    }
//
//    @Override
//    public ContentResolver getContentResolver() {
//        if (mNewContentResolver != null) {
//            return mNewContentResolver;
//        }
//        return super.getContentResolver();
//    }

    @Override
    public void startActivity(Intent intent, Bundle options) {
        PluginMetaBundle.putIntentPluginId(intent, mPluginId);
        super.startActivity(intent, options);
    }

    @Override
    public void startActivity(Intent intent) {
        PluginMetaBundle.putIntentPluginId(intent, mPluginId);
        super.startActivity(intent);
    }

    @Override
    public void startActivities(Intent[] intents) {
        for (Intent intent : intents)
            PluginMetaBundle.putIntentPluginId(intent, mPluginId);
        super.startActivities(intents);
    }

    @Override
    public void startActivities(Intent[] intents, Bundle options) {
        for (Intent intent : intents) {
            PluginMetaBundle.putIntentPluginId(intent, mPluginId);
        }
        super.startActivities(intents, options);
    }

    @Override
    public void startIntentSender(IntentSender intent, Intent fillInIntent, int flagsMask, int flagsValues, int extraFlags) throws IntentSender.SendIntentException {
        super.startIntentSender(intent, fillInIntent, flagsMask, flagsValues, extraFlags);
    }

    @Override
    public void startIntentSender(IntentSender intent, Intent fillInIntent, int flagsMask, int flagsValues, int extraFlags, Bundle options) throws IntentSender.SendIntentException {
        super.startIntentSender(intent, fillInIntent, flagsMask, flagsValues, extraFlags, options);
    }

    @Override
    public Intent registerReceiver(BroadcastReceiver receiver, IntentFilter filter) {
        return super.registerReceiver(receiver, filter);
    }

    @Override
    public Intent registerReceiver(BroadcastReceiver receiver, IntentFilter filter, int flags) {
        return super.registerReceiver(receiver, filter, flags);
    }

    @Override
    public Intent registerReceiver(BroadcastReceiver receiver, IntentFilter filter, String broadcastPermission, Handler scheduler) {
        return super.registerReceiver(receiver, filter, broadcastPermission, scheduler);
    }

    @Override
    public Intent registerReceiver(BroadcastReceiver receiver, IntentFilter filter, String broadcastPermission, Handler scheduler, int flags) {
        return super.registerReceiver(receiver, filter, broadcastPermission, scheduler, flags);
    }

    @Override
    public void sendBroadcast(Intent intent) {
        PluginMetaBundle.putIntentPluginId(intent, mPluginId);
        super.sendBroadcast(intent);
    }

    @Override
    public void sendBroadcast(Intent intent, String receiverPermission) {
        PluginMetaBundle.putIntentPluginId(intent, mPluginId);
        super.sendBroadcast(intent, receiverPermission);
    }

    @Override
    public void sendOrderedBroadcast(Intent intent, String receiverPermission) {
        PluginMetaBundle.putIntentPluginId(intent, mPluginId);
        super.sendOrderedBroadcast(intent, receiverPermission);
    }

    @Override
    public void sendOrderedBroadcast(Intent intent, String receiverPermission, BroadcastReceiver resultReceiver, Handler scheduler, int initialCode, String initialData, Bundle initialExtras) {
        PluginMetaBundle.putIntentPluginId(intent, mPluginId);
        super.sendOrderedBroadcast(intent, receiverPermission, resultReceiver, scheduler, initialCode, initialData, initialExtras);
    }


    @Override
    public ComponentName startService(Intent service) {
        PluginMetaBundle.putIntentPluginId(service, mPluginId);
        return super.startService(service);
    }

    @Override
    public ComponentName startForegroundService(Intent service) {
        PluginMetaBundle.putIntentPluginId(service, mPluginId);
        return super.startForegroundService(service);
    }

    @Override
    public boolean stopService(Intent name) {
        PluginMetaBundle.putIntentPluginId(name, mPluginId);
        return super.stopService(name);
    }

    @Override
    public boolean bindService(Intent service, ServiceConnection conn, int flags) {
        PluginMetaBundle.putIntentPluginId(service, mPluginId);
        return super.bindService(service, conn, flags);
    }

    @Override
    public void unbindService(ServiceConnection conn) {
        super.unbindService(conn);
    }
}
