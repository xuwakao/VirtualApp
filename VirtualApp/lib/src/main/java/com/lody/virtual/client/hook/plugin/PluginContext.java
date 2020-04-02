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

package com.lody.virtual.client.hook.plugin;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.ContextThemeWrapper;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewStub;

import com.lody.virtual.client.env.Constants;
import com.lody.virtual.client.hook.utils.FilePermissionUtils;
import com.lody.virtual.os.VEnvironment;
import com.lody.virtual.remote.InstalledAppInfo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.lang.reflect.Constructor;

/**
 * @author RePlugin Team
 */
public class PluginContext extends ContextThemeWrapper {

    private final ClassLoader mNewClassLoader;

    private final Resources mNewResources;

    private final String mPlugin;

    private final Object mSync = new Object();
    private final InstalledAppInfo mInstallAppInfo;
    private final int mUserId;

    private File mFilesDir;

    private File mCacheDir;

    private File mDatabasesDir;

    private LayoutInflater mInflater;

    private PluginContextInjector mContextInjector;

    LayoutInflater.Factory mFactory = new LayoutInflater.Factory() {

        @Override
        public View onCreateView(String name, Context context, AttributeSet attrs) {
            return handleCreateView(name, context, attrs);
        }
    };

    public PluginContext(Context base, int themeResId, ClassLoader cl, Resources r,
                         String plugin, int userId, InstalledAppInfo installedAppInfo) {
        super(base, themeResId);

        mNewClassLoader = cl;
        mNewResources = r;
        mPlugin = plugin;
        mUserId = userId;
        mInstallAppInfo = installedAppInfo;
        mContextInjector = null;
    }

    @Override
    public ClassLoader getClassLoader() {
        if (mNewClassLoader != null) {
            return mNewClassLoader;
        }
        return super.getClassLoader();
    }

    @Override
    public Resources getResources() {
        if (mNewResources != null) {
            return mNewResources;
        }
        return super.getResources();
    }

    @Override
    public AssetManager getAssets() {
        if (mNewResources != null) {
            return mNewResources.getAssets();
        }
        return super.getAssets();
    }

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

        return VEnvironment.getDataUserPackageDirectory(mUserId, mInstallAppInfo.packageName);
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

    @Override
    public String getPackageName() {
        // NOTE 请不要修改此方法，因为有太多的地方用到了PackageName
        // 为兼容性考虑，请直接返回卫士自身的包名
        return super.getPackageName();
    }

    // --------------
    // WARNING 注意！
    // --------------
    // 以下所有方法均需框架版本（Framework Ver，见说明书）>=3时才有效（有的需要更高版本）
    // Added by Jiongxuan Zhang
    @Override
    public Context getApplicationContext() {
        // 直接获取插件的Application对象
        // NOTE 切勿获取mLoader.mPkgContext，因为里面的一些方法会调用getApplicationContext（如registerComponentCallback）
        // NOTE 这样会造成StackOverflow异常。所以只能获取Application对象（框架版本为3以上的会创建此对象）
        //entry中调用context.getApplicationContext时mApplicationClient还没被赋值，会导致空指针造成插件安装失败
        /*if (mLoader.mPluginObj.mApplicationClient == null) {
            return this;
        } else {
            return mLoader.mPluginObj.mApplicationClient.getObj();
        }*/
        return this;
    }


    @Override
    public void startActivity(Intent intent) {
        // HINT 只有插件Application才会走这里
        // 而Activity.startActivity系统最终会走startActivityForResult，不会走这儿

        // 这里会被调用两次：
        // 第一次：获取各种信息，最终确认坑位，并走startActivity，再次回到这里
        // 第二次：判断要打开的是“坑位Activity”，则返回False，直接走super，后面的事情你们都懂的
        // 当然，如果在获取坑位信息时遇到任何情况（例如要打开的是宿主的Activity），则直接返回false，走super
        /*if (!Factory2.startActivity(this, intent)) {
            if (mContextInjector != null) {
                mContextInjector.startActivityBefore(intent);
            }

            super.startActivity(intent);

            if (mContextInjector != null) {
                mContextInjector.startActivityAfter(intent);
            }
        }*/
    }

    @Override
    public void startActivity(Intent intent, Bundle options) {
        // HINT 保险起见，startActivity写两套相似逻辑
        // 具体见startActivity(intent)的描述（上面）
        /*if (!Factory2.startActivity(this, intent)) {
            if (mContextInjector != null) {
                mContextInjector.startActivityBefore(intent, options);
            }

            super.startActivity(intent, options);

            if (mContextInjector != null) {
                mContextInjector.startActivityAfter(intent, options);
            }
        }*/
    }

//    @Override
//    public ComponentName startService(Intent service) {
//        if (mContextInjector != null) {
//            mContextInjector.startServiceBefore(service);
//        }
//        try {
//            return PluginServiceClient.startService(this, service, true);
//        } catch (PluginClientHelper.ShouldCallSystem e) {
//            // 若打开插件出错，则直接走系统逻辑
//            return super.startService(service);
//        } finally {
//            if (mContextInjector != null) {
//                mContextInjector.startServiceAfter(service);
//            }
//        }
//    }
//
//    @Override
//    public boolean stopService(Intent name) {
//        try {
//            return PluginServiceClient.stopService(this, name, true);
//        } catch (PluginClientHelper.ShouldCallSystem e) {
//            // 若打开插件出错，则直接走系统逻辑
//            return super.stopService(name);
//        }
//    }
//
//    @Override
//    public boolean bindService(Intent service, ServiceConnection conn, int flags) {
//        try {
//            return PluginServiceClient.bindService(this, service, conn, flags, true);
//        } catch (PluginClientHelper.ShouldCallSystem e) {
//            // 若打开插件出错，则直接走系统逻辑
//            return super.bindService(service, conn, flags);
//        }
//    }
//
//    @Override
//    public void unbindService(ServiceConnection conn) {
//        // 先走一遍系统的逻辑
//        try {
//            super.unbindService(conn);
//        } catch (Throwable e) {
//            // Ignore
//        }
//        // 再走插件的unbindService
//        // NOTE 由于不应重新调用context.unbind命令，故传进去的是false
//        PluginServiceClient.unbindService(this, conn, false);
//    }

    @Override
    public String getPackageCodePath() {
        // 获取插件Apk的路径
        return mInstallAppInfo.apkPath;
    }

    @Override
    public ApplicationInfo getApplicationInfo() {
        return mInstallAppInfo.getApplicationInfo(mInstallAppInfo.getInstalledUsers()[0]);
    }
}
