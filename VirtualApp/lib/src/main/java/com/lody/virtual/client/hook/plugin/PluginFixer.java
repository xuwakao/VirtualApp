package com.lody.virtual.client.hook.plugin;

import android.content.pm.ActivityInfo;

import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.server.pm.PackageSetting;

public class PluginFixer {
    public static void fixApplicationInfo(PackageSetting setting, ActivityInfo info, int userId) {
        if (setting.isPlugin(userId)) {
            info.applicationInfo = VirtualCore.get().getContext().getApplicationInfo();
        }
    }
}
