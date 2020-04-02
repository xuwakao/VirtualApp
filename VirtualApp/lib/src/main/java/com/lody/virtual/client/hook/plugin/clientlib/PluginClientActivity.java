package com.lody.virtual.client.hook.plugin.clientlib;

import android.app.Activity;
import android.content.Context;

public abstract class PluginClientActivity extends Activity {
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(getApplication().getBaseContext());
    }
}
