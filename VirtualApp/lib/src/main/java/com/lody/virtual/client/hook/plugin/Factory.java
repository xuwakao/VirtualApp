package com.lody.virtual.client.hook.plugin;

import android.app.Activity;
import android.content.Context;

public class Factory {
    public static final Context createActivityContext(Activity activity, Context newBase) {
        return PluginCore.get().mPluginCp.get(0).getClient().getPluginContext();
    }
}
