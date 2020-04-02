package com.lody.virtual.client.hook.plugin;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;

import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.server.pm.PackageSetting;

import mirror.android.content.pm.ComponentInfo;


public class PluginFixer {
    public static void fixApplicationInfo(PackageSetting setting, ActivityInfo info, int userId) {
        if (setting.isPlugin(userId)) {
            info.applicationInfo = VirtualCore.get().getContext().getApplicationInfo();
        }
    }

    /**
     * Set theme before activity create and fix crash android.content.res.Resources$NotFoundException
     * when {@link Activity#initWindowDecorActionBar} set window default icon and logo.
     *
     * @param activity
     * @param themeResId
     * @param icon
     * @param logo
     */
    public static void fixActivity(Activity activity, int themeResId, int icon, int logo) {
        activity.setTheme(themeResId);
        ApplicationInfo info = ComponentInfo.applicationInfo.get(mirror.android.app.Activity.mActivityInfo.get(activity));
        if (icon > 0) {
            mirror.android.app.Activity.mActivityInfo.get(activity).icon = icon;
        } else if (info.icon > 0) {
            ApplicationInfo applicationInfo = new ApplicationInfo(info);
            applicationInfo.icon = 0;
            ComponentInfo.applicationInfo.set(mirror.android.app.Activity.mActivityInfo.get(activity), applicationInfo);
        }

        if (logo > 0) {
            mirror.android.app.Activity.mActivityInfo.get(activity).logo = logo;
        } else if (info.logo > 0) {
            ApplicationInfo applicationInfo = new ApplicationInfo(info);
            applicationInfo.logo = 0;
            ComponentInfo.applicationInfo.set(mirror.android.app.Activity.mActivityInfo.get(activity), applicationInfo);
        }
    }
}
