package com.lody.virtual.plugin.clientlib;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;

public class PluginClientContextHelper {
    /**
     * Get Host Context
     * @param activity
     * @return
     */
    public static Context getHostContext(Activity activity) {
        return ((ContextWrapper)activity.getBaseContext()).getBaseContext();
    }

    /**
     * Get Host PackageName
     * @param activity
     * @return
     */
    public static String getHostPackageName(Activity activity) {
        return getHostContext(activity).getPackageName();
    }

}
