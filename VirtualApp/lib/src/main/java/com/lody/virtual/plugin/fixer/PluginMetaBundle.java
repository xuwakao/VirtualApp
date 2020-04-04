package com.lody.virtual.plugin.fixer;

import android.content.pm.ComponentInfo;
import android.os.Bundle;

public class PluginMetaBundle {
    public static void putPluginIdToMeta(ComponentInfo componentInfo, int vpid) {
        if (componentInfo.metaData == null) {
            componentInfo.metaData = new Bundle();
        }
        componentInfo.metaData.putInt("_VA_|_vpid_", vpid);
    }

    public static int getPluginIdFromMeta(ComponentInfo componentInfo) {
        if (componentInfo.metaData == null) {
            return -1;
        }
        return componentInfo.metaData.getInt("_VA_|_vpid_", -1);
    }
}
