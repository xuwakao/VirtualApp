package com.lody.virtual.plugin.utils;

public class PluginHandle {

    private static final int PLUGIN_VPID_MIN = 100000;

    /**
     * generate virtual pid for plugin process
     *
     * @param pluginId
     * @return
     */
    public static int genVPidForPlugin(int pluginId) {
        return PLUGIN_VPID_MIN + pluginId;
    }

    /**
     * fetch plugin id from virtual pid
     *
     * @return
     */
    public static int fetchPluginIdFromVPid(int vPid) {
        return vPid - PLUGIN_VPID_MIN;
    }

    public static boolean isPluginVPid(int vPid) {
        return vPid - PLUGIN_VPID_MIN >= 0;
    }
}
