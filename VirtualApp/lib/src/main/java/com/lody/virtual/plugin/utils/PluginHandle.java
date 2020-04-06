package com.lody.virtual.plugin.utils;

public class PluginHandle {

    private static final int PER_PLUGIN_RANGE = 1000000;

    public static int getPluginHandle(int pluginId, int user) {
        return (pluginId + 1) * PER_PLUGIN_RANGE + user;
    }

    public static boolean isPlugin(int handle) {
        return handle / PER_PLUGIN_RANGE > 0;
    }

    public static int getPluginId(int handle) {
        return handle / PER_PLUGIN_RANGE - 1;
    }

    public static int getUser(int handle) {
        return handle % PER_PLUGIN_RANGE;
    }
}
