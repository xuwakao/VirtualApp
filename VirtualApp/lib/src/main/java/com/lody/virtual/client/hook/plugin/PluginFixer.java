package com.lody.virtual.client.hook.plugin;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.res.Resources;

import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.server.pm.PackageSetting;

import mirror.android.content.ContextWrapper;
import mirror.android.content.pm.ComponentInfo;
import mirror.android.view.ContextThemeWrapper;
import mirror.com.android.internal.policy.PhoneWindow;

import static android.content.Context.LAYOUT_INFLATER_SERVICE;


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
     * @param plugin
     */
    public static void fixActivity(Activity activity, PluginImpl plugin) {
        int icon = plugin.getApplicationInfo().icon;
        int logo = plugin.getApplicationInfo().logo;
        int themeResId = plugin.getApplicationInfo().theme;
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

        mirror.android.app.Activity.mApplication.set(activity, plugin.getApp());
        ContextThemeWrapper.mResources.set(activity, plugin.getPluginContext().getResources());
        if (plugin.getApplicationInfo().theme > 0) {
            Resources.Theme theme = plugin.getPluginContext().getResources().newTheme();
            theme.applyStyle(plugin.getApplicationInfo().theme, true);
            ContextThemeWrapper.mTheme.set(activity, theme);
        }
        PhoneWindow.mLayoutInflater.set(activity.getWindow(), plugin.getPluginContext().getSystemService(LAYOUT_INFLATER_SERVICE));
        ContextWrapper.mBase.set(activity, plugin.getPluginContext());
    }
}
