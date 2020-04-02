package com.lody.virtual.client.hook.plugin;

import android.app.Activity;
import android.content.pm.ActivityInfo;

import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.server.pm.PackageSetting;


public class PluginFixer {
    public static void fixApplicationInfo(PackageSetting setting, ActivityInfo info, int userId) {
        if (setting.isPlugin(userId)) {
            info.applicationInfo = VirtualCore.get().getContext().getApplicationInfo();
        }
    }

    public static void fixActivity(Activity activity, int themeResId, int icon, int logo) {
        activity.setTheme(themeResId);
        mirror.android.app.Activity.mActivityInfo.get(activity).icon = icon;
        mirror.android.app.Activity.mActivityInfo.get(activity).logo = logo;
    }
}
