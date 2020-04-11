package com.lody.virtual.plugin.fixer;

import android.content.Intent;
import android.content.pm.ComponentInfo;
import android.os.Bundle;

public class PluginMetaBundle {
    public static void putPluginIdToMeta(ComponentInfo componentInfo, int vpid) {
        if (componentInfo.metaData == null) {
            componentInfo.metaData = new Bundle();
        }
        componentInfo.metaData.putInt("_VA_|_v_plugin_id_", vpid);
    }

    public static int getPluginIdFromMeta(ComponentInfo componentInfo) {
        if (componentInfo.metaData == null) {
            return -1;
        }
        return componentInfo.metaData.getInt("_VA_|_v_plugin_id_", -1);
    }

    public static void bePlugin(ComponentInfo componentInfo, boolean isPlugin) {
        if (componentInfo.metaData == null) {
            componentInfo.metaData = new Bundle();
        }
        componentInfo.metaData.putBoolean("_VA_|_is_plugin_", isPlugin);
    }

    public static boolean isPlugin(ComponentInfo componentInfo) {
        if (componentInfo.metaData == null) {
            return false;
        }
        return componentInfo.metaData.getBoolean("_VA_|_is_plugin_", false);
    }

    public static void putIntentPluginId(Intent intent, int vpid) {
        if (intent != null)
            intent.putExtra("_VA_|_v_plugin_id_", vpid);
    }

    public static int getIntentPluginId(Intent intent) {
        return intent.getIntExtra("_VA_|_v_plugin_id_", -1);
    }
}
