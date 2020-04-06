package com.lody.virtual.plugin.fixer;

import android.app.Activity;
import android.content.pm.ApplicationInfo;
import android.content.res.Resources;

import com.lody.virtual.client.fixer.ActivityFixer;
import com.lody.virtual.plugin.PluginImpl;

import mirror.android.content.ContextWrapper;
import mirror.android.content.pm.ComponentInfo;
import mirror.android.view.ContextThemeWrapper;
import mirror.com.android.internal.policy.PhoneWindow;

import static android.content.Context.LAYOUT_INFLATER_SERVICE;


public class PluginFixer {

    public static void fixComponentApplicationInfo(android.content.pm.ComponentInfo componentInfo, ApplicationInfo oldAppInfo) {
        ApplicationInfo newAppInfo = new ApplicationInfo(oldAppInfo);
        componentInfo.applicationInfo = newAppInfo;
    }

    /**
     * Set theme before activity create and fix crash android.content.res.Resources$NotFoundException
     * when {@link Activity#initWindowDecorActionBar} set window default icon and logo.
     *
     * @param activity
     * @param plugin
     */
    public static void fixActivity(Activity activity, PluginImpl plugin) {
        ApplicationInfo pluginAppInfo = plugin.getApplicationInfo();
        int icon = pluginAppInfo.icon;
        int logo = pluginAppInfo.logo;
        int themeResId = pluginAppInfo.theme;
        activity.setTheme(themeResId);
        ApplicationInfo info = ComponentInfo.applicationInfo.get(mirror.android.app.Activity.mActivityInfo.get(activity));
        if (icon > 0) {
            mirror.android.app.Activity.mActivityInfo.get(activity).icon = icon;
        } else if (info.icon > 0) {
            fixComponentApplicationInfo(mirror.android.app.Activity.mActivityInfo.get(activity), info);
            mirror.android.app.Activity.mActivityInfo.get(activity).applicationInfo.icon = 0;
        }

        if (logo > 0) {
            mirror.android.app.Activity.mActivityInfo.get(activity).logo = logo;
        } else if (info.logo > 0) {
            fixComponentApplicationInfo(mirror.android.app.Activity.mActivityInfo.get(activity), info);
            mirror.android.app.Activity.mActivityInfo.get(activity).applicationInfo.logo = 0;
        }

        mirror.android.app.Activity.mApplication.set(activity, plugin.getApp());
        ContextThemeWrapper.mResources.set(activity, plugin.getPluginContext().getResources());
        if (pluginAppInfo.theme > 0) {
            Resources.Theme theme = plugin.getPluginContext().getResources().newTheme();
            theme.applyStyle(pluginAppInfo.theme, true);
            ContextThemeWrapper.mTheme.set(activity, theme);
        }
        PhoneWindow.mLayoutInflater.set(activity.getWindow(), plugin.getPluginContext().getSystemService(LAYOUT_INFLATER_SERVICE));
        ContextWrapper.mBase.set(activity, plugin.getPluginContext());
        activity.setTitle(plugin.getPluginContext().getResources().getString(pluginAppInfo.labelRes));
        ActivityFixer.fixActivity(activity);
    }
}