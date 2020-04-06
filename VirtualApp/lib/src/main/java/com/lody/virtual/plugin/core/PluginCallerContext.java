package com.lody.virtual.plugin.core;

import android.os.Looper;

public class PluginCallerContext {
    private static final String TAG = "PluginCallerContext";

    public static final int RESET = -1;
    private int mLatestCallRunningProcessPlugin = RESET;

    public PluginCallerContext() {
    }

    private void checkUIThread() {
        if (Looper.getMainLooper() != Looper.myLooper()) {
            throw new RuntimeException("Load in other thread");
        }
    }

    public void setLatestCallRunningProcessPlugin(int latestCallRunningProcessPlugin) {
        mLatestCallRunningProcessPlugin = latestCallRunningProcessPlugin;
    }

    public int getLatestCallRunningProcessPlugin() {
        int pluginId = mLatestCallRunningProcessPlugin;
        mLatestCallRunningProcessPlugin = RESET;
        return pluginId;
    }
}
